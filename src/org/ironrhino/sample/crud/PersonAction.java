package org.ironrhino.sample.crud;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;
import org.ironrhino.core.service.EntityManager;
import org.ironrhino.core.struts.EntityAction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

public class PersonAction extends EntityAction<Person> {

	private static final long serialVersionUID = 7605233440758696630L;

	@Autowired
	private EntityManager<Person> personManager;

	private File file;

	public void setFile(File file) {
		this.file = file;
	}

	@Transactional // only if needed
	public String upload() throws IOException {
		personManager.setEntityClass(Person.class);
		if (file != null) {
			IOUtils.readLines(new FileInputStream(file), StandardCharsets.UTF_8).stream().forEach(line -> {
				String[] arr = line.split(",");
				Person p = new Person();
				p.setId(new Identity(arr[0], arr[1]));
				p.setName(arr[2]);
				personManager.save(p);
			});
		}
		return SUCCESS;
	}

}
