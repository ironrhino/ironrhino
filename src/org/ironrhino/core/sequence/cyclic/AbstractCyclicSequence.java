package org.ironrhino.core.sequence.cyclic;

import java.util.Date;

import org.apache.commons.lang3.StringUtils;
import org.ironrhino.core.sequence.CyclicSequence;
import org.ironrhino.core.util.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.InitializingBean;

public abstract class AbstractCyclicSequence implements CyclicSequence, InitializingBean, BeanNameAware {

	protected Logger logger = LoggerFactory.getLogger(getClass());

	private CycleType cycleType = CycleType.DAY;

	private String sequenceName;

	private String beanName;

	private int paddingLength = 4;

	@Override
	public CycleType getCycleType() {
		return cycleType;
	}

	public void setCycleType(CycleType cycleType) {
		this.cycleType = cycleType;
	}

	public String getSequenceName() {
		return StringUtils.isNotBlank(sequenceName) ? sequenceName : beanName;
	}

	public void setSequenceName(String sequenceName) {
		this.sequenceName = sequenceName;
	}

	public int getPaddingLength() {
		return paddingLength;
	}

	public void setPaddingLength(int paddingLength) {
		this.paddingLength = paddingLength;
	}

	@Override
	public int nextIntValue() {
		String s = nextStringValue();
		return Integer.valueOf(s.substring(cycleType.getPattern().length()));
	}

	@Override
	public long nextLongValue() {
		return Long.valueOf(nextStringValue());
	}

	protected String getStringValue(Date date, int paddingLength, int nextId) {
		return getCycleType().format(date) + NumberUtils.format(nextId, paddingLength);
	}

	@Override
	public void setBeanName(String beanName) {
		if (StringUtils.isNotBlank(beanName)) {
			if (beanName.endsWith("CyclicSequence")) {
				beanName = beanName.substring(0, beanName.length() - "CyclicSequence".length());
			} else if (beanName.endsWith("Sequence")) {
				beanName = beanName.substring(0, beanName.length() - "Sequence".length());
			} else if (beanName.endsWith("Seq")) {
				beanName = beanName.substring(0, beanName.length() - "Seq".length());
			}
			this.beanName = beanName;
		}
	}
}
