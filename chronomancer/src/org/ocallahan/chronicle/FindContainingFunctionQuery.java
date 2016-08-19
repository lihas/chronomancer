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

public class FindContainingFunctionQuery extends Query {
    public FindContainingFunctionQuery(Session session, long address, long tStamp,
                                  Listener listener) {
    	super(session, "findContainingFunction");
    	builder.append("address", address);
    	builder.append("TStamp", tStamp);
    	this.listener = listener;
    }

    /**
     * Listener callbacks are run on the session's thread.
     */
    public interface Listener {
    	public void notifyDone(FindContainingFunctionQuery q, boolean complete, Function function);
    };
    
    void handleDone(boolean complete) {
    	listener.notifyDone(this, complete, result);
    	listener = null;
    }
    
	@Override
	void handleResult(JSONObject r) throws JSONParserException {
		if (r.hasValue("entryPoint")) {
			result = new Function(session, r);
		}
	}
	
	private Listener listener;
	private Function result;
}
