package org.ironrhino.security.model;

import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.MappedSuperclass;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Transient;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.annotations.NaturalId;
import org.ironrhino.core.hibernate.convert.StringMapConverter;
import org.ironrhino.core.metadata.CaseInsensitive;
import org.ironrhino.core.metadata.NotInCopy;
import org.ironrhino.core.metadata.UiConfig;
import org.ironrhino.core.model.BaseEntity;
import org.ironrhino.core.model.Enableable;
import org.ironrhino.core.model.Persistable;
import org.ironrhino.core.model.Recordable;
import org.ironrhino.core.search.elasticsearch.annotations.Index;
import org.ironrhino.core.search.elasticsearch.annotations.SearchableProperty;
import org.ironrhino.core.security.role.RoledUserDetails;
import org.ironrhino.core.service.BaseManager;
import org.ironrhino.core.util.ApplicationContextUtils;
import org.ironrhino.core.util.AuthzUtils;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.util.ClassUtils;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;

@MappedSuperclass
public class BaseUser extends BaseEntity implements RoledUserDetails, Recordable<BaseUser>, Enableable {

	private static final long serialVersionUID = -6135434863820342822L;

	public static final String USERNAME_REGEX = "^[\\w\\(\\)]{1,40}$";

	public static final String USERNAME_REGEX_FOR_SIGNUP = "^\\w{3,20}$";

	@SearchableProperty(boost = 5, index = Index.NOT_ANALYZED)
	@NotInCopy
	@CaseInsensitive
	@NaturalId
	@Column(nullable = false)
	private String username;

	@NotInCopy
	@Column(nullable = false)
	@UiConfig(hidden = true, excludedFromLike = true, excludedFromCriteria = true)
	private String password;

	@SearchableProperty(boost = 3, index = Index.NOT_ANALYZED)
	private String name;

	@SearchableProperty(boost = 3)
	@Column(unique = true)
	private String email;

	@SearchableProperty
	private String phone;

	@JsonIgnore
	@UiConfig(displayOrder = 99)
	private boolean enabled = true;

	@NotInCopy
	@JsonIgnore
	@UiConfig(hidden = true)
	@Transient
	private Date accountExpireDate;

	@NotInCopy
	@JsonIgnore
	@UiConfig(hidden = true)
	private Date passwordModifyDate;

	@NotInCopy
	@JsonIgnore
	@UiConfig(hidden = true)
	@Transient
	private boolean passwordExpired;

	@NotInCopy
	@JsonIgnore
	@UiConfig(hidden = true)
	@Column(updatable = false)
	private Date createDate = new Date();

	@NotInCopy
	@JsonIgnore
	@Transient
	@UiConfig(hidden = true)
	private Collection<? extends GrantedAuthority> authorities;

	@SearchableProperty
	@Transient
	@UiConfig(displayOrder = 100, alias = "role", template = "<#list value as r>${statics['org.ironrhino.core.util.ApplicationContextUtils'].getBean('userRoleManager').displayRole(r)}<#if r_has_next> </#if></#list>", csvTemplate = "<#list value as r>${statics['org.ironrhino.core.util.ApplicationContextUtils'].getBean('userRoleManager').displayRole(r)}<#if r_has_next>,</#if></#list>")
	private Set<String> roles = new LinkedHashSet<>(0);

	@NotInCopy
	@JsonIgnore
	@Column(length = 4000)
	@Convert(converter = StringMapConverter.class)
	@UiConfig(hidden = true)
	private Map<String, String> attributes;

	@NotInCopy
	@JsonIgnore
	@UiConfig(hidden = true)
	@Column(insertable = false)
	private Date modifyDate;

	@NotInCopy
	@JsonIgnore
	@UiConfig(hidden = true)
	@Column(updatable = false)
	private String createUser;

	@NotInCopy
	@JsonIgnore
	@UiConfig(hidden = true)
	@Column(insertable = false)
	private String modifyUser;

	@Override
	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	@Override
	@JsonIgnore
	public String getPassword() {
		return password;
	}

	@JsonProperty
	public void setPassword(String password) {
		this.password = password;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		if (email != null && email.endsWith("@gmail.com")) {
			String name = email.substring(0, email.indexOf('@'));
			if (name.indexOf('+') > 0)
				name = name.substring(0, name.indexOf('+'));
			name = name.replaceAll("\\.", "");
			email = name + "@gmail.com";
		}
		this.email = email;
	}

	public String getPhone() {
		return phone;
	}

	public void setPhone(String phone) {
		this.phone = phone;
	}

	@Override
	@JsonIgnore
	public Set<String> getRoles() {
		return roles;
	}

	@NotInCopy
	@JsonIgnore
	@Column(name = "roles", length = 4000)
	@Access(AccessType.PROPERTY)
	@UiConfig(hidden = true)
	public String getRolesAsString() {
		if (roles.size() > 0)
			return StringUtils.join(roles.iterator(), ',');
		return null;
	}

	@JsonProperty("roles")
	public Set<String> getRolesForApi() {
		if (authorities == null)
			return null;
		Set<String> roles = new LinkedHashSet<>();
		for (GrantedAuthority ga : authorities)
			roles.add(ga.getAuthority());
		return roles;
	}

	@JsonSetter
	public void setRoles(Set<String> roles) {
		this.roles = roles;
	}

	public void setRolesAsString(String rolesAsString) {
		roles.clear();
		if (StringUtils.isNotBlank(rolesAsString))
			roles.addAll(
					Arrays.asList(org.ironrhino.core.util.StringUtils.trimTail(rolesAsString, ",").split("\\s*,\\s*")));
	}

	@Override
	public boolean isEnabled() {
		return enabled;
	}

	@Override
	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public Date getAccountExpireDate() {
		return accountExpireDate;
	}

	public void setAccountExpireDate(Date accountExpireDate) {
		this.accountExpireDate = accountExpireDate;
	}

	public Date getPasswordModifyDate() {
		return passwordModifyDate;
	}

	public void setPasswordModifyDate(Date passwordModifyDate) {
		this.passwordModifyDate = passwordModifyDate;
	}

	public boolean isPasswordExpired() {
		return passwordExpired;
	}

	public void setPasswordExpired(boolean passwordExpired) {
		this.passwordExpired = passwordExpired;
	}

	@Override
	public Date getCreateDate() {
		return createDate;
	}

	@Override
	public void setCreateDate(Date createDate) {
		this.createDate = createDate;
	}

	public String getCreateUser() {
		return createUser;
	}

	public void setCreateUser(String createUser) {
		this.createUser = createUser;
	}

	@Override
	public void setCreateUserDetails(BaseUser createUser) {
		if (createUser != null)
			this.createUser = createUser.getUsername();
	}

	@Override
	public Date getModifyDate() {
		return modifyDate;
	}

	@Override
	public void setModifyDate(Date modifyDate) {
		this.modifyDate = modifyDate;
	}

	public String getModifyUser() {
		return modifyUser;
	}

	public void setModifyUser(String modifyUser) {
		this.modifyUser = modifyUser;
	}

	@Override
	public void setModifyUserDetails(BaseUser modifyUser) {
		if (modifyUser != null)
			this.modifyUser = modifyUser.getUsername();
	}

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		return authorities;
	}

	public void setAuthorities(Collection<? extends GrantedAuthority> authorities) {
		this.authorities = authorities;
	}

	@Override
	@JsonIgnore
	public boolean isAccountNonExpired() {
		return accountExpireDate == null || accountExpireDate.after(new Date());
	}

	@Override
	@JsonIgnore
	public boolean isAccountNonLocked() {
		return true;
	}

	@Override
	@JsonIgnore
	public boolean isCredentialsNonExpired() {
		return !isPasswordExpired();
	}

	public boolean isPasswordValid(String legiblePassword) {
		return AuthzUtils.isPasswordValid(this, legiblePassword);
	}

	public void setLegiblePassword(String legiblePassword) {
		this.password = AuthzUtils.encodePassword(this, legiblePassword);
		passwordModifyDate = new Date();
	}

	public String getAttribute(String key) {
		if (attributes == null)
			return null;
		return attributes.get(key);
	}

	public void setAttribute(String key, String value) {
		if (attributes == null)
			attributes = new HashMap<>(4);
		if (value == null)
			attributes.remove(key);
		else
			attributes.put(key, value);
	}

	public Map<String, String> getAttributes() {
		return attributes;
	}

	public void setAttributes(Map<String, String> attributes) {
		this.attributes = attributes;
	}

	public <T extends Persistable<?>> T getExtra(Class<T> clazz) {
		T extra = null;
		BaseManager<T> baseManager = ApplicationContextUtils.getEntityManager(clazz);
		if (baseManager != null)
			extra = baseManager.get(getId());
		return extra;
	}

	@SuppressWarnings("unchecked")
	public Persistable<?> getExtra(String className) {
		Class<? extends Persistable<?>> clazz = null;
		if (ClassUtils.isPresent(className, getClass().getClassLoader())) {
			try {
				clazz = (Class<? extends Persistable<?>>) Class.forName(className);
			} catch (ClassNotFoundException e) {
				throw new IllegalArgumentException(className + " not found");
			}
		}
		return (clazz != null) ? getExtra(clazz) : null;
	}

	@Override
	public String toString() {
		return StringUtils.isNotBlank(this.name) ? this.name : this.username;
	}

	@PrePersist
	@PreUpdate
	public void replaceBlankWithNull() {
		if (StringUtils.isBlank(name))
			name = null;
		if (StringUtils.isBlank(email))
			email = null;
		if (StringUtils.isBlank(phone))
			phone = null;
	}

}
