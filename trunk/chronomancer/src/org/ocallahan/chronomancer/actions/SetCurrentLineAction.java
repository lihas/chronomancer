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

import java.util.List;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.source.IVerticalRulerInfo;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.texteditor.AbstractRulerActionDelegate;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;
import org.ocallahan.chronomancer.ISourceAnnotator;
import org.ocallahan.chronomancer.State;
import org.ocallahan.chronomancer.events.ExecLineEvent;

public class SetCurrentLineAction extends AbstractRulerActionDelegate {
	@Override
	protected IAction createAction(final ITextEditor editor,
			final IVerticalRulerInfo rulerInfo) {
		return new Impl(editor, rulerInfo);
	}
	
	static class Impl extends BaseLineAction {
		public Impl(ITextEditor editor, IVerticalRulerInfo rulerInfo) {
			super(editor, rulerInfo);
		}

		public void update() {
			setEnabled(!getTStampAddrs().isEmpty());
			setText("Set Current Line");
		}
		
		@Override
		public void run() {
			List<ISourceAnnotator.TStampAddrPair> tStampAddrs = getTStampAddrs();
			if (tStampAddrs.isEmpty())
				return;
			
			State state = getState();
			int line = rulerInfo.getLineOfLastMouseButtonActivity();
			IEditorInput input = editor.getEditorInput();
			IDocumentProvider provider = editor.getDocumentProvider();
			IDocument doc = provider.getDocument(input);
			try {
				String lineText = doc.get(doc.getLineOffset(line), doc.getLineLength(line));
				ExecLineEvent event = new ExecLineEvent(tStampAddrs.get(0).getTStamp(), lineText);
				state.addEvent(event);
				state.setCurrentEvent(event);
			} catch (BadLocationException e) {
			}
		}
	}
}
