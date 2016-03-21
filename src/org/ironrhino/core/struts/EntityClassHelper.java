package org.ironrhino.core.struts;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
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
import javax.persistence.GeneratedValue;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;
import javax.persistence.Version;

import org.apache.commons.lang3.StringUtils;
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

	private static Map<Class<?>, Map<String, UiConfigImpl>> cache = new ConcurrentHashMap<>(64);

	public static Map<String, UiConfigImpl> getUiConfigs(Class<?> entityClass) {
		Map<String, UiConfigImpl> map = cache.get(entityClass);
		if (map == null || AppInfo.getStage() == Stage.DEVELOPMENT) {
			synchronized (entityClass) {
				GeneratedValue generatedValue = org.ironrhino.core.util.AnnotationUtils
						.getAnnotatedPropertyNameAndAnnotations(entityClass, GeneratedValue.class).get("id");
				GenericGenerator genericGenerator = org.ironrhino.core.util.AnnotationUtils
						.getAnnotatedPropertyNameAndAnnotations(entityClass, GenericGenerator.class).get("id");
				boolean idAssigned = generatedValue == null
						|| genericGenerator != null && "assigned".equals(genericGenerator.strategy());
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
					Class<?> declaredClass = pd.getReadMethod().getDeclaringClass();
					Version version = AnnotationUtils.findAnnotation(pd.getReadMethod(), Version.class);
					if (version == null)
						try {
							Field f = declaredClass.getDeclaredField(propertyName);
							if (f != null)
								version = f.getAnnotation(Version.class);
						} catch (Exception e) {
						}
					if (version != null)
						continue;
					Transient trans = AnnotationUtils.findAnnotation(pd.getReadMethod(), Transient.class);
					if (trans == null)
						try {
							Field f = declaredClass.getDeclaredField(propertyName);
							if (f != null)
								trans = f.getAnnotation(Transient.class);
						} catch (Exception e) {
						}
					UiConfig uiConfig = AnnotationUtils.findAnnotation(pd.getReadMethod(), UiConfig.class);
					if (uiConfig == null)
						try {
							Field f = declaredClass.getDeclaredField(propertyName);
							if (f != null)
								uiConfig = f.getAnnotation(UiConfig.class);
						} catch (Exception e) {
						}

					if (uiConfig != null && uiConfig.hidden())
						continue;
					if ("new".equals(propertyName) || !idAssigned && "id".equals(propertyName)
							|| "class".equals(propertyName) || "fieldHandler".equals(propertyName)
							|| pd.getReadMethod() == null || hides.contains(propertyName))
						continue;
					Column columnannotation = AnnotationUtils.findAnnotation(pd.getReadMethod(), Column.class);
					if (columnannotation == null)
						try {
							Field f = declaredClass.getDeclaredField(propertyName);
							if (f != null)
								columnannotation = f.getAnnotation(Column.class);
						} catch (Exception e) {
						}
					Basic basic = AnnotationUtils.findAnnotation(pd.getReadMethod(), Basic.class);
					if (basic == null)
						try {
							Field f = declaredClass.getDeclaredField(propertyName);
							if (f != null)
								basic = f.getAnnotation(Basic.class);
						} catch (Exception e) {
						}
					Lob lob = AnnotationUtils.findAnnotation(pd.getReadMethod(), Lob.class);
					if (lob == null)
						try {
							Field f = declaredClass.getDeclaredField(propertyName);
							if (f != null)
								lob = f.getAnnotation(Lob.class);
						} catch (Exception e) {
						}
					OneToOne oneToOne = AnnotationUtils.findAnnotation(pd.getReadMethod(), OneToOne.class);
					if (oneToOne == null)
						try {
							Field f = declaredClass.getDeclaredField(propertyName);
							if (f != null)
								oneToOne = f.getAnnotation(OneToOne.class);
						} catch (Exception e) {
						}
					Embedded embedded = AnnotationUtils.findAnnotation(pd.getReadMethod(), Embedded.class);
					Class<?> embeddedClass = null;
					if (embedded == null) {
						try {
							Field f = declaredClass.getDeclaredField(propertyName);
							if (f != null)
								embedded = f.getAnnotation(Embedded.class);
							embeddedClass = f.getType();
						} catch (Exception e) {
						}
					} else {
						embeddedClass = pd.getReadMethod().getReturnType();
					}
					if (uiConfig != null && uiConfig.embeddedAsSingle()) {
						embedded = null;
						embeddedClass = null;
					}

					Class<?> elementClass = null;
					if (Collection.class.isAssignableFrom(pd.getReadMethod().getReturnType())) {
						elementClass = ReflectionUtils.getGenericClass(pd.getReadMethod().getGenericReturnType(), 0);
						if (elementClass != null && elementClass.getAnnotation(Embeddable.class) == null)
							elementClass = null;
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
					if (oneToOne != null && StringUtils.isNotBlank(oneToOne.mappedBy())) {
						ReadonlyImpl ri = new ReadonlyImpl();
						ri.setValue(true);
						uci.setReadonly(ri);
					}
					if (embedded != null) {
						HiddenImpl hi = new HiddenImpl();
						hi.setValue(true);
						uci.setHiddenInList(hi);
						uci.setType("embedded");
						Map<String, UiConfigImpl> map2 = getUiConfigs(embeddedClass);
						for (UiConfigImpl ui : map2.values()) {
							if (StringUtils.isBlank(ui.getGroup()) && StringUtils.isNotBlank(uci.getGroup()))
								ui.setGroup(uci.getGroup());
						}
						uci.setEmbeddedUiConfigs(map2);
					}
					if (elementClass != null) {
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
					if (pd.getWriteMethod() == null)
						uci.setExcludedFromCriteria(true);
					if (trans != null) {
						uci.setExcludedFromCriteria(true);
						uci.setExcludedFromLike(true);
						uci.setExcludedFromOrdering(true);
					}
					if (lob != null) {
						uci.setExcludedFromCriteria(true);
						if (uci.getMaxlength() == 0)
							uci.setMaxlength(2 * 1024 * 1024);
					}
					if (columnannotation != null && !columnannotation.nullable() || basic != null && !basic.optional())
						uci.setRequired(true);
					if (columnannotation != null && columnannotation.length() != 255 && uci.getMaxlength() == 0)
						uci.setMaxlength(columnannotation.length());
					if (columnannotation != null) {
						if (!columnannotation.updatable() && !columnannotation.insertable()) {
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
						} else if (columnannotation.updatable() && !columnannotation.insertable()) {
							ReadonlyImpl ri = uci.getReadonly();
							if (ri == null || ri.isDefaultOptions()) {
								ri = new ReadonlyImpl();
								ri.setExpression("entity.new");
								uci.setReadonly(ri);
							}
						} else if (!columnannotation.updatable() && columnannotation.insertable()) {
							ReadonlyImpl ri = uci.getReadonly();
							if (ri == null || ri.isDefaultOptions()) {
								ri = new ReadonlyImpl();
								ri.setExpression("!entity.new");
								uci.setReadonly(ri);
							}
						}
					}
					if (lob != null || uci.getMaxlength() > 255)
						uci.setExcludedFromOrdering(true);
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
					if (Collection.class.isAssignableFrom(returnType)) {
						Type type = pd.getReadMethod().getGenericReturnType();
						if (type instanceof ParameterizedType) {
							type = ((ParameterizedType) type).getActualTypeArguments()[0];
							if (type instanceof Class) {
								Class<?> clazz = (Class<?>) type;
								if (clazz.isEnum() || clazz == String.class) {
									uci.setMultiple(true);
									returnType = clazz;
									uci.setPropertyType(returnType);
									uci.addCssClass("custom");
									uci.setThCssClass("excludeIfNotEdited");
								}
							}
						}
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
						JoinColumn joincolumnannotation = AnnotationUtils.findAnnotation(pd.getReadMethod(),
								JoinColumn.class);
						if (joincolumnannotation == null)
							try {
								Field f = declaredClass.getDeclaredField(propertyName);
								if (f != null)
									joincolumnannotation = f.getAnnotation(JoinColumn.class);
							} catch (Exception e) {
							}
						if (joincolumnannotation != null && !joincolumnannotation.nullable())
							uci.setRequired(true);
						if (joincolumnannotation != null) {
							if (!joincolumnannotation.updatable() && !joincolumnannotation.insertable()) {
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
							} else if (joincolumnannotation.updatable() && !joincolumnannotation.insertable()) {
								ReadonlyImpl ri = uci.getReadonly();
								if (ri == null || ri.isDefaultOptions()) {
									ri = new ReadonlyImpl();
									ri.setExpression("entity.new");
									uci.setReadonly(ri);
								}
							} else if (!joincolumnannotation.updatable() && joincolumnannotation.insertable()) {
								ReadonlyImpl ri = uci.getReadonly();
								if (ri == null || ri.isDefaultOptions()) {
									ri = new ReadonlyImpl();
									ri.setExpression("!entity.new");
									uci.setReadonly(ri);
								}
							}
						}
						ManyToOne manyToOne = AnnotationUtils.findAnnotation(pd.getReadMethod(), ManyToOne.class);
						if (manyToOne == null)
							try {
								Field f = declaredClass.getDeclaredField(propertyName);
								if (f != null)
									manyToOne = f.getAnnotation(ManyToOne.class);
							} catch (Exception e) {
							}
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
								int scale = columnannotation != null ? columnannotation.scale() : 2;
								if (scale == 0)
									scale = 2;
								StringBuilder step = new StringBuilder(scale + 2);
								step.append("0.");
								for (int i = 0; i < scale - 1; i++)
									step.append("0");
								step.append("1");
								uci.getDynamicAttributes().put("step", step.toString());
								uci.getDynamicAttributes().put("data-scale", String.valueOf(scale));
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
						if (cssClasses.contains("double") && !uci.getDynamicAttributes().containsKey("step"))
							uci.getDynamicAttributes().put("step", "0.01");
						if (cssClasses.contains("positive") && !uci.getDynamicAttributes().containsKey("min")) {
							uci.getDynamicAttributes().put("min", "1");
							if (cssClasses.contains("double"))
								uci.getDynamicAttributes().put("min", "0.01");
							if (cssClasses.contains("zero"))
								uci.getDynamicAttributes().put("min", "0");
						}
					} else if (Date.class.isAssignableFrom(returnType)) {
						Temporal temporal = AnnotationUtils.findAnnotation(pd.getReadMethod(), Temporal.class);
						if (temporal == null)
							try {
								Field f = declaredClass.getDeclaredField(propertyName);
								if (f != null)
									temporal = f.getAnnotation(Temporal.class);
							} catch (Exception e) {
							}
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
					if (columnannotation != null && columnannotation.unique())
						uci.setUnique(true);
					SearchableProperty searchableProperty = AnnotationUtils.findAnnotation(pd.getReadMethod(),
							SearchableProperty.class);
					if (searchableProperty == null)
						try {
							Field f = declaredClass.getDeclaredField(propertyName);
							if (f != null)
								searchableProperty = f.getAnnotation(SearchableProperty.class);
						} catch (Exception e) {
						}
					SearchableId searchableId = AnnotationUtils.findAnnotation(pd.getReadMethod(), SearchableId.class);
					if (searchableId == null)
						try {
							Field f = declaredClass.getDeclaredField(propertyName);
							if (f != null)
								searchableId = f.getAnnotation(SearchableId.class);
						} catch (Exception e) {
						}
					SearchableComponent searchableComponent = AnnotationUtils.findAnnotation(pd.getReadMethod(),
							SearchableComponent.class);
					if (searchableComponent == null)
						try {
							Field f = declaredClass.getDeclaredField(propertyName);
							if (f != null)
								searchableComponent = f.getAnnotation(SearchableComponent.class);
						} catch (Exception e) {
						}
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
								uci.getDynamicAttributes().put("data-checkwith", StringUtils.join(list, ","));
							}
						}
					}
					map.put(propertyName, uci);
				}
				List<Map.Entry<String, UiConfigImpl>> list = new ArrayList<>(map.entrySet());
				Collections.sort(list, comparator);
				Map<String, UiConfigImpl> sortedMap = new LinkedHashMap<>();
				for (Map.Entry<String, UiConfigImpl> entry : list)
					sortedMap.put(entry.getKey(), entry.getValue());
				map = sortedMap;
				cache.put(entityClass, Collections.unmodifiableMap(map));
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

	private static ValueThenKeyComparator<String, UiConfigImpl> comparator = new ValueThenKeyComparator<String, UiConfigImpl>() {
		@Override
		protected int compareValue(UiConfigImpl a, UiConfigImpl b) {
			return a.getDisplayOrder() - b.getDisplayOrder();
		}
	};

}
