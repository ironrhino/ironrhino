package org.ironrhino.batch.job;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.ironrhino.core.util.DateUtils;
import org.ironrhino.core.util.ReflectionUtils;
import org.springframework.batch.core.JobParameter;
import org.springframework.batch.core.JobParameter.ParameterType;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersValidator;
import org.springframework.batch.core.job.DefaultJobParametersValidator;

public class JobParameterHelper {

	public static List<SimpleJobParameter> parse(String jobParameters) {
		if (StringUtils.isBlank(jobParameters))
			return Collections.emptyList();
		String[] arr = jobParameters.split("\n");
		List<SimpleJobParameter> params = new ArrayList<>(arr.length);
		for (String s : arr) {
			if (StringUtils.isBlank(s)) {
				continue;
			}
			String[] arr2 = s.split("=", 2);
			if (arr2.length < 2)
				throw new IllegalArgumentException("\"" + s + "\" should be key value pair");
			String key = arr2[0].trim();
			boolean identifying = true;
			if (key.charAt(0) == '-')
				identifying = false;
			if (key.charAt(0) == '-' || key.charAt(0) == '+')
				key = key.substring(1);
			ParameterType type = ParameterType.STRING;
			if (key.indexOf('(') > 0 && key.indexOf(')') > 0) {
				type = ParameterType.valueOf(key.substring(key.indexOf('(') + 1, key.indexOf(')')).toUpperCase());
				key = key.substring(0, key.indexOf('('));
			}
			SimpleJobParameter sjp = new SimpleJobParameter();
			sjp.setKey(key);
			sjp.setType(type);
			sjp.setValue(arr2[1].trim());
			sjp.setRequired(identifying);
			params.add(sjp);
		}
		return params;
	}

	public static List<SimpleJobParameter> parse(JobParametersValidator jobParametersValidator) {
		if (jobParametersValidator instanceof SimpleJobParametersValidator) {
			SimpleJobParametersValidator sjpv = (SimpleJobParametersValidator) jobParametersValidator;
			Map<String, ParameterType> requiredKeys = sjpv.getRequiredKeys();
			Map<String, ParameterType> optionalKeys = sjpv.getOptionalKeys();
			List<SimpleJobParameter> params = new ArrayList<>(requiredKeys.size() + optionalKeys.size());
			for (Map.Entry<String, ParameterType> entry : requiredKeys.entrySet()) {
				SimpleJobParameter sjp = new SimpleJobParameter();
				sjp.setKey(entry.getKey());
				sjp.setType(entry.getValue());
				sjp.setRequired(true);
				params.add(sjp);
			}
			for (Map.Entry<String, ParameterType> entry : optionalKeys.entrySet()) {
				SimpleJobParameter sjp = new SimpleJobParameter();
				sjp.setKey(entry.getKey());
				sjp.setType(entry.getValue());
				sjp.setRequired(false);
				params.add(sjp);
			}
			return params;
		} else if (jobParametersValidator instanceof DefaultJobParametersValidator) {
			Collection<String> requiredKeys = ReflectionUtils.getFieldValue(jobParametersValidator, "requiredKeys");
			Collection<String> optionalKeys = ReflectionUtils.getFieldValue(jobParametersValidator, "optionalKeys");
			List<SimpleJobParameter> params = new ArrayList<>(requiredKeys.size() + optionalKeys.size());
			for (String key : requiredKeys) {
				SimpleJobParameter sjp = new SimpleJobParameter();
				sjp.setKey(key);
				sjp.setRequired(true);
				params.add(sjp);
			}
			for (String key : optionalKeys) {
				SimpleJobParameter sjp = new SimpleJobParameter();
				sjp.setKey(key);
				sjp.setRequired(true);
				params.add(sjp);
			}
			return params;
		}
		return Collections.emptyList();
	}

	public static List<SimpleJobParameter> parse(JobParametersValidator jobParametersValidator,
			List<SimpleJobParameter> params) {
		List<SimpleJobParameter> paramList = parse(jobParametersValidator);
		if (paramList.isEmpty())
			return Collections.emptyList();
		if (params != null) {
			if (params.size() != paramList.size())
				throw new IllegalArgumentException("params is illegal");
			for (int i = 0; i < paramList.size(); i++) {
				SimpleJobParameter param1 = paramList.get(i);
				SimpleJobParameter param2 = params.get(i);
				param1.setValue(param2.getValue());
				if (param1.getType() == null)
					param1.setType(param2.getType());
			}
		}
		return paramList;
	}

	public static String toString(JobParameters jobParameters) {
		Map<String, JobParameter> parameters = jobParameters.getParameters();
		if (parameters == null || parameters.isEmpty())
			return "{}";
		StringBuilder sb = new StringBuilder();
		sb.append("{");
		for (Map.Entry<String, JobParameter> entry : parameters.entrySet()) {
			sb.append(entry.getKey()).append("=");
			Object value = entry.getValue().getValue();
			if (entry.getValue().getType() == ParameterType.DATE) {
				sb.append(DateUtils.formatDate10((Date) value));
			} else {
				sb.append(value);
			}
			sb.append(",");
		}
		sb.deleteCharAt(sb.length() - 1);
		sb.append("}");
		return sb.toString();
	}

}
