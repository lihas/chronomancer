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

package org.ocallahan.chronomancer.views;

import java.util.ArrayList;
import java.util.Arrays;

import org.ocallahan.chronicle.Function;
import org.ocallahan.chronicle.MMapInfo;
import org.ocallahan.chronicle.MemRange;
import org.ocallahan.chronicle.ScanExecQuery;
import org.ocallahan.chronicle.ScanQuery;
import org.ocallahan.chronicle.Session;

/**
 * XXX does not yet handle recursive functions!
 * @author roc
 *
 */
public class LoopAnalyzer {
	private Session session;
	private Function function;
	private long startTStamp;
	private long endTStamp;
	private Listener listener;
	
	/**
	 * These methods are called on the session thread
	 */
	public static interface Listener {
		public void foundLoop(Exec head, long endTStamp,
				              long secondIterationTStamp, long lastIterationTStamp);
		/**
		 * @param instructionsExecuted sorted by timestamp
		 */
		public void foundOutsideLoop(Exec[] instructionsExecuted);
		public void done();
	}
	
	/**
	 * @param session
	 * @param function
	 * @param startTStamp
	 * @param endTStamp
	 * @param receiver gets called on the session thread
	 */
    public static void analyze(Session session, Function function,
    		                   long startTStamp, long endTStamp,
    		                   Listener listener) {
    	(new LoopAnalyzer(session, function, startTStamp, endTStamp, listener))
    	    .analyze();
    }
    
    private LoopAnalyzer(Session session, Function function,
            long startTStamp, long endTStamp, Listener listener) {
    	this.session = session;
    	this.function = function;
    	this.startTStamp = startTStamp;
    	this.endTStamp = endTStamp;
    	this.listener = listener;
    }
    
    public static class Exec implements Comparable<Exec> {
    	Exec(long address, byte length, long tStamp) {
    		this.address = address;
    		this.length = length;
    		this.tStamp = tStamp;
    	}
    	public int compareTo(Exec o) {
    		if (tStamp < o.tStamp)
    			return -1;
    		if (tStamp > o.tStamp)
    			return 1;
    		return 0;
    	}
    	long address;
    	byte length;
    	long tStamp;
		public long getAddress() {
			return address;
		}
		public byte getLength() {
			return length;
		}
		public long getTStamp() {
			return tStamp;
		}
		public String toString() {
			return address + "@" + tStamp;
		}
    }
    
    /**
     * The basic plan:
     * 1) find the time of first execution for all instructions
     * over the given time interval
     * 2) find the first instruction which is executed twice; that's the head
     * of the first, outermost loop; if none is found, we're done
     * 3) find the last execution of the loop head instruction. That's the start
     * of the last iteration of the loop
     * 4) find the first timestamp T such that all instructions executed
     * after T did not execute before T; this is the end of the last
     * iteration
     *   4a) collect all instruction ranges for instructions that executed during
     *   loop iterations
     *   4b) find the last execution of any of those instructions
     *   4c) repeat until fixpoint
     * 5) record loop info, restart at step 2 to analyze the time interval
     * from T to end
     */
    private void analyze() {
    	MemRange[] ranges = function.getRanges();
    	if (ranges == null)
    		throw new IllegalArgumentException("Range-less functions not (yet) supported");
    	ScanExecQuery q = new ScanExecQuery(session, startTStamp,
    			endTStamp, ranges, ScanQuery.Termination.FIRST_COVER,
    			new ScanExecQuery.Listener() {
    		public void notifyDone(ScanQuery q, boolean complete) {
    			Exec[] execArray = new Exec[execs.size()];
    			execs.toArray(execArray);
    			// sort the instruction executions by timestamp
    			Arrays.sort(execArray);
    			findLoop(execArray, 0);
    		}
    		public void notifyExecResult(ScanExecQuery q, long tStamp,
    				long start, long end) {
    			long length = end - start;
    			if ((byte)length != length)
    				throw new IllegalArgumentException("What kind of freak instruction is this?");
    			execs.add(new Exec(start, (byte)length, tStamp));
    		}
    		public void notifyMMapResult(ScanQuery q, long tStamp,
    				long start, long end, MMapInfo info) {}
    		private ArrayList<Exec> execs = new ArrayList<Exec>();
    	});
    	q.send();
    }
    
    private void notifyFoundOutsideLoop(Exec[] execs, int start, int end) {
		// None of these instructions repeat
		Exec[] subExecs = new Exec[end - start];
		System.arraycopy(execs, start, subExecs, 0, end - start);
		listener.foundOutsideLoop(subExecs);
    }
    
    private void findLoop(final Exec[] execs, final int startIndex) {
    	if (startIndex >= execs.length) {
    		listener.done();
    		return;
    	}
    	
    	// find run of instructions at consecutive timestamps. We know that
    	// they can only execute once each during the run. Also collect
    	// the list of memory ranges that the instructions comprise.
    	int runEnd;
    	for (runEnd = startIndex + 1; runEnd < execs.length; ++runEnd) {
    		if (execs[runEnd].tStamp != execs[runEnd - 1].tStamp + 1)
    			break;
    	}
	    final int finalRunEnd = runEnd;

	    long runEndTStamp = execs[runEnd - 1].tStamp + 1;
	    // See if anything in this run repeats
    	ScanExecQuery q = new ScanExecQuery(session, runEndTStamp,
    			endTStamp, getInstructionsForExecs(execs, startIndex, runEnd),
    			ScanQuery.Termination.FIRST, new ScanExecQuery.Listener() {
    		public void notifyDone(ScanQuery q, boolean complete) {
    			if (repeatIndex < 0) {
    				notifyFoundOutsideLoop(execs, startIndex, finalRunEnd);
    				findLoop(execs, finalRunEnd);
    				return;
    			}
    			// we've found the first, outermost loop head.
    			studyLoop(execs, repeatIndex, repeatTimestamp);
    		}
    		public void notifyExecResult(ScanExecQuery q, long tStamp,
    				long start, long end) {
    			for (int i = startIndex; i < finalRunEnd; ++i) {
    				if (execs[i].address == start) {
    					repeatTimestamp = tStamp;
    					repeatIndex = i;
    					return;
    				}
    			}
				throw new IllegalArgumentException("Where did this instruction come frame?");
    		}
    		public void notifyMMapResult(ScanQuery q, long tStamp,
    				long start, long end, MMapInfo info) {}
    		private long repeatTimestamp;
    		private int repeatIndex = -1;
    	});
    	q.send();
    }
    
    private void studyLoop(final Exec[] execs, final int headIndex,
    		final long secondIterationTStamp) {
    	long address = execs[headIndex].address;
    	ScanExecQuery q = new ScanExecQuery(session, startTStamp,
    			endTStamp, new MemRange[] { new MemRange(address, address + 1) },
    			ScanQuery.Termination.LAST, new ScanExecQuery.Listener() {
    		public void notifyDone(ScanQuery q, boolean complete) {
    			if (lastIterationStart == 0)
    				throw new IllegalArgumentException("We found this instruction before, why can't we find it now?");
    			findLoopEnd(execs, headIndex, lastIterationStart + 1,
    					secondIterationTStamp, lastIterationStart);
    		}
    		public void notifyExecResult(ScanExecQuery q, long tStamp,
    				long start, long end) {
    			lastIterationStart = tStamp;
    		}
    		public void notifyMMapResult(ScanQuery q, long tStamp,
    				long start, long end, MMapInfo info) {}
    		private long lastIterationStart;
    	});
    	q.send();
    }
    
    private MemRange[] getInstructionsForExecs(Exec[] execs, int start, int end) {
    	ArrayList<MemRange> runRanges = new ArrayList<MemRange>();
    	long currentStart = execs[start].address;
    	long currentEnd = currentStart + execs[start].length;
    	for (int i = start + 1; i < end; ++i) {
    		if (execs[i].address != currentEnd) {
    		    runRanges.add(new MemRange(currentStart, currentEnd));
    		    currentStart = execs[i].address;
    		    currentEnd = currentStart;
    		}
    		currentEnd += execs[i].length;
    	}
	    runRanges.add(new MemRange(currentStart, currentEnd));
	    MemRange[] runRangeArray = new MemRange[runRanges.size()];
	    return runRanges.toArray(runRangeArray);
    }
    
    private int findExecForTStamp(Exec[] execs, int startAt, long tStamp) {
    	int i;
    	for (i = startAt; i < execs.length; ++i) {
    		if (execs[i].tStamp >= tStamp)
    			break;
    	}
    	return i;
    }
    
    private MemRange[] getInstructionsInLoop(Exec[] execs, int headIndex,
    		                                 long loopEndTStamp) {
    	return getInstructionsForExecs(execs, headIndex,
    			findExecForTStamp(execs, headIndex, loopEndTStamp));
    }
    
    private void findLoopEnd(final Exec[] execs, final int headIndex,
    		final long currentEndTStamp, final long secondIterationTStamp,
    		final long lastIterationTStamp) {
    	ScanExecQuery q = new ScanExecQuery(session, currentEndTStamp,
    			endTStamp, getInstructionsInLoop(execs, headIndex, currentEndTStamp),
    			ScanQuery.Termination.LAST, new ScanExecQuery.Listener() {
    		public void notifyDone(ScanQuery q, boolean complete) {
    			if (newEnd == 0) {
    				foundLoopEnd(execs, headIndex, currentEndTStamp,
    						secondIterationTStamp, lastIterationTStamp);
    				return;
    			}
    			findLoopEnd(execs, headIndex, newEnd,
    					secondIterationTStamp, lastIterationTStamp);
    		}
    		public void notifyExecResult(ScanExecQuery q, long tStamp,
    				long start, long end) {
    			newEnd = tStamp + 1;
    		}
    		public void notifyMMapResult(ScanQuery q, long tStamp,
    				long start, long end, MMapInfo info) {}
    		private long newEnd;
    	});
    	q.send();
    }
    
    private void foundLoopEnd(final Exec[] execs, int headIndex, long loopEndTStamp,
    		long secondIterationTStamp, long lastIterationTStamp) {
    	listener.foundLoop(execs[headIndex], loopEndTStamp,
    			secondIterationTStamp, lastIterationTStamp);
    	findLoop(execs, findExecForTStamp(execs, headIndex, loopEndTStamp));
    }
    
    public long getStartTStamp() {
    	return startTStamp;
    }
}
