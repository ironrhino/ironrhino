package org.ironrhino.sample.polling;

import javax.persistence.Entity;

import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.Subselect;
import org.ironrhino.core.metadata.AutoConfig;
import org.ironrhino.core.metadata.Readonly;
import org.ironrhino.core.metadata.Richtable;

@Immutable
@Entity
@Subselect("select * from sample_balance_query union all select * from sample_balance_query_his")
@AutoConfig
@Richtable(readonly = @Readonly(true), order = "createDate desc")
public class AllBalanceQuery extends BaseBalanceQuery {

	private static final long serialVersionUID = 5143261413150829199L;

}
