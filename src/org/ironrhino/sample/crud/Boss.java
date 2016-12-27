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

@Searchable
@AutoConfig(fileupload = "text/plain")
@Table(name = "sample_boss")
@Entity
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

	@Override
	public boolean isNew() {
		return id == null;
	}

	@Override
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public Company getCompany() {
		return company;
	}

	public void setCompany(Company company) {
		this.company = company;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getIntro() {
		return intro;
	}

	public void setIntro(String intro) {
		this.intro = intro;
	}

	public File getResume() {
		return resume;
	}

	public void setResume(File resume) {
		this.resume = resume;
	}

	public String getResumeFileName() {
		return resumeFileName;
	}

	public void setResumeFileName(String resumeFileName) {
		this.resumeFileName = resumeFileName;
	}

	@PreUpdate
	@PrePersist
	private void processResume() {
		System.out.println("upload file name: " + resumeFileName + ", path: " + resume);
	}

}
