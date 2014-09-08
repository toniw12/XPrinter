package adsInterface;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.regex.*;

import de.beckhoff.jni.Convert;
import de.beckhoff.jni.JNIByteBuffer;
import de.beckhoff.jni.tcads.AdsCallDllFunction;
import de.beckhoff.jni.tcads.AdsSymbolEntry;
import de.beckhoff.jni.tcads.AmsAddr;

enum varType {
	BIT, INT, LREAL,STRUCT;
}

enum ioType {
	R, W, RW;
}

class ValueString {
    private int value;
    private String label;

    ValueString(int value, String label) {
        this.value = value;
        this.label = label;
    }

    public int getValue() {
        return value;
    }

    public String getLabel() {
        return label;
    }
}

public class adsConnection extends comandInterpreter{

	Semaphore semaphore = new Semaphore(1);
	Map<String, AdsSymbolEntry> adsVarMap = new HashMap<String, AdsSymbolEntry>();
	AmsAddr addr = new AmsAddr();
	
	
    // TODO: Check real values of each constant.
    private static final int ADST_VOID = 0;
    private static final int ADST_INT8 = 16;
    private static final int ADST_UINT8 = 17;
    private static final int ADST_INT16 = 2;
    private static final int ADST_UINT16 = 18;
    private static final int ADST_INT32 = 3;
    private static final int ADST_UINT32 = 19;
    private static final int ADST_INT64 = 20;
    private static final int ADST_UINT64 = 21;
    private static final int ADST_REAL32 = 4;
    private static final int ADST_REAL64 = 5;
    private static final int ADST_STRING = 30;
    private static final int ADST_WSTRING = 31;
    private static final int ADST_REAL80 = 32;
    private static final int ADST_BIT = 33;
    private static final int ADST_BIGTYPE = 65;
    private static final int ADST_MAXTYPES = 67;

    private static final ValueString[] adsDatatypeString = new ValueString[]{
        new ValueString(ADST_VOID, "ADST_VOID"),
        new ValueString(ADST_INT8, "ADST_INT8"),
        new ValueString(ADST_UINT8, "ADST_UINT8"),
        new ValueString(ADST_INT16, "ADST_INT16"),
        new ValueString(ADST_UINT16, "ADST_UINT16"),
        new ValueString(ADST_INT32, "ADST_INT32"),
        new ValueString(ADST_UINT32, "ADST_UINT32"),
        new ValueString(ADST_INT64, "ADST_INT64"),
        new ValueString(ADST_UINT64, "ADST_UINT64"),
        new ValueString(ADST_REAL32, "ADST_REAL32"),
        new ValueString(ADST_REAL64, "ADST_REAL64"),
        new ValueString(ADST_STRING, "ADST_STRING"),
        new ValueString(ADST_WSTRING, "ADST_WSTRING"),
        new ValueString(ADST_REAL80, "ADST_REAL80"),
        new ValueString(ADST_BIT, "ADST_BIT"),
        new ValueString(ADST_BIGTYPE, "ADST_BIGTYPE"),
        new ValueString(ADST_MAXTYPES, "ADST_MAXTYPES")
    };

	public adsConnection() throws Exception{
		long err;
		// Open communication
		AdsCallDllFunction.adsPortOpen();

		err = AdsCallDllFunction.getLocalAddress(addr);

		if (err != 0) {
			System.out.println("Error: Open communication: 0x"
					+ Long.toHexString(err));
		} else {
			System.out.println("ADS: Open communication!");
		}
		//addr.setPort(AdsCallDllFunction.AMSPORT_R0_PLC_RTS1);
		addr.setPort(851);
	}


	private ByteBuffer readAdsItem(AdsSymbolEntry adsItem) throws Exception {
		long err;
		JNIByteBuffer buffer = new JNIByteBuffer(adsItem.getSize());
		ByteBuffer bb = ByteBuffer.allocate(0);
		synchronized (addr) {
			err = AdsCallDllFunction.adsSyncReadReq(addr, adsItem.getiGroup(),adsItem.getiOffs(),adsItem.getSize(), buffer);
		}
		if (err != 0) {
			String error="Error: Read '"+adsItem.getName()+" on Port" + addr.getPort() +"': 0x"
					+ Long.toHexString(err);
			System.out.println(error);
			throw new Exception(error);
		}
		bb = ByteBuffer.wrap(buffer.getByteArray());
		bb.order(ByteOrder.LITTLE_ENDIAN);

		return bb;
	}

	private void writeAdsItem(AdsSymbolEntry adsItem, JNIByteBuffer buffer)
			throws Exception {
		if(adsItem==null){
			throw new Exception("writeAdsItem: Cannot read null Item");
		}
		synchronized (addr) {
			long err = AdsCallDllFunction.adsSyncWriteReq(addr, adsItem.getiGroup(), adsItem.getiOffs(),adsItem.getSize(), buffer);
			if (err != 0) {
				String error="Error: Read '"+adsItem.getName()+" on Port" + addr.getPort() +"': 0x"
						+ Long.toHexString(err);
				System.out.println(error);
				throw new Exception(error);
			}
		}
	}
	
	public void write(AdsSymbolEntry adsItem, byte[] data,int numBytes) throws Exception {
		//if(adsItem.getSize()!=numBytes){throw new Exception("cannot write "+ numBytes+ " bytes to:"+adsItem.getName());}
		writeAdsItem(adsItem, new JNIByteBuffer(data));
	}

	public void write(AdsSymbolEntry adsItem, boolean var) throws Exception {
		if(adsItem.getDataType()!=ADST_BIT){throw new Exception("cannot write a BIT to:"+adsItem.getName());}
		writeAdsItem(adsItem, new JNIByteBuffer(Convert.BoolToByteArr(var)));
	}
	
	public void write(AdsSymbolEntry adsItem, int var) throws Exception {
		if(adsItem.getDataType()!=ADST_INT16){throw new Exception("cannot write a INT to:"+adsItem.getName());}
		writeAdsItem(adsItem, new JNIByteBuffer(Convert.IntToByteArr(var)));
	}

	public void write(AdsSymbolEntry adsItem, double var) throws Exception {
		if(adsItem.getDataType()!=ADST_REAL64){throw new Exception("cannot read a REAL64 from:"+adsItem.getName());}
		writeAdsItem(adsItem, new JNIByteBuffer(Convert.DoubleToByteArr(var)));
	}
	
	public ByteBuffer read(AdsSymbolEntry adsItem) throws Exception {
		return readAdsItem(adsItem);
	}
	
	public boolean readBit(AdsSymbolEntry adsItem) throws Exception {
		if(adsItem.getDataType()!=ADST_BIT){throw new Exception("cannot read a BIT from:"+adsItem.getName());}
		ByteBuffer buffer = readAdsItem(adsItem);
		if(buffer.capacity()==Byte.SIZE / Byte.SIZE){
			return buffer.get()==1?true:false;
		}
		else{
			throw new Exception("Cannot get an Integer from '"
					+ adsItem.getName() + "' Buffer capacity:"+buffer.capacity());
		}
	}

	public int readInt(AdsSymbolEntry adsItem) throws Exception {
		
		ByteBuffer buffer = readAdsItem(adsItem);
		switch (buffer.capacity()) {
		case Byte.SIZE / Byte.SIZE:
			return (int) buffer.get();
		case Short.SIZE / Byte.SIZE:
			return (int) buffer.getShort();
		case Integer.SIZE / Byte.SIZE:
			return buffer.getInt();
		default:
			throw new Exception("Cannot get an Integer from '"
					+ adsItem.getName() + "'");
		}
	}

	public double readDouble(AdsSymbolEntry adsItem) throws Exception {
		if(adsItem.getDataType()!=ADST_REAL64){throw new Exception("cannot write a REAL64 to:"+adsItem.getName());}
		ByteBuffer buffer = readAdsItem(adsItem);
		return buffer.getDouble();
	}

	public AdsSymbolEntry getAdsEntry(String cmdName) {
		AdsSymbolEntry adsSymbolEntry=null;
		synchronized (adsVarMap) {
			if (adsVarMap.containsKey(cmdName)) {
				return adsVarMap.get(cmdName);
			} else {
	            try {
	            	long err;
	                JNIByteBuffer readBuff = new JNIByteBuffer(0xFFFF);
	                JNIByteBuffer writeBuff;
	                // Initialize writeBuff with user data
	                writeBuff = new JNIByteBuffer(
	                        Convert.StringToByteArr(cmdName, false));

	                // Get variable declaration
	                err = AdsCallDllFunction.adsSyncReadWriteReq(
	                                    addr,
	                                    AdsCallDllFunction.ADSIGRP_SYM_INFOBYNAMEEX,
	                                    0,
	                                    readBuff.getUsedBytesCount(),
	                                    readBuff,
	                                    writeBuff.getUsedBytesCount(),
	                                    writeBuff);
	                if(err!=0) {
	                    System.out.println("Cannot find variable "+ cmdName +" Error: 0x"
	                            + Long.toHexString(err));
	                    
	                    adsVarMap.put(cmdName, null);
	                    return null;
	                } else {
	                    // Convert stream to AdsSymbolEntry
	                    adsSymbolEntry = new AdsSymbolEntry(readBuff.getByteArray());
	                    adsVarMap.put(cmdName, adsSymbolEntry);
	                }
	            } catch (Exception ex) {
	                System.out.print(ex.getMessage());
	            }
			}
		}

		return adsSymbolEntry;
	}
	
	public void printEntryProprieties(AdsSymbolEntry adsSymbolEntry){
        // Write information to stdout
        System.out.println("Name:\t\t"
                            + adsSymbolEntry.getName());
        System.out.println("Index Group:\t"
                            + adsSymbolEntry.getiGroup());
        System.out.println("Index Offset:\t"
                            + adsSymbolEntry.getiOffs());
        System.out.println("Size:\t\t"
                            + adsSymbolEntry.getSize());
        System.out.println("Type:\t\t"
                            + adsSymbolEntry.getType());
        System.out.println("Comment:\t"
				+ adsSymbolEntry.getComment());
		// Iterate through ValueString[] and try to find a
		// datatype-match
		for (int i = 0; i < adsDatatypeString.length; i++) {
			if (adsDatatypeString[i].getValue() == adsSymbolEntry
					.getDataType()) {

				System.out.println("Datatype: " + "\t"
						+ adsDatatypeString[i].getLabel());
			}
		}
	}
	
	public void closeAds(){
		AdsCallDllFunction.adsPortClose();
	}

	public String setVariable(String varName, operation oper, String value)
			throws Exception {
		AdsSymbolEntry adsItem=getAdsEntry(varName);
		
		if (adsItem != null) {
			switch (adsItem.getDataType()) {
			case ADST_BIT:
				if (oper == operation.SET)
					write(adsItem, Integer.parseInt(value, 2)>=1);
				else
					throw new Exception("Cannot increment Boolean");
				break;
			case ADST_INT16:
				int intVal = 0;
				int newInt = Integer.parseInt(value);
				switch (oper) {
				case SET:
					intVal = newInt;
					break;
				case INCREMENT:
					intVal = readInt(adsItem) + newInt;
					break;
				case DECREMENT:
					intVal = readInt(adsItem) - newInt;
					break;
				}
				write(adsItem, intVal);
				break;
			case ADST_REAL64:
				double dobleVal = 0;
				double newDouble = Double.parseDouble(value);
				switch (oper) {
				case SET:
					dobleVal = newDouble;
					break;
				case INCREMENT:
					dobleVal = readDouble(adsItem) + newDouble;
					break;
				case DECREMENT:
					dobleVal = readDouble(adsItem) - newDouble;
					break;
				}
				write(adsItem, dobleVal);
				break;
			default:
				throw new Exception("type not defined: "+ adsItem.getType());
				
			}
			return varName + "=" + value;
		} else {
			return null;
		}
	}

	public String getVariable(String varName) throws Exception {
		AdsSymbolEntry adsItem=getAdsEntry(varName);
		
		if (adsItem != null) {

			switch (adsItem.getDataType()) {
			case ADST_BIT:
				return varName + "=" + readBit(adsItem);
			case ADST_INT16:
				return varName + "=" + readInt(adsItem);
			case ADST_REAL64:
				return varName + "=" + readDouble(adsItem);
			default:
				throw new Exception("type not defined");
			}
		} else {
			return null;
		}
	}

	public static void main(String args[]) {
		try {
			adsConnection config = new adsConnection();
			System.out.println(config.sendCmd("MAIN.iCounter?"));
			// config.sendCmd("Flamme_Error=0");
			config.sendCmd("MAIN.iCounter=1");
			//Thread.sleep(2000);
			System.out.println(config.sendCmd("MAIN.iCounter?"));
			config.sendCmd("nextStatus?");
			config.sendCmd("nextStatus?");
			config.sendCmd("nextStatus?");
			config.sendCmd("nextStatus?");
			config.sendCmd("nextStatus?");
			// config.testWriteInt(344);
			config.sendCmd("Flamme_Error?");
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
