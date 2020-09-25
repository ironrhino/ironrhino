package org.ironrhino.core.service;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.Collection;
import java.util.Iterator;

import org.ironrhino.core.service.BaseTreeControlTest.TreeConfiguration;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = { HibernateConfiguration.class, TreeConfiguration.class })
@TestPropertySource(properties = { "annotatedClasses=org.ironrhino.core.service.TreeNode", "hibernate.show_sql=true" })
public class BaseTreeControlTest {

	@Autowired
	private EntityManager<TreeNode> entityManager;

	@Autowired
	private BaseTreeControl<TreeNode> treeNodeControl;

	@Test
	public void test() {
		entityManager.setEntityClass(TreeNode.class);
		TreeNode parent = new TreeNode("parent");
		entityManager.save(parent);
		TreeNode child1 = new TreeNode("child1",1);
		child1.setParent(parent);
		entityManager.save(child1);
		TreeNode child2 = new TreeNode("child2",2);
		child2.setParent(parent);
		entityManager.save(child2);
		verify();
		entityManager.executeUpdate("update TreeNode t set t.fullId=concat(t.fullId,'xxx'),t.level=0");
		treeNodeControl.repairHierarchy();
		verify();
		entityManager.executeUpdate("delete from TreeNode t");
	}

	private void verify() {
		TreeNode root = treeNodeControl.getTree();
		assertThat(root.getId(), is(0L));
		assertThat(root.getFullId(), is(nullValue()));
		assertThat(root.getLevel(), is(0));
		TreeNode parent = root.getChildren().iterator().next();
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
	}

	@Configuration
	public static class TreeConfiguration {

		@Bean
		public BaseTreeControl<TreeNode> treeNodeControl() {
			return new BaseTreeControl<TreeNode>() {

			};
		}

	}

}
