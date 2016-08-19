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

/**
 * An object representing a query.
 * @author roc
 *
 */
public abstract class Query {
    protected Query(Session session, String cmd) {
    	this.session = session;
		id = session.allocateID();
		builder.append("cmd", cmd);
		builder.append("id", id);
	}

    abstract void handleDone(boolean complete);
	abstract void handleResult(JSONObject object) throws JSONParserException;

	/**
	 * Request cancellation of the query. This won't take effect until
	 * query confirmation is received from the agent.
	 */
	public void cancel() {
		session.cancel(id);
	}
	
	public int getID() { return id; }
	
    public void send() {
    	session.sendQuery(this, builder);
    	builder = null;
    }

   	private   int               id;
	protected Session           session;
	protected JSONObjectBuilder builder = new JSONObjectBuilder();
}
