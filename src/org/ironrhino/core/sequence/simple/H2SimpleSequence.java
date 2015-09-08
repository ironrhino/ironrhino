package org.ironrhino.core.sequence.simple;

public class H2SimpleSequence extends AbstractSequenceSimpleSequence {

	@Override
	protected String getQuerySequenceStatement() {
		return new StringBuilder("CALL NEXT VALUE FOR ").append(getActualSequenceName()).toString();
	}

}
