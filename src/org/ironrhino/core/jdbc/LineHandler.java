package org.ironrhino.core.jdbc;

public interface LineHandler {

	boolean isWithHeader();

	String getColumnSeperator();

	String getLineSeperator();

	void handleLine(int index, String line);

}
