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

import org.ocallahan.chronomancer.State;
import org.ocallahan.chronomancer.TraceEvent;

public class TraceEndPoints {
	public static class Start extends TraceEvent {
		Start() {
			super(0, null);
		}

		@Override
		public TraceEvent.IEventFigure createFigure(State state) {
    		return new EventFigure(this, "Start of trace");
		}
	}

	public static class End extends TraceEvent {
		End(long tStamp) {
			super(tStamp, null);
		}

		@Override
		public TraceEvent.IEventFigure createFigure(State state) {
    		return new EventFigure(this, "End of trace");
		}
	}

	/**
	 * Unlike other query adders, this adds the events on the *current thread*
	 * (hence no Display parameter).
	 */
	public static void add(final State st) {
		st.addEvent(new Start());
		st.addEvent(new End(st.getSession().getEndTStamp()));
	}
 }
