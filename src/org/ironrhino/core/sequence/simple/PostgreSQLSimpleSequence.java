package org.ironrhino.core.sequence.simple;

public class PostgreSQLSimpleSequence extends AbstractSequenceSimpleSequence {

	@Override
	protected String getQuerySequenceStatement() {
		return "SELECT NEXTVAL('" + getActualSequenceName() + "')";
	}

}
