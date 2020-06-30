package org.ironrhino.rest.component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletionException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;

import org.apache.commons.lang3.StringUtils;
import org.ironrhino.core.util.JsonUtils;
import org.ironrhino.rest.RestStatus;
import org.springframework.beans.TypeMismatchException;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.client.HttpClientErrorException;

import com.fasterxml.jackson.databind.JsonNode;

import lombok.extern.slf4j.Slf4j;

@ControllerAdvice
@Slf4j
public class RestExceptionHandler {

	@ExceptionHandler(Throwable.class)
	@ResponseBody
	public RestStatus handleException(HttpServletRequest req, HttpServletResponse response, Throwable ex) {
		if (ex instanceof IOException && ("Broken pipe".equals(ex.getMessage())))
			return null;
		Integer oldStatus = response.getStatus();
		if (ex instanceof CompletionException) {
			ex = ex.getCause();
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
		} else if (ex instanceof HttpRequestMethodNotSupportedException) {
			response.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
			return RestStatus.valueOf(RestStatus.CODE_FORBIDDEN, ex.getMessage());
		} else if (ex instanceof ServletRequestBindingException || ex instanceof TypeMismatchException
				|| ex instanceof IllegalArgumentException) {
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			return RestStatus.valueOf(RestStatus.CODE_FIELD_INVALID, ex.getMessage());
		} else if (ex instanceof BindException || ex instanceof MethodArgumentNotValidException) {
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			RestStatus rs = RestStatus.valueOf(RestStatus.CODE_FIELD_INVALID);
			BindingResult bindingResult = ex instanceof BindException ? (BindException) ex
					: ((MethodArgumentNotValidException) ex).getBindingResult();
			List<String> messages = new ArrayList<>();
			if (bindingResult.hasGlobalErrors())
				for (ObjectError oe : bindingResult.getGlobalErrors()) {
					messages.add(oe.getDefaultMessage());
					rs.addFieldError(oe.getObjectName(), oe.getDefaultMessage());
				}
			if (bindingResult.hasFieldErrors())
				for (FieldError fe : bindingResult.getFieldErrors()) {
					messages.add(fe.getDefaultMessage());
					rs.addFieldError(fe.getField(), fe.getDefaultMessage());
				}
			rs.setMessage(String.join("\n", messages));
			return rs;
		} else if (ex instanceof ConstraintViolationException) {
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			RestStatus rs = RestStatus.valueOf(RestStatus.CODE_FIELD_INVALID);
			ConstraintViolationException cve = (ConstraintViolationException) ex;
			Set<ConstraintViolation<?>> constraintViolations = cve.getConstraintViolations();
			List<String> messages = new ArrayList<>(constraintViolations.size());
			for (ConstraintViolation<?> cv : constraintViolations) {
				String field = cv.getPropertyPath().toString();
				if (cv.getExecutableParameters() != null) {
					// method parameter
					int index = field.indexOf('.');
					if (index > 0)
						field = field.substring(index + 1);
				}
				messages.add(cv.getMessage());
				rs.addFieldError(field, cv.getMessage());
			}
			rs.setMessage(String.join("\n", messages));
			return rs;
		} else if (ex instanceof HttpClientErrorException) {
			HttpClientErrorException hce = (HttpClientErrorException) ex;
			response.setStatus(hce.getRawStatusCode());
			String errorBody = hce.getResponseBodyAsString();
			if (JsonUtils.isValidJson(errorBody)) {
				try {
					JsonNode node = JsonUtils.fromJson(errorBody, JsonNode.class);
					if (node.has("code") && node.has("status"))
						return JsonUtils.fromJson(errorBody, RestStatus.class);
				} catch (IOException e) {

				}
			}
			switch (hce.getStatusCode()) {
			case UNAUTHORIZED:
				return RestStatus.UNAUTHORIZED;
			case FORBIDDEN:
				return RestStatus.FORBIDDEN;
			case NOT_FOUND:
				return RestStatus.NOT_FOUND;
			default:
				return RestStatus.valueOf(RestStatus.CODE_BAD_REQUEST, hce.getLocalizedMessage());
			}
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
		log.error(ex.getMessage(), ex);
		ResponseStatus rs = AnnotatedElementUtils.findMergedAnnotation(ex.getClass(), ResponseStatus.class);
		if (rs != null) {
			if (oldStatus == HttpServletResponse.SC_OK)
				response.setStatus(rs.value().value());
			return RestStatus.valueOf(rs.value().name(),
					StringUtils.isNotBlank(rs.reason()) ? rs.reason() : ex.getMessage());
		} else {
			if (oldStatus == HttpServletResponse.SC_OK)
				response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			return RestStatus.valueOf(RestStatus.CODE_INTERNAL_SERVER_ERROR, ex.getMessage());
		}
	}

}