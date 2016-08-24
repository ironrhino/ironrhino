package org.ironrhino.core.model;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.ironrhino.core.hibernate.CreationUser;
import org.ironrhino.core.hibernate.UpdateUser;
import org.ironrhino.core.metadata.NotInCopy;
import org.ironrhino.core.metadata.UiConfig;
import org.ironrhino.core.search.elasticsearch.annotations.SearchableProperty;

import com.fasterxml.jackson.annotation.JsonIgnore;

@MappedSuperclass
public class BaseRecordableEntity extends BaseEntity {

	private static final long serialVersionUID = 1566483362208525707L;

	@NotInCopy
	@JsonIgnore
	@SearchableProperty
	@UiConfig(hidden = true)
	@Column(updatable = false)
	@CreationTimestamp
	protected Date createDate;

	@NotInCopy
	@JsonIgnore
	@SearchableProperty
	@UiConfig(hidden = true)
	@Column(insertable = false)
	@UpdateTimestamp
	protected Date modifyDate;

	@NotInCopy
	@JsonIgnore
	@SearchableProperty(include_in_all = false)
	@UiConfig(hidden = true)
	@Column(updatable = false)
	@CreationUser
	protected String createUser;

	@NotInCopy
	@JsonIgnore
	@SearchableProperty(include_in_all = false)
	@UiConfig(hidden = true)
	@Column(insertable = false)
	@UpdateUser
	protected String modifyUser;

	public Date getCreateDate() {
		return createDate;
	}

	public void setCreateDate(Date createDate) {
		this.createDate = createDate;
	}

	public Date getModifyDate() {
		return modifyDate;
	}

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

}
