package com.github.reload.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.Hashtable;
import java.util.Map;
import java.util.TreeMap;
import javax.naming.ConfigurationException;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import com.github.reload.conf.Configuration;

/**
 * Utility class to fetch, parse and authenticate an overlay configuration from
 * a remote configuration server
 * 
 * @author Daniel Zozin <zdenial@gmx.com>
 * 
 */
public class ConfigurationFetcher extends RemoteFetcher {

	private static final String DNS_SRV_NAME_PREFIX = "_reload-config._tcp.";

	private static final String PROTOCOL = "https";
	private static final String WELL_KNOWN_PATH = ".well-known/reload-config";

	private ConfigurationFetcher() {
	}

	/**
	 * Get the configuration file fetched from the standard configuration
	 * location derived from the istance name. The instance name is used in the
	 * DNS SRV lookup as part of the service name.
	 * 
	 * 
	 * @param instanceName
	 *            The name of the overlay instance to connect to
	 * @throws ConfigurationException
	 *             if some error occurs while parsing the configuration
	 */
	public static Configuration fetchConfiguration(String instanceName) throws ConfigurationException {
		IOException ex = null;
		for (URL url : getServiceUrls(instanceName)) {
			try {
				return fetchConfiguration(instanceName, url.toURI());
			} catch (IOException e) {
				// Try all addresses before fail
				ex = e;
			} catch (URISyntaxException e) {
				throw new RuntimeException(e);
			}
		}
		throw new ConfigurationException(ex);
	}

	private static Collection<URL> getServiceUrls(String instanceName) throws ConfigurationException {
		Hashtable<String, String> env = new Hashtable<String, String>();
		env.put("java.naming.factory.initial", "com.sun.jndi.dns.DnsContextFactory");
		env.put("java.naming.provider.url", "dns:");

		try {
			DirContext ctx = new InitialDirContext(env);
			Attributes attrs = ctx.getAttributes(DNS_SRV_NAME_PREFIX + instanceName, new String[]{"SRV"});

			Map<Double, URL> results = new TreeMap<Double, URL>();

			// Sort results by priority
			for (Attribute attr : Collections.list(attrs.getAll())) {
				String[] res = ((String) attr.get()).split(" ");
				int priority = Integer.parseInt(res[0]);
				int weight = Integer.parseInt(res[1]);
				double pos = priority + weight * 0.01;
				try {
					results.put(pos, new URL(PROTOCOL, res[3], Integer.parseInt(res[2]), WELL_KNOWN_PATH));
				} catch (MalformedURLException e) {
					throw new RuntimeException(e);
				}
			}

			return results.values();
		} catch (NamingException e) {
			throw new ConfigurationException(e);
		}
	}

	/**
	 * Fetch the configuration file from the specified URI
	 * 
	 * @param instanceName
	 *            The name of the overlay instance to connect to
	 * @param configurationLocation
	 *            The location where to fetch the configuration file
	 * @throws ConfigurationException
	 *             if some error occurs while parsing the configuration
	 * @throws IOException
	 */
	public static Configuration fetchConfiguration(String instanceName, URI configurationLocation) throws ConfigurationException, IOException {
		ConfigurationFetcher f = new ConfigurationFetcher();
		try {
			HttpGet get = new HttpGet(configurationLocation);
			CloseableHttpResponse response = f.httpClient.execute(get);
			HttpEntity entity = response.getEntity();

			InputStream in = entity.getContent();

			ByteArrayOutputStream conf = new ByteArrayOutputStream();

			byte[] buf = new byte[1024];

			int readed = 0;
			while ((readed = in.read(buf)) != -1) {
				conf.write(buf, 0, readed);
			}

			in.close();

			return Configuration.parse(instanceName, conf.toByteArray());
		} catch (IOException e) {
			throw e;
		} finally {
			f.httpClient.getConnectionManager().shutdown();
		}
	}
}
