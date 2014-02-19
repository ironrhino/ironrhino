package org.ironrhino.security.action;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.struts2.interceptor.validation.SkipValidation;
import org.ironrhino.common.support.SettingControl;
import org.ironrhino.core.event.EventPublisher;
import org.ironrhino.core.mail.MailService;
import org.ironrhino.core.metadata.AutoConfig;
import org.ironrhino.core.metadata.Captcha;
import org.ironrhino.core.metadata.Redirect;
import org.ironrhino.core.metadata.Scope;
import org.ironrhino.core.security.util.Blowfish;
import org.ironrhino.core.spring.configuration.ClassPresentConditional;
import org.ironrhino.core.struts.BaseAction;
import org.ironrhino.core.util.AuthzUtils;
import org.ironrhino.core.util.CodecUtils;
import org.ironrhino.security.Constants;
import org.ironrhino.security.event.LoginEvent;
import org.ironrhino.security.event.SignupEvent;
import org.ironrhino.security.model.User;
import org.ironrhino.security.service.UserManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;

import com.opensymphony.xwork2.interceptor.annotations.InputConfig;
import com.opensymphony.xwork2.validator.annotations.EmailValidator;
import com.opensymphony.xwork2.validator.annotations.FieldExpressionValidator;
import com.opensymphony.xwork2.validator.annotations.RegexFieldValidator;
import com.opensymphony.xwork2.validator.annotations.RequiredStringValidator;
import com.opensymphony.xwork2.validator.annotations.Validations;
import com.opensymphony.xwork2.validator.annotations.ValidatorType;

@AutoConfig(namespace = "/")
@ClassPresentConditional("org.ironrhino.security.service.UserManagerImpl")
public class SignupAction extends BaseAction {

	private static final long serialVersionUID = 8175406892708878896L;

	protected static Logger log = LoggerFactory.getLogger(SignupAction.class);

	private String email;

	private String username;

	private String password;

	private String confirmPassword;

	@Autowired
	private transient UserManager userManager;

	@Autowired
	private transient SettingControl settingControl;

	@Autowired
	private transient EventPublisher eventPublisher;

	@Autowired(required = false)
	private transient MailService mailService;

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
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

	@Override
	public String input() {
		if (!settingControl.getBooleanValue(
				Constants.SETTING_KEY_SIGNUP_ENABLED, false))
			return ACCESSDENIED;
		return SUCCESS;
	}

	@Override
	@Redirect
	@InputConfig(methodName = "input")
	@Validations(requiredStrings = {
			@RequiredStringValidator(type = ValidatorType.FIELD, fieldName = "email", trim = true, key = "validation.required"),
			@RequiredStringValidator(type = ValidatorType.FIELD, fieldName = "password", trim = true, key = "validation.required") }, regexFields = { @RegexFieldValidator(type = ValidatorType.FIELD, fieldName = "username", regex = User.USERNAME_REGEX_FOR_SIGNUP, key = "validation.invalid") }, emails = { @EmailValidator(type = ValidatorType.FIELD, fieldName = "email", key = "validation.invalid") }, fieldExpressions = { @FieldExpressionValidator(expression = "password == confirmPassword", fieldName = "confirmPassword", key = "confirmPassword.error") })
	public String execute() {
		if (!settingControl.getBooleanValue(
				Constants.SETTING_KEY_SIGNUP_ENABLED, false))
			return ACCESSDENIED;
		if (StringUtils.isBlank(username))
			username = userManager.suggestUsername(email);
		if (!check())
			return INPUT;
		if (StringUtils.isBlank(password))
			password = CodecUtils.randomString(10);
		boolean activationRequired = settingControl.getBooleanValue(
				Constants.SETTING_KEY_SIGNUP_ACTIVATION_REQUIRED, false);
		User user = new User();
		user.setEmail(email);
		user.setUsername(username);
		user.setLegiblePassword(password);
		user.setEnabled(!activationRequired);
		userManager.save(user);
		eventPublisher.publish(new SignupEvent(user), Scope.LOCAL);
		if (activationRequired) {
			user.setPassword(password);// for send mail
			addActionMessage(getText("signup.success"));
			sendActivationMail(user);
		} else {
			AuthzUtils.autoLogin(user);
			eventPublisher.publish(new LoginEvent(user), Scope.LOCAL);
		}
		targetUrl = "/";
		return REDIRECT;
	}

	@Validations(regexFields = { @RegexFieldValidator(type = ValidatorType.FIELD, fieldName = "username", regex = User.USERNAME_REGEX_FOR_SIGNUP, key = "validation.invalid") }, emails = { @EmailValidator(type = ValidatorType.FIELD, fieldName = "email", key = "validation.invalid") })
	public String checkavailable() {
		return check() ? NONE : INPUT;
	}

	private boolean check() {
		boolean valid = true;
		if (settingControl.getBooleanValue(
				Constants.SETTING_KEY_SIGNUP_ENABLED, false)) {
			if (StringUtils.isNotBlank(username)
					&& userManager.findByNaturalId(username) != null) {
				addFieldError("username", getText("validation.already.exists"));
				valid = false;
			}
			if (StringUtils.isNotBlank(email)
					&& userManager.findOne("email", email) != null) {
				addFieldError("email", getText("validation.already.exists"));
				valid = false;
			}
		}
		return valid;
	}

	@SkipValidation
	public String activate() {
		String u = getUid();
		if (u != null) {
			String[] array = Blowfish.decrypt(u).split(",");
			User user = userManager.get(array[0]);
			if (user != null && !user.isEnabled()
					&& user.getEmail().equals(array[1])) {
				user.setEnabled(true);
				userManager.save(user);
				try {
					User ud = (User) userManager.loadUserByUsername(user
							.getUsername());
					AuthzUtils.autoLogin(ud);
					LoginEvent loginEvent = new LoginEvent(ud);
					loginEvent.setFirst(true);
					eventPublisher.publish(loginEvent, Scope.LOCAL);
				} catch (RuntimeException e) {
					log.warn(e.getMessage(), e);
				}
				targetUrl = "/";
				return REDIRECT;
			}
		}
		return ACCESSDENIED;
	}

	@InputConfig(resultName = "forgot")
	@Captcha(always = true)
	@Validations(requiredStrings = { @RequiredStringValidator(type = ValidatorType.FIELD, fieldName = "email", trim = true, key = "validation.required") }, emails = { @EmailValidator(type = ValidatorType.FIELD, fieldName = "email", key = "validation.invalid") })
	public String forgot() {
		User user = userManager.findOne("email", email);
		if (user == null) {
			addActionError(getText("validation.not.exists"));
		} else {
			password = CodecUtils.randomString(10);
			user.setLegiblePassword(password);
			userManager.save(user);
			user.setPassword(password);
			Map<String, Object> model = new HashMap<String, Object>(4);
			model.put("user", user);
			model.put("url", "/user/password");
			SimpleMailMessage smm = new SimpleMailMessage();
			smm.setTo(user + "<" + user.getEmail() + ">");
			smm.setSubject(getText("mail.subject.user_forgot"));
			mailService.send(smm, "template/user_forgot.ftl", model);
			addActionMessage(getText("operate.success"));
		}
		return "forgot";
	}

	private void sendActivationMail(User user) {
		Map<String, Object> model = new HashMap<String, Object>(4);
		model.put("user", user);
		model.put(
				"url",
				"/signup/activate/"
						+ Blowfish.encrypt(user.getId() + "," + user.getEmail()));
		SimpleMailMessage smm = new SimpleMailMessage();
		smm.setTo(user + "<" + user.getEmail() + ">");
		smm.setSubject(getText("mail.subject.user_activate"));
		mailService.send(smm, "template/user_activate.ftl", model);
	}

}
