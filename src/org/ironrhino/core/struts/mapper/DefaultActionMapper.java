package org.ironrhino.core.struts.mapper;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.lang.model.SourceVersion;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.apache.struts2.ServletActionContext;
import org.apache.struts2.StrutsConstants;
import org.apache.struts2.dispatcher.mapper.ActionMapping;
import org.ironrhino.common.action.DirectTemplateAction;
import org.ironrhino.core.metadata.AutoConfig;
import org.ironrhino.core.model.ResultPage;
import org.ironrhino.core.struts.EntityAction;
import org.ironrhino.core.struts.result.AutoConfigResult;
import org.ironrhino.core.util.ReflectionUtils;
import org.ironrhino.core.util.RequestUtils;
import org.springframework.web.context.support.WebApplicationContextUtils;

import com.opensymphony.xwork2.config.Configuration;
import com.opensymphony.xwork2.config.ConfigurationManager;
import com.opensymphony.xwork2.config.entities.ActionConfig;
import com.opensymphony.xwork2.config.entities.PackageConfig;
import com.opensymphony.xwork2.inject.Inject;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DefaultActionMapper extends AbstractActionMapper {

	public final static String DEFAULT_ACTION_NAME = "index";

	public final static String REQUEST_ATTRIBUTE_KEY_IMPLICIT_DEFAULT_ACTION = "IMPLICIT_DEFAULT_ACTION";

	private Collection<ActionMappingMatcher> actionMappingMatchers;

	@Inject(StrutsConstants.STRUTS_I18N_ENCODING)
	private String encoding = "UTF-8";

	public String getEncoding() {
		return encoding;
	}

	@Override
	public String getUriFromActionMapping(ActionMapping mapping) {
		StringBuilder sb = new StringBuilder();
		String namespace = mapping.getNamespace();
		namespace = StringUtils.isBlank(namespace) ? "/" : namespace;
		sb.append(namespace);
		if (sb.length() > 0 && sb.charAt(sb.length() - 1) != '/')
			sb.append("/");
		sb.append(mapping.getName());
		String method = mapping.getMethod();
		if (method != null)
			sb.append("/" + method);
		Map<String, Object> params = mapping.getParams();
		try {
			if (method != null && params != null && params.containsKey(ID))
				sb.append("/" + URLEncoder.encode((String) params.get(ID), getEncoding()));
		} catch (UnsupportedEncodingException e) {
			log.error(e.getMessage(), e);
		}
		return sb.toString();
	}

	@Override
	public ActionMapping getMapping(HttpServletRequest request, ConfigurationManager configManager) {
		ActionMapping mapping = null;
		String uri = RequestUtils.getRequestUri(request);
		Configuration config = configManager.getConfiguration();

		String namespace = null;
		ActionConfig actionConfig = null;
		String name = null;
		String methodAndUid = null;
		// Find the longest matching namespace and name
		uri = uri.replace("//", "/");
		if (uri.indexOf(';') > 0)
			uri = uri.substring(0, uri.indexOf(';'));
		for (Object var : config.getPackageConfigs().values()) {
			PackageConfig pc = (PackageConfig) var;
			String ns = pc.getNamespace();
			if (uri.startsWith(ns)) {
				if (namespace == null || (namespace != null && ns.length() >= namespace.length())) {
					String temp = uri.substring(ns.length());
					if ("".equals(temp) || "/".equals(temp)) {
						if (uri.endsWith("/")) {
							name = DEFAULT_ACTION_NAME;
							namespace = ns;
							if (pc.getActionConfigs().containsKey(name))
								actionConfig = pc.getActionConfigs().get(name);
							request.setAttribute(REQUEST_ATTRIBUTE_KEY_IMPLICIT_DEFAULT_ACTION, true);
						} else {
							continue;
						}
					} else {
						String[] array = StringUtils.split(temp, "/", 2);
						name = array[0];
						if (pc.getActionConfigs().containsKey(name)) {
							namespace = ns;
							actionConfig = pc.getActionConfigs().get(name);
						}
					}
				}
			}
		}

		if (actionConfig == null) {
			if (actionMappingMatchers == null)
				actionMappingMatchers = WebApplicationContextUtils
						.getRequiredWebApplicationContext(ServletActionContext.getServletContext())
						.getBeansOfType(ActionMappingMatcher.class).values();
			for (ActionMappingMatcher amm : actionMappingMatchers) {
				mapping = amm.tryMatch(request, this);
				if (mapping != null)
					return mapping;
			}

			String location = AutoConfigResult
					.getTemplateLocation(request.getAttribute(REQUEST_ATTRIBUTE_KEY_IMPLICIT_DEFAULT_ACTION) != null
							? uri + DEFAULT_ACTION_NAME
							: uri);
			if (location != null) {
				mapping = new ActionMapping();
				mapping.setNamespace(DirectTemplateAction.NAMESPACE);
				mapping.setName(DirectTemplateAction.ACTION_NAME);
				return mapping;
			}
			request.removeAttribute("com.opensymphony.sitemesh.APPLIED_ONCE");
			return null;
		}

		String str = uri.substring(namespace.length());
		if (!str.isEmpty() && !str.equals("/")) {
			String[] arr = StringUtils.split(str, "/", 2);
			if (arr.length > 1)
				methodAndUid = arr[1];
		}

		mapping = new ActionMapping();
		mapping.setNamespace(namespace);
		mapping.setName(name);
		Map<String, Object> params = new HashMap<>(4, 1);
		// process resultPage.pageNo and resultPage.pageSize
		String pn = request.getParameter(ResultPage.PAGENO_PARAM_NAME);
		if (StringUtils.isNumeric(pn))
			params.put("resultPage.pageNo", pn);
		String ps = request.getParameter(ResultPage.PAGESIZE_PARAM_NAME);
		if (StringUtils.isNumeric(ps))
			params.put("resultPage.pageSize", ps);
		String m = request.getParameter(ResultPage.MARKER_PARAM_NAME);
		if (m != null)
			params.put("resultPage.marker", m);
		String pm = request.getParameter(ResultPage.PREVIOUSMARKER_PARAM_NAME);
		if (pm != null)
			params.put("resultPage.previousMarker", pm);
		if (StringUtils.isNotBlank(methodAndUid)) {
			String uid = null;
			if (methodAndUid.indexOf('/') < 0
					|| !SourceVersion.isIdentifier(methodAndUid.substring(0, methodAndUid.indexOf('/')))) {
				Class<?> actionClass;
				try {
					actionClass = Class.forName(actionConfig.getClassName());
				} catch (ClassNotFoundException e) {
					throw new RuntimeException(e);
				}
				if (getAvailableMethods(actionClass).contains(methodAndUid)) {
					mapping.setMethod(methodAndUid);
				} else {
					if (!SourceVersion.isIdentifier(methodAndUid) || StringUtils.isNumeric(methodAndUid)
							|| methodAndUid.length() == 22 || methodAndUid.length() >= 32) {
						uid = methodAndUid;
					} else {
						AutoConfig ac = actionClass.getAnnotation(AutoConfig.class);
						if (ac == null && actionClass.getSuperclass() == EntityAction.class)
							ac = ReflectionUtils.getGenericClass(actionClass).getAnnotation(AutoConfig.class);
						if (ac != null && ac.lenientPathVariable()) {
							uid = methodAndUid;
						} else {
							// lead to not found
							mapping.setMethod(methodAndUid);
						}
					}
				}
			} else {
				String[] array = StringUtils.split(methodAndUid, "/", 2);
				mapping.setMethod(array[0]);
				if (array.length > 1)
					uid = array[1];
			}
			if (StringUtils.isNotBlank(uid)) {
				try {
					params.put(ID, URLDecoder.decode(uid, getEncoding()));
				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
				}
			}
		}
		if (params.size() > 0)
			mapping.setParams(params);
		return mapping;
	}

	private static Map<Class<?>, Set<String>> methodsCache = new ConcurrentHashMap<>(64);

	private static Set<String> getAvailableMethods(Class<?> actionClass) {
		return methodsCache.computeIfAbsent(actionClass, DefaultActionMapper::doGetAvailableMethods);
	}

	private static Set<String> doGetAvailableMethods(Class<?> actionClass) {
		Set<String> names = new HashSet<>();
		for (Method m : actionClass.getMethods()) {
			if (!Modifier.isStatic(m.getModifiers()) && m.getReturnType() == String.class
					&& m.getParameterTypes().length == 0) {
				String name = m.getName();
				if (!name.equals("toString") && !name.startsWith("get"))
					names.add(name);
			}
		}
		return names;
	}

}
