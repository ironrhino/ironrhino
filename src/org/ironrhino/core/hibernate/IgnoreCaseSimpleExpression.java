package org.ironrhino.core.hibernate;

import org.hibernate.criterion.SimpleExpression;

public class IgnoreCaseSimpleExpression extends SimpleExpression {

	private static final long serialVersionUID = 5753723498803085559L;

	public IgnoreCaseSimpleExpression(String propertyName, Object value, String op) {
		super(propertyName, value, op, true);
	}

}
