package org.ironrhino.core.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import lombok.Getter;
import lombok.Setter;

public class RoundRobin<T> {

	protected List<TargetWrapper<T>> targetWrappers = new ArrayList<>();

	protected UsableChecker<T> usableChecker;

	public RoundRobin(Collection<T> targets) {
		this(targets, null);
	}

	public RoundRobin(Collection<T> targets, UsableChecker<T> usableChecker) {
		if (targets == null || targets.size() == 0)
			throw new IllegalArgumentException("no target");
		for (T target : targets) {
			TargetWrapper<T> tw = new TargetWrapper<>(target);
			targetWrappers.add(tw);
		}
		this.usableChecker = usableChecker;
	}

	public RoundRobin(Map<T, Integer> targets) {
		this(targets, null);
	}

	public RoundRobin(Map<T, Integer> targets, UsableChecker<T> usableChecker) {
		if (targets == null || targets.size() == 0)
			throw new IllegalArgumentException("no target");
		for (Map.Entry<T, Integer> entry : targets.entrySet()) {
			TargetWrapper<T> tw = new TargetWrapper<>(entry.getKey(), entry.getValue());
			targetWrappers.add(tw);
		}
		this.usableChecker = usableChecker;
	}

	public T pick() {
		int totalWeight = 0;
		TargetWrapper<T> tw = null;
		for (int i = 0; i < targetWrappers.size(); i++) {
			TargetWrapper<T> target = targetWrappers.get(i);
			AtomicInteger targetStat = target.getStat();
			if (!(usableChecker == null || usableChecker.isUsable(target.getTarget())))
				continue;
			totalWeight += target.getWeight();
			int newStat = targetStat.addAndGet(target.getWeight());
			if (tw == null || newStat > tw.getStat().get())
				tw = target;
		}
		if (tw == null)
			return null;
		tw.getStat().addAndGet(-totalWeight);
		return tw.getTarget();
	}

	@FunctionalInterface
	public interface UsableChecker<T> {

		boolean isUsable(T target);

	}

	static class TargetWrapper<T> {

		public static final int DEFAULT_WEIGHT = 1;

		@Getter
		private final T target;

		@Getter
		@Setter
		private int weight = DEFAULT_WEIGHT;

		@Getter
		private AtomicInteger stat = new AtomicInteger(0);

		public TargetWrapper(T target) {
			this.target = target;
		}

		public TargetWrapper(T target, int weight) {
			this.target = target;
			if (weight > 0)
				this.weight = weight;
		}

	}

}
