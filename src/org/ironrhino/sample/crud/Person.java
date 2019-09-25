package org.ironrhino.sample.crud;

import java.time.LocalDate;
import java.time.LocalTime;

import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.validation.Valid;

import org.hibernate.annotations.NaturalId;
import org.ironrhino.common.model.Region;
import org.ironrhino.core.metadata.AutoConfig;
import org.ironrhino.core.metadata.CaseInsensitive;
import org.ironrhino.core.metadata.Hidden;
import org.ironrhino.core.metadata.Richtable;
import org.ironrhino.core.metadata.UiConfig;
import org.ironrhino.core.model.AbstractEntity;

import lombok.Getter;
import lombok.Setter;

@AutoConfig(fileupload = Person.FILE_MIME_TYPE)
@Entity
@Table(name = "sample_person")
@Richtable(order = "id.identityNo asc", paged = false, bottomButtons = "<@btn class='upload' data\\-accept='"
		+ Person.FILE_MIME_TYPE
		+ "' label='import'/> <@btn view='input' label='create'/> <@btn action='save'/> <@btn action='delete'/> <@btn class='reload'/> <@btn class='filter'/>")
@Getter
@Setter
public class Person extends AbstractEntity<Identity> {

	private static final long serialVersionUID = -8352037604261222984L;

	static final String FILE_MIME_TYPE = "text/csv";

	@Valid
	@EmbeddedId
	@UiConfig(alias = "identity")
	private Identity id;

	@CaseInsensitive
	@NaturalId(mutable = true)
	@Column(nullable = false)
	private String name;

	@UiConfig(width = "200px")
	@ManyToOne(fetch = FetchType.LAZY)
	private Region region;

	@UiConfig(width = "80px")
	private LocalDate birthDate;

	@UiConfig(width = "80px")
	private LocalTime birthTime;

	@Override
	public boolean isNew() {
		return id == null || id.getIdentityType() == null || id.getIdentityNo() == null;
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
