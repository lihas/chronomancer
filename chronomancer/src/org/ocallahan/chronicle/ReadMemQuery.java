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

import java.util.Arrays;

public class ReadMemQuery extends Query {
    public ReadMemQuery(Session session, long tStamp, MemRange[] ranges,
    		Listener listener) {
        super(session, "readMem");
        builder.append("TStamp", tStamp);
        MemRange.writeTo(ranges, builder.appendArray("ranges"));
        this.listener = listener;
        long totalLen = 0;
        for (MemRange r : ranges) {
        	totalLen += r.getEnd() - r.getStart();
        }
        if (totalLen > Integer.MAX_VALUE)
        	throw new IllegalArgumentException("Memory size overflow");
        this.data = new byte[(int)totalLen];
        this.valid = new boolean[(int)totalLen];
        this.ranges = ranges;
    }
    
    public static interface Listener {
    	public void notifyDone(ReadMemQuery q, boolean complete,
    			byte[] data, boolean[] valid);
    }
    
	@Override
	void handleDone(boolean complete) {
		listener.notifyDone(this, complete, data, valid);
	}

	@Override
	void handleResult(JSONObject object) throws JSONParserException {
		String s = object.getString("bytes");
		if (s != null) {
			long start = object.getLongRequired("start");
			long length = object.getLongRequired("length");
			byte[] data = VariablePiece.parseData(s);
			int offset = 0;
			for (MemRange r : ranges) {
				MemRange intersection = r.intersect(start, start + length);
				if (intersection != null) {
					int iStart = offset + (int)(intersection.getStart() - r.getStart());
					int iEnd = offset + (int)(intersection.getEnd() - r.getStart());
					Arrays.fill(valid, offset + iStart, offset + iEnd, true);
					System.arraycopy(data, (int)(intersection.getStart() - start),
							this.data, offset + iStart, iEnd - iStart);
				}
			}
		}
	}

	private Listener listener;
	private byte[] data;
	private boolean[] valid;
	private MemRange[] ranges;
}
