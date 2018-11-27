package org.ironrhino.core.remoting;

import lombok.experimental.UtilityClass;

@UtilityClass
public class RemotingContext {

	public static final int SC_SERIALIZATION_FAILED = 499;

	public static final String CONTENT_TYPE_JAVA_SERIALIZED_OBJECT = "application/x-java-serialized-object";

	public static final String CONTENT_TYPE_FST_SERIALIZED_OBJECT = "application/x-fst-serialized-object";

	public static final String CONTENT_TYPE_JSON_RPC = "application/json-rpc";

	public static final String CONTENT_TYPE_SMILE_RPC = "application/smile-rpc";

	public static final String HTTP_HEADER_EXCEPTION_MESSAGE = "X-Exception-Message";

}