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

import org.eclipse.draw2d.Border;
import org.eclipse.draw2d.CompoundBorder;
import org.eclipse.draw2d.Figure;
import org.eclipse.draw2d.MarginBorder;
import org.eclipse.draw2d.SimpleEtchedBorder;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.widgets.Composite;
import org.ocallahan.chronicle.Function;
import org.ocallahan.chronicle.GetVariablesQuery;
import org.ocallahan.chronicle.QueryUtils;
import org.ocallahan.chronicle.Session;
import org.ocallahan.chronicle.Variable;
import org.ocallahan.chronomancer.CompoundBuilder;
import org.ocallahan.chronomancer.DataContext;
import org.ocallahan.chronomancer.IInteractiveFigure;
import org.ocallahan.chronomancer.IReceiver;
import org.ocallahan.chronomancer.SequenceFigure;
import org.ocallahan.chronomancer.State;
import org.ocallahan.chronomancer.StringFigure;
import org.ocallahan.chronomancer.TraceEvent;
import org.ocallahan.chronomancer.State.Observer;
import org.ocallahan.chronomancer.events.FunctionParameters;

public class LocalsView extends FigureView {
    private SequenceFigure variables;
    private SequenceFigure parameters;
    private StateObserver observer = new StateObserver();
    private Font labelFont;

    private void removeAll() {
    	variables.removeAll();
    	parameters.removeAll();
    }
    
    class StateObserver extends Observer.Stub {
		public void notifyCurrentEventChanged(State s) {
			removeAll();
			TraceEvent t = s.getCurrentEvent();
			if (t != null) {
				buildLocalsFor(t.getTStamp());
			}
		}
		public void notifyReset(State s) {
			removeAll();
		}
	}

    public static class LocalContext extends DataContext {
		private Variable param;
		
		public LocalContext(DataContext parent, Variable param) {
			super(parent);
			this.param = param;
		}
		
		@Override
		public String toString() {
			return getParent().toString() + ":" + param.toString();
		}
	}
    
	private void buildLocalsFor(final long tStamp) {
		final State state = getState();
		Session s = state.getSession();
		QueryUtils.findRunningFunction(s, tStamp, new QueryUtils.FunctionReceiver() {
			public void receiveFunction(Function function) {
				buildLocalsForCaller(state,
                    new FunctionParameters.FunctionContext(function, tStamp), tStamp);
			}
		});
	}

	private static final Border ITEM_BORDER =
		new CompoundBorder(SimpleEtchedBorder.singleton, new MarginBorder(1));

	private IInteractiveFigure makeLabel(String label) {
		StringFigure fig = new StringFigure(label);
		fig.setFont(labelFont);
		return fig;
	}

	private class VariableListener implements GetVariablesQuery.Listener {
		private State state;
		private DataContext callerContext;
		private long tStamp;
		private Figure figure;
		
		VariableListener(State state, long tStamp, DataContext callerContext,
				         Figure figure) {
			this.state = state;
			this.callerContext = callerContext;
			this.tStamp = tStamp;
			this.figure = figure;
		}
		
		public void notifyDone(GetVariablesQuery q, boolean complete,
				final Variable[] vars) {
			state.getDisplay().asyncExec(new Runnable() {
				public void run() {
					for (final Variable v : vars) {
						final SequenceFigure fig = new SequenceFigure();
						fig.setGap(CompoundBuilder.STANDARD_GAP);
						fig.setBorder(ITEM_BORDER);
						fig.add(makeLabel(v.getIdentifier().toString()));
						figure.add(fig);
						FunctionParameters.renderVariable(state, tStamp, v,
								new IReceiver<IInteractiveFigure>() {
							public void receive(IInteractiveFigure f) {
								fig.add(f);
							}
						}, new LocalContext(callerContext, v));
					}
				}
			});
		}
	}
	
	private void buildLocalsForCaller(final State state,
			final DataContext callerContext, final long tStamp) {
		Session s = state.getSession();
		GetVariablesQuery q = new GetVariablesQuery(s, tStamp, GetVariablesQuery.Kind.PARAMETERS,
				new VariableListener(state, tStamp, callerContext, parameters));
		q.send();
		q = new GetVariablesQuery(s, tStamp, GetVariablesQuery.Kind.LOCALS,
				new VariableListener(state, tStamp, callerContext, variables));
		q.send();
	}
	
	/**
	 * This is a callback that will allow us
	 * to create the viewer and initialize it.
	 */
	public void createPartControl(Composite parent) {
		SequenceFigure figure = new SequenceFigure();
	    parameters = new SequenceFigure();
	    variables = new SequenceFigure();
		figure.add(parameters);
		figure.add(variables);

		setupControl(parent, figure);

		final State state = getState();
		labelFont = state.getColorScheme().getLabelFont(figure.getFont());
		
		state.addObserver(observer);
	}
}
