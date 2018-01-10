package org.ironrhino.sample.crud;

import java.io.Serializable;

import javax.persistence.Embeddable;

import org.apache.commons.lang3.StringUtils;
import org.ironrhino.core.metadata.UiConfig;
import org.ironrhino.core.validation.constraints.CitizenIdentificationNumber;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Identity implements Serializable {

	private static final long serialVersionUID = 3472143402158894620L;

	@UiConfig(type = "dictionary", description = "identityType.description")
	private String identityType;

	@CitizenIdentificationNumber
	private String identityNo;

	public Identity(String str) {
		if (StringUtils.isNotBlank(str)) {
			String[] arr = str.split("-");
			identityType = arr[0];
			identityNo = arr[1];
		}
	}

	@Override
	public String toString() {
		return identityType + "-" + identityNo;
	}

}