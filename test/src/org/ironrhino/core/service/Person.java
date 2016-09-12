package org.ironrhino.core.service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.PostLoad;
import javax.persistence.PostPersist;
import javax.persistence.PostRemove;
import javax.persistence.PostUpdate;
import javax.persistence.PrePersist;
import javax.persistence.PreRemove;
import javax.persistence.PreUpdate;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.NaturalId;
import org.ironrhino.common.model.Gender;
import org.ironrhino.core.model.BaseEntity;

@Entity
public class Person extends BaseEntity {

	private static final long serialVersionUID = 1L;

	@NaturalId
	private String name;

	private Gender gender;

	@Temporal(TemporalType.DATE)
	private Date dateOfBirth;

	@Column(nullable = false)
	@CreationTimestamp
	private Date createDate;

	transient List<String> lifyCycleEvents = new ArrayList<>();

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Gender getGender() {
		return gender;
	}

	public void setGender(Gender gender) {
		this.gender = gender;
	}

	public Date getDateOfBirth() {
		return dateOfBirth;
	}

	public void setDateOfBirth(Date dateOfBirth) {
		this.dateOfBirth = dateOfBirth;
	}

	public Date getCreateDate() {
		return createDate;
	}

	public void setCreateDate(Date createDate) {
		this.createDate = createDate;
	}

	@PostLoad
	public void postLoad() {
		lifyCycleEvents.add("PostLoad");
	}

	@PrePersist
	public void prePersist() {
		lifyCycleEvents.add(PrePersist.class.getSimpleName());
	}

	@PreUpdate
	public void preUpdate() {
		lifyCycleEvents.add(PreUpdate.class.getSimpleName());
	}

	@PreRemove
	public void preRemove() {
		lifyCycleEvents.add(PreRemove.class.getSimpleName());
	}

	@PostPersist
	public void postPersist() {
		lifyCycleEvents.add(PostPersist.class.getSimpleName());
	}

	@PostUpdate
	public void postUpdate() {
		lifyCycleEvents.add(PostUpdate.class.getSimpleName());
	}

	@PostRemove
	public void postRemove() {
		lifyCycleEvents.add(PostRemove.class.getSimpleName());
	}

}
