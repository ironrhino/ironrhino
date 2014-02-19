package org.ironrhino.common.support;

import java.util.Collections;
import java.util.List;

import org.ironrhino.core.mail.MailService;
import org.ironrhino.core.metadata.Scope;
import org.ironrhino.core.spring.ApplicationContextConsole;
import org.ironrhino.core.util.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;

public class BatchExecutor {

	protected final Logger log = LoggerFactory.getLogger(getClass());

	private List<String> commands = Collections.emptyList();

	@Autowired
	private ApplicationContextConsole applicationContextConsole;

	@Autowired(required = false)
	MailService mailService;

	public List<String> getCommands() {
		return commands;
	}

	public void setCommands(List<String> commands) {
		this.commands = commands;
	}

	public void execute() {
		for (String cmd : commands) {
			try {
				applicationContextConsole.execute(cmd, Scope.LOCAL);
			} catch (Exception e) {
				log.error(e.getMessage(), e);
				if (mailService != null) {
					try {
						SimpleMailMessage smm = new SimpleMailMessage();
						smm.setSubject(e.getMessage());
						smm.setText(ExceptionUtils.getStackTraceAsString(e));
						mailService.send(smm, false);
					} catch (Exception ee) {
						log.warn("send email failed", ee);
					}
				}
			}
		}
	}

}
