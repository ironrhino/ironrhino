package org.ironrhino.core.jdbc;

import java.util.List;

import org.springframework.transaction.annotation.Transactional;

@JdbcRepository
@Partition(partitioner = CustomerPartitioner.class)
public interface CustomerRepository {

	@Transactional
	@Sql("create table customer_@{partition} (identifyNo varchar(50) primary key, name varchar(50))")
	void createTable(String partition);

	@Transactional
	@Sql("drop table customer_@{partition}")
	void dropTable(String partition);

	@Transactional
	@PartitionKey("${customer.identifyNo}")
	@Sql("insert into customer_@{PARTITION}(identifyNo, name) values (:customer.identifyNo, :customer.name)")
	void save(Customer customer);

	@Transactional
	@PartitionKey("${identifyNo}")
	@Sql("delete from customer_@{PARTITION} where identifyNo=:identifyNo")
	int delete(String identifyNo);

	@Transactional(readOnly = true)
	@PartitionKey("${identifyNo}")
	@Sql("select * from customer_@{PARTITION} where identifyNo=:identifyNo")
	Customer get(String identifyNo);

	@Transactional(readOnly = true)
	@Sql("select * from customer_@{partition}")
	List<Customer> list(String partition);

}
