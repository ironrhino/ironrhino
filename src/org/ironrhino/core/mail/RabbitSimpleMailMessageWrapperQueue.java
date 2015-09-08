package org.ironrhino.core.mail;

import org.ironrhino.core.rabbitmq.RabbitQueue;
import org.springframework.beans.factory.annotation.Autowired;

public class RabbitSimpleMailMessageWrapperQueue extends RabbitQueue<SimpleMailMessageWrapper>
		implements SimpleMailMessageWrapperQueue {

	@Autowired
	private MailSender mailSender;

	@Override
	public void consume(SimpleMailMessageWrapper smmw) {
		mailSender.send(smmw.getSimpleMailMessage(), smmw.isUseHtmlFormat());
	}

}
