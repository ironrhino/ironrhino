package org.ironrhino.core.spring.configuration;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;

import org.ironrhino.core.jdbc.DatabaseProduct;
import org.ironrhino.core.util.AppInfo;

public class AddressAvailabilityCondition extends SimpleCondition<AddressAvailabilityConditional> {

	@Override
	public boolean matches(AddressAvailabilityConditional annotation) {
		return matches(annotation.address(), annotation.timeout(), annotation.negated());
	}

	public static boolean matches(String address, int timeout, boolean negated) {
		address = AppInfo.resolvePlaceholders(address);
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
					if (address.startsWith("[")) {
						String[] arr = address.split("]", 2);
						host = arr[0] + ']';
						if (arr[1].isEmpty()) {
							port = 0;
						} else {
							port = Integer.parseInt(arr[1].substring(1));
						}
					} else {
						host = address.substring(0, index);
						port = Integer.parseInt(address.substring(index + 1));
					}
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
