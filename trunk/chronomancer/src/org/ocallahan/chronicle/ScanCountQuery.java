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

public class ScanCountQuery extends Query {
    public ScanCountQuery(Session session, long beginTStamp, long endTStamp,
    		              String map, long address, Listener listener) {
    	super(session, "scanCount");
    	builder.append("beginTStamp", beginTStamp);
    	builder.append("endTStamp", endTStamp);
    	builder.append("map", map);
    	builder.append("address", address);
    	this.listener = listener;
    }
    
    public static interface Listener {
    	public void notifyDone(ScanCountQuery q, boolean complete, long count);
    }
    
    @Override
    void handleResult(JSONObject object) throws JSONParserException {
    	Long count = object.getLong("count");
    	if (count != null) {
    		this.count = count;
    	}
    }

    @Override
    void handleDone(boolean complete) {
    	listener.notifyDone(this, complete, count);
    }
    
    private Listener listener;
    private long count;
}
