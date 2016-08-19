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

import java.util.ArrayList;

public class VariableDataSource implements IDataSource {
	private Session session;
	private long invocationTStamp;
	private long currentTStamp;
	private Variable var;
	private ArrayList<ReadRequest> requests;
	private VariablePiece[] pieces;
	// private MemRange[] validForInstructionsInRanges;
	
	public VariableDataSource(Session session,
			long currentTStamp, Variable var) {
		this(session, currentTStamp, currentTStamp, var);
	}
	
	public VariableDataSource(Session session,
			long invocationTStamp, long currentTStamp, Variable var) {
		this.session = session;
		this.invocationTStamp = invocationTStamp;
		this.currentTStamp = currentTStamp;
		this.var = var;
		
		if (invocationTStamp != currentTStamp) {
			// TODO figure out what to use here
		}
		
		GetLocationQuery q = new GetLocationQuery(session, invocationTStamp,
				var, var.getType(), new GetLocationQuery.Listener() {
			public void notifyDone(GetLocationQuery q, boolean complete,
					VariablePiece[] pieces, MemRange[] validForInstructionsInRanges) {
				setPieces(pieces, validForInstructionsInRanges);
			}
		});
		q.send();
	}
	
	public void findNextChangeTStamp(long start, long length, TStampReceiver receiver) {
		// TODO Auto-generated method stub
		throw new Error("Unimplemented");
	}

	public void findPreviousChangeTStamp(long start, long length, TStampReceiver receiver) {
		// TODO Auto-generated method stub
		throw new Error("Unimplemented");
	}

	public Session getSession() {
		return session;
	}

	public IDataSource getSourceAtTime(long newTStamp) {
		return new VariableDataSource(session, invocationTStamp, newTStamp, var);
	}

	public long getTStamp() {
		return currentTStamp;
	}

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

	public void read(final long offset, final int length,
			final IDataSink sink) {
		if (length > 1000000)
			throw new IllegalArgumentException("Oversized request!");
		synchronized (this) {
			if (pieces == null) {
				if (requests == null) {
					requests = new ArrayList<ReadRequest>();
				}
				requests.add(new ReadRequest(offset, length, sink));
				return;
			}
		}
		doRead(offset, length, sink);
	}
	
	private void setPieces(VariablePiece[] pieces, MemRange[] validForInstructionsInRanges) {
		synchronized (this) {
			this.pieces = pieces;
			// this.validForInstructionsInRanges = validForInstructionsInRanges;
		}
		if (requests != null) {
			for (ReadRequest r : requests) {
				doRead(r.offset, r.length, r.sink);
			}
		}
	}
	
	private ConcatenationDataSource makeConcatenatedSource(long length) {
		ArrayList<ConcatenationDataSource.Element> elements =
			new ArrayList<ConcatenationDataSource.Element>();
		for (VariablePiece p : pieces) {
			long bits = p.getBitLength();
			if (bits == 0) {
				bits = Math.max(0, length*8 - p.getBitStart());
			}
			if (bits > 0) {
				elements.add(new ConcatenationDataSource.Element(
						p.getDataSource(session, currentTStamp),
						0, p.getBitStart(), bits));
			}
		}
		ConcatenationDataSource.Element[] elementArray =
			new ConcatenationDataSource.Element[elements.size()];
		return new ConcatenationDataSource(elements.toArray(elementArray));
	}
	
	private void doRead(long offset, int length, IDataSink sink) {
		makeConcatenatedSource(offset + length).read(offset, length, sink);
	}
}
