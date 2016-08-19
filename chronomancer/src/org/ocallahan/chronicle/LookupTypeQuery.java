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

public class LookupTypeQuery extends Query {
    public LookupTypeQuery(Session session, String name, String containerPrefix,
    		                String namespacePrefix, String contextTypeKey,
    		                Listener listener) {
    	super(session, "lookupGlobalType");
    	builder.append("name", name);
    	if (containerPrefix != null) {
    		builder.append("containerPrefix", containerPrefix);
    	}
    	if (namespacePrefix != null) {
    		builder.append("namespacePrefix", namespacePrefix);
    	}
    	if (contextTypeKey != null) {
    		builder.append("typeKey", contextTypeKey);
    	}
    	this.listener = listener;
    }

    /**
     * Listener callbacks are run on the session's thread.
     */
    public interface Listener {
    	public void notifyDone(LookupTypeQuery q, boolean complete, Type.Promise type);
    };
    
    void handleDone(boolean complete) {
    	listener.notifyDone(this, complete, result);
    	listener = null;
    }
    
	@Override
	void handleResult(JSONObject r) throws JSONParserException {
		String typeKey = r.getString("typeKey");
		if (typeKey != null) {
			result = session.getTypeManager().getPromise(typeKey);
		}
	}
	
	private Type.Promise result;
	private Listener listener;
}
