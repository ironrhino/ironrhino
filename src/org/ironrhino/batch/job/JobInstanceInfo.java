package org.ironrhino.batch.job;

import java.io.Serializable;

import org.springframework.batch.core.JobExecution;

import lombok.Data;

@Data
public class JobInstanceInfo implements Serializable {

	private static final long serialVersionUID = 1201440885829966282L;

	private Long id;

	private String jobName;

	private int executionCount;

	private JobExecution lastExecution;

}
