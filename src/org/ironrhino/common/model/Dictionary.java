package org.ironrhino.common.model;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Lob;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.Version;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.annotations.NaturalId;
import org.ironrhino.core.aop.PublishAware;
import org.ironrhino.core.metadata.Authorize;
import org.ironrhino.core.metadata.AutoConfig;
import org.ironrhino.core.metadata.CaseInsensitive;
import org.ironrhino.core.metadata.Hidden;
import org.ironrhino.core.metadata.NotInCopy;
import org.ironrhino.core.metadata.Richtable;
import org.ironrhino.core.metadata.UiConfig;
import org.ironrhino.core.model.BaseEntity;
import org.ironrhino.core.model.LabelValue;
import org.ironrhino.core.search.elasticsearch.annotations.Searchable;
import org.ironrhino.core.search.elasticsearch.annotations.SearchableComponent;
import org.ironrhino.core.search.elasticsearch.annotations.SearchableProperty;
import org.ironrhino.core.security.role.UserRole;
import org.ironrhino.core.struts.ValidationException;
import org.ironrhino.core.util.JsonUtils;

import com.fasterxml.jackson.core.type.TypeReference;

@PublishAware
@AutoConfig
@Searchable
@Authorize(ifAnyGranted = UserRole.ROLE_ADMINISTRATOR)
@Entity
@Table(name = "common_dictionary")
@Richtable(searchable = true, order = "name asc", exportable = true, importable = true)
public class Dictionary extends BaseEntity {

	private static final long serialVersionUID = -8352037604261222984L;

	private static final TypeReference<List<LabelValue>> TYPE_LIST = new TypeReference<List<LabelValue>>() {
	};

	@SearchableProperty(boost = 3)
	@UiConfig(displayOrder = 1, width = "300px")
	@CaseInsensitive
	@NaturalId(mutable = true)
	@Column(nullable = false)
	private String name;

	@SearchableProperty(boost = 3)
	@UiConfig(displayOrder = 2)
	private String description;

	@SearchableComponent
	@UiConfig(displayOrder = 3, hiddenInList = @Hidden(true))
	@Transient
	private List<LabelValue> items = new ArrayList<LabelValue>();

	@Version
	private int version = -1;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public List<LabelValue> getItems() {
		return items;
	}

	public void setItems(List<LabelValue> items) {
		this.items = items;
	}

	@NotInCopy
	@UiConfig(hidden = true)
	@Column(name = "items")
	@Lob
	@Access(AccessType.PROPERTY)
	public String getItemsAsString() {
		if (items == null || items.isEmpty())
			return null;
		return JsonUtils.toJson(items);
	}

	public void setItemsAsString(String str) {
		if (StringUtils.isNotBlank(str))
			try {
				items = JsonUtils.fromJson(str, TYPE_LIST);
			} catch (Exception e) {
				e.printStackTrace();
			}
	}

	public int getVersion() {
		return version;
	}

	public void setVersion(int version) {
		this.version = version;
	}

	@UiConfig(hidden = true)
	@NotInCopy
	public Map<String, String> getItemsAsMap() {
		Map<String, String> map = new LinkedHashMap<String, String>(
				items.size() + 1, 1);
		for (LabelValue lv : items)
			if (StringUtils.isNotBlank(lv.getValue()))
				map.put(lv.getValue(), lv.getLabel());
		return map;
	}

	@UiConfig(hidden = true)
	@NotInCopy
	public Map<String, Map<String, String>> getItemsAsGroup() {
		Map<String, Map<String, String>> map = new LinkedHashMap<String, Map<String, String>>();
		String group = "";
		for (LabelValue lv : items) {
			if (StringUtils.isBlank(lv.getValue())) {
				String label = lv.getLabel();
				if ((StringUtils.isBlank(label))
						&& StringUtils.isNotBlank(group)) {
					group = "";
				} else {
					group = label;
				}
			} else {
				Map<String, String> temp = map.get(group);
				if (temp == null) {
					temp = new LinkedHashMap<String, String>();
					map.put(group, temp);
				}
				temp.put(lv.getValue(), lv.getLabel());
			}
		}
		return map;
	}

	@UiConfig(hidden = true)
	@NotInCopy
	public boolean isGroupable() {
		boolean groupable = false;
		for (LabelValue item : items) {
			if (StringUtils.isNotBlank(item.getLabel())
					&& StringUtils.isBlank(item.getValue())) {
				groupable = true;
				break;
			}
		}
		return groupable;
	}

	@PrePersist
	@PreUpdate
	public void validate() {
		if (items != null && items.size() > 0) {
			Set<String> values = new HashSet<String>(items.size());
			Set<String> labels = new HashSet<String>(items.size());
			for (int i = 0; i < items.size(); i++) {
				LabelValue lv = items.get(i);
				if (StringUtils.isNotBlank(lv.getValue())) {
					String value = lv.getValue();
					if (values.contains(value)) {
						ValidationException ve = new ValidationException();
						ve.addFieldError("dictionary.items[" + i + "].value",
								"validation.already.exists");
						throw ve;
					} else {
						values.add(value);
					}
					if (StringUtils.isNotBlank(lv.getLabel())) {
						String label = lv.getLabel();
						if (labels.contains(label)) {
							ValidationException ve = new ValidationException();
							ve.addFieldError("dictionary.items[" + i
									+ "].label", "validation.already.exists");
							throw ve;
						} else {
							labels.add(label);
						}
					}

				}
			}

		}
	}

}
