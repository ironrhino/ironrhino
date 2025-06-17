package org.ironrhino.core.tracing;

import java.net.HttpURLConnection;
import java.util.Collections;
import java.util.Iterator;

import javax.servlet.http.HttpServletRequest;

import org.springframework.http.HttpMessage;

import io.opentelemetry.api.baggage.propagation.W3CBaggagePropagator;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.context.propagation.TextMapGetter;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.context.propagation.TextMapSetter;
import lombok.experimental.UtilityClass;

@UtilityClass
public class TextMapPropagators {

	private static final TextMapPropagator propagator = ContextPropagators.create(
			TextMapPropagator.composite(W3CTraceContextPropagator.getInstance(), W3CBaggagePropagator.getInstance()))
			.getTextMapPropagator();

	private static final TextMapSetter<HttpMessage> springHttpMessageTextMapSetter = new TextMapSetter<HttpMessage>() {

		@Override
		public void set(HttpMessage message, String key, String value) {
			message.getHeaders().set(key, value);
		}
	};

	private static final TextMapSetter<org.apache.http.HttpMessage> apacheHttpMessageTextMapSetter = new TextMapSetter<org.apache.http.HttpMessage>() {

		@Override
		public void set(org.apache.http.HttpMessage message, String key, String value) {
			message.setHeader(key, value);
		}
	};

	private static final TextMapSetter<HttpURLConnection> httpURLConnectionTextMapSetter = new TextMapSetter<HttpURLConnection>() {

		@Override
		public void set(HttpURLConnection connection, String key, String value) {
			connection.addRequestProperty(key, value);
		}
	};

	private static final TextMapGetter<HttpServletRequest> httpServletRequestTextMapGetter = new TextMapGetter<HttpServletRequest>() {

		@Override
		public String get(HttpServletRequest request, String key) {
			return request.getHeader(key);
		}

		@Override
		public Iterator<String> getAll(HttpServletRequest request, String key) {
			return Collections.list(request.getHeaders(key)).iterator();
		}

		@Override
		public Iterable<String> keys(HttpServletRequest request) {
			return Collections.list(request.getHeaderNames());
		}

	};

	public static void inject(HttpMessage httpMessage) {
		propagator.inject(Context.current(), httpMessage, springHttpMessageTextMapSetter);
	}

	public static void inject(org.apache.http.HttpMessage httpMessage) {
		propagator.inject(Context.current(), httpMessage, apacheHttpMessageTextMapSetter);
	}

	public static void inject(HttpURLConnection connection) {
		propagator.inject(Context.current(), connection, httpURLConnectionTextMapSetter);
	}

	public static Context extract(Context context, HttpServletRequest request) {
		return propagator.extract(context, request, httpServletRequestTextMapGetter);
	}
}
