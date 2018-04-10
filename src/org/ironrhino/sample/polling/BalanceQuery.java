package org.ironrhino.sample.polling;

import javax.persistence.Entity;
import javax.persistence.Index;
import javax.persistence.Table;

@Entity
@Table(name = "sample_balance_query", indexes = { @Index(columnList = "createDate desc") })
public class BalanceQuery extends BaseBalanceQuery {

	private static final long serialVersionUID = 3825969412916897020L;

}
