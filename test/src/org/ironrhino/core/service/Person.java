package org.ironrhino.core.service;

import java.util.Date;

import javax.persistence.Entity;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

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

}
