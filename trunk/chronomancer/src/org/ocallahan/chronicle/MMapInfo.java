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

public class MMapInfo {
	private String fileName;
	private Long fileOffset;
	private boolean isMapped;
	private boolean read;
	private boolean write;
	private boolean execute;

	public boolean isExecute() {
		return execute;
	}

	public String getFileName() {
		return fileName;
	}

	public Long getFileOffset() {
		return fileOffset;
	}

	public boolean isMapped() {
		return isMapped;
	}

	public boolean isRead() {
		return read;
	}

	public boolean isWrite() {
		return write;
	}

	public MMapInfo(JSONObject object) throws JSONParserException {
    	fileName = object.getString("filename");
    	fileOffset = object.getLong("offset");
    	isMapped = object.getBooleanRequired("mapped");
    	read = object.getBooleanRequired("read");
    	write = object.getBooleanRequired("write");
    	execute = object.getBooleanRequired("execute");
    }
}
