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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;

public class TypeManager {
	private Session session;
    private HashMap<String,Type> types = new HashMap<String,Type>();
    private HashMap<String,PendingType> pendingTypes = new HashMap<String,PendingType>();
    // Count the pending type loads. 
    private int pendingTypeLoads = 0;

    public TypeManager(Session session) {
		this.session = session;
	}
    
    public Session getSession() {
    	return session;
    }
    
    class PendingType {
    	PendingType(String typeKey) {
    		promise = new Type.Promise(typeKey);
    	}
    	String getTypeKey() {
    		return promise.getTypeKey();
        }
    	Type.Promise             promise;
    	ArrayList<Type.Receiver> receivers = new ArrayList<Type.Receiver>();
    	JSONObject               object;
    	Type                     type;
    	String                   aliasFor;
    }
    
    // Called with our lock held. Ensure that the receiver is called when
    // the pending type is loaded.
    void realizePromise(Type.Promise promise, final Type.Receiver receiver) {
    	String key = promise.getTypeKey();
    	PendingType pt = pendingTypes.get(key);
    	pt.receivers.add(receiver);
    }
    
    // Called with our lock held.
    private PendingType makePendingType(String key) {
    	PendingType pt = pendingTypes.get(key);
    	if (pt != null)
    		return pt;

    	++pendingTypeLoads;
    	pt = new PendingType(key);
    	pendingTypes.put(key, pt);
    	
    	TQuery q = new TQuery(session, key, pt);
    	q.send();
    	
    	return pt;
    }
    
    /**
     * key may be null, in which case we do nothing.
     * Called with our lock held.
     */
    void loadType(String key) {
    	if (key == null)
    		return;
    	Type t = types.get(key);
    	if (t != null)
    		return;
    	makePendingType(key);
    }

    synchronized public Type.Promise getPromise(String key) {
    	Type t = types.get(key);
    	if (t != null)
    		return new Type.Promise(key, t);
    	return makePendingType(key).promise;
    }
    
    /**
     * Called with our lock held. Called from Type.finish() methods only.
     * null resolves to VOID.
     */
    Type resolveType(String key) {
    	if (key == null)
    		return Type.VOID;
    	return types.get(key);
    }
    
    private void resolveAlias(String typeKey) {
    	HashSet<String> forwards = new HashSet<String>();
    	ArrayList<PendingType> chain = new ArrayList<PendingType>();
		String target = typeKey;
		Type t = null;

		while (true) {
			if (types.containsKey(target)) {
				t = types.get(target);
				break;
			}
		    PendingType targetPT = pendingTypes.get(target);
		    if (targetPT.aliasFor == null) {
		    	t = targetPT.type;
		    	break;
		    }
		    chain.add(targetPT);
			forwards.add(target);
		    target = targetPT.aliasFor;
		    if (forwards.contains(target)) {
		    	// cycle in alias chain. This should not happen.
		    	break;
		    }
		}
		
		for (String tk : forwards) {
			types.put(tk, t);
		}
		
		for (PendingType pt : chain) {
    		pt.promise.setType(t);
		}
    }
    
    // Called with our lock held.
    private Collection<PendingType> resolvePendingTypes() {
    	for (PendingType pt : pendingTypes.values()) {
    		if (pt.type != null) {
    			types.put(pt.getTypeKey(), pt.type);
	    		pt.promise.setType(pt.type);
    		}
    	}
    	
    	for (PendingType pt : pendingTypes.values()) {
    		if (pt.aliasFor != null) {
    			resolveAlias(pt.getTypeKey());
    		}
    	}

    	Collection<PendingType> pts = pendingTypes.values();
    	for (PendingType pt : pts) {
    		if (pt.type != null) {
         		try {
					pt.type.finish(pt.object, this);
				} catch (JSONParserException e) {
				}
    		}
    		// XXX what about the null (failure) case? How do we stop things from
    		// crashing when someone tries to realize their Promise but can't
    		// find the appropriate PendingType?
    	}

    	pendingTypes = new HashMap<String,PendingType>();
    	return pts;
    }
    
    // Called without our lock held.
    private static void notifyReceivers(Collection<PendingType> resolvedTypes) {
    	for (PendingType rpt : resolvedTypes) {
    		for (Type.Receiver r : rpt.receivers) {
    			r.receive(rpt.type);
    		}
    	}
    }
    
    void resolvePartialType(final PendingType pt, final JSONObject object)
            throws JSONParserException {
        Identifier ident = Identifier.parse(object);
		LookupTypeQuery q = new LookupTypeQuery(session, ident.getName(),
				ident.getContainerPrefix(), ident.getNamespacePrefix(),
				pt.getTypeKey(),
				new LookupTypeQuery.Listener() {
			public void notifyDone(LookupTypeQuery q, boolean complete,
					               Type.Promise type) {
				if (!complete || type == null) {
					// Just use the partial object for this type
					notifyTQueryDone(pt, object);
					return;
				}
				notifyTQueryDoneAlias(pt, type);
			}
		});
		q.send();
    }
    
    // Called on session thread, without our lock held
    void notifyTQueryDone(PendingType pt, JSONObject object) {
    	Collection<PendingType> resolvedTypes;

    	synchronized (this) {
    		if (object != null) {
        		pt.object = object;
    			try {
    				pt.type = Type.createFor(object, this);
    			} catch (JSONParserException e) {
    			}
    		}
    		--pendingTypeLoads;
    		if (pendingTypeLoads > 0)
    			return;
            resolvedTypes = resolvePendingTypes();
    	}

    	notifyReceivers(resolvedTypes);
	}
    
    // Called on session thread, without our lock held
    void notifyTQueryDoneAlias(PendingType pt, Type.Promise aliasFor) {
    	Collection<PendingType> resolvedTypes;

    	synchronized (this) {
    		pt.aliasFor = aliasFor.getTypeKey();
    		--pendingTypeLoads;
    		if (pendingTypeLoads > 0)
    			return;
            resolvedTypes = resolvePendingTypes();
    	}

    	notifyReceivers(resolvedTypes);
    }

    static class TQuery extends Query {
    	TQuery(Session session, String key, PendingType pendingType) {
    		super(session, "lookupType");
    		builder.append("typeKey", key);
    		this.pendingType = pendingType;
    	}
    	
    	@Override
    	void handleDone(boolean complete) {
    		TypeManager tm = session.getTypeManager();
			if (complete) {
				try {
					if (resultObject == null)
						throw new JSONParserException("No result object for type");
					if (resultObject.getBooleanOptional("partial", false)) {
						tm.resolvePartialType(pendingType, resultObject);
						return;
					}
				} catch (JSONParserException ex) {
				}
			}
    		tm.notifyTQueryDone(pendingType, resultObject);
    	}
    	
    	@Override
    	void handleResult(JSONObject object) throws JSONParserException {
    		String kind = object.getString("kind");
    		if (kind != null) {
    			resultObject = object;
    		}
    	}
    	
    	private JSONObject resultObject;
    	private PendingType pendingType;
    }
}
