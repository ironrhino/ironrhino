package org.ironrhino.core.stat;

import java.io.Serializable;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;
import javax.persistence.Transient;

@MappedSuperclass
public class KeyValuePair implements Serializable {

	private static final long serialVersionUID = 1939944128282158865L;

	@Transient
	protected Key key;

	@Transient
	protected Value value;

	@Column(name = "`date`", nullable = false)
	protected Date date;

	@Column(nullable = false)
	protected String host;

	public KeyValuePair() {

	}

	public KeyValuePair(Key key, Value value, Date date, String host) {
		super();
		this.key = key;
		this.value = value;
		this.date = date;
		this.host = host;
	}

	public Key getKey() {
		return key;
	}

	public void setKey(Key key) {
		this.key = key;
	}

	public Value getValue() {
		return value;
	}

	public void setValue(Value value) {
		this.value = value;
	}

	public Date getDate() {
		return date;
	}

	public void setDate(Date date) {
		this.date = date;
	}

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

}
