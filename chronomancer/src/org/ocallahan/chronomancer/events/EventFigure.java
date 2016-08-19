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
package org.ocallahan.chronomancer.events;

import org.ocallahan.chronomancer.SequenceFigure;
import org.ocallahan.chronomancer.StringFigure;
import org.ocallahan.chronomancer.TextExportDelimiters;
import org.ocallahan.chronomancer.TraceEvent;

public class EventFigure extends SequenceFigure
    implements TraceEvent.IEventFigure {
	private static TextExportDelimiters DELIMITERS =
		new TextExportDelimiters("event", "", "", "");
	
	public EventFigure(TraceEvent event) {
		this.event = event;
		setGap(STANDARD_GAP);
		setTextExportDelimiters(DELIMITERS);
	}
	
	public EventFigure(TraceEvent event, String verb) {
		this(event);
		add(new StringFigure(verb));
	}
	
	public EventFigure(TraceEvent event, String verb, String object) {
		this(event, verb);
		add(new StringFigure(object));
	}
	
    public TraceEvent getTraceEvent() {
    	return event;
    }
    
    private TraceEvent event;
    
    public static final int STANDARD_GAP = 4;
}
