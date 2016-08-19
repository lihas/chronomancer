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

public class JSONArrayBuilder {
    public JSONArrayBuilder() {
    	this.sb = new StringBuilder('[');
    }
    public JSONArrayBuilder(StringBuilder sb) {
    	this.sb = sb;
    	sb.append('[');
    }
    public void append(String value) {
    	appendSeparator();
    	JSONObjectBuilder.appendQuoted(sb, value);
    }
    public void append(int value) {
    	appendSeparator();
    	sb.append(value);
    }
    public void append(long value) {
    	appendSeparator();
    	sb.append(value);
    }
    public void append(boolean value) {
    	appendSeparator();
    	sb.append(value);
    }
    public JSONArrayBuilder appendArray() {
    	appendSeparator();
    	return new JSONArrayBuilder(sb);
    }
    public JSONObjectBuilder appendObject() {
    	appendSeparator();
    	return new JSONObjectBuilder(sb);
    }
    public String makeString() {
    	sb.append(']');
    	String s = sb.toString();
    	sb = null;
    	return s;
    }
    public void finish() {
    	sb.append(']');
    	sb = null;
    }

    private void appendSeparator() {
    	if (firstElem) {
    		firstElem = false;
    	} else {
    		sb.append(',');
    	}
    }

    private StringBuilder sb;
    private boolean firstElem = true;
}
