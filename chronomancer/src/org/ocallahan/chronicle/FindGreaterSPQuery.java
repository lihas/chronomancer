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

public class FindGreaterSPQuery extends Query {
    public FindGreaterSPQuery(Session session, long beginTStamp, long endTStamp,
    		long threshold, int thread, Listener listener) {
    	super(session, "findSPGreaterThan");
    	builder.append("beginTStamp", beginTStamp);
    	builder.append("endTStamp", endTStamp);
    	builder.append("threshold", threshold);
    	builder.append("thread", thread);
    	this.listener = listener;
    }
	
	@Override
	void handleDone(boolean complete) {
		listener.notifyDone(this, complete, result);
	}

	@Override
	void handleResult(JSONObject object) throws JSONParserException {
		Long tStamp = object.getLong("TStamp");
		if (tStamp != null) {
		    result = tStamp;
		}
	}
	
    public interface Listener {
    	// tStamp, if non-null, is the timestamp of the instruction that
    	// causes SP to become greater than the threshold; i.e., at time
    	// tStamp, SP is <= threshold, and at time tStamp + 1, SP > threshold.
    	public void notifyDone(FindGreaterSPQuery q, boolean complete,
    			Long tStamp);
    }
    
    private Listener listener;
    private Long result;
}
