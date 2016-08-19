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

import org.ocallahan.chronicle.IDataSink;
import org.ocallahan.chronicle.IDataSource;
import org.ocallahan.chronicle.MemoryDataSource;
import org.ocallahan.chronicle.Session;
import org.ocallahan.chronicle.VariablePiece;

public class AbstractLocationDataSource implements IDataSource {
	private Session session;
	private AbstractLocation location;
	private long tStamp;
	
	public AbstractLocationDataSource(Session session,
			AbstractLocation location, long tStamp) {
		this.session = session;
		this.location = location;
		this.tStamp = tStamp;
	}

	public void findNextChangeTStamp(long start, long length, TStampReceiver receiver) {
		// TODO Auto-generated method stub
		throw new Error("unimplemented");
	}

	public void findPreviousChangeTStamp(long start, long length, TStampReceiver receiver) {
		// TODO Auto-generated method stub
		throw new Error("unimplemented");
	}

	public Session getSession() {
		return session;
	}

	public IDataSource getSourceAtTime(long newTStamp) {
		return new AbstractLocationDataSource(session,location, newTStamp);
	}

	public long getTStamp() {
		return tStamp;
	}

	public void read(long offset, int length, IDataSink sink) {
		AbstractLocation.Home home = location.getHomeFor(tStamp);
		if (home == null) {
			VariablePiece.sendInvalidData(session, sink, length);
			return;
		}
		(new MemoryDataSource(session, tStamp, home.getAddress()))
		    .read(offset, length, sink);
	}
}
