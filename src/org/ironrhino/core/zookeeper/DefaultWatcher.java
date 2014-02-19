package org.ironrhino.core.zookeeper;

import java.util.Collection;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.api.CuratorEvent;
import org.apache.curator.framework.api.CuratorEventType;
import org.apache.curator.framework.api.CuratorListener;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher.Event.EventType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

public class DefaultWatcher implements CuratorListener {

	@Autowired
	private ApplicationContext applicationContext;

	@Autowired(required = false)
	private Collection<WatchedEventListener> eventListeners;

	@Override
	public void eventReceived(CuratorFramework curatorFramework,
			CuratorEvent event) throws Exception {
		if (event.getType() != CuratorEventType.WATCHED)
			return;
		String path = event.getPath();
		if (path != null) {
			if (eventListeners != null) {
				for (WatchedEventListener listener : eventListeners) {
					if (listener.supports(path)) {
						WatchedEvent we = event.getWatchedEvent();
						EventType wet = we.getType();
						switch (wet) {
						case NodeCreated:
							listener.onNodeCreated(path, curatorFramework
									.getData().watched().forPath(path));
							break;
						case NodeDeleted:
							listener.onNodeDeleted(path);
							break;
						case NodeDataChanged:
							listener.onNodeDataChanged(path, curatorFramework
									.getData().watched().forPath(path));
							break;
						case NodeChildrenChanged:
							listener.onNodeChildrenChanged(path,
									curatorFramework.getChildren().watched()
											.forPath(path));
							break;
						default:
							break;
						}
					}
				}
			}
		}
	}

}
