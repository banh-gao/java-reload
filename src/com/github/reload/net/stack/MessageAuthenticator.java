package com.github.reload.net.stack;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import java.security.GeneralSecurityException;
import java.security.SignatureException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.apache.log4j.Logger;
import com.github.reload.conf.Configuration;
import com.github.reload.crypto.CryptoHelper;
import com.github.reload.crypto.Keystore;
import com.github.reload.crypto.ReloadCertificate;
import com.github.reload.net.codecs.Header;
import com.github.reload.net.codecs.Message;
import com.github.reload.net.codecs.header.NodeID;
import com.github.reload.net.codecs.secBlock.Signature;

@Sharable
@Singleton
public class MessageAuthenticator extends SimpleChannelInboundHandler<Message> {

	private static final Logger l = Logger.getRootLogger();

	@Inject
	Configuration conf;

	@Inject
	Keystore keystore;

	@Inject
	CryptoHelper cryptoHelper;

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, Message msg) {

		Certificate trustedPeerCert;
		try {
			trustedPeerCert = authenticateCertificates(msg);

			ReloadCertificate cert = authenticateSender(msg, trustedPeerCert);

			keystore.addCertificate(cert);

			authenticateSignature(msg, cert, ctx.alloc());

		} catch (GeneralSecurityException e) {
			l.info(e.getMessage());
			return;
		}

		// Authentication succeed, pass to upper layer
		ctx.fireChannelRead(msg);
	}

	private ReloadCertificate authenticateSender(Message msg, Certificate trustedPeerCert) throws CertificateException {
		NodeID untrustedSender = msg.getHeader().getSenderId();
		ReloadCertificate reloadCert = cryptoHelper.toReloadCertificate(trustedPeerCert);

		if (!reloadCert.getNodeId().equals(untrustedSender))
			throw new CertificateException(String.format("Untrusted sender %s for message %#x: Sender node-id not matching certificate node-id %s", untrustedSender, msg.getHeader().getTransactionId(), reloadCert.getNodeId()));

		return reloadCert;
	}

	private Certificate authenticateCertificates(Message msg) throws CertificateException {

		@SuppressWarnings("unchecked")
		List<Certificate> certs = (List<Certificate>) msg.getSecBlock().getCertificates();

		for (Certificate c : certs) {
			for (Certificate validIssuer : conf.get(Configuration.ROOT_CERTS)) {
				try {
					certs.add(validIssuer);
					List<? extends Certificate> trustChain = cryptoHelper.getTrustRelationship(c, validIssuer, certs);
					return trustChain.get(0);
				} catch (GeneralSecurityException e) {
					e.printStackTrace();
				}
			}
		}

		throw new CertificateException("Untrusted peer certificate");
	}

	private void authenticateSignature(Message msg, ReloadCertificate peerCert, ByteBufAllocator alloc) throws GeneralSecurityException {
		ByteBuf signedData = alloc.buffer();
		Header h = msg.getHeader();

		signedData.writeInt(h.getOverlayHash());
		signedData.writeLong(h.getTransactionId());
		ByteBuf rawContent = h.getAttribute(Header.RAW_CONTENT);
		signedData.writeBytes(rawContent);
		rawContent.release();

		Signature sign = msg.getSecBlock().getSignature();

		if (!sign.verify(signedData, peerCert.getOriginalCertificate().getPublicKey())) {
			signedData.release();
			throw new SignatureException("Invalid message signature");
		}

		signedData.release();
	}
}
