package org.ironrhino.sample.polling;

import javax.persistence.Entity;
import javax.persistence.Index;
import javax.persistence.Table;

import org.hibernate.annotations.Immutable;

@Immutable
@Entity
@Table(name = "sample_balance_query_his", indexes = { @Index(columnList = "createDate desc") })
public class HistoricalBalanceQuery extends BaseBalanceQuery {

	private static final long serialVersionUID = 5143261413150829199L;

}
