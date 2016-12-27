package org.ironrhino.sample.crud;

import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.Table;

import org.hibernate.annotations.NaturalId;
import org.ironrhino.core.metadata.AutoConfig;
import org.ironrhino.core.metadata.CaseInsensitive;
import org.ironrhino.core.metadata.Hidden;
import org.ironrhino.core.metadata.UiConfig;
import org.ironrhino.core.model.AbstractEntity;

@AutoConfig
@Entity
@Table(name = "sample_person")
public class Person extends AbstractEntity<Identity> {

	private static final long serialVersionUID = -8352037604261222984L;

	@EmbeddedId
	@UiConfig(alias = "identity")
	private Identity id;

	@CaseInsensitive
	@NaturalId(mutable = true)
	@Column(nullable = false)
	private String name;

	public Identity getId() {
		return id;
	}

	public void setId(Identity id) {
		this.id = id;
	}

	@Override
	public boolean isNew() {
		return id == null || id.getIdentityType() == null || id.getIdentityNo() == null;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@UiConfig(type = "dictionary", width = "200px", displayOrder = -2, hiddenInView = @Hidden(true))
	public String getIdentityType() {
		if (id == null)
			return null;
		return id.getIdentityType();
	}

	@UiConfig(width = "200px", displayOrder = -1, hiddenInView = @Hidden(true))
	public String getIdentityNo() {
		if (id == null)
			return null;
		return id.getIdentityNo();
	}

}
