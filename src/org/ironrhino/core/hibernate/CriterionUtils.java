package org.ironrhino.core.hibernate;

import java.beans.PropertyDescriptor;
import java.io.Serializable;
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
					: Restrictions.like(name, value, entry.getValue());
			criterion = (criterion == null) ? temp : Restrictions.or(criterion, temp);
		}
		return criterion;
	}

	public static Criterion matchTag(String tagFieldName, String tag) {
		tag = tag.trim();
		return Restrictions.or(Restrictions.eq(tagFieldName, tag),
				Restrictions.or(Restrictions.like(tagFieldName, tag + ",", MatchMode.START),
						Restrictions.or(Restrictions.like(tagFieldName, "," + tag, MatchMode.END),
								Restrictions.like(tagFieldName, "," + tag + ",", MatchMode.ANYWHERE))));
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
							propertyName = propertyName.substring(0, index);
							if (propertyNames.contains(propertyName)) {
								Class<?> type = entityBeanWrapper.getPropertyType(propertyName);
								if (Persistable.class.isAssignableFrom(type)) {
									String alias = state.getAliases().get(propertyName);
									if (alias == null) {
										alias = propertyName + "_";
										while (state.getAliases().containsValue(alias))
											alias += "_";
										dc.createAlias(propertyName, alias);
										state.getAliases().put(propertyName, alias);
									}
									if (desc)
										dc.addOrder(Order.desc(alias + "." + subPropertyName));
									else
										dc.addOrder(Order.asc(alias + "." + subPropertyName));
									state.getOrderings().put(alias + "." + subPropertyName, desc);
								}
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
						operator = CriterionOperator.valueOf(operatorValue.toUpperCase());
					} catch (IllegalArgumentException e) {

					}
				if (operator == null)
					operator = CriterionOperator.EQ;
				if (parameterValues.length < operator.getParametersSize())
					continue;
				if (propertyName.indexOf('.') > 0) {
					String subPropertyName = propertyName.substring(propertyName.indexOf('.') + 1);
					propertyName = propertyName.substring(0, propertyName.indexOf('.'));
					if (propertyNames.contains(propertyName)) {
						Class<?> type = entityBeanWrapper.getPropertyType(propertyName);
						if (Persistable.class.isAssignableFrom(type)) {
							BeanWrapperImpl subBeanWrapper = new BeanWrapperImpl(type.newInstance());
							subBeanWrapper.setConversionService(conversionService);
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
								Criterion criterion = operator.operator(alias + "." + subPropertyName, values);
								if (criterion != null) {
									dc.add(criterion);
									state.getCriteria().add(alias + "." + subPropertyName);
								}
							} else {
								String[] arr = subPropertyName.split("\\.", 2);
								String subname = propertyName + "." + arr[0];
								String subalias = state.getAliases().get(subname);
								if (subalias == null) {
									subalias = propertyName + "_" + arr[0] + "_";
									while (state.getAliases().containsValue(subalias))
										subalias += "_";
									dc.createAlias(alias + "." + arr[0], subalias);
									state.getAliases().put(subname, subalias);
								}

								Criterion criterion = operator.operator(subalias + "." + arr[1], values);
								if (criterion != null) {
									dc.add(criterion);
									state.getCriteria().add(subalias + "." + arr[1]);
								}
							}
						}
					}
				} else if (propertyNames.contains(propertyName)) {
					Class<?> type = entityBeanWrapper.getPropertyType(propertyName);
					if (Persistable.class.isAssignableFrom(type)) {
						@SuppressWarnings("unchecked")
						BaseManager<?> em = ApplicationContextUtils
								.getEntityManager((Class<? extends Persistable<?>>) type);
						try {
							BeanWrapperImpl subBeanWrapper = new BeanWrapperImpl(type);
							subBeanWrapper.setConversionService(conversionService);
							subBeanWrapper.setPropertyValue("id", parameterValues[0]);
							Persistable<?> p = em.get((Serializable) subBeanWrapper.getPropertyValue("id"));
							if (p == null) {
								Map<String, NaturalId> naturalIds = AnnotationUtils
										.getAnnotatedPropertyNameAndAnnotations(type, NaturalId.class);
								if (naturalIds.size() == 1) {
									String name = naturalIds.entrySet().iterator().next().getKey();
									subBeanWrapper.setPropertyValue(name, parameterValues[0]);
									p = em.findOne((Serializable) subBeanWrapper.getPropertyValue(name));
								}
							}
							if (p != null) {
								dc.add(Restrictions.eq(propertyName, p));
								state.getCriteria().add(propertyName);
							}
						} catch (Exception e) {
							e.printStackTrace();
						}
					} else {
						if (!operator.isEffective(type, parameterValues.length > 0 ? parameterValues[0] : null,
								parameterValues.length > 1 ? parameterValues[1] : null))
							continue;
						values = new Object[parameterValues.length];
						for (int n = 0; n < values.length; n++) {
							entityBeanWrapper.setPropertyValue(propertyName, parameterValues[n]);
							values[n] = entityBeanWrapper.getPropertyValue(propertyName);
						}
						Criterion criterion = operator.operator(propertyName, values);
						if (criterion != null) {
							dc.add(criterion);
							state.getCriteria().add(propertyName);
						}
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return state;
	}

}