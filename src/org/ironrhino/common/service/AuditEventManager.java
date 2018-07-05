package org.ironrhino.common.service;

import java.util.Date;
import java.util.List;

import org.ironrhino.common.model.AuditEvent;
import org.ironrhino.core.service.BaseManager;

public interface AuditEventManager extends BaseManager<String, AuditEvent> {

	List<AuditEvent> findRecentEvents(String username, Date since, int size);

}
