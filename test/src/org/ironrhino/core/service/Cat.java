package org.ironrhino.core.service;

import javax.persistence.Entity;
import javax.persistence.Id;

import org.ironrhino.core.model.AbstractEntity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Cat extends AbstractEntity<String> {

	private static final long serialVersionUID = 1L;

	@Id
	private String id;

	private String name;

}
