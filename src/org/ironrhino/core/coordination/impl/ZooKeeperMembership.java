package org.ironrhino.core.coordination.impl;

import static org.ironrhino.core.metadata.Profiles.CLUSTER;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.leader.LeaderLatch;
import org.apache.curator.framework.recipes.leader.LeaderLatchListener;
import org.apache.curator.framework.recipes.leader.Participant;
import org.ironrhino.core.coordination.LeaderChangeListener;
import org.ironrhino.core.coordination.Membership;
import org.ironrhino.core.spring.configuration.ServiceImplementationConditional;
import org.ironrhino.core.util.AppInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component("membership")
@ServiceImplementationConditional(profiles = CLUSTER)
public class ZooKeeperMembership implements Membership {

	public static final String DEFAULT_ZOOKEEPER_PATH = "/membership";

	private CuratorFramework curatorFramework;

	@Autowired(required = false)
	private List<LeaderChangeListener> leaderChangeListeners;

	@Value("${membership.zooKeeperPath:" + DEFAULT_ZOOKEEPER_PATH + "}")
	private String zooKeeperPath = DEFAULT_ZOOKEEPER_PATH;

	private ConcurrentHashMap<String, LeaderLatch> latchs = new ConcurrentHashMap<>();

	@Autowired
	public ZooKeeperMembership(CuratorFramework curatorFramework) {
		this.curatorFramework = curatorFramework;
	}

	public void setZooKeeperPath(String zooKeeperPath) {
		this.zooKeeperPath = zooKeeperPath;
	}

	@Override
	public void join(final String group) throws Exception {
		LeaderLatch latch = latchs.computeIfAbsent(group, (key) -> {
			LeaderLatch newLatch = new LeaderLatch(curatorFramework, zooKeeperPath + "/" + key,
					AppInfo.getInstanceId());
			if (leaderChangeListeners != null)
				newLatch.addListener(new LeaderLatchListener() {

					@Override
					public void notLeader() {
						for (LeaderChangeListener leaderChangeListener : leaderChangeListeners)
							if (leaderChangeListener.supports(key))
								leaderChangeListener.notLeader();
					}

					@Override
					public void isLeader() {
						for (LeaderChangeListener leaderChangeListener : leaderChangeListeners)
							if (leaderChangeListener.supports(key))
								leaderChangeListener.isLeader();
					}

				});
			return newLatch;
		});
		latch.start();
	}

	@Override
	public void leave(final String group) throws Exception {
		LeaderLatch latch = latchs.remove(group);
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
		List<String> list = new ArrayList<>(participants.size());
		for (Participant p : participants)
			list.add(p.getId());
		return list;
	}

}
