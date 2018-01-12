package org.ironrhino.rest.component;

import java.io.IOException;
import java.util.concurrent.CompletionException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;

import org.ironrhino.rest.RestStatus;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@ControllerAdvice
public class RestExceptionHandler {

	@Autowired
	private Logger logger;

	@ExceptionHandler(Throwable.class)
	@ResponseBody
	public RestStatus handleException(HttpServletRequest req, HttpServletResponse response, Throwable ex) {
		Integer oldStatus = response.getStatus();
		if (ex instanceof CompletionException) {
			ex = ex.getCause();
		}
		if (ex instanceof BindException) {
			BindException be = ((BindException) ex);
			StringBuilder sb = new StringBuilder();
			for (FieldError fe : be.getFieldErrors())
				sb.append(fe.getField()).append(": ").append(fe.getDefaultMessage()).append("; ");
			return RestStatus.valueOf(RestStatus.CODE_FIELD_INVALID, sb.toString());
		}
		if (ex instanceof HttpMediaTypeNotAcceptableException) {
			response.setContentType("text/plain");
			response.setStatus(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE);
			try {
				response.getWriter().write("unsupported media type");
			} catch (IOException e) {
				e.printStackTrace();
			}
			return null;
		}
		if (ex instanceof HttpRequestMethodNotSupportedException) {
			response.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
			return RestStatus.valueOf(RestStatus.CODE_FORBIDDEN, ex.getMessage());
		}
		if (ex instanceof MethodArgumentNotValidException) {
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			MethodArgumentNotValidException me = (MethodArgumentNotValidException) ex;
			BindingResult bindingResult = me.getBindingResult();
			StringBuilder sb = new StringBuilder();
			for (FieldError fe : bindingResult.getFieldErrors()) {
				sb.append(fe.getField()).append(": ").append(fe.getDefaultMessage()).append("; ");
			}
			return RestStatus.valueOf(RestStatus.CODE_FIELD_INVALID, sb.toString());
		}
		if (ex instanceof ConstraintViolationException) {
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			ConstraintViolationException cve = (ConstraintViolationException) ex;
			StringBuilder sb = new StringBuilder();
			for (ConstraintViolation<?> cv : cve.getConstraintViolations()) {
				String path = cv.getPropertyPath().toString();
				if (cv.getExecutableParameters() != null) {
					// method parameter
					int index = path.indexOf('.');
					if (index > 0)
						path = path.substring(index + 1);
				}
				sb.append(path).append(": ").append(cv.getMessage()).append("; ");
			}
			return RestStatus.valueOf(RestStatus.CODE_FIELD_INVALID, sb.toString());
		}
		if (ex instanceof MethodArgumentTypeMismatchException || ex instanceof IllegalArgumentException) {
			return RestStatus.valueOf(RestStatus.CODE_FIELD_INVALID, ex.getMessage());
		}
		if (ex.getCause() instanceof RestStatus)
			ex = ex.getCause();
		if (ex instanceof RestStatus) {
			RestStatus rs = (RestStatus) ex;
			if (oldStatus == HttpServletResponse.SC_OK) {
				Integer httpStatusCode = rs.getHttpStatusCode();
				response.setStatus(httpStatusCode != null ? httpStatusCode : HttpServletResponse.SC_BAD_REQUEST);
			}
			return rs;
		}
		logger.error(ex.getMessage(), ex);
		if (oldStatus == HttpServletResponse.SC_OK)
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		return RestStatus.valueOf(RestStatus.CODE_INTERNAL_SERVER_ERROR, ex.getMessage());
	}

}