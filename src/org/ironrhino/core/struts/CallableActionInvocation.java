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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.support.WebApplicationContextUtils;

import com.opensymphony.xwork2.Action;
import com.opensymphony.xwork2.ActionContext;
import com.opensymphony.xwork2.ActionInvocation;
import com.opensymphony.xwork2.DefaultActionInvocation;
import com.opensymphony.xwork2.Result;
import com.opensymphony.xwork2.XWorkException;
import com.opensymphony.xwork2.config.entities.ActionConfig;
import com.opensymphony.xwork2.config.entities.ResultConfig;

public class CallableActionInvocation extends DefaultActionInvocation {

	private static final long serialVersionUID = -4310552665942898360L;

	private static Logger logger = LoggerFactory.getLogger(CallableActionInvocation.class);

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
				executorService = WebApplicationContextUtils.getWebApplicationContext(request.getServletContext())
						.getBean("executorService", ExecutorService.class);
			} catch (NoSuchBeanDefinitionException e) {
				logger.warn("No bean[executorService] defined, use ForkJoinPool.commonPool() as fallback");
			}
			if (executorService == null)
				executorService = ForkJoinPool.commonPool();
			final ExecutorService es = executorService;
			AsyncContext asyncContext = request.startAsync();
			@SuppressWarnings("serial")
			Result result = new Result() {
				@Override
				public void execute(ActionInvocation actionInvocation) throws Exception {
					es.submit(() -> {
						try {
							SecurityContextHolder.setContext(sc);
							ServletActionContext.setContext(context);
							String result = callable.call();
							ActionConfig config = proxy.getConfig();
							Map<String, ResultConfig> results = config.getResults();
							ResultConfig resultConfig = results.get(result);
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
							} else if (result != null && !Action.NONE.equals(result)
									&& unknownHandlerManager.hasUnknownHandlers()) {
								re = unknownHandlerManager.handleUnknownResult(invocationContext, proxy.getActionName(),
										proxy.getConfig(), result);
							}
							((CallableActionInvocation) actionInvocation).reset();
							actionInvocation.setResultCode(result);
							re.execute(actionInvocation);
						} catch (Exception e) {
							logger.error(e.getMessage(), e);
							try {
								response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
							} catch (IOException ex) {
								logger.error(ex.getMessage(), ex);
							}
						} finally {
							SecurityContextHolder.clearContext();
							ServletActionContext.setContext(null);
							asyncContext.complete();
						}
					});
				}
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

	protected void reset() {
		executed = false;
	}

}
