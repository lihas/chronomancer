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

import org.eclipse.draw2d.IFigure;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;

public class ColorScheme {
	private Display display;

	public ColorScheme(Display display) {
		this.display = display;
		WHITE = new Color(display, new RGB(0.0f, 0.0f, 1.0f));
	}
	
	private static Color WHITE;
	
	public Font getLabelFont(Font font) {
		FontData[] data = font.getFontData();
		FontData newData = new FontData(data[0].getName(), data[0].getHeight(), SWT.BOLD);
		return new Font(font.getDevice(), new FontData[] { newData });
	}
	
	public void setupDefaultFigureColors(IFigure f) {
	}
	
	public void setupFigureColors(IFigure f, int hash) {
		float hue = Math.abs(Integer.reverse(hash)) % 360;
		Color c = new Color(display, new RGB(hue, 0.2f, 1.0f));
		f.setBackgroundColor(c);
		f.setOpaque(true);
	}
	
	public void setupPointerFigureColors(IInteractiveFigure f, int hash,
			                             boolean highlight) {
		float hue = Math.abs(hash) % 360;
		RGB rgb = new RGB(hue, 1.0f, 0.6f);
		f.setForegroundColor(new Color(display, rgb));
		if (highlight) {
		    f.setBackgroundColor(WHITE);
		    f.setOpaque(true);
		} else {
    		f.setBackgroundColor(null);
    		f.setOpaque(false);
		}
	}
}
