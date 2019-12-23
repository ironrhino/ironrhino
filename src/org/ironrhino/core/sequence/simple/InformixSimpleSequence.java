package org.ironrhino.core.sequence.simple;

public class InformixSimpleSequence extends AbstractSequenceSimpleSequence {

	@Override
	protected String getQuerySequenceStatement() {
		return "SELECT " + getActualSequenceName() + ".NEXTVAL FROM INFORMIX.SYSTABLES WHERE TABID=1";
	}

}
