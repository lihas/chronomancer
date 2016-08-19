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

package org.ocallahan.chronicle;

public class ScanMMapQuery extends ScanQuery {
    public ScanMMapQuery(Session session, long beginTStamp, long endTStamp,
            MemRange[] ranges, Termination termination, Listener listener) {
    	super(session, beginTStamp, endTStamp, "MEM_MAP", ranges, termination);
    	this.listener = listener;
	}
    
	@Override
	protected ScanQuery.Listener getListener() {
		return listener;
	}
	
	public static interface Listener extends ScanQuery.Listener {
	}
	
	private Listener listener;
}
