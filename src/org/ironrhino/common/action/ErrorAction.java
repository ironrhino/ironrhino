package org.ironrhino.common.action;

import javax.servlet.RequestDispatcher;

import org.apache.struts2.ServletActionContext;
import org.ironrhino.core.metadata.AutoConfig;
import org.ironrhino.core.struts.BaseAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@AutoConfig(namespace = "/")
public class ErrorAction extends BaseAction {

	private static final long serialVersionUID = 7684824080798968019L;

	private static Logger logger = LoggerFactory.getLogger(ErrorAction.class);

	private Exception exception;

	public Exception getException() {
		return exception;
	}

	public String handle() {
		int errorcode = 404;
		try {
			errorcode = Integer.valueOf(getUid());
		} catch (Exception e) {

		}
		String result;
		switch (errorcode) {
		case 403:
			result = ACCESSDENIED;
			break;
		case 404:
			result = NOTFOUND;
			break;
		case 500:
			exception = (Exception) ServletActionContext.getRequest()
					.getAttribute(RequestDispatcher.ERROR_EXCEPTION);
			if (exception != null)
				logger.error(exception.getMessage(), exception);
			result = "internalServerError";
			break;
		default:
			result = NOTFOUND;
		}
		ServletActionContext.getResponse().setStatus(errorcode);
		return result;
	}
}
