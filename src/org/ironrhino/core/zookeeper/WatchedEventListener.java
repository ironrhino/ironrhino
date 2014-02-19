package org.ironrhino.core.zookeeper;

import java.util.List;

public interface WatchedEventListener {

	public boolean supports(String path);

	public void onNodeChildrenChanged(String path, List<String> children);

	public void onNodeCreated(String path, byte[] data);

	public void onNodeDeleted(String path);

	public void onNodeDataChanged(String path, byte[] data);

}
