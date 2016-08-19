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

import org.eclipse.draw2d.Figure;
import org.eclipse.draw2d.FlowLayout;
import org.eclipse.draw2d.MouseEvent;
import org.eclipse.draw2d.MouseListener;
import org.eclipse.swt.widgets.Composite;
import org.ocallahan.chronicle.Function;
import org.ocallahan.chronicle.QueryUtils;
import org.ocallahan.chronicle.Session;
import org.ocallahan.chronomancer.IInteractiveFigure;
import org.ocallahan.chronomancer.IReceiver;
import org.ocallahan.chronomancer.SequenceFigure;
import org.ocallahan.chronomancer.State;
import org.ocallahan.chronomancer.StringFigure;
import org.ocallahan.chronomancer.TextExportDelimiters;
import org.ocallahan.chronomancer.TraceEvent;
import org.ocallahan.chronomancer.State.Observer;
import org.ocallahan.chronomancer.events.EventFigure;
import org.ocallahan.chronomancer.events.FunctionCall;
import org.ocallahan.chronomancer.events.FunctionCaller;
import org.ocallahan.chronomancer.events.FunctionParameters;

public class StackView extends FigureView {
    private Figure figure;
    private StateObserver observer = new StateObserver();
    private StackBuilder builder;

    class StackBuilder implements QueryUtils.StartReceiver {
    	StackBuilder(Session s) {
    		this.s = s;
    	}
		public void receiveNothing() {
			control.getDisplay().asyncExec(new Runnable() {
				public void run() {
					if (cancelled)
						return;
					addTopOfStack();
				}
			});
		}
		public void receiveStart(final long tStamp, final long endTStamp,
				final long beforeCallSP, final long stackEnd, final int thread) {
			control.getDisplay().asyncExec(new Runnable() {
				public void run() {
					if (cancelled)
						return;
					// We add the activation record's figure now before we have
					// all the data, because we want to make sure it appears in the
					// the right order even if the findRunningFunction query
					// to fill in the function name takes a long time.
					final SequenceFigure activationFigure = addActivationRecord();
					QueryUtils.findStartOfCallWithRegs(s, tStamp, beforeCallSP,
							stackEnd, thread, StackBuilder.this);
					QueryUtils.findRunningFunction(s, tStamp + 1,
							new QueryUtils.FunctionReceiver() {
						public void receiveFunction(final Function function) {
							control.getDisplay().asyncExec(new Runnable() {
								public void run() {
									if (cancelled)
										return;
									fillInActivationRecord(activationFigure,
											tStamp + 1, function);
								}
							});
						}
					});
				}
			});
		}
		void cancel() {
			cancelled = true;
		}
		private Session s;
		// 'cancelled' is only accessed on the UI thread
		private boolean cancelled;
    }
    
    // call this on the the UI thread only!
    private void cancelBuilder() {
    	if (builder == null)
    		return;
    	builder.cancel();
    	builder = null;
    }
    
	class StateObserver extends Observer.Stub {
		public void notifyCurrentEventChanged(State s) {
			cancelBuilder();
			figure.removeAll();
			TraceEvent t = s.getCurrentEvent();
			if (t != null) {
				buildCallStackFor(t.getTStamp());
			}
		}
		public void notifyReset(State s) {
			cancelBuilder();
			figure.removeAll();
		}
	}

	private void buildCallStackFor(long tStamp) {
		Session s = getState().getSession();
		builder = new StackBuilder(s);
		QueryUtils.findStartOfCall(s, tStamp, builder);
	}
	
	private void addTopOfStack() {
		figure.add(new StringFigure("End of stack"));
	}
	
	private static TextExportDelimiters DELIMITERS =
		new TextExportDelimiters("stack", "", "", "");
	
	private SequenceFigure addActivationRecord() {
		SequenceFigure activationFigure = new SequenceFigure();
		activationFigure.setGap(EventFigure.STANDARD_GAP);
		activationFigure.setTextExportDelimiters(DELIMITERS);
		final State state = getState();
		state.getColorScheme().setupDefaultFigureColors(activationFigure);
		figure.add(activationFigure);
		return activationFigure;
	}
	
	private void fillInActivationRecord(final SequenceFigure activationFigure,
			final long firstTStamp, final Function function) {
		String str = function.toString();
		activationFigure.add(new StringFigure(str));
		final State state = getState();
		state.getColorScheme().setupFigureColors(activationFigure, str.hashCode());
		activationFigure.addMouseListener(new MouseListener() {
			public void mouseDoubleClicked(MouseEvent me) {
				// Add the function entry and exit event pair to the timeline.
				FunctionCall.add(state, null, null, function, firstTStamp);
				if (firstTStamp > 1) {
					// Add a "call" even to the timeline; this is the instruction
					// in the caller that does the call. This lets the user see
					// the caller's state, which is what you usually want.
					FunctionCaller event = new FunctionCaller(firstTStamp - 1, function);
					state.addEvent(event);
					// ... and make it the current event since that's what the user
					// usually wants.
					state.setCurrentEvent(event);
				}
			}
			public void mousePressed(MouseEvent me) {}
			public void mouseReleased(MouseEvent me) {}
		});
		FunctionParameters.renderParameters(state, firstTStamp,
				new IReceiver<IInteractiveFigure>() {
			public void receive(IInteractiveFigure child) {
				activationFigure.add(child);
			}
		}, new FunctionParameters.FunctionContext(function, firstTStamp));
	}
	
	/**
	 * This is a callback that will allow us
	 * to create the viewer and initialize it.
	 */
	public void createPartControl(Composite parent) {
		figure = new Figure();
		figure.setLayoutManager(new FlowLayout(false));

		setupControl(parent, figure);
		
		getState().addObserver(observer);
	}
}
