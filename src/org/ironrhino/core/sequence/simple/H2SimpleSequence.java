package org.ironrhino.core.sequence.simple;

public class H2SimpleSequence extends AbstractSequenceSimpleSequence {

	@Override
	protected String getQuerySequenceStatement() {
		return "CALL NEXT VALUE FOR " + getActualSequenceName();
	}

}
