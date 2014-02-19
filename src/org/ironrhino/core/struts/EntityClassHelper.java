package org.ironrhino.core.struts;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.ArrayList;
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
import org.ironrhino.core.metadata.UiConfig;
import org.ironrhino.core.model.Attributable;
import org.ironrhino.core.model.Persistable;
import org.ironrhino.core.search.elasticsearch.annotations.SearchableId;
import org.ironrhino.core.search.elasticsearch.annotations.SearchableProperty;
import org.ironrhino.core.struts.AnnotationShadows.UiConfigImpl;
import org.ironrhino.core.util.AnnotationUtils;
import org.ironrhino.core.util.AppInfo;
import org.ironrhino.core.util.AppInfo.Stage;
import org.ironrhino.core.util.ValueThenKeyComparator;

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
				UiConfigImpl uci = new UiConfigImpl(pd.getPropertyType(),
						uiConfig);
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
					map.put(propertyName, uci);
					continue;
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
					uci.setType("listpick");
					uci.setExcludeIfNotEdited(true);
					if (StringUtils.isBlank(uci.getPickUrl())) {
						String url = AutoConfigPackageProvider
								.getEntityUrl(returnType);
						StringBuilder sb = url != null ? new StringBuilder(url)
								: new StringBuilder("/").append(StringUtils
										.uncapitalize(returnType
												.getSimpleName()));
						sb.append("/pick");
						Set<String> columns = new LinkedHashSet<String>();
						columns.addAll(AnnotationUtils
								.getAnnotatedPropertyNameAndAnnotations(
										returnType, NaturalId.class).keySet());
						Map<String, UiConfigImpl> configs = getUiConfigs(returnType);
						for (String column : "fullname,name,code".split(","))
							if (configs.containsKey(column)
									&& (!columns.contains("fullname")
											&& column.equals("name") || !column
												.equals("name")))
								columns.add(column);
						for (Map.Entry<String, UiConfigImpl> entry : configs
								.entrySet())
							if (entry.getValue().isShownInPick())
								columns.add(entry.getKey());
						if (!columns.isEmpty()) {
							sb.append("?columns="
									+ StringUtils.join(columns, ','));
						}
						uci.setPickUrl(sb.toString());
					}
					map.put(propertyName, uci);
					continue;
				}
				if (returnType == Integer.TYPE || returnType == Short.TYPE
						|| returnType == Long.TYPE || returnType == Double.TYPE
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
				if (searchableProperty != null || searchableId != null)
					uci.setSearchable(true);

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

	private static ValueThenKeyComparator<String, UiConfigImpl> comparator = new ValueThenKeyComparator<String, UiConfigImpl>() {
		@Override
		protected int compareValue(UiConfigImpl a, UiConfigImpl b) {
			return a.getDisplayOrder() - b.getDisplayOrder();
		}
	};

}
