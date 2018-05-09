package org.ironrhino.sample.remoting;

import java.util.List;

import org.ironrhino.core.cache.CheckCache;
import org.ironrhino.core.jdbc.JdbcRepository;
import org.ironrhino.core.jdbc.Sql;
import org.ironrhino.sample.crud.Person;
import org.springframework.transaction.annotation.Transactional;

@JdbcRepository
// @JdbcRepository("personRepository")
public interface PersonRepositoryImpl extends PersonRepository {

	@Override
	@CheckCache(key = "personRepository.findAll()")
	@Transactional(readOnly = true)
	@Sql("select identityNo as \"id.identityNo\",identityType as \"id.identityType\",name,region_id as \"region.id\" from sample_person")
	List<Person> findAll();

}