package org.ironrhino.core.coordination.impl;

import static org.ironrhino.core.metadata.Profiles.CLUSTER;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.leader.LeaderLatch;
import org.apache.curator.framework.recipes.leader.Participant;
import org.ironrhino.core.coordination.Membership;
import org.ironrhino.core.util.AppInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component("membership")
@Profile(CLUSTER)
public class ZooKeeperMembership implements Membership {

	public static final String DEFAULT_ZOOKEEPER_PATH = "/membership";

	@Autowired
	private CuratorFramework curatorFramework;

	private String zooKeeperPath = DEFAULT_ZOOKEEPER_PATH;

	private ConcurrentHashMap<String, LeaderLatch> latchs = new ConcurrentHashMap<String, LeaderLatch>();

	public void setZooKeeperPath(String zooKeeperPath) {
		this.zooKeeperPath = zooKeeperPath;
	}

	@Override
	public void join(final String group) throws Exception {
		LeaderLatch latch = latchs.get(group);
		if (latch == null) {
			latchs.putIfAbsent(group, new LeaderLatch(curatorFramework,
					zooKeeperPath + "/" + group, AppInfo.getInstanceId()));
			latch = latchs.get(group);
		}
		latch.start();
	}

	@Override
	public void leave(final String group) throws Exception {
		LeaderLatch latch = latchs.get(group);
		if (latch == null)
			throw new Exception("Please join group " + group + " first");
		latch.close();
	}

	@Override
	public boolean isLeader(String group) throws Exception {
		LeaderLatch latch = latchs.get(group);
		if (latch == null)
			throw new Exception("Please join group " + group + " first");
		return latch.hasLeadership();
	}

	@Override
	public String getLeader(String group) throws Exception {
		LeaderLatch latch = latchs.get(group);
		if (latch == null)
			throw new Exception("Please join group " + group + " first");
		return latch.getLeader().getId();
	}

	@Override
	public List<String> getMembers(String group) throws Exception {
		LeaderLatch latch = latchs.get(group);
		if (latch == null)
			throw new Exception("Please join group " + group + " first");
		Collection<Participant> participants = latch.getParticipants();
		List<String> list = new ArrayList<String>(participants.size());
		for (Participant p : participants) {
			list.add(p.getId());
		}
		return list;
	}

}
