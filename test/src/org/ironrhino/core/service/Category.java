package org.ironrhino.core.service;

import javax.persistence.Entity;

import org.hibernate.annotations.NaturalId;
import org.ironrhino.core.model.BaseEntity;

import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class Category extends BaseEntity {

	private static final long serialVersionUID = 1L;

	@NaturalId
	private String name;

}
