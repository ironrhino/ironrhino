package org.ironrhino.core.jdbc;

@FunctionalInterface
public interface Partitioner {

	public String partition(Object partitionKey);

}