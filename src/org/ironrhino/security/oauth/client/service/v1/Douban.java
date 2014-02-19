package org.ironrhino.security.oauth.client.service.v1;

import java.io.StringReader;

import javax.xml.parsers.DocumentBuilderFactory;

import org.ironrhino.security.oauth.client.model.Profile;
import org.ironrhino.security.oauth.client.service.OAuth1Provider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

@Component
public class Douban extends OAuth1Provider {

	@Value("${douban.requestTokenUrl:http://www.douban.com/service/auth/request_token}")
	private String requestTokenUrl;

	@Value("${douban.authorizeUrl:http://www.douban.com/service/auth/authorize}")
	private String authorizeUrl;

	@Value("${douban.accessTokenUrl:http://www.douban.com/service/auth/access_token}")
	private String accessTokenUrl;

	@Value("${douban.logo:http://img3.douban.com/pics/nav/lg_main_a6.png}")
	private String logo;

	@Value("${douban.profileUrl:http://api.douban.com/people/%40me}")
	private String profileUrl;

	@Override
	public String getProfileUrl() {
		return profileUrl;
	}

	@Override
	public String getLogo() {
		return logo;
	}

	@Override
	public String getRequestTokenUrl() {
		return requestTokenUrl;
	}

	@Override
	public String getAuthorizeUrl() {
		return authorizeUrl;
	}

	@Override
	public String getAccessTokenUrl() {
		return accessTokenUrl;
	}

	@Override
	protected Profile getProfileFromContent(String content) throws Exception {
		Document doc = DocumentBuilderFactory.newInstance()
				.newDocumentBuilder()
				.parse(new InputSource(new StringReader(content)));
		String uid = doc.getElementsByTagName("db:uid").item(0)
				.getTextContent();
		String displayName = doc.getElementsByTagName("title").item(0)
				.getTextContent();
		Profile p = new Profile();
		p.setUid(generateUid(uid));
		p.setDisplayName(displayName);
		return p;
	}

}
