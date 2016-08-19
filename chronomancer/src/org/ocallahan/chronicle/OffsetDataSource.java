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

/**
 * 
 */
package org.ocallahan.chronicle;

public class OffsetDataSource implements IDataSource {
	private IDataSource source;
	private long offset;
	
	public static IDataSource make(IDataSource source, long offset) {
		if (offset == 0)
			return source;
		return new OffsetDataSource(source, offset);
	}
	
	public OffsetDataSource(IDataSource source, long offset) {
		this.source = source;
		this.offset = offset;
	}
	
	public Session getSession() {
		return source.getSession();
	}
	
	public void read(long offset, int length, IDataSink sink) {
		source.read(offset + this.offset, length, sink);
	}

	public void findNextChangeTStamp(long start, long length, TStampReceiver receiver) {
		source.findNextChangeTStamp(offset + start, length, receiver);
	}

	public void findPreviousChangeTStamp(long start, long length, TStampReceiver receiver) {
		source.findPreviousChangeTStamp(offset + start, length, receiver);
	}

	public IDataSource getSourceAtTime(long tStamp) {
		return new OffsetDataSource(source.getSourceAtTime(tStamp), offset);
	}

	public long getTStamp() {
		return source.getTStamp();
	}
}
