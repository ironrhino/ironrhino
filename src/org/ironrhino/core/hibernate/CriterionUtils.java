package org.ironrhino.core.hibernate;

import java.beans.PropertyDescriptor;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.annotations.NaturalId;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.hibernate.sql.JoinType;
import org.ironrhino.core.model.Persistable;
import org.ironrhino.core.service.BaseManager;
import org.ironrhino.core.servlet.RequestContext;
import org.ironrhino.core.struts.AnnotationShadows.UiConfigImpl;
import org.ironrhino.core.struts.EntityClassHelper;
import org.ironrhino.core.util.AnnotationUtils;
import org.ironrhino.core.util.ApplicationContextUtils;
import org.ironrhino.core.util.BeanUtils;
import org.ironrhino.core.util.DateUtils;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.core.convert.ConversionService;

import lombok.experimental.UtilityClass;

@UtilityClass
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
				if (name.equals(field)) {
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
				if (name.equals(field)) {
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
		return new FindInSetExpression(tagFieldName, tag);
	}

	public static Criterion matchTagPrefix(String tagFieldName, String tagPrefix) {
		return new PrefixFindInSetCriterion(tagFieldName, tagPrefix);
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
			BeanWrapperImpl entityBeanWrapper = new BeanWrapperImpl(entityClass.getConstructor().newInstance());
			entityBeanWrapper.setConversionService(conversionService);
			for (String parameterName : parameterMap.keySet()) {
				String propertyName;
				String[] parameterValues;
				Object[] values;
				String operatorValue;
				if (parameterName.endsWith(CRITERION_ORDER_SUFFIX)) {
					// ordering
					propertyName = parameterName.substring(0, parameterName.length() - CRITERION_ORDER_SUFFIX.length());
					Boolean desc = "desc".equalsIgnoreCase(parameterMap.get(parameterName)[0]);
					propertyName = trimEntityNamePrefixAndIdSuffix(propertyName, entityName);
					String topPropertyName = getTopPropertyName(propertyName);
					if (propertyName.equals("id") || propertyName.endsWith("Date")) {
						if (desc)
							dc.addOrder(Order.desc(propertyName));
						else
							dc.addOrder(Order.asc(propertyName));
						continue;
					}
					UiConfigImpl config = uiConfigs.get(topPropertyName);
					if (topPropertyName.equals("id") && config == null) {
						config = new UiConfigImpl("id",
								entityBeanWrapper.getPropertyDescriptor("id").getReadMethod().getGenericReturnType(),
								null);
					}
					if (config != null && !config.isExcludedFromOrdering()) {
						int index;
						if ((index = propertyName.indexOf('.')) > 0) {
							// nested property
							Type type = config.getGenericPropertyType();
							if (!(type instanceof Class))
								continue;
							if (Persistable.class.isAssignableFrom((Class<?>) type)) {
								// @ManyToOne
								String subPropertyName = propertyName.substring(index + 1);
								UiConfigImpl uci = EntityClassHelper.getUiConfigs((Class<?>) type)
										.get(getTopPropertyName(subPropertyName));
								if (uci == null || uci.isExcludedFromOrdering())
									continue;
								String alias = state.getAliases().get(topPropertyName);
								if (alias == null) {
									alias = topPropertyName + "_";
									while (state.getAliases().containsValue(alias))
										alias += "_";
									dc.createAlias(topPropertyName, alias);
									state.getAliases().put(topPropertyName, alias);
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
				propertyName = trimEntityNamePrefixAndIdSuffix(propertyName, entityName);
				String topPropertyName = getTopPropertyName(propertyName);
				UiConfigImpl config = uiConfigs.get(topPropertyName);
				if (topPropertyName.equals("id") && config == null) {
					config = new UiConfigImpl("id",
							entityBeanWrapper.getPropertyDescriptor("id").getReadMethod().getGenericReturnType(), null);
				}
				if (config == null || config.isExcludedFromCriteria())
					continue;
				CriterionOperator operator = null;
				if (StringUtils.isNotBlank(operatorValue))
					try {
						operator = CriterionOperator.valueOf(operatorValue.toUpperCase(Locale.ROOT));
					} catch (IllegalArgumentException e) {

					}
				if (operator == null) {
					if (parameterValues.length == 1 && StringUtils.isEmpty(parameterValues[0]))
						continue;
					else
						operator = CriterionOperator.EQ;
				}

				if (parameterValues.length < operator.getParametersSize())
					continue;
				Type type = config.getGenericPropertyType();
				if (propertyName.indexOf('.') > 0) {
					// nested property
					if (!(type instanceof Class))
						continue;
					String subPropertyName = propertyName.substring(propertyName.indexOf('.') + 1);
					if (Persistable.class.isAssignableFrom((Class<?>) type)) {
						// @ManyToOne or @OneToOne
						@SuppressWarnings("unchecked")
						Class<? extends Persistable<?>> enClass = (Class<? extends Persistable<?>>) type;
						UiConfigImpl uci = EntityClassHelper.getUiConfigs(enClass)
								.get(getTopPropertyName(subPropertyName));
						if (uci == null || uci.isExcludedFromCriteria())
							continue;
						PropertyDescriptor pd = BeanUtils.getPropertyDescriptor(enClass, subPropertyName);
						if (!operator.isEffective(pd.getReadMethod().getGenericReturnType(), parameterValues))
							continue;
						BeanWrapperImpl subBeanWrapper = new BeanWrapperImpl(enClass.getConstructor().newInstance());
						subBeanWrapper.setConversionService(conversionService);
						values = new Object[parameterValues.length];
						for (int n = 0; n < values.length; n++) {
							BeanUtils.setPropertyValue(subBeanWrapper.getWrappedInstance(), subPropertyName,
									parameterValues[n]);
							values[n] = subBeanWrapper.getPropertyValue(subPropertyName);
						}
						String alias = state.getAliases().get(topPropertyName);
						if (alias == null) {
							alias = topPropertyName + "_";
							while (state.getAliases().containsValue(alias))
								alias += "_";
							dc.createAlias(topPropertyName, alias);
							state.getAliases().put(topPropertyName, alias);
						}
						if (subPropertyName.indexOf('.') < 0) {
							Criterion criterion = operator.operator(alias + '.' + subPropertyName,
									pd.getReadMethod().getGenericReturnType(), values);
							if (criterion != null) {
								dc.add(criterion);
								state.getCriteria().add(alias + '.' + subPropertyName);
							}
						} else {
							String[] arr = subPropertyName.split("\\.", 2);
							String subname = topPropertyName + '.' + arr[0];
							String subalias = state.getAliases().get(subname);
							if (subalias == null) {
								subalias = alias + arr[0] + "_";
								while (state.getAliases().containsValue(subalias))
									subalias += "_";
								dc.createAlias(alias + '.' + arr[0], subalias);
								state.getAliases().put(subname, subalias);
							}
							Criterion criterion = operator.operator(subalias + '.' + arr[1],
									pd.getReadMethod().getGenericReturnType(), values);
							if (criterion != null) {
								dc.add(criterion);
								state.getCriteria().add(subalias + '.' + arr[1]);
							}
						}
					} else {
						// @EmbeddedId or @Embedded
						if (!operator.isEffective(BeanUtils.getPropertyDescriptor((Class<?>) type, subPropertyName)
								.getReadMethod().getGenericReturnType(), parameterValues))
							continue;
						BeanWrapperImpl subBeanWrapper = new BeanWrapperImpl((Class<?>) type);
						subBeanWrapper.setConversionService(conversionService);
						values = new Object[parameterValues.length];
						for (int n = 0; n < values.length; n++) {
							subBeanWrapper.setPropertyValue(subPropertyName, parameterValues[n]);
							values[n] = subBeanWrapper.getPropertyValue(subPropertyName);
						}
						Criterion criterion = operator.operator(propertyName, config.getGenericPropertyType(), values);
						if (criterion != null) {
							dc.add(criterion);
							state.getCriteria().add(propertyName);
						}
					}
				} else {
					// not nested property
					Class<?> collectionType = config.getCollectionType();
					Class<?> elementType = config.getElementType();
					if (type instanceof Class && Persistable.class.isAssignableFrom((Class<?>) type)
							|| elementType != null && Persistable.class.isAssignableFrom(elementType)) {
						// @ManyToOne or @OneToOne or @ManyToMany
						@SuppressWarnings("unchecked")
						Class<? extends Persistable<?>> enClass = (Class<? extends Persistable<?>>) (elementType != null
								? elementType
								: type);
						BaseManager<?> em = ApplicationContextUtils.getEntityManager(enClass);
						try {
							BeanWrapperImpl subBeanWrapper = new BeanWrapperImpl(enClass);
							subBeanWrapper.setConversionService(conversionService);
							if (parameterValues.length > 0)
								subBeanWrapper.setPropertyValue("id", parameterValues[0]);
							if (collectionType == null) {
								Persistable<?> p = null;
								if (parameterValues.length > 0) {
									p = em.get((Serializable) subBeanWrapper.getPropertyValue("id"));
									if (p == null) {
										Map<String, NaturalId> naturalIds = AnnotationUtils
												.getAnnotatedPropertyNameAndAnnotations(enClass, NaturalId.class);
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
								}
								if (config.isInverseRelation()) {
									// @OneToOne
									Criterion criterion;
									if (operator == CriterionOperator.ISNULL
											|| operator == CriterionOperator.ISNOTNULL) {
										String alias = state.getAliases().get(propertyName);
										if (alias == null) {
											alias = propertyName + "_";
											while (state.getAliases().containsValue(alias))
												alias += "_";
											dc.createAlias(propertyName, alias, JoinType.LEFT_OUTER_JOIN);
											state.getAliases().put(propertyName, alias);
										}
										criterion = operator.operator(alias + ".id",
												subBeanWrapper.getPropertyType("id"));
									} else {
										criterion = operator.operator("id", subBeanWrapper.getPropertyType("id"),
												subBeanWrapper.getPropertyValue("id"));
									}
									if (criterion != null) {
										dc.add(criterion);
										state.getCriteria().add(propertyName);
									}
									continue;
								}
								Criterion criterion = operator.operator(propertyName, type, p);
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
						if (!operator.isEffective(type, parameterValues)) {
							if (operator != CriterionOperator.IN && operator != CriterionOperator.NOTIN)
								continue;
						}
						values = new Object[parameterValues.length];
						for (int n = 0; n < values.length; n++) {
							entityBeanWrapper.setPropertyValue(propertyName, parameterValues[n]);
							Object v = entityBeanWrapper.getPropertyValue(propertyName);
							if (operator == CriterionOperator.CONTAINS || operator == CriterionOperator.NOTCONTAINS) {
								if (v instanceof Collection) {
									Collection<?> coll = (Collection<?>) v;
									v = coll.size() > 0 ? ((Collection<?>) v).iterator().next() : null;
								} else if (v != null && v.getClass().isArray()) {
									v = Array.getLength(v) > 0 ? Array.get(v, 0) : null;
								}
								if (v instanceof Enum) {
									Enum<?> en = (Enum<?>) v;
									v = en.name();
								}
							}
							values[n] = v;
						}
						Criterion criterion = operator.operator(propertyName, config.getGenericPropertyType(), values);
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

	public static <EN extends Persistable<?>> Collection<EN> filter(Collection<EN> result) {
		if (result == null || result.isEmpty())
			return result;
		Class<?> entityClass = result.iterator().next().getClass();
		Map<String, String[]> parameterMap = RequestContext.getRequest().getParameterMap();
		String entityName = StringUtils.uncapitalize(entityClass.getSimpleName());
		Comparator<EN> comparator = (o1, o2) -> 0;
		Predicate<EN> predicate = en -> true;
		try {
			ConversionService conversionService = ApplicationContextUtils.getBean(ConversionService.class);
			BeanWrapperImpl entityBeanWrapper = new BeanWrapperImpl(entityClass.getConstructor().newInstance());
			entityBeanWrapper.setConversionService(conversionService);
			for (String parameterName : parameterMap.keySet()) {
				String propertyName;
				String[] parameterValues;
				String operatorValue;
				if (parameterName.endsWith(CRITERION_ORDER_SUFFIX)) {
					// ordering
					propertyName = parameterName.substring(0, parameterName.length() - CRITERION_ORDER_SUFFIX.length());
					propertyName = trimEntityNamePrefixAndIdSuffix(propertyName, entityName);
					if (entityBeanWrapper.isReadableProperty(propertyName)) {
						final String pname = propertyName;
						Comparator<EN> c = (o1, o2) -> {
							Object value1 = new BeanWrapperImpl(o1).getPropertyValue(pname);
							Object value2 = new BeanWrapperImpl(o2).getPropertyValue(pname);
							return DEFAULT_COMPARATOR.compare(value1, value2);
						};
						comparator = comparator.thenComparing(
								"desc".equalsIgnoreCase(parameterMap.get(parameterName)[0]) ? c.reversed() : c);
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
				propertyName = trimEntityNamePrefixAndIdSuffix(propertyName, entityName);
				if (!entityBeanWrapper.isReadableProperty(propertyName)
						|| !entityBeanWrapper.isWritableProperty(propertyName))
					continue;
				CriterionOperator operator = null;
				if (StringUtils.isNotBlank(operatorValue))
					try {
						operator = CriterionOperator.valueOf(operatorValue.toUpperCase(Locale.ROOT));
					} catch (IllegalArgumentException e) {

					}
				if (operator == null) {
					if (parameterValues.length == 1 && StringUtils.isEmpty(parameterValues[0]))
						continue;
					else
						operator = CriterionOperator.EQ;
				}
				if (parameterValues.length < operator.getParametersSize())
					continue;
				Object[] values = new Object[parameterValues.length];
				for (int i = 0; i < parameterValues.length; i++) {
					entityBeanWrapper.setPropertyValue(propertyName, parameterValues[i]);
					values[i] = entityBeanWrapper.getPropertyValue(propertyName);
					if (i == parameterValues.length - 1) {
						if (values[i] instanceof Date) {
							values[i] = DateUtils.endOfDay((Date) values[i]);
						} else if (values[i] instanceof LocalDateTime) {
							LocalDateTime datetime = ((LocalDateTime) values[i]);
							datetime = datetime.withHour(23).withMinute(59).withSecond(59).withNano(99999);
							values[i] = datetime;
						}
					}
				}
				final String pname = propertyName;
				final Class<?> ptype = entityBeanWrapper.getPropertyType(pname);
				if (Persistable.class.isAssignableFrom(ptype))
					continue;
				final CriterionOperator op = operator;
				Predicate<EN> p = en -> {
					Object value = new BeanWrapperImpl(en).getPropertyValue(pname);
					switch (op) {
					case EQ:
						return value != null && value.equals(values[0]);
					case NEQ:
						return !(value != null && value.equals(values[0]));
					case START:
						if (ptype != String.class)
							return true;
						return value != null && value.toString().startsWith(values[0].toString());
					case NOTSTART:
						if (ptype != String.class)
							return true;
						return !(value != null && value.toString().startsWith(values[0].toString()));
					case END:
						if (ptype != String.class)
							return true;
						return value != null && value.toString().endsWith(values[0].toString());
					case NOTEND:
						if (ptype != String.class)
							return true;
						return !(value != null && value.toString().endsWith(values[0].toString()));
					case INCLUDE:
						if (ptype != String.class)
							return true;
						return value != null && value.toString().contains(values[0].toString());
					case NOTINCLUDE:
						if (ptype != String.class)
							return true;
						return !(value != null && value.toString().contains(values[0].toString()));
					case LT:
						return DEFAULT_COMPARATOR.compare(value, values[0]) < 0;
					case LE:
						return DEFAULT_COMPARATOR.compare(value, values[0]) <= 0;
					case GT:
						return DEFAULT_COMPARATOR.compare(value, values[0]) > 0;
					case GE:
						return DEFAULT_COMPARATOR.compare(value, values[0]) >= 0;
					case BETWEEN:
						return (values[0] == null || DEFAULT_COMPARATOR.compare(value, values[0]) >= 0)
								&& (values[1] == null || DEFAULT_COMPARATOR.compare(value, values[1]) <= 0);
					case NOTBETWEEN:
						return !((values[0] == null || DEFAULT_COMPARATOR.compare(value, values[0]) >= 0)
								&& (values[1] == null || DEFAULT_COMPARATOR.compare(value, values[1]) <= 0));
					case ISNULL:
						return value == null;
					case ISNOTNULL:
						return value != null;
					case ISEMPTY:
						if (ptype != String.class)
							return true;
						return value == null || value.toString().isEmpty();
					case ISNOTEMPTY:
						if (ptype != String.class)
							return true;
						return !(value == null || value.toString().isEmpty());
					case ISTRUE:
						if (ptype != Boolean.class)
							return true;
						return value != null && (Boolean) value;
					case ISFALSE:
						if (ptype != Boolean.class)
							return true;
						return value != null && !(Boolean) value;
					default:
						return true;
					}
				};
				predicate = predicate.and(p);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return result.stream().filter(predicate).sorted(comparator).collect(Collectors.toList());
	}

	private static String trimEntityNamePrefixAndIdSuffix(String propertyName, String entityName) {
		if (propertyName.startsWith(entityName + "."))
			propertyName = propertyName.substring(propertyName.indexOf('.') + 1);
		if (propertyName.endsWith(".id"))
			propertyName = propertyName.substring(0, propertyName.lastIndexOf('.'));
		return propertyName;
	}

	private static String getTopPropertyName(String propertyName) {
		int i = propertyName.indexOf('.');
		return (i > 0) ? propertyName.substring(0, i) : propertyName;
	}

	static Comparator<Object> DEFAULT_COMPARATOR = (value1, value2) -> {
		if (value1 != null && value2 != null) {
			if (value1 instanceof Number) {
				double d = ((Number) value1).doubleValue() - ((Number) value2).doubleValue();
				return d == 0 ? 0 : d > 0 ? 1 : -1;
			} else if (value1 instanceof Date) {
				return ((Date) value1).compareTo((Date) value2);
			} else if (value1 instanceof Enum) {
				return ((Enum<?>) value1).ordinal() - ((Enum<?>) value2).ordinal();
			} else {
				return (value1.toString()).compareTo(value2.toString());
			}
		} else if (value1 == null && value2 != null) {
			return -1;
		} else if (value1 != null && value2 == null) {
			return 1;
		}
		return 0;
	};

}