package org.ironrhino.core.service;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.persistence.Embeddable;
import javax.persistence.ManyToOne;
import javax.persistence.MappedSuperclass;
import javax.persistence.OneToOne;
import javax.persistence.PrimaryKeyJoinColumn;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hibernate.metadata.ClassMetadata;
import org.hibernate.metadata.CollectionMetadata;
import org.hibernate.type.CollectionType;
import org.hibernate.type.ComponentType;
import org.hibernate.type.ManyToOneType;
import org.hibernate.type.OneToOneType;
import org.hibernate.type.Type;
import org.ironrhino.core.model.BaseTreeableEntity;
import org.ironrhino.core.model.Enableable;
import org.ironrhino.core.model.Persistable;
import org.ironrhino.core.model.Tuple;
import org.ironrhino.core.spring.configuration.BeanPresentConditional;
import org.ironrhino.core.util.ErrorMessage;
import org.ironrhino.core.util.ReflectionUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@BeanPresentConditional(type = SessionFactory.class)
public class DeleteChecker {

	@Autowired
	private SessionFactory sessionFactory;

	private Map<Class<?>, List<Tuple<Class<?>, String>>> mapping = new HashMap<>();

	private Map<Class<?>, List<Tuple<Class<?>, Tuple<String, String>>>> componentMapping = new HashMap<>();

	private Map<Class<?>, List<Tuple<Class<?>, Tuple<String, String>>>> collectionMapping = new HashMap<>();

	@PostConstruct
	public void init() {
		Map<String, ClassMetadata> map = sessionFactory.getAllClassMetadata();
		for (Map.Entry<String, ClassMetadata> entry : map.entrySet()) {
			ClassMetadata cm = entry.getValue();
			String[] names = cm.getPropertyNames();
			for (String name : names) {
				if (name.startsWith("_") && name.lastIndexOf('_') > 0) {
					// ignore additional _com_acme_entity_attributes
					continue;
				}
				Type type = cm.getPropertyType(name);
				if (type instanceof ManyToOneType) {
					if (BaseTreeableEntity.class.isAssignableFrom(cm.getMappedClass()) && name.equals("parent"))
						continue;
					ManyToOneType mtoType = (ManyToOneType) type;
					Class<?> referrer = mtoType.getReturnedClass();
					List<Tuple<Class<?>, String>> list = mapping.get(referrer);
					if (list == null) {
						list = new ArrayList<>();
						mapping.put(referrer, list);
					}
					list.add(Tuple.of(cm.getMappedClass(), name));
				} else if (type instanceof OneToOneType) {
					OneToOneType otoType = (OneToOneType) type;
					Class<?> referrer = otoType.getReturnedClass();
					PropertyDescriptor pd = BeanUtils.getPropertyDescriptor(cm.getMappedClass(), name);
					if (pd != null) {
						PrimaryKeyJoinColumn pkjc = null;
						OneToOne oto = null;
						Class<?> declaredClass = pd.getReadMethod() != null ? pd.getReadMethod().getDeclaringClass()
								: cm.getMappedClass();
						if (pd.getReadMethod() != null) {
							pkjc = pd.getReadMethod().getAnnotation(PrimaryKeyJoinColumn.class);
							oto = pd.getReadMethod().getAnnotation(OneToOne.class);
						}
						if (pkjc == null)
							try {
								Field f = declaredClass.getDeclaredField(name);
								if (f != null) {
									pkjc = f.getAnnotation(PrimaryKeyJoinColumn.class);
									oto = f.getAnnotation(OneToOne.class);
								}
							} catch (Exception e) {
							}
						if (pkjc != null || oto != null && StringUtils.isNotBlank(oto.mappedBy())) {
							continue;
						}
					}
					List<Tuple<Class<?>, String>> list = mapping.get(referrer);
					if (list == null) {
						list = new ArrayList<>();
						mapping.put(referrer, list);
					}
					list.add(Tuple.of(cm.getMappedClass(), name));
				} else if (type instanceof CollectionType) {
					if (BaseTreeableEntity.class.isAssignableFrom(cm.getMappedClass()) && name.equals("children"))
						continue;
					CollectionType collectionType = (CollectionType) type;
					CollectionMetadata collectionMetadata = sessionFactory
							.getCollectionMetadata(collectionType.getRole());
					Class<?> componentClass = collectionMetadata.getElementType().getReturnedClass();
					if (!componentClass.isAnnotationPresent(Embeddable.class)) {
						// skip @OneToMany and @ManyToMany
						continue;
					}
					try {
						Class<?> clz = componentClass;
						while (true) {
							for (Field f : clz.getDeclaredFields()) {
								if (f.getAnnotation(ManyToOne.class) != null
										|| f.getAnnotation(OneToOne.class) != null) {
									OneToOne oneToOne = f.getAnnotation(OneToOne.class);
									if (oneToOne != null && StringUtils.isNotBlank(oneToOne.mappedBy()))
										continue;
									Class<?> referrer = f.getType();
									List<Tuple<Class<?>, Tuple<String, String>>> list = collectionMapping.get(referrer);
									if (list == null) {
										list = new ArrayList<>();
										collectionMapping.put(referrer, list);
									}
									list.add(Tuple.of(cm.getMappedClass(), Tuple.of(name, f.getName())));
								}
							}
							clz = clz.getSuperclass();
							if (clz.equals(Object.class) || clz.getAnnotation(MappedSuperclass.class) == null)
								break;
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				} else if (type instanceof ComponentType) {
					ComponentType componentType = (ComponentType) type;
					Class<?> componentClass = componentType.getReturnedClass();
					try {
						Class<?> clz = componentClass;
						while (true) {
							for (Field f : clz.getDeclaredFields()) {
								if (f.getAnnotation(ManyToOne.class) != null
										|| f.getAnnotation(OneToOne.class) != null) {
									OneToOne oneToOne = f.getAnnotation(OneToOne.class);
									if (oneToOne != null && StringUtils.isNotBlank(oneToOne.mappedBy()))
										continue;
									Class<?> referrer = f.getType();
									List<Tuple<Class<?>, Tuple<String, String>>> list = componentMapping.get(referrer);
									if (list == null) {
										list = new ArrayList<>();
										componentMapping.put(referrer, list);
									}
									list.add(Tuple.of(cm.getMappedClass(), Tuple.of(name, f.getName())));
								}
							}
							clz = clz.getSuperclass();
							if (clz.equals(Object.class) || clz.getAnnotation(MappedSuperclass.class) == null)
								break;
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		}
	}

	public void check(Persistable<?> entity) {
		if (entity instanceof Enableable) {
			Enableable enableable = (Enableable) entity;
			if (enableable.isEnabled())
				throw new ErrorMessage("delete.forbidden", new Object[] { entity }, "delete.forbidden.notdisabled");
		}
		List<Tuple<Class<?>, String>> references = mapping.get(ReflectionUtils.getActualClass(entity));
		if (references != null && references.size() > 0) {
			Session session = sessionFactory.getCurrentSession();
			for (Tuple<Class<?>, String> tuple : references) {
				Criteria c = session.createCriteria(tuple.getKey());
				c.add(Restrictions.eq(tuple.getValue(), entity));
				c.setProjection(Projections.projectionList().add(Projections.rowCount()));
				long count = (Long) c.uniqueResult();
				if (count > 0)
					throw new ErrorMessage("delete.forbidden", new Object[] { entity }, "delete.forbidden.referrer");
			}
		}
		List<Tuple<Class<?>, Tuple<String, String>>> componentReferences = componentMapping
				.get(ReflectionUtils.getActualClass(entity));
		if (componentReferences != null && componentReferences.size() > 0) {
			Session session = sessionFactory.getCurrentSession();
			for (Tuple<Class<?>, Tuple<String, String>> tuple : componentReferences) {
				Criteria c = session.createCriteria(tuple.getKey());
				Tuple<String, String> value = tuple.getValue();
				c.add(Restrictions.eq(value.getKey() + '.' + value.getValue(), entity));
				c.setProjection(Projections.projectionList().add(Projections.rowCount()));
				long count = (Long) c.uniqueResult();
				if (count > 0)
					throw new ErrorMessage("delete.forbidden", new Object[] { entity }, "delete.forbidden.referrer");
			}
		}
		List<Tuple<Class<?>, Tuple<String, String>>> collectionReferences = collectionMapping
				.get(ReflectionUtils.getActualClass(entity));
		if (collectionReferences != null && collectionReferences.size() > 0) {
			Session session = sessionFactory.getCurrentSession();
			for (Tuple<Class<?>, Tuple<String, String>> tuple : collectionReferences) {
				Criteria c = session.createCriteria(tuple.getKey());
				Tuple<String, String> value = tuple.getValue();
				c.createAlias(value.getKey(), value.getKey())
						.add(Restrictions.eq(value.getKey() + '.' + value.getValue(), entity));
				c.setProjection(Projections.projectionList().add(Projections.rowCount()));
				long count = (Long) c.uniqueResult();
				if (count > 0)
					throw new ErrorMessage("delete.forbidden", new Object[] { entity }, "delete.forbidden.referrer");
			}
		}
	}

}