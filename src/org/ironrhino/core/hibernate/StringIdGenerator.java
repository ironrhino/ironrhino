package org.ironrhino.core.hibernate;

import java.io.Serializable;

import org.hibernate.SessionFactory;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.id.IdentifierGenerator;
import org.ironrhino.core.spring.configuration.BeanPresentConditional;
import org.ironrhino.core.util.CodecUtils;
import org.springframework.stereotype.Component;

@Component
@BeanPresentConditional(type = SessionFactory.class)
public class StringIdGenerator implements IdentifierGenerator {

	@Override
	public Serializable generate(SessionImplementor session, Object obj) {
		return CodecUtils.nextId();
	}

}
