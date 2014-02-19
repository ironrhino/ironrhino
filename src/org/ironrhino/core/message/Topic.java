package org.ironrhino.core.message;

import java.io.Serializable;

import org.ironrhino.core.metadata.Scope;

public interface Topic<T extends Serializable> {

	public void subscribe(T message);

	public void publish(T message, Scope scope);

}
