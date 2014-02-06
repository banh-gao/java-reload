package com.github.reload.overlay;

import java.util.EnumSet;

/**
 * The identifier of an application defined in IANA Application-ID registry for
 * RELOAD
 * 
 * @author Daniel Zozin <zdenial@gmx.com>
 * 
 */
public enum ApplicationID {
	SIP_5060(5060), SIP_5061(5061);

	private final int id;

	private ApplicationID(int id) {
		this.id = id;
	}

	public static ApplicationID valueOf(int id) {
		for (ApplicationID v : EnumSet.allOf(ApplicationID.class))
			if (v.id == id)
				return v;
		return null;
	}

	/**
	 * @return the application id associated with this application
	 */
	public int getId() {
		return id;
	}
}
