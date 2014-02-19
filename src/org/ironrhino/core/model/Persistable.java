package org.ironrhino.core.model;

import java.io.Serializable;

public interface Persistable<PK extends Serializable> extends Serializable {

	public boolean isNew();

	public PK getId();

}
