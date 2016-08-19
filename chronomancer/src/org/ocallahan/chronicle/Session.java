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

package org.ocallahan.chronicle;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * A Chronicle query session
 * @author roc
 * 
 * Threading design: queries can be constructed and sent by any thread.
 * Reply messages are read by the ReadThread, parsed, and then forwarded to
 * the SessionThread. All query processing and query listener callbacks happen
 * on the SessionThread. (Additional activities can also be scheduled on the
 * SessionThread using Session.runOnThread().) The goal for complex cascades
 * of queries to run without blocking or being blocked by the UI thread.
 */
public class Session {
	private BufferedReader in;
	private Writer out;
    private HashMap<Integer,Query> queries = new HashMap<Integer,Query>();
    private Listener listener;
    private boolean isClosed = false;
    private TypeManager typeManager = new TypeManager(this);
    private ArrayList<Runnable> pendingRunnables = new ArrayList<Runnable>();
    private long endTStamp;
    private Architecture architecture;
	
	class ReadThread extends Thread {
		ReadThread() {
			super("Chronicle Session Reader");
		}
		public void run() {
			String s;
			try {
				while ((s = in.readLine()) != null) {
					try {
						handleMessage(s);
					} catch (Throwable t) {
						t.printStackTrace();
					}
					synchronized (Session.this) {
						if (isClosed)
							break;
					}
				}
			} catch (IOException e) {
				synchronized (Session.this) {
					if (isClosed)
						return;
				}
				listener.notifyMessage(Severity.FATAL,
				    "I/O error reading from agent", null);
			}
		}
	}
	
	class SessionThread extends Thread {
		SessionThread() {
			super("Chronicle Session");
		}
		public void run() {
			while (true) {
				ArrayList<Runnable> runnables;
				synchronized (Session.this) {
					if (isClosed)
						break;
					runnables = pendingRunnables;
					pendingRunnables = new ArrayList<Runnable>();
					if (runnables.isEmpty()) {
						try {
							Session.this.wait();
						} catch (InterruptedException e) {
							break;
						}
					}
				}
				for (Runnable r : runnables) {
					r.run();
				}
			}
		}
	}
	
	synchronized public void runOnThread(Runnable r) {
		pendingRunnables.add(r);
		notifyAll();
	}
	
	public enum Severity { INFO, WARNING, ERROR, FATAL }
	
	public interface Listener {
		// These can be called on *any thread*
		public void notifyStarted();
		public void notifyMessage(Severity severity, String info, Query query);
		public void notifySending(String s);
		public void notifyReceived(String s);
	}
	
    public Session(InputStream inStream, OutputStream outStream, Listener listener) {
    	this.listener = listener;
    	try {
    		this.in = new BufferedReader(new InputStreamReader(inStream, "UTF-8"));
    		this.out = new OutputStreamWriter(outStream, "UTF-8");
    	} catch (UnsupportedEncodingException ex) {
    		listener.notifyMessage(Severity.FATAL, "Unsupported UTF8??", null);
    	}
    	
    	endTStamp = 0;
    	InfoQuery q = new InfoQuery(this, new InfoQuery.Listener() {
    		public void notifyDone(InfoQuery q, boolean complete,
    				long endTStamp, Architecture architecture) {
    			Session.this.endTStamp = endTStamp;
    			Session.this.architecture = architecture;
    			Session.this.listener.notifyStarted();
    		}
    	});
    	q.send();

    	(new ReadThread()).start();
    	(new SessionThread()).start();
    }

    public TypeManager getTypeManager() {
    	return typeManager;
    }

    private static HashMap<String,Severity> severityNames =
    	new HashMap<String,Severity>();
    static {
    	severityNames.put("info", Severity.INFO);
    	severityNames.put("warning", Severity.WARNING);
    	severityNames.put("error", Severity.ERROR);
    	severityNames.put("fatal", Severity.FATAL);
    }
    
    // runs on ReadThread. Parses the message and hands it to the SessionThread.
	void handleMessage(String s) {
		listener.notifyReceived(s);
		JSONParser parser = new JSONParser(s);
		try {
    		final JSONObject obj = parser.parseObject();
    		runOnThread(new Runnable() {
    			public void run() {
    				handleParsedMessage(obj);
    			}
    		});
		} catch (JSONParserException ex) {
			listener.notifyMessage(Severity.ERROR,
					"Failed to parse response: " + ex.getMessage(),
					null);
		}
	}
	
	// runs on SessionThread
	void handleParsedMessage(JSONObject obj) {
		Query query = null;
		try {
			Integer id = obj.getInt("id");
			if (id != null) {
				synchronized (this) {
					query = queries.get(id);
				}
			}
			String message = obj.getString("message");
			boolean fatal = false;
			if (message != null) {
				Severity severity = severityNames.get(obj.getStringRequired("severity"));
				String text = obj.getStringRequired("text");
				if (severity == null)
					throw new JSONParserException("Unknown severity: " + severity);
				listener.notifyMessage(severity, text, query);
				fatal = severity == Severity.FATAL;
			}
			
			if (query != null) {
				query.handleResult(obj);
				String terminated = obj.getString("terminated");
				if (terminated != null) {
	                query.handleDone(terminated.equals("normal"));
	                query = null;
	                synchronized (this) {
	                	queries.remove(id);
	                }
				}
			}
			
			if (fatal) {
				close();
			}
		} catch (JSONParserException ex) {
			listener.notifyMessage(Severity.ERROR,
					"Failed to parse response: " + ex.getMessage(),
					query);
			if (query != null) {
				query.handleDone(false);
			}
		}
	}

	public void close() {
		HashMap<Integer,Query> qs;
		synchronized (this) {
			if (isClosed)
				return;

			isClosed = true;
			try {
				in.close();
				out.close();
			} catch (IOException e) {
			}
			qs = queries;
			queries = null;
			notifyAll();
		}
		
		for (Query q : qs.values()) {
			q.handleDone(false);
		}
    }
    
    private int lastId = 0;
    synchronized int allocateID() { return ++lastId; }

    /**
     * @param id
     */
    void cancel(int id) {
    	Query q;
    	synchronized (this) {
    		q = queries.get(id);
    		if (q == null || isClosed)
    			return;

    		JSONObjectBuilder builder = new JSONObjectBuilder();
    		builder.append("id", id);
    		builder.append("cmd", "cancel");
    		try {
    			send(builder);
    			return;
    		} catch (IOException e) {
    		}
    	}
		listener.notifyMessage(Severity.ERROR, "I/O error", q);
	}
    
    void send(JSONObjectBuilder builder) throws IOException {
    	String s = builder.makeString();
    	listener.notifySending(s);
    	out.write(s);
    	out.write('\n');
    	out.flush();
    }

    void sendQuery(Query query, JSONObjectBuilder builder) {
    	boolean ioError = false;
    	synchronized (this) {
    		if (!isClosed) {
    			try {
    				send(builder);
    				queries.put(query.getID(), query);
    				return;
    			} catch (IOException e) {
    				ioError = true;
    			}
    		}
    	}
    	if (ioError) {
			listener.notifyMessage(Severity.ERROR, "I/O error", query);
    	}
		query.handleDone(false);
	}
    
    public boolean isClosed() {
    	return isClosed;
    }
    
	public long getEndTStamp() {
		return endTStamp;
	}
	
	public Architecture getArchitecture() {
		return architecture;
	}
}
