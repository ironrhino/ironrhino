package org.ironrhino.sample.remoting;

import org.ironrhino.core.remoting.Remoting;

@Remoting
public interface FooService {

	String test(String value);

}
