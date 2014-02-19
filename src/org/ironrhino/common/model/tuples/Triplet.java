package org.ironrhino.common.model.tuples;

import java.io.Serializable;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

public class Triplet<A, B, C> implements Serializable {

	private static final long serialVersionUID = -41533742214141900L;

	private A a;

	private B b;

	private C c;

	public Triplet() {

	}

	public Triplet(A a, B b, C c) {
		this.a = a;
		this.b = b;
		this.c = c;
	}

	public A getA() {
		return a;
	}

	public void setA(A a) {
		this.a = a;
	}

	public B getB() {
		return b;
	}

	public void setB(B b) {
		this.b = b;
	}

	public C getC() {
		return c;
	}

	public void setC(C c) {
		this.c = c;
	}

	@Override
	public int hashCode() {
		return HashCodeBuilder.reflectionHashCode(this, false);
	}

	@Override
	public boolean equals(Object that) {
		return EqualsBuilder.reflectionEquals(this, that, false);
	}

	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this);
	}

}