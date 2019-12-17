package org.ironrhino.batch.partition;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.FutureTask;

import org.ironrhino.batch.JobStepExecutor;
import org.ironrhino.core.coordination.Membership;
import org.ironrhino.core.remoting.client.HttpInvokerClient;
import org.ironrhino.core.remoting.client.HttpInvokerRequestExecutor;
import org.ironrhino.core.remoting.client.SimpleHttpInvokerRequestExecutor;
import org.ironrhino.core.util.AppInfo;
import org.ironrhino.core.util.RoundRobin;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.partition.support.TaskExecutorPartitionHandler;
import org.springframework.beans.factory.annotation.Autowired;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RemoteInvocationPartitionHandler extends TaskExecutorPartitionHandler {

	@Autowired
	private JobStepExecutor localJobStepExecutor;

	@Autowired(required = false)
	private Membership membership;

	private RoundRobin<String> roundRobin;

	private Map<String, JobStepExecutor> remoteJobStepExecutors = new ConcurrentHashMap<>();

	@Setter
	private int connectTimeout;

	@Setter
	private int readTimeout;

	@Override
	protected Set<StepExecution> doHandle(StepExecution managerStepExecution,
			Set<StepExecution> partitionStepExecutions) throws Exception {
		if (membership != null)
			roundRobin = new RoundRobin<>(membership.getMembers(AppInfo.getAppName()));
		return super.doHandle(managerStepExecution, partitionStepExecutions);
	}

	@Override
	protected FutureTask<StepExecution> createTask(Step step, StepExecution stepExecution) {
		return new FutureTask<>(new Callable<StepExecution>() {
			@Override
			public StepExecution call() throws Exception {
				String jobName = stepExecution.getJobExecution().getJobInstance().getJobName();
				Long jobExecutionId = stepExecution.getJobExecutionId();
				Long stepExecutionId = stepExecution.getId();
				String stepName = step.getName();

				String instanceId = roundRobin != null ? roundRobin.pick() : null;
				JobStepExecutor jobStepExecutor;
				if (instanceId != null && !instanceId.equals(AppInfo.getInstanceId())) {
					String host = instanceId.substring(instanceId.lastIndexOf('@') + 1);
					jobStepExecutor = createJobStepExecutor(host);
					log.info("Using remote JobStepExecutor [{}] to execute step[{}#{}] of job[{}#{}]", host, stepName,
							stepExecutionId, jobName, jobExecutionId);
				} else {
					jobStepExecutor = localJobStepExecutor;
					log.info("Using local JobStepExecutor to execute step[{}#{}] of job[{}#{}]", stepName,
							stepExecutionId, jobName, jobExecutionId);
				}
				jobStepExecutor.execute(jobExecutionId, stepExecutionId, stepName);
				return stepExecution;
			}
		});
	}

	protected JobStepExecutor createJobStepExecutor(String host) {
		return remoteJobStepExecutors.computeIfAbsent(host, key -> {
			HttpInvokerRequestExecutor httpInvokerRequestExecutor = new SimpleHttpInvokerRequestExecutor();
			httpInvokerRequestExecutor.setConnectTimeout(connectTimeout);
			httpInvokerRequestExecutor.setReadTimeout(readTimeout);
			HttpInvokerClient hic = new HttpInvokerClient();
			hic.setServiceInterface(JobStepExecutor.class);
			hic.setHttpInvokerRequestExecutor(httpInvokerRequestExecutor);
			hic.setBaseUrl("http://" + key);
			hic.afterPropertiesSet();
			return (JobStepExecutor) hic.getObject();
		});
	}

}