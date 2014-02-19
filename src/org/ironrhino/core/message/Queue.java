package org.ironrhino.core.message;

import java.io.Serializable;

public interface Queue<T extends Serializable> {

	public void consume(T message);

	public void produce(T message);

}
