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

import java.util.Collections;
import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.text.source.IVerticalRulerInfo;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.IUpdate;
import org.ocallahan.chronomancer.ISourceAnnotator;
import org.ocallahan.chronomancer.State;

public abstract class BaseLineAction extends Action implements IUpdate {
	BaseLineAction(ITextEditor editor, IVerticalRulerInfo rulerInfo) {
		this.editor = editor;
		this.rulerInfo = rulerInfo;
	}

	protected State getState() {
		return State.getState(editor.getSite().getWorkbenchWindow());
	}

	protected List<ISourceAnnotator.TStampAddrPair> getTStampAddrs() {
		ISourceAnnotator annotator = getState().getSourceAnnotator();
		if (annotator == null)
			return Collections.emptyList();
		int line = rulerInfo.getLineOfLastMouseButtonActivity();
		return annotator.getExecAnnotations(editor, line);
	}

	protected ITextEditor editor;
	protected IVerticalRulerInfo rulerInfo;
}
