package org.ironrhino.core.remoting;

import java.io.Serializable;
import java.util.Date;

import org.ironrhino.core.util.DateUtils;

public class InvocationWarning implements Serializable {

	private static final long serialVersionUID = -7375531820015503869L;

	private String source;

	private String target;

	private String service;

	private long time;

	private boolean failed;

	private Date date = new Date();

	public InvocationWarning() {

	}

	public InvocationWarning(String source, String target, String service, long time, boolean failed) {
		this.source = source;
		this.target = target;
		this.service = service;
		this.time = time;
		this.failed = failed;
	}

	public String getSource() {
		return source;
	}

	public void setSource(String source) {
		this.source = source;
	}

	public String getTarget() {
		return target;
	}

	public void setTarget(String target) {
		this.target = target;
	}

	public String getService() {
		return service;
	}

	public void setService(String service) {
		this.service = service;
	}

	public long getTime() {
		return time;
	}

	public void setTime(long time) {
		this.time = time;
	}

	public boolean isFailed() {
		return failed;
	}

	public void setFailed(boolean failed) {
		this.failed = failed;
	}

	public Date getDate() {
		return date;
	}

	public void setDate(Date date) {
		this.date = date;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("Invoke ").append(service).append(" from ").append(source).append(" to ").append(target)
				.append(" tooks ").append(time).append(" ms").append(" at ").append(DateUtils.formatDatetime(date));
		if (failed)
			sb.append(", and failed");
		return sb.toString();
	}

}
