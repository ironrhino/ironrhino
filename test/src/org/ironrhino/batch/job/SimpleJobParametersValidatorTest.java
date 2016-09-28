package org.ironrhino.batch.job;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.batch.core.JobParameter.ParameterType;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.JobParametersInvalidException;

public class SimpleJobParametersValidatorTest {

	static SimpleJobParametersValidator jobParametersValidator;

	@BeforeClass
	public static void init() {
		jobParametersValidator = new SimpleJobParametersValidator();
		Map<String, ParameterType> requiredKeys = new LinkedHashMap<>();
		requiredKeys.put("key", ParameterType.STRING);
		Map<String, ParameterType> optionalKeys = new LinkedHashMap<>();
		optionalKeys.put("key2", ParameterType.LONG);
		jobParametersValidator.setRequiredKeys(requiredKeys);
		jobParametersValidator.setOptionalKeys(optionalKeys);
	}

	@Test(expected = JobParametersInvalidException.class)
	public void testValidateWithEmpty() throws JobParametersInvalidException {
		JobParametersBuilder jobParametersBuilder = new JobParametersBuilder();
		jobParametersValidator.validate(jobParametersBuilder.toJobParameters());
	}

	@Test(expected = JobParametersInvalidException.class)
	public void testValidateWithMissingRequiredKey() throws JobParametersInvalidException {
		JobParametersBuilder jobParametersBuilder = new JobParametersBuilder();
		jobParametersBuilder.addLong("key2", 1234L);
		jobParametersValidator.validate(jobParametersBuilder.toJobParameters());
	}

	@Test(expected = JobParametersInvalidException.class)
	public void testValidateWithWrongType() throws JobParametersInvalidException {
		JobParametersBuilder jobParametersBuilder = new JobParametersBuilder();
		jobParametersBuilder.addLong("key", 1234L);
		jobParametersValidator.validate(jobParametersBuilder.toJobParameters());
	}

	@Test
	public void testValidateWithNormal() throws JobParametersInvalidException {
		JobParametersBuilder jobParametersBuilder = new JobParametersBuilder();
		jobParametersBuilder.addString("key", "test");
		jobParametersBuilder.addLong("key2", 1234L);
		jobParametersValidator.validate(jobParametersBuilder.toJobParameters());
		jobParametersBuilder = new JobParametersBuilder();
		jobParametersBuilder.addString("key", "test");
		jobParametersValidator.validate(jobParametersBuilder.toJobParameters());
	}

}
