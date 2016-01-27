package org.ironrhino.core.spring.configuration;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.Map;

import org.ironrhino.core.jdbc.DatabaseProduct;
import org.ironrhino.core.util.AppInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.core.type.ClassMetadata;

public class AddressAvailabilityCondition implements Condition {

	private Logger logger = LoggerFactory.getLogger(getClass());

	@Override
	public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
		Map<String, Object> attributes = metadata
				.getAnnotationAttributes(AddressAvailabilityConditional.class.getName());
		String address = (String) attributes.get("address");
		address = AppInfo.resolvePlaceholders(address);
		int timeout = (Integer) attributes.get("timeout");
		boolean negated = (Boolean) attributes.get("negated");
		boolean matched = matches(address, timeout, negated);
		if (!matched && (metadata instanceof ClassMetadata)) {
			ClassMetadata cm = (ClassMetadata) metadata;
			logger.info("Bean[" + cm.getClassName() + "] is skipped registry");
		}
		return matched;
	}

	public static boolean matches(String address, int timeout, boolean negated) {
		boolean matched = check(address, timeout);
		if (negated)
			matched = !matched;
		return matched;
	}

	public static boolean check(String address, int timeout) {
		String host;
		int port;
		int index = address.indexOf("://");
		if (index > 0) {
			String url = address;
			String scheme = address.substring(0, index);
			address = address.substring(index + 3);
			if (address.isEmpty() || address.startsWith("/")) {
				host = "localhost";
				port = 0;
			} else {
				index = address.indexOf('/');
				if (index > -1) {
					address = address.substring(0, index);
				}
				index = address.indexOf(':');
				if (index > 0) {
					host = address.substring(0, index);
					port = Integer.parseInt(address.substring(index + 1));
				} else {
					host = address;
					port = 0;
				}
			}
			if (host == null) {
				host = "localhost";
			}
			if (port == 0) {
				if (scheme.startsWith("jdbc:")) {
					port = DatabaseProduct.parse(url).getDefaultPort();
				} else {
					switch (scheme) {
					case "http":
						port = 80;
						break;
					case "https":
						port = 443;
						break;
					case "ftp":
						port = 21;
						break;
					case "ssh":
						port = 22;
						break;
					case "telnet":
						port = 23;
						break;
					case "ldap":
						port = 636;
						break;
					case "smtp":
						port = 25;
						break;
					case "imap":
						port = 143;
						break;
					case "pop3":
						port = 110;
						break;
					default:
						break;
					}
				}
			}

		} else {
			index = address.lastIndexOf(':');
			if (index < 0)
				throw new IllegalArgumentException("address '" + address + "' should be host:port");
			host = address.substring(0, index);
			port = Integer.parseInt(address.substring(index + 1));
		}
		Socket s = null;
		try {
			s = new Socket();
			s.setReuseAddress(true);
			SocketAddress sa = new InetSocketAddress(host, port);
			s.connect(sa, timeout);
			return true;
		} catch (IOException e) {
			return false;
		} finally {
			if (s != null) {
				try {
					s.close();
				} catch (IOException e) {
				}
			}
		}
	}

}
