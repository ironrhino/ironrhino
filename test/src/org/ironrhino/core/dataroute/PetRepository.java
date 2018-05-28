package org.ironrhino.core.dataroute;

import java.util.List;

import org.ironrhino.core.jdbc.JdbcRepository;
import org.ironrhino.core.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

@JdbcRepository
@DataRoute(nodeName = "sharding3")
public interface PetRepository {

	@DataRoute(routingKey = "${pet.name}")
	@Transactional
	@Sql("insert into pet(name) values(:pet.name)")
	void save(Pet pet);

	@DataRoute(routingKey = "${name}")
	@Transactional(readOnly = true)
	@Sql("select * from pet where name=:name")
	Pet get(String name);

	@Transactional
	@Sql("insert into ownership(name,owner) values(:name,:owner)")
	void saveOwnership(String name, String owner);

	@Transactional
	@Sql("select * from ownership where owner=:owner")
	List<Ownership> findOwnership(String owner);

}
