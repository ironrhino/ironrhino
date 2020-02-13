package org.ironrhino.common.record;

import javax.persistence.Entity;

import org.ironrhino.core.model.BaseEntity;

import lombok.Getter;
import lombok.Setter;

@RecordAware
@Entity
@Getter
@Setter
public class TestEntity extends BaseEntity {

	private static final long serialVersionUID = 1L;

	private String name;

}
