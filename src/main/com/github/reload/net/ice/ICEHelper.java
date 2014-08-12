package com.github.reload.net.ice;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import com.github.reload.components.ComponentsRepository.Component;
import com.github.reload.net.ice.HostCandidate.OverlayLinkType;

// TODO: Implement full ICE
@Component(ICEHelper.class)
public class ICEHelper {

	/**
	 * @return the ICE candidates to be used to reach this peer
	 */
	public List<HostCandidate> getCandidates(InetSocketAddress listeningAddress) {
		List<HostCandidate> candidates = new ArrayList<HostCandidate>();
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
		List<InetAddress> out = new ArrayList<InetAddress>();

		// Include loopback address for local connections
		out.add(InetAddress.getLoopbackAddress());

		Enumeration<NetworkInterface> netIfs;
		try {
			netIfs = NetworkInterface.getNetworkInterfaces();
		} catch (SocketException e) {
			return out;
		}
		while (netIfs.hasMoreElements()) {
			NetworkInterface netIf = netIfs.nextElement();
			for (InterfaceAddress ifAddr : netIf.getInterfaceAddresses()) {
				out.add(ifAddr.getAddress());
			}
		}
		System.out.println(out);
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
	public HostCandidate testAndSelectCandidate(List<HostCandidate> candidates) throws NoSuitableCandidateException {
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
