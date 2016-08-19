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

import java.math.BigInteger;
import java.util.ArrayList;

public class RegisterValues {
	public RegisterValues() {
	}
	
	public void parseValues(JSONObject object) throws JSONParserException {
		for (String f : object.getFields()) {
		    String s = object.getString(f);
		    if (s != null) {
		    	Value val = parseValue(f, s);
		    	if (val != null) {
		    		vList.add(val);
		        }
		    }
		}
	}
	
	public static int parseHexChar(char ch) throws JSONParserException {
		int v = parseHexCharWithFailure(ch);
		if (v < 0)
			throw new JSONParserException("Unexpected hex character: " + ch);
		return v;
	}
	
	public static int parseHexCharWithFailure(char ch) {
		if (ch >= '0' && ch <= '9')
			return ch - '0';
		if (ch >= 'A' && ch <= 'F')
			return ch - 'A' + 10;
		if (ch >= 'a' && ch <= 'f')
			return ch - 'a' + 10;
		return -1;
	}
	
	static Value parseValue(String name, String s) {
		long v = 0;
		int bits = s.length()*4;
		for (int i = 0; i < s.length(); ++i) {
			int digit = parseHexCharWithFailure(s.charAt(i));
			if (digit < 0)
				return null;
			v = v*16 + digit;
		}
		if (bits > 64)
			return new Value(name, bits, v, new BigInteger(name, 16));
		return new Value(name, bits, v);
	}
	
	public Value getValue(String name) {
		name = name.intern();
		for (Value v : vList) {
			if (v.name == name)
				return v;
		}
		return null;
	}
	
	public long getLongValue(String name) {
		Value v = getValue(name);
		if (v == null || v.getBits() > 64)
			throw new IllegalArgumentException("Invalid value"); 
		return v.getLong();
	}
	
	public int getIntValue(String name) {
		Value v = getValue(name);
		if (v == null || v.getBits() > 32)
			throw new IllegalArgumentException("Invalid value"); 
		return (int)v.getLong();
	}
	
	public Iterable<Value> getValues() {
		return vList;
	}
	
	public static class Value {
		Value(String name, int bits, long v) {
			this.name = name;
			this.bits = bits;
			this.value = v;
		}
		Value(String name, int bits, long v, BigInteger b) {
			this.name = name;
			this.bits = bits;
			this.value = v;
			this.bigValue = b;
		}
		public int getBits() {
			return bits;
		}
		public BigInteger getBig() {
			return bigValue;
		}
		public long getLong() {
			return value;
		}
		private String name;
		private int bits;
		private long value;
		private BigInteger bigValue;
	}
	
	private ArrayList<Value> vList = new ArrayList<Value>();
}
