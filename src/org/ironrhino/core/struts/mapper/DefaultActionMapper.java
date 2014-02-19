package org.ironrhino.core.struts.mapper;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.apache.struts2.ServletActionContext;
import org.apache.struts2.StrutsConstants;
import org.apache.struts2.dispatcher.mapper.ActionMapping;
import org.ironrhino.common.action.DirectTemplateAction;
import org.ironrhino.core.model.ResultPage;
import org.ironrhino.core.struts.result.AutoConfigResult;
import org.springframework.web.context.support.WebApplicationContextUtils;

import com.opensymphony.xwork2.config.Configuration;
import com.opensymphony.xwork2.config.ConfigurationManager;
import com.opensymphony.xwork2.config.entities.PackageConfig;
import com.opensymphony.xwork2.inject.Inject;

public class DefaultActionMapper extends AbstractActionMapper {

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
				sb.append("/"
						+ URLEncoder.encode((String) params.get(ID),
								getEncoding()));
		} catch (UnsupportedEncodingException e) {
			log.error(e.getMessage(), e);
		}
		return sb.toString();
	}

	@Override
	public ActionMapping getMapping(HttpServletRequest request,
			ConfigurationManager configManager) {
		ActionMapping mapping = null;
		String uri = getUri(request);
		Configuration config = configManager.getConfiguration();

		String namespace = null;
		String name = null;
		String methodAndUid = null;
		// Find the longest matching namespace and name
		uri = uri.replace("//", "/");
		if (uri.indexOf(';') > 0)
			uri = uri.substring(0, uri.indexOf(';'));
		for (Object var : config.getPackageConfigs().values()) {
			PackageConfig pc = (PackageConfig) var;
			String ns = pc.getNamespace();
			if (!uri.equals(ns) && uri.startsWith(ns)) {
				if (namespace == null
						|| (namespace != null && ns.length() >= namespace
								.length())) {
					String temp = uri.substring(ns.length());
					if ("".equals(temp) || "/".equals(temp))
						continue;
					String[] array = StringUtils.split(temp, "/", 2);
					name = org.ironrhino.core.util.StringUtils
							.toCamelCase(array[0]);
					if (pc.getActionConfigs().containsKey(name))
						namespace = ns;
				}
			}
		}

		if (namespace == null) {
			if (actionMappingMatchers == null)
				actionMappingMatchers = WebApplicationContextUtils
						.getWebApplicationContext(
								ServletActionContext.getServletContext())
						.getBeansOfType(ActionMappingMatcher.class).values();
			for (ActionMappingMatcher amm : actionMappingMatchers) {
				mapping = amm.tryMatch(request, this);
				if (mapping != null)
					return mapping;
			}

			String location = AutoConfigResult
					.getTemplateLocation(org.ironrhino.core.util.StringUtils
							.toCamelCase(uri));
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
		String[] arr = StringUtils.split(str, "/", 2);
		name = org.ironrhino.core.util.StringUtils.toCamelCase(arr[0]);
		if (arr.length > 1)
			methodAndUid = arr[1];

		mapping = new ActionMapping();
		mapping.setNamespace(namespace);
		mapping.setName(org.ironrhino.core.util.StringUtils.toCamelCase(name));
		Map<String, Object> params = new HashMap<String, Object>(4, 1);
		// process resultPage.pageNo and resultPage.pageSize
		String pn = request.getParameter(ResultPage.PAGENO_PARAM_NAME);
		if (StringUtils.isNumeric(pn))
			params.put("resultPage.pageNo", pn);
		String ps = request.getParameter(ResultPage.PAGESIZE_PARAM_NAME);
		if (StringUtils.isNumeric(ps))
			params.put("resultPage.pageSize", ps);
		if (StringUtils.isNotBlank(methodAndUid)) {
			String uid = null;
			String[] array = StringUtils.split(methodAndUid, "/", 2);
			mapping.setMethod(array[0]);
			if (array.length > 1) {
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

}
