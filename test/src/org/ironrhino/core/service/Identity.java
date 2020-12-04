package org.ironrhino.core.service;

import java.io.Serializable;

import javax.persistence.Embeddable;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Identity implements Serializable {

	private static final long serialVersionUID = 3472143402158894620L;

	private String identityType;

	private String identityNo;

}