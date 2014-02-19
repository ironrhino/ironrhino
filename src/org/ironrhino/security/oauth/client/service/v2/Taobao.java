package org.ironrhino.security.oauth.client.service.v2;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import org.ironrhino.core.util.CodecUtils;
import org.ironrhino.core.util.DateUtils;
import org.ironrhino.core.util.JsonUtils;
import org.ironrhino.security.oauth.client.model.Profile;
import org.ironrhino.security.oauth.client.service.OAuth2Provider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;

@Component
public class Taobao extends OAuth2Provider {

	@Value("${taobao.logo:http://img01.taobaocdn.com/tps/i1/T1T2RZXf8nXXXXXXXX-140-35.png}")
	private String logo;

	@Value("${taobao.authorizeUrl:https://oauth.taobao.com/authorize}")
	private String authorizeUrl;

	@Value("${taobao.accessTokenEndpoint:https://oauth.taobao.com/token}")
	private String accessTokenEndpoint;

	@Value("${taobao.scope:}")
	private String scope;

	@Value("${taobao.profileUrl:http://gw.api.taobao.com/router/rest?method=taobao.user.buyer.get&fields=user_id,nick,sex}")
	private String profileUrl;

	@Override
	public String getLogo() {
		return logo;
	}

	@Override
	public String getAuthorizeUrl() {
		return authorizeUrl;
	}

	@Override
	public String getAccessTokenEndpoint() {
		return accessTokenEndpoint;
	}

	@Override
	public String getScope() {
		return scope;
	}

	@Override
	public String getProfileUrl() {
		StringBuilder sb = new StringBuilder(profileUrl);
		sb.append("&app_key=").append(getClientId()).append("&timestamp=");
		try {
			sb.append(URLEncoder.encode(DateUtils.formatDatetime(new Date()),
					"UTF-8"));
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return sb.toString();
	}

	@Override
	public boolean isUseAuthorizationHeader() {
		return false;
	}

	@Override
	protected String getAccessTokenParameterName() {
		return "session";
	}

	@Override
	protected Profile getProfileFromContent(String content) throws Exception {
		JsonNode data = JsonUtils.getObjectMapper().readValue(content,
				JsonNode.class);
		JsonNode response = data.get("user_buyer_get_response");
		if (response == null)
			response = data.get("user_seller_get_response");
		JsonNode user = response.get("user");
		String uid = user.get("user_id").asText();
		Profile p = new Profile();
		p.setUid(generateUid(uid));
		p.setDisplayName(user.get("nick").textValue());
		p.setName(user.get("nick").textValue());
		if (user.get("sex") != null)
			p.setGender(user.get("sex").textValue());
		return p;
	}

	@Override
	protected String invoke(String protectedURL, Map<String, String> params,
			Map<String, String> headers) throws IOException {
		if (params == null)
			params = new HashMap<String, String>(8);
		params.put("format", "json");
		params.put("v", "2.0");
		params.put("sign_method", "md5");
		params.put("sign", sign(protectedURL, params));
		return super.invoke(protectedURL, params, headers);
	}

	private String sign(String protectedURL, Map<String, String> params) {
		Map<String, String> map = new TreeMap<String, String>();
		if (protectedURL.indexOf('?') > 0) {
			String queryString = protectedURL.substring(protectedURL
					.indexOf('?') + 1);
			String[] arr = queryString.split("&");
			for (String s : arr) {
				String[] arr2 = s.split("=");
				String key = arr2[0];
				String value = "";
				if (arr2.length > 1) {
					try {
						value = URLDecoder.decode(arr2[1], "UTF-8");
					} catch (UnsupportedEncodingException e) {
						e.printStackTrace();
					}
				}
				map.put(key, value);
			}
		}
		if (params != null)
			map.putAll(params);
		StringBuilder sb = new StringBuilder(getClientSecret());
		for (Map.Entry<String, String> entry : map.entrySet())
			sb.append(entry.getKey()).append(entry.getValue());
		sb.append(getClientSecret());
		return CodecUtils.md5Hex(sb.toString()).toUpperCase();
	}

}
