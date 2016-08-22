package org.ironrhino.core.struts;

import java.beans.PropertyDescriptor;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.Embedded;
import javax.persistence.EmbeddedId;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.MapsId;
import javax.persistence.OneToOne;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;
import javax.persistence.Version;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.annotations.Formula;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.NaturalId;
import org.ironrhino.core.hibernate.CriterionOperator;
import org.ironrhino.core.metadata.FullnameSeperator;
import org.ironrhino.core.metadata.UiConfig;
import org.ironrhino.core.model.Attributable;
import org.ironrhino.core.model.BaseTreeableEntity;
import org.ironrhino.core.model.Persistable;
import org.ironrhino.core.search.elasticsearch.annotations.Index;
import org.ironrhino.core.search.elasticsearch.annotations.SearchableComponent;
import org.ironrhino.core.search.elasticsearch.annotations.SearchableId;
import org.ironrhino.core.search.elasticsearch.annotations.SearchableProperty;
import org.ironrhino.core.struts.AnnotationShadows.HiddenImpl;
import org.ironrhino.core.struts.AnnotationShadows.ReadonlyImpl;
import org.ironrhino.core.struts.AnnotationShadows.UiConfigImpl;
import org.ironrhino.core.util.AppInfo;
import org.ironrhino.core.util.AppInfo.Stage;
import org.ironrhino.core.util.ReflectionUtils;
import org.ironrhino.core.util.ValueThenKeyComparator;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.core.annotation.AnnotationUtils;

public class EntityClassHelper {

	private static Map<Class<?>, Map<String, UiConfigImpl>> uiConfigCache = new ConcurrentHashMap<>(64);
	private static Map<Class<?>, Boolean> idAssignedCache = new ConcurrentHashMap<>(64);

	public static Map<String, UiConfigImpl> getUiConfigs(Class<?> entityClass) {
		Map<String, UiConfigImpl> map = uiConfigCache.get(entityClass);
		if (map == null || AppInfo.getStage() == Stage.DEVELOPMENT) {
			synchronized (entityClass) {
				boolean idAssigned = isIdAssigned(entityClass);
				Map<String, NaturalId> naturalIds = org.ironrhino.core.util.AnnotationUtils
						.getAnnotatedPropertyNameAndAnnotations(entityClass, NaturalId.class);
				Set<String> hides = new HashSet<>();
				map = new HashMap<>();
				PropertyDescriptor[] pds = org.springframework.beans.BeanUtils.getPropertyDescriptors(entityClass);
				List<String> fields = ReflectionUtils.getAllFields(entityClass);
				for (PropertyDescriptor pd : pds) {
					String propertyName = pd.getName();
					if (pd.getReadMethod() == null || pd.getWriteMethod() == null
							&& AnnotationUtils.findAnnotation(pd.getReadMethod(), UiConfig.class) == null)
						continue;
					Method readMethod = pd.getReadMethod();
					Field declaredField;
					try {
						declaredField = readMethod.getDeclaringClass().getDeclaredField(propertyName);
					} catch (NoSuchFieldException e) {
						declaredField = null;
					} catch (SecurityException e) {
						throw new RuntimeException(e);
					}
					if (findAnnotation(readMethod, declaredField, Version.class) != null)
						continue;
					UiConfig uiConfig = findAnnotation(readMethod, declaredField, UiConfig.class);
					if (uiConfig != null && uiConfig.hidden())
						continue;
					if ("new".equals(propertyName) || !idAssigned && "id".equals(propertyName) && uiConfig == null
							|| "class".equals(propertyName) || "fieldHandler".equals(propertyName)
							|| hides.contains(propertyName))
						continue;
					Class<?> collectionClass = null;
					Class<?> elementClass = null;
					if (readMethod.getGenericReturnType() instanceof ParameterizedType) {
						ParameterizedType pt = (ParameterizedType) readMethod.getGenericReturnType();
						Type[] args = pt.getActualTypeArguments();
						if (pt.getRawType() instanceof Class) {
							collectionClass = (Class<?>) pt.getRawType();
							if (!Collection.class.isAssignableFrom(collectionClass))
								collectionClass = null;
						}
						if (collectionClass != null && args.length == 1 && args[0] instanceof Class)
							elementClass = (Class<?>) args[0];
					}

					UiConfigImpl uci = new UiConfigImpl(pd.getName(), pd.getPropertyType(), uiConfig);
					if (uiConfig == null || uiConfig.displayOrder() == Integer.MAX_VALUE) {
						int index = fields.indexOf(pd.getName());
						if (index == -1)
							index = Integer.MAX_VALUE;
						uci.setDisplayOrder(index);
					}
					if (pd.getWriteMethod() == null && StringUtils.isBlank(uci.getInputTemplate())) {
						HiddenImpl hi = new HiddenImpl();
						hi.setValue(true);
						uci.setHiddenInInput(hi);
						ReadonlyImpl ri = new ReadonlyImpl();
						ri.setValue(true);
						uci.setReadonly(ri);
					}

					OneToOne oneToOne = findAnnotation(readMethod, declaredField, OneToOne.class);
					if (oneToOne != null && StringUtils.isNotBlank(oneToOne.mappedBy())) {
						ReadonlyImpl ri = new ReadonlyImpl();
						ri.setValue(true);
						uci.setReadonly(ri);
					}

					Embedded embedded = findAnnotation(readMethod, declaredField, Embedded.class);
					EmbeddedId embeddedId = findAnnotation(readMethod, declaredField, EmbeddedId.class);
					if ((embedded != null || embeddedId != null)
							&& (uiConfig == null || !uiConfig.embeddedAsSingle())) {
						HiddenImpl hi = new HiddenImpl();
						hi.setValue(true);
						uci.setHiddenInList(hi);
						uci.setType("embedded");
						Map<String, UiConfigImpl> map2 = getUiConfigs(readMethod.getReturnType());
						for (UiConfigImpl ui : map2.values()) {
							if (StringUtils.isBlank(ui.getGroup()) && StringUtils.isNotBlank(uci.getGroup()))
								ui.setGroup(uci.getGroup());
							if (embeddedId != null) {
								ui.addCssClass("required");
								ReadonlyImpl ri = new ReadonlyImpl();
								ri.setExpression("!entity.new");
								ui.setReadonly(ri);
							}
						}
						uci.setEmbeddedUiConfigs(map2);
					}

					if (collectionClass != null && elementClass != null
							&& elementClass.getAnnotation(Embeddable.class) != null) {
						HiddenImpl hi = new HiddenImpl();
						hi.setValue(true);
						uci.setHiddenInList(hi);
						uci.setType("collection");
						uci.setEmbeddedUiConfigs(getUiConfigs(elementClass));
					}

					if (idAssigned && propertyName.equals("id"))
						uci.addCssClass("required checkavailable");
					if (Attributable.class.isAssignableFrom(entityClass) && pd.getName().equals("attributes")) {
						uci.setType("attributes");
					}
					if (!uci.getType().equals("dictionary") && collectionClass != null
							&& elementClass == String.class) {
						uci.addCssClass("tags");
						uci.setExcludedFromOrdering(true);
						if (StringUtils.isBlank(uci.getTemplate()))
							uci.setTemplate(
									"<#if value?has_content><#list value as var><span class=\"label\">${var}</span><#if var_has_next> </#if></#list></#if>");
					}
					if (pd.getWriteMethod() == null)
						uci.setExcludedFromCriteria(true);
					if (findAnnotation(readMethod, declaredField, Transient.class) != null) {
						uci.setExcludedFromCriteria(true);
						uci.setExcludedFromLike(true);
						uci.setExcludedFromOrdering(true);
					}
					Lob lob = findAnnotation(readMethod, declaredField, Lob.class);
					if (lob != null) {
						uci.setExcludedFromCriteria(true);
						if (uci.getMaxlength() == 0)
							uci.setMaxlength(2 * 1024 * 1024);
					}
					if (lob != null || uci.getMaxlength() > 255)
						uci.setExcludedFromOrdering(true);

					Column column = findAnnotation(readMethod, declaredField, Column.class);
					Basic basic = findAnnotation(readMethod, declaredField, Basic.class);
					if (column != null && !column.nullable() || basic != null && !basic.optional())
						uci.setRequired(true);
					if (column != null) {
						if (column.length() != 255 && uci.getMaxlength() == 0)
							uci.setMaxlength(column.length());
						if (column.unique())
							uci.setUnique(true);
						if (!column.updatable() && !column.insertable()) {
							HiddenImpl hi = uci.getHiddenInInput();
							if (hi == null || hi.isDefaultOptions()) {
								hi = new HiddenImpl();
								hi.setValue(true);
								uci.setHiddenInInput(hi);
							}
							ReadonlyImpl ri = uci.getReadonly();
							if (ri == null || ri.isDefaultOptions()) {
								ri = new ReadonlyImpl();
								ri.setValue(true);
								uci.setReadonly(ri);
							}
						} else if (column.updatable() && !column.insertable()) {
							ReadonlyImpl ri = uci.getReadonly();
							if (ri == null || ri.isDefaultOptions()) {
								ri = new ReadonlyImpl();
								ri.setExpression("entity.new");
								uci.setReadonly(ri);
							}
						} else if (!column.updatable() && column.insertable()) {
							ReadonlyImpl ri = uci.getReadonly();
							if (ri == null || ri.isDefaultOptions()) {
								ri = new ReadonlyImpl();
								ri.setExpression("!entity.new");
								uci.setReadonly(ri);
							}
						}
					}
					if (findAnnotation(readMethod, declaredField, Formula.class) != null) {
						ReadonlyImpl ri = new ReadonlyImpl();
						ri.setValue(true);
						uci.setReadonly(ri);
					}

					Class<?> returnType = pd.getPropertyType();
					if (returnType.isArray()) {
						Class<?> clazz = returnType.getComponentType();
						if (clazz.isEnum() || clazz == String.class) {
							uci.setMultiple(true);
							returnType = clazz;
							uci.setPropertyType(returnType);
							uci.addCssClass("custom");
							uci.setThCssClass("excludeIfNotEdited");
						}
					}
					if (collectionClass != null && elementClass != null && (elementClass.isEnum()
							|| elementClass == String.class && uci.getType().equals("dictionary"))) {
						uci.setMultiple(true);
						returnType = elementClass;
						uci.setPropertyType(returnType);
						uci.addCssClass("custom");
						uci.setThCssClass("excludeIfNotEdited");
					}
					if (uci.isMultiple()) {
						if (uci.getType().equals("dictionary")) {
							uci.setTemplate(
									"<#if value?has_content><#if displayDictionaryLabel??><#assign templateName><@config.templateName?interpret /></#assign></#if><#list value as var><span class=\"label\"><#if displayDictionaryLabel??><@displayDictionaryLabel dictionaryName=templateName value=var!/><#else>${(var?string)!}</#if></span><#sep> </#list></#if>");
						} else {
							uci.setTemplate(
									"<#if value?has_content><#list value as var><span class=\"label\">${(var?string)!}</span><#sep> </#list></#if>");
						}
					}

					if (returnType.isEnum()) {
						uci.setType("enum");
						try {
							returnType.getMethod("getName");
							uci.setListKey("name");
						} catch (NoSuchMethodException e) {
							uci.setListKey("top");
						}
						try {
							returnType.getMethod("getDisplayName");
							uci.setListValue("displayName");
						} catch (NoSuchMethodException e) {
							uci.setListValue(uci.getListKey());
						}
					} else if (Persistable.class.isAssignableFrom(returnType)) {
						JoinColumn joinColumn = findAnnotation(readMethod, declaredField, JoinColumn.class);
						if (joinColumn != null && !joinColumn.nullable())
							uci.setRequired(true);
						if (joinColumn != null) {
							if (!joinColumn.updatable() && !joinColumn.insertable()) {
								HiddenImpl hi = uci.getHiddenInInput();
								if (hi == null || hi.isDefaultOptions()) {
									hi = new HiddenImpl();
									hi.setValue(true);
									uci.setHiddenInInput(hi);
								}
								ReadonlyImpl ri = uci.getReadonly();
								if (ri == null || ri.isDefaultOptions()) {
									ri = new ReadonlyImpl();
									ri.setValue(true);
									uci.setReadonly(ri);
								}
							} else if (joinColumn.updatable() && !joinColumn.insertable()) {
								ReadonlyImpl ri = uci.getReadonly();
								if (ri == null || ri.isDefaultOptions()) {
									ri = new ReadonlyImpl();
									ri.setExpression("entity.new");
									uci.setReadonly(ri);
								}
							} else if (!joinColumn.updatable() && joinColumn.insertable()) {
								ReadonlyImpl ri = uci.getReadonly();
								if (ri == null || ri.isDefaultOptions()) {
									ri = new ReadonlyImpl();
									ri.setExpression("!entity.new");
									uci.setReadonly(ri);
								}
							}
						}
						ManyToOne manyToOne = findAnnotation(readMethod, declaredField, ManyToOne.class);
						if (manyToOne != null && !manyToOne.optional())
							uci.setRequired(true);
						if (uci.getType().equals(UiConfig.DEFAULT_TYPE))
							uci.setType("listpick");
						uci.setExcludeIfNotEdited(true);
						if (StringUtils.isBlank(uci.getPickUrl())) {
							uci.setPickUrl(getPickUrl(returnType));
						}
						if (StringUtils.isBlank(uci.getListTemplate()) && !uci.isSuppressViewLink()) {
							String url = AutoConfigPackageProvider.getEntityUrl(returnType);
							if (url == null)
								url = new StringBuilder("/")
										.append(StringUtils.uncapitalize(returnType.getSimpleName())).toString();
							uci.setListTemplate("<#if value?has_content&&value.id?has_content><a href=\"" + url
									+ "/view/${value.id}\" class=\"view\" rel=\"richtable\" title=\"${action.getText('view')}\">${value?html}</a></#if>");
						}
					} else if (returnType == Integer.TYPE || returnType == Short.TYPE || returnType == Long.TYPE
							|| returnType == Double.TYPE || returnType == Float.TYPE
							|| Number.class.isAssignableFrom(returnType)) {
						if (returnType == Integer.TYPE || returnType == Integer.class || returnType == Short.TYPE
								|| returnType == Short.class) {
							uci.setInputType("number");
							uci.addCssClass("integer");

						} else if (returnType == Long.TYPE || returnType == Long.class) {
							uci.setInputType("number");
							uci.addCssClass("long");
						} else if (returnType == Double.TYPE || returnType == Double.class || returnType == Float.TYPE
								|| returnType == Float.class || returnType == BigDecimal.class) {
							uci.setInputType("number");
							uci.addCssClass("double");
							if (returnType == BigDecimal.class) {
								int scale = column != null ? column.scale() : 2;
								if (scale == 0)
									scale = 2;
								StringBuilder step = new StringBuilder(scale + 2);
								step.append("0.");
								for (int i = 0; i < scale - 1; i++)
									step.append("0");
								step.append("1");
								uci.getInternalDynamicAttributes().put("step", step.toString());
								uci.getInternalDynamicAttributes().put("data-scale", String.valueOf(scale));
								if (StringUtils.isBlank(uci.getTemplate()) && returnType == BigDecimal.class) {
									StringBuilder template = new StringBuilder(scale + 40);
									template.append("<#if value?is_number>${value?string('#,##0.");
									for (int i = 0; i < scale; i++)
										template.append("0");
									template.append("')}<#else>${value!}</#if>");
									uci.setTemplate(template.toString());
								}
							}
						}
						Set<String> cssClasses = uci.getCssClasses();
						if (cssClasses.contains("double") && !uci.getInternalDynamicAttributes().containsKey("step"))
							uci.getInternalDynamicAttributes().put("step", "0.01");
						if (cssClasses.contains("positive") && !uci.getInternalDynamicAttributes().containsKey("min")) {
							uci.getInternalDynamicAttributes().put("min", "1");
							if (cssClasses.contains("double"))
								uci.getInternalDynamicAttributes().put("min", "0.01");
							if (cssClasses.contains("zero"))
								uci.getInternalDynamicAttributes().put("min", "0");
						}
					} else if (Date.class.isAssignableFrom(returnType)) {
						Temporal temporal = findAnnotation(readMethod, declaredField, Temporal.class);
						String temporalType = "date";
						if (temporal != null)
							if (temporal.value() == TemporalType.TIMESTAMP)
								temporalType = "datetime";
							else if (temporal.value() == TemporalType.TIME)
								temporalType = "time";
						uci.addCssClass(temporalType);
						// uci.setInputType(temporalType);
						if (StringUtils.isBlank(uci.getCellEdit()))
							uci.setCellEdit("click," + temporalType);
					} else if (String.class == returnType && pd.getName().toLowerCase().contains("email")
							&& !pd.getName().contains("Password")) {
						uci.setInputType("email");
						uci.addCssClass("email");
					} else if (returnType == Boolean.TYPE || returnType == Boolean.class) {
						uci.setType("checkbox");
					}

					SearchableProperty searchableProperty = findAnnotation(readMethod, declaredField,
							SearchableProperty.class);
					SearchableId searchableId = findAnnotation(readMethod, declaredField, SearchableId.class);
					SearchableComponent searchableComponent = findAnnotation(readMethod, declaredField,
							SearchableComponent.class);
					if (searchableProperty != null || searchableId != null || searchableComponent != null) {
						uci.setSearchable(true);
						if (searchableId != null
								|| searchableProperty != null && searchableProperty.index() == Index.NOT_ANALYZED)
							uci.setExactMatch(true);
						if (searchableComponent != null) {
							String s = searchableComponent.nestSearchableProperties();
							if (StringUtils.isNotBlank(s)) {
								Set<String> nestSearchableProperties = new LinkedHashSet<>();
								nestSearchableProperties.addAll(Arrays.asList(s.split("\\s*,\\s*")));
								uci.setNestSearchableProperties(nestSearchableProperties);
							}
						}
					}
					if (naturalIds.containsKey(pd.getName())) {
						uci.setRequired(true);
						// if (naturalIds.size() == 1)
						uci.addCssClass("checkavailable");
						if (naturalIds.size() > 1) {
							if (uci.getPropertyType() != null
									&& Persistable.class.isAssignableFrom(uci.getPropertyType())) {
								List<String> list = new ArrayList<>(naturalIds.size() - 1);
								for (String name : naturalIds.keySet())
									if (!name.equals(pd.getName()))
										list.add(StringUtils.uncapitalize(entityClass.getSimpleName()) + "." + name);
								uci.getInternalDynamicAttributes().put("data-checkwith", StringUtils.join(list, ","));
							}
						}
					}
					if (uci.getType().equals("textarea") && uci.getMaxlength() > 0)
						uci.getInternalDynamicAttributes().put("maxlength", String.valueOf(uci.getMaxlength()));
					if (StringUtils.isNotBlank(uci.getGroup())) {
						uci.getInternalDynamicAttributes().put("data-group", I18N.getText(uci.getGroup()));
					}
					map.put(propertyName, uci);
				}
				List<Map.Entry<String, UiConfigImpl>> list = new ArrayList<>(map.entrySet());
				Collections.sort(list, comparator);
				Map<String, UiConfigImpl> sortedMap = new LinkedHashMap<>();
				for (Map.Entry<String, UiConfigImpl> entry : list)
					sortedMap.put(entry.getKey(), entry.getValue());
				map = sortedMap;
				uiConfigCache.put(entityClass, Collections.unmodifiableMap(map));
			}
		}
		return map;
	}

	public static Map<String, UiConfigImpl> filterPropertyNamesInCriteria(Map<String, UiConfigImpl> uiConfigs) {
		Map<String, UiConfigImpl> propertyNamesInCriterion = new LinkedHashMap<>();
		for (Map.Entry<String, UiConfigImpl> entry : uiConfigs.entrySet()) {
			if (!entry.getValue().isExcludedFromCriteria() && !entry.getKey().endsWith("AsString")
					&& !CriterionOperator.getSupportedOperators(entry.getValue().getPropertyType()).isEmpty()) {
				UiConfigImpl config = new UiConfigImpl();
				BeanUtils.copyProperties(entry.getValue(), config);
				Set<String> cssClasses = new LinkedHashSet<>();
				if (config.getCssClasses().contains("date")) {
					cssClasses.add("date");
				} else if (config.getCssClasses().contains("datetime")) {
					cssClasses.add("datetime");
				} else if (config.getCssClasses().contains("time")) {
					cssClasses.add("time");
				}
				config.setCssClasses(cssClasses);
				if ("email".equals(config.getInputType()))
					config.setInputType("text");
				propertyNamesInCriterion.put(entry.getKey(), config);
			}
		}
		return propertyNamesInCriterion;
	}

	public static Map<String, UiConfigImpl> getPropertyNamesInCriteria(Class<? extends Persistable<?>> entityClass) {
		return filterPropertyNamesInCriteria(getUiConfigs(entityClass));
	}

	public static String getPickUrl(Class<?> entityClass) {
		String url = AutoConfigPackageProvider.getEntityUrl(entityClass);
		StringBuilder sb = url != null ? new StringBuilder(url)
				: new StringBuilder("/").append(StringUtils.uncapitalize(entityClass.getSimpleName()));
		sb.append("/pick");
		Set<String> columns = new LinkedHashSet<>();
		BeanWrapperImpl bw = new BeanWrapperImpl(entityClass);
		if (BaseTreeableEntity.class.isAssignableFrom(entityClass)) {
			FullnameSeperator fs = entityClass.getAnnotation(FullnameSeperator.class);
			if (fs != null && !fs.independent() && bw.isReadableProperty("fullname"))
				columns.add("fullname");
			else
				columns.add("name");
		} else {
			if (bw.isReadableProperty("name"))
				columns.add("name");
			if (bw.isReadableProperty("fullname"))
				columns.add("fullname");
		}
		columns.addAll(org.ironrhino.core.util.AnnotationUtils
				.getAnnotatedPropertyNameAndAnnotations(entityClass, NaturalId.class).keySet());
		for (PropertyDescriptor pd : bw.getPropertyDescriptors()) {
			if (pd.getReadMethod() == null)
				continue;
			UiConfig uic = AnnotationUtils.findAnnotation(pd.getReadMethod(), UiConfig.class);
			if (uic == null) {
				try {
					Field f = pd.getReadMethod().getDeclaringClass().getDeclaredField(pd.getName());
					if (f != null)
						uic = f.getAnnotation(UiConfig.class);
				} catch (Exception e) {

				}
			}
			if (uic != null && uic.shownInPick())
				columns.add(pd.getName());
		}
		if (!columns.isEmpty()) {
			sb.append("?columns=" + StringUtils.join(columns, ','));
		}
		return sb.toString();
	}

	public static boolean isIdAssigned(Class<?> entityClass) {
		Boolean b = idAssignedCache.get(entityClass);
		if (b == null || AppInfo.getStage() == Stage.DEVELOPMENT) {
			b = _isIdAssigned(entityClass);
			idAssignedCache.put(entityClass, b);
		}
		return b;
	}

	private static boolean _isIdAssigned(Class<?> entityClass) {
		if (entityClass.isInterface())
			return false;
		Map<String, MapsId> map = org.ironrhino.core.util.AnnotationUtils
				.getAnnotatedPropertyNameAndAnnotations(entityClass, MapsId.class);
		if (!map.isEmpty())
			return false;
		AnnotatedElement ae = null;
		try {
			Method m = entityClass.getMethod("getId");
			if (AnnotationUtils.findAnnotation(m, Id.class) != null
					|| AnnotationUtils.findAnnotation(m, EmbeddedId.class) != null) {
				ae = m;
			} else {
				Class<?> clz = entityClass;
				loop: while (clz != Object.class) {
					Field[] fields = clz.getDeclaredFields();
					for (Field f : fields) {
						if (f.getAnnotation(Id.class) != null || f.getAnnotation(EmbeddedId.class) != null) {
							ae = f;
							break loop;
						}
					}
					clz = clz.getSuperclass();
				}
			}
		} catch (Exception e) {
			return false;
		}
		if (ae == null)
			return false;
		GeneratedValue generatedValue = AnnotationUtils.findAnnotation(ae, GeneratedValue.class);
		GenericGenerator genericGenerator = AnnotationUtils.findAnnotation(ae, GenericGenerator.class);
		return generatedValue == null || genericGenerator != null && "assigned".equals(genericGenerator.strategy());
	}

	private static <T extends Annotation> T findAnnotation(Method readMethod, Field declaredField,
			Class<T> annotationClass) {
		T annotation = AnnotationUtils.findAnnotation(readMethod, annotationClass);
		if (annotation == null && declaredField != null)
			annotation = declaredField.getAnnotation(annotationClass);
		return annotation;
	}

	private static ValueThenKeyComparator<String, UiConfigImpl> comparator = new ValueThenKeyComparator<String, UiConfigImpl>() {
		@Override
		protected int compareValue(UiConfigImpl a, UiConfigImpl b) {
			return a.getDisplayOrder() - b.getDisplayOrder();
		}
	};

}
