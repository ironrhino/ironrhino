package org.ironrhino.batch.job;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;

import org.springframework.batch.core.JobParameter;
import org.springframework.batch.core.JobParameter.ParameterType;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.JobParametersValidator;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

public class SimpleJobParametersValidator implements JobParametersValidator, InitializingBean {

	private Map<String, ParameterType> requiredKeys;

	private Map<String, ParameterType> optionalKeys;

	public SimpleJobParametersValidator() {

	}

	public SimpleJobParametersValidator(Map<String, ParameterType> requiredKeys,
			Map<String, ParameterType> optionalKeys) {
		setRequiredKeys(requiredKeys);
		setOptionalKeys(optionalKeys);
	}

	public Map<String, ParameterType> getRequiredKeys() {
		return requiredKeys;
	}

	public void setRequiredKeys(Map<String, ParameterType> requiredKeys) {
		this.requiredKeys = requiredKeys;
	}

	public Map<String, ParameterType> getOptionalKeys() {
		return optionalKeys;
	}

	public void setOptionalKeys(Map<String, ParameterType> optionalKeys) {
		this.optionalKeys = optionalKeys;
	}

	@Override
	public void afterPropertiesSet() throws IllegalStateException {
		if (requiredKeys == null)
			requiredKeys = Collections.emptyMap();
		if (optionalKeys == null)
			optionalKeys = Collections.emptyMap();
		for (String key : requiredKeys.keySet()) {
			Assert.state(!optionalKeys.containsKey(key), "Optional keys canot be required: " + key);
		}
	}

	@Override
	public void validate(JobParameters parameters) throws JobParametersInvalidException {

		if (parameters == null) {
			throw new JobParametersInvalidException("The JobParameters can not be null");
		}

		Map<String, JobParameter> paramteters = parameters.getParameters();

		Collection<String> missingKeys = new HashSet<String>();
		for (Map.Entry<String, JobParameter> entry : paramteters.entrySet()) {
			String key = entry.getKey();
			if (!optionalKeys.containsKey(key) && !requiredKeys.containsKey(key)) {
				missingKeys.add(key);
			} else {
				ParameterType actualType = entry.getValue().getType();
				ParameterType expectedType = requiredKeys.get(key);
				if (expectedType == null)
					expectedType = optionalKeys.get(key);
				if (actualType != expectedType)
					throw new JobParametersInvalidException(
							"The JobParameter [" + key + "] is not type of " + expectedType);
			}
		}
		if (!missingKeys.isEmpty()) {
			throw new JobParametersInvalidException(
					"The JobParameters contains keys that are not explicitly optional or required: " + missingKeys);
		}

		missingKeys = new HashSet<String>();
		for (String key : requiredKeys.keySet()) {
			if (!paramteters.containsKey(key)) {
				missingKeys.add(key);
			}
		}
		if (!missingKeys.isEmpty()) {
			throw new JobParametersInvalidException("The JobParameters do not contain required keys: " + missingKeys);
		}

	}

}