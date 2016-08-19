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

package org.ocallahan.chronomancer.events;

import org.ocallahan.chronicle.Function;
import org.ocallahan.chronicle.QueryUtils;
import org.ocallahan.chronomancer.IEventQuery;
import org.ocallahan.chronomancer.IInteractiveFigure;
import org.ocallahan.chronomancer.IReceiver;
import org.ocallahan.chronomancer.State;
import org.ocallahan.chronomancer.TraceEvent;

public class FunctionCall {
	public static class Start extends TraceEvent {
    	Start(long tStamp, Function function, IEventQuery query) {
    		super(tStamp, query);
    		this.function = function;
    	}
    	@Override
    	public TraceEvent getStart() {
    		return end == null ? null : this;
    	}
    	@Override
    	public TraceEvent getEnd() {
    		return end;
    	}
    	End end;
    	Function function;
    	
    	@Override
    	public int getColorHash() {
    		return function.toString().hashCode();
    	}
    	
    	@Override
        public boolean subsumes(TraceEvent t) {
        	if (!(t instanceof Start))
        		return false;
        	// Two function calls starting at the same tstamp always subsume,
        	// they must be the same function invocation.
        	return true;
        }
    	
    	public String getFunctionName() {
    		return function.toString();
    	}
    	
    	@Override
    	public TraceEvent.IEventFigure createFigure(State state) {
    		final EventFigure figure = new EventFigure(this, "Entering ",
    				getFunctionName());
    		
    		FunctionParameters.renderParameters(state, getTStamp(),
    				new IReceiver<IInteractiveFigure>() {
    			public void receive(IInteractiveFigure child) {
    				figure.add(child);
    			}
    		}, new FunctionParameters.FunctionContext(function, getTStamp()));
    		return figure;
    	}
    	
    	@Override
    	public TraceEvent.IEventFigure createIntervalFigure(State state) {
    		return new IntervalBar(this);
    	}
    }
    
    public static class End extends TraceEvent {
    	End(long tStamp, Start start) {
    		super(tStamp, start.getQuery());
    		this.start = start;
    	}
    	@Override
    	public TraceEvent getStart() {
    		return start;
    	}
    	@Override
    	public TraceEvent getEnd() {
    		return this;
    	}
    	Start start;

    	@Override
    	public int getColorHash() {
    		return start.getColorHash();
    	}
    	
    	@Override
        public boolean subsumes(TraceEvent t) {
        	if (!(t instanceof End))
        		return false;
        	// Two function calls starting at the same tstamp always subsume,
        	// they must be the same function invocation.
        	return start.getTStamp() == ((End)t).start.getTStamp();
        }
    	
    	@Override
    	public TraceEvent.IEventFigure createFigure(State state) {
    		return new EventFigure(this, "Return from ", start.getFunctionName());
    	}
    }
    
    private static void insertEvents(final State state,
    		final IReceiver<TraceEvent> receiver, final Start start) {
    	state.getDisplay().asyncExec(new Runnable() {
    		public void run() {
    			if (receiver == null) {
    				state.addEvent(start);
    			} else {
    				receiver.receive(start);
    			}
    			TraceEvent end = start.getEnd();
    			if (end != null) {
        			if (receiver == null) {
        				state.addEvent(end);
        			} else {
        				receiver.receive(end);
        			}
    			}
    		}
    	});
    }
    
	/**
	 * entryTStamp should be the timestamp of the first instruction of the function
	 * invocation and entTStamp should be the timestamp of the end of the
	 * invocation. Call this on any thread.
	 */
    public static void add(final State st, final IEventQuery query,
    		final IReceiver<TraceEvent> receiver, final Function function, final long entryTStamp,
    		final long endTStamp) {
		Start start = new Start(entryTStamp, function, query);
		if (endTStamp < st.getSession().getEndTStamp()) {
			start.end = new End(endTStamp, start);
		}
		insertEvents(st, receiver, start);
    }
    
	/**
	 * entryTStamp should be the timestamp of the first instruction of the function
	 * invocation. Call this on any thread.
	 */
    public static void add(final State st, final IEventQuery query,
    		final IReceiver<TraceEvent> receiver, final Function function,
    		final long entryTStamp) {
    	QueryUtils.findEndOfCall(st.getSession(), entryTStamp,
    			new QueryUtils.EndReceiver() {
    		public void receiveEnd(long tStamp) {
    			add(st, query, receiver, function, entryTStamp, tStamp);
    		}
    		public void receiveNothing() {
    			add(st, query, receiver, function, entryTStamp,
    					st.getSession().getEndTStamp());
    		}
    	});
    }
}
