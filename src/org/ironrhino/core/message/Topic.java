package org.ironrhino.core.message;

import java.io.Serializable;

import org.ironrhino.core.metadata.Scope;

public interface Topic<T extends Serializable> {

	void subscribe(T message);

	void publish(T message, Scope scope);

	default void publish(T message) {
		publish(message, null);
	}

}
