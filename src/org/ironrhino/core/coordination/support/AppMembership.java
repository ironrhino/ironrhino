package org.ironrhino.core.coordination.support;

import java.util.List;

import javax.annotation.PostConstruct;

import org.ironrhino.core.coordination.Membership;
import org.ironrhino.core.util.AppInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class AppMembership {

	private String group = AppInfo.getAppName();

	private Membership membership;

	@Autowired
	public AppMembership(Membership membership) {
		this.membership = membership;
	}

	public boolean isLeader() throws Exception {
		return membership.isLeader(group);
	}

	public String getLeader(String group) throws Exception {
		return membership.getLeader(group);
	}

	public List<String> getMembers(String group) throws Exception {
		return membership.getMembers(group);
	}

	@PostConstruct
	public void init() throws Exception {
		membership.join(group);
	}

}
