package org.ironrhino.core.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hibernate.metadata.ClassMetadata;
import org.hibernate.type.ManyToOneType;
import org.hibernate.type.Type;
import org.ironrhino.core.model.BaseTreeableEntity;
import org.ironrhino.core.model.Enableable;
import org.ironrhino.core.model.Persistable;
import org.ironrhino.core.model.Tuple;
import org.ironrhino.core.spring.configuration.ResourcePresentConditional;
import org.ironrhino.core.util.ErrorMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@ResourcePresentConditional("resources/spring/applicationContext-hibernate.xml")
public class DeleteChecker {

	@Autowired
	private SessionFactory sessionFactory;

	private Map<Class<?>, List<Tuple<Class<?>, String>>> mapping;

	@PostConstruct
	public void init() {
		mapping = new HashMap<Class<?>, List<Tuple<Class<?>, String>>>();
		Map<String, ClassMetadata> map = sessionFactory.getAllClassMetadata();
		for (Map.Entry<String, ClassMetadata> entry : map.entrySet()) {
			ClassMetadata cm = entry.getValue();
			String[] names = cm.getPropertyNames();
			for (String name : names) {
				Type type = cm.getPropertyType(name);
				if (type instanceof ManyToOneType) {
					if (BaseTreeableEntity.class.isAssignableFrom(cm
							.getMappedClass()) && name.equals("parent"))
						continue;
					ManyToOneType mtp = (ManyToOneType) type;
					Class<?> referrer = mtp.getReturnedClass();
					List<Tuple<Class<?>, String>> list = mapping.get(referrer);
					if (list == null) {
						list = new ArrayList<Tuple<Class<?>, String>>();
						mapping.put(referrer, list);
					}
					list.add(new Tuple<Class<?>, String>(cm.getMappedClass(),
							name));
				}
			}

		}
	}

	public void check(Persistable<?> entity) {
		if (entity instanceof Enableable) {
			Enableable enableable = (Enableable) entity;
			if (enableable.isEnabled())
				throw new ErrorMessage("delete.forbidden",
						new Object[] { entity }, "delete.forbidden.notdisabled");
		}
		List<Tuple<Class<?>, String>> references = mapping.get(entity
				.getClass());
		if (references != null && references.size() > 0) {
			Session session = sessionFactory.getCurrentSession();
			for (Tuple<Class<?>, String> tuple : references) {
				Criteria c = session.createCriteria(tuple.getKey());
				c.add(Restrictions.eq(tuple.getValue(), entity));
				c.setProjection(Projections.projectionList().add(
						Projections.rowCount()));
				long count = (Long) c.uniqueResult();
				if (count > 0)
					throw new ErrorMessage("delete.forbidden",
							new Object[] { entity },
							"delete.forbidden.referrer");
			}
		}
	}

}
