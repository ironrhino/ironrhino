package org.ironrhino.core.tracing;

import java.io.IOException;

import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import io.opentracing.Scope;
import io.opentracing.Span;
import io.opentracing.Tracer;
import io.opentracing.propagation.Format.Builtin;
import io.opentracing.tag.Tags;
import io.opentracing.util.GlobalTracer;

public class TracingClientHttpRequestInterceptor implements ClientHttpRequestInterceptor {

	@Override
	public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
			throws IOException {
		Tracer tracer = GlobalTracer.get();
		Span span = tracer.buildSpan("RestTemplate.execute").start();
		tracer.inject(span.context(), Builtin.HTTP_HEADERS, new SpringHttpMessageTextMap(request));
		Tags.SPAN_KIND.set(span, Tags.SPAN_KIND_CLIENT);
		Tags.HTTP_METHOD.set(span, request.getMethodValue());
		Tags.HTTP_URL.set(span, request.getURI().toString());
		try (Scope scope = tracer.activateSpan(span)) {
			ClientHttpResponse response = execution.execute(request, body);
			Tags.HTTP_STATUS.set(span, response.getStatusCode().value());
			return response;
		} catch (Exception ex) {
			Tracing.logError(ex);
			throw ex;
		} finally {
			span.finish();
		}
	}

}
