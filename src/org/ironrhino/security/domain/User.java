package org.ironrhino.security.domain;

import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.validation.constraints.Email;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import lombok.Data;

@Data
public class User implements UserDetails {

	private static final long serialVersionUID = -6135434863820342822L;

	private String username;

	private String password;

	private String name;

	@Email
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
	public boolean isAccountNonExpired() {
		return accountExpireDate == null || accountExpireDate.after(new Date());
	}

	@Override
	public boolean isAccountNonLocked() {
		return true;
	}

	@Override
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

	@Override
	public String toString() {
		return this.name != null && !this.name.isEmpty() ? this.name : this.username;
	}

}
