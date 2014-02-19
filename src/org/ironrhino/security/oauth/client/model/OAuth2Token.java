package org.ironrhino.security.oauth.client.model;

import org.apache.commons.lang3.StringUtils;
import org.ironrhino.core.metadata.NotInJson;
import org.ironrhino.core.util.JsonUtils;

import com.fasterxml.jackson.databind.JsonNode;

public class OAuth2Token extends OAuthToken {

	private static final long serialVersionUID = 3664222731669918663L;
	private String access_token;
	private String token_type;
	private int expires_in;
	private long create_time;
	private String refresh_token;

	public OAuth2Token() {
		super();
	}

	public OAuth2Token(String source) {
		super(source);
	}

	@Override
	public void setSource(String source) {
		if (StringUtils.isBlank(source))
			return;
		if (JsonUtils.isValidJson(source)) {
			source = JsonUtils.unprettify(source);
			try {
				JsonNode map = JsonUtils.fromJson(source, JsonNode.class);
				JsonNode node = map.get("access_token");
				if (node != null)
					access_token = node.asText();
				node = map.get("token_type");
				if (node != null)
					token_type = node.asText();
				node = map.get("refresh_token");
				if (node != null)
					refresh_token = node.asText();
				node = map.get("expires_in");
				if (node != null)
					expires_in = node.asInt();
				node = map.get("create_time");
				if (node != null) {
					create_time = node.asLong();
				} else {
					create_time = System.currentTimeMillis();
					StringBuilder sb = new StringBuilder(source.substring(0,
							source.length() - 1));
					sb.append(",\"create_time\":");
					sb.append(create_time);
					sb.append("}");
					source = sb.toString();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else {
			String[] arr1 = source.split("&");
			for (String s : arr1) {
				String[] arr2 = s.split("=", 2);
				if (arr2.length > 1 && arr2[0].equals("access_token"))
					access_token = arr2[1];
				else if (arr2.length > 1 && arr2[0].equals("token_type"))
					token_type = arr2[1];
				else if (arr2.length > 1 && arr2[0].equals("refresh_token"))
					refresh_token = arr2[1];
				else if (arr2.length > 1 && arr2[0].equals("expires_in"))
					expires_in = Integer.valueOf(arr2[1]);
				else if (arr2.length > 1 && arr2[0].equals("create_time"))
					create_time = Long.valueOf(arr2[1]);
			}
			if (expires_in > 0 && create_time == 0) {
				create_time = System.currentTimeMillis();
				source += "&create_time=" + create_time;
			}
		}
		super.setSource(source);
	}

	public String getAccess_token() {
		return access_token;
	}

	public void setAccess_token(String access_token) {
		this.access_token = access_token;
	}

	public String getToken_type() {
		return token_type;
	}

	public void setToken_type(String token_type) {
		this.token_type = token_type;
	}

	public int getExpires_in() {
		return expires_in;
	}

	public void setExpires_in(int expires_in) {
		this.expires_in = expires_in;
	}

	public String getRefresh_token() {
		return refresh_token;
	}

	public void setRefresh_token(String refresh_token) {
		this.refresh_token = refresh_token;
	}

	public long getCreate_time() {
		return create_time;
	}

	public void setCreate_time(long create_time) {
		this.create_time = create_time;
		if (expires_in > 0) {
			if (JsonUtils.isValidJson(source)) {
				StringBuilder sb = new StringBuilder(source.substring(0,
						source.length() - 1));
				sb.append(",\"create_time\":");
				sb.append(create_time);
				sb.append("}");
				source = sb.toString();
			} else {
				create_time = System.currentTimeMillis();
				source += "&create_time=" + create_time;
			}
		}
	}

	@NotInJson
	public boolean isExpired() {
		if (expires_in <= 0 || create_time <= 0)
			return false;
		int offset = 60;
		return (System.currentTimeMillis() - create_time) / 1000 > (expires_in - offset);
	}

	@NotInJson
	public boolean isGoingToExpired() {
		if (expires_in <= 0 || create_time <= 0)
			return false;
		int offset = 5 * 60;
		return (System.currentTimeMillis() - create_time) / 1000 > (expires_in - offset);
	}

}