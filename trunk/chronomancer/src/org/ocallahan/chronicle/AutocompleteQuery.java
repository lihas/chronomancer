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

public class AutocompleteQuery extends Query {
    private static HashMap<Kind,String> kindNames =
    	new HashMap<Kind, String>();
    private static HashMap<String,Kind> nameKinds =
    	new HashMap<String, Kind>();
    private static void registerKindName(Kind kind, String name) {
    	kindNames.put(kind, name);
    	nameKinds.put(name, kind);
    }
    static {
    	registerKindName(Kind.FUNCTION, "function");
    	registerKindName(Kind.TYPE, "type");
    	registerKindName(Kind.VARIABLE, "variable");
    }
    
    public AutocompleteQuery(Session session, String prefix, boolean caseSensitive,
                             int from, int desiredCount, Kind[] kinds,
                             Listener listener) {
    	super(session, "autocomplete");
    	builder.append("prefix", prefix);
    	builder.append("caseSensitive", caseSensitive);
    	builder.append("from", from);
    	builder.append("desiredCount", desiredCount);
    	if (kinds != null) {
    		JSONArrayBuilder a = builder.appendArray("kinds");
    		for (Kind k : kinds) {
    			a.append(kindNames.get(k));
    		}
    		a.finish();
    	}
    	this.listener = listener;
    }

    /**
     * Listener callbacks are run on the session's thread.
     */
    public interface Listener {
    	public void notifyAutocompleteResult(AutocompleteQuery q, String name, Kind kind);
    	public void notifyDone(AutocompleteQuery q, boolean complete);
    };
    
    public static enum Kind { VARIABLE, FUNCTION, TYPE }

    void handleDone(boolean complete) {
    	listener.notifyDone(this, complete);
    	listener = null;
    }
    
	@Override
	void handleResult(JSONObject r) throws JSONParserException {
		String kindName = r.getString("kind");
		if (kindName != null) {
			Kind kind = nameKinds.get(kindName);
			if (kind == null)
				throw new JSONParserException("Kind not understood: " + r.getStringRequired("kind"));
			listener.notifyAutocompleteResult(this, r.getStringRequired("name"), kind);
		}
	}
	
	private Listener listener;
}
