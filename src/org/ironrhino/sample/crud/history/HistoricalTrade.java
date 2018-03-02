package org.ironrhino.sample.crud.history;

import javax.persistence.Entity;
import javax.persistence.Table;

import org.hibernate.annotations.Immutable;
import org.ironrhino.core.metadata.AutoConfig;
import org.ironrhino.core.metadata.Richtable;

@Immutable
@Entity
@AutoConfig
@Richtable(alias = "trade", order = "workdate desc,seqno desc")
@Table(name = "sample_historical_trade")
public class HistoricalTrade extends BaseTrade {

	private static final long serialVersionUID = -8352037603261222984L;

}
