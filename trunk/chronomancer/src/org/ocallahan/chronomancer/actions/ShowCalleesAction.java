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

package org.ocallahan.chronomancer.actions;

import java.util.List;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.text.source.IVerticalRulerInfo;
import org.eclipse.ui.texteditor.AbstractRulerActionDelegate;
import org.eclipse.ui.texteditor.ITextEditor;
import org.ocallahan.chronicle.Function;
import org.ocallahan.chronicle.QueryUtils;
import org.ocallahan.chronomancer.IReceiver;
import org.ocallahan.chronomancer.ISourceAnnotator;
import org.ocallahan.chronomancer.State;
import org.ocallahan.chronomancer.TraceEvent;
import org.ocallahan.chronomancer.events.FunctionCall;

public class ShowCalleesAction extends AbstractRulerActionDelegate {
	@Override
	protected IAction createAction(final ITextEditor editor,
			final IVerticalRulerInfo rulerInfo) {
		return new Impl(editor, rulerInfo);
	}
	
	static class Impl extends BaseLineAction {
		public Impl(ITextEditor editor, IVerticalRulerInfo rulerInfo) {
			super(editor, rulerInfo);
		}

		public void update() {
			setEnabled(!getTStampAddrs().isEmpty());
			setText("Show Callees");
		}
		
		@Override
		public void run() {
			List<ISourceAnnotator.TStampAddrPair> tStampAddrs = getTStampAddrs();
			if (tStampAddrs.isEmpty())
				return;
			
			State state = getState();
			long lastTStamp = -1;
			boolean[] found = { false };
			for (ISourceAnnotator.TStampAddrPair p : tStampAddrs) {
				long tStamp = p.getTStamp();
				if (tStamp != lastTStamp + 1 && lastTStamp >= 0) {
					checkTStamp(state, lastTStamp, found);
				}
				lastTStamp = tStamp;
			}
			checkTStamp(state, lastTStamp, found);
		}
		
		private static void checkTStamp(final State state, final long tStamp,
				final boolean[] found) {
			QueryUtils.findStartOfCall(state.getSession(), tStamp + 1,
					new QueryUtils.StartReceiver() {
				public void receiveNothing() {
				}
				public void receiveStart(long startTStamp, final long endTStamp,
						long beforeCallSP, long stackEnd, int thread) {
					if (tStamp != startTStamp)
						return;
					QueryUtils.findRunningFunction(state.getSession(), tStamp + 1,
							new QueryUtils.FunctionReceiver() {
						public void receiveFunction(Function function) {
							if (function.getIdentifier() == null) {
								// XXX this is a terrible hack! Why are we getting
								// anonymous functions here?
								checkTStamp(state, tStamp + 1, found);
								return;
							}
							FunctionCall.add(state, null, new IReceiver<TraceEvent>() {
								public void receive(TraceEvent event) {
									state.addEvent(event);
									if (!found[0] && event.getStart() == event) {
										found[0] = true;
										state.setCurrentEvent(event);
									}
								}
							}, function, tStamp + 1, endTStamp);
						}
					});
				}
			});
		}
	}
}
