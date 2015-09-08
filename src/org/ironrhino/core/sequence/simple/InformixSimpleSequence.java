package org.ironrhino.core.sequence.simple;

public class InformixSimpleSequence extends AbstractSequenceSimpleSequence {

	@Override
	protected String getQuerySequenceStatement() {
		return new StringBuilder("SELECT ").append(getActualSequenceName())
				.append(".NEXTVAL FROM INFORMIX.SYSTABLES WHERE TABID=1").toString();
	}

}
