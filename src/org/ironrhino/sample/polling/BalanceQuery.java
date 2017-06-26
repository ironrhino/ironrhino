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

@Entity
@Table(name = "sample_balance_query")
@AutoConfig
@Richtable(readonly = @Readonly(expression = "!entity.new"), order = "createDate desc", showActionColumn = false)
public class BalanceQuery extends BaseEntity {

	private static final long serialVersionUID = 3825969412916897020L;

	@UiConfig(width = "150px", regex="\\d+")
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

	public String getAccountNo() {
		return accountNo;
	}

	public void setAccountNo(String accountNo) {
		this.accountNo = accountNo;
	}

	public BigDecimal getBalance() {
		return balance;
	}

	public void setBalance(BigDecimal balance) {
		this.balance = balance;
	}

	public BalanceQueryStatus getStatus() {
		return status;
	}

	public void setStatus(BalanceQueryStatus status) {
		this.status = status;
	}

	public Date getCreateDate() {
		return createDate;
	}

	public void setCreateDate(Date createDate) {
		this.createDate = createDate;
	}

	public Date getModifyDate() {
		return modifyDate;
	}

	public void setModifyDate(Date modifyDate) {
		this.modifyDate = modifyDate;
	}

	public String getErrorInfo() {
		return errorInfo;
	}

	public void setErrorInfo(String errorInfo) {
		this.errorInfo = errorInfo;
	}

}
