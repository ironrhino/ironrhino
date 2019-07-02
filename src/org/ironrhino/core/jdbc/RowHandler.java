package org.ironrhino.core.jdbc;

public interface RowHandler {

	boolean isWithHeader();

	void handleRow(int index, Object[] row);

}
