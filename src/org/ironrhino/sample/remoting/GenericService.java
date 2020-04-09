package org.ironrhino.sample.remoting;

public interface GenericService<T> {

	T echoGenericUserDetails(T t);

}
