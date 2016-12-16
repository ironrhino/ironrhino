package org.ironrhino.core.jdbc;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import java.util.Map;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;

import org.ironrhino.common.model.Gender;
import org.ironrhino.core.util.DateUtils;

public class Person implements Serializable {

	private static final long serialVersionUID = 7400168548407982903L;

	private String name;

	@Enumerated(EnumType.STRING)
	private Gender gender;

	@Column(name = "f_dob")
	private Date dob;

	private int age;

	private BigDecimal amount;

	private Map<String, String> attributes;

	private Set<String> roles;

	private PersonShadow shadow;

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

	public Date getDob() {
		return dob;
	}

	public void setDob(Date dob) {
		this.dob = dob;
	}

	public int getAge() {
		return age;
	}

	public void setAge(int age) {
		this.age = age;
	}

	public BigDecimal getAmount() {
		return amount;
	}

	public void setAmount(BigDecimal amount) {
		this.amount = amount;
	}

	public Map<String, String> getAttributes() {
		return attributes;
	}

	public void setAttributes(Map<String, String> attributes) {
		this.attributes = attributes;
	}

	public Set<String> getRoles() {
		return roles;
	}

	public void setRoles(Set<String> roles) {
		this.roles = roles;
	}

	public PersonShadow getShadow() {
		return shadow;
	}

	public void setShadow(PersonShadow shadow) {
		this.shadow = shadow;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + age;
		result = prime * result + ((amount == null) ? 0 : amount.hashCode());
		result = prime * result + ((dob == null) ? 0 : dob.hashCode());
		result = prime * result + ((gender == null) ? 0 : gender.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Person other = (Person) obj;
		if (age != other.age)
			return false;
		if (amount == null) {
			if (other.amount != null)
				return false;
		} else if (amount.doubleValue() != amount.doubleValue())
			return false;
		if (dob == null) {
			if (other.dob != null)
				return false;
		} else if (!DateUtils.formatDate10(dob).equals(DateUtils.formatDate10(other.dob))) {
			return false;
		}
		if (gender != other.gender)
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (attributes == null) {
			if (other.attributes != null)
				return false;
		} else if (!attributes.equals(other.attributes))
			return false;
		if (roles == null) {
			if (other.roles != null)
				return false;
		} else if (!roles.equals(other.roles))
			return false;
		return true;
	}

}
