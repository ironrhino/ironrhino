/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.struts2;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.UnsupportedEncodingException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang3.StringUtils;
import org.apache.struts2.dispatcher.Dispatcher;
import org.apache.struts2.dispatcher.mapper.ActionMapper;
import org.apache.struts2.dispatcher.mapper.ActionMapping;
import org.apache.struts2.util.StrutsTestCaseHelper;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockServletContext;

import com.opensymphony.xwork2.ActionContext;
import com.opensymphony.xwork2.ActionProxy;
import com.opensymphony.xwork2.ActionProxyFactory;
import com.opensymphony.xwork2.LocaleProvider;
import com.opensymphony.xwork2.TextProvider;
import com.opensymphony.xwork2.TextProviderFactory;
import com.opensymphony.xwork2.ValidationAware;
import com.opensymphony.xwork2.XWorkJUnit4TestCase;
import com.opensymphony.xwork2.config.Configuration;
import com.opensymphony.xwork2.interceptor.annotations.After;
import com.opensymphony.xwork2.interceptor.annotations.Before;
import com.opensymphony.xwork2.util.ValueStack;

public abstract class StrutsJUnit4TestCase<T> extends XWorkJUnit4TestCase implements LocaleProvider, TextProvider {

	protected MockHttpServletResponse response;
	protected MockHttpServletRequest request;
	protected MockServletContext servletContext;
	protected Map<String, String> dispatcherInitParams;
	protected Dispatcher dispatcher;
	protected TextProvider textProvider;

	protected DefaultResourceLoader resourceLoader = new DefaultResourceLoader();

	/**
	 * gets an object from the stack after an action is executed
	 */
	protected Object findValueAfterExecute(String key) {
		return ServletActionContext.getValueStack(request).findValue(key);
	}

	/**
	 * gets an object from the stack after an action is executed
	 *
	 * @return The executed action
	 */
	@SuppressWarnings("unchecked")
	protected T getAction() {
		return (T) findValueAfterExecute("action");
	}

	protected boolean containsErrors() {
		T action = this.getAction();
		if (action instanceof ValidationAware) {
			return ((ValidationAware) action).hasActionErrors();
		}
		throw new UnsupportedOperationException("Current action does not implement ValidationAware interface");
	}

	/**
	 * Executes an action and returns it's output (not the result returned from
	 * execute()), but the actual output that would be written to the response. For
	 * this to work the configured result for the action needs to be FreeMarker, or
	 * Velocity (JSPs can be used with the Embedded JSP plugin)
	 */
	protected String executeAction(String uri) {
		request.setRequestURI(uri);
		ActionMapping mapping = getActionMapping(request);

		assertThat(mapping, is(notNullValue()));
		try {
			Dispatcher.getInstance().serviceAction(request, response, servletContext, mapping);
			return response.getContentAsString();
		} catch (ServletException | UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Creates an action proxy for a request, and sets parameters of the
	 * ActionInvocation to the passed parameters. Make sure to set the request
	 * parameters in the protected "request" object before calling this method.
	 */
	protected ActionProxy getActionProxy(String uri) {
		request.setRequestURI(uri);
		ActionMapping mapping = getActionMapping(request);
		String namespace = mapping.getNamespace();
		String name = mapping.getName();
		String method = mapping.getMethod();

		Configuration config = configurationManager.getConfiguration();
		ActionProxy proxy = config.getContainer().getInstance(ActionProxyFactory.class).createActionProxy(namespace,
				name, method, new HashMap<String, Object>(), true, false);

		initActionContext(proxy.getInvocation().getInvocationContext());

		// this is normally done in onSetUp(), but we are using Struts internal
		// objects (proxy and action invocation)
		// so we have to hack around so it works
		ServletActionContext.setServletContext(servletContext);
		ServletActionContext.setRequest(request);
		ServletActionContext.setResponse(response);

		return proxy;
	}

	protected void initActionContext(ActionContext actionContext) {
		actionContext.setParameters(new HashMap<String, Object>(request.getParameterMap()));
		initSession(actionContext);
		// set the action context to the one used by the proxy
		ActionContext.setContext(actionContext);
	}

	protected void initSession(ActionContext actionContext) {
		if (actionContext.getSession() == null) {
			actionContext.setSession(new HashMap<String, Object>());
			request.setSession(new MockHttpSession(servletContext));
		}
	}

	/**
	 * Finds an ActionMapping for a given request
	 */
	protected ActionMapping getActionMapping(HttpServletRequest request) {
		return container.getInstance(ActionMapper.class).getMapping(request, configurationManager);
	}

	/**
	 * Finds an ActionMapping for a given url
	 */
	protected ActionMapping getActionMapping(String url) {
		MockHttpServletRequest req = new MockHttpServletRequest();
		req.setRequestURI(url);
		return getActionMapping(req);
	}

	/**
	 * Injects dependencies on an Object using Struts internal IoC container
	 */
	protected void injectStrutsDependencies(Object object) {
		container.inject(object);
	}

	protected void setupBeforeInitDispatcher() throws Exception {
	}

	protected void initServletMockObjects() {
		servletContext = new MockServletContext(resourceLoader);
		response = new MockHttpServletResponse();
		request = new MockHttpServletRequest();
	}

	public void finishExecution() {
		HttpSession session = this.request.getSession();
		if (session != null) {
			Enumeration<String> attributeNames = session.getAttributeNames();

			MockHttpServletRequest nextRequest = new MockHttpServletRequest();

			while (attributeNames.hasMoreElements()) {
				String key = attributeNames.nextElement();
				Object attribute = session.getAttribute(key);
				HttpSession s = nextRequest.getSession();
				if (s != null)
					s.setAttribute(key, attribute);
			}

			this.response = new MockHttpServletResponse();
			this.request = nextRequest;
		}
	}

	/**
	 * Sets up the configuration settings, XWork configuration, and message
	 * resources
	 */
	@Override
	@Before
	public void setUp() throws Exception {
		super.setUp();
		initServletMockObjects();
		setupBeforeInitDispatcher();
		initDispatcherParams();
		initDispatcher(dispatcherInitParams);
	}

	protected void initDispatcherParams() {
		if (StringUtils.isNotBlank(getConfigPath())) {
			dispatcherInitParams = new HashMap<>();
			dispatcherInitParams.put("config", "struts-default.xml," + getConfigPath());
		}
	}

	protected Dispatcher initDispatcher(Map<String, String> params) {
		dispatcher = StrutsTestCaseHelper.initDispatcher(servletContext, params);
		configurationManager = dispatcher.getConfigurationManager();
		configuration = configurationManager.getConfiguration();
		container = configuration.getContainer();
		container.inject(dispatcher);
		return dispatcher;
	}

	/**
	 * Override this method to return a comma separated list of paths to a
	 * configuration file.
	 * <p>
	 * The default implementation simply returns <code>null</code>.
	 * 
	 * @return a comma separated list of config locations
	 */
	protected String getConfigPath() {
		return null;
	}

	@Override
	@After
	public void tearDown() throws Exception {
		super.tearDown();
		if (dispatcher != null && dispatcher.getConfigurationManager() != null) {
			dispatcher.cleanup();
			dispatcher = null;
		}
		StrutsTestCaseHelper.tearDown();
	}

	private TextProvider getTextProvider() {
		if (this.textProvider == null) {
			TextProviderFactory tpf = new TextProviderFactory();
			if (this.container != null) {
				this.container.inject(tpf);
			}
			this.textProvider = tpf.createInstance(this.getClass(), this);
		}
		return this.textProvider;
	}

	@Override
	public Locale getLocale() {
		ActionContext ctx = ActionContext.getContext();
		if (ctx != null) {
			return ctx.getLocale();
		} else {
			return null;
		}
	}

	@Override
	public boolean hasKey(String key) {
		return getTextProvider().hasKey(key);
	}

	@Override
	public String getText(String aTextName) {
		return getTextProvider().getText(aTextName);
	}

	@Override
	public String getText(String aTextName, String defaultValue) {
		return getTextProvider().getText(aTextName, defaultValue);
	}

	@Override
	public String getText(String aTextName, String defaultValue, String obj) {
		return getTextProvider().getText(aTextName, defaultValue, obj);
	}

	@Override
	public String getText(String aTextName, List<?> args) {
		return getTextProvider().getText(aTextName, args);
	}

	@Override
	public String getText(String key, String[] args) {
		return getTextProvider().getText(key, args);
	}

	@Override
	public String getText(String aTextName, String defaultValue, List<?> args) {
		return getTextProvider().getText(aTextName, defaultValue, args);
	}

	@Override
	public String getText(String key, String defaultValue, String[] args) {
		return getTextProvider().getText(key, defaultValue, args);
	}

	@Override
	public String getText(String key, String defaultValue, List<?> args, ValueStack stack) {
		return getTextProvider().getText(key, defaultValue, args, stack);
	}

	@Override
	public String getText(String key, String defaultValue, String[] args, ValueStack stack) {
		return getTextProvider().getText(key, defaultValue, args, stack);
	}

	@Override
	public ResourceBundle getTexts() {
		return getTextProvider().getTexts();
	}

	@Override
	public ResourceBundle getTexts(String aBundleName) {
		return getTextProvider().getTexts(aBundleName);
	}
}
