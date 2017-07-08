package org.ironrhino.sample.remoting;

import java.util.List;

import org.ironrhino.core.remoting.Remoting;
import org.ironrhino.sample.crud.Person;

@Remoting
public interface PersonRepository {

	List<Person> findAll();

}