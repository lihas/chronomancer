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

import java.util.Map;

import org.ocallahan.chronicle.Architecture;
import org.ocallahan.chronicle.FindSourceInfoQuery;
import org.ocallahan.chronicle.ReadRegQuery;
import org.ocallahan.chronicle.RegisterValues;
import org.ocallahan.chronicle.SourceCoordinate;
import org.ocallahan.chronomancer.State;
import org.ocallahan.chronomancer.TraceEvent;

public class SourceViewer {
	private State state;
	private boolean enabled = true;
	private State.Observer observer = new StateObserver();
	private SourceAnnotator annotator;
	
	class StateObserver extends State.Observer.Stub {
		public void notifyCurrentEventChanged(State s) {
			if (!enabled)
				return;
			setupSourceForEvent(s.getCurrentEvent());
		}
	}
	
	// runs on UI thread
	public void notifyAnnotatorComplete(SourceAnnotator annotator) {
		if (this.annotator != null) {
			this.annotator.terminate();
		}
		this.annotator = annotator;
	}
	
	// runs on UI thread
	private void setupSourceForEvent(TraceEvent event) {
		if (annotator != null) {
			annotator.terminate();
			annotator = null;
		}

		if (event == null)
			return;

		long tStamp = event.getTStamp();
		annotator = new SourceAnnotator(this, tStamp);
		showCurrentLine(annotator, tStamp);
	}
	
	private void showCurrentLine(final SourceAnnotator annotator, final long tStamp) {
		ReadRegQuery rq = new ReadRegQuery(state.getSession(), tStamp,
				new String[] { Architecture.getPCReg() }, 64, new ReadRegQuery.Listener() {
			public void notifyDone(ReadRegQuery rq, boolean complete,
					RegisterValues values) {
				if (!complete)
					return;
				final long pc = values.getLongValue(Architecture.getPCReg());
				FindSourceInfoQuery q = new FindSourceInfoQuery(state.getSession(),
						tStamp, new long[] { pc }, new FindSourceInfoQuery.Listener() {
					public void notifyDone(FindSourceInfoQuery q,
							boolean complete,
							Map<Long, SourceCoordinate> sources) {
						final SourceCoordinate src = sources.get(pc);
						if (src == null)
							return;
						state.getDisplay().asyncExec(new Runnable() {
							public void run() {
								annotator.showCurrentLine(src);
							}							
						});
					}
				});
				q.send();
			}
		});
		rq.send();
	}
	
	public SourceViewer(State state) {
		this.state = state;
		state.addObserver(observer);
	}
	
	public State getState() {
		return state;
	}
    
	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
		setupSourceForEvent(enabled ? state.getCurrentEvent() : null);
	}
}
