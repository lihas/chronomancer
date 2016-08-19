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

public class ShiftDataSource implements IDataSource {
    private IDataSource source;
    private int shift;
    
    public static IDataSource make(IDataSource source, long shift) {
    	if (shift < 0)
    		throw new IllegalArgumentException("Negative shift: " + shift);
    	IDataSource offsetSource = OffsetDataSource.make(source, shift/8);
    	int intShift = (int)(shift%8);
    	if (intShift == 0)
    		return offsetSource;
    	return new ShiftDataSource(offsetSource, intShift);
    }
    
	public ShiftDataSource(IDataSource source, int shift) {
		if (shift >= 8)
			throw new IllegalArgumentException("shift too many bits: " + shift);
		if (shift <= 0)
			throw new IllegalArgumentException("shift too few bits: " + shift);
		this.source = source;
		this.shift = shift;
	}

	public void findNextChangeTStamp(long start, long length, TStampReceiver receiver) {
		source.findNextChangeTStamp(start, length + 1, receiver);
	}
	
	public void findPreviousChangeTStamp(long start, long length, TStampReceiver receiver) {
		source.findPreviousChangeTStamp(start, length + 1, receiver);
	}

	public Session getSession() {
		return source.getSession();
	}

	public IDataSource getSourceAtTime(long tStamp) {
		return new ShiftDataSource(source.getSourceAtTime(tStamp), shift);
	}

	public long getTStamp() {
		return source.getTStamp();
	}

	public void read(long offset, final int length, final IDataSink sink) {
		source.read(offset, length + 1, new IDataSink() {
			public void receive(byte[] data, boolean[] valid) {
				byte[] result = new byte[length];
				boolean[] resultValid = new boolean[length];
				for (int i = 0; i < length; ++i) {
					resultValid[i] = valid[i] && valid[i + 1];
					result[i] = (byte)((data[i] >> shift) |
					    (data[i + 1] & ((1 << shift) - 1)));
				}
				sink.receive(result, resultValid);
			}
		});
	}
}
