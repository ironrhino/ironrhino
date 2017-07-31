package org.ironrhino.core.remoting;

import java.io.Serializable;
import java.util.Date;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InvocationSample implements Serializable {

	private static final long serialVersionUID = -5779730637305188070L;

	private int count;

	private long totalTime;

	private String host;

	private Date start;

	private Date end;

	@JsonIgnore
	public long getMeanTime() {
		return count > 0 ? totalTime / count : 0;
	}

}
