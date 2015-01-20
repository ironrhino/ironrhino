package org.ironrhino.rest.component;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;

import org.ironrhino.rest.RestStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

@ControllerAdvice
public class RestExceptionHandler {

	private Logger logger = LoggerFactory.getLogger(getClass());

	@ExceptionHandler(Throwable.class)
	@ResponseBody
	public RestStatus handleException(HttpServletRequest req,
			HttpServletResponse response, Throwable ex) {
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
			return RestStatus.valueOf(RestStatus.CODE_FORBIDDEN,
					ex.getMessage());
		}
		if (ex instanceof MethodArgumentNotValidException) {
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			MethodArgumentNotValidException me = (MethodArgumentNotValidException) ex;
			BindingResult bindingResult = me.getBindingResult();
			StringBuilder sb = new StringBuilder();
			for (FieldError fe : bindingResult.getFieldErrors()) {
				sb.append(fe.getField()).append(": ")
						.append(fe.getDefaultMessage()).append("; ");
			}
			return RestStatus.valueOf(RestStatus.CODE_FIELD_INVALID,
					sb.toString());
		}
		if (ex instanceof ConstraintViolationException) {
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			ConstraintViolationException cve = (ConstraintViolationException) ex;
			StringBuilder sb = new StringBuilder();
			for (ConstraintViolation<?> cv : cve.getConstraintViolations()) {
				sb.append(cv.getPropertyPath()).append(": ")
						.append(cv.getMessage()).append("; ");
			}
			return RestStatus.valueOf(RestStatus.CODE_FIELD_INVALID,
					sb.toString());
		}
		if (ex instanceof RestStatus) {
			RestStatus rs = (RestStatus) ex;
			if (rs.getCode().equals(RestStatus.CODE_REQUEST_TIMEOUT))
				response.setStatus(HttpServletResponse.SC_REQUEST_TIMEOUT);
			else if (rs.getCode().equals(RestStatus.CODE_FORBIDDEN))
				response.setStatus(HttpServletResponse.SC_FORBIDDEN);
			else if (rs.getCode().equals(RestStatus.CODE_UNAUTHORIZED))
				response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
			else if (rs.getCode().equals(RestStatus.CODE_NOT_FOUND))
				response.setStatus(HttpServletResponse.SC_NOT_FOUND);
			return rs;
		}
		logger.error(ex.getMessage(), ex);
		response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		return RestStatus.valueOf(RestStatus.CODE_INTERNAL_SERVER_ERROR,
				ex.getMessage());
	}

}