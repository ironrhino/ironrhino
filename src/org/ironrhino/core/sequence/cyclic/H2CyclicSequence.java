package org.ironrhino.core.sequence.cyclic;

public class H2CyclicSequence extends AbstractSequenceCyclicSequence {

	@Override
	protected String getQuerySequenceStatement() {
		return new StringBuilder("CALL NEXT VALUE FOR ").append(getActualSequenceName()).toString();
	}

}
