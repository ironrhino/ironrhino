package org.ironrhino.batch.repository;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

public class Jackson2ExecutionContextStringSerializer
		extends org.springframework.batch.core.repository.dao.Jackson2ExecutionContextStringSerializer {

	public Jackson2ExecutionContextStringSerializer() {
		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.configure(MapperFeature.DEFAULT_VIEW_INCLUSION, false);
		objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true);
		objectMapper.activateDefaultTyping(objectMapper.getPolymorphicTypeValidator());
		setObjectMapper(objectMapper);
	}

}
