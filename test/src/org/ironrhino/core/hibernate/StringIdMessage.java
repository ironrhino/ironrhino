package org.ironrhino.core.hibernate;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.hibernate.annotations.GenericGenerator;
import org.ironrhino.core.model.Persistable;

import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class StringIdMessage implements Persistable<String> {

	private static final long serialVersionUID = 0L;

	@Id
	@GeneratedValue(generator = "messageId")
	@GenericGenerator(name = "messageId", strategy = "snowflake")
	private String id;

	private String title;

}