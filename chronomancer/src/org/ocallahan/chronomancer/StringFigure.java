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

import org.eclipse.draw2d.FigureUtilities;
import org.eclipse.draw2d.Graphics;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Insets;

public class StringFigure extends InteractiveFigure {
	private String string;
	private String name = "leaf";
	
	private Dimension size;
	private int ascent = -1;
	
	public StringFigure(String string) {
		this.string = string;
	}
	
	public StringFigure(String string, String name) {
		this(string);
		this.name = name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public String getName() {
		return name;
	}
	
	public Dimension getMinimumSize(int wHint, int hHint) {
		return getPreferredSize(wHint, hHint);	
	}

	public Dimension getPreferredSize(int wHint, int hHint) {
		if (size == null) {
			size = FigureUtilities.getStringExtents(string, getFont());
			Insets insets = getInsets();
			size.expand(insets.getWidth(), insets.getHeight());
		}
		return size;
	}
	
	protected void paintFigure(Graphics graphics) {
		super.paintFigure(graphics);
		graphics.drawText(string, getClientArea().getTopLeft());
	}
	
	public int getAscent(int wHint) {
		if (ascent < 0) {
			ascent = FigureUtilities.getFontMetrics(getFont()).getAscent() +
			    getInsets().top;
		}
		return ascent;
	}
	
	@Override
	public void addNotify() {
		super.addNotify();
	}

	public void invalidate() {
	    size = null;
	    ascent = -1;
	}
	
	public String getString() {
		return string;
	}
	
	public void export(IExportSink sink) {
		sink.writeLeaf(getName(), getString());
	}
}
