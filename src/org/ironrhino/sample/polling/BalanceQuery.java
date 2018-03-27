package org.ironrhino.sample.polling;

import java.math.BigDecimal;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

import org.ironrhino.common.model.BasePollingEntity;
import org.ironrhino.core.metadata.AutoConfig;
import org.ironrhino.core.metadata.Readonly;
import org.ironrhino.core.metadata.Richtable;
import org.ironrhino.core.metadata.UiConfig;

import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "sample_balance_query")
@AutoConfig
@Richtable(readonly = @Readonly(expression = "!entity.new"), order = "createDate desc", showActionColumn = false)
@Getter
@Setter
public class BalanceQuery extends BasePollingEntity {

	private static final long serialVersionUID = 3825969412916897020L;

	@UiConfig(width = "150px", regex = "\\d+")
	@Column(nullable = false, length = 20)
	private String accountNo;

	@UiConfig(width = "80px", readonly = @Readonly(true))
	private BigDecimal balance;

	public String toString() {
		return String.format("%s#%s", getId(), getAccountNo());
	}

}
