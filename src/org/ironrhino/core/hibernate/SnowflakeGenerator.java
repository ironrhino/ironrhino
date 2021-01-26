package org.ironrhino.core.hibernate;

import java.io.Serializable;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.IdentifierGenerator;
import org.ironrhino.core.util.Snowflake;
import org.springframework.stereotype.Component;

@Component
@HibernateEnabled
public class SnowflakeGenerator implements IdentifierGenerator {

	private final Snowflake snowflake = Snowflake.DEFAULT_INSTANCE;

	@Override
	public Serializable generate(SharedSessionContractImplementor session, Object obj) {
		Class<?> idType = session.getFactory().getMetamodel().entity(obj.getClass()).getIdType().getJavaType();
		return idType == String.class ? snowflake.nextBase62Id() : snowflake.nextId();
	}

}
