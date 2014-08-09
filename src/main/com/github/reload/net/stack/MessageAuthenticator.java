package com.github.reload.net.stack;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import java.security.GeneralSecurityException;
import java.security.SignatureException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.List;
import org.apache.log4j.Logger;
import com.github.reload.crypto.CryptoHelper;
import com.github.reload.crypto.ReloadCertificate;
import com.github.reload.net.encoders.Header;
import com.github.reload.net.encoders.Message;
import com.github.reload.net.encoders.header.NodeID;
import com.github.reload.net.encoders.secBlock.GenericCertificate;
import com.github.reload.net.encoders.secBlock.Signature;

public class MessageAuthenticator extends SimpleChannelInboundHandler<Message> {

	private static final Logger l = Logger.getRootLogger();

	private final CryptoHelper<Certificate> cryptoHelper;

	public MessageAuthenticator(CryptoHelper<Certificate> cryptoHelper) {
		this.cryptoHelper = cryptoHelper;
	}

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, Message msg) {

		Certificate trustedPeerCert;
		try {
			trustedPeerCert = authenticateCertificates(msg);

			ReloadCertificate cert = authenticateSender(msg, trustedPeerCert);

			cryptoHelper.addCertificate(cert);

			authenticateSignature(msg, cert, ctx.alloc());

		} catch (GeneralSecurityException e) {
			// TODO: react to authentication failure
			l.info(e.getMessage());
			return;
		}

		// Authentication succeed, pass to upper layer
		ctx.fireChannelRead(msg);
	}

	private ReloadCertificate authenticateSender(Message msg, Certificate trustedPeerCert) throws CertificateException {
		NodeID untrustedSender = msg.getHeader().getSenderId();
		ReloadCertificate reloadCert = cryptoHelper.getCertificateParser().parse(trustedPeerCert);

		if (!reloadCert.getNodeId().equals(untrustedSender))
			throw new CertificateException(String.format("Untrusted sender %s for message %#x: Sender node-id not matching certificate node-id %s", untrustedSender, msg.getHeader().getTransactionId(), reloadCert.getNodeId()));

		return reloadCert;
	}

	private Certificate authenticateCertificates(Message msg) throws CertificateException {

		List<GenericCertificate> certs = msg.getSecBlock().getCertificates();

		List<Certificate> javaCerts = new ArrayList<Certificate>();
		for (GenericCertificate c : certs) {
			javaCerts.add(c.getCertificate());
		}

		for (Certificate c : javaCerts) {
			for (Certificate validIssuer : cryptoHelper.getAcceptedIssuers()) {
				try {
					javaCerts.add(validIssuer);
					List<? extends Certificate> trustChain = cryptoHelper.getTrustRelationship(c, validIssuer, javaCerts);
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
