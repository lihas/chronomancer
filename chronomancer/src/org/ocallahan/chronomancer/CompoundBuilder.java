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

public class CompoundBuilder {
    private SequenceFigure compound = new SequenceFigure();
    private IReceiver<IInteractiveFigure> receiver;
    private int outstandingRequests = 1;
    private TextExportDelimiters childLabelDelimiters = CHILD_LABEL_DELIMITERS;
    private ArrayList<IInteractiveFigure> children = new ArrayList<IInteractiveFigure>();
    
    private static TextExportDelimiters CHILD_LABEL_DELIMITERS =
    	new TextExportDelimiters("label", "", "", " ");
    
    /**
     * This should only be used on the UI thread.
     * @param receiver
     */
    public CompoundBuilder(IReceiver<IInteractiveFigure> receiver) {
    	this.receiver = receiver;
    	compound.setGap(STANDARD_GAP);
    }
    
    public void setTextExportDelimiters(TextExportDelimiters delimiters) {
    	compound.setTextExportDelimiters(delimiters);
    }
    
    public void setChildLabelDelimiters(TextExportDelimiters delimiters) {
    	childLabelDelimiters = delimiters;
    }
        
    private void notifyDoneRequests() {
    	for (IInteractiveFigure f : children) {
    		compound.add(f);
    	}
    	receiver.receive(compound);
    }
    
    // Call this on the UI thread
    public IReceiver<IInteractiveFigure> addChild(final DataContext context,
        final String label) {
    	final int index = children.size();
    	children.add(null);
    	++outstandingRequests;

    	return new IReceiver<IInteractiveFigure>() {
    		public void receive(IInteractiveFigure figure) {
    			if (label != null) {
    				DelegatingSequenceFigure sequence = new DelegatingSequenceFigure(figure);
    				sequence.setGap(STANDARD_GAP);
    				sequence.setTextExportDelimiters(childLabelDelimiters);
    				StringFigure labelFigure = new StringFigure(label);
    				sequence.add(labelFigure);
    				sequence.add(figure);
    				figure = sequence;
    			}
    			
    			context.setupFigureStyle(figure);
    			if (children.get(index) != null)
    				throw new IllegalArgumentException("Child at index " + index + " received twice!");
    			children.set(index, figure);
    			completeRequest();
    		}
    	};
    }
    
    public IReceiver<IInteractiveFigure> addChild(final DataContext context) {
    	return addChild(context, null);
    }
    
    private void completeRequest() {
		--outstandingRequests;
		if (outstandingRequests == 0) {
			notifyDoneRequests();
		}
    }
    
    // Call this on the UI thread
    public void finishAddingChildren() {
    	completeRequest();
    }

    public static final int STANDARD_GAP = 4;
}
