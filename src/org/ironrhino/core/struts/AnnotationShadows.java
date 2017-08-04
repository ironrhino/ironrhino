package org.ironrhino.core.struts;

import java.beans.PropertyDescriptor;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.struts2.ServletActionContext;
import org.hibernate.criterion.MatchMode;
import org.ironrhino.core.metadata.Hidden;
import org.ironrhino.core.metadata.Readonly;
import org.ironrhino.core.metadata.Richtable;
import org.ironrhino.core.metadata.UiConfig;
import org.ironrhino.core.model.Persistable;
import org.springframework.beans.BeanWrapperImpl;

import lombok.Data;
import lombok.NoArgsConstructor;

public class AnnotationShadows {

	@Data
	@NoArgsConstructor
	public static class UiConfigImpl implements Serializable {

		private static final long serialVersionUID = -5963246979386241924L;
		private Class<?> propertyType;
		private Class<?> collectionType;
		private Class<?> elementType;
		private String referencedColumnName = "";
		private boolean inverseRelation;
		private String id;
		private String type = UiConfig.DEFAULT_TYPE;
		private String inputType = UiConfig.DEFAULT_INPUT_TYPE;
		private boolean required;
		private boolean unique;
		private boolean multiple;
		private int maxlength;
		private String regex;
		private boolean trim = true;
		private Set<String> cssClasses = new HashSet<>();
		private String thCssClass = "";
		private ReadonlyImpl readonly = new ReadonlyImpl();
		private int displayOrder = Integer.MAX_VALUE;
		private String alias = "";
		private String description = "";
		private String group = "";
		private HiddenImpl hiddenInList = new HiddenImpl();
		private HiddenImpl hiddenInInput = new HiddenImpl();
		private HiddenImpl hiddenInView = new HiddenImpl();
		private boolean shownInPick = false;
		private String template = "";
		private String listTemplate = "";
		private String viewTemplate = "";
		private String inputTemplate = "";
		private String csvTemplate = "";
		private String width;
		private Map<String, String> internalDynamicAttributes = new HashMap<>(0);
		private String dynamicAttributes = "";
		private String cellDynamicAttributes = "";
		private boolean excludeIfNotEdited;
		private String listKey = "";
		private String listValue = "";
		private String listOptions = "";
		private String cellEdit = "";
		private String pickUrl = "";
		private String templateName = "";
		private boolean excludedFromLike = false;
		private boolean excludedFromCriteria = false;
		private boolean excludedFromOrdering = false;
		private boolean excludedFromQuery = false;
		private boolean searchable;
		private boolean exactMatch;
		private Set<String> nestSearchableProperties;
		private Map<String, UiConfigImpl> embeddedUiConfigs;
		private boolean suppressViewLink;
		private boolean showSum;
		private MatchMode queryMatchMode = MatchMode.ANYWHERE;
		private boolean queryWithRange;

		public UiConfigImpl(String propertyName, Class<?> propertyType, UiConfig config) {
			this.propertyType = propertyType;
			if (config == null)
				return;
			if (StringUtils.isNotBlank(config.id()))
				this.id = config.id();
			this.type = config.type();
			this.inputType = config.inputType();
			this.listKey = config.listKey();
			this.listValue = config.listValue();
			setRequired(config.required());
			setUnique(config.unique());
			this.maxlength = config.maxlength();
			this.regex = config.regex();
			this.trim = config.trim();
			this.readonly = new ReadonlyImpl(config.readonly());
			this.displayOrder = config.displayOrder();
			this.alias = config.alias();
			this.description = config.description();
			this.hiddenInList = new HiddenImpl(config.hiddenInList());
			this.hiddenInInput = new HiddenImpl(config.hiddenInInput());
			this.hiddenInView = new HiddenImpl(config.hiddenInView());
			this.shownInPick = config.shownInPick();
			this.listTemplate = config.listTemplate();
			this.viewTemplate = config.viewTemplate();
			this.inputTemplate = config.inputTemplate();
			this.csvTemplate = config.csvTemplate();
			setTemplate(config.template());
			this.width = config.width();
			this.dynamicAttributes = config.dynamicAttributes();
			this.cellDynamicAttributes = config.cellDynamicAttributes();
			this.cellEdit = config.cellEdit();
			setExcludeIfNotEdited(config.excludeIfNotEdited());
			if (StringUtils.isNotBlank(config.cssClass()))
				this.cssClasses.addAll(Arrays.asList(config.cssClass().split("\\s")));
			this.thCssClass = config.thCssClass();
			this.listOptions = config.listOptions().trim();
			if (listOptions.indexOf('{') == 0) {
				this.listKey = "key";
				this.listValue = "value";
			} else if (listOptions.indexOf('[') == 0) {
				this.listKey = "top";
				this.listValue = "top";
			}
			this.pickUrl = config.pickUrl();
			this.templateName = config.templateName();
			if (StringUtils.isBlank(templateName))
				this.templateName = propertyName;
			if (StringUtils.isNotBlank(this.regex)) {
				cssClasses.add("regex");
				internalDynamicAttributes.put("data-regex", this.regex);
			}
			this.excludedFromLike = config.excludedFromLike();
			this.excludedFromCriteria = config.excludedFromCriteria();
			this.excludedFromOrdering = config.excludedFromOrdering();
			this.excludedFromQuery = config.excludedFromQuery();
			this.group = config.group();
			this.searchable = config.searchable();
			this.suppressViewLink = config.suppressViewLink();
			if (config.showSum())
				this.showSum = Number.class.isAssignableFrom(propertyType)
						|| Number.class.isAssignableFrom(ClassUtils.primitiveToWrapper(propertyType));
			this.queryMatchMode = config.queryMatchMode();
			if (Number.class.isAssignableFrom(propertyType)
					|| Number.class.isAssignableFrom(ClassUtils.primitiveToWrapper(propertyType))
					|| Date.class.isAssignableFrom(propertyType))
				this.queryWithRange = config.queryWithRange();
		}

		public void setRequired(boolean required) {
			this.required = required;
			if (this.required)
				cssClasses.add("required");
			else
				cssClasses.remove("required");
		}

		public void setUnique(boolean unique) {
			this.unique = unique;
			if (this.unique)
				cssClasses.add("checkavailable");
			else
				cssClasses.remove("checkavailable");
		}

		public String getCssClass() {
			return StringUtils.join(cssClasses, " ");
		}

		public void addCssClass(String cssClass) {
			this.cssClasses.add(cssClass);
		}

		public void setTemplate(String template) {
			this.template = template;
			if (StringUtils.isEmpty(this.listTemplate))
				this.listTemplate = this.template;
			if (StringUtils.isEmpty(this.viewTemplate))
				this.viewTemplate = this.template;
		}

		public void setExcludeIfNotEdited(boolean excludeIfNotEdited) {
			this.excludeIfNotEdited = excludeIfNotEdited;
			if (excludeIfNotEdited) {
				cssClasses.add("excludeIfNotEdited");
			} else {
				cssClasses.remove("excludeIfNotEdited");
			}
		}

		public boolean isReference() {
			return propertyType != null && Persistable.class.isAssignableFrom(propertyType);
		}

		public boolean isSingleReference() {
			return collectionType == null && propertyType != null && Persistable.class.isAssignableFrom(propertyType);
		}

	}

	@Data
	@NoArgsConstructor
	public static class ReadonlyImpl implements Serializable {

		private static final long serialVersionUID = 6566440254646584026L;
		private boolean value = false;
		private String expression = "";
		private boolean deletable = false;

		public ReadonlyImpl(Readonly config) {
			if (config == null)
				return;
			this.value = config.value();
			this.expression = config.expression();
			this.deletable = config.deletable();
		}

		public boolean isDefaultOptions() {
			return !value && "".equals(expression) && !deletable;
		}

	}

	@Data
	@NoArgsConstructor
	public static class HiddenImpl implements Serializable {

		private static final long serialVersionUID = 6566440254646584026L;
		private boolean value = false;
		private String expression = "";

		public HiddenImpl(Hidden config) {
			if (config == null)
				return;
			this.value = config.value();
			this.expression = config.expression();
			if (!this.value && config.hideWhenBlank())
				if (StringUtils.isBlank(this.expression))
					this.expression = "!(value?has_content)";
				else
					this.expression = "!(value?has_content) || " + this.expression;
		}

		public boolean isDefaultOptions() {
			return !value && "".equals(expression);
		}

	}

	@Data
	@NoArgsConstructor
	public static class RichtableImpl implements Serializable {

		private static final long serialVersionUID = 7346213812241502993L;
		private String alias = "";
		private String formid = "";
		private boolean downloadable = true;
		private boolean filterable = true;
		private boolean celleditable = true;
		private boolean showPageSize = true;
		private boolean showCheckColumn = true;
		private boolean showActionColumn = true;
		private boolean showBottomButtons = true;
		private boolean searchable;
		private boolean exportable;
		private boolean importable;
		private String actionColumnButtons = "";
		private String bottomButtons = "";
		private String listHeader = "";
		private String listFooter = "";
		private String formHeader = "";
		private String formFooter = "";
		private String rowDynamicAttributes = "";
		private String inputFormCssClass = "";
		private String listFormCssClass = "";
		private String inputWindowOptions = "";
		private String viewWindowOptions = "";
		private int inputGridColumns = 0;
		private int viewGridColumns = 0;
		private boolean showQueryForm = false;
		private int queryFormGridColumns = 0;

		public RichtableImpl(Richtable config) {
			if (config == null)
				return;
			this.alias = config.alias();
			this.formid = config.formid();
			this.downloadable = config.downloadable();
			this.filterable = config.filterable();
			this.celleditable = config.celleditable();
			this.showPageSize = !config.fixPageSize() && config.showPageSize();
			this.showCheckColumn = config.showCheckColumn();
			this.showActionColumn = config.showActionColumn();
			this.showBottomButtons = config.showBottomButtons();
			this.searchable = config.searchable();
			this.exportable = config.exportable();
			this.importable = config.importable();
			this.actionColumnButtons = config.actionColumnButtons();
			this.bottomButtons = config.bottomButtons();
			this.listHeader = config.listHeader();
			this.listFooter = config.listFooter();
			this.formHeader = config.formHeader();
			this.formFooter = config.formFooter();
			this.rowDynamicAttributes = config.rowDynamicAttributes();
			this.inputFormCssClass = config.inputFormCssClass();
			this.listFormCssClass = config.listFormCssClass();
			this.inputWindowOptions = config.inputWindowOptions();
			this.viewWindowOptions = config.viewWindowOptions();
			if (config.inputGridColumns() > 1 && config.inputGridColumns() < 5 || config.inputGridColumns() == 6)
				this.inputGridColumns = config.inputGridColumns();
			if (config.viewGridColumns() > 1 && config.viewGridColumns() < 5 || config.viewGridColumns() == 6)
				this.viewGridColumns = config.viewGridColumns();
			if (config.gridColumns() > 1 && config.gridColumns() < 5 || config.gridColumns() == 6) {
				if (this.inputGridColumns == 0)
					this.inputGridColumns = config.gridColumns();
				if (this.viewGridColumns == 0)
					this.viewGridColumns = config.gridColumns();
			}
			this.showQueryForm = config.showQueryForm();
			this.queryFormGridColumns = config.queryFormGridColumns();
			if (this.queryFormGridColumns == 0) {
				this.queryFormGridColumns = this.inputGridColumns;
			}
			if (this.queryFormGridColumns == 0)
				this.queryFormGridColumns = 3;
			overrideByRequestParameters(this);
		}

		private static void overrideByRequestParameters(RichtableImpl richtable) {
			if (ServletActionContext.getRequest() == null)
				return;
			BeanWrapperImpl bw = new BeanWrapperImpl(richtable);
			for (PropertyDescriptor pd : bw.getPropertyDescriptors()) {
				if (pd.getReadMethod() == null || pd.getWriteMethod() == null)
					continue;
				Object value = bw.getPropertyValue(pd.getName());
				if (Boolean.TRUE.equals(value)) {
					if ("false".equals(ServletActionContext.getRequest().getParameter("richtable." + pd.getName())))
						bw.setPropertyValue(pd.getName(), false);
				}
			}
		}

	}

}
