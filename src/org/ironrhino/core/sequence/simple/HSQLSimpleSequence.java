package org.ironrhino.core.sequence.simple;

public class HSQLSimpleSequence extends AbstractSequenceSimpleSequence {

	@Override
	protected String getCreateSequenceStatement() {
		return "CREATE SEQUENCE " + getActualSequenceName() + " AS BIGINT START WITH 1";
	}

	@Override
	protected String getQuerySequenceStatement() {
		return "CALL NEXT VALUE FOR " + getActualSequenceName();
	}

}
