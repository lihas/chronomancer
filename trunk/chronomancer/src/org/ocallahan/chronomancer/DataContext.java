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

import org.eclipse.draw2d.Border;
import org.eclipse.draw2d.SimpleRaisedBorder;

/**
 * A DataContext is essentially an expression that identifies a unique variable.
 * For example:
 *   nsViewManager::Refresh(invoked at timestamp 317103111) : this -> nsViewManager::mObserver
 */
public abstract class DataContext {
    private DataContext parent;

    public DataContext(DataContext parent) {
		this.parent = parent;
	}

	public DataContext getParent() {
		return parent;
	}
	
	public void setupFigureStyle(IInteractiveFigure figure) {
		figure.setBorder(getBorder());
	}
	
	public Border getBorder() {
		return STANDARD_BORDER;
	}

	public abstract String toString();
	
	private static final Border STANDARD_BORDER =
		new SimpleRaisedBorder();
}
