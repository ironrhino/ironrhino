package org.ironrhino.security.model;

import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.annotations.NaturalId;
import org.ironrhino.core.metadata.AutoConfig;
import org.ironrhino.core.metadata.CaseInsensitive;
import org.ironrhino.core.metadata.NotInCopy;
import org.ironrhino.core.metadata.NotInJson;
import org.ironrhino.core.metadata.Richtable;
import org.ironrhino.core.metadata.UiConfig;
import org.ironrhino.core.model.BaseEntity;
import org.ironrhino.core.model.Enableable;
import org.ironrhino.core.model.Persistable;
import org.ironrhino.core.model.Recordable;
import org.ironrhino.core.search.elasticsearch.annotations.Index;
import org.ironrhino.core.search.elasticsearch.annotations.Searchable;
import org.ironrhino.core.search.elasticsearch.annotations.SearchableProperty;
import org.ironrhino.core.service.BaseManager;
import org.ironrhino.core.spring.configuration.ClassPresentConditional;
import org.ironrhino.core.util.ApplicationContextUtils;
import org.ironrhino.core.util.AuthzUtils;
import org.ironrhino.core.util.JsonUtils;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.util.ClassUtils;

@AutoConfig
@Searchable
@Entity
@Table(name = "`user`")
@Richtable(order = "username asc")
@ClassPresentConditional("org.ironrhino.security.service.UserManagerImpl")
public class User extends BaseEntity implements UserDetails, Recordable<User>,
		Enableable {

	private static final long serialVersionUID = -6135434863820342822L;

	public static final String USERNAME_REGEX = "^[\\w\\(\\)]{1,40}$";

	public static final String USERNAME_REGEX_FOR_SIGNUP = "^\\w{3,20}$";

	@SearchableProperty(boost = 5, index = Index.NOT_ANALYZED)
	@NotInCopy
	@CaseInsensitive
	@NaturalId
	@Column(nullable = false)
	@UiConfig(displayOrder = 1)
	private String username;

	@NotInCopy
	@NotInJson
	@Column(nullable = false)
	@UiConfig(displayOrder = 2, excludedFromLike = true, excludedFromCriteria = true)
	private String password;

	@SearchableProperty(boost = 3, index = Index.NOT_ANALYZED)
	@UiConfig(displayOrder = 3)
	private String name;

	@SearchableProperty(boost = 3)
	@Column(unique = true)
	@UiConfig(displayOrder = 4)
	private String email;

	@SearchableProperty
	@NotInJson
	@UiConfig(displayOrder = 5)
	private String phone;

	@NotInJson
	@UiConfig(displayOrder = 6)
	private boolean enabled = true;

	@NotInCopy
	@NotInJson
	@UiConfig(hidden = true)
	@Column(updatable = false)
	private Date createDate = new Date();

	@NotInCopy
	@NotInJson
	@Transient
	private Collection<GrantedAuthority> authorities;

	@SearchableProperty
	@Transient
	private Set<String> roles = new HashSet<String>(0);

	@NotInCopy
	@NotInJson
	@Transient
	private Map<String, String> attributes;

	@NotInCopy
	@NotInJson
	@UiConfig(hidden = true)
	@Column(insertable = false)
	private Date modifyDate;

	@NotInCopy
	@NotInJson
	@UiConfig(hidden = true)
	@Column(updatable = false)
	private String createUser;

	@NotInCopy
	@NotInJson
	@UiConfig(hidden = true)
	@Column(insertable = false)
	private String modifyUser;

	@Override
	public Collection<GrantedAuthority> getAuthorities() {
		return authorities;
	}

	@Override
	public Date getCreateDate() {
		return createDate;
	}

	public String getCreateUser() {
		return createUser;
	}

	public String getEmail() {
		return email;
	}

	@Override
	public Date getModifyDate() {
		return modifyDate;
	}

	public String getModifyUser() {
		return modifyUser;
	}

	public String getName() {
		return name;
	}

	@Override
	public String getPassword() {
		return password;
	}

	public String getPhone() {
		return phone;
	}

	public Set<String> getRoles() {
		return roles;
	}

	@NotInCopy
	@NotInJson
	@Column(name = "roles", length = 4000)
	@Access(AccessType.PROPERTY)
	public String getRolesAsString() {
		if (roles.size() > 0)
			return StringUtils.join(roles.iterator(), ',');
		return null;
	}

	@Override
	public String getUsername() {
		return username;
	}

	@Override
	@NotInJson
	public boolean isAccountNonExpired() {
		return true;
	}

	@Override
	@NotInJson
	public boolean isAccountNonLocked() {
		return true;
	}

	@Override
	@NotInJson
	public boolean isCredentialsNonExpired() {
		return true;
	}

	@Override
	public boolean isEnabled() {
		return enabled;
	}

	public boolean isPasswordValid(String legiblePassword) {
		return AuthzUtils.isPasswordValid(this, legiblePassword);
	}

	public void setAuthorities(Collection<GrantedAuthority> authorities) {
		this.authorities = authorities;
	}

	@Override
	public void setCreateDate(Date createDate) {
		this.createDate = createDate;
	}

	public void setCreateUser(String createUser) {
		this.createUser = createUser;
	}

	@Override
	public void setCreateUserDetails(User createUser) {
		if (createUser != null)
			this.createUser = createUser.getUsername();
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

	@Override
	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public void setLegiblePassword(String legiblePassword) {
		this.password = AuthzUtils.encodePassword(this, legiblePassword);
	}

	@Override
	public void setModifyDate(Date modifyDate) {
		this.modifyDate = modifyDate;
	}

	public void setModifyUser(String modifyUser) {
		this.modifyUser = modifyUser;
	}

	@Override
	public void setModifyUserDetails(User modifyUser) {
		if (modifyUser != null)
			this.modifyUser = modifyUser.getUsername();
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public void setPhone(String phone) {
		this.phone = phone;
	}

	public void setRoles(Set<String> roles) {
		this.roles = roles;
	}

	public void setRolesAsString(String rolesAsString) {
		roles.clear();
		if (StringUtils.isNotBlank(rolesAsString))
			roles.addAll(Arrays.asList(org.ironrhino.core.util.StringUtils
					.trimTail(rolesAsString, ",").split("\\s*,\\s*")));
	}

	public void setUsername(String username) {
		this.username = username;
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

	public Map<String, String> getAttributes() {
		return attributes;
	}

	public void setAttributes(Map<String, String> attributes) {
		this.attributes = attributes;
	}

	@Column(name = "attributes", length = 4000)
	@Access(AccessType.PROPERTY)
	public String getAttributesAsString() {
		if (attributes == null || attributes.isEmpty())
			return null;
		return JsonUtils.toJson(attributes);
	}

	public void setAttributesAsString(String str) {
		if (StringUtils.isNotBlank(str))
			try {
				attributes = JsonUtils.fromJson(str, JsonUtils.STRING_MAP_TYPE);
			} catch (Exception e) {
				e.printStackTrace();
			}
	}

	public <T extends Persistable<?>> T getExtra(Class<T> clazz) {
		T extra = null;
		BaseManager<T> baseManager = ApplicationContextUtils
				.getEntityManager(clazz);
		if (baseManager != null)
			extra = baseManager.get(getId());
		return extra;
	}

	@SuppressWarnings("unchecked")
	public Persistable<?> getExtra(String className) {
		Class<? extends Persistable<?>> clazz = null;
		if (ClassUtils.isPresent(className, getClass().getClassLoader())) {
			try {
				clazz = (Class<? extends Persistable<?>>) Class
						.forName(className);
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
