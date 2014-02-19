package org.ironrhino.common.model;

import java.util.Date;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.hibernate.annotations.GenericGenerator;
import org.ironrhino.core.dataroute.DataRoute;
import org.ironrhino.core.metadata.AutoConfig;
import org.ironrhino.core.metadata.Readonly;
import org.ironrhino.core.metadata.Richtable;
import org.ironrhino.core.metadata.UiConfig;
import org.ironrhino.core.model.Persistable;
import org.ironrhino.core.stat.Key;
import org.ironrhino.core.stat.KeyValuePair;
import org.ironrhino.core.stat.Value;

@DataRoute("miscGroup")
@AutoConfig
@Entity
@Table(name = "common_stat")
@Richtable(readonly = @Readonly(true), order = "date desc")
public class Stat extends KeyValuePair implements Persistable<String> {

	private static final long serialVersionUID = -1795832273603877285L;

	@Id
	@GeneratedValue(generator = "stringId")
	@GenericGenerator(name = "stringId", strategy = "org.ironrhino.core.hibernate.StringIdGenerator")
	private String id;

	public Stat() {

	}

	public Stat(Key key, Value value, Date date, String host) {
		super();
		this.key = key;
		this.value = value;
		this.date = date;
		this.host = host;
	}

	@Override
	public boolean isNew() {
		return StringUtils.isNotBlank(id);
	}

	@Override
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	@Override
	@UiConfig(hidden = true)
	public Key getKey() {
		return key;
	}

	@Override
	@UiConfig(hidden = true)
	public Value getValue() {
		return value;
	}

	@UiConfig(displayOrder = 0, alias = "key")
	@Access(AccessType.PROPERTY)
	@Column(nullable = false)
	public String getKeyAsString() {
		if (key != null)
			return key.toString();
		return null;
	}

	public void setKeyAsString(String keyAsString) {
		if (key == null)
			key = Key.fromString(keyAsString);
	}

	@UiConfig(displayOrder = 1, alias = "value")
	@Access(AccessType.PROPERTY)
	@Column(nullable = false)
	public String getValueAsString() {
		if (value != null)
			return value.toString();
		return null;
	}

	public void setValueAsString(String valueAsString) {
		if (value == null)
			value = Value.fromString(valueAsString);
	}

	@Override
	@UiConfig(displayOrder = 2)
	public Date getDate() {
		return date;
	}

	@Override
	@UiConfig(displayOrder = 3)
	public String getHost() {
		return host;
	}

	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this);
	}

}
