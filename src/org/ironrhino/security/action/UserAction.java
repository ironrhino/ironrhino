package org.ironrhino.security.action;

import java.beans.PropertyDescriptor;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.struts2.ServletActionContext;
import org.hibernate.criterion.DetachedCriteria;
import org.ironrhino.core.event.EventPublisher;
import org.ironrhino.core.hibernate.CriteriaState;
import org.ironrhino.core.hibernate.CriterionUtils;
import org.ironrhino.core.metadata.Authorize;
import org.ironrhino.core.metadata.CurrentPassword;
import org.ironrhino.core.metadata.JsonConfig;
import org.ironrhino.core.metadata.Scope;
import org.ironrhino.core.model.LabelValue;
import org.ironrhino.core.model.Persistable;
import org.ironrhino.core.security.role.UserRole;
import org.ironrhino.core.security.role.UserRoleFilter;
import org.ironrhino.core.security.role.UserRoleManager;
import org.ironrhino.core.struts.EntityAction;
import org.ironrhino.core.util.AuthzUtils;
import org.ironrhino.core.util.BeanUtils;
import org.ironrhino.security.event.PasswordChangedEvent;
import org.ironrhino.security.event.ProfileEditedEvent;
import org.ironrhino.security.model.User;
import org.ironrhino.security.service.UserManager;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import com.opensymphony.xwork2.interceptor.annotations.InputConfig;
import com.opensymphony.xwork2.validator.annotations.EmailValidator;
import com.opensymphony.xwork2.validator.annotations.ExpressionValidator;
import com.opensymphony.xwork2.validator.annotations.FieldExpressionValidator;
import com.opensymphony.xwork2.validator.annotations.RegexFieldValidator;
import com.opensymphony.xwork2.validator.annotations.RequiredStringValidator;
import com.opensymphony.xwork2.validator.annotations.Validations;
import com.opensymphony.xwork2.validator.annotations.ValidatorType;

@Authorize(ifAnyGranted = UserRole.ROLE_ADMINISTRATOR)
public class UserAction extends EntityAction<User> {

	private static final long serialVersionUID = -79191921685741502L;

	private User user;

	private List<LabelValue> roles;

	private Set<String> hiddenRoles;

	private String password;

	private String confirmPassword;

	@Value("${user.profile.readonly:false}")
	private boolean userProfileReadonly;

	@Value("${user.password.readonly:false}")
	private boolean userPasswordReadonly;

	@Autowired
	private transient UserManager userManager;

	@Autowired
	private transient UserRoleManager userRoleManager;

	@Autowired(required = false)
	private transient UserRoleFilter userRoleFilter;

	@Autowired
	protected transient EventPublisher eventPublisher;

	public List<LabelValue> getRoles() {
		return roles;
	}

	public Set<String> getHiddenRoles() {
		return hiddenRoles;
	}

	public String getConfirmPassword() {
		return confirmPassword;
	}

	public void setConfirmPassword(String confirmPassword) {
		this.confirmPassword = confirmPassword;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public User getUser() {
		return user;
	}

	public void setUser(User user) {
		this.user = user;
	}

	public boolean isUserProfileReadonly() {
		return userProfileReadonly;
	}

	public boolean isUserPasswordReadonly() {
		return userPasswordReadonly;
	}

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
							@RegexFieldValidator(type = ValidatorType.FIELD, fieldName = "user.username", regex = User.USERNAME_REGEX, key = "validation.invalid") }, fieldExpressions = {
									@FieldExpressionValidator(expression = "password == confirmPassword", fieldName = "confirmPassword", key = "validation.repeat.not.matched") })
	public String save() {
		if (!makeEntityValid())
			return INPUT;
		userManager.save(user);
		addActionMessage(getText("save.success"));
		return SUCCESS;
	}

	@Override
	@Validations(regexFields = {
			@RegexFieldValidator(type = ValidatorType.FIELD, fieldName = "user.username", regex = User.USERNAME_REGEX, key = "validation.invalid") })
	public String checkavailable() {
		return makeEntityValid() ? NONE : INPUT;
	}

	@Override
	protected boolean makeEntityValid() {
		if (user == null) {
			addActionError(getText("access.denied"));
			return false;
		}
		if (user.isNew()) {
			if (StringUtils.isNotBlank(user.getUsername())) {
				user.setUsername(user.getUsername().toLowerCase());
				if (userManager.findByNaturalId(user.getUsername()) != null) {
					addFieldError("user.username", getText("validation.already.exists"));
					return false;
				}
			}
			if (StringUtils.isNotBlank(user.getEmail()) && userManager.findOne("email", user.getEmail()) != null) {
				addFieldError("user.email", getText("validation.already.exists"));
				return false;
			}
			user.setLegiblePassword(password);
		} else {
			User temp = user;
			if (temp.getId() != null) {
				user = userManager.get(temp.getId());
			}
			if (temp.getUsername() != null) {
				user = userManager.findByNaturalId(temp.getUsername());
			}
			if (StringUtils.isNotBlank(temp.getEmail()) && !temp.getEmail().equals(user.getEmail())
					&& userManager.findOne("email", temp.getEmail()) != null) {
				addFieldError("user.email", getText("validation.already.exists"));
				return false;
			}
			BeanUtils.copyProperties(temp, user);
			if (StringUtils.isNotBlank(password))
				user.setLegiblePassword(password);
			userManager.evict(user);
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

	@Override
	public String delete() {
		String[] id = getId();
		if (id != null) {
			userManager.delete((Serializable[]) id);
			addActionMessage(getText("delete.success"));
		}
		return SUCCESS;
	}

	@Authorize(ifAnyGranted = UserRole.ROLE_BUILTIN_USER)
	@InputConfig(resultName = "password")
	@CurrentPassword
	@Validations(requiredStrings = {
			@RequiredStringValidator(type = ValidatorType.FIELD, trim = true, fieldName = "password", key = "validation.required") }, expressions = {
					@ExpressionValidator(expression = "password == confirmPassword", key = "validation.repeat.not.matched") })
	public String password() {
		if (userPasswordReadonly) {
			addActionError(getText("access.denied"));
			return ACCESSDENIED;
		}
		User user = AuthzUtils.getUserDetails();
		if (user != null) {
			user.setLegiblePassword(password);
			userManager.save(user);
			addActionMessage(getText("save.success"));
			eventPublisher.publish(
					new PasswordChangedEvent(user.getUsername(), ServletActionContext.getRequest().getRemoteAddr()),
					Scope.LOCAL);
		}
		return "password";
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
		user.setId(AuthzUtils.<User>getUserDetails().getId());
		if (!makeEntityValid())
			return INPUT;
		User userInSession = AuthzUtils.getUserDetails();
		if (userInSession == null || user == null) {
			return "profile";
		}
		userInSession.setName(user.getName());
		userInSession.setEmail(user.getEmail());
		userInSession.setPhone(user.getPhone());
		userManager.save(userInSession);
		addActionMessage(getText("save.success"));
		eventPublisher.publish(
				new ProfileEditedEvent(user.getUsername(), ServletActionContext.getRequest().getRemoteAddr()),
				Scope.LOCAL);
		return "profile";
	}

	@Authorize(ifAnyGranted = UserRole.ROLE_BUILTIN_USER)
	public String inputprofile() {
		user = AuthzUtils.getUserDetails();
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
