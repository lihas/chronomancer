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

import org.eclipse.ui.texteditor.ITextEditor;

public interface ISourceAnnotator {
	/**
	 * @return the TStampAddrPair annotations for the line. Returns an empty
	 * list if the line was not executed in the current view. The results are
	 * sorted by timestamp.
	 * @param editor
	 * @param line zero-based line index
	 */
    public List<TStampAddrPair> getExecAnnotations(ITextEditor editor, int line);
	/**
	 * @return the LoopInfo annotations for the line. Returns an empty list if
	 * the line was not executed in the current view or the execution does not
	 * contain a loop head. The results are sorted by timestamp.
	 * @param editor
	 * @param line zero-based line index
	 */
    public List<LoopInfo> getLoopAnnotations(ITextEditor editor, int line);
	/**
	 * @return the text editor containing the currently focused line, or null
	 * if there is no currently focused line or there is no editor for it.
	 */
    public ITextEditor getCurrentEditor();
	/**
	 * @return the (zero-based) line number of the currently focused line in its editor, or -1
	 * if there is no currently focused line or there is no editor for it.
	 */
    public int getCurrentLine();
    
	public static class TStampAddrPair implements Comparable<TStampAddrPair> {
		public TStampAddrPair(long tStamp, long address) {
			this.tStamp = tStamp;
			this.address = address;
		}
		private long tStamp;
		private long address;
		
		public long getTStamp() {
			return tStamp;
		}

		public long getAddress() {
			return address;
		}

		public int compareTo(TStampAddrPair o) {
			int cmp = Utils.compareLong(tStamp, o.tStamp);
			if (cmp != 0)
				return cmp;
			return Utils.compareLong(address, o.address);
		}
		
		@Override
		public boolean equals(Object o) {
			if (!(o instanceof TStampAddrPair))
				return false;
			TStampAddrPair t = (TStampAddrPair)o;
			return t.tStamp == tStamp && t.address == address;
		}
	}
	
	public static class LoopInfo extends TStampAddrPair {
		public LoopInfo(long tStamp, long address, long currentIteration, long totalIterations) {
			super(tStamp, address);
			this.currentIteration = currentIteration;
			this.totalIterations = totalIterations;
		}

		private long currentIteration;
		private long totalIterations;

		public long getCurrentIteration() {
			return currentIteration;
		}

		public long getTotalIterations() {
			return totalIterations;
		}
	}
}
