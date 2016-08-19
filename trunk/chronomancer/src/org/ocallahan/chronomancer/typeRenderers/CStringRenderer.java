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

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.util.Arrays;

import org.eclipse.draw2d.Label;
import org.ocallahan.chronicle.Architecture;
import org.ocallahan.chronicle.IDataSink;
import org.ocallahan.chronicle.IDataSource;
import org.ocallahan.chronicle.MemoryDataSource;
import org.ocallahan.chronicle.Type;
import org.ocallahan.chronomancer.DataContext;
import org.ocallahan.chronomancer.IInteractiveFigure;
import org.ocallahan.chronomancer.IReceiver;
import org.ocallahan.chronomancer.ITypeRenderer;
import org.ocallahan.chronomancer.ITypeRendererFactory;
import org.ocallahan.chronomancer.RenderedData;
import org.ocallahan.chronomancer.State;
import org.ocallahan.chronomancer.StringFigure;
import org.ocallahan.chronomancer.TypeRendererCollection;
import org.ocallahan.chronomancer.UIDataSink;

public class CStringRenderer implements ITypeRenderer {
	private Type charType;
	private int charLimit = DEFAULT_LIMIT;
	
	// These length limits all include the trailing null (if any).
	public final static int TOOLTIP_LIMIT = 4096;
	public final static int DEFAULT_LIMIT = 64;
	
	public CStringRenderer(Type charType) {
		this.charType = charType;
	}
	
	public String getName() {
		return charType.getIdentifier().getName() + "* String";
	}
	
	public static long toUnsigned(byte[] data, Architecture arch) {
		data = arch.toBigEndian(data);
		if ((data[0] & 0x80) != 0) {
			byte[] newData = new byte[data.length + 1];
			System.arraycopy(data, 0, newData, 1, data.length);
			data = newData;
		}
		return (new BigInteger(data)).longValue();
	}
	
	public static String quote(String s) {
		StringBuilder builder = new StringBuilder("\"");
		for (int i = 0; i < s.length(); ++i) {
			char ch = s.charAt(i);
			switch (ch) {
			case '\n': builder.append("\\n"); break;
			case '\f': builder.append("\\f"); break;
			case '\t': builder.append("\\t"); break;
			case '\r': builder.append("\\r"); break;
			case '\b': builder.append("\\b"); break;
			case '\\': builder.append("\\\\"); break;
			case '"': builder.append("\\\""); break;
			default: builder.append(ch); break;
			}
		}
		builder.append('"');
		return builder.toString();
	}
	
	private static Charset defaultCharset8Bit = Charset.forName("ISO-8859-1");
	private static Charset defaultCharset16BitLE = Charset.forName("UTF-16LE");
	private static Charset defaultCharset16BitBE = Charset.forName("UTF-16BE");
	
	public static String translateWithCharset(Charset charset,
			byte[] buffer, int len) {
		ByteBuffer buf = ByteBuffer.wrap(buffer, 0, len);
		CharBuffer charBuf = charset.decode(buf);
		return charBuf.toString();
	}
	
	public static String translateUCS32(byte[] buffer, int len, boolean littleEndian) {
		StringBuilder builder = new StringBuilder();
		DataInputStream stream = new DataInputStream(new ByteArrayInputStream(buffer, 0, len));
		try {
			while (stream.available() >= 4) {
				int v = stream.readInt();
				if (littleEndian) {
					v = Integer.reverseBytes(v);
				}
				builder.appendCodePoint(v);
			}
		} catch (IOException ex) {
		}
		return builder.toString();
	}
	
	public static int findNull(byte[] buffer, int size) {
		for (int i = 0; i + size <= buffer.length; ++i) {
			boolean zeroes = true;
			for (int j = 0; j < size; ++j) {
				if (buffer[i + j] != 0) {
					zeroes = false;
					break;
				}
			}
			if (zeroes)
				return i;
		}
		return buffer.length;
	}
	
	public static String makeString(Type charType, Architecture arch, byte[] buffer, int len) {
		switch (charType.getSize()) {
		case 1:
			return translateWithCharset(defaultCharset8Bit, buffer, len);
		case 2:
			return translateWithCharset(arch.isLittleEndian() ?
					defaultCharset16BitLE : defaultCharset16BitBE, buffer, len);
		case 4:
			// Grr, why doesn't Java have a Charset for this?
			return translateUCS32(buffer, len, arch.isLittleEndian());
	    default:
	    	return "?";
		}
	}
	
	public static int findValidLength(boolean[] valid) {
		for (int i = 0; i < valid.length; ++i) {
			if (!valid[i])
				return i;
		}
		return valid.length;
	}

	public static void findMemoryContainingNull(final IDataSource source, final int nullSize,
			final int length, final int remainingLength, final byte[] priorData, final IDataSink sink) {
		source.read(priorData.length, length, new IDataSink() {
			public void receive(byte[] data, boolean[] valid) {
				byte[] newData = new byte[priorData.length + data.length];
				System.arraycopy(priorData, 0, newData, 0, priorData.length);
				System.arraycopy(data, 0, newData, priorData.length, data.length);
				
				int validLen = findValidLength(valid);
				int nullLen = findNull(data, nullSize);
				if (validLen < valid.length || nullLen < data.length || remainingLength == 0) {
					boolean[] allValid = new boolean[priorData.length + valid.length];
					Arrays.fill(allValid, 0, priorData.length, true);
					System.arraycopy(valid, 0, allValid, priorData.length, valid.length);
					sink.receive(newData, allValid);
					return;
				}
				
				int newLength = Math.min(length*2, remainingLength);
				findMemoryContainingNull(source, nullSize, newLength,
						remainingLength - newLength, newData, sink);
			}
		});
	}
	
	private static byte[] EMPTY_BYTES = new byte[0];
	
	public static interface StringReceiver {
		public void receive(String result, boolean isTerminated);
	}
	
	public static void evaluateString(final State state, final Type charType,
			IDataSource source, final int maxLength, final StringReceiver receiver) {
		final int size = charType.getSize();
		int initialLength = Math.min(maxLength, 64);
		findMemoryContainingNull(source, size, initialLength,
				maxLength - initialLength, EMPTY_BYTES, new UIDataSink(state) {
			public void receiveOnUIThread(byte[] data, boolean[] valid) {
				int nullLen = findNull(data, size);
				int validLen = findValidLength(valid);
				Architecture arch = state.getSession().getArchitecture();
				receiver.receive(makeString(charType, arch, data,
						                 Math.min(nullLen, validLen)),
						nullLen + size <= validLen);
			}
		});
	}

	public static String trimTrailingNull(String s) {
		int len = s.length();
		if (len > 0 && s.charAt(len - 1) == 0)
			return s.substring(0, len - 1);
		return s;
	}
	
	public void renderType(final State state, final RenderedData rd,
			final IReceiver<IInteractiveFigure> receiver) {
		final Type.Pointer tPtr = (Type.Pointer)rd.getBareType();
		rd.getSource().read(0, tPtr.getSize(), new IDataSink() {
			public void receive(byte[] data, boolean[] valid) {
				if (!RenderedData.allValid(valid)) {
					state.getDisplay().asyncExec(new Runnable() {
						public void run() {
							receiver.receive(new InvalidDataFigure());
						}
					});
					return;
				}

				Architecture arch = state.getSession().getArchitecture();
				IDataSource source = new MemoryDataSource(state.getSession(),
						rd.getTStamp(), toUnsigned(data, arch));
				evaluateString(state, charType, source, Math.max(charLimit, TOOLTIP_LIMIT),
						new StringReceiver() {
					public void receive(String s, boolean isTerminated) {
						String quoted = quote(s.substring(0, Math.min(s.length(), charLimit)));
						if (s.length() > charLimit || !isTerminated) {
							quoted += "...";
						}
						StringFigure fig = new StringFigure(quoted);
						fig.setToolTip(new Label(s));
						receiver.receive(fig);
					}
				});
			}
		});
	}
	
	public static Type getCharType(Type t) {
		String name = t.getIdentifier().getName();
		if (name == "char" || name == "wchar_t")
			return t;
		if (t instanceof Type.Typedef)
			return getCharType(((Type.Typedef)t).getInnerType());
		if (t instanceof Type.Annotation)
			return getCharType(((Type.Annotation)t).getInnerType());
		return null;
	}
	
	public static class Factory extends ITypeRendererFactory.Stub {
		public ITypeRenderer getRendererFor(Type t, DataContext context) {
			if (!(t instanceof Type.Pointer))
				return null;
			Type.Pointer ptr = (Type.Pointer)t;
			if (ptr.isReference())
				return null;
			Type charType = getCharType(ptr.getInnerType());
			if (charType == null)
				return null;
		    return new CStringRenderer(charType);
		}
		
		@Override
		public void captureRendererPreferences(Type t, DataContext context,
				ITypeRenderer renderer, TypeRendererCollection collection) {
			for (ITypeRenderer other : collection.getRenderersByFactoryClass(PointerRenderer.Factory.class)) {
				collection.setPreferred(renderer, other);
			}
		}
	}
}
