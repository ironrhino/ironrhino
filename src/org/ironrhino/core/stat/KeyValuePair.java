package org.ironrhino.core.stat;

import java.io.Serializable;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;
import javax.persistence.Transient;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@MappedSuperclass
@Data
@NoArgsConstructor
@AllArgsConstructor
public class KeyValuePair implements Serializable {

	private static final long serialVersionUID = 1939944128282158865L;

	public static final KeyValuePair EMPTY = new KeyValuePair();

	@Transient
	protected Key key;

	@Transient
	protected Value value;

	@Column(name = "date", nullable = false)
	protected Date date;

	@Column(nullable = false)
	protected String host;

}
