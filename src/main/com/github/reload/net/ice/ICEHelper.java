package com.github.reload.net.ice;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import com.github.reload.net.ice.IceCandidate.OverlayLinkType;

// TODO: Implement full ICE
public class ICEHelper {

	/**
	 * @return the ICE candidates to be used to reach this peer
	 */
	public List<IceCandidate> getCandidates(InetSocketAddress listeningAddress) {
		List<IceCandidate> candidates = new ArrayList<IceCandidate>();
		for (InetAddress addr : getInterfaceAddresses()) {
			candidates.add(createHostCandidate(addr, listeningAddress.getPort()));
		}
		return candidates;
	}

	private static HostCandidate createHostCandidate(InetAddress address, int port) {
		HostCandidate candidate = null;
		if (address instanceof Inet4Address) {
			candidate = new HostCandidate(new IPv4AddressPort(address, port));
		} else if (address instanceof Inet6Address) {
			candidate = new HostCandidate(new IPv6AddressPort(address, port));
		}
		assert (candidate != null);

		candidate.setOverlayLinkType(OverlayLinkType.TLS_TCP_FH_NO_ICE);
		return candidate;
	}

	public List<InetAddress> getInterfaceAddresses() {
		// FIXME: TEST return loopback only for test
		if (true) {
			try {
				return Collections.singletonList(InetAddress.getByName("127.0.0.1"));
			} catch (UnknownHostException e1) {
				e1.printStackTrace();
			}
		}

		// END TEST BLOCK

		List<InetAddress> out = new ArrayList<InetAddress>();
		Enumeration<NetworkInterface> netIfs;
		try {
			netIfs = NetworkInterface.getNetworkInterfaces();
		} catch (SocketException e) {
			return out;
		}
		while (netIfs.hasMoreElements()) {
			NetworkInterface netIf = netIfs.nextElement();
			for (InterfaceAddress ifAddr : netIf.getInterfaceAddresses()) {
				// NOTE: TEST only IPv4 for wireshark ssl decrypt
				if (ifAddr.getAddress() instanceof Inet4Address) {
					out.add(ifAddr.getAddress());
				}
			}
		}
		return out;
	}

	/**
	 * Tests the specified ICE candidates and return the one to use
	 * 
	 * @param candidates
	 *            the available candidates
	 * @return the ICE candidate to use
	 * @throws NoSuitableCandidateException
	 *             if no valid candidate has been found
	 */
	public IceCandidate testAndSelectCandidate(List<IceCandidate> candidates) throws NoSuitableCandidateException {
		if (candidates.size() == 0)
			throw new NoSuitableCandidateException("Empty candidates list");
		return candidates.get(0);
	}

	public byte[] getUserFragment() {
		return new byte[0];
	}

	public byte[] getPassword() {
		return new byte[0];
	}
}
