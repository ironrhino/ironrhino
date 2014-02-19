package org.ironrhino.core.stat.analysis;

import java.util.Iterator;

import org.ironrhino.core.stat.KeyValuePair;

public interface Analyzer<T> {

	public void analyze();

	public Iterator<? extends KeyValuePair> iterate();

	public T getResult();

}