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

import java.util.Iterator;

public class JSONObject {
	/**
	 * 
	 * @param fieldNames must be all interned
	 * @param values
	 */
    JSONObject(Object[] contents) {
    	this.contents = contents;
    }
    
    public Object getValue(String fieldName) {
    	fieldName = fieldName.intern();
        for (int i = 0; i < contents.length; i += 2) {
        	if (contents[i] == fieldName) {
        		return contents[i + 1];
        	}
        }
        return null;
    }
    
    public boolean hasValue(String fieldName) {
    	fieldName = fieldName.intern();
        for (int i = 0; i < contents.length; i += 2) {
        	if (contents[i] == fieldName) {
        		return true;
        	}
        }
        return false;
    }
    
    public JSONObject getObject(String fieldName) {
    	Object o = getValue(fieldName);
    	if (o instanceof JSONObject)
    		return (JSONObject)o;
    	return null;
    }
    
    public JSONObject getObjectRequired(String fieldName) throws JSONParserException {
    	JSONObject o = getObject(fieldName);
    	if (o != null)
    		return o;
    	throw new JSONParserException("Expected JSON object for field " + fieldName);
    }
    
    public Object[] getArray(String fieldName) {
    	Object o = getValue(fieldName);
    	if (o instanceof Object[])
    		return (Object[])o;
    	return null;
    }
    
    public Object[] getArrayRequired(String fieldName) throws JSONParserException {
    	Object[] o = getArray(fieldName);
    	if (o != null)
    		return o;
    	throw new JSONParserException("Expected array for field " + fieldName);
    }
    
    public String getString(String fieldName) {
    	Object o = getValue(fieldName);
    	if (o instanceof String)
    		return (String)o;
    	return null;
    }
    
    public String getStringRequired(String fieldName) throws JSONParserException {
    	String o = getString(fieldName);
    	if (o != null)
    		return o;
    	throw new JSONParserException("Expected string for field " + fieldName);
    }
    
    public Integer getInt(String fieldName) throws JSONParserException {
    	Number n = getNumber(fieldName);
    	if (n == null)
    		return null;
    	if (n instanceof Integer)
    		return (Integer)n;
    	return n.intValue();
    }
    
    public int getIntOptional(String fieldName, int def) throws JSONParserException {
    	Integer o = getInt(fieldName);
    	if (o == null)
    		return def;
    	return o;
    }
    
    public int getIntRequired(String fieldName) throws JSONParserException {
    	Integer o = getInt(fieldName);
    	if (o != null)
    		return o;
    	throw new JSONParserException("Expected integer for field " + fieldName);
    }

    public Number getNumber(String fieldName) throws JSONParserException {
    	Object o = getValue(fieldName);
    	if (o instanceof Number)
    		return (Number)o;
    	return null;
    }
    
    public Long getLong(String fieldName) throws JSONParserException {
    	Number n = getNumber(fieldName);
    	if (n == null)
    		return null;
    	if (n instanceof Long)
    		return (Long)n;
    	return n.longValue();
    }
    
    public long getLongRequired(String fieldName) throws JSONParserException {
    	Long o = getLong(fieldName);
    	if (o != null)
    		return o;
    	throw new JSONParserException("Expected long for field " + fieldName);
    }
    
    public long getLongOptional(String fieldName, long def) throws JSONParserException {
    	Long o = getLong(fieldName);
    	if (o == null)
    		return def;
    	return o;
    }

    public Boolean getBoolean(String fieldName) {
    	Object o = getValue(fieldName);
    	if (o instanceof Boolean)
    		return (Boolean)o;
    	return null;
    }
    
    public boolean getBooleanRequired(String fieldName) throws JSONParserException {
    	Boolean o = getBoolean(fieldName);
    	if (o != null)
    		return o;
    	throw new JSONParserException("Expected boolean for field " + fieldName);
    }
    
    public boolean getBooleanOptional(String fieldName, boolean def) throws JSONParserException {
    	Boolean o = getBoolean(fieldName);
    	if (o != null)
    		return o;
    	return def;
    }
    
    public Iterable<String> getFields() {
    	return new Iterable<String>() {
    		public Iterator<String> iterator() {
    			return new Iterator<String>() {
    				private int i = 0;
    				public boolean hasNext() {
    					return i < contents.length;
    				}
    				public String next() {
    					if (i >= contents.length)
    						throw new ArrayIndexOutOfBoundsException();
    					String s = (String)contents[i];
    					i += 2;
    					return s;
    				}
    				public void remove() {
    					throw new IllegalArgumentException("Cannot remove field");
    				}
    			};
    		}
    	};
    }
    
    private Object[] contents;
}
