package org.ironrhino.core.remoting.server;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Supplier;

import javax.servlet.AsyncContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.ironrhino.core.metadata.JsonDesensitize;
import org.ironrhino.core.model.NullObject;
import org.ironrhino.core.remoting.RemotingContext;
import org.ironrhino.core.remoting.ServiceRegistry;
import org.ironrhino.core.remoting.serializer.HttpInvokerSerializer;
import org.ironrhino.core.remoting.serializer.HttpInvokerSerializers;
import org.ironrhino.core.remoting.stats.ServiceStats;
import org.ironrhino.core.servlet.AccessFilter;
import org.ironrhino.core.servlet.ProxySupportHttpServletRequest;
import org.ironrhino.core.tracing.Tracing;
import org.ironrhino.core.util.CheckedFunction;
import org.ironrhino.core.util.ExceptionUtils;
import org.ironrhino.core.util.JsonDesensitizer;
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
		MDC.put(MDC_KEY_INTERFACE_NAME, interfaceName);
		Object target = serviceRegistry.getExportedServices().get(interfaceName);
		if (target == null) {
			log.error("Service Not Found: " + interfaceName);
			response.setStatus(HttpServletResponse.SC_NOT_FOUND);
			return;
		}
		AtomicReference<Method> methodHolder = new AtomicReference<>();
		invoke(request, response, req -> {

			Class<?> serviceInterface = ClassUtils.forName(interfaceName, null);
			RemoteInvocation invocation = Tracing.execute("readRemoteInvocation", () -> HttpInvokerSerializers
					.forRequest(req).readRemoteInvocation(serviceInterface, req.getInputStream()));
			List<String> parameterTypeList = new ArrayList<>(invocation.getParameterTypes().length);
			for (Class<?> cl : invocation.getParameterTypes())
				parameterTypeList.add(cl.getSimpleName());
			MDC.put(MDC_KEY_ROLE, "SERVER");
			MDC.put(MDC_KEY_SERVICE, MDC.get(MDC_KEY_INTERFACE_NAME) + '.' + invocation.getMethodName() + "("
					+ String.join(",", parameterTypeList) + ")");
			if (loggingPayload) {
				Object[] arguments = invocation.getArguments();
				Method method = serviceInterface.getMethod(invocation.getMethodName(), invocation.getParameterTypes());
				methodHolder.set(method);
				Parameter[] parameters = method.getParameters();
				JsonDesensitize[] config = new JsonDesensitize[arguments.length];
				for (int i = 0; i < method.getParameterCount(); i++) {
					config[i] = parameters[i].getAnnotation(JsonDesensitize.class);
				}
				remotingLogger.info("Request: {}",
						JsonDesensitizer.DEFAULT_INSTANCE.desensitizeArray(arguments, config));
			}
			return invocation;
		}, invocation -> {
			try {
				return Tracing.execute("invokeAndCreateResult", () -> createRemoteInvocationResult(request, invocation,
						invocation.invoke(target), () -> methodHolder.get()));
			} catch (Throwable ex) {
				if (ex instanceof InvocationTargetException) {
					ex = new InvocationTargetException(
							transform(((InvocationTargetException) ex).getTargetException()));
				}
				Tracing.logError(ex);
				log.error("Processing of " + MDC.get(MDC_KEY_INTERFACE_NAME) + " remote call resulted in exception",
						ex instanceof InvocationTargetException ? ex.getCause() : ex);
				return new RemoteInvocationResult(ex);
			}
		}, () -> {
			MDC.remove(MDC_KEY_INTERFACE_NAME);
			MDC.remove(MDC_KEY_ROLE);
			MDC.remove(MDC_KEY_SERVICE);
		}, () -> methodHolder.get());
	}

	protected RemoteInvocationResult createRemoteInvocationResult(HttpServletRequest request,
			RemoteInvocation invocation, Object value, Supplier<Method> methodSupplier) {
		if (value instanceof Optional) {
			Optional<?> optional = ((Optional<?>) value);
			return new RemoteInvocationResult(optional.orElse(null));
		} else if (value instanceof Callable || value instanceof Future) {
			AsyncContext context = request.startAsync();
			Map<String, String> contextMap = MDC.getCopyOfContextMap();
			// generated lambda function use Span as captured parameter type
			// NoClassDefFoundError could be thrown when perform Class.getDeclaredMethods()
			if (value instanceof CompletionStage<?>) {
				((CompletionStage<?>) value).whenComplete((obj, e) -> {
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
						Tracing.logError(ex);
						asyncResult.setException(new InvocationTargetException(ex));
					}
					invokeWithAsyncResult(context, contextMap, invocation, asyncResult, methodSupplier);
				});
			} else if (value instanceof ListenableFuture) {
				((ListenableFuture<?>) value).addCallback(obj -> {
					RemoteInvocationResult asyncResult = new RemoteInvocationResult();
					asyncResult.setValue(obj);
					invokeWithAsyncResult(context, contextMap, invocation, asyncResult, methodSupplier);
				}, ex -> {
					Tracing.logError(ex);
					invokeWithAsyncResult(context, contextMap, invocation,
							new RemoteInvocationResult(new InvocationTargetException(ex)), methodSupplier);
				});
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
						Tracing.logError(e);
						asyncResult.setException(new InvocationTargetException(e));
					}
					invokeWithAsyncResult(context, contextMap, invocation, asyncResult, methodSupplier);
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
			CheckedFunction<HttpServletRequest, RemoteInvocation, Exception> invocationFunction,
			Function<RemoteInvocation, RemoteInvocationResult> invocationResultFunction, Runnable completion,
			Supplier<Method> methodSupplier) {
		HttpInvokerSerializer serializer = HttpInvokerSerializers.forRequest(request);
		try {
			RemoteInvocation invocation = invocationFunction.apply(request);
			long time = System.nanoTime();
			RemoteInvocationResult result = invocationResultFunction.apply(invocation);
			if (result == null) {
				return; // async
			}
			if (result.getValue() == NullObject.get()) {
				// JSON-RPC notification
				response.setStatus(HttpServletResponse.SC_NO_CONTENT);
				return;
			}
			time = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - time);
			if (serviceStats != null) {
				String service = MDC.get(MDC_KEY_SERVICE);
				int index = service.substring(0, service.indexOf('(')).lastIndexOf('.');
				serviceStats.serverSideEmit(service.substring(0, index), service.substring(index + 1), time);
			}
			if (loggingPayload) {
				if (!result.hasException()) {
					Object value = result.getValue();
					remotingLogger.info("Response: {}", JsonDesensitizer.DEFAULT_INSTANCE.desensitizeValue(value,
							methodSupplier != null ? methodSupplier.get().getAnnotation(JsonDesensitize.class) : null));
				} else {
					Throwable throwable = result.getException();
					if (throwable != null && throwable.getCause() != null)
						throwable = throwable.getCause();
					remotingLogger.error("Error:", throwable);
				}
			}
			remotingLogger.info("Invoked from {} in {}ms", MDC.get(AccessFilter.MDC_KEY_REQUEST_FROM), time);

			Tracing.execute("writeRemoteInvocationResult", () -> {
				response.setContentType(serializer.getContentType());
				serializer.writeRemoteInvocationResult(invocation, result, response.getOutputStream());
				return null;
			});
		} catch (SerializationFailedException sfe) {
			log.error(sfe.getMessage(), sfe);
			response.setHeader(RemotingContext.HTTP_HEADER_EXCEPTION_MESSAGE, sfe.getMessage());
			response.setStatus(RemotingContext.SC_SERIALIZATION_FAILED);
		} catch (Exception ex) {
			try {
				if (!serializer.handleException(ex, request, response)) {
					log.error(ex.getMessage(), ex);
					Tracing.logError(ex);
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
			RemoteInvocation invocation, RemoteInvocationResult result, Supplier<Method> methodSupplier) {
		MDC.setContextMap(contextMap);
		invoke((HttpServletRequest) context.getRequest(), (HttpServletResponse) context.getResponse(),
				req -> invocation, inv -> result, () -> {
					context.complete();
					MDC.clear();
				}, methodSupplier);
	}

}
