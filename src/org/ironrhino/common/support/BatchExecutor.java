package org.ironrhino.common.support;

import java.util.Collections;
import java.util.List;

import org.ironrhino.core.mail.MailService;
import org.ironrhino.core.metadata.Scope;
import org.ironrhino.core.spring.ApplicationContextConsole;
import org.ironrhino.core.util.ExceptionUtils;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;

public class BatchExecutor {

	@Autowired
	private Logger logger;

	@Autowired
	private ApplicationContextConsole applicationContextConsole;

	@Autowired(required = false)
	MailService mailService;

	private List<String> commands = Collections.emptyList();

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
			} catch (Throwable e) {
				logger.error(e.getMessage(), e);
				if (mailService != null) {
					try {
						SimpleMailMessage smm = new SimpleMailMessage();
						smm.setSubject(e.getMessage());
						smm.setText(ExceptionUtils.getStackTraceAsString(e));
						mailService.send(smm, false);
					} catch (Exception ee) {
						logger.warn("send email failed", ee);
					}
				}
			}
		}
	}

}
