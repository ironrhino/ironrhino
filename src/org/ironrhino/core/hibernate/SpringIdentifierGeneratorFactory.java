package org.ironrhino.core.hibernate;

import java.util.Map;
import java.util.Properties;

import org.hibernate.id.IdentifierGenerator;
import org.hibernate.id.factory.internal.DefaultIdentifierGeneratorFactory;
import org.hibernate.type.Type;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class SpringIdentifierGeneratorFactory extends DefaultIdentifierGeneratorFactory {

	private static final long serialVersionUID = 5422614562567921257L;

	@Autowired(required = false)
	private Map<String, IdentifierGenerator> identifierGenerators;

	@Override
	public IdentifierGenerator createIdentifierGenerator(String strategy, Type type, Properties config) {
		IdentifierGenerator generator = tryFindIdentifierGenerator(strategy);
		if (generator != null)
			return generator;
		return super.createIdentifierGenerator(strategy, type, config);
	}

	@Override
	public Class<?> getIdentifierGeneratorClass(String strategy) {
		IdentifierGenerator generator = tryFindIdentifierGenerator(strategy);
		if (generator != null)
			return generator.getClass();
		return super.getIdentifierGeneratorClass(strategy);
	}

	protected IdentifierGenerator tryFindIdentifierGenerator(String strategy) {
		if (identifierGenerators != null) {
			for (Map.Entry<String, IdentifierGenerator> entry : identifierGenerators.entrySet()) {
				String name = entry.getKey();
				IdentifierGenerator generator = entry.getValue();
				if (name.equals(strategy) || generator.getClass().getName().equals(strategy)
						|| name.equals(strategy + "Generator") || name.equals(strategy + "IdentifierGenerator"))
					return generator;
			}
		}
		return null;
	}

}
