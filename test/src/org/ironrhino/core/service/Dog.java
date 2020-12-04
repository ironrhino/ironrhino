package org.ironrhino.core.service;

import javax.persistence.EmbeddedId;
import javax.persistence.Entity;

import org.ironrhino.core.model.AbstractEntity;

import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class Dog extends AbstractEntity<Identity> {

	private static final long serialVersionUID = 273975322516248303L;

	@EmbeddedId
	private Identity id;

	private String name;

	@Override
	public boolean isNew() {
		return id == null || id.getIdentityType() == null || id.getIdentityNo() == null;
	}

}
