package org.ironrhino.core.struts;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
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
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
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
import org.ironrhino.core.search.elasticsearch.annotations.SearchableComponent;
import org.ironrhino.core.search.elasticsearch.annotations.SearchableId;
import org.ironrhino.core.search.elasticsearch.annotations.SearchableProperty;
import org.ironrhino.core.struts.AnnotationShadows.HiddenImpl;
import org.ironrhino.core.struts.AnnotationShadows.UiConfigImpl;
import org.ironrhino.core.util.AnnotationUtils;
import org.ironrhino.core.util.AppInfo;
import org.ironrhino.core.util.AppInfo.Stage;
import org.ironrhino.core.util.ReflectionUtils;
import org.ironrhino.core.util.ValueThenKeyComparator;
import org.springframework.beans.BeanWrapperImpl;

public class EntityClassHelper {

	private static Map<Class<?>, Map<String, UiConfigImpl>> cache = new ConcurrentHashMap<Class<?>, Map<String, UiConfigImpl>>(
			64);

	public static Map<String, UiConfigImpl> getUiConfigs(Class<?> entityClass) {
		Map<String, UiConfigImpl> map = cache.get(entityClass);
		if (map == null || AppInfo.getStage() == Stage.DEVELOPMENT) {
			GenericGenerator genericGenerator = AnnotationUtils
					.getAnnotatedPropertyNameAndAnnotations(entityClass,
							GenericGenerator.class).get("id");
			boolean idAssigned = genericGenerator != null
					&& "assigned".equals(genericGenerator.strategy());
			Map<String, NaturalId> naturalIds = AnnotationUtils
					.getAnnotatedPropertyNameAndAnnotations(entityClass,
							NaturalId.class);
			Set<String> hides = new HashSet<String>();
			map = new HashMap<String, UiConfigImpl>();
			PropertyDescriptor[] pds = org.springframework.beans.BeanUtils
					.getPropertyDescriptors(entityClass);
			for (PropertyDescriptor pd : pds) {
				String propertyName = pd.getName();
				if (pd.getReadMethod() == null
						|| pd.getWriteMethod() == null
						&& pd.getReadMethod().getAnnotation(UiConfig.class) == null)
					continue;
				Class<?> declaredClass = pd.getReadMethod().getDeclaringClass();
				Version version = pd.getReadMethod().getAnnotation(
						Version.class);
				if (version == null)
					try {
						Field f = declaredClass.getDeclaredField(propertyName);
						if (f != null)
							version = f.getAnnotation(Version.class);
					} catch (Exception e) {
					}
				if (version != null)
					continue;
				Transient trans = pd.getReadMethod().getAnnotation(
						Transient.class);
				if (trans == null)
					try {
						Field f = declaredClass.getDeclaredField(propertyName);
						if (f != null)
							trans = f.getAnnotation(Transient.class);
					} catch (Exception e) {
					}
				UiConfig uiConfig = pd.getReadMethod().getAnnotation(
						UiConfig.class);
				if (uiConfig == null)
					try {
						Field f = declaredClass.getDeclaredField(propertyName);
						if (f != null)
							uiConfig = f.getAnnotation(UiConfig.class);
					} catch (Exception e) {
					}

				if (uiConfig != null && uiConfig.hidden())
					continue;
				if ("new".equals(propertyName) || !idAssigned
						&& "id".equals(propertyName)
						|| "class".equals(propertyName)
						|| "fieldHandler".equals(propertyName)
						|| pd.getReadMethod() == null
						|| hides.contains(propertyName))
					continue;
				Column columnannotation = pd.getReadMethod().getAnnotation(
						Column.class);
				if (columnannotation == null)
					try {
						Field f = declaredClass.getDeclaredField(propertyName);
						if (f != null)
							columnannotation = f.getAnnotation(Column.class);
					} catch (Exception e) {
					}
				Basic basic = pd.getReadMethod().getAnnotation(Basic.class);
				if (basic == null)
					try {
						Field f = declaredClass.getDeclaredField(propertyName);
						if (f != null)
							basic = f.getAnnotation(Basic.class);
					} catch (Exception e) {
					}
				Lob lob = pd.getReadMethod().getAnnotation(Lob.class);
				if (lob == null)
					try {
						Field f = declaredClass.getDeclaredField(propertyName);
						if (f != null)
							lob = f.getAnnotation(Lob.class);
					} catch (Exception e) {
					}
				Embedded embedded = pd.getReadMethod().getAnnotation(
						Embedded.class);
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

				Class<?> elementClass = null;
				if (Collection.class.isAssignableFrom(pd.getReadMethod()
						.getReturnType())) {
					elementClass = ReflectionUtils.getGenericClass(pd
							.getReadMethod().getGenericReturnType(), 0);
					if (elementClass != null
							&& elementClass.getAnnotation(Embeddable.class) == null)
						elementClass = null;
				}
				UiConfigImpl uci = new UiConfigImpl(pd.getName(),
						pd.getPropertyType(), uiConfig);
				if (pd.getWriteMethod() == null) {
					HiddenImpl hi = new HiddenImpl();
					hi.setValue(true);
					uci.setHiddenInInput(hi);
				}
				if (embedded != null) {
					HiddenImpl hi = new HiddenImpl();
					hi.setValue(true);
					uci.setHiddenInList(hi);
					uci.setType("embedded");
					Map<String, UiConfigImpl> map2 = getUiConfigs(embeddedClass);
					for (UiConfigImpl ui : map2.values()) {
						if (StringUtils.isBlank(ui.getGroup())
								&& StringUtils.isNoneBlank(uci.getGroup()))
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
				if (Attributable.class.isAssignableFrom(entityClass)
						&& pd.getName().equals("attributes")) {
					uci.setType("attributes");
				}
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
				if (columnannotation != null && !columnannotation.nullable()
						|| basic != null && !basic.optional())
					uci.setRequired(true);
				if (columnannotation != null
						&& columnannotation.length() != 255
						&& uci.getMaxlength() == 0)
					uci.setMaxlength(columnannotation.length());
				if (lob != null || uci.getMaxlength() > 255)
					uci.setExcludedFromOrdering(true);
				Class<?> returnType = pd.getPropertyType();
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
					JoinColumn joincolumnannotation = pd.getReadMethod()
							.getAnnotation(JoinColumn.class);
					if (joincolumnannotation == null)
						try {
							Field f = declaredClass
									.getDeclaredField(propertyName);
							if (f != null)
								joincolumnannotation = f
										.getAnnotation(JoinColumn.class);
						} catch (Exception e) {
						}
					if (joincolumnannotation != null
							&& !joincolumnannotation.nullable())
						uci.setRequired(true);
					ManyToOne manyToOne = pd.getReadMethod().getAnnotation(
							ManyToOne.class);
					if (manyToOne == null)
						try {
							Field f = declaredClass
									.getDeclaredField(propertyName);
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
				} else if (returnType == Integer.TYPE
						|| returnType == Short.TYPE || returnType == Long.TYPE
						|| returnType == Double.TYPE
						|| returnType == Float.TYPE
						|| Number.class.isAssignableFrom(returnType)) {
					if (returnType == Integer.TYPE
							|| returnType == Integer.class
							|| returnType == Short.TYPE
							|| returnType == Short.class) {
						uci.setInputType("number");
						uci.addCssClass("integer");

					} else if (returnType == Long.TYPE
							|| returnType == Long.class) {
						uci.setInputType("number");
						uci.addCssClass("long");
					} else if (returnType == Double.TYPE
							|| returnType == Double.class
							|| returnType == Float.TYPE
							|| returnType == Float.class
							|| returnType == BigDecimal.class) {
						uci.setInputType("number");
						uci.addCssClass("double");
						if (returnType == BigDecimal.class) {
							int scale = columnannotation != null ? columnannotation
									.scale() : 2;
							if (scale == 0)
								scale = 2;
							StringBuilder step = new StringBuilder(scale + 2);
							step.append("0.");
							for (int i = 0; i < scale - 1; i++)
								step.append("0");
							step.append("1");
							uci.getDynamicAttributes().put("step",
									step.toString());
							uci.getDynamicAttributes().put("data-scale",
									String.valueOf(scale));
							if (StringUtils.isBlank(uci.getTemplate())
									&& returnType == BigDecimal.class) {
								StringBuilder template = new StringBuilder(
										scale + 40);
								template.append("<#if value?is_number>${value?string('#,##0.");
								for (int i = 0; i < scale; i++)
									template.append("0");
								template.append("')}<#else>${value!}</#if>");
								uci.setTemplate(template.toString());
							}
						}
					}
					Set<String> cssClasses = uci.getCssClasses();
					if (cssClasses.contains("double")
							&& !uci.getDynamicAttributes().containsKey("step"))
						uci.getDynamicAttributes().put("step", "0.01");
					if (cssClasses.contains("positive")
							&& !uci.getDynamicAttributes().containsKey("min")) {
						uci.getDynamicAttributes().put("min", "1");
						if (cssClasses.contains("double"))
							uci.getDynamicAttributes().put("min", "0.01");
						if (cssClasses.contains("zero"))
							uci.getDynamicAttributes().put("min", "0");
					}
				} else if (Date.class.isAssignableFrom(returnType)) {
					Temporal temporal = pd.getReadMethod().getAnnotation(
							Temporal.class);
					if (temporal == null)
						try {
							Field f = declaredClass
									.getDeclaredField(propertyName);
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
				} else if (String.class == returnType
						&& pd.getName().toLowerCase().contains("email")
						&& !pd.getName().contains("Password")) {
					uci.setInputType("email");
					uci.addCssClass("email");
				} else if (returnType == Boolean.TYPE
						|| returnType == Boolean.class) {
					uci.setType("checkbox");
				}
				if (columnannotation != null && columnannotation.unique())
					uci.setUnique(true);
				SearchableProperty searchableProperty = pd.getReadMethod()
						.getAnnotation(SearchableProperty.class);
				if (searchableProperty == null)
					try {
						Field f = declaredClass.getDeclaredField(propertyName);
						if (f != null)
							searchableProperty = f
									.getAnnotation(SearchableProperty.class);
					} catch (Exception e) {
					}
				SearchableId searchableId = pd.getReadMethod().getAnnotation(
						SearchableId.class);
				if (searchableId == null)
					try {
						Field f = declaredClass.getDeclaredField(propertyName);
						if (f != null)
							searchableId = f.getAnnotation(SearchableId.class);
					} catch (Exception e) {
					}
				SearchableComponent searchableComponent = pd.getReadMethod()
						.getAnnotation(SearchableComponent.class);
				if (searchableComponent == null)
					try {
						Field f = declaredClass.getDeclaredField(propertyName);
						if (f != null)
							searchableComponent = f
									.getAnnotation(SearchableComponent.class);
					} catch (Exception e) {
					}
				if (searchableProperty != null || searchableId != null
						|| searchableComponent != null) {
					uci.setSearchable(true);
					if (searchableComponent != null) {
						String s = searchableComponent
								.nestSearchableProperties();
						if (StringUtils.isNotBlank(s)) {
							Set<String> nestSearchableProperties = new LinkedHashSet<String>();
							nestSearchableProperties.addAll(Arrays.asList(s
									.split("\\s*,\\s*")));
							uci.setNestSearchableProperties(nestSearchableProperties);
						}
					}
				}
				if (naturalIds.containsKey(pd.getName())) {
					uci.setRequired(true);
					if (naturalIds.size() == 1)
						uci.addCssClass("checkavailable");
				}
				map.put(propertyName, uci);
			}
			List<Map.Entry<String, UiConfigImpl>> list = new ArrayList<Map.Entry<String, UiConfigImpl>>(
					map.entrySet());
			Collections.sort(list, comparator);
			Map<String, UiConfigImpl> sortedMap = new LinkedHashMap<String, UiConfigImpl>();
			for (Map.Entry<String, UiConfigImpl> entry : list)
				sortedMap.put(entry.getKey(), entry.getValue());
			map = sortedMap;
			cache.put(entityClass, Collections.unmodifiableMap(map));
		}
		return map;
	}

	public static Map<String, UiConfigImpl> filterPropertyNamesInCriteria(
			Map<String, UiConfigImpl> uiConfigs) {
		Map<String, UiConfigImpl> propertyNamesInCriterion = new LinkedHashMap<String, UiConfigImpl>();
		for (Map.Entry<String, UiConfigImpl> entry : uiConfigs.entrySet()) {
			if (!entry.getValue().isExcludedFromCriteria()
					&& !entry.getKey().endsWith("AsString")
					&& !CriterionOperator.getSupportedOperators(
							entry.getValue().getPropertyType()).isEmpty()) {
				UiConfigImpl config = entry.getValue();
				Set<String> cssClasses = config.getCssClasses();
				if (cssClasses.contains("date")) {
					config.getCssClasses().clear();
					config.getCssClasses().add("date");
				} else if (cssClasses.contains("datetime")) {
					config.getCssClasses().clear();
					config.getCssClasses().add("datetime");
				} else if (cssClasses.contains("time")) {
					config.getCssClasses().clear();
					config.getCssClasses().add("time");
				} else {
					config.getCssClasses().clear();
				}
				propertyNamesInCriterion.put(entry.getKey(), config);
			}
		}
		return propertyNamesInCriterion;
	}

	public static Map<String, UiConfigImpl> getPropertyNamesInCriteria(
			Class<? extends Persistable<?>> entityClass) {
		return filterPropertyNamesInCriteria(getUiConfigs(entityClass));
	}

	public static String getPickUrl(Class<?> entityClass) {
		String url = AutoConfigPackageProvider.getEntityUrl(entityClass);
		StringBuilder sb = url != null ? new StringBuilder(url)
				: new StringBuilder("/").append(StringUtils
						.uncapitalize(entityClass.getSimpleName()));
		sb.append("/pick");
		Set<String> columns = new LinkedHashSet<String>();
		BeanWrapperImpl bw = new BeanWrapperImpl(entityClass);
		if (BaseTreeableEntity.class.isAssignableFrom(entityClass)) {
			FullnameSeperator fs = entityClass
					.getAnnotation(FullnameSeperator.class);
			if (fs != null && !fs.independent()
					&& bw.isReadableProperty("fullname"))
				columns.add("fullname");
			else
				columns.add("name");
		} else {
			if (bw.isReadableProperty("name"))
				columns.add("name");
			if (bw.isReadableProperty("fullname"))
				columns.add("fullname");
		}
		columns.addAll(AnnotationUtils.getAnnotatedPropertyNameAndAnnotations(
				entityClass, NaturalId.class).keySet());
		for (PropertyDescriptor pd : bw.getPropertyDescriptors()) {
			if (pd.getReadMethod() == null)
				continue;
			UiConfig uic = pd.getReadMethod().getAnnotation(UiConfig.class);
			if (uic == null) {
				try {
					Field f = pd.getReadMethod().getDeclaringClass()
							.getDeclaredField(pd.getName());
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
