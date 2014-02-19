package org.ironrhino.core.mail;

import java.io.UnsupportedEncodingException;

import org.springframework.beans.factory.annotation.Autowired;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeUtility;

import org.apache.commons.lang3.StringUtils;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.mail.javamail.MimeMessagePreparator;

public class MailSender {

	private String defaultFrom = "zhouyanming@gmail.com";

	private String defaultTo = "zhouyanming@gmail.com";

	@Autowired
	private JavaMailSenderImpl javaMailSender;

	public String getDefaultFrom() {
		return defaultFrom;
	}

	public void setDefaultFrom(String defaultFrom) {
		this.defaultFrom = defaultFrom;
	}

	public String getDefaultTo() {
		return defaultTo;
	}

	public void setDefaultTo(String defaultTo) {
		this.defaultTo = defaultTo;
	}

	public void send(final SimpleMailMessage smm) {
		send(smm, true);
	}

	public void send(final SimpleMailMessage smm, final boolean useHtmlFormat) {
		if (smm == null)
			return;
		if (smm.getTo() == null || smm.getTo().length == 0)
			smm.setTo(defaultTo);
		for (final String to : smm.getTo()) {
			javaMailSender.send(new MimeMessagePreparator() {
				@Override
				public void prepare(MimeMessage mimeMessage)
						throws MessagingException {
					MimeMessageHelper message = new MimeMessageHelper(
							mimeMessage, true, "UTF-8");
					if (StringUtils.isNotBlank(smm.getFrom()))
						message.setFrom(encode(smm.getFrom()));
					else
						message.setFrom(encode(defaultFrom));
					if (StringUtils.isNotBlank(smm.getReplyTo()))
						message.setReplyTo(encode(smm.getReplyTo()));
					message.setTo(encode(to));
					message.setSubject(smm.getSubject());
					message.setText(smm.getText(), useHtmlFormat);
				}
			});
		}
	}

	public static String encode(String to) {
		if (StringUtils.isBlank(to))
			return to;
		String[] array = to.split("\\s*,\\s*");
		for (int i = 0; i < array.length; i++) {
			if (array[i].indexOf('<') < 0)
				continue;
			String name = array[i].substring(0, array[i].indexOf('<'));
			name = name.trim();
			name = StringUtils.remove(name, "\"");
			name = StringUtils.remove(name, "'");
			String email = array[i].substring(array[i].indexOf('<'));
			try {
				array[i] = MimeUtility.encodeWord(name, "UTF-8", "Q") + email;
			} catch (UnsupportedEncodingException e) {
				email = email.substring(1, email.length());
				array[i] = email;
			}
		}
		return StringUtils.join(array, ",");
	}

	public void send(MimeMessage mimeMessage) {
		javaMailSender.send(mimeMessage);
	}

	public void send(MimeMessagePreparator mimeMessagePreparator) {
		javaMailSender.send(mimeMessagePreparator);
	}

}
