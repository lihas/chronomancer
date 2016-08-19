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

public class ScanWriteQuery extends ScanQuery {
    public ScanWriteQuery(Session session, long beginTStamp, long endTStamp,
            MemRange[] ranges, Termination termination, Listener listener) {
    	super(session, beginTStamp, endTStamp, "MEM_WRITE", ranges, termination);
    	this.listener = listener;
	}

    public static byte[] parseBytes(String s) throws JSONParserException {
    	int len = s.length();
    	if ((len & 1) != 0)
    		return parseBytes("0" + s);
    	byte[] result = new byte[len/2];
    	for (int i = 0; i < result.length; ++i) {
    		result[i] = (byte)
    			(RegisterValues.parseHexChar(s.charAt(2*i))*16 +
    			 RegisterValues.parseHexChar(s.charAt(2*i + 1)));
    	}
    	return result;
    }
    
    @Override
    void handleResult(JSONObject object) throws JSONParserException {
    	String type = object.getString("type");
    	if (type != null && type.equals("normal")) {
    		long start = object.getLongRequired("start");
    		long end = object.getLongRequired("length") + start;
    		byte[] bytes = parseBytes(object.getStringRequired("bytes"));
    		listener.notifyWriteResult(this,
    				object.getLongRequired("TStamp"), start, end, bytes);
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
		public void notifyWriteResult(ScanWriteQuery q, long tStamp,
				long start, long end, byte[] data);
	}
	
	private Listener listener;
}
