package org.ironrhino.core.hibernate;

import java.io.Serializable;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.struts2.ServletActionContext;
import org.hibernate.annotations.NaturalId;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.ironrhino.core.model.Persistable;
import org.ironrhino.core.service.BaseManager;
import org.ironrhino.core.struts.AnnotationShadows.UiConfigImpl;
import org.ironrhino.core.struts.EntityClassHelper;
import org.ironrhino.core.util.AnnotationUtils;
import org.ironrhino.core.util.ApplicationContextUtils;
import org.ironrhino.core.util.CodecUtils;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.core.convert.ConversionService;

public class CriterionUtils {

	public static final String CRITERION_OPERATOR_SUFFIX = "-op";

	public static final String CRITERION_ORDER_SUFFIX = "-od";

	public static Criterion like(String value, MatchMode mode, String... names) {
		Criterion c = null;
		int index = value.indexOf(':');
		String field = null;
		if (index > 0) {
			field = value.substring(0, index);
			value = value.substring(index + 1);
			for (String name : names) {
				if (name.equals(field) || name.equals(field + "AsString")) {
					if (field.equals("tags")) {
						c = matchTag(name, value);
					} else {
						c = Restrictions.like(name, value, MatchMode.ANYWHERE);
					}
					break;
				}
			}
		} else {
			for (String name : names) {
				if (c == null) {
					c = Restrictions.like(name, value, MatchMode.ANYWHERE);
				} else {
					c = Restrictions.or(c,
							Restrictions.like(name, value, MatchMode.ANYWHERE));
				}
			}
		}
		return c;
	}

	public static Criterion matchTag(String tagFieldName, String tag) {
		tag = tag.trim();
		return Restrictions.or(Restrictions.eq(tagFieldName, tag),
				Restrictions.or(Restrictions.like(tagFieldName, tag + ",",
						MatchMode.START), Restrictions.or(Restrictions.like(
						tagFieldName, "," + tag, MatchMode.END),
						Restrictions.like(tagFieldName, "," + tag + ",",
								MatchMode.ANYWHERE))));
	}

	public static CriteriaState filter(DetachedCriteria dc,
			Class<? extends Persistable<?>> entityClass) {
		return filter(dc, entityClass,
				EntityClassHelper.getUiConfigs(entityClass));
	}

	public static CriteriaState filter(DetachedCriteria dc,
			Class<? extends Persistable<?>> entityClass,
			Map<String, UiConfigImpl> uiConfigs) {
		CriteriaState state = new CriteriaState();
		try {
			ConversionService conversionService = ApplicationContextUtils
					.getBean(ConversionService.class);
			Set<String> propertyNames = uiConfigs.keySet();
			BeanWrapperImpl bw = new BeanWrapperImpl(entityClass.newInstance());
			bw.setConversionService(conversionService);
			String entityName = StringUtils.uncapitalize(entityClass
					.getSimpleName());
			Map<String, String[]> parameterMap = ServletActionContext
					.getRequest().getParameterMap();
			for (String parameterName : parameterMap.keySet()) {
				String propertyName;
				String[] parameterValues;
				Object[] values;
				String operatorValue;
				if (parameterName.endsWith(CRITERION_ORDER_SUFFIX)) {
					propertyName = parameterName.substring(
							0,
							parameterName.length()
									- CRITERION_ORDER_SUFFIX.length());
					String s = parameterMap.get(parameterName)[0];
					Boolean desc = s.equalsIgnoreCase("desc");
					if (propertyName.startsWith(entityName + "."))
						propertyName = propertyName.substring(propertyName
								.indexOf('.') + 1);
					s = propertyName;
					if (s.indexOf('.') > 0)
						s = s.substring(0, s.indexOf('.'));
					UiConfigImpl config = uiConfigs.get(s);
					if (config != null && !config.isExcludedFromOrdering()) {
						if (propertyName.indexOf('.') > 0) {
							String subPropertyName = propertyName
									.substring(propertyName.indexOf('.') + 1);
							propertyName = propertyName.substring(0,
									propertyName.indexOf('.'));
							if (propertyNames.contains(propertyName)) {
								Class<?> type = bw
										.getPropertyType(propertyName);
								if (Persistable.class.isAssignableFrom(type)) {
									String alias = state.getAliases().get(
											propertyName);
									if (alias == null) {
										alias = CodecUtils.randomString(4);
										dc.createAlias(propertyName, alias);
										state.getAliases().put(propertyName,
												alias);
									}
									if (desc)
										dc.addOrder(Order.desc(alias + "."
												+ subPropertyName));
									else
										dc.addOrder(Order.asc(alias + "."
												+ subPropertyName));
									state.getOrderings()
											.put(alias + "." + subPropertyName,
													desc);
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
					propertyName = parameterName.substring(
							0,
							parameterName.length()
									- CRITERION_OPERATOR_SUFFIX.length());
					if (parameterMap.containsKey(propertyName))
						continue;
					parameterValues = new String[0];
					operatorValue = parameterMap.get(parameterName)[0];
				} else {
					propertyName = parameterName;
					parameterValues = parameterMap.get(parameterName);
					operatorValue = ServletActionContext.getRequest()
							.getParameter(
									parameterName + CRITERION_OPERATOR_SUFFIX);
				}
				if (propertyName.startsWith(entityName + "."))
					propertyName = propertyName.substring(propertyName
							.indexOf('.') + 1);
				String s = propertyName;
				if (s.indexOf('.') > 0)
					s = s.substring(0, s.indexOf('.'));
				UiConfigImpl config = uiConfigs.get(s);
				if (config == null || config.isExcludedFromCriteria())
					continue;
				CriterionOperator operator = null;
				if (StringUtils.isNotBlank(operatorValue))
					try {
						operator = CriterionOperator.valueOf(operatorValue
								.toUpperCase());
					} catch (IllegalArgumentException e) {

					}
				if (operator == null)
					operator = CriterionOperator.EQ;
				if (parameterValues.length < operator.getParametersSize())
					continue;
				if (propertyName.indexOf('.') > 0) {
					String subPropertyName = propertyName
							.substring(propertyName.indexOf('.') + 1);
					propertyName = propertyName.substring(0,
							propertyName.indexOf('.'));
					if (propertyNames.contains(propertyName)) {
						Class<?> type = bw.getPropertyType(propertyName);
						if (Persistable.class.isAssignableFrom(type)) {
							BeanWrapperImpl bw2 = new BeanWrapperImpl(
									type.newInstance());
							bw2.setConversionService(conversionService);
							if (!operator.isEffective(
									bw2.getPropertyType(subPropertyName),
									parameterValues))
								continue;
							values = new Object[parameterValues.length];
							for (int n = 0; n < values.length; n++) {
								bw2.setPropertyValue(subPropertyName,
										parameterValues[n]);
								values[n] = bw2
										.getPropertyValue(subPropertyName);
							}
							String alias = state.getAliases().get(propertyName);
							if (alias == null) {
								alias = CodecUtils.randomString(4);
								dc.createAlias(propertyName, alias);
								state.getAliases().put(propertyName, alias);
							}
							Criterion criterion = operator.operator(alias + "."
									+ subPropertyName, values);
							if (criterion != null) {
								dc.add(criterion);
								state.getCriteria().add(
										alias + "." + subPropertyName);
							}
						}
					}
				} else if (propertyNames.contains(propertyName)) {
					Class<?> type = bw.getPropertyType(propertyName);
					if (Persistable.class.isAssignableFrom(type)) {
						@SuppressWarnings("unchecked")
						BaseManager<?> em = ApplicationContextUtils
								.getEntityManager((Class<? extends Persistable<?>>) type);
						try {
							BeanWrapperImpl bw2 = new BeanWrapperImpl(type);
							bw2.setConversionService(conversionService);
							bw2.setPropertyValue("id", parameterValues[0]);
							Persistable<?> p = em.get((Serializable) bw2
									.getPropertyValue("id"));
							if (p == null) {
								Map<String, NaturalId> naturalIds = AnnotationUtils
										.getAnnotatedPropertyNameAndAnnotations(
												type, NaturalId.class);
								if (naturalIds.size() == 1) {
									String name = naturalIds.entrySet()
											.iterator().next().getKey();
									bw2.setPropertyValue(name,
											parameterValues[0]);
									p = em.findOne((Serializable) bw2
											.getPropertyValue(name));
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
						if (!operator.isEffective(type,
								parameterValues.length > 0 ? parameterValues[0]
										: null,
								parameterValues.length > 1 ? parameterValues[1]
										: null))
							continue;
						values = new Object[parameterValues.length];
						for (int n = 0; n < values.length; n++) {
							bw.setPropertyValue(propertyName,
									parameterValues[n]);
							values[n] = bw.getPropertyValue(propertyName);
						}
						Criterion criterion = operator.operator(propertyName,
								values);
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
