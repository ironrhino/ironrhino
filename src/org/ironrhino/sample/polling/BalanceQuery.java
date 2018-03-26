package org.ironrhino.sample.polling;

import java.math.BigDecimal;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Lob;
import javax.persistence.Table;

import org.hibernate.annotations.CreationTimestamp;
import org.ironrhino.core.metadata.AutoConfig;
import org.ironrhino.core.metadata.Readonly;
import org.ironrhino.core.metadata.Richtable;
import org.ironrhino.core.metadata.UiConfig;
import org.ironrhino.core.model.BaseEntity;

import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "sample_balance_query")
@AutoConfig
@Richtable(readonly = @Readonly(expression = "!entity.new"), order = "createDate desc", showActionColumn = false)
@Getter
@Setter
public class BalanceQuery extends BaseEntity {

	private static final long serialVersionUID = 3825969412916897020L;

	@UiConfig(width = "150px", regex = "\\d+")
	@Column(nullable = false, length = 20)
	private String accountNo;

	@UiConfig(width = "80px", readonly = @Readonly(true))
	private BigDecimal balance;

	@UiConfig(width = "80px", readonly = @Readonly(true))
	private BalanceQueryStatus status = BalanceQueryStatus.INITIALIZED;

	@UiConfig(width = "130px")
	@Column(updatable = false)
	@CreationTimestamp
	private Date createDate;

	@UiConfig(width = "130px")
	@Column(insertable = false)
	private Date modifyDate;

	@UiConfig(type = "textarea")
	@Lob
	@Column(insertable = false)
	private String errorInfo;

	@UiConfig(width = "80px", alias = "尝试次数")
	private int attempts;

}
