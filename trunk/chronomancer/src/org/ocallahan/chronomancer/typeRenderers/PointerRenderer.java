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

package org.ocallahan.chronomancer.typeRenderers;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.draw2d.MouseEvent;
import org.eclipse.draw2d.MouseListener;
import org.eclipse.draw2d.MouseMotionListener;
import org.ocallahan.chronicle.Type;
import org.ocallahan.chronomancer.AbstractLocation;
import org.ocallahan.chronomancer.AbstractLocationMap;
import org.ocallahan.chronomancer.DataContext;
import org.ocallahan.chronomancer.IInteractiveFigure;
import org.ocallahan.chronomancer.IReceiver;
import org.ocallahan.chronomancer.ITypeRenderer;
import org.ocallahan.chronomancer.ITypeRendererFactory;
import org.ocallahan.chronomancer.RenderedData;
import org.ocallahan.chronomancer.State;
import org.ocallahan.chronomancer.StringFigure;
import org.ocallahan.chronomancer.UIDataSink;

public class PointerRenderer implements ITypeRenderer {
	private boolean isReference;
	
	public PointerRenderer(boolean isReference) {
		this.isReference = isReference;
	}

	public String getName() {
		return isReference ? "Reference" : "Pointer";
	}
	
	private static HashMap<AbstractLocation,Set<Figure>> figures =
		new HashMap<AbstractLocation,Set<Figure>>();
    private static HashSet<AbstractLocation> hoveredLocations =
    	new HashSet<AbstractLocation>();
	
	public static class Figure extends StringFigure
	    implements AbstractLocationMap.Receiver, MouseMotionListener,
	        MouseListener {
		private AbstractLocation location;
		private State state;
		private DataContext context;
		private Type type;
		private boolean pendingDoubleClick;
		
	    public Figure(String string, State state, DataContext context, Type type) {
			super(string);
			this.state = state;
			this.context = context;
			this.type = type;
			addMouseMotionListener(this);
			addMouseListener(this);
		}
	    
	    public void receive(AbstractLocation l) {
	    	this.location = l;
	    	Set<Figure> set = figures.get(l);
	    	if (set == null) {
	    		set = new HashSet<Figure>();
	    		figures.put(l, set);
	    	}
	    	set.add(this);
	    	setColors();
	    	if (pendingDoubleClick) {
	    		activateLocation();
	    	}
	    }
	    
	    @Override
	    public void addNotify() {
	    	setColors();
	    }
	    
	    @Override
	    public void removeNotify() {
	    	Set<Figure> set = figures.get(location);
	    	if (set != null) {
	    		set.remove(this);
	    		if (set.isEmpty()) {
	    			figures.remove(location);
	    		}
	    	}
	    	super.removeNotify();
	    }

		public void mouseDragged(MouseEvent me) {}
		public void mouseHover(MouseEvent me) {}
		public void mouseMoved(MouseEvent me) {}
		public void mousePressed(MouseEvent me) {}
		public void mouseReleased(MouseEvent me) {}

		public void mouseDoubleClicked(MouseEvent me) {
			if (location == null) {
				pendingDoubleClick = true;
				return;
			}
			activateLocation();
		}

		public void mouseEntered(MouseEvent me) {
			if (location != null) {
				hoveredLocations.add(location);
				for (Figure f : figures.get(location)) {
					f.setColors();
				}
			}
		}

		public void mouseExited(MouseEvent me) {
			if (location != null) {
				hoveredLocations.remove(location);
				for (Figure f : figures.get(location)) {
					f.setColors();
				}
			}
		}

		public void setColors() {
			boolean highlight = location != null &&
			    hoveredLocations.contains(location);
		    state.getColorScheme().setupPointerFigureColors(this,
		    		location != null ? location.hashCode() : 0, highlight);
		}
		
		public void activateLocation() {
			if (!(type instanceof Type.Pointer)) {
				// strange...
				return;
			}
			Type.Pointer ptr = (Type.Pointer)type;
			state.activateLocation(context, ptr.getInnerType(), location);
		}
	}
	
	public IInteractiveFigure renderPointer(AbstractLocation location,
			long address, State state, DataContext context, Type.Pointer type) {
		Figure f = new Figure("0x" + Long.toHexString(address), state,
				              context, type);
		f.receive(location);
		return f;
	}

	public void renderType(final State state, final RenderedData rd,
			final IReceiver<IInteractiveFigure> receiver) {
		rd.getSource().read(0, rd.getBareType().getSize(), new UIDataSink(state) {
			public void receiveOnUIThread(byte[] data, boolean[] valid) {
				if (!RenderedData.allValid(valid)) {
					receiver.receive(new InvalidDataFigure());
					return;
				}

				data = state.getSession().getArchitecture().toBigEndian(data);
				if ((data[0] & 0x80) != 0) {
					byte[] newData = new byte[data.length + 1];
					System.arraycopy(data, 0, newData, 1, data.length);
					data = newData;
				}
				BigInteger val = new BigInteger(data);
				Figure figure = new Figure("0x" + val.toString(16), state,
						rd.getContext(), rd.getType().getBareType());
			    state.getLocationMap().getLocation(rd.getTStamp(), val.longValue(),
			    		figure);
				receiver.receive(figure);
			}
		});
	}
	
	private static PointerRenderer ptrRenderer = new PointerRenderer(false);
	private static PointerRenderer refRenderer = new PointerRenderer(true);

	public static class Factory extends ITypeRendererFactory.Stub {
		public ITypeRenderer getRendererFor(Type t, DataContext context) {
			if (!(t instanceof Type.Pointer))
				return null;
			Type.Pointer ptrT = (Type.Pointer)t;
			return ptrT.isReference() ? refRenderer : ptrRenderer;
		}
	}
}
