package org.ironrhino.core.event;

public class ExpressionEvent extends BaseEvent<String> {

	private static final long serialVersionUID = 6493345801143748086L;

	public ExpressionEvent(String expression) {
		super(expression);
	}

	public String getExpression() {
		return getSource();
	}

	@Override
	public String toString() {
		return getClass().getName() + "[source=" + getSource() + ",timestamp=" + getTimestamp() + "]";
	}

}
