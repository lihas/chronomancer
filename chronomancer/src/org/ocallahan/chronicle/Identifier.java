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

public class Identifier {
	private String name;
	private String namespacePrefix;
	private String containerPrefix;
	private boolean synthetic;

	/**
	 * Returns an interned string.
	 * @return
	 */
	public String getContainerPrefix() {
		return containerPrefix;
	}
	/**
	 * Returns an interned string.
	 * @return
	 */
	public String getName() {
		return name;
	}
	/**
	 * Returns an interned string.
	 * @return
	 */
	public String getNamespacePrefix() {
		return namespacePrefix;
	}
	public boolean isSynthetic() {
		return synthetic;
	}

	public static String getInterned(String s) {
		if (s == null)
			return null;
		return s.intern();
	}
	
	private Identifier(JSONObject obj) throws JSONParserException {
		name = getInterned(obj.getString("name"));
		namespacePrefix = getInterned(obj.getString("namespacePrefix"));
		containerPrefix = getInterned(obj.getString("containerPrefix"));
		synthetic = obj.getBooleanOptional("synthetic", false);
	}
	
	private Identifier() {}
	
	public Identifier(String standardIdent) {
		name = standardIdent;
	}
	
	public static final Identifier EMPTY = new Identifier();
	
	public static Identifier parse(JSONObject obj) throws JSONParserException {
		Identifier ident = new Identifier(obj);
		if (ident.name == null && !ident.synthetic &&
	        ident.namespacePrefix == null && ident.containerPrefix == null)
			return EMPTY;
		return ident;
	}
	
	public String toString() {
	    String s = name == null ? "<anonymous>" : name;
	    if (containerPrefix != null) {
	    	s = containerPrefix + s;
	    }
	    if (namespacePrefix != null) {
	    	s = namespacePrefix + s;
	    }
	    return s;
	}
}
