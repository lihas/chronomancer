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

import java.util.ArrayList;

import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Insets;
import org.eclipse.draw2d.geometry.Rectangle;

/**
 * A figure for drawing a list of IDataFigures.
 */
public class SequenceFigure extends InteractiveFigure {
	private static TextExportDelimiters DELIMITERS =
		new TextExportDelimiters("sequence", "{", "}", ",");
	
	private TextExportDelimiters delimiters = DELIMITERS;
	private Dimension preferredSize;
	private int preferredSizeWidthHint;
	private int ascent = -1;
	private int gap = 0;
	
	public void setTextExportDelimiters(TextExportDelimiters delimiters) {
		this.delimiters = delimiters;
	}

	public Dimension getMinimumSize(int wHint, int hHint) {
		return getPreferredSize(wHint, hHint);
	}

	public Dimension getPreferredSize(int wHint, int hHint) {
		if (preferredSize == null || preferredSizeWidthHint != wHint) {
			int y = 0;
			Insets insets = getInsets();
			int innerWHint = wHint < 0 ? wHint
					: Math.max(0, wHint - insets.getWidth());
			Line line = new Line(innerWHint);
			int prefW = 0;
			for (Object child : getChildren()) {
				IInteractiveFigure d = (IInteractiveFigure)child;
				if (!line.add(d)) {
					prefW = Math.max(prefW, line.getWidth());
					if (y > 0) {
						y += gap;
					}
					y += line.reset();
					line.add(d);
				}
			}
			prefW = Math.max(prefW, line.getWidth());
			if (y > 0) {
				y += gap;
			}
			y += line.reset();
			preferredSize = new Dimension(prefW, y);
			preferredSize.expand(insets.getWidth(), insets.getHeight());
			preferredSizeWidthHint = wHint;
		}
		return preferredSize;
	}
	
	class Line {
		private ArrayList<IInteractiveFigure> figures = new ArrayList<IInteractiveFigure>();
		private int maxAscent = 0;
		private int width = 0;
		private int wHint;
		Line(int wHint) {
			this.wHint = wHint;
		}
		boolean add(IInteractiveFigure d) {
			int newWidth = width;
			if (!figures.isEmpty()) {
				newWidth += gap;
			}
			newWidth += d.getPreferredSize(wHint, -1).width;
			if (wHint >= 0 && newWidth > wHint && !figures.isEmpty())
				return false;
			width = newWidth;
			maxAscent = Math.max(maxAscent, d.getAscent(wHint));
			figures.add(d);
			return true;
		}
		int positionFigures(int x, int y) {
			int newY = y;
			for (IInteractiveFigure d : figures) {
				Dimension dim = d.getPreferredSize(wHint, -1);
				int dY = y + maxAscent - d.getAscent(wHint);
				d.setBounds(new Rectangle(x, dY, dim.width, dim.height));
				x += dim.width + gap;
				newY = Math.max(newY, dY + dim.height);
			}
			figures.clear();
			width = 0;
			maxAscent = 0;
			return newY;
		}
		int reset() {
			int newY = 0;
			for (IInteractiveFigure d : figures) {
				Dimension dim = d.getPreferredSize(wHint, -1);
				int dY =  maxAscent - d.getAscent(wHint);
				newY = Math.max(newY, dY + dim.height);
			}
			figures.clear();
			width = 0;
			maxAscent = 0;
			return newY;
		}
		int getAscent() {
			return maxAscent;
		}
		int getWidth() {
			return width;
		}
	}
	
	protected void layout() {
		Rectangle r = getClientArea();
		int y = r.y;
		Line line = new Line(r.width);
		for (Object child : getChildren()) {
			IInteractiveFigure d = (IInteractiveFigure)child;
			if (!line.add(d)) {
				if (y > r.y) {
					y += gap;
				}
				y = line.positionFigures(r.x, y);
				line.add(d);
			}
		}
		if (y > r.y) {
			y += gap;
		}
		line.positionFigures(r.x, y);
	}

	public int getAscent(int wHint) {
		if (ascent < 0) {
			Line line = new Line(wHint);
			for (Object child : getChildren()) {
				IInteractiveFigure d = (IInteractiveFigure)child;
				if (!line.add(d))
					break;
			}
			ascent = line.getAscent() + getInsets().top;
		}
		return ascent;
	}
	
	public void setGap(int gap) {
		this.gap = gap;
		revalidate();
	}

	public void invalidate() {
		super.invalidate();
	    preferredSize = null;
	    minSize = null;
	    ascent = -1;
	}

	public String getExportName() {
		return delimiters.getName();
	}
	
	public String getExportHeader() {
		return delimiters.getHeader();
	}
	
	public String getExportTrailer() {
		return delimiters.getTrailer();
	}
	
	public String getExportSeparator() {
		return delimiters.getSeparator();
	}
	
	public void export(IExportSink sink) {
		sink.beginContainer(getExportName(), getExportHeader(),
				getExportSeparator(), getExportTrailer());
		for (Object o : getChildren()) {
			IFigure figure = (IFigure)o;
			if (figure instanceof IInteractiveFigure) {
				IInteractiveFigure ifig = (IInteractiveFigure)figure;
				ifig.export(sink);
			}
		}
		sink.endContainer();
	}
}
