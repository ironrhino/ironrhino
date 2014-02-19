package org.ironrhino.core.search.elasticsearch;

import static org.elasticsearch.node.NodeBuilder.nodeBuilder;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.node.Node;
import org.ironrhino.core.util.AppInfo;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;

public class ElasticSearchClientFactoryBean implements FactoryBean<Client>,
		InitializingBean, DisposableBean {

	private Client client;

	private String connectString = "";

	private Map<String, String> settings;

	public void setSettings(Map<String, String> settings) {
		this.settings = settings;
	}

	public String getConnectString() {
		return connectString;
	}

	public void setConnectString(String connectString) {
		this.connectString = connectString;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		if (StringUtils.isBlank(connectString)) {
			Map<String, String> map = new HashMap<String, String>();
			map.put("path.home", AppInfo.getAppHome() + "/search");
			if (settings != null)
				map.putAll(settings);
			Settings settings = ImmutableSettings.settingsBuilder().put(map)
					.build();
			Node node = nodeBuilder().settings(settings).node();
			client = node.client();
		} else {
			TransportClient tclient = new TransportClient();
			for (String s : connectString.split("\\s*,\\s*")) {
				String arr[] = s.trim().split(":", 2);
				tclient.addTransportAddress(new InetSocketTransportAddress(
						arr[0], arr.length == 2 ? Integer.valueOf(arr[1])
								: 9300));
			}
			client = tclient;
		}
	}

	@Override
	public void destroy() throws Exception {
		client.close();
	}

	@Override
	public Client getObject() throws Exception {
		return client;
	}

	@Override
	public Class<? extends Client> getObjectType() {
		return Client.class;
	}

	@Override
	public boolean isSingleton() {
		return true;
	}
}