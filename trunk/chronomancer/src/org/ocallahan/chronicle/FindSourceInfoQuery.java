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

import java.util.HashMap;
import java.util.Map;

public class FindSourceInfoQuery extends Query {
    public FindSourceInfoQuery(Session session, long tStamp, long[] addresses,
    		Listener listener) {
    	super(session, "findSourceInfo");
    	builder.append("TStamp", tStamp);
    	JSONArrayBuilder array = builder.appendArray("addresses");
    	for (long a : addresses) {
    		array.append(a);
    	}
    	array.finish();
    	this.listener = listener;
    }
	
	@Override
	void handleDone(boolean complete) {
		listener.notifyDone(this, complete, sources);
	}

	@Override
	void handleResult(JSONObject object) throws JSONParserException {
		Long addr = object.getLong("address");
		if (addr != null) {
			sources.put(addr, new SourceCoordinate(session, object));
		}
	}
	
    public interface Listener {
    	public void notifyDone(FindSourceInfoQuery q, boolean complete,
    			Map<Long,SourceCoordinate> sources);
    }
    
    private Listener listener;
    private HashMap<Long,SourceCoordinate> sources = new HashMap<Long,SourceCoordinate>();
}
