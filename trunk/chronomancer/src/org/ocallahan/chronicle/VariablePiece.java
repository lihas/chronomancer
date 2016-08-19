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

import java.util.Arrays;

public abstract class VariablePiece {
    protected int bitStart;
    protected int bitLength;
    
    protected VariablePiece(JSONObject o) throws JSONParserException {
		Integer start = o.getInt("valueBitStart");
		if (start != null && start < 0)
			throw new JSONParserException("Negative bit start");
		bitStart = start == null ? -1 : start;
		bitLength = o.getIntRequired("bitLength");
	}
    
	protected int getBitsAtBitOffset(int offset, int length) {
	    int bits = length*8;
	    if (bitLength == 0)
	    	return bits;
	    return Math.min(bitLength - offset*8, bits);
	}
	
    /**
     * Reads a location into an array of bytes returned in the DataSink.
     * Bits beyond the bitLength are returned as zeroes (and are considered
     * valid).
     * Can be called by any thread. The results (like all DataSink results)
     * are returned by calling receive() on the Session thread.
     */
    public abstract IDataSource getDataSource(Session session, long tStamp);
    
    public static VariablePiece parse(JSONObject o) throws JSONParserException {
    	String type = o.getStringRequired("type").intern();
    	if (type == "memory")
    		return new Memory(o);
    	if (type == "register")
    		return new Register(o);
    	if (type == "error")
    		return new Error(o);
    	if (type == "undefined")
    		return new Undefined(o);
    	if (type == "constant")
    		return new Constant(o);
    	throw new JSONParserException("Unknown location type: " + type);
    }
    
    public static class Memory extends VariablePiece {
    	private long address;
    	private int addressBitOffset;

    	public Memory(JSONObject o) throws JSONParserException {
			super(o);
			address = o.getLongRequired("address");
			addressBitOffset = o.getIntRequired("addressBitOffset");
		}
    	
    	public long getAddress() {
			return address;
		}
		public int getAddressBitOffset() {
			return addressBitOffset;
		}
		
		@Override
		public IDataSource getDataSource(Session session, long tStamp) {
			if (!session.getArchitecture().isLittleEndian())
				throw new IllegalArgumentException("Only little-endian handled currently");

			MemoryDataSource mem =
				    new MemoryDataSource(session, tStamp, address);
			return ShiftDataSource.make(mem, addressBitOffset);
		}
    }
    
    public static class Register extends VariablePiece {
    	private String register;
    	private int registerBitOffset;

    	public Register(JSONObject o) throws JSONParserException {
			super(o);
			register = o.getStringRequired("register");
			registerBitOffset = o.getIntRequired("registerBitOffset");
		}
    	
    	public String getRegister() {
			return register;
		}
		public int getRegisterBitOffset() {
			return registerBitOffset;
		}
		
		@Override
		public IDataSource getDataSource(Session session, long tStamp) {
			RegisterDataSource regSource = new RegisterDataSource(session,
					tStamp, register);
			return ShiftDataSource.make(regSource, registerBitOffset);
		}
    }
    
    public static byte[] parseData(String hexData) throws JSONParserException {
    	if (hexData.length()%2 != 0)
    		throw new JSONParserException("Odd length in string: " + hexData);
    	byte[] data = new byte[hexData.length()/2];
    	for (int i = 0; i < data.length; ++i) {
    		data[i] = (byte)(RegisterValues.parseHexChar(hexData.charAt(i*2))*16 +
    		                 RegisterValues.parseHexChar(hexData.charAt(i*2 + 1)));
    	}
    	return data;
    }
    
    public static class Constant extends VariablePiece {
    	private byte[] data;

    	public Constant(JSONObject o) throws JSONParserException {
			super(o);
		    data = parseData(o.getStringRequired("data"));
		}

		public byte[] getData() {
			return data;
		}
		
        @Override
		public IDataSource getDataSource(final Session session, final long tStamp) {
        	return new IDataSource() {
        		public void findNextChangeTStamp(long start, long length, final TStampReceiver receiver) {
        			session.runOnThread(new Runnable() {
        				public void run() {
        					receiver.receiveEndOfScope(session.getEndTStamp());
        				}
        			});
        		}
        		public void findPreviousChangeTStamp(long start, long length, final TStampReceiver receiver) {
        			session.runOnThread(new Runnable() {
        				public void run() {
        					receiver.receiveEndOfScope(0);
        				}
        			});
        		}
        		public Session getSession() {
        			return session;
        		}
        		public IDataSource getSourceAtTime(long tStamp) {
        			return getDataSource(session, tStamp);
        		}
        		public long getTStamp() {
        			return tStamp;
        		}
        		public void read(final long offset, final int length, final IDataSink sink) {
        			session.runOnThread(new Runnable() {
        				public void run() {
        					byte[] result = new byte[length];
        					boolean[] resultValid = new boolean[length];
        					if (offset < data.length) {
        						int intOffset = (int)offset;
        						int len = Math.min(data.length - intOffset, length);
        						Arrays.fill(resultValid, 0, len, true);
        						System.arraycopy(data, intOffset, result, 0, len);
        					}
        					sink.receive(result, resultValid);
        				}
        			});
        		}				 
        	};
		}
    }
    
    public static class InvalidDataSource implements IDataSource {
    	private Session session;
    	private long tStamp;
		public InvalidDataSource(Session session, long tStamp) {
			this.session = session;
			this.tStamp = tStamp;
		}
		public void findNextChangeTStamp(long start, long length, final TStampReceiver receiver) {
			session.runOnThread(new Runnable() {
				public void run() {
					receiver.receiveEndOfScope(session.getEndTStamp());
				}
			});
		}
		public void findPreviousChangeTStamp(long start, long length, final TStampReceiver receiver) {
			session.runOnThread(new Runnable() {
				public void run() {
					receiver.receiveEndOfScope(0);
				}
			});
		}
		public Session getSession() {
			return session;
		}
		public IDataSource getSourceAtTime(long tStamp) {
			return new InvalidDataSource(session, tStamp);
		}
		public long getTStamp() {
			return tStamp;
		}
		public void read(long offset, final int length, final IDataSink sink) {
			sendInvalidData(session, sink, length);
		}
    }
    
    public static void sendInvalidData(Session session,
    		final IDataSink sink, final int length) {
		session.runOnThread(new Runnable() {
			public void run() {
				byte[] data = new byte[length];
				boolean[] valid = new boolean[length];
				sink.receive(data, valid);
			}
		});    	
    }
    
    public static class Error extends VariablePiece {
    	public Error(JSONObject o) throws JSONParserException {
			super(o);
		}

    	@Override
    	public IDataSource getDataSource(Session session, long tStamp) {
    		return new InvalidDataSource(session, tStamp);
    	}
    }
    
    public static class Undefined extends Error {
    	public Undefined(JSONObject o) throws JSONParserException {
			super(o);
		}

    	@Override
    	public IDataSource getDataSource(Session session, long tStamp) {
    		return new InvalidDataSource(session, tStamp);
    	}
    }

	public int getBitLength() {
		return bitLength;
	}

	public int getBitStart() {
		return bitStart;
	}
}
