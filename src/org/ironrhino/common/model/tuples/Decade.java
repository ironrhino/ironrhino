package org.ironrhino.common.model.tuples;

import java.io.Serializable;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

public class Decade<A, B, C, D, E, F, G, H, I, J> implements Serializable {

	private static final long serialVersionUID = 2057926095491457209L;

	private A a;

	private B b;

	private C c;

	private D d;

	private E e;

	private F f;

	private G g;

	private H h;

	private I i;

	private J j;

	public Decade() {

	}

	public Decade(A a, B b, C c, D d, E e, F f, G g, H h, I i, J j) {
		this.a = a;
		this.b = b;
		this.c = c;
		this.d = d;
		this.e = e;
		this.f = f;
		this.g = g;
		this.h = h;
		this.i = i;
		this.j = j;
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

	public D getD() {
		return d;
	}

	public void setD(D d) {
		this.d = d;
	}

	public E getE() {
		return e;
	}

	public void setE(E e) {
		this.e = e;
	}

	public F getF() {
		return f;
	}

	public void setF(F f) {
		this.f = f;
	}

	public G getG() {
		return g;
	}

	public void setG(G g) {
		this.g = g;
	}

	public H getH() {
		return h;
	}

	public void setH(H h) {
		this.h = h;
	}

	public I getI() {
		return i;
	}

	public void setI(I i) {
		this.i = i;
	}

	public J getJ() {
		return j;
	}

	public void setJ(J j) {
		this.j = j;
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