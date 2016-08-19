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

package org.ocallahan.chronomancer.events;

import org.ocallahan.chronicle.Function;
import org.ocallahan.chronicle.GetVariablesQuery;
import org.ocallahan.chronicle.IDataSource;
import org.ocallahan.chronicle.Type;
import org.ocallahan.chronicle.Variable;
import org.ocallahan.chronicle.VariableDataSource;
import org.ocallahan.chronomancer.CompoundBuilder;
import org.ocallahan.chronomancer.DataContext;
import org.ocallahan.chronomancer.EmptyFigure;
import org.ocallahan.chronomancer.IInteractiveFigure;
import org.ocallahan.chronomancer.IReceiver;
import org.ocallahan.chronomancer.State;
import org.ocallahan.chronomancer.TextExportDelimiters;
import org.ocallahan.chronomancer.TypeRendererManager;

public class FunctionParameters {    
    public static class FunctionContext extends DataContext {
    	private Function function;
    	private long invocationTStamp;
    	
    	/**
    	 * @param function the function that was invoked
    	 * @param invocationTStamp the timestamp of the execution of its
    	 * first instruction.
    	 */
    	public FunctionContext(Function function, long invocationTStamp) {
    		super(null);
    		this.function = function;
    		this.invocationTStamp = invocationTStamp;
    	}
    	
    	public String toString() {
    		if (function == null)
    			return "<root>";
    		return function.toString() + "@" + invocationTStamp;
    	}
    }

    public static class ParameterContext extends DataContext {
		private Variable param;
		
		public ParameterContext(DataContext parent, Variable param) {
			super(parent);
			this.param = param;
		}
		
		@Override
		public String toString() {
			return getParent().toString() + ":" + param.toString();
		}
	}
	
    // call this on any thread
	public static void renderVariable(final State state, final long tStamp,
			final Variable v, final IReceiver<IInteractiveFigure> receiver,
			final DataContext context) {
		// Create the source now because it executes queries on creation
		final IDataSource source =
			new VariableDataSource(state.getSession(), tStamp, v);
		v.getType().realize(state.getSession(), new Type.Receiver() {
			public void receive(final Type t) {
				state.getDisplay().asyncExec(new Runnable() {
			    	public void run() {
			    		IDataSource src = TypeRendererManager.getEagerSource(source, t);
			    		state.getTypeRenderer().renderType(t, tStamp, src, context, receiver);
			    	}
			    });
			}
		});
	}
	
	private static TextExportDelimiters PARAMETER_LIST_DELIMITERS =
		new TextExportDelimiters("params", "(", ")", ", ");
	private static TextExportDelimiters PARAMETER_DELIMITERS =
		new TextExportDelimiters("param", "", "", "=");
	
	/**
	 * Callers to this function might want to think about advancing tStamp to
	 * the end of the function prologue, if tStamp is the start of the function
	 * call.
	 */
    public static void renderParameters(final State state, final long tStamp,
    		final IReceiver<IInteractiveFigure> receiver, final DataContext context) {
    	GetVariablesQuery q = new GetVariablesQuery(state.getSession(),
    			tStamp, GetVariablesQuery.Kind.PARAMETERS,
    			new GetVariablesQuery.Listener() {
    		public void notifyDone(GetVariablesQuery q, final boolean complete,
    				final Variable[] vars) {
				state.getDisplay().asyncExec(new Runnable() {
					public void run() {
		    			if (!complete) {
							receiver.receive(new EmptyFigure());
		    			} else {
		        			CompoundBuilder builder = new CompoundBuilder(receiver);
		        			builder.setTextExportDelimiters(PARAMETER_LIST_DELIMITERS);
		        			builder.setChildLabelDelimiters(PARAMETER_DELIMITERS);
		        			for (Variable v : vars) {
		        				renderVariable(state, tStamp, v,
		        					builder.addChild(new ParameterContext(context, v),
		        							         v.toString()),
		                            new ParameterContext(context, v));
		        			}
		        			builder.finishAddingChildren();
		    			}
					}
				});
    		}
    	});
    	q.send();
    }
}
