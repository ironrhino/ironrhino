package api.intercept;

import javax.servlet.http.HttpServletRequest;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

import api.RestStatus;

@ControllerAdvice
public class RestExceptionHandler {

	@ExceptionHandler(Throwable.class)
	@ResponseBody
	public RestStatus handleException(HttpServletRequest req, Throwable ex) {
		if (ex instanceof RestStatus)
			return (RestStatus) ex;
		return RestStatus.valueOf(RestStatus.CODE_INTERNAL_SERVER_ERROR,
				ex.getMessage());
	}

}