/* ***** BEGIN LICENSE BLOCK *****
The contents of this file are subject to the Mozilla Public License
Version 1.1 (the "License"); you may not use this file except in
compliance with the License. You may obtain a copy of the License at
http://www.mozilla.org/MPL/

Software distributed under the License is distributed on an "AS IS"
basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
License for the specific language governing rights and limitations
under the License.

The Original Code is mozilla.org Chronicle code.

The Initial Developer of the Original Code is Mozilla Foundation.
Portions created by Mozilla Foundation are Copyright (C) 2007
Mozilla Foundation. All Rights Reserved.

Contributor(s): robert@ocallahan.org
*/

/**
 * 
 */
package org.ocallahan.chronomancer;

public class AbstractLocation {
	public static class Home {
		private long address;
		private long beginTStamp;
		private long endTStamp;
		
		public Home(long address, long beginTStamp, long endTStamp) {
			this.address = address;
			this.beginTStamp = beginTStamp;
			this.endTStamp = endTStamp;
		}
	
		public long getAddress() {
			return address;
		}
		public long getBeginTStamp() {
			return beginTStamp;
		}
		public long getEndTStamp() {
			return endTStamp;
		}
	}

	AbstractLocation(long validLength, Home[] homes) {
		this.validLength = validLength;
		this.homes = homes;
	}

	public long getValidLength() {
		return validLength;
	}
	
	public Home[] getHomes() {
		return homes;
	}
	
	public Home getHomeFor(long tStamp) {
		for (Home h : homes) {
			if (h.beginTStamp <= tStamp && tStamp < h.endTStamp)
				return h;
		}
		return null;
	}
	
	public String toString() {
		return homes[0].address + "@" + homes[0].beginTStamp;
	}
	
	private long validLength;
	private Home[] homes;
}
