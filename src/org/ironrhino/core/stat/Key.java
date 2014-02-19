package org.ironrhino.core.stat;

import java.io.Serializable;

import org.apache.commons.lang3.StringUtils;

public class Key implements Serializable, Comparable<Key> {

	private static final long serialVersionUID = -4752654130307409656L;

	private String namespace = "";

	private String[] names;

	// minutes
	private int intervalMultiple = 1;

	private boolean cumulative = true;

	private long lastWriteTime = System.currentTimeMillis();

	public Key(String... strings) {
		namespace = null;
		names = strings;
	}

	public Key(int intervalMultiple, String... strings) {
		if (intervalMultiple > 0)
			this.intervalMultiple = intervalMultiple;
		namespace = null;
		names = strings;
	}

	public Key(String namespace, int intervalMultiple, String... strings) {
		if (intervalMultiple > 0)
			this.intervalMultiple = intervalMultiple;
		this.namespace = namespace;
		names = strings;
	}

	public Key(String namespace, int intervalMultiple, boolean cumulative,
			String... strings) {
		if (intervalMultiple > 0)
			this.intervalMultiple = intervalMultiple;
		this.namespace = namespace;
		this.cumulative = cumulative;
		names = strings;
	}

	public Key(String namespace, boolean cumulative, String... strings) {
		this.namespace = namespace;
		this.cumulative = cumulative;
		names = strings;
	}

	public int getIntervalMultiple() {
		return intervalMultiple;
	}

	public String[] getNames() {
		return names;
	}

	public String getNamespace() {
		return namespace;
	}

	public boolean isCumulative() {
		return cumulative;
	}

	public int getLevel() {
		return names.length;
	}

	public Key parent(int level) {
		if (level < 1)
			return parent(1);
		if (level > getLevel() - 1)
			return this;
		String[] newkeys = new String[level];
		System.arraycopy(names, 0, newkeys, 0, level);
		return new Key(namespace, intervalMultiple, cumulative, newkeys);
	}

	public Key child(String subkey) {
		String[] newkeys = new String[this.names.length + 1];
		System.arraycopy(names, 0, newkeys, 0, names.length);
		newkeys[this.names.length] = subkey;
		return new Key(namespace, intervalMultiple, cumulative, newkeys);
	}

	public boolean isAncestorOf(Key descendant) {
		return isAncestorOf(descendant, false);
	}

	public boolean isAncestorOf(Key descendant, boolean excludeSelf) {
		String d = descendant.toString();
		String a = this.toString();
		return d.startsWith(a) && !(excludeSelf && d.equals(a));
	}

	public long getLastWriteTime() {
		return lastWriteTime;
	}

	public void setLastWriteTime(long lastWriteTime) {
		this.lastWriteTime = lastWriteTime;
	}

	@Override
	public int compareTo(Key o) {
		if (o == null)
			return 1;
		return this.toString().compareTo(o.toString());
	}

	@Override
	public int hashCode() {
		return toString().hashCode();
	}

	@Override
	public boolean equals(Object o) {
		if (o == null)
			return false;
		return this.toString().equals(o.toString());
	}

	@Override
	public String toString() {
		return (StringUtils.isNotBlank(namespace) ? namespace + ":" : "")
				+ StringUtils.join(names, '>') + (cumulative ? "" : ",0");
	}

	public static Key fromString(String s) {
		if (StringUtils.isBlank(s))
			return null;
		String[] array1 = s.split(",");
		boolean cum = true;
		if (array1.length > 1)
			cum = !array1[1].equals("0");
		String[] array2 = array1[0].split(":");
		String namespace = "";
		String names = null;
		if (array2.length == 1) {
			names = array2[0];
		} else {
			namespace = array2[0];
			names = array2[1];
		}
		return new Key(namespace, 0, cum, names.split(">"));
	}

}
