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

import java.util.HashMap;

import org.eclipse.draw2d.Border;
import org.eclipse.draw2d.CompoundBorder;
import org.eclipse.draw2d.MarginBorder;
import org.eclipse.draw2d.SimpleEtchedBorder;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.widgets.Composite;
import org.ocallahan.chronicle.IDataSource;
import org.ocallahan.chronicle.MemoryDataSource;
import org.ocallahan.chronicle.Type;
import org.ocallahan.chronomancer.AbstractLocation;
import org.ocallahan.chronomancer.DataContext;
import org.ocallahan.chronomancer.IInteractiveFigure;
import org.ocallahan.chronomancer.IReceiver;
import org.ocallahan.chronomancer.SequenceFigure;
import org.ocallahan.chronomancer.State;
import org.ocallahan.chronomancer.StringFigure;
import org.ocallahan.chronomancer.TraceEvent;
import org.ocallahan.chronomancer.TypeRendererManager;
import org.ocallahan.chronomancer.State.Observer;
import org.ocallahan.chronomancer.typeRenderers.PointerRenderer;

public class DataView extends FigureView {
    private SequenceFigure figure;
    private StateObserver observer = new StateObserver();
    private Font labelFont;
    private HashMap<AbstractLocation,Item> items = new HashMap<AbstractLocation,Item>();
    
	class StateObserver extends Observer.Stub {
		public void notifyCurrentEventChanged(State st) {
			for (Item item : items.values()) {
				reloadItem(st, item);
			}
		}
		public void notifyReset(State st) {
			items.clear();
			figure.removeAll();
		}
		@Override
		public void notifyActivateLocation(DataContext context, Type type,
				AbstractLocation location) {
			addData(context, type, location);
		}
	}
	
	private static final Border ITEM_BORDER =
		new CompoundBorder(SimpleEtchedBorder.singleton, new MarginBorder(1));

	static class Item {
		DataContext context;
		Type type;
		AbstractLocation location;
		SequenceFigure figure;
		// We use this to track which receiver we're currently using, so
		// old obsolete receivers know to cancel themselves
		IReceiver<IInteractiveFigure> pendingReceiver;
		public Item(DataContext context, Type type, AbstractLocation location) {
			this.context = context;
			this.type = type;
			this.location = location;
			this.figure = new SequenceFigure();
			figure.setBorder(ITEM_BORDER);
		}
	}
	
	private void addData(DataContext context, Type type, AbstractLocation location) {
		Item item = items.get(location);
		if (item != null) {
			item.type = type;
			item.context = context;
		} else {
    		item = new Item(context, type, location);
    		items.put(location, item);
    		figure.add(item.figure);
		}
		reloadItem(getState(), item);
	}
	
	private IInteractiveFigure makeLabel(String label, IInteractiveFigure ptr) {
		StringFigure fig = new StringFigure(label + "*");
		SequenceFigure f = new SequenceFigure();
		fig.setFont(labelFont);
		ptr.setFont(labelFont);
		f.add(fig);
		f.add(ptr);
		return f;
	}

	private void reloadItem(State st, final Item item) {
		item.figure.removeAll();

		String label = "(" + item.type.getName() + ")";
		TraceEvent event = st.getCurrentEvent();
		if (event == null) {
			item.figure.add(makeLabel(label, null));
			return;
		}

		long tStamp = event.getTStamp();
		AbstractLocation.Home home = item.location.getHomeFor(tStamp);
		if (home == null) {
			item.figure.add(makeLabel(label, null));
			return;
		}

		long address = home.getAddress();
		IInteractiveFigure ptr = (new PointerRenderer(false)).
		    renderPointer(item.location, address, st, item.context,
		    		new Type.Pointer(st.getSession(), item.type, false));
		item.figure.add(makeLabel(label, ptr));
		IDataSource source = new MemoryDataSource(st.getSession(), tStamp,
			    address);
		item.pendingReceiver = new IReceiver<IInteractiveFigure>() {
			public void receive(IInteractiveFigure figure) {
				if (item.pendingReceiver != this)
					return;
				item.figure.add(figure);
			}
		};
		IDataSource src = TypeRendererManager.getEagerSource(source, item.type);
		st.getTypeRenderer().renderType(item.type, tStamp, src,
				item.context, item.pendingReceiver);
	}
	
	/**
	 * This is a callback that will allow us
	 * to create the viewer and initialize it.
	 */
	public void createPartControl(Composite parent) {
		State state = getState();

		figure = new SequenceFigure();
		setupControl(parent, figure);
		
		labelFont = state.getColorScheme().getLabelFont(figure.getFont());

		state.addObserver(observer);
	}
}
