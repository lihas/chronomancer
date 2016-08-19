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

public class JSONObjectBuilder {
    public JSONObjectBuilder() {
    	this(new StringBuilder());
    }
    public JSONObjectBuilder(StringBuilder sb) {
    	this.sb = sb;
    	sb.append('{');
    }
    public void append(String key, String value) {
    	appendField(key);
    	appendQuoted(sb, value);
    }
    public void append(String key, int value) {
    	appendField(key);
    	sb.append(value);
    }
    public void append(String key, long value) {
    	appendField(key);
    	sb.append(value);
    }
    public void append(String key, boolean value) {
    	appendField(key);
    	sb.append(value);
    }
    public JSONArrayBuilder appendArray(String key) {
    	appendField(key);
    	return new JSONArrayBuilder(sb);
    }
    public JSONObjectBuilder appendObject(String key) {
    	appendField(key);
    	return new JSONObjectBuilder(sb);
    }
    public String makeString() {
    	sb.append('}');
    	String s = sb.toString();
    	sb = null;
    	return s;
    }
    public void finish() {
    	sb.append('}');
    	sb = null;
    }

    private void appendField(String key) {
    	if (firstField) {
    		firstField = false;
    	} else {
    		sb.append(',');
    	}
    	appendQuoted(sb, key);
    	sb.append(':');    	
    }
	static void appendQuoted(StringBuilder sb, String s) {
		sb.append('"');
        for (int i = 0; i < s.length(); ++i) {
        	char ch = s.charAt(i);
        	switch (ch) {
        	case '\n':
        	case '\t':
        	case '\b':
        	case '\r':
        	case '\f':
        	case '\\':
        	case '"':
        		sb.append('\\');
        		sb.append(ch);
        		break;
        	default:
        		sb.append(ch);
        	    break;
        	}
        }
        sb.append('"');
	}

	private StringBuilder sb = new StringBuilder();
	private boolean firstField = true;
}
