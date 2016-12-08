package org.ironrhino.core.jdbc;

import java.util.List;

import javax.persistence.EnumType;

import org.ironrhino.common.model.Gender;
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
	List<Person> listByGender(@Enumerated(EnumType.STRING) Gender gender);

	@Transactional(readOnly = true)
	List<Person> search(String namePrefix);

}
