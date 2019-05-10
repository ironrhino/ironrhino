package org.ironrhino.security.model;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Index;
import javax.persistence.Table;

import org.hibernate.annotations.CreationTimestamp;
import org.ironrhino.core.metadata.Authorize;
import org.ironrhino.core.metadata.AutoConfig;
import org.ironrhino.core.metadata.Readonly;
import org.ironrhino.core.metadata.Richtable;
import org.ironrhino.core.metadata.UiConfig;
import org.ironrhino.core.model.BaseEntity;
import org.ironrhino.core.search.elasticsearch.annotations.Searchable;
import org.ironrhino.core.search.elasticsearch.annotations.SearchableProperty;
import org.ironrhino.core.security.role.UserRole;

import lombok.Getter;
import lombok.Setter;

@AutoConfig
@Authorize(ifAnyGranted = { UserRole.ROLE_ADMINISTRATOR, UserRole.ROLE_AUDITOR })
@Entity
@Searchable
@Table(name = "loginrecord", indexes = @Index(columnList = "date desc"))
@Richtable(searchable = true, order = "date desc", readonly = @Readonly(true))
@Getter
@Setter
public class LoginRecord extends BaseEntity {

	private static final long serialVersionUID = -7691080078972338500L;

	@UiConfig(width = "100px")
	@Column(nullable = false)
	@SearchableProperty
	private String username;

	@UiConfig(width = "150px")
	@SearchableProperty
	private String address;

	@UiConfig(width = "80px")
	private boolean failed;

	@SearchableProperty
	private String cause;

	@UiConfig(width = "130px")
	@Column(name = "date")
	@CreationTimestamp
	private Date date;

}
