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

package org.ocallahan.chronomancer.typeRenderers;

import org.ocallahan.chronicle.OffsetDataSource;
import org.ocallahan.chronicle.Type;
import org.ocallahan.chronomancer.CompoundBuilder;
import org.ocallahan.chronomancer.DataContext;
import org.ocallahan.chronomancer.IInteractiveFigure;
import org.ocallahan.chronomancer.IReceiver;
import org.ocallahan.chronomancer.ITypeRenderer;
import org.ocallahan.chronomancer.ITypeRendererFactory;
import org.ocallahan.chronomancer.RenderedData;
import org.ocallahan.chronomancer.State;

public class ArrayRenderer implements ITypeRenderer {
	private int fixedLength;
	
	public ArrayRenderer(int fixedLength) {
		this.fixedLength = fixedLength;
	}
	
	public static final int UNKNOWN_LENGTH = -1;
	
	public ArrayRenderer() {
		this(UNKNOWN_LENGTH);
	}

	public String getName() {
		return "Array";
	}
	
	public static final int DEFAULT_SHOW_LENGTH = 5;
	
	public static class ArrayIndex extends DataContext {
		private int index;
		
		public ArrayIndex(DataContext parent, int index) {
		    super(parent);
		    this.index = index;
		}
		
		@Override
		public String toString() {
			return getParent().toString() + "[" + index + "]";
		}
	}
	
	public void renderType(State state, RenderedData rd,
			IReceiver<IInteractiveFigure> receiver) {
		Type.Array arrayType = (Type.Array)rd.getBareType();
		Integer length = arrayType.getLength();
		int len;
		if (fixedLength != UNKNOWN_LENGTH) {
			len = fixedLength;
		} else if (length != null) {
			len = length.intValue();
		} else {
			len = DEFAULT_SHOW_LENGTH;
		}
		Type innerType = arrayType.getInnerType();
		int innerSize = innerType.getSize();
		
		CompoundBuilder builder = new CompoundBuilder(receiver);
		for (int i = 0; i < len; ++i) {
			ArrayIndex indexContext = new ArrayIndex(rd.getContext(), i);
		    state.getTypeRenderer().renderType(innerType, rd.getTStamp(),
		    		new OffsetDataSource(rd.getSource(), i*innerSize),
		    		indexContext, builder.addChild(indexContext));
		}
		builder.finishAddingChildren();
	}
	
	private static ArrayRenderer renderer = new ArrayRenderer();

	public static class Factory extends ITypeRendererFactory.Stub {
		public ITypeRenderer getRendererFor(Type t, DataContext context) {
			if (!(t instanceof Type.Array))
				return null;
			return renderer;
		}
	}
}
