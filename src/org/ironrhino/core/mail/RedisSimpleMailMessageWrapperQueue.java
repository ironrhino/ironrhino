package org.ironrhino.core.mail;

import org.ironrhino.core.redis.RedisQueue;
import org.springframework.beans.factory.annotation.Autowired;

public class RedisSimpleMailMessageWrapperQueue extends RedisQueue<SimpleMailMessageWrapper>
		implements SimpleMailMessageWrapperQueue {

	@Autowired
	private MailSender mailSender;

	@Override
	public void consume(SimpleMailMessageWrapper smmw) {
		mailSender.send(smmw.getSimpleMailMessage(), smmw.isUseHtmlFormat());
	}

}
