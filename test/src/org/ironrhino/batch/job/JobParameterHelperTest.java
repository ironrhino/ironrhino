package org.ironrhino.batch.job;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.springframework.batch.core.JobParameter.ParameterType;
import org.springframework.batch.core.job.DefaultJobParametersValidator;

public class JobParameterHelperTest {

	@Test
	public void testParseString() {
		List<SimpleJobParameter> parameters = JobParameterHelper.parse("");
		assertThat(parameters.isEmpty(), is(true));
		parameters = JobParameterHelper.parse("key=value\n\n-key2(long)=12345");
		assertThat(parameters.size(), is(2));
		assertThat(parameters.get(0).getType(), is(ParameterType.STRING));
		assertThat(parameters.get(0).isRequired(), is(true));
		assertThat(parameters.get(1).getType(), is(ParameterType.LONG));
		assertThat(parameters.get(1).isRequired(), is(false));
	}

	@Test
	public void testJobParametersValidator() {
		List<SimpleJobParameter> parameters = JobParameterHelper.parse(paramJobParameters -> {
		});
		assertThat(parameters.isEmpty(), is(true));
		SimpleJobParametersValidator sjpv = new SimpleJobParametersValidator();
		Map<String, ParameterType> requiredKeys = new LinkedHashMap<>();
		requiredKeys.put("key", ParameterType.STRING);
		Map<String, ParameterType> optionalKeys = new LinkedHashMap<>();
		optionalKeys.put("key2", ParameterType.LONG);
		sjpv.setRequiredKeys(requiredKeys);
		sjpv.setOptionalKeys(optionalKeys);
		parameters = JobParameterHelper.parse(sjpv);
		assertThat(parameters.size(), is(2));
		assertThat(parameters.get(0).getType(), is(ParameterType.STRING));
		assertThat(parameters.get(0).isRequired(), is(true));
		assertThat(parameters.get(1).getType(), is(ParameterType.LONG));
		assertThat(parameters.get(1).isRequired(), is(false));
		DefaultJobParametersValidator djpv = new DefaultJobParametersValidator();
		djpv.setRequiredKeys(new String[] { "key" });
		djpv.setOptionalKeys(new String[] { "key2" });
		parameters = JobParameterHelper.parse(sjpv);
		assertThat(parameters.size(), is(2));
		assertThat(parameters.get(0).getType(), is(ParameterType.STRING));
		assertThat(parameters.get(0).isRequired(), is(true));
		assertThat(parameters.get(1).getType(), is(ParameterType.LONG));
		assertThat(parameters.get(1).isRequired(), is(false));
	}

}
