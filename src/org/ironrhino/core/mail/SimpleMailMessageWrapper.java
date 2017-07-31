package org.ironrhino.core.mail;

import java.io.Serializable;

import org.springframework.mail.SimpleMailMessage;

import lombok.Data;

@Data
public class SimpleMailMessageWrapper implements Serializable {

	private static final long serialVersionUID = 1654838401432724325L;

	private final SimpleMailMessage simpleMailMessage;

	private final boolean useHtmlFormat;

}
