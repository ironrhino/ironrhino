package org.ironrhino.core.jdbc;

@FunctionalInterface
public interface Partitioner {

	String partition(Object partitionKey);

}