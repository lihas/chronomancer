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
 * Line numbers and column numbers start at 1.
 */
public class SourceCoordinate {
    private String fileName;
    private int startLine;
    private int startColumn;
    private int endLine;
    private int endColumn;
    
	public SourceCoordinate(Session session, JSONObject obj) throws JSONParserException {
    	fileName = obj.getString("filename");
    	startLine = obj.getIntOptional("startLine", 0);
    	startColumn = obj.getIntOptional("startColumn", 0);
    	endLine = obj.getIntOptional("endLine", startLine);
    	endColumn = obj.getIntOptional("endColumn", startColumn);
    }
	
	/**
	 * Create "unknown" source coordinate. fileName is null and the other fields
	 * are zero.
	 */
	private SourceCoordinate() {
	}
	
	public static final SourceCoordinate UNKNOWN = new SourceCoordinate();

	public String getFileName() {
		return fileName;
	}
	public int getStartLine() {
		return startLine;
	}
	public int getStartColumn() {
		return startColumn;
	}
	public int getEndLine() {
		return endLine;
	}
	public int getEndColumn() {
		return endColumn;
	}

	public String toString() {
		if (fileName == null)
			return "<unknown>";
		return fileName + ":" + startLine;
	}
}
