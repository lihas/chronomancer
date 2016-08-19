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

import java.math.BigInteger;

import org.ocallahan.chronicle.Type;
import org.ocallahan.chronomancer.DataContext;
import org.ocallahan.chronomancer.IInteractiveFigure;
import org.ocallahan.chronomancer.IReceiver;
import org.ocallahan.chronomancer.ITypeRenderer;
import org.ocallahan.chronomancer.ITypeRendererFactory;
import org.ocallahan.chronomancer.RenderedData;
import org.ocallahan.chronomancer.State;
import org.ocallahan.chronomancer.StringFigure;
import org.ocallahan.chronomancer.UIDataSink;

public class IntRenderer implements ITypeRenderer {
	public String getName() {
		return "Integer";
	}
	
	public void renderType(final State state, RenderedData rd,
			final IReceiver<IInteractiveFigure> receiver) {
		final Type.Int tInt = (Type.Int)rd.getBareType();
		rd.getSource().read(0, tInt.getSize(), new UIDataSink(state) {
			public void receiveOnUIThread(byte[] data, boolean[] valid) {
				if (!RenderedData.allValid(valid)) {
					receiver.receive(new InvalidDataFigure());
					return;
				}

				data = state.getSession().getArchitecture().toBigEndian(data);
				if (!tInt.isSigned() && (data[0] & 0x80) != 0) {
					byte[] newData = new byte[data.length + 1];
					System.arraycopy(data, 0, newData, 1, data.length);
					data = newData;
				}
				BigInteger val = new BigInteger(data);
				receiver.receive(new StringFigure(val.toString()));
			}
		});
	}
	
	private static IntRenderer renderer = new IntRenderer();

	public static class Factory extends ITypeRendererFactory.Stub {
		public ITypeRenderer getRendererFor(Type t, DataContext context) {
			if (!(t instanceof Type.Int))
				return null;
			return renderer;
		}
	}
}
