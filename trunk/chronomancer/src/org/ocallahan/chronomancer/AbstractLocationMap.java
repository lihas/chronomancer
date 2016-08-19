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

import java.util.ArrayList;
import java.util.HashMap;

public class AbstractLocationMap {
	private State state;
	// TODO make this use weak references to Locations
	private HashMap<Long,ArrayList<AbstractLocation>> map =
		new HashMap<Long,ArrayList<AbstractLocation>>();
	
	public AbstractLocationMap(State state) {
		this.state = state;
	}
	
	public static interface Receiver {
		// Called on the UI thread
		public void receive(AbstractLocation l);
	}
	
    public void getLocation(long tStamp, long address, final Receiver r) {
    	ArrayList<AbstractLocation> locations = map.get(address);
    	if (locations == null) {
    		locations = new ArrayList<AbstractLocation>();
    		map.put(address, locations);
    	}
    	for (final AbstractLocation l : locations) {
    		AbstractLocation.Home[] homes = l.getHomes();
    		if (homes[0].getBeginTStamp() <= tStamp &&
    			tStamp < homes[homes.length - 1].getEndTStamp()) {
    			state.getDisplay().asyncExec(new Runnable() {
    				public void run() {
    					r.receive(l);
    				}
    			});
    			return;
    		}
    	}
    	
    	final AbstractLocation l = new AbstractLocation(1, new AbstractLocation.Home[] {
            new AbstractLocation.Home(address, 0, state.getSession().getEndTStamp())	
    	});
    	locations.add(l);
		state.getDisplay().asyncExec(new Runnable() {
			public void run() {
				r.receive(l);
			}
		});
    }
}
