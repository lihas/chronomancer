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

public class ScanEnterSPQuery extends ScanQuery {
    public ScanEnterSPQuery(Session session, long beginTStamp, long endTStamp,
            MemRange[] ranges, Termination termination, Listener listener) {
    	super(session, beginTStamp, endTStamp, "ENTER_SP", ranges, termination);
    	this.listener = listener;
	}

    @Override
    void handleResult(JSONObject object) throws JSONParserException {
    	String type = object.getString("type");
    	if (type != null && type.equals("normal")) {
    		long start = object.getLongRequired("start");
    		long end = object.getLongRequired("length") + start;
    		listener.notifyEnterSPResult(this,
    				object.getLongRequired("TStamp"), start, end);
    	} else {
    		super.handleResult(object);
    	}
    }

    @Override
    void handleDone(boolean complete) {
    	getListener().notifyDone(this, complete);
    }
        
	@Override
	protected ScanQuery.Listener getListener() {
		return listener;
	}

	public static interface Listener extends ScanQuery.Listener {
		public void notifyEnterSPResult(ScanEnterSPQuery q, long tStamp,
				long start, long end);
	}
	
	private Listener listener;
}
