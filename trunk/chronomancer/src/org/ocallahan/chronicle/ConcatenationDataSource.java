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

public class ConcatenationDataSource implements IDataSource {
	public static class Element {
		IDataSource source;
		long sourceStartBit;
		long destStartBit;
		long numBits;

		public Element(IDataSource source, long sourceStartBit,
				long destStartBit, long numBits) {
			this.source = source;
			this.sourceStartBit = sourceStartBit;
			this.destStartBit = destStartBit;
			this.numBits = numBits;
		}
	}
	
    private Element[] elements;
    
    public ConcatenationDataSource(Element[] elements) {
    	if (elements.length < 1)
    		throw new IllegalArgumentException("Empty elements list!");
    	for (int i = 1; i < elements.length; ++i) {
    		if (elements[i].source.getTStamp() != elements[0].source.getTStamp())
    			throw new IllegalArgumentException("Mismatched timestamps!");
    		if (elements[i].source.getSession() != elements[0].source.getSession())
    			throw new IllegalArgumentException("Mismatched sessions!");
    	}
    	this.elements = elements;
    }
    
	public void findNextChangeTStamp(long start, long length, TStampReceiver receiver) {
		// TODO Auto-generated method stub

	}

	public void findPreviousChangeTStamp(long start, long length, TStampReceiver receiver) {
		// TODO Auto-generated method stub

	}

	public Session getSession() {
		return elements[0].source.getSession();
	}

	public IDataSource getSourceAtTime(long tStamp) {
		Element[] newElements = new Element[elements.length];
		for (int i = 0; i < elements.length; ++i) {
			newElements[i] = new Element(
					elements[i].source.getSourceAtTime(tStamp),
					elements[i].sourceStartBit,
					elements[i].destStartBit, elements[i].numBits);
		}
		return new ConcatenationDataSource(newElements);
	}

	public long getTStamp() {
		return elements[0].source.getTStamp();
	}
	
	private static int checkedToInt(long l) {
		int i = (int)l;
		if (i != l)
			throw new IllegalArgumentException("Integer " + l + " out of bounds");
		return i;
	}
	
	private static class Accumulator {
		private IDataSink sink;
		private byte[] result;
		private byte[] resultValidMask;
		private int outstandingLoads = 1;
		
		Accumulator(IDataSink sink, int length) {
			this.sink = sink;
			result = new byte[length];
			resultValidMask = new byte[length];
		}

		IDataSink addRead(final long destStartBit, final long destNumBits) {
			synchronized (this) {
				++outstandingLoads;
			}
			return new IDataSink() {
				public void receive(byte[] data, boolean[] valid) {
					synchronized (this) {
						if (destStartBit%8 == 0 && destNumBits%8 == 0) {
							// this should be OK because in the caller
							// we know bits <= 8*length
							int dest = checkedToInt(destStartBit/8);
							// this should be OK because in the caller
							// we know bits <= 8*length
							int len = checkedToInt(destNumBits/8);
							for (int i = 0; i < len; ++i) {
								if (valid[i]) {
									result[i + dest] = data[i];
									resultValidMask[i + dest] = (byte)0xFF;
								}
							}
						} else {
							for (long i = 0; i < destNumBits; ++i) {
								// this should be OK because in the caller
								// we know bits <= 8*length
								int srcOffset = checkedToInt(i/8);
								if (!valid[srcOffset])
									continue;
								int srcVal = (data[srcOffset] >> (i%8)) & 1;
								long dest = destStartBit + i;
								int destOffset = checkedToInt(dest/8);
								int destBit = (int)(dest%8);
								int destBitInvMask = ~(1 << destBit);
								result[destOffset] = (byte)(
									(result[destOffset] & destBitInvMask)
								    | (srcVal << destBit));
								resultValidMask[destOffset] = (byte)(
									(resultValidMask[destOffset] & destBitInvMask)
								    | (1 << destBit));
							}
						}
					}
					doneLoad();
				}
			};
		}
		
		// Called from session thread
		private void doneLoad() {
			synchronized (this) {
				--outstandingLoads;
				if (outstandingLoads > 0)
					return;
			}
			boolean[] valid = new boolean[resultValidMask.length];
			for (int i = 0; i < valid.length; ++i) {
				valid[i] = resultValidMask[i] == (byte)0xFF;
			}
			sink.receive(result, valid);
		}
		
		// Called from any thread
		void done(Session session) {
			session.runOnThread(new Runnable() {
				public void run() {
					doneLoad();
				}
			});
		}
	}

	public void read(long offset, int length, IDataSink sink) {
		Accumulator acc = new Accumulator(sink, length);
		
		for (Element e : elements) {
			long destStartBit = Math.max(e.destStartBit - offset*8, 0);
			long destEndBit = Math.min(e.destStartBit + e.numBits - offset*8, length*8);
			if (destStartBit < destEndBit) {
				long sourceBit = destStartBit - e.destStartBit + e.sourceStartBit;
				long bits = destEndBit - destStartBit;
				int bytes = (int)((bits + 7)/8); // must succeed because it's <= length
				IDataSource source = ShiftDataSource.make(e.source, sourceBit);
				source.read(sourceBit/8, bytes, acc.addRead(destStartBit, bits));
			}
		}
		
		acc.done(getSession());
	}
}
