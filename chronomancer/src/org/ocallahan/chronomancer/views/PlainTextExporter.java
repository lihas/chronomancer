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

package org.ocallahan.chronomancer.views;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Iterator;

import org.ocallahan.chronomancer.IExportSink;

public class PlainTextExporter implements IExportSink {
	private int maxWidth = 72;
	private int indentAmount = 2;
	private int positionOnLine;
	private ArrayList<Container> stack = new ArrayList<Container>();
	private Writer w;
	private ArrayList<IOException> exs = new ArrayList<IOException>();
	
	public PlainTextExporter(Writer w) {
		this.w = w;
	}
	
	static class Container {
		Container(String header, String separator, String trailer) {
			this.header = header;
			this.separator = separator;
			this.trailer = trailer;
		}
		String header;
		String separator;
		String trailer;
		ArrayList<Object> children = new ArrayList<Object>();
	}
	
	public void beginContainer(String name, String header, String separator,
			String trailer) {
		stack.add(new Container(header, separator, trailer));
	}
	
	public void endContainer() {
		Container c = stack.remove(stack.size() - 1);
		if (stack.isEmpty()) {
			try {
				output(c, 0, 0);
				writeEndLine();
			} catch (IOException ex) {
				exs.add(ex);
			}
		} else {
			stack.get(stack.size() - 1).children.add(c);
		}
	}
	
	public void writeLeaf(String name, String s) {
		if (stack.isEmpty()) {
			try {
				writeString(s);
				writeEndLine();
			} catch (IOException ex) {
				exs.add(ex);
			}
		} else {
			stack.get(stack.size() - 1).children.add(s);
		}
	}
	
	private void writeIndent(int indent) throws IOException {
		if (positionOnLine != 0)
			throw new IllegalArgumentException("Not at start of line");
		for (int i = 0; i < indent; ++i) {
			w.write(' ');
		}
		positionOnLine = indent;
	}
	
	private void writeString(String s) throws IOException {
		w.write(s);
		positionOnLine += s.length();
	}
	
	private void writeEndLine() throws IOException {
		w.write('\n');
		positionOnLine = 0;
	}
	
	private void tryEndLine(int indent) throws IOException {
		if (positionOnLine <= indent)
			return;
		writeEndLine();
		writeIndent(indent);
	}

	private void output(Container c, int indent, int trailing) throws IOException {
		writeString(c.header);
		int ourTrailing = trailing + c.trailer.length();
		int childIndent = indent + indentAmount;
		for (int i = 0; i < c.children.size(); ++i) {
			Object o = c.children.get(i);
			String afterSeparator = i == c.children.size() - 1 ? "" : c.separator;
			int childTrailing = ourTrailing + afterSeparator.length();
			if (o instanceof String) {
				String s = (String)o;
				// put it on the current line if it fits, otherwise start a new line
				// for it
				if (positionOnLine + s.length() + childTrailing <= maxWidth) {
					// The string fits, we're OK
				} else {
					tryEndLine(childIndent);
				}
				writeString(s);
			} else {
				Container child = (Container)o;
				int width = getWidth(child);
				if (positionOnLine + width + childTrailing <= maxWidth) {
					// The child fits right here, so let's just do it
				} else if (childIndent + width + childTrailing <= maxWidth) {
					// The child fits on a new line, so break and put it there
					tryEndLine(childIndent);
				} else {
					// The child will span multiple lines. Don't bother trying
					// to break yet.
				}
				output(child, childIndent, childTrailing);
			}
			writeString(afterSeparator);
		}
		writeString(c.trailer);
	}
	
	/**
	 * @return the width of the container if it were to be placed on a single line
	 */
	private static int getWidth(Container c) {
		int childCount = c.children.size();
		int sepCount = childCount > 0 ? childCount - 1 : 0;
		int width = c.header.length() + c.trailer.length() + sepCount*c.separator.length();
		for (Object o : c.children) {
			if (o instanceof String) {
				width += ((String)o).length();
			} else {
				width += getWidth((Container)o);
			}
		}
		return width;
	}
	
	public Iterator<IOException> finish() {
		try {
			w.flush();
		} catch (IOException e) {
			exs.add(e);
		}
		return exs.iterator();
	}
}
