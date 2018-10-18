package org.ironrhino.security.action;

import java.beans.PropertyDescriptor;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.struts2.ServletActionContext;
import org.hibernate.criterion.DetachedCriteria;
import org.ironrhino.core.event.EventPublisher;
import org.ironrhino.core.hibernate.CriteriaState;
import org.ironrhino.core.hibernate.CriterionUtils;
import org.ironrhino.core.metadata.Authorize;
import org.ironrhino.core.metadata.JsonConfig;
import org.ironrhino.core.metadata.Scope;
import org.ironrhino.core.model.LabelValue;
import org.ironrhino.core.model.Persistable;
import org.ironrhino.core.security.event.ProfileEditedEvent;
import org.ironrhino.core.security.role.UserRole;
import org.ironrhino.core.security.role.UserRoleFilter;
import org.ironrhino.core.security.role.UserRoleManager;
import org.ironrhino.core.struts.EntityAction;
import org.ironrhino.core.util.AuthzUtils;
import org.ironrhino.core.util.BeanUtils;
import org.ironrhino.security.model.User;
import org.ironrhino.security.service.UserManager;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import com.opensymphony.xwork2.interceptor.annotations.InputConfig;
import com.opensymphony.xwork2.validator.annotations.EmailValidator;
import com.opensymphony.xwork2.validator.annotations.RegexFieldValidator;
import com.opensymphony.xwork2.validator.annotations.RequiredStringValidator;
import com.opensymphony.xwork2.validator.annotations.Validations;
import com.opensymphony.xwork2.validator.annotations.ValidatorType;

import lombok.Getter;
import lombok.Setter;

@Authorize(ifAnyGranted = UserRole.ROLE_ADMINISTRATOR)
public class UserAction extends EntityAction<User> {

	private static final long serialVersionUID = -79191921685741502L;

	@Getter
	@Setter
	private User user;

	@Getter
	private List<LabelValue> roles;

	@Getter
	private Set<String> hiddenRoles;

	@Getter
	@Value("${user.profile.readonly:false}")
	private boolean userProfileReadonly;

	@Autowired
	private UserManager userManager;

	@Autowired
	private UserRoleManager userRoleManager;

	@Autowired(required = false)
	private UserRoleFilter userRoleFilter;

	@Autowired
	protected EventPublisher eventPublisher;

	@Override
	protected void prepare(DetachedCriteria dc, CriteriaState criteriaState) {
		String role = ServletActionContext.getRequest().getParameter("role");
		if (StringUtils.isNotBlank(role))
			dc.add(CriterionUtils.matchTag("roles", role));
	}

	@Override
	public String input() {
		String id = getUid();
		if (StringUtils.isNotBlank(id)) {
			user = userManager.get(id);
			if (user == null)
				user = userManager.findByNaturalId(id);
		}
		if (user == null) {
			user = new User();
		}
		Map<String, String> map = userRoleManager.getAllRoles(true);
		if (userRoleFilter != null) {
			Map<String, String> temp = userRoleFilter.filter(user, map);
			if (temp != null)
				map = temp;
		}
		roles = new ArrayList<>(map.size());
		for (Map.Entry<String, String> entry : map.entrySet())
			roles.add(new LabelValue(
					StringUtils.isNotBlank(entry.getValue()) ? entry.getValue() : getText(entry.getKey()),
					entry.getKey()));
		if (!user.isNew()) {
			Set<String> userRoles = user.getRoles();
			for (String r : userRoles) {
				if (!map.containsKey(r)) {
					if (hiddenRoles == null)
						hiddenRoles = new LinkedHashSet<>();
					hiddenRoles.add(r);
				}
			}
		}
		return INPUT;
	}

	@Override
	@Validations(requiredStrings = {
			@RequiredStringValidator(type = ValidatorType.FIELD, fieldName = "user.username", trim = true, key = "validation.required"),
			@RequiredStringValidator(type = ValidatorType.FIELD, fieldName = "user.name", trim = true, key = "validation.required") }, emails = {
					@EmailValidator(fieldName = "user.email", key = "validation.invalid") }, regexFields = {
							@RegexFieldValidator(type = ValidatorType.FIELD, fieldName = "user.username", regex = User.USERNAME_REGEX, key = "validation.invalid") })
	public String save() {
		if (!makeEntityValid())
			return INPUT;
		int previousVersion = user.getVersion();
		userManager.save(user);
		int currentVersion = user.getVersion();
		if (currentVersion != previousVersion)
			ServletActionContext.getResponse().addHeader("X-Postback", "user.version=" + currentVersion);
		notify("save.success");
		return SUCCESS;
	}

	@Override
	@Validations(regexFields = {
			@RegexFieldValidator(type = ValidatorType.FIELD, fieldName = "user.username", regex = User.USERNAME_REGEX, key = "validation.invalid") })
	public String checkavailable() {
		makeEntityValid();
		return JSON;
	}

	@Override
	protected boolean makeEntityValid() {
		if (user == null) {
			addActionError(getText("access.denied"));
			return false;
		}
		if (user.isNew()) {
			if (StringUtils.isNotBlank(user.getUsername())) {
				user.setUsername(user.getUsername().toLowerCase(Locale.ROOT));
				if (userManager.existsOne(true, new Serializable[] { "username", user.getUsername() })) {
					addFieldError("user.username", getText("validation.already.exists"));
					return false;
				}
			}
			if (StringUtils.isNotBlank(user.getEmail())
					&& userManager.existsOne(true, new Serializable[] { "email", user.getEmail() })) {
				addFieldError("user.email", getText("validation.already.exists"));
				return false;
			}
		} else {
			User temp = user;
			user = userManager.get(temp.getId());
			if (StringUtils.isNotBlank(temp.getEmail()) && !temp.getEmail().equals(user.getEmail())
					&& userManager.existsOne(true, new Serializable[] { "email", temp.getEmail() })) {
				addFieldError("user.email", getText("validation.already.exists"));
				return false;
			}
			BeanUtils.copyProperties(temp, user);
			int versionInDb = user.getVersion();
			int versionInUi = temp.getVersion();
			if (versionInUi > -1 && versionInUi != versionInDb) {
				addActionError(getText("validation.version.conflict"));
				return false;
			}
		}
		try {
			userRoleManager.checkMutex(user.getRoles());
		} catch (Exception e) {
			addFieldError("user.roles", e.getLocalizedMessage());
			return false;
		}
		BeanWrapperImpl bw = new BeanWrapperImpl(user);
		PropertyDescriptor[] pds = bw.getPropertyDescriptors();
		for (PropertyDescriptor pd : pds) {
			if (pd.getReadMethod() == null || pd.getWriteMethod() == null)
				continue;
			String name = pd.getName();
			Object value = bw.getPropertyValue(name);
			if (value instanceof Persistable) {
				if (((Persistable<?>) value).isNew()) {
					bw.setPropertyValue(name, null);
				}
			}
		}
		return true;
	}

	public String resetPassword() {
		User user = userManager.get(getUid());
		if (user == null)
			return NONE;
		userManager.resetPassword(user);
		notify("operate.success");
		return SUCCESS;
	}

	@Authorize(ifAnyGranted = UserRole.ROLE_BUILTIN_USER)
	@InputConfig(methodName = "inputprofile")
	@Validations(requiredStrings = {
			@RequiredStringValidator(type = ValidatorType.FIELD, fieldName = "user.name", trim = true, key = "validation.required") }, emails = {
					@EmailValidator(fieldName = "user.email", key = "validation.invalid") })
	public String profile() {
		if (userProfileReadonly) {
			addActionError(getText("access.denied"));
			return ACCESSDENIED;
		}
		User temp = user;
		User user = AuthzUtils.getUserDetails();
		if (StringUtils.isNotBlank(temp.getEmail()) && !temp.getEmail().equals(user.getEmail())
				&& userManager.existsOne(true, new Serializable[] { "email", temp.getEmail() })) {
			addFieldError("user.email", getText("validation.already.exists"));
			return inputprofile();
		}
		user.setName(temp.getName());
		user.setEmail(temp.getEmail());
		user.setPhone(temp.getPhone());
		userManager.save(user);
		notify("save.success");
		eventPublisher.publish(
				new ProfileEditedEvent(user.getUsername(), ServletActionContext.getRequest().getRemoteAddr()),
				Scope.LOCAL);
		return "profile";
	}

	@Authorize(ifAnyGranted = UserRole.ROLE_BUILTIN_USER)
	public String inputprofile() {
		user = AuthzUtils.getUserDetails();
		user = userManager.get(user.getId());
		return "profile";
	}

	@JsonConfig(root = "user")
	@Authorize(ifAnyGranted = UserRole.ROLE_BUILTIN_USER)
	public String self() {
		user = AuthzUtils.getUserDetails();
		return JSON;
	}

	@JsonConfig(root = "roles")
	@Authorize(ifAnyGranted = UserRole.ROLE_BUILTIN_USER)
	public String roles() {
		Map<String, String> map = userRoleManager
				.getAllRoles(ServletActionContext.getRequest().getParameter("excludeBuiltin") != null);
		roles = new ArrayList<>(map.size());
		for (Map.Entry<String, String> entry : map.entrySet())
			roles.add(new LabelValue(
					StringUtils.isNotBlank(entry.getValue()) ? entry.getValue() : getText(entry.getKey()),
					entry.getKey()));
		return JSON;
	}

	@Override
	@Authorize(ifAnyGranted = UserRole.ROLE_BUILTIN_USER)
	public String pick() throws Exception {
		return super.pick();
	}

}
