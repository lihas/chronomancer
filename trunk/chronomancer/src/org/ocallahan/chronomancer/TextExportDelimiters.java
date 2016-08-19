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

package org.ocallahan.chronomancer;

public class TextExportDelimiters {
	public TextExportDelimiters(String name, String header, String trailer, String separator) {
		this.name = name;
		this.header = header;
		this.trailer = trailer;
		this.separator = separator;
	}
	
	private String name;
	private String header;
	private String trailer;
	private String separator;

	public String getName() {
		return name;
	}
	public String getHeader() {
		return header;
	}
	public String getTrailer() {
		return trailer;
	}
	public String getSeparator() {
		return separator;
	}
}
