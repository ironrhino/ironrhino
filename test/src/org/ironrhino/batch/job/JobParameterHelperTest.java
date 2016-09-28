package org.ironrhino.batch.job;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.springframework.batch.core.JobParameter.ParameterType;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.JobParametersValidator;
import org.springframework.batch.core.job.DefaultJobParametersValidator;

public class JobParameterHelperTest {

	@Test
	public void testParseString() {
		List<SimpleJobParameter> parameters = JobParameterHelper.parse("");
		assertTrue(parameters.isEmpty());
		parameters = JobParameterHelper.parse("key=value\n\n-key2(long)=12345");
		assertEquals(2, parameters.size());
		assertEquals(ParameterType.STRING, parameters.get(0).getType());
		assertTrue(parameters.get(0).isRequired());
		assertEquals(ParameterType.LONG, parameters.get(1).getType());
		assertFalse(parameters.get(1).isRequired());
	}

	@Test
	public void testJobParametersValidator() {
		List<SimpleJobParameter> parameters = JobParameterHelper.parse(new JobParametersValidator() {

			@Override
			public void validate(JobParameters paramJobParameters) throws JobParametersInvalidException {

			}

		});
		assertTrue(parameters.isEmpty());
		SimpleJobParametersValidator sjpv = new SimpleJobParametersValidator();
		Map<String, ParameterType> requiredKeys = new LinkedHashMap<>();
		requiredKeys.put("key", ParameterType.STRING);
		Map<String, ParameterType> optionalKeys = new LinkedHashMap<>();
		optionalKeys.put("key2", ParameterType.LONG);
		sjpv.setRequiredKeys(requiredKeys);
		sjpv.setOptionalKeys(optionalKeys);
		parameters = JobParameterHelper.parse(sjpv);
		assertEquals(2, parameters.size());
		assertEquals(ParameterType.STRING, parameters.get(0).getType());
		assertTrue(parameters.get(0).isRequired());
		assertEquals(ParameterType.LONG, parameters.get(1).getType());
		assertFalse(parameters.get(1).isRequired());
		DefaultJobParametersValidator djpv = new DefaultJobParametersValidator();
		djpv.setRequiredKeys(new String[] { "key" });
		djpv.setOptionalKeys(new String[] { "key2" });
		parameters = JobParameterHelper.parse(sjpv);
		assertEquals(2, parameters.size());
		assertEquals(ParameterType.STRING, parameters.get(0).getType());
		assertTrue(parameters.get(0).isRequired());
		assertEquals(ParameterType.LONG, parameters.get(1).getType());
		assertFalse(parameters.get(1).isRequired());
	}

}
