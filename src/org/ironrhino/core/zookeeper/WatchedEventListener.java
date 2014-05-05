package org.ironrhino.core.zookeeper;

import java.util.List;

/**
 * ZooKeeper节点上的WatchedEvent事件变化时的监听器，WatchedEvent包括了详细的事件发生，包括ZooKeeper的当前状态，包含事件的znode的路径.
 */
public interface WatchedEventListener {

	public boolean supports(String path);

	public void onNodeChildrenChanged(String path, List<String> children);

	public void onNodeCreated(String path, byte[] data);

	public void onNodeDeleted(String path);

	public void onNodeDataChanged(String path, byte[] data);

}
