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

import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.RegistryFactory;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.ocallahan.chronicle.EagerDataSource;
import org.ocallahan.chronicle.IDataSource;
import org.ocallahan.chronicle.Type;
import org.ocallahan.chronomancer.events.DataWriteEvent;
import org.ocallahan.chronomancer.typeRenderers.InvalidDataFigure;

/**
 * Generic engine for rendering types.
 * @author roc
 *
 */
public class TypeRendererManager {
    private State state;
	
	public TypeRendererManager(State state) {
    	this.state = state;
    }
	
    private static Type getPassThroughInnerType(Type t) {
		if (t instanceof Type.Typedef)
			return ((Type.Typedef)t).getInnerType();
		if (t instanceof Type.Annotation)
			return ((Type.Annotation)t).getInnerType();
		return null;
	}
    
    private void generateCandidatesInternal(Type t, DataContext context,
    		TypeRendererCollection renderers, IExtensionPoint extensionPoint,
    		int passThroughCount) {
    	String typeName = t.getIdentifier().toString();
    	String typeKind = t.getTypeKind();
    	for (IExtension e : extensionPoint.getExtensions()) {
    		for (IConfigurationElement elem : e.getConfigurationElements()) {
    			if (elem.getName().equals("typeRenderer")) {
    				String elemTypeKind = elem.getAttribute("typeKind");
    				if (elemTypeKind != null) {
    					if (!elemTypeKind.equals(typeKind))
    						continue;
    				}
    				String elemTypeName = elem.getAttribute("typeName");
    				if (elemTypeName != null) {
    					if (!elemTypeName.equals(typeName))
    						continue;
    				}
    				
    				ITypeRendererFactory f;
    				try {
    					f = (ITypeRendererFactory)elem.createExecutableExtension("class");
    				} catch (CoreException ex) {
    					Activator.log("Cannot instantiate class " + elem.getAttribute("class"),
    							      ex);
    					continue;
    				}
    				
    				ITypeRenderer r = f.getRendererFor(t, context);
    				if (r == null)
    					continue;
    				
    				renderers.addRenderer(t, context, r, f, elemTypeName != null,
    						elemTypeKind != null, passThroughCount);
    			}
    		}
    	}
    }
    
    private List<ITypeRenderer> generateCandidates(Type t, DataContext context) {
    	TypeRendererCollection renderers = new TypeRendererCollection();
    	
		IExtensionPoint extensionPoint = RegistryFactory.getRegistry().
            getExtensionPoint("org.ocallahan.chronomancer.typeRenderers");
	    if (extensionPoint != null) {
	    	int passThroughCount = 0;
	    	while (true) {
	    		generateCandidatesInternal(t, context, renderers, extensionPoint,
	    				passThroughCount);
	            final Type inner = getPassThroughInnerType(t);
	            if (inner == null)
	            	break;
	            t = inner;
	            ++passThroughCount;
	    	}
	    }
	    
	    return renderers.getOrderedRenderers();
	}

    public static IDataSource getEagerSource(IDataSource source, Type t) {
    	int size = t.getSize();
    	if (size == Type.UNKNOWN_SIZE)
    		return source;
    	return new EagerDataSource(source, size);
    }
    
    /**
     * Request rendering of a type t based on a given DataSource in a given
     * DataContext. Asynchronously a DataFigure is created and handed to the
     * receiver. This call and the FigureReceiver callback both happen on the
     * UI thread.
     * If there is a failure, we pass null to the receiver.
     */
	public void renderType(Type t, long tStamp, IDataSource source,
    		DataContext context, final IReceiver<IInteractiveFigure> receiver) {
		final RenderedData rd = new RenderedData(context, t, source, tStamp);
		List<ITypeRenderer> candidates = generateCandidates(t, context);
		IReceiver<IInteractiveFigure> intermediate = new IReceiver<IInteractiveFigure>() {
			public void receive(IInteractiveFigure figure) {
				figure.getProps().put(rd);
				receiver.receive(figure);
			}
		};
		if (candidates.isEmpty()) {
			intermediate.receive(new InvalidDataFigure());
			return;
		}

		ITypeRenderer r = candidates.get(0);
		r.renderType(state, rd, intermediate);
	}
	
	public List<ITypeRenderer> getRenderers(RenderedData rd) {
		return generateCandidates(rd.getType(), rd.getContext());
	}
	
	public Action makeTypeRendererAction(ITypeRenderer r, DataContext context,
			IInteractiveFigure interactive) {
		Action a = new Action() {
			@Override
			public void run() {
				// TODO change the figure
			}
		};
		a.setText("View As " + r.getName());
		a.setToolTipText("View " + context.toString() + " as a " + r.getName());
		return a;
	}
	
	public void contributeTypeRenderers(MenuManager menuMgr, RenderedData rd,
			                            IInteractiveFigure interactive) {
		for (ITypeRenderer r : getRenderers(rd)) {
			menuMgr.add(makeTypeRendererAction(r, rd.getContext(), interactive));
		}
	}
	
	public IDataSource.TStampReceiver makeWriteEventReceiver(final RenderedData rd) {
		return new IDataSource.TStampReceiver() {
			public void receiveChange(final long tStamp, long offset,
					long length) {
				// We are on the session thread, but we need to on the main thread
				// to mess with events
				state.getDisplay().asyncExec(new Runnable() {
					public void run() {
						DataWriteEvent event = new DataWriteEvent(tStamp,
								rd.getContext(), rd.getSource(), rd.getType());
						state.addEvent(event);
						state.setCurrentEvent(event);
					}
				});
			}
			public void receiveEndOfScope(long tStamp) {
			}
		};
	}
	
	public Action makeFindPrevWrite(final RenderedData rd) {
		Action a = new Action() {
			@Override
			public void run() {
				rd.getSource().findPreviousChangeTStamp(0, rd.getType().getSize(),
						makeWriteEventReceiver(rd));
			}
		};
		a.setText("Find Last Write");
		a.setToolTipText("Find the last write to " + rd.getContext().toString());
		return a;
	}
	
	public Action makeFindNextWrite(final RenderedData rd) {
		Action a = new Action() {
			@Override
			public void run() {
				rd.getSource().findNextChangeTStamp(0, rd.getType().getSize(),
						makeWriteEventReceiver(rd));
			}
		};
		a.setText("Find Next Write");
		a.setToolTipText("Find the next write to " + rd.getContext().toString());
		return a;
	}
	
	public void contributeDataNavigation(MenuManager menuMgr, RenderedData rd) {
		menuMgr.add(makeFindPrevWrite(rd));
		menuMgr.add(makeFindNextWrite(rd));
	}
	
	public void contributeMenuItems(MenuManager menuMgr, IInteractiveFigure interactive) {
		RenderedData rd = interactive.getProps().get(RenderedData.class);
		if (rd != null) {
			menuMgr.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
			contributeTypeRenderers(menuMgr, rd, interactive);
			menuMgr.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
			contributeDataNavigation(menuMgr, rd);
			menuMgr.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
		}
	}
}
