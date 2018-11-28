package org.ironrhino.core.remoting.server;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.function.Function;

import javax.servlet.AsyncContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.ironrhino.core.remoting.RemotingContext;
import org.ironrhino.core.remoting.ServiceRegistry;
import org.ironrhino.core.remoting.serializer.HttpInvokerSerializer;
import org.ironrhino.core.remoting.serializer.HttpInvokerSerializers;
import org.ironrhino.core.remoting.stats.ServiceStats;
import org.ironrhino.core.servlet.AccessFilter;
import org.ironrhino.core.servlet.ProxySupportHttpServletRequest;
import org.ironrhino.core.util.ExceptionUtils;
import org.ironrhino.core.util.JsonDesensitizer;
import org.ironrhino.core.util.ThrowableFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.serializer.support.SerializationFailedException;
import org.springframework.remoting.support.RemoteInvocation;
import org.springframework.remoting.support.RemoteInvocationResult;
import org.springframework.util.ClassUtils;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.web.HttpRequestHandler;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class HttpInvokerServer implements HttpRequestHandler {

	private static final int MAX_STACK_TRACE_ELEMENTS = 10;
	private static final String MDC_KEY_INTERFACE_NAME = "interfaceName";
	private static final String MDC_KEY_ROLE = "role";
	private static final String MDC_KEY_SERVICE = "service";

	private Logger remotingLogger = LoggerFactory.getLogger("remoting");

	@Value("${httpInvoker.loggingPayload:true}")
	private boolean loggingPayload;

	@Autowired
	private ServiceRegistry serviceRegistry;

	@Autowired(required = false)
	private ServiceStats serviceStats;

	@Override
	public void handleRequest(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		if (request instanceof ProxySupportHttpServletRequest) {
			log.error("Forbidden for Proxy");
			response.setStatus(HttpServletResponse.SC_FORBIDDEN);
			return;
		}
		String uri = request.getRequestURI();
		String interfaceName = uri.substring(uri.lastIndexOf('/') + 1);
		MDC.put(AccessFilter.MDC_KEY_REQUEST_FROM, request.getHeader(AccessFilter.HTTP_HEADER_REQUEST_FROM));
		MDC.put(MDC_KEY_INTERFACE_NAME, interfaceName);
		Object target = serviceRegistry.getExportedServices().get(interfaceName);
		if (target == null) {
			log.error("Service Not Found: " + interfaceName);
			response.setStatus(HttpServletResponse.SC_NOT_FOUND);
			return;
		}
		invoke(request, response, req -> {
			RemoteInvocation invocation = HttpInvokerSerializers.forRequest(req)
					.readRemoteInvocation(ClassUtils.forName(interfaceName, null), req.getInputStream());
			List<String> parameterTypeList = new ArrayList<>(invocation.getParameterTypes().length);
			for (Class<?> cl : invocation.getParameterTypes())
				parameterTypeList.add(cl.getSimpleName());
			String method = new StringBuilder(invocation.getMethodName()).append("(")
					.append(String.join(",", parameterTypeList)).append(")").toString();
			MDC.put(MDC_KEY_ROLE, "SERVER");
			MDC.put(MDC_KEY_SERVICE, MDC.get(MDC_KEY_INTERFACE_NAME) + '.' + method);
			if (loggingPayload) {
				Object[] args = invocation.getArguments();
				remotingLogger.info("Request: {}", JsonDesensitizer.DEFAULT_INSTANCE
						.toJson(args.length == 1 ? args[0] : invocation.getArguments()));
			}
			return invocation;
		}, invocation -> {
			try {
				return createRemoteInvocationResult(request, invocation, invocation.invoke(target));
			} catch (Throwable ex) {
				if (ex instanceof InvocationTargetException) {
					ex = new InvocationTargetException(
							transform(((InvocationTargetException) ex).getTargetException()));
				}
				log.error("Processing of " + MDC.get(MDC_KEY_INTERFACE_NAME) + " remote call resulted in exception",
						ex instanceof InvocationTargetException ? ex.getCause() : ex);
				return new RemoteInvocationResult(ex);
			}
		}, () -> {
			MDC.remove(MDC_KEY_INTERFACE_NAME);
			MDC.remove(MDC_KEY_ROLE);
			MDC.remove(MDC_KEY_SERVICE);
		});
	}

	protected RemoteInvocationResult createRemoteInvocationResult(HttpServletRequest request,
			RemoteInvocation invocation, Object value) {
		if (value instanceof Optional) {
			Optional<?> optional = ((Optional<?>) value);
			return new RemoteInvocationResult(optional.orElse(null));
		} else if (value instanceof Callable || value instanceof Future) {
			AsyncContext context = request.startAsync();
			Map<String, String> contextMap = MDC.getCopyOfContextMap();
			if (value instanceof CompletableFuture) {
				((CompletableFuture<?>) value).whenComplete((obj, e) -> {
					RemoteInvocationResult asyncResult = new RemoteInvocationResult();
					if (e == null) {
						asyncResult.setValue(obj);
					} else {
						Throwable ex = e;
						if (ex instanceof CompletionException) {
							ex = transform(e.getCause());
						}
						log.error("Processing of " + MDC.get(MDC_KEY_INTERFACE_NAME)
								+ " remote call resulted in exception", ex);
						asyncResult.setException(new InvocationTargetException(ex));
					}
					invokeWithAsyncResult(context, contextMap, invocation, asyncResult);
				});
			} else if (value instanceof ListenableFuture) {
				((ListenableFuture<?>) value).addCallback(obj -> {
					RemoteInvocationResult asyncResult = new RemoteInvocationResult();
					asyncResult.setValue(obj);
					invokeWithAsyncResult(context, contextMap, invocation, asyncResult);
				}, ex -> invokeWithAsyncResult(context, contextMap, invocation,
						new RemoteInvocationResult(new InvocationTargetException(ex))));
			} else {
				context.start(() -> {
					RemoteInvocationResult asyncResult = new RemoteInvocationResult();
					try {
						asyncResult.setValue(value instanceof Callable ? (((Callable<?>) value)).call()
								: (((Future<?>) value)).get());
					} catch (Throwable e) {
						if (value instanceof Future && e instanceof ExecutionException)
							e = transform(e.getCause());
						log.error("Processing of " + MDC.get(MDC_KEY_INTERFACE_NAME)
								+ " remote call resulted in exception", e);
						asyncResult.setException(new InvocationTargetException(e));
					}
					invokeWithAsyncResult(context, contextMap, invocation, asyncResult);
				});
			}
			return null;
		}
		return new RemoteInvocationResult(value);
	}

	protected Throwable transform(Throwable throwable) {
		ExceptionUtils.trimStackTrace(throwable, MAX_STACK_TRACE_ELEMENTS);
		throwable = ExceptionUtils.transformForSerialization(throwable);
		return throwable;
	}

	private void invoke(HttpServletRequest request, HttpServletResponse response,
			ThrowableFunction<HttpServletRequest, RemoteInvocation, Exception> invocationFunction,
			Function<RemoteInvocation, RemoteInvocationResult> invocationResultFunction, Runnable completion) {
		HttpInvokerSerializer serializer = HttpInvokerSerializers.forRequest(request);
		try {
			RemoteInvocation invocation = invocationFunction.apply(request);
			long time = System.currentTimeMillis();
			RemoteInvocationResult result = invocationResultFunction.apply(invocation);
			if (result == null) {
				return; // async
			}
			time = System.currentTimeMillis() - time;
			if (serviceStats != null) {
				String service = MDC.get(MDC_KEY_SERVICE);
				int index = service.substring(0, service.indexOf('(')).lastIndexOf('.');
				serviceStats.serverSideEmit(service.substring(0, index), service.substring(index + 1), time);
			}
			if (loggingPayload) {
				if (!result.hasException()) {
					Object value = result.getValue();
					remotingLogger.info("Response: {}", JsonDesensitizer.DEFAULT_INSTANCE.toJson(value));
				} else {
					Throwable throwable = result.getException();
					if (throwable != null && throwable.getCause() != null)
						throwable = throwable.getCause();
					remotingLogger.error("Error:", throwable);
				}
			}
			remotingLogger.info("Invoked from {} in {}ms", MDC.get(AccessFilter.MDC_KEY_REQUEST_FROM), time);
			response.setContentType(serializer.getContentType());
			serializer.writeRemoteInvocationResult(invocation, result, response.getOutputStream());
		} catch (SerializationFailedException sfe) {
			log.error(sfe.getMessage(), sfe);
			response.setHeader(RemotingContext.HTTP_HEADER_EXCEPTION_MESSAGE, sfe.getMessage());
			response.setStatus(RemotingContext.SC_SERIALIZATION_FAILED);
		} catch (Exception ex) {
			try {
				if (!serializer.handleException(ex, response)) {
					log.error(ex.getMessage(), ex);
					response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
				}
			} catch (IOException e) {
				log.error(e.getMessage(), e);
			}
		} finally {
			completion.run();
		}
	}

	private void invokeWithAsyncResult(AsyncContext context, Map<String, String> contextMap,
			RemoteInvocation invocation, RemoteInvocationResult result) {
		MDC.setContextMap(contextMap);
		invoke((HttpServletRequest) context.getRequest(), (HttpServletResponse) context.getResponse(),
				req -> invocation, inv -> result, () -> {
					context.complete();
					MDC.clear();
				});
	}

}
