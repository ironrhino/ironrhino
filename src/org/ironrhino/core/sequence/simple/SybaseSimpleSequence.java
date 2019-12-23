package org.ironrhino.core.sequence.simple;

public class SybaseSimpleSequence extends AbstractSequenceSimpleSequence {

	@Override
	protected String getQuerySequenceStatement() {
		return "SELECT " + getActualSequenceName() + ".NEXTVAL";
	}

}