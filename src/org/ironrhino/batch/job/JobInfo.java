package org.ironrhino.batch.job;

import java.io.Serializable;

import org.springframework.batch.core.JobExecution;

import lombok.Data;

@Data
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

}
