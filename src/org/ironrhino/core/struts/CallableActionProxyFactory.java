package org.ironrhino.core.struts;

import java.util.Map;

import com.opensymphony.xwork2.ActionInvocation;
import com.opensymphony.xwork2.ActionProxy;

public class CallableActionProxyFactory extends org.apache.struts2.impl.StrutsActionProxyFactory {

	@Override
	public ActionProxy createActionProxy(String namespace, String actionName, String methodName,
			Map<String, Object> extraContext, boolean executeResult, boolean cleanupContext) {
		ActionInvocation inv = new CallableActionInvocation(extraContext, true);
		container.inject(inv);
		return createActionProxy(inv, namespace, actionName, methodName, executeResult, cleanupContext);
	}
}
