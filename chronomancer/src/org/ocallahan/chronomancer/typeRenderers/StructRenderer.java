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

import java.util.HashMap;

import org.ocallahan.chronicle.IDataSource;
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

public class StructRenderer implements ITypeRenderer {
	private Type.Struct.Kind kind;
	
	public StructRenderer(Type.Struct.Kind kind) {
		this.kind = kind;
	}

	public String getName() {
		switch (kind) {
		case CLASS: return "Class";
		case STRUCT: return "Struct";
		case UNION: return "Union";
		default:
			throw new IllegalArgumentException("Unknown kind: " + kind);
		}
	}
	
	public static class FieldContext extends DataContext {
		private Type.Field field;
		
		public FieldContext(DataContext parent, Type.Field field) {
		    super(parent);
		    this.field = field;
		}
		
		@Override
		public String toString() {
			String ps = getParent().toString();
			if (field.isSubobject())
				return ps;
			return ps + "." + field.toString();
		}
	}
	
	private void renderSubobject(Type.Struct t, State state, long tStamp,
			IDataSource source, int offset, DataContext context,
			CompoundBuilder builder) {
		Type.Field[] fields = t.getFields();
		if (fields == null)
			return;
		for (Type.Field f : fields) {
			if (f.isSynthetic())
				continue;
			Type fieldType = f.getType();
			FieldContext fieldContext = new FieldContext(context, f);
			if (f.isSubobject()) {
				if (!(fieldType instanceof Type.Struct)) {
					// Strange, subobjects should only be structs/classes...
					// You can't inherit from base types in C++!
					continue;
				}
				renderSubobject((Type.Struct)fieldType, state, tStamp, source,
						offset + f.getByteOffset(), fieldContext, builder);
			} else {
				state.getTypeRenderer().renderType(fieldType, tStamp,
						new OffsetDataSource(source, offset + f.getByteOffset()),
						fieldContext, builder.addChild(fieldContext, f.toString()));
			}
		}
	}
	
	public void renderType(State state, RenderedData rd,
			IReceiver<IInteractiveFigure> receiver) {
		Type.Struct structType = (Type.Struct)rd.getBareType();
		CompoundBuilder builder = new CompoundBuilder(receiver);
		renderSubobject(structType, state, rd.getTStamp(), rd.getSource(), 0,
				rd.getContext(), builder);
		builder.finishAddingChildren();
	}
	
	private static HashMap<Type.Struct.Kind,StructRenderer> renderers =
		new HashMap<Type.Struct.Kind,StructRenderer>();
	static {
		for (Type.Struct.Kind k : Type.Struct.Kind.values()) {
			renderers.put(k, new StructRenderer(k));
		}
	}

	public static class Factory extends ITypeRendererFactory.Stub {
		public ITypeRenderer getRendererFor(Type t, DataContext context) {
			if (!(t instanceof Type.Struct))
				return null;
			Type.Struct structType = (Type.Struct)t;
			return renderers.get(structType.getKind());
		}
	}
}
