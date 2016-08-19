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

import java.util.Arrays;

public class RegisterDataSource implements IDataSource {
	private Session session;
	private long tStamp;
	private String register;
	
	public RegisterDataSource(Session session, long tStamp, String register) {
		this.session = session;
		this.tStamp = tStamp;
		this.register = register;
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
		return new RegisterDataSource(session, newTStamp, register);
	}

	public long getTStamp() {
		return tStamp;
	}

	public void read(long offset, final int length, final IDataSink sink) {
		if (offset + length > Integer.MAX_VALUE) {
			VariablePiece.sendInvalidData(session, sink, length);
			return;
		}
		final int intOffset = (int)offset;
		ReadRegQuery q = new ReadRegQuery(session, tStamp,
				new String[] { register }, (intOffset + length)*8,
                new ReadRegQuery.Listener() {
			public void notifyDone(ReadRegQuery q, boolean complete,
					RegisterValues values) {
				if (!session.getArchitecture().isLittleEndian())
					throw new IllegalArgumentException("Only little-endian handled currently");
				RegisterValues.Value v = values.getValue(register);
				int vBytes = v.getBits()/8;
				byte[] result = new byte[length];
				boolean[] resultValid = new boolean[length];
				if (intOffset < vBytes) {
					int resultBytes = Math.min(length, vBytes - intOffset);
					Arrays.fill(resultValid, 0, resultBytes, true);
					if (vBytes <= 8) {
						long l = v.getLong();
						for (int i = 0; i < resultBytes; ++i) {
							result[i] = (byte)(l >> ((i + intOffset)*8));
						}
					} else {
						byte[] bytes = v.getBig().toByteArray();
						for (int i = 0; i < resultBytes; ++i) {
							result[i] = bytes[bytes.length - 1 - (i + intOffset)];
						}
					}
				}
				sink.receive(result, resultValid);
			}
		});
		q.send();
	}
}
