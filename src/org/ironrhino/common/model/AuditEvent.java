package org.ironrhino.common.model;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;

import org.ironrhino.core.metadata.Authorize;
import org.ironrhino.core.metadata.AutoConfig;
import org.ironrhino.core.metadata.Readonly;
import org.ironrhino.core.metadata.Richtable;
import org.ironrhino.core.metadata.UiConfig;
import org.ironrhino.core.model.BaseEntity;
import org.ironrhino.core.search.elasticsearch.annotations.Index;
import org.ironrhino.core.search.elasticsearch.annotations.Searchable;
import org.ironrhino.core.search.elasticsearch.annotations.SearchableProperty;
import org.ironrhino.core.security.event.AbstractEvent;
import org.ironrhino.core.security.role.UserRole;

@AutoConfig
@Authorize(ifAnyGranted = UserRole.ROLE_ADMINISTRATOR)
@Entity
@Searchable
@Richtable(searchable = true, order = "date desc", readonly = @Readonly(true))
public class AuditEvent extends BaseEntity {

	private static final long serialVersionUID = -7691080078972338500L;

	@UiConfig(width = "100px")
	@Column(nullable = false)
	@SearchableProperty(index = Index.NOT_ANALYZED)
	private String username;

	@UiConfig(width = "100px")
	@SearchableProperty
	private String address;

	@UiConfig(template = "${value}<#if value?ends_with('Event')> | ${action.getText(value)}</#if>")
	@SearchableProperty
	@Column(name = "`event`", nullable = false)
	private String event;

	@UiConfig(width = "150px")
	@Column(name = "`date`")
	private Date date = new Date();

	public AuditEvent() {

	}

	public AuditEvent(String username, String address, String event) {
		this.username = username;
		this.address = address;
		this.event = event;
	}

	public AuditEvent(AbstractEvent abstractEvent) {
		this.username = abstractEvent.getUsername();
		this.address = abstractEvent.getRemoteAddr();
		this.event = abstractEvent.getClass().getName();
		this.date = new Date(abstractEvent.getTimestamp());
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getAddress() {
		return address;
	}

	public void setAddress(String address) {
		this.address = address;
	}

	public String getEvent() {
		return event;
	}

	public void setEvent(String event) {
		this.event = event;
	}

	public Date getDate() {
		return date;
	}

	public void setDate(Date date) {
		this.date = date;
	}

}
