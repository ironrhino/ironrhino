package org.ironrhino.common.model;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Converter;
import javax.persistence.Entity;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.annotations.NaturalId;
import org.ironrhino.core.hibernate.convert.JsonConverter;
import org.ironrhino.core.metadata.Authorize;
import org.ironrhino.core.metadata.AutoConfig;
import org.ironrhino.core.metadata.CaseInsensitive;
import org.ironrhino.core.metadata.Hidden;
import org.ironrhino.core.metadata.Richtable;
import org.ironrhino.core.metadata.UiConfig;
import org.ironrhino.core.model.BaseRecordableEntity;
import org.ironrhino.core.search.elasticsearch.annotations.Searchable;
import org.ironrhino.core.search.elasticsearch.annotations.SearchableComponent;
import org.ironrhino.core.search.elasticsearch.annotations.SearchableProperty;
import org.ironrhino.core.security.role.UserRole;
import org.ironrhino.core.struts.ValidationException;

import lombok.Getter;
import lombok.Setter;

@AutoConfig
@Searchable
@Authorize(ifAnyGranted = UserRole.ROLE_ADMINISTRATOR)
@Entity
@Table(name = "common_schema")
@Richtable(searchable = true, order = "name asc", exportable = true, importable = true)
@Getter
@Setter
public class Schema extends BaseRecordableEntity {

	private static final long serialVersionUID = -8352037604269012984L;

	@SearchableProperty(boost = 3)
	@UiConfig(width = "300px")
	@Column(nullable = false)
	@CaseInsensitive
	@NaturalId(mutable = true)
	private String name;

	@SearchableProperty(boost = 3)
	@UiConfig(type = "textarea")
	@Column(length = 4000)
	private String description;

	@UiConfig(width = "100px")
	private boolean strict;

	// @Lob
	@Column(length = 4000)
	@SearchableComponent
	@UiConfig(hiddenInList = @Hidden(true), excludedFromCriteria = true)
	private List<SchemaField> fields = new ArrayList<>();

	public Schema merge(Schema other) {
		if (other != null)
			fields.addAll(other.getFields());
		Set<String> names = new HashSet<>();
		Iterator<SchemaField> it = fields.iterator();
		while (it.hasNext()) {
			SchemaField f = it.next();
			if (names.contains(f.getName()) || StringUtils.isBlank(f.getName())) {
				it.remove();
				continue;
			}
			names.add(f.getName());
		}
		return this;
	}

	@PrePersist
	@PreUpdate
	private void validate() {
		if (fields != null && fields.size() > 0) {
			boolean hasGroup = false;
			boolean needApendBlankGroup = false;
			Set<String> names = new HashSet<>();
			for (int i = 0; i < fields.size(); i++) {
				SchemaField f = fields.get(i);
				if (f.getType().equals(SchemaFieldType.GROUP)) {
					hasGroup = true;
					continue;
				}
				if (StringUtils.isBlank(f.getName())) {
					ValidationException ve = new ValidationException();
					ve.addFieldError("schema.fields[" + i + "].name", "validation.required");
					throw ve;
				} else {
					if (names.contains(f.getName())) {
						ValidationException ve = new ValidationException();
						ve.addFieldError("schema.fields[" + i + "].name", "validation.already.exists");
						throw ve;
					} else {
						names.add(f.getName());
					}
				}
				if (f.getType().equals(SchemaFieldType.SELECT) || f.getType().equals(SchemaFieldType.CHECKBOX)) {
					List<String> values = f.getValues();
					if (values == null || values.size() == 0
							|| values.size() == 1 && StringUtils.isBlank(values.get(0))) {
						ValidationException ve = new ValidationException();
						ve.addFieldError("schema.fields[" + i + "].values[0]", "validation.required");
						throw ve;
					} else {
						Set<String> set = new HashSet<>();
						for (int j = 0; j < values.size(); j++) {
							String value = values.get(j);
							if (StringUtils.isBlank(value)) {
								ValidationException ve = new ValidationException();
								ve.addFieldError("schema.fields[" + i + "].values[" + j + "]", "validation.required");
								throw ve;
							} else {
								if (set.contains(value)) {
									ValidationException ve = new ValidationException();
									ve.addFieldError("schema.fields[" + i + "].values[" + j + "]",
											"validation.already.exists");
									throw ve;
								} else {
									set.add(value);
								}
							}
						}
					}
				} else {
					f.setValues(null);
				}
				if (hasGroup && i == fields.size() - 1)
					needApendBlankGroup = hasGroup && i == fields.size() - 1;
			}
			if (needApendBlankGroup) {
				SchemaField f = new SchemaField();
				f.setName("");
				f.setType(SchemaFieldType.GROUP);
				fields.add(f);
			}
		}
	}

	@Converter(autoApply = true)
	public static class FieldsConverter extends JsonConverter<List<SchemaField>> {

	}

}
