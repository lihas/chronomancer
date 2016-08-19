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
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.ocallahan.chronicle.Type;

public class TypeRendererCollection {
	private HashMap<Class<? extends ITypeRendererFactory>,ArrayList<ITypeRenderer>> renderers =
		new HashMap<Class<? extends ITypeRendererFactory>,ArrayList<ITypeRenderer>>();
	private HashMap<ITypeRenderer,RendererState> states = new HashMap<ITypeRenderer,RendererState>();

	private static class RendererState {
		RendererState(Type type, DataContext context, ITypeRendererFactory factory,
				      boolean nameMatch, boolean kindMatch, int passThroughCount) {
			this.type = type;
			this.context = context;
			this.factory = factory;
			this.nameMatch = nameMatch;
			this.kindMatch = kindMatch;
			this.passThroughCount = passThroughCount;
		}
		Type type;
		DataContext context;
		ITypeRendererFactory factory;
		boolean nameMatch;
		boolean kindMatch;
		int passThroughCount;
		HashSet<ITypeRenderer> supercedes = new HashSet<ITypeRenderer>();
		HashSet<ITypeRenderer> supercededBy = new HashSet<ITypeRenderer>();
		HashSet<ITypeRenderer> preferredOver = new HashSet<ITypeRenderer>();
		HashSet<ITypeRenderer> preferredUnder = new HashSet<ITypeRenderer>();
		
		int preferredUnderCount;
	}

	public void addRenderer(Type t, DataContext context,
			ITypeRenderer renderer, ITypeRendererFactory factory,
			boolean nameMatch, boolean kindMatch, int passThroughCount) {
		Class<? extends ITypeRendererFactory> cl = factory.getClass();
		ArrayList<ITypeRenderer> list = renderers.get(cl);
		if (list == null) {
			list = new ArrayList<ITypeRenderer>();
			renderers.put(cl, list);
		}
		list.add(renderer);
		states.put(renderer, new RendererState(t, context, factory,
				nameMatch, kindMatch, passThroughCount));
	}
	
	public List<ITypeRenderer> getRenderersByFactoryClass(Class<? extends ITypeRendererFactory> cl) {
		List<ITypeRenderer> list = renderers.get(cl);
		if (list == null)
			return Collections.emptyList();
		return list;
	}
	
	public void setSupercedes(ITypeRenderer superior, ITypeRenderer inferior) {
		states.get(superior).supercedes.add(inferior);
		states.get(inferior).supercededBy.add(superior);
	}
	
	public void setPreferred(ITypeRenderer superior, ITypeRenderer inferior) {
		states.get(superior).preferredOver.add(inferior);
		states.get(inferior).preferredUnder.add(superior);
	}
	
	public static int toInt(boolean b) {
		return b ? 1 : 0;
	}
	
	public static class Result {
		Result(Type type, ITypeRenderer renderer) {
			this.type = type;
			this.renderer = renderer;
		}
		public Type type;
		public ITypeRenderer renderer;
	}
	
	public List<ITypeRenderer> getOrderedRenderers() {
		for (ITypeRenderer r : states.keySet()) {
			RendererState state = states.get(r);
			state.factory.captureRendererPreferences(state.type,
					state.context, r, this);
		}
		
		// Collect all the renderers that haven't been superceded
		ArrayList<ITypeRenderer> valid = new ArrayList<ITypeRenderer>();
		for (ITypeRenderer r : states.keySet()) {
			if (states.get(r).supercededBy.isEmpty()) {
				valid.add(r);
			}
		}
		if (valid.isEmpty()) {
			// Must be some cycle of superceded renderers, which is bogus. But
			// we'll deal by just ignoring superceding relationships.
			valid.addAll(states.keySet());
		}
		
		for (ITypeRenderer r : valid) {
			for (ITypeRenderer r2 : states.get(r).preferredOver) {
				states.get(r2).preferredUnderCount++;
			}
		}

		// Now sort them by preference. We'll take a short cut and just sort
		// by the number of renderers that are preferred over each renderer.
		Collections.sort(valid, new Comparator<ITypeRenderer>() {
			public int compare(ITypeRenderer o1, ITypeRenderer o2) {
				RendererState state1 = states.get(o1);
				RendererState state2 = states.get(o2);
				int countDelta = state1.preferredUnderCount - state2.preferredUnderCount;
				if (countDelta != 0)
					return countDelta;
				int passThroughDelta = state1.passThroughCount - state2.passThroughCount;
				if (passThroughDelta != 0)
					return passThroughDelta;
				int nameMatchDelta = toInt(state2.nameMatch) - toInt(state1.nameMatch);
				if (nameMatchDelta != 0)
					return nameMatchDelta;
				int kindMatchDelta = toInt(state2.kindMatch) - toInt(state1.kindMatch);
				if (kindMatchDelta != 0)
					return kindMatchDelta;
				return o1.getName().compareTo(o2.getName());
			}
		});
		
		return valid;
	}
}
