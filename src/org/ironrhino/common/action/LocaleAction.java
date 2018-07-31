package org.ironrhino.common.action;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.apache.struts2.ServletActionContext;
import org.ironrhino.common.support.LocaleProvider;
import org.ironrhino.core.metadata.AutoConfig;
import org.ironrhino.core.session.HttpSessionManager;
import org.ironrhino.core.struts.BaseAction;
import org.ironrhino.core.util.RequestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import lombok.Getter;
import lombok.Setter;

@AutoConfig(namespace = "/")
public class LocaleAction extends BaseAction {

	private static final long serialVersionUID = -4036864903754537469L;

	public static final String SETTING_KEY_AVAILABLE_LOCALES = "availableLocales";

	@Getter
	@Setter
	private String lang;

	@Getter
	private Locale[] availableLocales;

	@Value("${globalCookie:false}")
	private boolean globalCookie;

	@Autowired(required = false)
	private LocaleProvider localeProvider;

	@Autowired
	private HttpSessionManager httpSessionManager;

	@Override
	public String execute() {
		HttpServletRequest request = ServletActionContext.getRequest();
		if (lang != null) {
			HttpServletResponse response = ServletActionContext.getResponse();
			Locale loc = null;
			if (StringUtils.isBlank(lang)) {
				loc = null;
			} else {
				for (Locale var : Locale.getAvailableLocales()) {
					if (lang.equalsIgnoreCase(var.toString())) {
						loc = var;
						break;
					}
				}
			}
			if (loc != null) {
				RequestUtils.saveCookie(request, response, httpSessionManager.getLocaleCookieName(), loc.toString(),
						globalCookie);
			} else {
				RequestUtils.deleteCookie(request, response, httpSessionManager.getLocaleCookieName(), true);
			}
			targetUrl = "/";
			return REDIRECT;
		} else {
			Locale locale = request.getLocale();
			if (locale != null)
				lang = locale.toString();
		}
		availableLocales = Locale.getAvailableLocales();
		String[] locales = null;
		if (localeProvider != null)
			locales = localeProvider.getAvailableLocales();
		if (locales != null && locales.length > 0) {
			List<String> _locales = Arrays.asList(locales);
			List<Locale> list = new ArrayList<>(locales.length);
			for (Locale locale : availableLocales) {
				if (_locales.contains(locale.toString()))
					list.add(locale);
			}
			availableLocales = list.toArray(new Locale[list.size()]);
		}
		return SUCCESS;
	}

}
