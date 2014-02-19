package org.ironrhino.core.jdbc;

public interface LineHandler {

	public boolean isWithHeader();

	public String getColumnSeperator();

	public String getLineSeperator();

	public void handleLine(int index, String line);

}
