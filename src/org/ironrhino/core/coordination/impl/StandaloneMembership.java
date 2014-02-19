package org.ironrhino.core.coordination.impl;

import static org.ironrhino.core.metadata.Profiles.CLOUD;
import static org.ironrhino.core.metadata.Profiles.DEFAULT;
import static org.ironrhino.core.metadata.Profiles.DUAL;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.ironrhino.core.coordination.Membership;
import org.ironrhino.core.util.AppInfo;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component("membership")
@Profile({ DEFAULT, DUAL, CLOUD })
public class StandaloneMembership implements Membership {

	private Map<String, List<String>> groups = new HashMap<String, List<String>>();

	@Override
	public void join(String group) {
		List<String> members = groups.get(group);
		if (members == null) {
			members = new ArrayList<String>();
			groups.put(group, members);
		}
		String instanceId = AppInfo.getInstanceId();
		if (!members.contains(instanceId))
			members.add(instanceId);
	}

	@Override
	public void leave(String group) {
		List<String> members = groups.get(group);
		if (members != null) {
			String instanceId = AppInfo.getInstanceId();
			members.remove(instanceId);
		}
	}

	@Override
	public boolean isLeader(String group) {
		return AppInfo.getInstanceId().equals(getLeader(group));
	}

	@Override
	public String getLeader(String group) {
		List<String> members = getMembers(group);
		if (members == null || members.isEmpty())
			return null;
		else
			return members.get(0);
	}

	@Override
	public List<String> getMembers(String group) {
		return groups.get(group);
	}

}
