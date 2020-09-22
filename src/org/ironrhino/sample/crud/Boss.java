package org.ironrhino.sample.crud;

import java.io.File;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.MapsId;
import javax.persistence.OneToOne;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.hibernate.annotations.NaturalId;
import org.ironrhino.core.metadata.AutoConfig;
import org.ironrhino.core.metadata.Hidden;
import org.ironrhino.core.metadata.UiConfig;
import org.ironrhino.core.model.AbstractEntity;
import org.ironrhino.core.search.elasticsearch.annotations.Searchable;
import org.ironrhino.core.search.elasticsearch.annotations.SearchableProperty;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Searchable
@AutoConfig(fileupload = "text/plain")
@Table(name = "sample_boss")
@Entity
@Getter
@Setter
@Slf4j
public class Boss extends AbstractEntity<String> {

	private static final long serialVersionUID = 4908831348636951422L;

	@Id
	private String id;

	@UiConfig(width = "200px", description = "一对一关系并且共用主键")
	@OneToOne(fetch = FetchType.LAZY)
	@MapsId
	private Company company;

	@SearchableProperty
	@UiConfig(width = "200px")
	@NaturalId(mutable = true)
	private String name;

	@UiConfig(hiddenInList = @Hidden(true), description = "resume.description")
	@Transient
	private File resume;

	@Transient
	@UiConfig(hidden = true)
	private String resumeFileName;

	@Lob
	@UiConfig(type = "textarea")
	private String intro;

	@PreUpdate
	@PrePersist
	private void processResume() {
		log.info("upload file name: {}, path: {}",resumeFileName, resume);
	}

}
