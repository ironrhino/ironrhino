package org.ironrhino.sample.polling;

import java.math.BigDecimal;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;

import org.ironrhino.common.model.BasePollingEntity;
import org.ironrhino.core.metadata.Readonly;
import org.ironrhino.core.metadata.UiConfig;

import lombok.Getter;
import lombok.Setter;

@MappedSuperclass
@Getter
@Setter
public class BaseBalanceQuery extends BasePollingEntity {

	private static final long serialVersionUID = -3757140569377900609L;

	@UiConfig(width = "150px", regex = "\\d+")
	@Column(nullable = false, length = 20)
	private String accountNo;

	@UiConfig(width = "80px", readonly = @Readonly(true))
	private BigDecimal balance;

	public String toString() {
		return String.format("%s#%s", getId(), getAccountNo());
	}

}
