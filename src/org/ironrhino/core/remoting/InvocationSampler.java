package org.ironrhino.core.remoting;

import java.util.Date;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class InvocationSampler {

	private Lock lock = new ReentrantLock();

	private InvocationSample sample = new InvocationSample();

	public InvocationSampler(String host) {
		sample.setHost(host);
	}

	public void add(long time) {
		lock.lock();
		try {
			sample.setCount(sample.getCount() + 1);
			sample.setTotalTime(sample.getTotalTime() + time);
			Date date = new Date();
			if (sample.getStart() == null)
				sample.setStart(date);
			sample.setEnd(date);
		} finally {
			lock.unlock();
		}
	}

	public InvocationSample peekAndReset() {
		lock.lock();
		try {
			InvocationSample temp = new InvocationSample(sample.getCount(), sample.getTotalTime(), sample.getHost(),
					sample.getStart(), sample.getEnd());
			sample.setCount(0);
			sample.setTotalTime(0);
			sample.setStart(null);
			sample.setEnd(null);
			return temp;
		} finally {
			lock.unlock();
		}
	}

}
