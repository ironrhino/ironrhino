package org.ironrhino.core.spring;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.PostConstruct;

import org.aopalliance.intercept.MethodInvocation;
import org.ironrhino.core.metadata.Profiles;
import org.ironrhino.core.util.ThrowableFunction;
import org.springframework.aop.support.JdkRegexpMethodPointcut;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile(Profiles.CHAOS)
public class ChaosMethodInvocationFilter implements MethodInvocationFilter {

	private static final Map<Method, AtomicInteger> counters = new ConcurrentHashMap<>();

	private static final Random random = new Random();

	@Value("${chaosMethodInvocationFilter.pattern:.*}")
	private String pattern;

	@Value("${chaosMethodInvocationFilter.strategy:ALWAYS}")
	private Strategy strategy;

	@Value("${chaosMethodInvocationFilter.attack:EXCEPTION}")
	private Attack attack;

	@Value("${chaosMethodInvocationFilter.attack.exception:}")
	private String exception;

	@Value("${chaosMethodInvocationFilter.attack.latency:5000-20000}")
	private String latency;

	private JdkRegexpMethodPointcut pointcut;

	@PostConstruct
	public void init() {
		pointcut = new JdkRegexpMethodPointcut();
		pointcut.setPattern(pattern);
	}

	public Object filter(MethodInvocation methodInvocation,
			ThrowableFunction<MethodInvocation, Object, Throwable> actualInvocation) throws Throwable {
		Method method = methodInvocation.getMethod();
		Class<?> clazz = method.getDeclaringClass();
		if (pointcut.getClassFilter().matches(clazz) && pointcut.getMethodMatcher().matches(method, clazz)) {
			if (strategy.shouldCreateChaos(method))
				attack.perform(this);
		}
		return actualInvocation.apply(methodInvocation);
	}

	static enum Strategy {
		ALWAYS {
			boolean shouldCreateChaos(Method method) {
				return true;
			}
		},
		ALTERNATING {
			boolean shouldCreateChaos(Method method) {
				return counters.computeIfAbsent(method, m -> new AtomicInteger()).getAndIncrement() % 2 == 0;
			}
		},
		RANDOM {
			boolean shouldCreateChaos(Method method) {
				return random.nextBoolean();
			}
		},
		NONE {
			boolean shouldCreateChaos(Method method) {
				return false;
			}
		};

		abstract boolean shouldCreateChaos(Method method);
	}

	static enum Attack {
		EXCEPTION {
			void perform(ChaosMethodInvocationFilter _this) {
				String message = "Chaos created";
				String ex = _this.exception;
				RuntimeException re;
				if (!ex.isEmpty()) {
					String[] arr = ex.split(":\\s*", 2);
					try {
						Class<?> c = Class.forName(arr[0]);
						if (arr.length > 1)
							message = arr[1];
						re = (RuntimeException) c.getConstructor(String.class).newInstance(message);
					} catch (Exception e) {
						re = new RuntimeException(e);
					}
				} else {
					re = new RuntimeException(message);
				}
				throw re;
			}
		},
		LATENCY {
			void perform(ChaosMethodInvocationFilter _this) {
				try {
					String s = _this.latency;
					int index = s.indexOf('-');
					if (index < 0) {
						Thread.sleep(Long.parseLong(s));
					} else {
						long min = Long.parseLong(s.substring(0, index));
						long max = Long.parseLong(s.substring(index + 1));
						long latency = min + random.nextInt((int) (max + 1 - min));
						Thread.sleep(latency);
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		},
		KILL {
			void perform(ChaosMethodInvocationFilter _this) {
				System.exit(1);
			}
		};

		abstract void perform(ChaosMethodInvocationFilter _this);
	}

}
