package org.ironrhino.core.jdbc;

import java.util.List;
import java.util.function.Consumer;

import org.springframework.beans.factory.annotation.Lookup;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
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

	@Lookup
	JdbcTemplate jdbcTemplate();

	@Transactional(readOnly = true)
	default Dog findDog(String name) {
		List<Dog> dogs = jdbcTemplate().query("select * from dog where name=?", new Object[] { name },
				new BeanPropertyRowMapper<>(Dog.class));
		if (dogs.size() == 1)
			return dogs.get(0);
		return null;
	}

}
