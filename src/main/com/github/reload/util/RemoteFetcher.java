package com.github.reload.util;

import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.conn.params.ConnRoutePNames;
import org.apache.http.impl.client.DefaultHttpClient;

/**
 * Http client base class
 * 
 * @author Daniel Zozin <zdenial@gmx.com>
 * 
 */
abstract class RemoteFetcher {

	protected DefaultHttpClient httpClient = new DefaultHttpClient();

	/**
	 * Set the proxy configuration to use
	 * 
	 * @param config
	 */
	public void setProxy(ProxyConfig config) {
		if (config.isUseAuth()) {
			httpClient.getCredentialsProvider().setCredentials(new AuthScope(config.host, config.port), new UsernamePasswordCredentials(config.username, config.password));
		}
		HttpHost proxy = new HttpHost(config.host, config.port);
		httpClient.getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY, proxy);
	}

	/**
	 * The proxy configuration to be used
	 * 
	 * @author Daniel Zozin <zdenial@gmx.com>
	 */
	public static class ProxyConfig {

		private String host;
		private int port;
		private String username;
		private String password;

		public String getHost() {
			return host;
		}

		public void setHost(String host) {
			this.host = host;
		}

		public int getPort() {
			return port;
		}

		public void setPort(int port) {
			this.port = port;
		}

		public String getUsername() {
			return username;
		}

		public void setUsername(String username) {
			this.username = username;
		}

		public String getPassword() {
			return password;
		}

		public void setPassword(String password) {
			this.password = password;
		}

		public boolean isUseAuth() {
			return (username != null && !username.isEmpty());
		}
	}
}
