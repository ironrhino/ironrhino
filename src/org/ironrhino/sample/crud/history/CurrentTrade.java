package org.ironrhino.sample.crud.history;

import javax.persistence.Entity;
import javax.persistence.Table;

import org.ironrhino.core.metadata.AutoConfig;
import org.ironrhino.core.metadata.Richtable;
import org.ironrhino.core.search.elasticsearch.annotations.Searchable;

@Searchable
@AutoConfig
@Richtable(alias = "trade", order = "workdate desc,seqno desc")
@Entity
@Table(name = "sample_current_trade")
public class CurrentTrade extends BaseTrade {

	private static final long serialVersionUID = 4466852817197384135L;

}
