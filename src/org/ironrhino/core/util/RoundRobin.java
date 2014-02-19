package org.ironrhino.core.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

public class RoundRobin<T> {

	protected List<TargetWrapper<T>> targetWrappers = new ArrayList<TargetWrapper<T>>();

	protected UsableChecker<T> usableChecker;

	public RoundRobin(Collection<T> targets) {
		this(targets, null);
	}

	public RoundRobin(Collection<T> targets, UsableChecker<T> usableChecker) {
		if (targets == null || targets.size() == 0)
			throw new IllegalArgumentException("no target");
		for (T target : targets) {
			TargetWrapper<T> tw = new TargetWrapper<T>(target);
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
			TargetWrapper<T> tw = new TargetWrapper<T>(entry.getKey(),
					entry.getValue());
			targetWrappers.add(tw);
		}
		this.usableChecker = usableChecker;
	}

	public T pick() {
		int totalWeight = 0;
		TargetWrapper<T> tw = null;
		for (int i = 0; i < targetWrappers.size(); i++) {
			TargetWrapper<T> temp = targetWrappers.get(i);
			AtomicLong tempStat = temp.getStat();
			if (!(usableChecker == null || usableChecker.isUsable(temp
					.getTarget())))
				continue;
			long newStat = tempStat.get();
			newStat += temp.getWeight();
			totalWeight += temp.getWeight();
			tempStat.set(newStat);
			if (tw == null || newStat > tw.getStat().get())
				tw = temp;
		}
		if (tw == null)
			return null;
		tw.getStat().addAndGet(-totalWeight);
		return tw.getTarget();
	}

	public static interface UsableChecker<T> {

		boolean isUsable(T target);

	}

	static class TargetWrapper<T> {

		public static final int DEFAULT_WEIGHT = 1;

		private T target;

		private int weight = DEFAULT_WEIGHT;

		private AtomicLong stat = new AtomicLong(0);

		public TargetWrapper(T target) {
			this.target = target;
		}

		public TargetWrapper(T target, int weight) {
			this.target = target;
			if (weight > 0)
				this.weight = weight;
		}

		public T getTarget() {
			return target;
		}

		public int getWeight() {
			return weight;
		}

		public void setWeight(int weight) {
			this.weight = weight;
		}

		public AtomicLong getStat() {
			return stat;
		}
	}

}
