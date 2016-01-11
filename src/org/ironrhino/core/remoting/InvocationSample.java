package org.ironrhino.core.remoting;

import java.io.Serializable;
import java.util.Date;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class InvocationSample implements Serializable {

	private static final long serialVersionUID = -5779730637305188070L;

	private int count;

	private long totalTime;

	private String host;

	private Date start;

	private Date end;

	public InvocationSample() {

	}

	public InvocationSample(int count, long totalTime, String host, Date start, Date end) {
		this.count = count;
		this.totalTime = totalTime;
		this.host = host;
		this.start = start;
		this.end = end;
	}

	public int getCount() {
		return count;
	}

	public void setCount(int count) {
		this.count = count;
	}

	public long getTotalTime() {
		return totalTime;
	}

	public void setTotalTime(long totalTime) {
		this.totalTime = totalTime;
	}

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public Date getStart() {
		return start;
	}

	public void setStart(Date start) {
		this.start = start;
	}

	public Date getEnd() {
		return end;
	}

	public void setEnd(Date end) {
		this.end = end;
	}

	@JsonIgnore
	public long getMeanTime() {
		return count > 0 ? totalTime / count : 0;
	}

}
