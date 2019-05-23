package org.ironrhino.security.model;

import javax.persistence.Entity;
import javax.persistence.Table;

import org.ironrhino.common.record.RecordAware;

@RecordAware
@Entity
@Table(name = "user")
public class User extends BaseUser {

	private static final long serialVersionUID = 7307419528067871480L;

}
