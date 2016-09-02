package org.ironrhino.batch.job;

import java.io.Serializable;

import org.springframework.batch.core.JobExecution;

public class JobInfo implements Serializable {

	private static final long serialVersionUID = 1131849993495534195L;

	private String name;

	private String description;

	private boolean incrementable;

	private boolean restartable;

	private boolean launchable;

	private JobExecution lastExecution;

	public String getId() {
		return getName();
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public boolean isIncrementable() {
		return incrementable;
	}

	public void setIncrementable(boolean incrementable) {
		this.incrementable = incrementable;
	}

	public boolean isRestartable() {
		return restartable;
	}

	public void setRestartable(boolean restartable) {
		this.restartable = restartable;
	}

	public boolean isLaunchable() {
		return launchable;
	}

	public void setLaunchable(boolean launchable) {
		this.launchable = launchable;
	}

	public JobExecution getLastExecution() {
		return lastExecution;
	}

	public void setLastExecution(JobExecution lastExecution) {
		this.lastExecution = lastExecution;
	}

}
