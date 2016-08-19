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

public class Variable {
    private CompilationUnit compilationUnit;
    private Identifier identifier;
    private Type.Promise type;
    private String valKey;
        
    public CompilationUnit getCompilationUnit() {
		return compilationUnit;
	}

	public Identifier getIdentifier() {
		return identifier;
	}

	public Type.Promise getType() {
		return type;
	}

	public String getValKey() {
		return valKey;
	}

	public Variable(Session session, JSONObject obj) throws JSONParserException {
    	compilationUnit = new CompilationUnit(obj);
    	identifier = Identifier.parse(obj);
    	String typeKey = obj.getString("typeKey");
    	if (typeKey != null) {
    	    type = session.getTypeManager().getPromise(typeKey);
    	}
    	valKey = obj.getStringRequired("valKey");
    }
	
	public String toString() {
		return identifier.toString();
	}
}
