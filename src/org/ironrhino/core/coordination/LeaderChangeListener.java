package org.ironrhino.core.coordination;

public interface LeaderChangeListener {

	public boolean supports(String group);

	public void notLeader();

	public void isLeader();

}
