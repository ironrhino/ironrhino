package org.ironrhino.core.hibernate;

import java.io.Serializable;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.IdentifierGenerator;
import org.ironrhino.core.spring.configuration.ResourcePresentConditional;
import org.ironrhino.core.util.CodecUtils;
import org.springframework.stereotype.Component;

@Component
@ResourcePresentConditional("resources/spring/applicationContext-hibernate.xml")
public class StringIdGenerator implements IdentifierGenerator {

	@Override
	public Serializable generate(SharedSessionContractImplementor session, Object obj) {
		return CodecUtils.nextId();
	}

}
