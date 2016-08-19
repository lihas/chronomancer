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

public class InfoQuery extends Query {
    public InfoQuery(Session session, Listener listener) {
	    super(session, "info");
	    this.listener = listener;
    }

    public interface Listener {
    	public void notifyDone(InfoQuery q, boolean complete,
    			long endTStamp, Architecture architecture);
    }

    void handleDone(boolean complete) {
    	listener.notifyDone(this, complete, endTStamp, architecture);
    	listener = null;
    }
    
    private static HashMap<String,Architecture> architectures =
    	new HashMap<String,Architecture>();
    static {
    	architectures.put("x86", Architecture.getX86());
    	architectures.put("amd64", Architecture.getAMD64());
    }
    
	@Override
	void handleResult(JSONObject r) throws JSONParserException {
		String archName = r.getString("arch");
		if (archName != null) {
			architecture = architectures.get(archName);
			if (architecture == null)
				throw new JSONParserException("Unknown architecture '" + archName + "'");
			endTStamp = r.getLongRequired("endTStamp");
		}
	}
	
	private long endTStamp;
	private Architecture architecture;
	private Listener listener;
}
