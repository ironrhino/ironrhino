package org.ironrhino.core.service;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.Collection;
import java.util.Iterator;

import org.hibernate.SessionFactory;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Restrictions;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.EventType;
import org.hibernate.event.spi.PreUpdateEvent;
import org.hibernate.event.spi.PreUpdateEventListener;
import org.hibernate.internal.SessionFactoryImpl;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = { HibernateConfiguration.class })
@TestPropertySource(properties = { "annotatedClasses=org.ironrhino.core.service.TreeNode", "hibernate.show_sql=true" })
public class BaseTreeableEntityTest {

	@Autowired
	private EntityManager<TreeNode> entityManager;

	@Autowired
	private SessionFactory sessionFactory;

	@Before
	public void setUp() {
		EventListenerRegistry registry = sessionFactory.unwrap(SessionFactoryImpl.class).getServiceRegistry()
				.getService(EventListenerRegistry.class);
		registry.appendListeners(EventType.PRE_UPDATE, new PreUpdateEventListener() {
			private static final long serialVersionUID = 1L;

			@Override
			public boolean onPreUpdate(PreUpdateEvent event) {
				throw new RuntimeException("Update operation shouldn't happen");
			}
		});
	}

	@Test
	public void test() {
		entityManager.setEntityClass(TreeNode.class);
		TreeNode parent = new TreeNode("parent");
		entityManager.save(parent);
		TreeNode child1 = new TreeNode("child1", 1);
		child1.setParent(parent);
		entityManager.save(child1);
		TreeNode child2 = new TreeNode("child2", 2);
		child2.setParent(parent);
		entityManager.save(child2);
		verify();
	}

	private void verify() {
		entityManager.execute(session -> {
			DetachedCriteria dc = entityManager.detachedCriteria().add(Restrictions.isNull("parent"));
			TreeNode parent = (TreeNode) dc.getExecutableCriteria(session).uniqueResult();
			assertThat("parent", is(parent.getName()));
			assertThat(parent.getFullId(), is(parent.getId() + "."));
			assertThat(parent.getLevel(), is(1));
			Collection<TreeNode> children = parent.getChildren();
			assertThat(children.size(), is(2));
			Iterator<TreeNode> it = children.iterator();
			TreeNode child1 = it.next();
			assertThat(child1.getId(), is(parent.getId() + 1));
			assertThat("child1", is(child1.getName()));
			assertThat(child1.getFullId(), is(parent.getId() + "." + child1.getId() + "."));
			assertThat(child1.getLevel(), is(2));
			TreeNode child2 = it.next();
			assertThat(child2.getId(), is(parent.getId() + 2));
			assertThat("child2", is(child2.getName()));
			assertThat(child2.getFullId(), is(parent.getId() + "." + child2.getId() + "."));
			assertThat(child2.getLevel(), is(2));
			return null;
		});

	}

}
