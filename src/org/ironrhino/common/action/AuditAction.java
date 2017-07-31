package org.ironrhino.common.action;

import java.util.Date;
import java.util.List;

import org.ironrhino.common.model.AuditEvent;
import org.ironrhino.common.service.AuditEventManager;
import org.ironrhino.core.metadata.AutoConfig;
import org.ironrhino.core.struts.BaseAction;
import org.ironrhino.core.util.AuthzUtils;
import org.springframework.beans.factory.annotation.Autowired;

import lombok.Getter;
import lombok.Setter;

@AutoConfig(namespace = "/")
public class AuditAction extends BaseAction {

	private static final long serialVersionUID = 2526249000781826949L;

	private static final int PAGESIZE = 10;

	@Autowired
	private AuditEventManager auditEventManager;

	@Getter
	@Setter
	private Long since;

	@Getter
	private List<AuditEvent> events;

	public int getPageSize() {
		return PAGESIZE;
	}

	@Override
	public String execute() throws Exception {
		String username = AuthzUtils.getUsername();
		if (username == null)
			return ACCESSDENIED;
		events = auditEventManager.findRecentEvents(username, since != null ? new Date(since) : null, PAGESIZE);
		return SUCCESS;
	}

}
