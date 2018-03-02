package org.ironrhino.sample.crud.history;

import javax.persistence.Entity;

import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.Subselect;
import org.ironrhino.core.metadata.AutoConfig;
import org.ironrhino.core.metadata.Readonly;
import org.ironrhino.core.metadata.Richtable;
import org.ironrhino.core.search.elasticsearch.annotations.Searchable;

@Searchable
@AutoConfig
@Richtable(alias = "trade", order = "workdate desc,seqno desc", readonly = @Readonly(true))
@Immutable
@Entity
@Subselect("select * from sample_current_trade union all select * from sample_historical_trade")
public class AllTrade extends BaseTrade {

	private static final long serialVersionUID = -8352037603261222984L;

}
