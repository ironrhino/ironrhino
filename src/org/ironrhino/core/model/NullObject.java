package org.ironrhino.core.model;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamException;
import java.io.Serializable;

public class NullObject implements Serializable {

	private static final long serialVersionUID = 6123024289220735487L;

	private static final NullObject instance = new NullObject();

	private NullObject() {
	}

	public static NullObject get() {
		return instance;
	}

	private Object readResolve() throws ObjectStreamException {
		return instance;
	}

	private void writeObject(ObjectOutputStream out) throws IOException {

	}

	private void readObject(ObjectInputStream in) throws IOException {

	}

	public boolean equals(Object obj) {
		return this == obj || obj instanceof NullObject;
	}

	@Override
	public int hashCode() {
		return NullObject.class.hashCode();
	}

	@Override
	public String toString() {
		return "null";
	}

}
