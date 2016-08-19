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

public abstract class ScanQuery extends Query {
    protected ScanQuery(Session session, long beginTStamp, long endTStamp,
    		            String map, MemRange[] ranges, Termination termination) {
    	super(session, "scan");
    	builder.append("beginTStamp", beginTStamp);
    	builder.append("endTStamp", endTStamp);
    	builder.append("map", map);
    	MemRange.writeTo(ranges, builder.appendArray("ranges"));
    	if (termination != Termination.NONE) {
    	    builder.append("termination", terminationNames.get(termination));
    	}
    }
    
    private static HashMap<Termination,String> terminationNames =
    	new HashMap<Termination,String>();
    static {
    	terminationNames.put(Termination.FIRST, "findFirst");
    	terminationNames.put(Termination.LAST, "findLast");
    	terminationNames.put(Termination.FIRST_COVER, "findFirstCover");
    	terminationNames.put(Termination.LAST_COVER, "findLastCover");
    }
    
    public static enum Termination { NONE, FIRST, LAST, FIRST_COVER, LAST_COVER };
    
    public static interface Listener {
    	public void notifyMMapResult(ScanQuery q, long tStamp,
    			long start, long end, MMapInfo info);
    	public void notifyDone(ScanQuery q, boolean complete);
    }
    
    @Override
    void handleResult(JSONObject object) throws JSONParserException {
    	String type = object.getString("type");
    	if (type != null && type.equals("mmap")) {
    		long start = object.getLongRequired("start");
    		long end = object.getLongRequired("length") + start;
    		getListener().notifyMMapResult(this,
    				object.getLongRequired("TStamp"),
    				start, end, new MMapInfo(object));
    	}
    }

    @Override
    void handleDone(boolean complete) {
    	getListener().notifyDone(this, complete);
    }
    
    protected abstract Listener getListener();
}
