package org.ironrhino.core.jdbc;

import java.util.EnumSet;
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

	@Transactional(readOnly = true)
	@Sql("select count(*) from t_person")
	long count();

	@Transactional(readOnly = true)
	@Sql("select count(*) from t_person where name like concat(:namePrefix,'%')")
	int countByNamePrefix(String namePrefix);

	@Transactional(readOnly = true)
	@Sql("select name from t_person")
	List<String> listNames();

	@Transactional(readOnly = true)
	@Sql("select age from t_person")
	List<Integer> listAges();
	
	@Transactional(readOnly = true)
	@Sql("select * from t_person where name in (:names)")
	List<Person> getByNames(String[] names);
	
	@Transactional(readOnly = true)
	@Sql("select * from t_person where gender in (:genders)")
	List<Person> getByGenders(@Enumerated(EnumType.STRING) EnumSet<Gender> genders);

}
