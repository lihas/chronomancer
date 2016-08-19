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

public class Function {
    private CompilationUnit compilationUnit;
    private Identifier identifier;
    private long startTStamp;
    private long endTStamp;
    private long entryPoint;
    private Long prologueEnd;
    private MemRange[] ranges;
    private Type.Promise type;
    
    public CompilationUnit getCompilationUnit() {
		return compilationUnit;
	}

	public long getEndTStamp() {
		return endTStamp;
	}

	public long getEntryPoint() {
		return entryPoint;
	} 

	public Identifier getIdentifier() {
		return identifier;
	}

	public Long getPrologueEnd() {
		return prologueEnd;
	}

	public MemRange[] getRanges() {
		return ranges;
	}

	public long getStartTStamp() {
		return startTStamp;
	}

	public Type.Promise getType() {
		return type;
	}

	public Function(Session session, JSONObject obj) throws JSONParserException {
    	compilationUnit = new CompilationUnit(obj);
    	identifier = Identifier.parse(obj);
    	startTStamp = obj.getLongRequired("beginTStamp");
    	endTStamp = obj.getLongRequired("endTStamp");
    	entryPoint = obj.getLongRequired("entryPoint");
    	prologueEnd = obj.getLong("prologueEnd");
    	ranges = MemRange.parseRanges(obj.getArray("ranges"));
    	String typeKey = obj.getString("typeKey");
    	if (typeKey != null) {
    	    type = session.getTypeManager().getPromise(typeKey);
    	}
    }
	
	public Function(Session session, long address) {
		// TODO startTStamp and endTStamp can be tightened up using memory
		// map history
		startTStamp = 0;
		endTStamp = session.getEndTStamp();
		entryPoint = address;
	}
	
	public String toString() {
		if (identifier == null) {
			 return "0x" + Long.toHexString(entryPoint);
		}
		return identifier.toString();
	}
}
