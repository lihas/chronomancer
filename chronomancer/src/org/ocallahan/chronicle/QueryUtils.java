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
import java.util.Collections;

/**
 * Utilities providing compound queries for useful operations such as finding
 * the start or end of the function invocation active at a given time.
 * @author roc
 *
 */
public class QueryUtils {
	public static interface StartReceiver {
		/**
		 * @param tStamp the tstamp of the call instruction
		 * @param endTStamp the tstamp after this invocation returned (or -1
		 * if it did not return)
		 * @param beforeCallSP the value of the stack pointer before the call instruction
		 * @param stackEnd the end of the stack
		 * @param thread the current thread
		 */
		public void receiveStart(long tStamp, long endTStamp,
				long beforeCallSP, long stackEnd, int thread);
		public void receiveNothing();
	}

	public static interface MemoryEndReceiver {
	    public void receiveEnd(long end);	
	}

	public static interface EndReceiver {
		public void receiveEnd(long tStamp);
		public void receiveNothing();
	}
	
	public static interface FunctionReceiver {
		/**
		 * If there is no debug info for a function at the requested address,
		 * we pass a dummy Function object with null Identifier
		 */
		public void receiveFunction(Function function);
	}

	private static final int DEFAULT_BUMP = 0x10000;

	public static void findStartOfCallWithRegs(final Session s, final long tStamp,
			long sp,
	        final long stackEnd, final int thread, final StartReceiver receiver) {
		ScanEnterSPQuery q = new ScanEnterSPQuery(s,
				0, tStamp, new MemRange[] { new MemRange(sp, stackEnd) },
				ScanQuery.Termination.LAST, new ScanEnterSPQuery.Listener() {
			public void notifyDone(ScanQuery q, boolean complete) {
				if (!found) {
					receiver.receiveNothing();
				}
			}
			public void notifyEnterSPResult(ScanEnterSPQuery q, final long enterTStamp,
					final long enterSP, long enterSPEnd) {
				found = true;
				final long beforeCallSP = enterSP + s.getArchitecture().getPointerSize();
				// This might be the call ... or it might be a previous call
				// by the same caller that returned before tStamp.
				findEndOfCallWithRegs(s, enterTStamp + 1, enterSP, thread,
						new EndReceiver() {
					public void receiveEnd(long endTStamp) {
						if (endTStamp > tStamp) {
							// This call contains tStamp so it's the one we want
							receiver.receiveStart(enterTStamp, endTStamp,
									beforeCallSP, stackEnd, thread);
							return;
						}
						// This call returned before tStamp. Spawn another
						// query to find out who called it, that will be the one
						// we want.
						findStartOfCallWithRegs(s, enterTStamp, beforeCallSP,
								stackEnd, thread, receiver);
					}
					public void receiveNothing() {
						// This call did not end, so this must be the one we want
						receiver.receiveStart(enterTStamp, s.getEndTStamp(),
								beforeCallSP, stackEnd, thread);
					}
				});
			}
			public void notifyMMapResult(ScanQuery q, long tStamp, long start,
					long end, MMapInfo info) {}
			private boolean found = false;
		});
		q.send();
	}

	public static void findStartOfCall(final Session s, final long tStamp,
			final StartReceiver receiver) {
		Architecture arch = s.getArchitecture();
		final String spReg = arch.getSPReg();
		final String threadReg = Architecture.getThreadReg();
		ReadRegQuery q = new ReadRegQuery(s,
				tStamp, new String[] { threadReg, spReg }, 64,
				new ReadRegQuery.Listener() {
			public void notifyDone(ReadRegQuery q, boolean complete,
					final RegisterValues values) {
				if (complete) {
					final long sp = values.getLongValue(spReg);
					findMemoryEnd(s, tStamp, sp, new MemoryEndReceiver() {
						public void receiveEnd(long stackEnd) {
		    				findStartOfCallWithRegs(s, tStamp, sp, stackEnd,
		        					values.getIntValue(threadReg), receiver);
						}
					});
				} else {
	                receiver.receiveNothing();
				}
			}
		});
		q.send();
	}

	/**
	 * Finds the end of mapped memory at 'address' at time 'tStamp'.
	 */
	public static void findMemoryEnd(Session s, long tStamp,
		 long address, MemoryEndReceiver receiver) {
		findMemoryEnd(s, tStamp, address, DEFAULT_BUMP, receiver);
	}

	/**
	 * Finds the end of mapped memory at 'address' at time 'tStamp'.
	 */
	public static void findMemoryEnd(final Session s, final long tStamp,
			final long address, final long bump,
			final MemoryEndReceiver receiver) {
		final long endAddress = address <= Long.MAX_VALUE - bump ?
				address + bump : Long.MAX_VALUE;
		ScanMMapQuery q = new ScanMMapQuery(s,
				0, tStamp, new MemRange[] { new MemRange(address, endAddress) },
				ScanQuery.Termination.LAST_COVER, new ScanMMapQuery.Listener() {
			public void notifyMMapResult(ScanQuery q, long tStamp, long start,
					long end, MMapInfo info) {
				if (info.isMapped()) {
					mappedRanges.add(new MemRange(start, end));
				}
			}
			public void notifyDone(ScanQuery q, boolean complete) {
				Collections.sort(mappedRanges, MemRange.getStartComparator());
				long mappedEndAddress = address + 1;
				for (MemRange r : mappedRanges) {
					if (r.getStart() <= mappedEndAddress) {
						mappedEndAddress = Math.max(mappedEndAddress, r.getEnd());
					}
				}
				if (mappedEndAddress >= endAddress) {
					long newBump = bump;
					if (newBump*2 > 0) {
						newBump = newBump*2;
					}
					findMemoryEnd(s, tStamp, mappedEndAddress - 1, newBump,
							      receiver);
					return;
				}
				receiver.receiveEnd(mappedEndAddress);
		    }
			private ArrayList<MemRange> mappedRanges = new ArrayList<MemRange>();
		});
		q.send();
	}

	/**
	 * tStamp should be the timestamp of the first instruction of the function
	 * invocation.
	 */
	public static void findEndOfCallWithRegs(Session s, long tStamp, long sp,
			int thread, final EndReceiver receiver) {
		FindGreaterSPQuery q = new FindGreaterSPQuery(s,
				tStamp, s.getEndTStamp(), sp, thread,
				new FindGreaterSPQuery.Listener() {
			public void notifyDone(FindGreaterSPQuery q, boolean complete,
					Long tStamp) {
				if (tStamp != null) {
					receiver.receiveEnd(tStamp + 1);
				} else {
					receiver.receiveNothing();
				}
			}
		});
		q.send();
	}

	/**
	 * tStamp should be the timestamp of the first instruction of the function
	 * invocation.
	 */
	public static void findEndOfCall(final Session s, final long tStamp,
			final EndReceiver receiver) {
		Architecture arch = s.getArchitecture();
		final String spReg = arch.getSPReg();
		final String threadReg = Architecture.getThreadReg();
		ReadRegQuery q = new ReadRegQuery(s,
				tStamp, new String[] { threadReg, spReg }, 64,
				new ReadRegQuery.Listener() {
			public void notifyDone(ReadRegQuery q, boolean complete,
					RegisterValues values) {
				if (complete) {
					findEndOfCallWithRegs(s, tStamp, values.getLongValue(spReg),
	    					values.getIntValue(threadReg), receiver);
				} else {
	                receiver.receiveNothing();
				}
			}
		});
		q.send();
	}
	
	public static void findRunningFunction(final Session s, final long tStamp,
			final FunctionReceiver receiver) {
		final String pcReg = Architecture.getPCReg();
		ReadRegQuery q = new ReadRegQuery(s,
				tStamp, new String[] { pcReg }, 64,
				new ReadRegQuery.Listener() {
			public void notifyDone(ReadRegQuery q, boolean complete,
					final RegisterValues values) {
				if (complete) {
					final long address = values.getLongValue(pcReg);
					FindContainingFunctionQuery fq = new FindContainingFunctionQuery(s, address, tStamp,
							new FindContainingFunctionQuery.Listener() {
						public void notifyDone(FindContainingFunctionQuery q,
								boolean complete, Function function) {
							if (complete) {
								if (function == null) {
									function = new Function(s, address);
								}
								receiver.receiveFunction(function);
							} else {
								receiver.receiveFunction(null);
							}
						}
					});
					fq.send();
				} else {
	                receiver.receiveFunction(null);
				}
			}
		});
		q.send();
	}
}
