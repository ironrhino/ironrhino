package org.ironrhino.core.sequence.simple;

import org.apache.commons.lang3.StringUtils;
import org.ironrhino.core.sequence.SimpleSequence;
import org.ironrhino.core.util.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.InitializingBean;

public abstract class AbstractSimpleSequence implements SimpleSequence, InitializingBean, BeanNameAware {

	protected Logger logger = LoggerFactory.getLogger(getClass());

	private String sequenceName;

	private String beanName;

	private int paddingLength = 4;

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
	public String nextStringValue() {
		return NumberUtils.format(nextIntValue(), paddingLength);
	}

	@Override
	public void setBeanName(String beanName) {
		if (StringUtils.isNotBlank(beanName)) {
			if (beanName.endsWith("SimpleSequence")) {
				beanName = beanName.substring(0, beanName.length() - "SimpleSequence".length());
			} else if (beanName.endsWith("Sequence")) {
				beanName = beanName.substring(0, beanName.length() - "Sequence".length());
			} else if (beanName.endsWith("Seq")) {
				beanName = beanName.substring(0, beanName.length() - "Seq".length());
			}
			this.beanName = beanName;
		}
	}
}
