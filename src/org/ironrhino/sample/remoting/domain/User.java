package org.ironrhino.sample.remoting.domain;

import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;

import org.apache.commons.lang3.StringUtils;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

public class User implements UserDetails {

	private static final long serialVersionUID = -6135434863820342822L;

	private String username;

	private String password;

	private String name;

	private String email;

	private String phone;

	private boolean enabled = true;

	private Date accountExpireDate;

	private Date passwordModifyDate;

	private boolean passwordExpired;

	private Date createDate = new Date();

	private Collection<GrantedAuthority> authorities;

	private Map<String, String> attributes;

	private Date modifyDate;

	private String createUser;

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
	public boolean isEnabled() {
		return enabled;
	}

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

	public Date getCreateDate() {
		return createDate;
	}

	public void setCreateDate(Date createDate) {
		this.createDate = createDate;
	}

	public String getCreateUser() {
		return createUser;
	}

	public void setCreateUser(String createUser) {
		this.createUser = createUser;
	}

	public Date getModifyDate() {
		return modifyDate;
	}

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
	public Collection<GrantedAuthority> getAuthorities() {
		return authorities;
	}

	public void setAuthorities(Collection<GrantedAuthority> authorities) {
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
