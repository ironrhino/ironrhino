package org.ironrhino.sample.crud;

import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.Table;

import org.hibernate.annotations.NaturalId;
import org.ironrhino.core.metadata.AutoConfig;
import org.ironrhino.core.metadata.CaseInsensitive;
import org.ironrhino.core.metadata.Hidden;
import org.ironrhino.core.metadata.Richtable;
import org.ironrhino.core.metadata.UiConfig;
import org.ironrhino.core.model.AbstractEntity;

@AutoConfig(fileupload = Person.FILE_MIME_TYPE)
@Entity
@Table(name = "sample_person")
@Richtable(order = "id.identityNo asc", bottomButtons = "<@btn class='upload' data\\-accept='" + Person.FILE_MIME_TYPE
		+ "' label='import'/> <@btn view='input' label='create'/> <@btn action='save'/> <@btn action='delete'/> <@btn class='reload'/> <@btn class='filter'/>")
public class Person extends AbstractEntity<Identity> {

	private static final long serialVersionUID = -8352037604261222984L;

	static final String FILE_MIME_TYPE = "text/csv";

	@EmbeddedId
	@UiConfig(alias = "identity")
	private Identity id;

	@CaseInsensitive
	@NaturalId(mutable = true)
	@Column(nullable = false)
	private String name;

	@Override
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
