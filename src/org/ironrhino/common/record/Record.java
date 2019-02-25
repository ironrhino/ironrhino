package org.ironrhino.common.record;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

import org.hibernate.criterion.MatchMode;
import org.ironrhino.core.metadata.AppendOnly;
import org.ironrhino.core.metadata.AutoConfig;
import org.ironrhino.core.metadata.Readonly;
import org.ironrhino.core.metadata.Richtable;
import org.ironrhino.core.metadata.UiConfig;
import org.ironrhino.core.model.BaseEntity;

import lombok.Getter;
import lombok.Setter;

@AutoConfig
@AppendOnly
@Entity
@Table(name = "common_record")
@Richtable(order = "recordDate desc", readonly = @Readonly(value = true), showQueryForm = true)
@Getter
@Setter
public class Record extends BaseEntity {

	private static final long serialVersionUID = -8287907984213799302L;

	@UiConfig(width = "300px", queryMatchMode = MatchMode.EXACT)
	private String entityClass;

	@UiConfig(width = "200px", queryMatchMode = MatchMode.EXACT)
	private String entityId;

	@UiConfig
	@Column(length = 10000)
	private String entityToString;

	@UiConfig(width = "80px", queryMatchMode = MatchMode.EXACT)
	private String action;

	@UiConfig(width = "100px", queryMatchMode = MatchMode.EXACT)
	private String operatorId;

	@UiConfig(width = "130px")
	private Date recordDate;

	@UiConfig(hidden = true)
	private String operatorClass;

}
