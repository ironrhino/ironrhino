package org.ironrhino.core.hibernate;

import java.io.Serializable;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.IdentifierGenerator;
import org.ironrhino.core.util.AppInfo;
import org.ironrhino.core.util.Snowflake;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Component
@HibernateEnabled
@Slf4j
public class SnowflakeGenerator implements IdentifierGenerator {

	private final Snowflake snowflake;

	public SnowflakeGenerator() {
		int workerId = 0;
		String id = AppInfo.getEnv("worker.id");
		if (id == null) {
			String ip = AppInfo.getHostAddress();
			id = ip.substring(ip.lastIndexOf('.') + 1);
		}
		if (StringUtils.isNumeric(id)) {
			workerId = Integer.parseInt(id);
		}
		log.info("Snowflake worker id is {}", workerId);
		snowflake = new Snowflake(workerId);
	}

	@Override
	public Serializable generate(SharedSessionContractImplementor session, Object obj) {
		return snowflake.nextId();
	}

}
