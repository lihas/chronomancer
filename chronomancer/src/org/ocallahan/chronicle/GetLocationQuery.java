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

public class GetLocationQuery extends Query {
	public GetLocationQuery(Session session, long tStamp, Variable var,
			Type.Promise type, Listener listener) {
		super(session, "getLocation");
		builder.append("TStamp", tStamp);
		builder.append("valKey", var.getValKey());
		builder.append("typeKey", var.getType().getTypeKey());
		this.listener = listener;
	}
	
	public static interface Listener {
		public void notifyDone(GetLocationQuery q, boolean complete,
				VariablePiece[] vars, MemRange[] validForInstructionsInRanges);
	}
	
	@Override
	void handleDone(boolean complete) {
		listener.notifyDone(this, complete,
				results.toArray(new VariablePiece[results.size()]),
				validForInstructionsInRanges);
	}

	@Override
	void handleResult(JSONObject object) throws JSONParserException {
		if (object.hasValue("valueBitStart")) {
			results.add(VariablePiece.parse(object));
		}
		Object[] validFor = object.getArray("validForInstructions");
		if (validFor != null) {
			validForInstructionsInRanges = MemRange.parseRanges(validFor);
		}
	}

	private Listener listener;
	private ArrayList<VariablePiece> results = new ArrayList<VariablePiece>();
	private MemRange[] validForInstructionsInRanges;
}
