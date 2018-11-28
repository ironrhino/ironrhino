package org.ironrhino.core.struts;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;

import javax.servlet.AsyncContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts2.ServletActionContext;
import org.ironrhino.core.tracing.Tracing;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.support.WebApplicationContextUtils;

import com.opensymphony.xwork2.Action;
import com.opensymphony.xwork2.ActionContext;
import com.opensymphony.xwork2.DefaultActionInvocation;
import com.opensymphony.xwork2.Result;
import com.opensymphony.xwork2.XWorkException;
import com.opensymphony.xwork2.config.ConfigurationException;
import com.opensymphony.xwork2.config.entities.ActionConfig;
import com.opensymphony.xwork2.config.entities.InterceptorMapping;
import com.opensymphony.xwork2.config.entities.ResultConfig;
import com.opensymphony.xwork2.interceptor.PreResultListener;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CallableActionInvocation extends DefaultActionInvocation {

	private static final long serialVersionUID = -4310552665942898360L;

	protected Callable<String> callableResult;

	public CallableActionInvocation(Map<String, Object> extraContext, boolean pushAction) {
		super(extraContext, pushAction);
	}

	@Override
	public Result createResult() throws Exception {
		if (callableResult != null) {
			Callable<String> callable = callableResult;
			ActionContext context = ActionContext.getContext();
			SecurityContext sc = SecurityContextHolder.getContext();
			HttpServletRequest request = ServletActionContext.getRequest();
			HttpServletResponse response = ServletActionContext.getResponse();
			ExecutorService executorService = null;
			try {
				executorService = WebApplicationContextUtils
						.getRequiredWebApplicationContext(request.getServletContext())
						.getBean("executorService", ExecutorService.class);
			} catch (NoSuchBeanDefinitionException e) {
				log.warn("No bean[executorService] defined, use ForkJoinPool.commonPool() as fallback");
			}
			if (executorService == null)
				executorService = ForkJoinPool.commonPool();
			final ExecutorService es = executorService;
			AsyncContext asyncContext = request.startAsync();
			Result result = actionInvocation -> {
				es.execute(() -> {
					try {
						SecurityContextHolder.setContext(sc);
						ActionContext.setContext(context);
						String rst = callable.call();
						ActionConfig config = proxy.getConfig();
						Map<String, ResultConfig> results = config.getResults();
						ResultConfig resultConfig = results.get(rst);
						if (resultConfig == null) {
							resultConfig = results.get("*");
						}
						Result re = null;
						if (resultConfig != null) {
							try {
								re = objectFactory.buildResult(resultConfig, invocationContext.getContextMap());
							} catch (Exception e) {
								throw new XWorkException(e, resultConfig);
							}
						} else if (rst != null && !Action.NONE.equals(rst)
								&& unknownHandlerManager.hasUnknownHandlers()) {
							re = unknownHandlerManager.handleUnknownResult(invocationContext, proxy.getActionName(),
									proxy.getConfig(), rst);
						}
						((CallableActionInvocation) actionInvocation).reset();
						actionInvocation.setResultCode(rst);
						executeResult(re);
					} catch (Exception e) {
						log.error(e.getMessage(), e);
						try {
							response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
						} catch (IOException ex) {
							log.error(ex.getMessage(), ex);
						}
					} finally {
						SecurityContextHolder.clearContext();
						ActionContext.setContext(null);
						asyncContext.complete();
					}
				});
			};
			callableResult = null;
			return result;
		}
		return super.createResult();
	}

	@SuppressWarnings("unchecked")
	@Override
	protected String saveResult(ActionConfig actionConfig, Object methodResult) {
		if ((methodResult instanceof Callable)) {
			callableResult = ((Callable<String>) methodResult);
			return null;
		}
		return super.saveResult(actionConfig, methodResult);
	}

	@Override
	public String invoke() throws Exception {
		if (executed) {
			throw new IllegalStateException("Action has already executed");
		}
		if (interceptors.hasNext()) {
			InterceptorMapping interceptor = interceptors.next();
			resultCode = interceptor.getInterceptor().intercept(this);
		} else {
			resultCode = invokeActionOnly();
		}
		if (!executed) {
			if (preResultListeners != null) {
				for (Object preResultListener : preResultListeners) {
					PreResultListener listener = (PreResultListener) preResultListener;
					listener.beforeResult(this, resultCode);
				}
			}
			if (proxy.getExecuteResult()) {
				result = createResult();
				executeResult(result);
			}
			executed = true;
		}
		return resultCode;
	}

	protected void executeResult(Result result) throws Exception {
		if (result == null) {
			if ((resultCode != null) && (!"none".equals(resultCode)))
				throw new ConfigurationException("No result defined for action " + getAction().getClass().getName()
						+ " and result " + getResultCode(), proxy.getConfig());
			else
				return;
		}
		Tracing.execute(result.getClass().getName() + ".execute(ActionInvocation)", () -> {
			result.execute(this);
			return null;
		}, "component", "result");
	}

	@Override
	protected String invokeAction(Object action, ActionConfig actionConfig) throws Exception {
		return Tracing.execute(action.getClass().getName() + "." + proxy.getMethod() + "()", () -> {
			return super.invokeAction(action, actionConfig);
		}, "component", "action");
	}

	protected void reset() {
		executed = false;
	}

}
