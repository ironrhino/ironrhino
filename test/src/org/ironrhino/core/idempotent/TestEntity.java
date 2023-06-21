package org.ironrhino.core.idempotent;

import javax.persistence.Entity;

import org.hibernate.annotations.NaturalId;
import org.ironrhino.core.model.BaseEntity;

import lombok.Getter;
import lombok.Setter;

@Entity
@Setter
@Getter
class TestEntity extends BaseEntity {

	private static final long serialVersionUID = 1L;

	@NaturalId
	private String seqNo;

}
