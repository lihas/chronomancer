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

import org.eclipse.draw2d.IFigure;

public abstract class TraceEvent implements Comparable<TraceEvent> {
    private long tStamp;
    private IEventQuery query;

    public TraceEvent(long tStamp, IEventQuery query) {
    	this.tStamp = tStamp;
    	this.query = query;
    }
    
    public interface IEventFigure extends IFigure {
    	public TraceEvent getTraceEvent();
    }
    
    public int getColorHash() { return 0; }

    /**
     * Create a figure representing this event. This can be called
     * multiple times in different contexts, so the relation between
     * TraceEvents and figures is one to many.
     */
    public abstract IEventFigure createFigure(State state);
    /**
     * Creates a figure representing the interval for this event. This can
     * be called multiple times in different contexts. It will only be called
     * on start events (getStart() == this). Return null if there isn't one.
     */
    public IEventFigure createIntervalFigure(State state) {
    	return null;
    }
    
    public long getTStamp() {
    	return tStamp;
    }
    public IEventQuery getQuery() {
    	return query;
    }

    /**
     * Check whether this event subsumes another event with the same timestamp.
     * Return true if the other event should not be displayed if this event is.
     * (This is not equality because the events may have different details.)
     * This is only called when t.getTStamp() == this.getTStamp().
     */
    public boolean subsumes(TraceEvent t) {
    	return false;
    }
    
    /**
     * If this is the boundary of an interval, return the start of the interval.
     * Otherwise return null.
     */
    public TraceEvent getStart() {
    	return null;
    }

    /**
     * If this is the boundary of an interval, return the end of the interval.
     * Otherwise return null.
     */
    public TraceEvent getEnd() {
    	return null;
    }
    
    final public TraceEvent getSingletonStart() {
    	TraceEvent t = getStart();
    	return t == null ? this : t;
    }
    
    private int getSameTimestampCategory() {
    	TraceEvent start = getStart();
    	TraceEvent end = getEnd();
    	if (end == this && start.tStamp < end.tStamp)
    		return 1;
    	if (start == this && start.tStamp < end.tStamp)
    		return 3;
    	return 2;
    }

    public int compareTo(TraceEvent o) {
    	// First order by timestamp
    	if (tStamp < o.tStamp)
    		return -1;
    	if (tStamp > o.tStamp)
    		return 1;
    	// Then order events with the same timestamp in the following groups:
    	// 1) end events that started before this timestamp (ordered by
    	// reverse start-tstamp, so events that started later seem to end
    	// earlier, and then identity of starts)
    	// 2) start and end events that start and end on this timestamp
    	// (grouped together), and singleton events (all ordered by identity)
    	// 3) start events that end after this timestamp (ordered by
    	// reverse end-tstamp, so events that end later seem to start earlier,
    	// and then reverse identity of ends)
    	int cat = getSameTimestampCategory();
    	int catO = o.getSameTimestampCategory();
    	if (cat != catO)
    		return cat - catO;
    	switch (cat) {
    	case 1:
    		return compareTStamps(getStart(), o.getStart());
    	case 2: {
    		int cmp = compareIdentity(getSingletonStart(), o.getSingletonStart());
    		if (cmp != 0)
    			return cmp;
    		if (o == this)
    			return 0;
    		return this.getEnd() == o ? -1 : 1;
    	}	
    	case 3:
    		return compareTStamps(getEnd(), o.getEnd());
        default:
        	throw new IllegalArgumentException("Unknown category");
    	}
    }

	private static int compareTStamps(TraceEvent t1, TraceEvent t2) {
		if (t1.tStamp < t2.tStamp)
			return 1;
		if (t1.tStamp > t2.tStamp)
			return -1;
		return -compareIdentity(t1, t2);
	}

	private static int compareIdentity(TraceEvent t1, TraceEvent t2) {
		int h1 = System.identityHashCode(t1);
		int h2 = System.identityHashCode(t2);
		if (h1 < h2)
			return -1;
		if (h1 > h2)
			return 1;
		return 0;
	}
}
