package org.ironrhino.core.zookeeper;

import java.util.List;

public interface WatchedEventListener {

	boolean supports(String path);

	void onNodeChildrenChanged(String path, List<String> children);

	void onNodeCreated(String path, byte[] data);

	void onNodeDeleted(String path);

	void onNodeDataChanged(String path, byte[] data);

}
