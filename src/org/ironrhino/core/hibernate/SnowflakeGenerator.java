package org.ironrhino.core.hibernate;

import java.io.Serializable;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.IdentifierGenerator;
import org.ironrhino.core.util.Snowflake;
import org.springframework.stereotype.Component;

@Component
@HibernateEnabled
public class SnowflakeGenerator implements IdentifierGenerator {

	@Override
	public Serializable generate(SharedSessionContractImplementor session, Object obj) {
		return Snowflake.DEFAULT_INSTANCE.nextId();
	}

}
