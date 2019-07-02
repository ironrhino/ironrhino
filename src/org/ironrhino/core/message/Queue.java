package org.ironrhino.core.message;

import java.io.Serializable;

public interface Queue<T extends Serializable> {

	void consume(T message);

	void produce(T message);

}
