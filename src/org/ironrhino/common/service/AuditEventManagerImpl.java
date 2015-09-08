package org.ironrhino.common.service;

import java.util.Date;
import java.util.List;

import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.ironrhino.common.model.AuditEvent;
import org.ironrhino.core.service.BaseManagerImpl;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class AuditEventManagerImpl extends BaseManagerImpl<AuditEvent>implements AuditEventManager {

	@Override
	@Transactional(readOnly = true)
	public List<AuditEvent> findRecentEvents(String username, Date since, int size) {
		DetachedCriteria dc = detachedCriteria();
		dc.add(Restrictions.eq("username", username));
		if (since != null)
			dc.add(Restrictions.lt("date", since));
		dc.addOrder(Order.desc("date"));
		return findListByCriteria(dc, 1, size);
	}

}
