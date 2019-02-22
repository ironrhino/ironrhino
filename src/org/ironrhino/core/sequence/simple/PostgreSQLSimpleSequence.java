package org.ironrhino.core.sequence.simple;

public class PostgreSQLSimpleSequence extends AbstractSequenceSimpleSequence {

	@Override
	protected String getQuerySequenceStatement() {
		return new StringBuilder("SELECT NEXTVAL('").append(getActualSequenceName()).append("')").toString();
	}

}
