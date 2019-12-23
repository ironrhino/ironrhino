package org.ironrhino.core.sequence.simple;

public class MariaDBSimpleSequence extends AbstractSequenceSimpleSequence {

	@Override
	protected String getQuerySequenceStatement() {
		return "SELECT NEXTVAL(" + getActualSequenceName() + ")";
	}

}
