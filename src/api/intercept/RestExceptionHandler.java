package api.intercept;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

import api.RestStatus;

@ControllerAdvice
public class RestExceptionHandler {

	@ExceptionHandler(Throwable.class)
	@ResponseBody
	public RestStatus handleException(HttpServletRequest req,
			HttpServletResponse response, Throwable ex) {
		if (ex instanceof HttpMediaTypeNotAcceptableException) {
			response.setContentType("text/plain");
			try {
				response.getWriter().write("unsupported media type");
			} catch (IOException e) {
				e.printStackTrace();
			}
			return null;
		}
		if (ex instanceof RestStatus)
			return (RestStatus) ex;
		return RestStatus.valueOf(RestStatus.CODE_INTERNAL_SERVER_ERROR,
				ex.getMessage());
	}

}