package org.ironrhino.core.spring;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.PostConstruct;

import org.aopalliance.intercept.MethodInvocation;
import org.ironrhino.core.metadata.Profiles;
import org.ironrhino.core.util.CheckedFunction;
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

	@Override
	public Object filter(MethodInvocation methodInvocation,
			CheckedFunction<MethodInvocation, Object, Throwable> actualInvocation) throws Throwable {
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
			@Override
			boolean shouldCreateChaos(Method method) {
				return true;
			}
		},
		ALTERNATING {
			@Override
			boolean shouldCreateChaos(Method method) {
				return counters.computeIfAbsent(method, m -> new AtomicInteger()).getAndIncrement() % 2 == 0;
			}
		},
		RANDOM {
			@Override
			boolean shouldCreateChaos(Method method) {
				return random.nextBoolean();
			}
		},
		NONE {
			@Override
			boolean shouldCreateChaos(Method method) {
				return false;
			}
		};

		abstract boolean shouldCreateChaos(Method method);
	}

	static enum Attack {
		EXCEPTION {
			@Override
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
			@Override
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
		OOM {
			@Override
			void perform(ChaosMethodInvocationFilter _this) {
				Map<String, String> map = new HashMap<>();
				int counter = 0;
				while (true) {
					StringBuilder value = new StringBuilder();
					for (int i = 0; i < 10000; i++)
						value.append("value");
					map.put("key" + counter, value.toString());
					++counter;
				}
			}
		},
		MEMORY_LEAK {
			List<String> list = new LinkedList<>();

			@Override
			void perform(ChaosMethodInvocationFilter _this) {
				StringBuilder value = new StringBuilder();
				for (int i = 0; i < 10000; i++)
					value.append("value");
				list.add(value.toString());
			}
		},
		STACK_OVERFLOW {
			@Override
			void perform(ChaosMethodInvocationFilter _this) {
				perform(_this);
			}
		},
		CPU_SPIKE {
			@Override
			void perform(ChaosMethodInvocationFilter _this) {
				for (int i = 0; i < 10; i++)
					new Thread(() -> {
						while (true) {
						}
					}, "Chaos-CPUSpikerThread-" + i).start();
			}
		},
		DEADLOCK {
			@Override
			void perform(ChaosMethodInvocationFilter _this) {
				Object lock1 = new Object();
				Object lock2 = new Object();
				new Thread(() -> {
					synchronized (lock1) {
						try {
							Thread.sleep(10);
						} catch (InterruptedException e) {
						}
						synchronized (lock2) {
						}
					}
				}, "Chaos-DeadlockThread-1").start();
				new Thread(() -> {
					synchronized (lock2) {
						try {
							Thread.sleep(10);
						} catch (InterruptedException e) {
						}
						synchronized (lock1) {
						}
					}
				}, "Chaos-DeadlockThread-2").start();
			}
		},
		THREAD_LEAK {
			@Override
			void perform(ChaosMethodInvocationFilter _this) {
				new Thread(() -> {
					while (true) {
						try {
							Thread.sleep(10000);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
				}, "Chaos-LeakThread").start();
			}
		},
		KILL {
			@Override
			void perform(ChaosMethodInvocationFilter _this) {
				System.exit(1);
			}
		};

		abstract void perform(ChaosMethodInvocationFilter _this);
	}

}
