package com.github.reload.crypto;

import java.security.cert.Certificate;
import java.security.cert.CertificateException;

/**
 * Parses a certificate to a reload certificate
 */
public interface ReloadCertificateParser {

	/**
	 * Try to parse a certificate to find values needed to operate in a RELOAD
	 * overlay
	 * 
	 * @return The RELOAD compatible certificate
	 * @throws CertificateException
	 *             if the certificate is not usable with RELOAD
	 */
	public ReloadCertificate parse(Certificate certificate) throws CertificateException;
}