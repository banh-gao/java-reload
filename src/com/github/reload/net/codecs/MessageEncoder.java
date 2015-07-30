package com.github.reload.net.codecs;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import org.apache.log4j.Logger;
import com.github.reload.crypto.CryptoHelper;
import com.github.reload.crypto.Signer;
import com.github.reload.net.codecs.content.Content;
import com.github.reload.net.codecs.secBlock.GenericCertificate;
import com.github.reload.net.codecs.secBlock.SecurityBlock;
import com.github.reload.net.codecs.secBlock.Signature;
import com.github.reload.net.codecs.secBlock.GenericCertificate.CertificateType;

/**
 * Codec for message payload (content + security block)
 */
@Singleton
@Sharable
public class MessageEncoder extends MessageToByteEncoder<Message> {

	private static final int MAX_MESSAGE_SIZE = 5000;

	/**
	 * Size in bytes of the first part of the header from the beginning to the
	 * message length field
	 */
	public static int HDR_LEADING_LEN = 16;

	@Inject
	@Named("headerCodec")
	Codec<Header> hdrCodec;

	@Inject
	@Named("contentCodec")
	Codec<Content> contentCodec;

	@Inject
	@Named("secBlockCodec")
	Codec<SecurityBlock> secBlockCodec;

	@Inject
	CryptoHelper cryptoHelper;

	@Override
	protected void encode(ChannelHandlerContext ctx, Message msg, ByteBuf out) throws Exception {
		out.capacity(MAX_MESSAGE_SIZE);
		int messageStart = out.writerIndex();

		hdrCodec.encode(msg.header, out);

		int contentStart = out.writerIndex();
		contentCodec.encode(msg.content, out);

		ByteBuf rawContent = out.slice(contentStart, out.writerIndex() - contentStart);

		SecurityBlock secBlock = computeSecBlock(msg.header, rawContent, ctx.alloc());
		secBlockCodec.encode(secBlock, out);

		setMessageLength(out, messageStart);

		Logger.getRootLogger().trace(String.format("Message %#x encoded", msg.getHeader().getTransactionId()));
	}

	private void setMessageLength(ByteBuf buf, int messageStart) {
		int messageLength = buf.writerIndex() - messageStart;
		buf.setInt(messageStart + HDR_LEADING_LEN, messageLength);
	}

	private SecurityBlock computeSecBlock(Header header, ByteBuf rawContent, ByteBufAllocator bufAlloc) throws Exception {
		Signer signer = cryptoHelper.newSigner();

		ByteBuf signedDataBuf = bufAlloc.buffer();
		signedDataBuf.writeInt(header.getOverlayHash());
		signedDataBuf.writeLong(header.getTransactionId());
		signedDataBuf.writeBytes(rawContent);

		signer.update(signedDataBuf);

		signedDataBuf.release();

		Signature signature = signer.sign();

		List<GenericCertificate> gCerts = new ArrayList<GenericCertificate>();

		for (Certificate c : cryptoHelper.getLocalTrustRelationship()) {
			gCerts.add(new GenericCertificate(CertificateType.valueOfString(c.getType()), c));
		}

		return new SecurityBlock(gCerts, signature);
	}
}