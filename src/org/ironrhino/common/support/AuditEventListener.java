package org.ironrhino.common.support;

import org.ironrhino.common.model.AuditEvent;
import org.ironrhino.common.service.AuditEventManager;
import org.ironrhino.core.event.AbstractAuditEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

@Component
public class AuditEventListener implements ApplicationListener<AbstractAuditEvent> {

	@Autowired
	private AuditEventManager auditEventManager;

	@Override
	public void onApplicationEvent(AbstractAuditEvent event) {
		auditEventManager.save(new AuditEvent(event));
	}

}
