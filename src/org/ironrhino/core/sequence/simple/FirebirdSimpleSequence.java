package org.ironrhino.core.sequence.simple;

public class FirebirdSimpleSequence extends AbstractSequenceSimpleSequence {

	@Override
	protected String getCreateSequenceStatement() {
		return "CREATE SEQUENCE " + getActualSequenceName();
	}

	@Override
	protected String getQuerySequenceStatement() {
		return "SELECT NEXT VALUE FOR " + getActualSequenceName();
	}

}
