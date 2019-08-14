package org.ironrhino.core.struts.interceptor;

import org.apache.struts2.ServletActionContext;
import org.apache.struts2.interceptor.StrutsConversionErrorInterceptor;

public class MyConversionErrorInterceptor extends StrutsConversionErrorInterceptor {

	private static final long serialVersionUID = -3564850694658379205L;

	@Override
	protected boolean shouldAddError(String propertyName, Object value) {
		if (!ServletActionContext.getRequest().getMethod().equals("POST"))
			return false;
		return super.shouldAddError(propertyName, value);
	}

}
