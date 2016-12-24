package org.ironrhino.core.struts;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.criterion.MatchMode;
import org.ironrhino.core.metadata.Hidden;
import org.ironrhino.core.metadata.Readonly;
import org.ironrhino.core.metadata.Richtable;
import org.ironrhino.core.metadata.UiConfig;
import org.ironrhino.core.model.Persistable;

public class AnnotationShadows {

	public static class UiConfigImpl implements Serializable {

		private static final long serialVersionUID = -5963246979386241924L;
		private Class<?> propertyType;
		private Class<?> collectionType;
		private Class<?> elementType;
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
		private String listKey = UiConfig.DEFAULT_LIST_KEY;
		private String listValue = UiConfig.DEFAULT_LIST_VALUE;
		private String listOptions = "";
		private String cellEdit = "";
		private String pickUrl = "";
		private boolean pickMultiple;
		private String templateName = "";
		private boolean excludedFromLike = false;
		private boolean excludedFromCriteria = false;
		private boolean excludedFromOrdering = false;
		private boolean searchable;
		private boolean exactMatch;
		private Set<String> nestSearchableProperties;
		private Map<String, UiConfigImpl> embeddedUiConfigs;
		private boolean suppressViewLink;
		private boolean showSum;
		private MatchMode queryMatchMode;

		public UiConfigImpl() {
		}

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
			this.required = config.required();
			this.unique = config.unique();
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
			this.template = config.template();
			this.listTemplate = config.listTemplate();
			this.viewTemplate = config.viewTemplate();
			if (this.listTemplate.isEmpty())
				this.listTemplate = this.template;
			if (this.viewTemplate.isEmpty())
				this.viewTemplate = this.template;
			this.inputTemplate = config.inputTemplate();
			this.csvTemplate = config.csvTemplate();
			this.width = config.width();
			this.dynamicAttributes = config.dynamicAttributes();
			this.cellDynamicAttributes = config.cellDynamicAttributes();
			this.cellEdit = config.cellEdit();
			this.excludeIfNotEdited = config.excludeIfNotEdited();
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
			this.pickMultiple = config.pickMultiple();
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
			this.group = config.group();
			this.searchable = config.searchable();
			this.suppressViewLink = config.suppressViewLink();
			if (config.showSum())
				this.showSum = Number.class.isAssignableFrom(propertyType) || propertyType == short.class
						|| propertyType == int.class || propertyType == long.class || propertyType == float.class
						|| propertyType == double.class;
			this.queryMatchMode = config.queryMatchMode();
		}

		public boolean isExcludedFromLike() {
			return excludedFromLike;
		}

		public void setExcludedFromLike(boolean excludedFromLike) {
			this.excludedFromLike = excludedFromLike;
		}

		public boolean isExcludedFromCriteria() {
			return excludedFromCriteria;
		}

		public void setExcludedFromCriteria(boolean excludedFromCriteria) {
			this.excludedFromCriteria = excludedFromCriteria;
		}

		public boolean isExcludedFromOrdering() {
			return excludedFromOrdering;
		}

		public void setExcludedFromOrdering(boolean excludedFromOrdering) {
			this.excludedFromOrdering = excludedFromOrdering;
		}

		public Class<?> getPropertyType() {
			return propertyType;
		}

		public void setPropertyType(Class<?> propertyType) {
			this.propertyType = propertyType;
		}

		public Class<?> getCollectionType() {
			return collectionType;
		}

		public void setCollectionType(Class<?> collectionType) {
			this.collectionType = collectionType;
		}

		public Class<?> getElementType() {
			return elementType;
		}

		public void setElementType(Class<?> elementType) {
			this.elementType = elementType;
		}

		public HiddenImpl getHiddenInList() {
			return hiddenInList;
		}

		public void setHiddenInList(HiddenImpl hiddenInList) {
			this.hiddenInList = hiddenInList;
		}

		public HiddenImpl getHiddenInInput() {
			return hiddenInInput;
		}

		public void setHiddenInInput(HiddenImpl hiddenInInput) {
			this.hiddenInInput = hiddenInInput;
		}

		public HiddenImpl getHiddenInView() {
			return hiddenInView;
		}

		public void setHiddenInView(HiddenImpl hiddenInView) {
			this.hiddenInView = hiddenInView;
		}

		public boolean isShownInPick() {
			return shownInPick;
		}

		public void setShownInPick(boolean shownInPick) {
			this.shownInPick = shownInPick;
		}

		public String getPickUrl() {
			return pickUrl;
		}

		public void setPickUrl(String pickUrl) {
			this.pickUrl = pickUrl;
		}

		public boolean isPickMultiple() {
			return pickMultiple;
		}

		public void setPickMultiple(boolean pickMultiple) {
			this.pickMultiple = pickMultiple;
		}

		public String getTemplateName() {
			return templateName;
		}

		public void setTemplateName(String templateName) {
			this.templateName = templateName;
		}

		public Map<String, String> getInternalDynamicAttributes() {
			return internalDynamicAttributes;
		}

		public void setInternalDynamicAttributes(Map<String, String> internalDynamicAttributes) {
			this.internalDynamicAttributes = internalDynamicAttributes;
		}

		public String getDynamicAttributes() {
			return dynamicAttributes;
		}

		public void setDynamicAttributes(String dynamicAttributes) {
			this.dynamicAttributes = dynamicAttributes;
		}

		public String getCellDynamicAttributes() {
			return cellDynamicAttributes;
		}

		public void setCellDynamicAttributes(String cellDynamicAttributes) {
			this.cellDynamicAttributes = cellDynamicAttributes;
		}

		public boolean isRequired() {
			return required;
		}

		public void setRequired(boolean required) {
			this.required = required;
		}

		public boolean isUnique() {
			return unique;
		}

		public void setUnique(boolean unique) {
			this.unique = unique;
		}

		public boolean isMultiple() {
			return multiple;
		}

		public void setMultiple(boolean multiple) {
			this.multiple = multiple;
		}

		public String getAlias() {
			return alias;
		}

		public void setAlias(String alias) {
			this.alias = alias;
		}

		public String getDescription() {
			return description;
		}

		public void setDescription(String description) {
			this.description = description;
		}

		public String getGroup() {
			return group;
		}

		public void setGroup(String group) {
			this.group = group;
		}

		public int getDisplayOrder() {
			return displayOrder;
		}

		public void setDisplayOrder(int displayOrder) {
			this.displayOrder = displayOrder;
		}

		public String getId() {
			return id;
		}

		public void setId(String id) {
			this.id = id;
		}

		public String getType() {
			return type;
		}

		public void setType(String type) {
			this.type = type;
		}

		public String getInputType() {
			return inputType;
		}

		public void setInputType(String inputType) {
			this.inputType = inputType;
		}

		public int getMaxlength() {
			return maxlength;
		}

		public void setMaxlength(int maxlength) {
			this.maxlength = maxlength;
		}

		public String getRegex() {
			return regex;
		}

		public void setRegex(String regex) {
			this.regex = regex;
		}

		public boolean isTrim() {
			return trim;
		}

		public void setTrim(boolean trim) {
			this.trim = trim;
		}

		public String getListKey() {
			return listKey;
		}

		public void setListKey(String listKey) {
			this.listKey = listKey;
		}

		public String getListValue() {
			return listValue;
		}

		public void setListValue(String listValue) {
			this.listValue = listValue;
		}

		public String getCssClass() {
			if (required)
				addCssClass("required");
			if (unique)
				addCssClass("checkavailable");
			if (excludeIfNotEdited)
				addCssClass("excludeIfNotEdited");
			return StringUtils.join(cssClasses, " ");
		}

		public void addCssClass(String cssClass) {
			this.cssClasses.add(cssClass);
		}

		public Set<String> getCssClasses() {
			return cssClasses;
		}

		public void setCssClasses(Set<String> cssClasses) {
			this.cssClasses = cssClasses;
		}

		public String getThCssClass() {
			return thCssClass;
		}

		public void setThCssClass(String thCssClass) {
			this.thCssClass = thCssClass;
		}

		public ReadonlyImpl getReadonly() {
			return readonly;
		}

		public void setReadonly(ReadonlyImpl readonly) {
			this.readonly = readonly;
		}

		public String getTemplate() {
			return template;
		}

		public void setTemplate(String template) {
			this.template = template;
		}

		public String getListTemplate() {
			return listTemplate;
		}

		public void setListTemplate(String listTemplate) {
			this.listTemplate = listTemplate;
		}

		public String getViewTemplate() {
			return viewTemplate;
		}

		public void setViewTemplate(String viewTemplate) {
			this.viewTemplate = viewTemplate;
		}

		public String getInputTemplate() {
			return inputTemplate;
		}

		public void setInputTemplate(String inputTemplate) {
			this.inputTemplate = inputTemplate;
		}

		public String getCsvTemplate() {
			return csvTemplate;
		}

		public void setCsvTemplate(String csvTemplate) {
			this.csvTemplate = csvTemplate;
		}

		public String getWidth() {
			return width;
		}

		public void setWidth(String width) {
			this.width = width;
		}

		public boolean isExcludeIfNotEdited() {
			return excludeIfNotEdited;
		}

		public void setExcludeIfNotEdited(boolean excludeIfNotEdited) {
			this.excludeIfNotEdited = excludeIfNotEdited;
		}

		public String getCellEdit() {
			return cellEdit;
		}

		public void setCellEdit(String cellEdit) {
			this.cellEdit = cellEdit;
		}

		public String getListOptions() {
			return listOptions;
		}

		public void setListOptions(String listOptions) {
			this.listOptions = listOptions;
		}

		public boolean isSearchable() {
			return searchable;
		}

		public void setSearchable(boolean searchable) {
			this.searchable = searchable;
		}

		public boolean isExactMatch() {
			return exactMatch;
		}

		public void setExactMatch(boolean exactMatch) {
			this.exactMatch = exactMatch;
		}

		public Set<String> getNestSearchableProperties() {
			return nestSearchableProperties;
		}

		public void setNestSearchableProperties(Set<String> nestSearchableProperties) {
			this.nestSearchableProperties = nestSearchableProperties;
		}

		public boolean isSuppressViewLink() {
			return suppressViewLink;
		}

		public void setSuppressViewLink(boolean suppressViewLink) {
			this.suppressViewLink = suppressViewLink;
		}

		public boolean isShowSum() {
			return showSum;
		}

		public void setShowSum(boolean showSum) {
			this.showSum = showSum;
		}

		public Map<String, UiConfigImpl> getEmbeddedUiConfigs() {
			return embeddedUiConfigs;
		}

		public void setEmbeddedUiConfigs(Map<String, UiConfigImpl> embeddedUiConfigs) {
			this.embeddedUiConfigs = embeddedUiConfigs;
		}

		public MatchMode getQueryMatchMode() {
			return queryMatchMode;
		}

		public void setQueryMatchMode(MatchMode queryMatchMode) {
			this.queryMatchMode = queryMatchMode;
		}

		public boolean isReference() {
			return propertyType != null && Persistable.class.isAssignableFrom(propertyType);
		}

	}

	public static class ReadonlyImpl implements Serializable {

		private static final long serialVersionUID = 6566440254646584026L;
		private boolean value = false;
		private String expression = "";
		private boolean deletable = false;

		public ReadonlyImpl() {
		}

		public ReadonlyImpl(Readonly config) {
			if (config == null)
				return;
			this.value = config.value();
			this.expression = config.expression();
			this.deletable = config.deletable();
		}

		public boolean isValue() {
			return value;
		}

		public void setValue(boolean value) {
			this.value = value;
		}

		public String getExpression() {
			return expression;
		}

		public void setExpression(String expression) {
			this.expression = expression;
		}

		public boolean isDeletable() {
			return deletable;
		}

		public void setDeletable(boolean deletable) {
			this.deletable = deletable;
		}

		public boolean isDefaultOptions() {
			return !value && "".equals(expression) && !deletable;
		}

	}

	public static class HiddenImpl implements Serializable {

		private static final long serialVersionUID = 6566440254646584026L;
		private boolean value = false;
		private String expression = "";

		public HiddenImpl() {
		}

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

		public boolean isValue() {
			return value;
		}

		public void setValue(boolean value) {
			this.value = value;
		}

		public String getExpression() {
			return expression;
		}

		public void setExpression(String expression) {
			this.expression = expression;
		}

		public boolean isDefaultOptions() {
			return !value && "".equals(expression);
		}

	}

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

		public RichtableImpl() {
		}

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
		}

		public String getAlias() {
			return alias;
		}

		public void setAlias(String alias) {
			this.alias = alias;
		}

		public String getFormid() {
			return formid;
		}

		public void setFormid(String formid) {
			this.formid = formid;
		}

		public boolean isFilterable() {
			return filterable;
		}

		public void setFilterable(boolean filterable) {
			this.filterable = filterable;
		}

		public boolean isDownloadable() {
			return downloadable;
		}

		public void setDownloadable(boolean downloadable) {
			this.downloadable = downloadable;
		}

		public boolean isCelleditable() {
			return celleditable;
		}

		public void setCelleditable(boolean celleditable) {
			this.celleditable = celleditable;
		}

		public boolean isShowPageSize() {
			return showPageSize;
		}

		public void setShowPageSize(boolean showPageSize) {
			this.showPageSize = showPageSize;
		}

		public boolean isShowCheckColumn() {
			return showCheckColumn;
		}

		public void setShowCheckColumn(boolean showCheckColumn) {
			this.showCheckColumn = showCheckColumn;
		}

		public boolean isShowActionColumn() {
			return showActionColumn;
		}

		public void setShowActionColumn(boolean showActionColumn) {
			this.showActionColumn = showActionColumn;
		}

		public boolean isShowBottomButtons() {
			return showBottomButtons;
		}

		public void setShowBottomButtons(boolean showBottomButtons) {
			this.showBottomButtons = showBottomButtons;
		}

		public boolean isSearchable() {
			return searchable;
		}

		public void setSearchable(boolean searchable) {
			this.searchable = searchable;
		}

		public boolean isExportable() {
			return exportable;
		}

		public void setExportable(boolean exportable) {
			this.exportable = exportable;
		}

		public boolean isImportable() {
			return importable;
		}

		public void setImportable(boolean importable) {
			this.importable = importable;
		}

		public String getActionColumnButtons() {
			return actionColumnButtons;
		}

		public void setActionColumnButtons(String actionColumnButtons) {
			this.actionColumnButtons = actionColumnButtons;
		}

		public String getBottomButtons() {
			return bottomButtons;
		}

		public void setBottomButtons(String bottomButtons) {
			this.bottomButtons = bottomButtons;
		}

		public String getListHeader() {
			return listHeader;
		}

		public void setListHeader(String listHeader) {
			this.listHeader = listHeader;
		}

		public String getListFooter() {
			return listFooter;
		}

		public void setListFooter(String listFooter) {
			this.listFooter = listFooter;
		}

		public String getFormHeader() {
			return formHeader;
		}

		public void setFormHeader(String formHeader) {
			this.formHeader = formHeader;
		}

		public String getFormFooter() {
			return formFooter;
		}

		public void setFormFooter(String formFooter) {
			this.formFooter = formFooter;
		}

		public String getRowDynamicAttributes() {
			return rowDynamicAttributes;
		}

		public void setRowDynamicAttributes(String rowDynamicAttributes) {
			this.rowDynamicAttributes = rowDynamicAttributes;
		}

		public String getInputFormCssClass() {
			return inputFormCssClass;
		}

		public void setInputFormCssClass(String inputFormCssClass) {
			this.inputFormCssClass = inputFormCssClass;
		}

		public String getListFormCssClass() {
			return listFormCssClass;
		}

		public void setListFormCssClass(String listFormCssClass) {
			this.listFormCssClass = listFormCssClass;
		}

		public String getInputWindowOptions() {
			return inputWindowOptions;
		}

		public void setInputWindowOptions(String inputWindowOptions) {
			this.inputWindowOptions = inputWindowOptions;
		}

		public String getViewWindowOptions() {
			return viewWindowOptions;
		}

		public void setViewWindowOptions(String viewWindowOptions) {
			this.viewWindowOptions = viewWindowOptions;
		}

		public int getInputGridColumns() {
			return inputGridColumns;
		}

		public void setInputGridColumns(int inputGridColumns) {
			this.inputGridColumns = inputGridColumns;
		}

		public int getViewGridColumns() {
			return viewGridColumns;
		}

		public void setViewGridColumns(int viewGridColumns) {
			this.viewGridColumns = viewGridColumns;
		}

		public boolean isshowQueryForm() {
			return showQueryForm;
		}

		public void setshowQueryForm(boolean showQueryForm) {
			this.showQueryForm = showQueryForm;
		}

	}

}
