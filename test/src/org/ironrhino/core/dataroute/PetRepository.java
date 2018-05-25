package org.ironrhino.core.dataroute;

import org.ironrhino.core.jdbc.JdbcRepository;
import org.ironrhino.core.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

@JdbcRepository
public interface PetRepository {

	@Transactional
	@Sql("insert into pet(name) values(:pet.name)")
	void save(Pet pet);

	@Transactional(readOnly = true)
	@Sql("select * from pet where name=:name")
	Pet get(String name);

}
