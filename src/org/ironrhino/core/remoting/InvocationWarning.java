package org.ironrhino.core.remoting;

import java.io.Serializable;
import java.util.Date;

import org.ironrhino.core.util.DateUtils;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class InvocationWarning implements Serializable {

	private static final long serialVersionUID = -7375531820015503869L;

	private String source;

	private String target;

	private String service;

	private long time;

	private boolean failed;

	private Date date = new Date();

	public InvocationWarning(String source, String target, String service, long time, boolean failed) {
		this.source = source;
		this.target = target;
		this.service = service;
		this.time = time;
		this.failed = failed;
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
