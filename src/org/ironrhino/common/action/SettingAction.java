package org.ironrhino.common.action;

import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Restrictions;
import org.ironrhino.common.model.Setting;
import org.ironrhino.core.hibernate.CriteriaState;
import org.ironrhino.core.struts.EntityAction;

public class SettingAction extends EntityAction<String, Setting> {

	private static final long serialVersionUID = -9014704633958010335L;

	@Override
	protected void prepare(DetachedCriteria dc, CriteriaState criteriaState) {
		super.prepare(dc, criteriaState);
		dc.add(Restrictions.eq("hidden", false));
	}

}
