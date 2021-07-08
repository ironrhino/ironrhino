package org.ironrhino.core.coordination;

import java.util.List;

public interface Membership {

	void join(String group);

	void leave(String group);

	boolean isLeader(String group);

	String getLeader(String group);

	List<String> getMembers(String group);

}