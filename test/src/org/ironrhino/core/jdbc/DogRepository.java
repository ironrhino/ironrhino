package org.ironrhino.core.jdbc;

import java.util.function.Consumer;

import org.springframework.transaction.annotation.Transactional;

@JdbcRepository
public interface DogRepository {

	@Transactional
	@Sql("create table dog (id int not null auto_increment primary key, name varchar(255))")
	void createTable();

	@Transactional
	@Sql("drop table dog")
	void dropTable();

	@Transactional
	@Sql("insert into dog(name) values(:dog.name)")
	void save(Dog dog);

	@Transactional
	@Sql("insert into dog(name) values(:name)")
	void insert(String name, Consumer<Integer> keyConsumer);

	@Transactional
	@Sql("delete from dog where id=:id")
	boolean delete(Integer id);

}
