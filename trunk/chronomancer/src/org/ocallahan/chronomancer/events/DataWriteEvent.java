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

import org.ocallahan.chronicle.IDataSource;
import org.ocallahan.chronicle.Type;
import org.ocallahan.chronomancer.DataContext;
import org.ocallahan.chronomancer.State;
import org.ocallahan.chronomancer.TraceEvent;

public class DataWriteEvent extends TraceEvent {
	private DataContext context;
	
	public DataWriteEvent(long tStamp, DataContext context,
			IDataSource source, Type type) {
		super(tStamp, null);
		this.context = context;
	}
	
	@Override
	public int getColorHash() {
		return context.toString().hashCode();
	}
	
	@Override
    public boolean subsumes(TraceEvent t) {
    	if (!(t instanceof DataWriteEvent))
    		return false;
    	DataWriteEvent w = (DataWriteEvent)t;
    	// This is a bit of a hack.
    	return context.toString().equals(w.context.toString());
    }
	
	@Override
	public TraceEvent.IEventFigure createFigure(State state) {
		return  new EventFigure(this, "Write to ", context.toString());
	}
}
