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

import java.util.Comparator;

public class MemRange {
    private long start;
    private long end;

    public long getEnd() {
		return end;
	}
	public long getStart() {
		return start;
	}
	
	public static MemRange[] parseRanges(Object[] objs) throws JSONParserException {
		if (objs == null)
			return null;
		MemRange[] r = new MemRange[objs.length];
		for (int i = 0; i < objs.length; ++i) {
			Object o = objs[i];
			if (!(o instanceof JSONObject))
				throw new JSONParserException("Expected object, got " + o);
			r[i] = new MemRange((JSONObject)o);
		}
		return r;
	}
	
	public MemRange(long start, long end) {
		this.start = start;
		this.end = end;
	}
	public MemRange(JSONObject obj) throws JSONParserException {
	    start = obj.getLongRequired("start");
	    long length = obj.getLongRequired("length");
	    if (length < 0)
	    	throw new JSONParserException("Negative length: " + length);
	    end = start + length;
	}
	
	public void writeTo(JSONObjectBuilder builder) {
		builder.append("start", start);
		builder.append("length", end - start);
		builder.finish();
	}
	
	public static void writeTo(MemRange[] ranges, JSONArrayBuilder builder) {
		for (MemRange r : ranges) {
			r.writeTo(builder.appendObject());
		}
		builder.finish();
	}
	
	public MemRange intersect(long start, long end) {
		long s = Math.max(this.start, start);
		long e = Math.min(this.end, end);
		if (s < e)
			return new MemRange(s, e);
		return null;
	}
	
	public MemRange union(long start, long end) {
		long s = Math.min(this.start, start);
		long e = Math.max(this.end, end);
		return new MemRange(s, e);
	}
	
	private static Comparator<MemRange> startComparator = new Comparator<MemRange>() {
		public int compare(MemRange arg0, MemRange arg1) {
			if (arg0.start < arg1.start)
				return -1;
			if (arg1.start < arg0.start)
				return 1;
			return 0;
		}
	};
	
	public static Comparator<MemRange> getStartComparator() {
		return startComparator;
	}
	
	public long getLength() {
		return end - start;
	}
}
