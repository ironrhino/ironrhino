package org.ironrhino.common.model;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Version;

import org.hibernate.annotations.NaturalId;
import org.ironrhino.common.record.RecordAware;
import org.ironrhino.core.aop.PublishAware;
import org.ironrhino.core.metadata.AutoConfig;
import org.ironrhino.core.metadata.CaseInsensitive;
import org.ironrhino.core.metadata.NotInCopy;
import org.ironrhino.core.metadata.Richtable;
import org.ironrhino.core.metadata.UiConfig;
import org.ironrhino.core.model.BaseEntity;
import org.ironrhino.core.model.Recordable;
import org.ironrhino.core.search.elasticsearch.annotations.Searchable;
import org.ironrhino.core.search.elasticsearch.annotations.SearchableProperty;
import org.springframework.security.core.userdetails.UserDetails;

@RecordAware
@PublishAware
@AutoConfig
@Searchable
@Entity
@Table(name = "common_setting")
@Richtable(searchable = true, order = "key asc", exportable = true, importable = true)
public class Setting extends BaseEntity implements Recordable<UserDetails> {

	private static final long serialVersionUID = -8352037603261222984L;

	@UiConfig(displayOrder = 1, width = "300px")
	@SearchableProperty(boost = 3)
	@CaseInsensitive
	@NaturalId(mutable = true)
	@Column(name = "`key`", nullable = false)
	private String key = "";

	@UiConfig(displayOrder = 2, type = "textarea", width = "400px")
	@SearchableProperty
	@Column(length = 4000)
	private String value = "";

	@UiConfig(displayOrder = 3, type = "textarea")
	@SearchableProperty
	@Column(length = 4000)
	private String description = "";

	@NotInCopy
	@UiConfig(hidden = true)
	@Column(updatable = false)
	private Date createDate = new Date();

	@NotInCopy
	@UiConfig(hidden = true)
	@Column(insertable = false)
	private Date modifyDate;

	@NotInCopy
	@UiConfig(hidden = true)
	@Column(updatable = false)
	private String createUser;

	@NotInCopy
	@UiConfig(hidden = true)
	@Column(insertable = false)
	private String modifyUser;

	@Version
	private int version = -1;

	public Setting() {

	}

	public Setting(String key, String value) {
		this.key = key;
		this.value = value;
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	@Override
	public Date getCreateDate() {
		return createDate;
	}

	@Override
	public void setCreateDate(Date createDate) {
		this.createDate = createDate;
	}

	@Override
	public Date getModifyDate() {
		return modifyDate;
	}

	@Override
	public void setModifyDate(Date modifyDate) {
		this.modifyDate = modifyDate;
	}

	public String getCreateUser() {
		return createUser;
	}

	public void setCreateUser(String createUser) {
		this.createUser = createUser;
	}

	public String getModifyUser() {
		return modifyUser;
	}

	public void setModifyUser(String modifyUser) {
		this.modifyUser = modifyUser;
	}

	@Override
	public void setCreateUserDetails(UserDetails user) {
		if (user != null)
			createUser = user.getUsername();
	}

	@Override
	public void setModifyUserDetails(UserDetails user) {
		if (user != null)
			modifyUser = user.getUsername();
	}

	public int getVersion() {
		return version;
	}

	public void setVersion(int version) {
		this.version = version;
	}

}
