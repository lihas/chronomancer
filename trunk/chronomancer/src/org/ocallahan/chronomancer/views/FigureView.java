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

import java.io.StringWriter;

import org.eclipse.draw2d.FigureCanvas;
import org.eclipse.draw2d.IFigure;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.MenuDetectEvent;
import org.eclipse.swt.events.MenuDetectListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;
import org.ocallahan.chronomancer.IExportSink;
import org.ocallahan.chronomancer.IInteractiveFigure;
import org.ocallahan.chronomancer.RenderedData;
import org.ocallahan.chronomancer.State;

public abstract class FigureView extends ViewPart {
	protected FigureCanvas control;
	private Action copyAction;

	private static <T> IInteractiveFigure findInteractiveFigureWithProp(IFigure fig,
			Class<T> propClass) {
		while (fig != null) {
			if (fig instanceof IInteractiveFigure) {
				IInteractiveFigure interactive = (IInteractiveFigure)fig;
				if (interactive.getProps().get(propClass) != null)
					return interactive;
			}
			fig = fig.getParent();
		}
		return null;
	}
	
	protected void setupFigureContextMenu() {
		final MenuManager menuMgr = new MenuManager("#PopupMenu");
		getSite().registerContextMenu(menuMgr, null);

		control.addMenuDetectListener(new MenuDetectListener() {
			public void menuDetected(MenuDetectEvent e) {
				Point pt = control.toControl(e.x, e.y);
				IFigure fig = control.getViewport().findFigureAt(pt.x, pt.y);
				IInteractiveFigure interactive =
					findInteractiveFigureWithProp(fig, RenderedData.class);
				if (interactive == null) {
					e.doit = false;
					return;
				}
				
				menuMgr.removeAll();
				State state = State.getState(getSite().getWorkbenchWindow());
				state.getTypeRenderer().contributeMenuItems(menuMgr, interactive);
				interactive.populateContextMenu(state, menuMgr);
				// required, for extensions
				// menuMgr.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
				Menu menu = menuMgr.createContextMenu(control);
				control.setMenu(menu);
			}
		});
	}
	
	public State getState() {
		return State.getState(getSite().getWorkbenchWindow());
	}

	public void setupControl(Composite parent, IFigure figure) {
		control = new FigureCanvas(parent);
		control.setContents(figure);
		control.getViewport().setContentsTracksWidth(true);
		setupFigureContextMenu();
		
		IActionBars bars = getViewSite().getActionBars();
		makeActions();
		bars.getMenuManager().add(copyAction);
	}
	
	public void export(IExportSink sink) {
		for (Object o : control.getContents().getChildren()) {
			if (o instanceof IInteractiveFigure) {
				IInteractiveFigure ifig = (IInteractiveFigure)o;
				ifig.export(sink);
			}
		}
	}
	
	private void copyContents() {
        StringWriter sw = new StringWriter();
        PlainTextExporter exporter = new PlainTextExporter(sw);
        export(exporter);
        if (exporter.finish().hasNext())
        	return;

        Clipboard clipboard = new Clipboard(getState().getDisplay());
        String plainText = sw.getBuffer().toString();
        TextTransfer textTransfer = TextTransfer.getInstance();
        clipboard.setContents(new String[] { plainText },
        		              new Transfer[] { textTransfer });
        clipboard.dispose();		
	}
	
	private void makeActions() {
		copyAction = new Action() {
			public void run() {
				copyContents();
			}
		};
		copyAction.setText("Copy Contents");
		copyAction.setToolTipText("Copy contents to the clipboard");
		copyAction.setImageDescriptor(PlatformUI.getWorkbench().getSharedImages().
			getImageDescriptor(ISharedImages.IMG_OBJS_INFO_TSK));
	}
	
	/**
	 * Passing the focus request to the viewer's control.
	 */
	public void setFocus() {
	}
}
