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

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.WeakHashMap;

import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleManager;
import org.eclipse.ui.console.MessageConsole;
import org.ocallahan.chronicle.Query;
import org.ocallahan.chronicle.Session;
import org.ocallahan.chronicle.Type;

/**
 * Only for use by the UI thread.
 * @author roc
 *
 */
public class State implements Session.Listener {
    private Session session = null;
    private Display display;
    private Process process = null;
    private TraceEvent currentEvent = null;
    private WeakHashMap<Observer,Integer> observers = new WeakHashMap<Observer,Integer>();
    private SortedSet<TraceEvent> events = new TreeSet<TraceEvent>();
    private ArrayList<IEventQuery> queries = new ArrayList<IEventQuery>();
    private IWorkbenchWindow window;
    private MessageConsole console;
    private AbstractLocationMap locationMap = new AbstractLocationMap(this);
    private ColorScheme colorScheme;
    private TypeRendererManager typeRenderer = new TypeRendererManager(this);
    private ISourceAnnotator annotator;
    
    static class EventQueryState implements Comparable<EventQueryState> {
    	public int compareTo(EventQueryState o) {
    		return id - o.id;
    	}
    	public EventQueryState(int id, IEventQuery eq) {
			this.id = id;
			this.eq = eq;
    	}
      	private int id;
      	IEventQuery eq;
      	Set<TraceEvent> events = new HashSet<TraceEvent>();
    }
    
    public State(IWorkbenchWindow window) {
    	this.window = window;
    	display = window.getShell().getDisplay();
    	colorScheme = new ColorScheme(getDisplay());
    }
    
    public void connectConsole() {
    	ConsolePlugin plugin = ConsolePlugin.getDefault();
    	IConsoleManager manager = plugin.getConsoleManager();
    	console = new MessageConsole("Chronicle", null);
    	manager.addConsoles(new IConsole[] { console });
    }
    
    public void disconnectConsole() {
    	if (console == null)
    		return;
    	ConsolePlugin plugin = ConsolePlugin.getDefault();
    	IConsoleManager manager = plugin.getConsoleManager();
    	manager.removeConsoles(new IConsole[] { console });
    	console = null;
    }

    public void writeToConsole(String message) {
		OutputStream stream = console.newOutputStream();
		try {
			Writer w = new OutputStreamWriter(stream, "UTF8");
			w.write(message);
			w.close();
		} catch (UnsupportedEncodingException e) {
		} catch (IOException e) {
		}
    }
    
    public void start(String[] command) {
    	ProcessBuilder pb = new ProcessBuilder(command);
    	try {
    		process = pb.start();
    	} catch (IOException ex) {
    		notifyMessage(Session.Severity.FATAL, "Cannot start process " + command[0] + ": " + ex, null);
    	}
	    connectConsole();
    	session = new Session(process.getInputStream(), process.getOutputStream(), this);
    	if (session.isClosed()) {
    		disconnectConsole();
    		session = null;
    	}
    }
    
    public void stop() {
    	if (process != null) {
    		process.destroy();
    		process = null;
    	}
    	if (session != null) {
    		session.close();
    		session = null;
    	}
    	events.clear();
    	queries.clear();
    	currentEvent = null;
    	disconnectConsole();
    	for (Observer o : observers.keySet()) {
    		o.notifyReset(this);
    	}
    }
    
    public ISourceAnnotator getSourceAnnotator() {
    	return annotator;
    }
    
    public void setSourceAnnotator(ISourceAnnotator annotator) {
    	this.annotator = annotator;
    }
    
    public boolean isRunning() {
    	return session != null;
    }
    
    public static interface Observer {
    	public static class Stub implements Observer {
			public void notifyStarted(State s) {}
			public void notifyCurrentEventChanged(State s) {}
		    public void notifyEventAdded(State s, TraceEvent e) {}
		    public void notifyEventRemoved(State s, TraceEvent e) {}
			public void notifyReset(State s) {}
			public void notifyQueryAdded(IEventQuery q) {}
			public void notifyQueryRemoved(IEventQuery q) {}
		    public void notifyActivateLocation(DataContext context, Type type, AbstractLocation location) {}
		}

    	public void notifyStarted(State s);
        public void notifyCurrentEventChanged(State s);
        public void notifyQueryAdded(IEventQuery q);
        public void notifyQueryRemoved(IEventQuery q);
        public void notifyEventAdded(State s, TraceEvent e);
        public void notifyEventRemoved(State s, TraceEvent e);
        // all events and queries removed, current event is null
        public void notifyReset(State s);
        public void notifyActivateLocation(DataContext context, Type type, AbstractLocation location);
    }
    
	/**
	 * 'type' is the type of the data (i.e., it might not be a pointer type)
	 */
	public void activateLocation(DataContext context, Type type,
			AbstractLocation location) {
    	for (Observer o : observers.keySet()) {
    		o.notifyActivateLocation(context, type, location);
    	}
	}
    
    public void setCurrentEvent(TraceEvent t) {
    	if (currentEvent == t)
    		return;
    	currentEvent = t;
    	for (Observer o : observers.keySet()) {
    		o.notifyCurrentEventChanged(this);
    	}
    }

    public void addEvent(TraceEvent e) {
    	events.add(e);
    	for (Observer o : observers.keySet()) {
    		o.notifyEventAdded(this, e);
    	}
    }
    
    public void removeEvent(TraceEvent e) {
    	if (e == currentEvent) {
    		setCurrentEvent(null);
    	}
    	events.remove(e);
    	for (Observer o : observers.keySet()) {
    		o.notifyEventRemoved(this, e);
    	}
    }

    public void notifyStarted() {
    	getDisplay().asyncExec(new Runnable() {
    		public void run() {
    	  	    for (Observer o : observers.keySet()) {
    			    o.notifyStarted(State.this);
    		    }
    		}
    	});
    }
    
    public void notifyMessage(final Session.Severity severity,
    		                  final String error, final Query query) {
    	getDisplay().asyncExec(new Runnable() {
    		public void run() {
    			writeToConsole(severity + ": " + error + "\n");
    	    	if (severity == Session.Severity.FATAL) {
    	    	    stop();
    	    	}
    		}
    	});
    }
    
    public void notifyReceived(final String s) {
    	Activator.log("RECEIVED: " + s);
    }
    
    public void notifySending(final String s) {
    	Activator.log("SENT: " + s);
    }

    public void addQuery(IEventQuery query) {
    	queries.add(query);
    	for (Observer o : observers.keySet()) {
    		o.notifyQueryAdded(query);
    	}
    	query.runQuery(new IReceiver<TraceEvent>() {
    	    public void receive(TraceEvent event) {
    	    	events.add(event);
    	    	for (Observer o : observers.keySet()) {
    	    		o.notifyEventAdded(State.this, event);
    	    	}
    	    }
    	}, this);
    }
    
    public TraceEvent getCurrentEvent() {
		return currentEvent;
	}

	public SortedSet<TraceEvent> getEvents() {
		return events;
	}
	
	private static void addEventWithSubsumption(ArrayList<TraceEvent> events, TraceEvent t,
			                                    List<TraceEvent> subsumedEvents) {
		for (TraceEvent e : events) {
			if (e.subsumes(t)) {
				if (subsumedEvents != null) {
					subsumedEvents.add(t);
				}
				return;
			}
		}
		for (Iterator<TraceEvent> i = events.iterator(); i.hasNext();) {
			TraceEvent e = i.next();
			if (t.subsumes(e)) {
				if (subsumedEvents != null) {
					subsumedEvents.add(e);
				}
				i.remove();
			}
		}
		events.add(t);
	}
	
	public List<TraceEvent> getEventsWithSubsumption(List<TraceEvent> subsumedEvents) {
		ArrayList<TraceEvent> result = new ArrayList<TraceEvent>();
		ArrayList<TraceEvent> eventsWithSameTimestamp = new ArrayList<TraceEvent>();
		long lastTStamp = -1;
		for (TraceEvent t : events) {
			if (t.getTStamp() != lastTStamp) {
				result.addAll(eventsWithSameTimestamp);
				eventsWithSameTimestamp.clear();
				lastTStamp = t.getTStamp();
			}
			addEventWithSubsumption(eventsWithSameTimestamp, t, subsumedEvents);
		}
		result.addAll(eventsWithSameTimestamp);
		return result;
	}

	public List<TraceEvent> getEventsWithSubsumption() {
		return getEventsWithSubsumption(null);
	}
	
	public void addObserver(Observer o) {
    	Integer i = observers.get(o);
        int v = i == null ? 1 : i + 1;
    	observers.put(o, v);
    }
    
    public void removeObserver(Observer o) {
    	observers.put(o, observers.get(o) - 1);
    }
	
	public Session getSession() {
		return session;
	}
	
	public AbstractLocationMap getLocationMap() {
		return locationMap;
	}
	
	public Display getDisplay() {
		return display;
	}
	
	public IWorkbenchWindow getWindow() {
		return window;
	}
	
	public ColorScheme getColorScheme() {
		return colorScheme;
	}
	
	public TypeRendererManager getTypeRenderer() {
		return typeRenderer;
	}
	
	private static WeakHashMap<IWorkbenchWindow,State> states
	    = new WeakHashMap<IWorkbenchWindow,State>();
	
	public static State getState(IWorkbenchWindow window) {
		State st = states.get(window);
		if (st != null) 
			return st;
	    st = new State(window);
	    states.put(window, st);
	    return st;
	}
}
