package org.ironrhino.common.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

import org.hibernate.annotations.NaturalId;
import org.ironrhino.common.record.RecordAware;
import org.ironrhino.core.aop.PublishAware;
import org.ironrhino.core.metadata.AutoConfig;
import org.ironrhino.core.metadata.CaseInsensitive;
import org.ironrhino.core.metadata.Readonly;
import org.ironrhino.core.metadata.Richtable;
import org.ironrhino.core.metadata.UiConfig;
import org.ironrhino.core.model.BaseRecordableEntity;
import org.ironrhino.core.search.elasticsearch.annotations.Searchable;
import org.ironrhino.core.search.elasticsearch.annotations.SearchableProperty;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@RecordAware
@PublishAware
@AutoConfig
@Searchable
@Entity
@Table(name = "common_setting")
@Richtable(searchable = true, readonly = @Readonly(expression = "entity.readonly"), order = "key asc", exportable = true, importable = true)
@Getter
@Setter
@NoArgsConstructor
public class Setting extends BaseRecordableEntity {

	private static final long serialVersionUID = -8352037603261222984L;

	@UiConfig(width = "300px")
	@SearchableProperty(boost = 3)
	@CaseInsensitive
	@NaturalId(mutable = true)
	@Column(name = "key", nullable = false)
	private String key = "";

	@UiConfig(type = "textarea", width = "400px")
	@SearchableProperty
	@Column(length = 4000)
	private String value = "";

	@UiConfig(type = "textarea")
	@SearchableProperty
	@Column(length = 4000)
	private String description = "";

	@UiConfig(hidden = true)
	private boolean readonly;

	@UiConfig(hidden = true)
	private boolean hidden;

	public Setting(String key, String value) {
		this.key = key;
		this.value = value;
	}

}
