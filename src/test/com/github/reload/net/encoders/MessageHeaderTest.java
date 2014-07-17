package com.github.reload.net.encoders;

import static org.junit.Assert.assertEquals;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.Test;
import com.github.reload.net.encoders.content.PingRequest;
import com.github.reload.net.encoders.header.DestinationList;
import com.github.reload.net.encoders.header.ForwardingOption;
import com.github.reload.net.encoders.header.Header;
import com.github.reload.net.encoders.header.Header.Builder;
import com.github.reload.net.encoders.header.UnknownForwardingOption;
import com.github.reload.net.encoders.secBlock.GenericCertificate;
import com.github.reload.net.encoders.secBlock.SecurityBlock;
import com.github.reload.net.encoders.secBlock.Signature;

public class MessageHeaderTest extends MessageTest {

	private static final int C_SEQ = 2;
	private static final DestinationList D_LIST = new DestinationList(TEST_NODEID);
	private static final List<? extends ForwardingOption> F_OPT = Collections.singletonList(new UnknownForwardingOption() {

		@Override
		public boolean isDestinationCritical() {
			return true;
		}

		public boolean isForwardCritical() {
			return true;
		};

		public boolean isResponseCopy() {
			return true;
		};
	});
	private static final int F_OFF = 23;
	private static final boolean F_LAST = false;
	private static final int M_RESP_L = 2230;
	private static final int O_HASH = 1234567;
	private static final short TTL = 6;
	private static final short VERS = 0x0a;
	private static final DestinationList V_LIST = new DestinationList(TEST_NODEID);

	@Test
	public void testHeader() throws Exception {

		Builder b = new Header.Builder();

		b.setConfigurationSequence(C_SEQ);
		b.setDestinationList(D_LIST);
		b.setForwardingOptions(F_OPT);
		b.setFragmentOffset(F_OFF);
		b.setLastFragment(F_LAST);
		b.setMaxResponseLength(M_RESP_L);
		b.setOverlayHash(O_HASH);
		b.setTtl(TTL);
		b.setVersion(VERS);
		b.setViaList(V_LIST);

		Header header = b.build();

		SecurityBlock s = new SecurityBlock(new ArrayList<GenericCertificate>(), Signature.EMPTY_SIGNATURE);
		Message message = new Message(header, new PingRequest(), s);

		Header echo = sendMessage(message).getHeader();

		assertEquals(header.getTransactionId(), echo.getTransactionId());

		assertEquals(C_SEQ, echo.getConfigurationSequence());
		assertEquals(D_LIST, echo.getDestinationList());
		assertEquals(F_OPT.get(0).getType(), echo.getForwardingOptions().get(0).getType());
		assertEquals(F_OFF, echo.getFragmentOffset());
		assertEquals(F_LAST, echo.isLastFragment());
		assertEquals(M_RESP_L, echo.getMaxResponseLength());
		assertEquals(O_HASH, echo.getOverlayHash());
		assertEquals(TTL, echo.getTimeToLive());
		assertEquals(VERS, echo.getVersion());
		assertEquals(V_LIST, echo.getViaList());
	}
}
