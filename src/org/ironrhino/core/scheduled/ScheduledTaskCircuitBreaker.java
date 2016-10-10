package org.ironrhino.core.scheduled;

public interface ScheduledTaskCircuitBreaker {

	boolean isShortCircuit(String task);

	void setShortCircuit(String task, boolean value);

}