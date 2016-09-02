package org.ironrhino.batch.job;

import java.io.Serializable;

import org.springframework.batch.core.JobExecution;

public class JobInstanceInfo implements Serializable {

	private static final long serialVersionUID = 1201440885829966282L;

	private Long id;

	private String jobName;

	private int executionCount;

	private JobExecution lastExecution;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getJobName() {
		return jobName;
	}

	public void setJobName(String jobName) {
		this.jobName = jobName;
	}

	public int getExecutionCount() {
		return executionCount;
	}

	public void setExecutionCount(int executionCount) {
		this.executionCount = executionCount;
	}

	public JobExecution getLastExecution() {
		return lastExecution;
	}

	public void setLastExecution(JobExecution lastExecution) {
		this.lastExecution = lastExecution;
	}

}
