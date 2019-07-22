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

	@Value("${chaosMethodInvocationFilter.pattern:.*}")
	private String pattern;

	@Value("${chaosMethodInvocationFilter.strategy:ALWAYS}")
	private Strategy strategy;

	@Value("${chaosMethodInvocationFilter.attack:EXCEPTION}")
	private Attack attack;

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
				attack.perform();
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

		private static final Random random = new Random();

		private static final Map<Method, AtomicInteger> counters = new ConcurrentHashMap<>();

		abstract boolean shouldCreateChaos(Method method);
	}

	static enum Attack {
		EXCEPTION {
			void perform() {
				throw new RuntimeException("Chaos created");
			}
		},
		LATENCY {
			void perform() {
				try {
					Thread.sleep(20000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		},
		KILL {
			void perform() {
				System.exit(1);
			}
		};

		abstract void perform();
	}

}
