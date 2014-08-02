package com.github.reload.util;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpPost;
import org.bouncycastle.jce.PKCS10CertificationRequest;
import com.github.reload.crypto.ReloadCertificate;
import com.github.reload.crypto.X509CertificateParser;

/**
 * Utility class to request a signed certificate to a remote enrollment web
 * server for the overlay
 * 
 * @author Daniel Zozin <zdenial@gmx.com>
 * 
 */
public class CertificateFetcher extends RemoteFetcher {

	private static final Pattern EMAIL_PATTERN = Pattern.compile("^[_A-Za-z0-9-]+(\\.[_A-Za-z0-9-]+)*@[A-Za-z0-9]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$");

	private final List<Certificate> rootCerts;
	private final URL enrollmentServer;

	private final PKCS10CertificationRequest csr;
	private String username;
	private String password;
	private int neededIds = 1;

	/**
	 * Create a new certificate fetcher
	 * 
	 * @param enrollmentServer
	 *            The address of the enrollment server
	 * @param csr
	 *            The sign request to be signed by the enrollment server
	 * @param rootCerts
	 *            The root certificates of the enrollment server to authenticate
	 *            the returned certificate
	 */
	public CertificateFetcher(URL enrollmentServer, PKCS10CertificationRequest csr, List<Certificate> rootCerts) {
		this.enrollmentServer = enrollmentServer;
		this.rootCerts = rootCerts;
		this.csr = csr;
	}

	/**
	 * Set login values for enrollment server
	 * 
	 * @param username
	 *            a valid RFC822Name email address
	 * @param password
	 * @throws IllegalArgumentException
	 *             if the username is not a valid RFC822Name email address
	 */
	public void setLogin(String username, String password) {
		Matcher matcher = EMAIL_PATTERN.matcher(username);
		if (!matcher.matches())
			throw new IllegalArgumentException("The username must be a RFC822Name email address");
		this.username = username;
		this.password = password;
	}

	/**
	 * Set the amount of node-ids wanted in the certificate, defaults is 1
	 * 
	 * @param neededIds
	 * @throws IllegalArgumentException
	 *             if the neededIds is lower than 1
	 */
	public void setNeededNodeIds(int neededIds) {
		if (neededIds < 1)
			throw new IllegalArgumentException();
		this.neededIds = neededIds;
	}

	/**
	 * Send the request to the enrollment server
	 * 
	 * @return The signed certificate
	 * @throws IOException
	 *             If a network error occurs while fetching the certificate
	 * @throws CertificateException
	 *             If the returned certificate is not valid
	 */
	public ReloadCertificate fetch() throws IOException, CertificateException {
		HttpPost postRequest;
		try {
			postRequest = new HttpPost(enrollmentServer.toURI());
			postRequest.addHeader("Accept", "application/pkix-cert");

			MultipartEntity requestForm = new MultipartEntity();
			if (username != null && !username.isEmpty()) {
				requestForm.addPart("username", new StringBody(username, Charset.forName("UTF-8")));
			}
			if (password != null && !password.isEmpty()) {
				requestForm.addPart("password", new StringBody(password, Charset.forName("UTF-8")));
			}

			if (neededIds > 1) {
				requestForm.addPart("nodeids", new StringBody(String.valueOf(neededIds), Charset.forName("UTF-8")));
			}

			requestForm.addPart("csr", new ByteArrayBody(csr.getEncoded(), "application/pkcs10"));

			postRequest.setEntity(requestForm);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

		HttpResponse response = httpClient.execute(postRequest);

		return parseResponse(response);
	}

	private ReloadCertificate parseResponse(HttpResponse response) throws IOException, CertificateException {
		if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK)
			throw new IOException(response.getStatusLine().getReasonPhrase());

		List<String> contentType = Arrays.asList(response.getEntity().getContentType().getValue().toLowerCase().replaceAll(" ", "").split(";"));

		if (!contentType.contains("application/pkix-cert"))
			throw new IOException("Invalid response content type enrollment server");

		if (!contentType.contains("charset=utf-8"))
			throw new IOException("Invalid response encoding from enrollment server");

		X509Certificate cert = (X509Certificate) CertificateFactory.getInstance("x.509").generateCertificate(response.getEntity().getContent());

		if (!Arrays.equals(cert.getPublicKey().getEncoded(), (csr.getSubjectPublicKeyInfo().getEncoded())))
			throw new CertificateException("The fetched certificate is doesn't contain the requested public key");

		for (Certificate rootCert : rootCerts) {
			try {
				cert.verify(rootCert.getPublicKey());

				return X509CertificateParser.parse(cert);
			} catch (Exception e) {
				// Checked later
			}
		}

		throw new CertificateException("The fetched certificate is not authenticated by a trusted root CA");
	}
}
