package org.ironrhino.core.hibernate;

import java.beans.PropertyDescriptor;
import java.io.Serializable;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.annotations.NaturalId;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.ironrhino.core.model.Persistable;
import org.ironrhino.core.service.BaseManager;
import org.ironrhino.core.servlet.RequestContext;
import org.ironrhino.core.struts.AnnotationShadows.UiConfigImpl;
import org.ironrhino.core.struts.EntityClassHelper;
import org.ironrhino.core.util.AnnotationUtils;
import org.ironrhino.core.util.ApplicationContextUtils;
import org.ironrhino.core.util.BeanUtils;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.core.convert.ConversionService;

public class CriterionUtils {

	public static final String CRITERION_OPERATOR_SUFFIX = "-op";

	public static final String CRITERION_ORDER_SUFFIX = "-od";

	public static Criterion like(String value, String... names) {
		Criterion criterion = null;
		int index = value.indexOf(':');
		String field = null;
		if (index > 0) {
			field = value.substring(0, index);
			String value2 = value.substring(index + 1);
			for (String name : names) {
				if (name.equals(field) || name.equals(field + "AsString")) {
					if (field.equals("tags")) {
						criterion = matchTag(name, value2);
					} else {
						criterion = Restrictions.like(name, value2, MatchMode.ANYWHERE);
					}
					return criterion;
				}
			}
		}
		for (String name : names) {
			Criterion temp = Restrictions.like(name, value, MatchMode.ANYWHERE);
			criterion = (criterion == null) ? temp : Restrictions.or(criterion, temp);
		}
		return criterion;
	}

	public static Criterion like(String value, Map<String, MatchMode> map) {
		Criterion criterion = null;
		int index = value.indexOf(':');
		String field = null;
		if (index > 0) {
			field = value.substring(0, index);
			String value2 = value.substring(index + 1);
			for (Map.Entry<String, MatchMode> entry : map.entrySet()) {
				String name = entry.getKey();
				if (name.equals(field) || name.equals(field + "AsString")) {
					if (field.equals("tags")) {
						criterion = matchTag(name, value2);
					} else {
						criterion = (entry.getValue() == MatchMode.EXACT) ? Restrictions.eq(name, value)
								: Restrictions.like(name, value2, entry.getValue());
					}
					return criterion;
				}
			}
		}
		for (Map.Entry<String, MatchMode> entry : map.entrySet()) {
			String name = entry.getKey();
			Criterion temp = (entry.getValue() == MatchMode.EXACT) ? Restrictions.eq(name, value)
					: (entry.getValue() == null) ? matchTag(name, value)
							: Restrictions.like(name, value, entry.getValue());
			// null marks as tag
			criterion = (criterion == null) ? temp : Restrictions.or(criterion, temp);
		}
		return criterion;
	}

	public static Criterion matchTag(String tagFieldName, String tag) {
		return new FindInSetCriterion(tagFieldName, tag.trim());
	}

	public static CriteriaState filter(DetachedCriteria dc, Class<? extends Persistable<?>> entityClass) {
		return filter(dc, entityClass, EntityClassHelper.getUiConfigs(entityClass));
	}

	public static void filter(DetachedCriteria dc, Object entity, String... propertyNames) {
		BeanWrapperImpl bw = new BeanWrapperImpl(entity);
		for (String propertyName : propertyNames) {
			Object value = bw.getPropertyValue(propertyName);
			if (value instanceof String) {
				if (StringUtils.isNotBlank((String) value))
					dc.add(Restrictions.eq(propertyName, value));
			} else if (value != null) {
				dc.add(Restrictions.eq(propertyName, value));
			}
		}
	}

	public static CriteriaState filter(DetachedCriteria dc, Class<? extends Persistable<?>> entityClass,
			Map<String, UiConfigImpl> uiConfigs) {
		Map<String, String[]> parameterMap = RequestContext.getRequest().getParameterMap();
		String entityName = StringUtils.uncapitalize(entityClass.getSimpleName());
		Set<String> propertyNames = uiConfigs.keySet();
		CriteriaState state = new CriteriaState();
		try {
			ConversionService conversionService = ApplicationContextUtils.getBean(ConversionService.class);
			BeanWrapperImpl entityBeanWrapper = new BeanWrapperImpl(entityClass.newInstance());
			entityBeanWrapper.setConversionService(conversionService);
			for (String parameterName : parameterMap.keySet()) {
				String propertyName;
				String[] parameterValues;
				Object[] values;
				String operatorValue;
				if (parameterName.endsWith(CRITERION_ORDER_SUFFIX)) {
					propertyName = parameterName.substring(0, parameterName.length() - CRITERION_ORDER_SUFFIX.length());
					String s = parameterMap.get(parameterName)[0];
					Boolean desc = s.equalsIgnoreCase("desc");
					if (propertyName.equals("id") || propertyName.endsWith("Date")) {
						if (desc)
							dc.addOrder(Order.desc(propertyName));
						else
							dc.addOrder(Order.asc(propertyName));
						continue;
					}
					if (propertyName.startsWith(entityName + "."))
						propertyName = propertyName.substring(propertyName.indexOf('.') + 1);
					s = propertyName;
					if (s.indexOf('.') > 0)
						s = s.substring(0, s.indexOf('.'));
					UiConfigImpl config = uiConfigs.get(s);
					if (config != null && !config.isExcludedFromOrdering()) {
						int index;
						if ((index = propertyName.indexOf('.')) > 0) {
							String subPropertyName = propertyName.substring(index + 1);
							String pname = propertyName.substring(0, index);
							Class<?> type = entityBeanWrapper.getPropertyType(pname);
							if (Persistable.class.isAssignableFrom(type)) {
								// @ManyToOne
								propertyName = pname;
								String alias = state.getAliases().get(propertyName);
								if (alias == null) {
									alias = propertyName + "_";
									while (state.getAliases().containsValue(alias))
										alias += "_";
									dc.createAlias(propertyName, alias);
									state.getAliases().put(propertyName, alias);
								}
								if (desc)
									dc.addOrder(Order.desc(alias + '.' + subPropertyName));
								else
									dc.addOrder(Order.asc(alias + '.' + subPropertyName));
								state.getOrderings().put(alias + '.' + subPropertyName, desc);
							} else {
								// @EmbeddedId or @Embedded
								if (desc)
									dc.addOrder(Order.desc(propertyName));
								else
									dc.addOrder(Order.asc(propertyName));
								state.getOrderings().put(propertyName, desc);
							}
						} else if (propertyNames.contains(propertyName)) {
							if (desc)
								dc.addOrder(Order.desc(propertyName));
							else
								dc.addOrder(Order.asc(propertyName));
							state.getOrderings().put(propertyName, desc);
						}
					}
					continue;
				}
				if (parameterName.endsWith(CRITERION_OPERATOR_SUFFIX)) {
					propertyName = parameterName.substring(0,
							parameterName.length() - CRITERION_OPERATOR_SUFFIX.length());
					if (parameterMap.containsKey(propertyName))
						continue;
					parameterValues = new String[0];
					operatorValue = parameterMap.get(parameterName)[0];
				} else {
					propertyName = parameterName;
					parameterValues = parameterMap.get(parameterName);
					operatorValue = RequestContext.getRequest().getParameter(parameterName + CRITERION_OPERATOR_SUFFIX);
				}
				if (propertyName.startsWith(entityName + "."))
					propertyName = propertyName.substring(propertyName.indexOf('.') + 1);
				String s = propertyName;
				if (s.indexOf('.') > 0)
					s = s.substring(0, s.indexOf('.'));
				UiConfigImpl config = uiConfigs.get(s);
				if (config == null || config.isExcludedFromCriteria())
					continue;
				CriterionOperator operator = null;
				if (StringUtils.isNotBlank(operatorValue))
					try {
						operator = CriterionOperator.valueOf(operatorValue.toUpperCase(Locale.ROOT));
					} catch (IllegalArgumentException e) {

					}
				if (operator == null)
					operator = CriterionOperator.EQ;
				if (parameterValues.length < operator.getParametersSize())
					continue;
				if (propertyName.indexOf('.') > 0) {
					String subPropertyName = propertyName.substring(propertyName.indexOf('.') + 1);
					String pname = propertyName.substring(0, propertyName.indexOf('.'));
					Class<?> type = entityBeanWrapper.getPropertyType(pname);
					if (Persistable.class.isAssignableFrom(type)) {
						// @ManyToOne
						propertyName = pname;
						BeanWrapperImpl subBeanWrapper = new BeanWrapperImpl(type.newInstance());
						subBeanWrapper.setConversionService(conversionService);
						if (subPropertyName.equals("id") && StringUtils.isBlank(config.getReferencedColumnName())) {
							if (parameterValues.length > 0)
								subBeanWrapper.setPropertyValue("id", parameterValues[0]);
							Criterion criterion = operator.operator(propertyName, subBeanWrapper.getWrappedInstance());
							if (criterion != null) {
								dc.add(criterion);
								state.getCriteria().add(propertyName);
							}
							continue;
						}
						PropertyDescriptor pd = BeanUtils.getPropertyDescriptor(subBeanWrapper.getWrappedClass(),
								subPropertyName);
						if (pd == null || !operator.isEffective(pd.getPropertyType(), parameterValues))
							continue;
						values = new Object[parameterValues.length];
						for (int n = 0; n < values.length; n++) {
							BeanUtils.setPropertyValue(subBeanWrapper.getWrappedInstance(), subPropertyName,
									parameterValues[n]);
							values[n] = subBeanWrapper.getPropertyValue(subPropertyName);
						}
						String alias = state.getAliases().get(propertyName);
						if (alias == null) {
							alias = propertyName + "_";
							while (state.getAliases().containsValue(alias))
								alias += "_";
							dc.createAlias(propertyName, alias);
							state.getAliases().put(propertyName, alias);
						}
						if (subPropertyName.indexOf('.') < 0 || subPropertyName.endsWith(".id")) {
							Criterion criterion = operator.operator(alias + '.' + subPropertyName, values);
							if (criterion != null) {
								dc.add(criterion);
								state.getCriteria().add(alias + '.' + subPropertyName);
							}
						} else {
							String[] arr = subPropertyName.split("\\.", 2);
							String subname = propertyName + '.' + arr[0];
							String subalias = state.getAliases().get(subname);
							if (subalias == null) {
								subalias = propertyName + '_' + arr[0] + "_";
								while (state.getAliases().containsValue(subalias))
									subalias += "_";
								dc.createAlias(alias + '.' + arr[0], subalias);
								state.getAliases().put(subname, subalias);
							}

							Criterion criterion = operator.operator(subalias + '.' + arr[1], values);
							if (criterion != null) {
								dc.add(criterion);
								state.getCriteria().add(subalias + '.' + arr[1]);
							}
						}
					} else {
						// @EmbeddedId or @Embedded
						BeanWrapperImpl subBeanWrapper = new BeanWrapperImpl(type);
						subBeanWrapper.setConversionService(conversionService);
						if (!operator.isEffective(subBeanWrapper.getPropertyType(subPropertyName), parameterValues))
							continue;
						values = new Object[parameterValues.length];
						for (int n = 0; n < values.length; n++) {
							subBeanWrapper.setPropertyValue(subPropertyName, parameterValues[n]);
							values[n] = subBeanWrapper.getPropertyValue(subPropertyName);
						}
						Criterion criterion = operator.operator(propertyName, values);
						if (criterion != null) {
							dc.add(criterion);
							state.getCriteria().add(propertyName);
						}
					}
					continue;
				}
				Class<?> type = null;
				Class<?> collectionType = null;
				PropertyDescriptor pd = entityBeanWrapper.getPropertyDescriptor(propertyName);
				Type returnType = pd.getReadMethod().getGenericReturnType();
				if (returnType instanceof ParameterizedType) {
					// detect if is @ManyToMany
					ParameterizedType pt = (ParameterizedType) returnType;
					if (pt.getActualTypeArguments().length == 1) {
						Type argType = pt.getActualTypeArguments()[0];
						Type rawType = pt.getRawType();
						if (rawType instanceof Class && argType instanceof Class) {
							collectionType = (Class<?>) rawType;
							type = (Class<?>) argType;
							if (!Collection.class.isAssignableFrom(collectionType)
									|| !Persistable.class.isAssignableFrom(type)) {
								collectionType = null;
								type = null;
							}
						}
					}
				} else if (returnType instanceof Class) {
					type = (Class<?>) returnType;
				}
				if (type == null)
					type = entityBeanWrapper.getPropertyType(propertyName);
				if (Persistable.class.isAssignableFrom(type)) {
					// @ManyToOne or @ManyToMany
					@SuppressWarnings("unchecked")
					BaseManager<?> em = ApplicationContextUtils
							.getEntityManager((Class<? extends Persistable<?>>) type);
					try {
						BeanWrapperImpl subBeanWrapper = new BeanWrapperImpl(type);
						subBeanWrapper.setConversionService(conversionService);
						if (parameterValues.length > 0)
							subBeanWrapper.setPropertyValue("id", parameterValues[0]);
						if (collectionType == null) {
							Persistable<?> p = em.get((Serializable) subBeanWrapper.getPropertyValue("id"));
							if (p == null) {
								Map<String, NaturalId> naturalIds = AnnotationUtils
										.getAnnotatedPropertyNameAndAnnotations(type, NaturalId.class);
								if (naturalIds.size() == 1) {
									String name = naturalIds.entrySet().iterator().next().getKey();
									if (parameterValues.length > 0)
										subBeanWrapper.setPropertyValue(name, parameterValues[0]);
									p = em.findOne((Serializable) subBeanWrapper.getPropertyValue(name));
								}
							}
							if (p == null) {
								dc.add(Restrictions.isNull("id"));
								// return empty result set
								break;
							}
							Criterion criterion = operator.operator(propertyName, p);
							if (criterion != null) {
								dc.add(criterion);
								state.getCriteria().add(propertyName);
							}
						} else if (operator == CriterionOperator.CONTAINS) {
							// @ManyToMany
							String alias = state.getAliases().get(propertyName);
							if (alias == null) {
								alias = propertyName + "_";
								while (state.getAliases().containsValue(alias))
									alias += "_";
								dc.createAlias(propertyName, alias);
								state.getAliases().put(propertyName, alias);
							}
							dc.add(Restrictions.eq(alias + ".id", subBeanWrapper.getPropertyValue("id")));
							state.getCriteria().add(propertyName);
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				} else {
					if (!operator.isEffective(type, parameterValues))
						if (!("dictionary".equals(config.getType())
								&& (operator == CriterionOperator.IN || operator == CriterionOperator.NOTIN)))
							continue;
					values = new Object[parameterValues.length];
					for (int n = 0; n < values.length; n++) {
						entityBeanWrapper.setPropertyValue(propertyName, parameterValues[n]);
						Object v = entityBeanWrapper.getPropertyValue(propertyName);
						if (operator == CriterionOperator.CONTAINS) {
							if (v instanceof Collection) {
								Collection<?> coll = (Collection<?>) v;
								v = coll.size() > 0 ? ((Collection<?>) v).iterator().next() : null;
							} else if (v instanceof Object[]) {
								Object[] arr = (Object[]) v;
								v = arr.length > 0 ? arr[0] : null;
							}
							if (v instanceof Enum) {
								Enum<?> en = (Enum<?>) v;
								v = en.name();
							}
						}
						values[n] = v;
					}
					Criterion criterion = operator.operator(propertyName, values);
					if (criterion != null) {
						dc.add(criterion);
						state.getCriteria().add(propertyName);
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return state;
	}

}