package org.ironrhino.core.sequence.simple;

public class HSQLSimpleSequence extends AbstractSequenceSimpleSequence {

	@Override
	protected String getCreateSequenceStatement() {
		return new StringBuilder("CREATE SEQUENCE ").append(getActualSequenceName()).append(" AS BIGINT START WITH 1")
				.toString();
	}

	@Override
	protected String getQuerySequenceStatement() {
		return new StringBuilder("CALL NEXT VALUE FOR ").append(getActualSequenceName()).toString();
	}

}
