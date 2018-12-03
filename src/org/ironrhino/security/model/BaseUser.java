package org.ironrhino.security.model;

import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Column;
import javax.persistence.MappedSuperclass;
import javax.persistence.Transient;
import javax.validation.constraints.Email;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.hibernate.annotations.NaturalId;
import org.ironrhino.core.metadata.CaseInsensitive;
import org.ironrhino.core.metadata.NotInCopy;
import org.ironrhino.core.metadata.UiConfig;
import org.ironrhino.core.model.BaseRecordableEntity;
import org.ironrhino.core.model.Enableable;
import org.ironrhino.core.model.Persistable;
import org.ironrhino.core.search.elasticsearch.annotations.Index;
import org.ironrhino.core.search.elasticsearch.annotations.SearchableProperty;
import org.ironrhino.core.security.role.RoledUserDetails;
import org.ironrhino.core.service.BaseManager;
import org.ironrhino.core.util.ApplicationContextUtils;
import org.ironrhino.core.util.AuthzUtils;
import org.ironrhino.core.validation.constraints.MobilePhoneNumber;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.util.ClassUtils;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;

import lombok.Getter;
import lombok.Setter;

@MappedSuperclass
@Getter
@Setter
public class BaseUser extends BaseRecordableEntity implements RoledUserDetails, Enableable {

	private static final long serialVersionUID = -6135434863820342822L;

	public static final String USERNAME_REGEX = "^[\\w\\(\\)]{1,40}$";

	public static final String USERNAME_REGEX_FOR_SIGNUP = "^\\w{3,20}$";

	@SearchableProperty(boost = 5, index = Index.NOT_ANALYZED)
	@NotInCopy
	@CaseInsensitive
	@NaturalId
	@Column(nullable = false)
	@UiConfig(width = "120px")
	private String username;

	@NotInCopy
	@Column(nullable = false)
	@UiConfig(hidden = true, excludedFromLike = true, excludedFromCriteria = true)
	private String password;

	@SearchableProperty(boost = 3)
	@UiConfig(width = "120px")
	private String name;

	@Email
	@SearchableProperty(boost = 3)
	@Column(unique = true)
	@UiConfig(width = "180px")
	private String email;

	@MobilePhoneNumber
	@SearchableProperty
	@UiConfig(width = "120px")
	private String phone;

	@JsonIgnore
	@UiConfig(width = "80px", displayOrder = 100)
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
	private int passwordExpiresInDays;

	@NotInCopy
	@JsonIgnore
	@Transient
	@UiConfig(hidden = true)
	private Collection<? extends GrantedAuthority> authorities = Collections.emptyList();

	@SearchableProperty
	@Column(length = 4000)
	@UiConfig(displayOrder = 99, type = "multiselect", cssClass = "input-xxlarge chosen", listOptions = "beans['userRoleManager'].getAllRoles(true)", listKey = "key", listValue = "value", template = "<#list beans['userRoleManager'].displayRoles(value) as role>${action.getText(role)}&nbsp;&nbsp;</#list>", csvTemplate = "<#list value as r>${beans['userRoleManager'].displayRole(r)}<#sep>,</#list>")
	private Set<String> roles = new LinkedHashSet<>(0);

	@NotInCopy
	@JsonIgnore
	@Column(length = 4000)
	@UiConfig(hidden = true)
	private Map<String, String> attributes;

	@Override
	@JsonIgnore
	public String getPassword() {
		return password;
	}

	@JsonProperty
	public void setPassword(String password) {
		this.password = password;
	}

	public void setName(String name) {
		name = StringUtils.isBlank(name) ? null : name;
		this.name = name;
	}

	public void setEmail(String email) {
		email = StringUtils.isBlank(email) ? null : email;
		if (email != null && email.endsWith("@gmail.com")) {
			String name = email.substring(0, email.indexOf('@'));
			if (name.indexOf('+') > 0)
				name = name.substring(0, name.indexOf('+'));
			name = name.replaceAll("\\.", "");
			email = name + "@gmail.com";
		}
		this.email = email;
	}

	public void setPhone(String phone) {
		phone = StringUtils.isBlank(phone) ? null : phone;
		this.phone = phone;
	}

	@Override
	@JsonIgnore
	@Access(AccessType.PROPERTY)
	public Set<String> getRoles() {
		return roles;
	}

	@JsonSetter
	public void setRoles(Set<String> roles) {
		if (roles != null)
			this.roles = roles;
	}

	@JsonProperty("roles")
	public Set<String> getRolesForApi() {
		return AuthorityUtils.authorityListToSet(authorities);
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
		if (passwordModifyDate != null && passwordModifyDate.getTime() == 0)
			return false; // reset password
		return passwordModifyDate == null || passwordExpiresInDays <= 0
				|| DateUtils.addDays(passwordModifyDate, passwordExpiresInDays).after(new Date());
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
			attributes = new HashMap<String, String>(4);
		if (value == null)
			attributes.remove(key);
		else
			attributes.put(key, value);
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

}
