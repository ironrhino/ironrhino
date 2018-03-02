package org.ironrhino.sample.crud.history;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.NaturalId;
import org.ironrhino.core.metadata.Readonly;
import org.ironrhino.core.metadata.UiConfig;
import org.ironrhino.core.model.Persistable;

import lombok.Getter;
import lombok.Setter;

@MappedSuperclass
@Getter
@Setter
public class BaseTrade implements Persistable<String> {

	private static final long serialVersionUID = 4466852817197384135L;

	@Id
	@GeneratedValue(generator = "stringId")
	@GenericGenerator(name = "stringId", strategy = "stringId")
	@Column(length = 22)
	private String id;

	@UiConfig(width = "80px")
	@Column(nullable = false, updatable = false)
	private LocalDate workdate;

	@NaturalId
	@UiConfig(width = "150px")
	@Column(nullable = false, updatable = false)
	private String seqno;

	@UiConfig(width = "100px")
	@Column(nullable = false, updatable = false)
	private BigDecimal amount;

	@UiConfig(type = "textarea")
	private String memo;

	@UiConfig(width = "130px", readonly = @Readonly(true))
	@Column(nullable = false, updatable = false)
	private Date createDate = new Date();

	@Override
	public String toString() {
		return seqno;
	}

}
