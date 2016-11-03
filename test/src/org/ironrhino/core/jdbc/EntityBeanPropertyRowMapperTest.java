package org.ironrhino.core.jdbc;

import static org.junit.Assert.assertEquals;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;

import org.junit.Test;

public class EntityBeanPropertyRowMapperTest {

	static enum Grade {
		A, B, C;
	}

	static class Person {
		private String name;
		private Date dateOfBirth;
		private Grade grade1;

		@Column(name = "g2")
		@Enumerated(EnumType.STRING)
		private Grade grade2;

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public Date getDateOfBirth() {
			return dateOfBirth;
		}

		public void setDateOfBirth(Date dateOfBirth) {
			this.dateOfBirth = dateOfBirth;
		}

		public Grade getGrade1() {
			return grade1;
		}

		public void setGrade1(Grade grade1) {
			this.grade1 = grade1;
		}

		public Grade getGrade2() {
			return grade2;
		}

		public void setGrade2(Grade grade2) {
			this.grade2 = grade2;
		}

	}

	@Test
	public void test() {
		EntityBeanPropertyRowMapper<Person> mapper = new EntityBeanPropertyRowMapper<>(Person.class);
		assertEquals("date_of_birth", mapper.underscoreName("dateOfBirth"));
		assertEquals("g2", mapper.underscoreName("grade2"));
	}

}
