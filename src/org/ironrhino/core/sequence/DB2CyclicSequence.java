package org.ironrhino.core.sequence;

public class DB2CyclicSequence extends AbstractSequenceCyclicSequence {

	@Override
	protected String getCreateSequenceStatement() {
		StringBuilder sb = new StringBuilder("CREATE SEQUENCE ").append(
				getActualSequenceName()).append(" AS BIGINT START WITH 1");
		if (getCacheSize() > 1)
			sb.append(" CACHE ").append(getCacheSize());
		return sb.toString();
	}

	@Override
	protected String getQuerySequenceStatement() {
		return new StringBuilder("SELECT NEXTVAL FOR ")
				.append(getActualSequenceName())
				.append(" FROM SYSIBM.SYSDUMMY1").toString();
	}

	@Override
	protected String getCurrentTimestamp() {
		return "CURRENT TIMESTAMP";
	}

}
