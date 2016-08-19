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

package org.ocallahan.chronomancer.queries;

import java.util.ArrayList;
import java.util.HashSet;

import org.eclipse.jface.fieldassist.ContentProposalAdapter;
import org.eclipse.jface.fieldassist.IContentProposal;
import org.eclipse.jface.fieldassist.IContentProposalProvider;
import org.eclipse.jface.fieldassist.TextContentAdapter;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.ocallahan.chronicle.AutocompleteQuery;
import org.ocallahan.chronicle.Function;
import org.ocallahan.chronicle.LookupFunctionsQuery;
import org.ocallahan.chronicle.MMapInfo;
import org.ocallahan.chronicle.MemRange;
import org.ocallahan.chronicle.Query;
import org.ocallahan.chronicle.ScanExecQuery;
import org.ocallahan.chronicle.ScanQuery;
import org.ocallahan.chronicle.AutocompleteQuery.Kind;
import org.ocallahan.chronomancer.IEventQuery;
import org.ocallahan.chronomancer.IEventQueryFactory;
import org.ocallahan.chronomancer.IReceiver;
import org.ocallahan.chronomancer.State;
import org.ocallahan.chronomancer.TraceEvent;
import org.ocallahan.chronomancer.events.FunctionCall;

public class FunctionCalls implements IEventQueryFactory {
    public Configurator createQueryConfigurator(Composite parent,
    		State st, IEventQuery base) {
    	return new Config(parent, st);
    }
    
    static class FunctionAutocomplete
        implements IContentProposalProvider {
    	public FunctionAutocomplete(State st) {
    		this.st = st;
    	}
    	
    	public synchronized IContentProposal[] getProposals(String contents, 
    			final int position) {
    		AutocompleteQuery q = new AutocompleteQuery(st.getSession(),
    				contents.substring(0, position), true, 0, 100,
    				new AutocompleteQuery.Kind[] { AutocompleteQuery.Kind.FUNCTION },
    				new AutocompleteQuery.Listener() {
    			public void notifyAutocompleteResult(AutocompleteQuery q, String name, Kind kind) {
    				synchronized (FunctionAutocomplete.this) {
        				results.add(name);
    				}
    			}
    			public void notifyDone(AutocompleteQuery q, boolean complete) {
    				synchronized (FunctionAutocomplete.this) {
    					FunctionAutocomplete.this.notify();
    				}
    			}
    		});
    		q.send();
    		results.clear();
			try {
				wait(50);
			} catch (InterruptedException e) {
			}
			q.cancel();
    		IContentProposal[] proposals = new IContentProposal[results.size()];
    		int i = 0;
    		for (final String result : results) {
    			proposals[i++] = new IContentProposal() {
    				public String getContent() { return result.substring(position); }
    				public int getCursorPosition() { return result.length(); }
    				public String getDescription() { return null; }
    				public String getLabel() { return result; }
    			};
    		}
    		return proposals;
    	}
    	
    	private State st;
    	private ArrayList<String> results = new ArrayList<String>();
    }
    
    static class Config extends Composite implements Configurator {
    	private Text t;
    	
    	Config(Composite parent, State st) {
    		super(parent, SWT.NULL);
    		
    		GridData data = new GridData(GridData.FILL_HORIZONTAL);
    		data.grabExcessHorizontalSpace = true;
    		setLayoutData(data);
    		
    	    generateChildren(st);
    		GridLayoutFactory gridFactory = GridLayoutFactory.swtDefaults();
    		gridFactory.numColumns(2);
    		gridFactory.generateLayout(this);
    	}
    	
    	protected void generateChildren(State st) {
    		Label l = new Label(this, SWT.RIGHT);
    		l.setText("Select function:");

    		t = new Text(this, SWT.BORDER);
            new ContentProposalAdapter(t, new TextContentAdapter(),
    					new FunctionAutocomplete(st), null, null);
            GridData data = new GridData(GridData.FILL_HORIZONTAL);
            data.grabExcessHorizontalSpace = true;
            t.setLayoutData(data);
    	}
    	
    	public void cancel() {
    	}
    	public IEventQuery create() {
    		return new FunCallQuery(t.getText());
    	}
    }
    
    static class FunCallQuery implements IEventQuery {
    	public FunCallQuery(String name) {
    		this.name = name;
    	}
    	
    	synchronized public void runQuery(final IReceiver<TraceEvent> receiver,
    			final State st) {
    		LookupFunctionsQuery q = new LookupFunctionsQuery(st.getSession(), name,
    				new LookupFunctionsQuery.Listener() {
                public void notifyFunctionResult(LookupFunctionsQuery q, final Function function) {
                	long entry = function.getEntryPoint();
                	ScanExecQuery scan = new ScanExecQuery(st.getSession(),
                			function.getStartTStamp(), function.getEndTStamp(),
                			new MemRange[] { new MemRange(entry, entry + 1) },
                			ScanQuery.Termination.NONE,
                			new ScanExecQuery.Listener() {
                		public void notifyExecResult(ScanExecQuery q, long tStamp, long start, long end) {
                			FunctionCall.add(st, FunCallQuery.this, receiver, function, tStamp);
                		}
                		public void notifyDone(ScanQuery q, boolean complete) {
                			synchronized (FunCallQuery.this) {
                				queries.remove(q);
                			}
                		}
                		public void notifyMMapResult(ScanQuery q, long tStamp, long start, long end, MMapInfo info) {
                		}
                	});
                	synchronized (FunCallQuery.this) {
                	    queries.add(scan);
                	    scan.send();
                	}
    			}
                public void notifyDone(LookupFunctionsQuery q, boolean complete) {
                    synchronized (FunCallQuery.this) {
                	    queries.remove(q);
                    }
    			}
    		});
    		queries.add(q);
    		q.send();
    	}
    	
    	synchronized public void stopQuery(State st) {
    		for (Query q : queries) {
    			q.cancel();
    		}
    	}
    	
    	String name;
    	HashSet<Query> queries = new HashSet<Query>();
    }
}
