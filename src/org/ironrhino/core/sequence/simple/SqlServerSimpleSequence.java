package org.ironrhino.core.sequence.simple;

public class SqlServerSimpleSequence extends AbstractSequenceSimpleSequence {

	@Override
	protected String getQuerySequenceStatement() {
		return new StringBuilder("SELECT NEXT VALUE FOR ").append(getActualSequenceName()).toString();
	}

}
