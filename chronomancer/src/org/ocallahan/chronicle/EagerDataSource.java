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

import java.util.ArrayList;

public class EagerDataSource implements IDataSource {
	private IDataSource source;
	private ArrayList<ReadRequest> readRequests;
	private byte[] data;
	private boolean[] valid;
    private int length;

	private static class ReadRequest {
		long offset;
		int length;
		IDataSink sink;
		public ReadRequest(long offset, int length, IDataSink sink) {
			this.offset = offset;
			this.length = length;
			this.sink = sink;
		}
	}
	
	private void gotData(byte[] data, boolean[] valid) {
		synchronized (this) {
			this.data = data;
			this.valid = valid;
		}
		if (readRequests != null) {
			for (ReadRequest r : readRequests) {
				doSinkReceive(r.offset, r.length, r.sink);
			}
			readRequests = null;
		}
	}
	
	public EagerDataSource(IDataSource source, int length) {
		this.source = source;
		this.length = length;
		
        source.read(0, length, new IDataSink() {
        	public void receive(byte[] data, boolean[] valid) {
        		gotData(data, valid);
        	}
        });	
	}
		
	public void findNextChangeTStamp(long start, long length, TStampReceiver receiver) {
		source.findNextChangeTStamp(start, length, receiver);
	}

	public void findPreviousChangeTStamp(long start, long length, TStampReceiver receiver) {
		source.findPreviousChangeTStamp(start, length, receiver);
	}

	public IDataSource getSourceAtTime(long tStamp) {
		return new EagerDataSource(source.getSourceAtTime(tStamp), length);
	}

	public long getTStamp() {
		return source.getTStamp();
	}

	// Called on the Session thread with 'data' and 'valid' set
	void doSinkReceive(long requestedOffset, int requestedLength, IDataSink sink) {
		byte[] result = new byte[requestedLength];
		boolean[] resultValid = new boolean[requestedLength];
		if (requestedOffset < length) {
			int intOffset = (int)requestedOffset;
			int len = Math.min(requestedLength, length - intOffset);
			System.arraycopy(data, intOffset, result, 0, len);
			System.arraycopy(valid, intOffset, resultValid, 0, len);
		}
        sink.receive(result, resultValid);
	}
	
	public synchronized void read(final long offset, final int length,
			final IDataSink sink) {
		if (data != null) {
			source.getSession().runOnThread(new Runnable() {
				public void run() {
					doSinkReceive(offset, length, sink);
				}
			});
			return;
		}
		
		if (readRequests == null) {
			readRequests = new ArrayList<ReadRequest>();
		}
		readRequests.add(new ReadRequest(offset, length, sink));
	}
	
	public Session getSession() {
		return source.getSession();
	}
}
