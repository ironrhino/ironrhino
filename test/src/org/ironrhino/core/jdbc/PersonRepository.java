package org.ironrhino.core.jdbc;

import java.util.List;

import org.ironrhino.core.jdbc.JdbcRepository;
import org.springframework.transaction.annotation.Transactional;

@JdbcRepository
public interface PersonRepository {
	
	@Transactional
	void createTable();
	
	@Transactional
	void dropTable();

	@Transactional
	void save(Person person);

	@Transactional
	int delete(String name);

	@Transactional(readOnly = true)
	Person get(String name);

	@Transactional(readOnly = true)
	List<Person> list();

	@Transactional(readOnly = true)
	List<Person> search(String namePrefix);

}
