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

public class JSONParser {
	private String s;
	private int    i = 0;
	
	public JSONParser(String s) {
		this.s = s;
		skipWhitespace();
	}
	
	private void skipWhitespace() {
	    while (i < s.length()) {
	    	char ch = s.charAt(i);
	    	switch (ch) {
	    	case '\n':
	    	case '\r':
	    	case ' ':
	    	case '\t':
	    		break;
	        default:
	        	return;
	    	}
	    	++i;
	    }
	}

	private void checkDone() throws JSONParserException {
	    if (i != s.length())
	     	throw new JSONParserException("Leftover characters: " + s.substring(i));
	}
	
	public Object parse() throws JSONParserException {
		Object o = parseValue();
		checkDone();
		return o;
	}
	
    private Object parseValue() throws JSONParserException {
    	char ch = getChar();
    	switch (ch) {
    	case '[': return parseArray();
    	case 't': consumeToken("true"); return Boolean.TRUE;
    	case 'f': consumeToken("false"); return Boolean.FALSE;
    	case 'n': consumeToken("null"); return null;
    	case '"': return parseString();
    	case '{': return parseObject();
    	default:
    		return parseNumber();
    	}
    }
    
    public JSONObject parseObject() throws JSONParserException {
    	ArrayList<Object> all = new ArrayList<Object>();

    	consumeToken('{');
    	while (!isChar('}')) {
    	    all.add(parseString().intern());
    	    consumeToken(':');
    	    all.add(parseValue());
    	    if (isChar(',')) {
    	        consumeToken(',');
    	    }
    	}
    	consumeToken('}');
    	return new JSONObject(all.toArray());
    }
    
    public String parseString() throws JSONParserException {
    	StringBuilder sb = new StringBuilder();
        consume('"');
        for (;;) {
        	if (i >= s.length())
        		throw new JSONParserException("String was not terminated");
        	char ch = s.charAt(i);
        	++i;
        	if (ch == '\\') {
        		if (i >= s.length())
        			throw new JSONParserException("String was not terminated at \\ escape");
        		char ch2 = s.charAt(i);
        		++i;
        	    switch (ch2) {
        	    case 'n': ch = '\n'; break;
        	    case 't': ch = '\t'; break;
        	    case 'r': ch = '\r'; break;
        	    case 'f': ch = '\f'; break;
        	    default: ch = ch2; break;
        	    }
        	} else if (ch == '"') {
        		break;
        	}
        	sb.append(ch);
        }
        skipWhitespace();
        return sb.toString();
    }
    
    public Object[] parseArray() throws JSONParserException {
    	ArrayList<Object> array = new ArrayList<Object>();
    	consumeToken('[');
    	while (!isChar(']')) {
 			array.add(parseValue());
    	    if (isChar(',')) {
    	        consumeToken(',');
    	    }
    	}
    	consumeToken(']');
		return array.toArray();
    }
    
    public Number parseNumber() throws JSONParserException {
    	// TODO handle floats, maybe BigInteger/BigDecimal
    	int start = i;
    	char ch = consumeChar();
    	int sign = 1;
    	if (ch == '+') {
    		ch = consumeChar();
    	} else if (ch == '-') {
    		sign = -1;
    		ch = consumeChar();
    	}
		if (ch < '0' || ch > '9')
			throw new JSONParserException("Found " + ch + "', expected digit");
    	long v = 0;
    	for (;;) {
    		if (v > Long.MAX_VALUE/10)
    			throw new JSONParserException("Long value out of bounds at '" + this.s.substring(start) + "'");
    		v *= 10;
    		long newV = v + sign*(ch - '0');
    		// check for overflow
    		if (sign < 0 ? newV > v : newV < v)
    			throw new JSONParserException("Long value out of bounds at '" + this.s.substring(start) + "'");
    		v = newV;
    		ch = consumeChar();
    		if (ch < '0' || ch > '9') {
    			putBackChar();
    			skipWhitespace();
    			if (v >= Integer.MIN_VALUE && v <= Integer.MAX_VALUE)
    				return (int)v;
    			return v;
    		}
    	}
    }
    
    private void consume(char ch) throws JSONParserException {
        if (i >= s.length())
        	throw new JSONParserException("End of string, expected '" + ch + '"');
        if (s.charAt(i) != ch)
        	throw new JSONParserException("Expected '" + ch + "', got '" + s.charAt(i) + '"');
        ++i;
    }
    
    private void consumeToken(String s) throws JSONParserException {
    	int start = i;
    	for (int j = 0; j < s.length(); ++j) {
    	    if (!isChar(s.charAt(j)))
    	        throw new JSONParserException("Expected '" + s + "', found '" + this.s.substring(start) + "'");
    	    ++i;
    	}
    	skipWhitespace();
    }
    
    private void consumeToken(char ch) throws JSONParserException {
    	consume(ch);
    	skipWhitespace();
    }
    
    private boolean isChar(char ch) {
    	return i < s.length() && s.charAt(i) == ch;
    }
    
    private char getChar() {
    	return i < s.length() ? s.charAt(i) : 0;
    }
    
    private char consumeChar() throws JSONParserException {
    	if (i >= s.length())
    		throw new JSONParserException("Expected character, found end of string");
    	char ch = s.charAt(i);
    	++i;
    	return ch;
    }
    
    private void putBackChar() {
    	--i;
    }
}
