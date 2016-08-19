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

public class MemoryDataSource implements IDataSource {
	private Session session;
	private long tStamp;
	private long start;
	
    public MemoryDataSource(Session session, long tStamp, long start) {
    	this.session = session;
    	this.tStamp = tStamp;
    	this.start = start;
    }
    
    public Session getSession() {
    	return session;
    }

    public long getTStamp() {
    	return tStamp;
    }

	public void findNextChangeTStamp(long offset, long length, final TStampReceiver receiver) {
		ScanWriteQuery q = new ScanWriteQuery(session, tStamp + 1, session.getEndTStamp(),
				new MemRange[] { new MemRange(start + offset, start + offset + length) },
				ScanQuery.Termination.FIRST,
				new ScanWriteQuery.Listener() {
			private void accumulate(long tStamp, long start, long end) {
				if (lastTStamp < 0 || tStamp <= lastTStamp) {
					if (tStamp < lastTStamp || range == null) {
						range = new MemRange(start, end);
					} else {
						range = range.union(start, end);
					}
					lastTStamp = tStamp;
				}
			}
			public void notifyWriteResult(ScanWriteQuery q, long tStamp,
					long start, long end, byte[] data) {
				accumulate(tStamp, start, end);
			}
			public void notifyMMapResult(ScanQuery q, long tStamp, long start, long end, MMapInfo info) {
				accumulate(tStamp, start, end);
			}
			public void notifyDone(ScanQuery q, boolean complete) {
				if (lastTStamp < 0) {
					receiver.receiveEndOfScope(session.getEndTStamp());
				} else {
					receiver.receiveChange(lastTStamp, range.getStart(),
							range.getLength());
				}
			}
			private long lastTStamp = -1;
			private MemRange range;
		});
		q.send();
	}

	public void findPreviousChangeTStamp(long offset, long length, final TStampReceiver receiver) {
		ScanWriteQuery q = new ScanWriteQuery(session, 0, tStamp,
				new MemRange[] { new MemRange(start + offset, start + offset + length) },
				ScanQuery.Termination.LAST,
				new ScanWriteQuery.Listener() {
			private void accumulate(long tStamp, long start, long end) {
				if (lastTStamp < 0 || tStamp >= lastTStamp) {
					if (tStamp > lastTStamp || range == null) {
						range = new MemRange(start, end);
					} else {
						range = range.union(start, end);
					}
					lastTStamp = tStamp;
				}				
			}
			public void notifyWriteResult(ScanWriteQuery q, long tStamp,
					long start, long end, byte[] data) {
				accumulate(tStamp, start, end);
			}
			public void notifyMMapResult(ScanQuery q, long tStamp, long start, long end, MMapInfo info) {
				accumulate(tStamp, start, end);
			}
			public void notifyDone(ScanQuery q, boolean complete) {
				if (lastTStamp < 0) {
					receiver.receiveEndOfScope(0);
				} else {
					receiver.receiveChange(lastTStamp, range.getStart(), range.getLength());
				}
			}
			private long lastTStamp = -1;
			private MemRange range;
		});
		q.send();		
	}

	public IDataSource getSourceAtTime(long newTStamp) {
		return new MemoryDataSource(session, newTStamp, start);
	}

	public void read(long offset, int length, final IDataSink sink) {
		ReadMemQuery q = new ReadMemQuery(session, tStamp,
				new MemRange[] { new MemRange(start + offset, start + offset + length) },
				new ReadMemQuery.Listener() {
			public void notifyDone(ReadMemQuery q, boolean complete,
					byte[] data, boolean[] valid) {
				sink.receive(data, valid);
			}
		});
		q.send();		
	}
}
