package org.ironrhino.common.model;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.GenericGenerator;
import org.ironrhino.core.metadata.Readonly;
import org.ironrhino.core.metadata.UiConfig;
import org.ironrhino.core.model.Persistable;

import lombok.Getter;
import lombok.Setter;

@MappedSuperclass
@Getter
@Setter
public abstract class BasePollingEntity implements Persistable<Long> {

	private static final long serialVersionUID = 3825969412916897020L;

	@Id
	@GeneratedValue(generator = "snowflake")
	@GenericGenerator(name = "snowflake", strategy = "snowflake")
	private Long id;

	@UiConfig(width = "130px", displayOrder = 94)
	private Date scheduledFor;

	@UiConfig(width = "80px", displayOrder = 95)
	private int precedence;

	@UiConfig(width = "80px", readonly = @Readonly(true), displayOrder = 96)
	private PollingStatus status = PollingStatus.INITIALIZED;

	@UiConfig(width = "130px", displayOrder = 97)
	@Column(updatable = false)
	@CreationTimestamp
	private Date createDate;

	@UiConfig(width = "130px", displayOrder = 98)
	@Column(insertable = false)
	private Date modifyDate;

	@UiConfig(type = "textarea", displayOrder = 99)
	@Column(length = 4000)
	private String errorInfo;

	@UiConfig(width = "100px", displayOrder = 100)
	private int attempts;

}
