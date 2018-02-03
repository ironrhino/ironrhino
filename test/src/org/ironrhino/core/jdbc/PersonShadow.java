package org.ironrhino.core.jdbc;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

import javax.persistence.Column;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;

import org.ironrhino.common.model.Gender;

import lombok.Data;

@Data
public class PersonShadow implements Serializable {

	private static final long serialVersionUID = 7400168548407982903L;

	private String name;

	@Enumerated(EnumType.STRING)
	private Gender gender;

	@Column(name = "f_dob")
	private LocalDate dob;

	private int age;

	private BigDecimal amount;

}
