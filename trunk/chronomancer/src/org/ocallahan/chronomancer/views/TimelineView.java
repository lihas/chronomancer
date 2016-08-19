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

import java.util.ArrayList;
import java.util.HashMap;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.RegistryFactory;
import org.eclipse.draw2d.AbstractLayout;
import org.eclipse.draw2d.Figure;
import org.eclipse.draw2d.FocusBorder;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.MouseEvent;
import org.eclipse.draw2d.MouseListener;
import org.eclipse.draw2d.Viewport;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.ocallahan.chronomancer.IEventQueryFactory;
import org.ocallahan.chronomancer.IExportSink;
import org.ocallahan.chronomancer.IInteractiveFigure;
import org.ocallahan.chronomancer.State;
import org.ocallahan.chronomancer.TraceEvent;
import org.ocallahan.chronomancer.State.Observer;
import org.ocallahan.chronomancer.events.TraceEndPoints;

public class TimelineView extends FigureView implements MouseListener {
	private Action connectAction;
	private Action disconnectAction;
	private ArrayList<Action> queryActions;
	private StateObserver observer = new StateObserver();
    private HashMap<TraceEvent,TraceEvent.IEventFigure> eventFigures =
    	new HashMap<TraceEvent,TraceEvent.IEventFigure>();
    private HashMap<TraceEvent,TraceEvent.IEventFigure> intervalFigures =
    	new HashMap<TraceEvent,TraceEvent.IEventFigure>();
    private Figure figure;
    private TraceEvent.IEventFigure currentEventFigure;
    private SourceViewer sourceViewer;

    @Override
    public void init(IViewSite site) throws PartInitException {
    	super.init(site);
    	sourceViewer = new SourceViewer(getState());
    }
    
    public SourceViewer getSourceViewer() {
    	return sourceViewer;
    }
    
	public void mousePressed(MouseEvent me) {
		Object src = me.getSource();
		if (src instanceof TraceEvent.IEventFigure) {
			TraceEvent.IEventFigure f = (TraceEvent.IEventFigure)src;
			getState().setCurrentEvent(f.getTraceEvent());
			me.consume();
		}
	}
	 
	public void mouseDoubleClicked(MouseEvent me) {
	}
	
	public void mouseReleased(MouseEvent me) {
	}
	
	private void setupFigure(TraceEvent.IEventFigure f) {
		getState().getColorScheme().setupFigureColors(f, f.getTraceEvent().getColorHash());
		f.addMouseListener(this);
	}
	
	private void scrollIntoView(IFigure f) {
		Viewport v = control.getViewport();
		v.validate();
		Rectangle bounds = f.getBounds();
		Rectangle r = v.getClientArea();
		if (bounds.y >= r.y && bounds.bottom() <= r.bottom())
			return;

		int targetY = Math.max(bounds.y,
				(bounds.y + bounds.height/2) - r.height/2);
		control.scrollToY(targetY);
	}
	
	class StateObserver extends Observer.Stub {
		public void notifyStarted(State s) {
			setConnectionState(true);
			TraceEndPoints.add(s);
		}
		public void notifyCurrentEventChanged(State s) {
			if (currentEventFigure != null) {
				currentEventFigure.setBorder(null);
				currentEventFigure = null;
			}
			TraceEvent currentEvent = s.getCurrentEvent();
			if (currentEvent != null) {
				currentEventFigure = eventFigures.get(currentEvent);
				currentEventFigure.setBorder(new FocusBorder());
				scrollIntoView(currentEventFigure);
			}
		}
		public void notifyEventAdded(State s, TraceEvent t) {
		    TraceEvent.IEventFigure f = t.createFigure(s);
		    setupFigure(f);
			eventFigures.put(t, f);
			figure.add(f);
			if (t.getStart() == t) {
				TraceEvent.IEventFigure ifig = t.createIntervalFigure(s);
				if (ifig != null) {
					setupFigure(ifig);
					intervalFigures.put(t, ifig);
					figure.add(ifig);
				}
			}
		}
		public void notifyEventRemoved(State s, TraceEvent t) {
		    TraceEvent.IEventFigure f = eventFigures.remove(t);
			figure.remove(f);
			if (t.getStart() == t) {
				TraceEvent.IEventFigure ifig = intervalFigures.remove(t);
				if (ifig != null) {
					figure.remove(ifig);
				}
			}
		}
		public void notifyReset(State s) {
			notifyCurrentEventChanged(s);
			figure.removeAll();
			setConnectionState(false);
		}
	}
	
	private void setConnectionState(boolean enabled) {
		connectAction.setEnabled(!enabled);
		disconnectAction.setEnabled(enabled);
		for (Action a : queryActions) {
			a.setEnabled(enabled);
		}
	}
	
	private static int getGapHeight(long deltaT) {
		// 0 must give 0, > 0 must give > 0, generally we want logarithmic
		// scale.
		return 64 - Long.numberOfLeadingZeros(deltaT);
	}
	
	private int getIndent() {
		return 8; // pixels
	}

	public class TimelineLayout extends AbstractLayout {
		@Override
		protected Dimension calculatePreferredSize(IFigure container,
				int wHint, int hHint) {
			int x = 0;
			int y = 0;
			int width = 0;
			int indent = getIndent();
			long lastEventTStamp = 0;
			for (TraceEvent t : getState().getEventsWithSubsumption()) {
				IFigure f = eventFigures.get(t);
				Dimension d = f.getPreferredSize();
				
				long tStamp = t.getTStamp();
				int gap = getGapHeight(tStamp - lastEventTStamp);
				lastEventTStamp = tStamp;

				if (t.getEnd() == t) {
					x -= indent;
				}
				width = Math.max(width, x + d.width);
				if (t.getStart() == t) {
					x += indent;
				}
				y += gap + d.height;
			}
			return new Dimension(width, y);
		}
		
		public void layout(IFigure container) {
			Rectangle r = container.getClientArea();
			int x = r.x;
			int y = r.y;
			int indent = getIndent();
			long lastEventTStamp = 0;
			ArrayList<TraceEvent> subsumedEvents = new ArrayList<TraceEvent>();
			for (TraceEvent t : getState().getEventsWithSubsumption(subsumedEvents)) {
				IFigure f = eventFigures.get(t);
				Dimension d = f.getPreferredSize();
				
				long tStamp = t.getTStamp();
				int gap = getGapHeight(tStamp - lastEventTStamp);
				lastEventTStamp = tStamp;
				y += gap;

				if (t.getEnd() == t) {
					x -= indent;
				}
				f.setBounds(new Rectangle(x, y, d.width, d.height));
				if (t.getEnd() == t) {
					// place the interval figure now that we've laid out
					// the start and end
					TraceEvent start = t.getStart();
					IFigure ifig = intervalFigures.get(start);
					if (ifig != null) {
						IFigure startFig = eventFigures.get(start);
						Rectangle startBounds = startFig.getBounds();
						int ifigY = startBounds.y + startBounds.height;
						ifig.setBounds(new Rectangle(x, ifigY, indent, y - ifigY));
					}
				}
				if (t.getStart() == t) {
					x += indent;
				}
				y += d.height;
			}
			
			Rectangle invisible = new Rectangle(r.x, r.y, 0, 0);
			for (TraceEvent t : subsumedEvents) {
				IFigure f = eventFigures.get(t);
				f.setBounds(invisible);
				if (t.getStart() == t) {
					IFigure ifig = intervalFigures.get(t);
					if (ifig != null) {
						ifig.setBounds(invisible);
					}
				}
			}
		}
	}

	public void export(IExportSink sink) {
		for (TraceEvent t : getState().getEventsWithSubsumption()) {
			IFigure f = eventFigures.get(t);
			if (f instanceof IInteractiveFigure) {
				IInteractiveFigure ifig = (IInteractiveFigure)f;
				ifig.export(sink);
			}
		}
	}
	
	/**
	 * This is a callback that will allow us
	 * to create the viewer and initialize it.
	 */
	public void createPartControl(Composite parent) {
		figure = new Figure();
		figure.setLayoutManager(new TimelineLayout());

		setupControl(parent, figure);
		
		getState().addObserver(observer);
		contributeToActionBars();

		setConnectionState(getState().isRunning());
	}

	private void contributeToActionBars() {
		IActionBars bars = getViewSite().getActionBars();
		makeActions();
		for (Action a : queryActions) {
			bars.getMenuManager().add(a);
		}
		bars.getToolBarManager().add(connectAction);
		bars.getToolBarManager().add(disconnectAction);
	}

	private void makeActions() {
		connectAction = new Action() {
			public void run() {
				showConnectionDialog();
			}
		};
		connectAction.setText("Connect");
		connectAction.setToolTipText("Connect to a Chronicle database");
		connectAction.setImageDescriptor(PlatformUI.getWorkbench().getSharedImages().
			getImageDescriptor(ISharedImages.IMG_OBJS_INFO_TSK));
		
		disconnectAction = new Action() {
			public void run() {
				getState().stop();
			}
		};
		disconnectAction.setText("Disconnect");
		disconnectAction.setToolTipText("Disconnect the Chronicle database");
		disconnectAction.setImageDescriptor(PlatformUI.getWorkbench().getSharedImages().
			getImageDescriptor(ISharedImages.IMG_OBJS_INFO_TSK));		

		queryActions = new ArrayList<Action>();
		IExtensionPoint queryExtensionPoint = RegistryFactory.getRegistry().
		    getExtensionPoint("org.ocallahan.chronomancer.queries");
		if (queryExtensionPoint != null) {
		    for (IExtension e : queryExtensionPoint.getExtensions()) {
		        for (IConfigurationElement elem : e.getConfigurationElements()) {
		        	if (elem.getName().equals("query")) {
		        		queryActions.add(createActionForQueryFactory(elem));
		        	}
		        }
		    }
		}
    }

	private Action createActionForQueryFactory(final IConfigurationElement elem) {
		final String name = elem.getAttribute("name");
		Action a = new Action() {
			public void run() {
	        	Object o;
				try {
					o = elem.createExecutableExtension("class");
				} catch (CoreException e) {
					return;
				}
	        	if (o instanceof IEventQueryFactory) {
	        		IEventQueryFactory factory = (IEventQueryFactory)o;
	        		QueryCreationDialog dialog =
	        			new QueryCreationDialog(control.getShell(), name,
	        					getState(), factory, null);
	        		dialog.open();
	        	}
			}
		};
		a.setText("Search " + name);
		a.setToolTipText("Add " + name + " to the timeline");
		a.setImageDescriptor(PlatformUI.getWorkbench().getSharedImages().
			getImageDescriptor(ISharedImages.IMG_OBJS_INFO_TSK));		
		return a;
	}
	
	private void showConnectionDialog() {
		InputDialog dialog = new InputDialog(control.getShell(),
		    "Connect To Chronicle",
		    "Enter a command to connect to the Chronicle database:",
		    "/Users/roc/bin/chronicle-query", null);
		if (dialog.open() == Window.OK) {
			getState().start(dialog.getValue().split(" "));
		}
	}
}
