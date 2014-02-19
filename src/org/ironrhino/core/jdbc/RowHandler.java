package org.ironrhino.core.jdbc;

public interface RowHandler {

	public boolean isWithHeader();

	public void handleRow(int index, Object[] row);

}
