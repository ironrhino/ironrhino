package org.ironrhino.core.util;

import java.io.Serializable;

public class SemanticVersion implements Serializable, Comparable<SemanticVersion> {

	private static final long serialVersionUID = 7953824383945625786L;

	private int major;
	private int minor;
	private Integer patch;
	private String prerelease;
	private String build;

	public SemanticVersion() {

	}

	public SemanticVersion(String semver) {
		int i = semver.indexOf('+');
		if (i > 0) {
			build = semver.substring(i + 1);
			semver = semver.substring(0, i);
		}
		i = semver.indexOf('-');
		if (i > 0) {
			prerelease = semver.substring(i + 1);
			semver = semver.substring(0, i);
		}
		String[] arr = semver.split("\\.");
		major = Integer.parseInt(arr[0]);
		if (arr.length > 1)
			minor = Integer.parseInt(arr[1]);
		if (arr.length > 2)
			patch = Integer.parseInt(arr[2]);
	}

	public int getMajor() {
		return major;
	}

	public void setMajor(int major) {
		this.major = major;
	}

	public int getMinor() {
		return minor;
	}

	public void setMinor(int minor) {
		this.minor = minor;
	}

	public Integer getPatch() {
		return patch;
	}

	public void setPatch(Integer patch) {
		this.patch = patch;
	}

	public String getPrerelease() {
		return prerelease;
	}

	public void setPrerelease(String prerelease) {
		this.prerelease = prerelease;
	}

	public String getBuild() {
		return build;
	}

	public void setBuild(String build) {
		this.build = build;
	}

	@Override
	public int compareTo(SemanticVersion that) {
		if (that == null)
			return 1;
		if (this.major != that.major)
			return this.major - that.major;
		if (this.minor != that.minor)
			return this.minor - that.minor;
		int thisPatch = this.patch != null ? this.patch : -1;
		int thatPatch = that.patch != null ? that.patch : -1;
		if (thisPatch != thatPatch)
			return thisPatch - thatPatch;
		if (this.prerelease != null && that.prerelease != null)
			return this.prerelease.compareTo(that.prerelease);
		else if (this.prerelease != null && that.prerelease == null)
			return 1;
		else if (this.prerelease == null && that.prerelease != null)
			return -1;
		else
			return 0;
	}

	@Override
	public int hashCode() {
		return toString().hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		return (obj instanceof SemanticVersion && obj.toString().equals(this.toString()));
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(major).append(".").append(minor);
		if (patch != null)
			sb.append(".").append(patch);
		if (org.apache.commons.lang3.StringUtils.isNotBlank(prerelease))
			sb.append("-").append(prerelease);
		if (org.apache.commons.lang3.StringUtils.isNotBlank(build))
			sb.append("+").append(build);
		return sb.toString();
	}

}
