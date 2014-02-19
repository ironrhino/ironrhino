package org.ironrhino.common.model;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

import org.ironrhino.core.metadata.AutoConfig;
import org.ironrhino.core.metadata.Hidden;
import org.ironrhino.core.metadata.Readonly;
import org.ironrhino.core.metadata.Richtable;
import org.ironrhino.core.metadata.UiConfig;
import org.ironrhino.core.model.BaseEntity;
import org.ironrhino.core.model.Recordable;
import org.ironrhino.core.search.elasticsearch.annotations.Searchable;
import org.ironrhino.core.search.elasticsearch.annotations.SearchableProperty;
import org.springframework.security.core.userdetails.UserDetails;

@AutoConfig
@Searchable
@Entity
@Table(name = "common_feedback")
@Richtable(searchable = true, order = "createDate desc")
public class Feedback extends BaseEntity implements Recordable<UserDetails> {

	private static final long serialVersionUID = 7857273372050062349L;

	@SearchableProperty(boost = 3)
	@UiConfig(displayOrder = 1, hiddenInList = @Hidden(true))
	private String name;

	@SearchableProperty(boost = 3)
	@UiConfig(displayOrder = 2, hiddenInList = @Hidden(true))
	private String contact;

	@SearchableProperty(boost = 3)
	@UiConfig(displayOrder = 3, type = "textarea")
	@Column(length = 4000)
	private String content;

	@SearchableProperty(boost = 3)
	@UiConfig(displayOrder = 4, hiddenInList = @Hidden(true), type = "textarea")
	@Column(name = "`comment`", length = 4000)
	private String comment;

	@UiConfig(displayOrder = 5, width = "100px")
	private String domain;

	@UiConfig(readonly = @Readonly(true), width = "150px")
	@Column(updatable = false)
	private Date createDate;

	@UiConfig(readonly = @Readonly(true), width = "150px")
	@Column(insertable = false)
	private Date modifyDate;

	@UiConfig(readonly = @Readonly(true), width = "80px")
	@Column(insertable = false)
	private String modifyUser;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getContact() {
		return contact;
	}

	public void setContact(String contact) {
		this.contact = contact;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public String getComment() {
		return comment;
	}

	public void setComment(String comment) {
		this.comment = comment;
	}

	public String getDomain() {
		return domain;
	}

	public void setDomain(String domain) {
		this.domain = domain;
	}

	@Override
	public Date getModifyDate() {
		return modifyDate;
	}

	@Override
	public Date getCreateDate() {
		return createDate;
	}

	@Override
	public void setModifyDate(Date date) {
		this.modifyDate = date;

	}

	@Override
	public void setCreateDate(Date date) {
		this.createDate = date;

	}

	public String getModifyUser() {
		return modifyUser;
	}

	public void setModifyUser(String modifyUser) {
		this.modifyUser = modifyUser;
	}

	@Override
	public void setCreateUserDetails(UserDetails userDetails) {

	}

	@Override
	public void setModifyUserDetails(UserDetails userDetails) {
		this.modifyUser = userDetails.getUsername();

	}

}
