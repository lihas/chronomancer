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

import java.util.ArrayList;

public class GetVariablesQuery extends Query {
	public static enum Kind { LOCALS, PARAMETERS }
	
	public GetVariablesQuery(Session session, long tStamp, Kind kind,
			Listener listener) {
		super(session, kind == Kind.LOCALS ? "getLocals" : "getParameters");
		builder.append("TStamp", tStamp);
		this.listener = listener;
	}

	public static interface Listener {
		public void notifyDone(GetVariablesQuery q, boolean complete,
				Variable[] vars);
	}

	@Override
	void handleDone(boolean complete) {
		listener.notifyDone(this, complete,
				results.toArray(new Variable[results.size()]));
	}

	@Override
	void handleResult(JSONObject object) throws JSONParserException {
		if (object.hasValue("valKey")) {
		    results.add(new Variable(session, object));
		}
	}

	private Listener listener;
	private ArrayList<Variable> results = new ArrayList<Variable>();
}
