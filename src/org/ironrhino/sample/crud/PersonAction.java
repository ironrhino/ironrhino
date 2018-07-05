package org.ironrhino.sample.crud;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import org.ironrhino.core.service.EntityManager;
import org.ironrhino.core.struts.EntityAction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import lombok.Setter;

public class PersonAction extends EntityAction<Identity, Person> {

	private static final long serialVersionUID = 7605233440758696630L;

	@Autowired
	private EntityManager<Identity, Person> personManager;

	@Setter
	private File file;

	@Transactional // only if needed
	public String upload() throws IOException {
		personManager.setEntityClass(Person.class);
		if (file != null) {
			try (BufferedReader br = new BufferedReader(
					new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
				br.lines().forEach(line -> {
					String[] arr = line.split(",");
					Person p = new Person();
					p.setId(new Identity(arr[0], arr[1]));
					p.setName(arr[2]);
					personManager.save(p);
				});
			}
		}
		return SUCCESS;
	}

}
