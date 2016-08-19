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

public abstract class Architecture {
    public abstract String getSPReg();
    public abstract boolean isLittleEndian();
    public abstract int getPointerSize();
    
    protected Architecture() {}
    
    public static class X86 extends Architecture {
    	private X86() {}
    	@Override
    	public String getSPReg() {
    		return "esp";
    	}
    	@Override
    	public boolean isLittleEndian() {
    		return true;
    	}
    	@Override
    	public int getPointerSize() {
    		return 4;
    	}
    }

    public static class AMD64 extends Architecture {
    	private AMD64() {}
    	@Override
    	public String getSPReg() {
    		return "rsp";
    	}
    	@Override
    	public boolean isLittleEndian() {
    		return true;
    	}
    	@Override
    	public int getPointerSize() {
    		return 8;
    	}
    }
    
    public static String getThreadReg() { return "thread"; }
    public static String getPCReg() { return "pc"; }
    
    public byte[] toBigEndian(byte[] input) {
    	if (!isLittleEndian())
    		return input;
    	byte[] r = new byte[input.length];
	    for (int i = 0; i < input.length; ++i) {
	    	r[input.length - 1 - i] = input[i];
	    }
	    return r;
    }
    
    public static X86 getX86() {
    	return x86;
    }
    
    public static AMD64 getAMD64() {
    	return amd64;
    }
    
    private static X86 x86 = new X86();
    private static AMD64 amd64 = new AMD64();
}
