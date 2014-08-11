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
import de.beckhoff.jni.tcads.AmsAddr;

enum varType {
	BIT, INT, LREAL,STRUCT;
}

enum ioType {
	R, W, RW;
}

public class adsConnection extends comandInterpreter{
	static Pattern configPattern = Pattern
			.compile("\\A\\s*([\\w]+):\\s*(0x)?([0-9A-F]+)\\z");

	Semaphore semaphore = new Semaphore(1);
	Map<String, adsConfigItem> adsVarMap = new HashMap<String, adsConfigItem>();
	AmsAddr addr = new AmsAddr();

	public adsConnection(String file) throws Exception{
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

		readConfigFile(file);
		
	}

	private void addVarName(String line) throws Exception {
		String[] parts = line.split(",");
		adsConfigItem adsItem = new adsConfigItem();
		for (int i = 0; i < parts.length; i++) {
			parts[i] = parts[i].trim();
		}
		if (parts.length == 8) {
			adsItem.connection = parts[0];
			adsItem.cmdName = parts[1];

			varType vType;
			switch (parts[2]) {
			case "BIT":
				vType = varType.BIT;
				break;
			case "INT":
				vType = varType.INT;
				break;
			case "LREAL":
				vType = varType.LREAL;
				break;
			case "STRUCT":
				vType = varType.STRUCT;
				break;
			default:
				throw new Exception("unknown variable type " + parts[2]);
			}
			adsItem.vType = vType;

			ioType iType;
			switch (parts[3].toUpperCase()) {
			case "R":
				iType = ioType.R;
				break;
			case "W":
				iType = ioType.W;
				break;
			case "RW":
				iType = ioType.RW;
				break;
			default:
				throw new Exception("r / w " + parts[3]);
			}
			adsItem.iType = iType;

			for (int i = 4; i < parts.length; i++) {
				Matcher m = configPattern.matcher(parts[i]);
				if (m.find()) {
					int varNum;
					try {
						varNum = (int) Long.parseLong(m.group(3).toLowerCase(),
								m.group(2) == null ? 10 : 16);
						if (i == 4 && m.group(1).equals("Port")) {
							adsItem.port = varNum;
						} else if (i == 5 && m.group(1).equals("IGrp")) {
							adsItem.igrp = varNum;
						} else if (i == 6 && m.group(1).equals("IOffs")) {
							adsItem.ioffs = varNum;
						} else if (i == 7 && m.group(1).equals("Len")) {
							adsItem.len = varNum;
						} else {
							throw new Exception("item " + i + " = "
									+ m.group(1));
						}
					} catch (NumberFormatException e) {
						// TODO add error handling
						throw new Exception("cannot parse number " + m.group(3)
								+ " group2 = " + (m.group(2) == null ? 10 : 16));
					}

				} else {
					throw new Exception("no match item" + i + " :" + parts[i]);
				}
			}
		} else {
			throw new Exception("cannot split in 8 items ");
		}
		synchronized (adsVarMap) {
			adsVarMap.put(adsItem.cmdName, adsItem);
		}
	}

	void readConfigFile(String file) throws IOException, Exception {
		// Open the file
		FileInputStream fstream = new FileInputStream(file);
		BufferedReader br = new BufferedReader(new InputStreamReader(fstream));

		String strLine;
		int lineNum = 0;
		// Read File Line By Line
		while ((strLine = br.readLine()) != null) {
			strLine = strLine.trim();
			if (!strLine.startsWith("%") && !strLine.equals("")) {
				lineNum++;
				try {
					addVarName(strLine);
				} catch (Exception e) {
					br.close();
					System.err.println("Cannot extract config line " + lineNum
							+ ": '" + strLine + "'");
					throw new Exception("Line" + lineNum + ":" + e.getMessage());
				}
			}
		}
		br.close();
	}

	private ByteBuffer readAdsItem(adsConfigItem adsItem) throws Exception {
		long err;
		JNIByteBuffer buffer = new JNIByteBuffer(adsItem.len);
		ByteBuffer bb = ByteBuffer.allocate(0);
		synchronized (addr) {
			addr.setPort(adsItem.port);
			err = AdsCallDllFunction.adsSyncReadReq(addr, adsItem.igrp, // Index
																		// Group
					adsItem.ioffs, // Index Offset
					adsItem.len, buffer);
		}
		if (err != 0) {
			
			System.out.println("Error: Read '"+adsItem.cmdName+"': 0x"
					+ Long.toHexString(err));
			throw new Exception("Error: Read '"+adsItem.cmdName+"': 0x"
					+ Long.toHexString(err));
		}
		bb = ByteBuffer.wrap(buffer.getByteArray());
		bb.order(ByteOrder.LITTLE_ENDIAN);

		return bb;
	}

	private void writeAdsItem(adsConfigItem adsItem, JNIByteBuffer buffer)
			throws Exception {
		synchronized (addr) {
			addr.setPort(adsItem.port);
			long err = AdsCallDllFunction.adsSyncWriteReq(addr, adsItem.igrp, // Index
																				// Group
					adsItem.ioffs, // Index Offset
					adsItem.len, buffer);
			if (err != 0) {
				System.out.println("Error: Write by adress: 0x"
						+ Long.toHexString(err));
			}
		}
	}
	
	public void write(adsConfigItem adsItem, byte[] data,int numBytes) throws Exception {
		if(adsItem.len!=numBytes){throw new Exception("cannot write "+ numBytes+ " bytes to:"+adsItem.cmdName);}
		if(adsItem.iType==ioType.R){throw new Exception("cannot write to readonly variable :"+adsItem.cmdName);}
		writeAdsItem(adsItem, new JNIByteBuffer(data));
	}

	public void write(adsConfigItem adsItem, boolean var) throws Exception {
		if(adsItem.vType!=varType.BIT){throw new Exception("cannot write a BIT to:"+adsItem.cmdName);}
		if(adsItem.iType==ioType.R){throw new Exception("cannot write to readonly variable :"+adsItem.cmdName);}
		writeAdsItem(adsItem, new JNIByteBuffer(Convert.BoolToByteArr(var)));
	}
	
	public void write(adsConfigItem adsItem, int var) throws Exception {
		if(adsItem.vType!=varType.INT){throw new Exception("cannot write a INT to:"+adsItem.cmdName);}
		if(adsItem.iType==ioType.R){throw new Exception("cannot write to readonly variable :"+adsItem.cmdName);}
		writeAdsItem(adsItem, new JNIByteBuffer(Convert.IntToByteArr(var)));
	}

	public void write(adsConfigItem adsItem, double var) throws Exception {
		if(adsItem.vType!=varType.LREAL){throw new Exception("cannot write a LREAL to:"+adsItem.cmdName);}
		if(adsItem.iType==ioType.R){throw new Exception("cannot write to readonly variable :"+adsItem.cmdName);}
		writeAdsItem(adsItem, new JNIByteBuffer(Convert.DoubleToByteArr(var)));
	}
	
	public boolean readBit(adsConfigItem adsItem) throws Exception {
		if(adsItem.vType!=varType.BIT){throw new Exception("cannot read a BIT from:"+adsItem.cmdName);}
		if(adsItem.iType==ioType.W){throw new Exception("cannot read to writeonly variable :"+adsItem.cmdName);}
		ByteBuffer buffer = readAdsItem(adsItem);
		if(buffer.capacity()==Byte.SIZE / Byte.SIZE){
			return buffer.get()==1?true:false;
		}
		else{
			throw new Exception("Cannot get an Integer from '"
					+ adsItem.cmdName + "'");
		}
	}

	public int readInt(adsConfigItem adsItem) throws Exception {
		if(adsItem.vType!=varType.INT){throw new Exception("cannot read a INT from:"+adsItem.cmdName);}
		if(adsItem.iType==ioType.W){throw new Exception("cannot read to writeonly variable :"+adsItem.cmdName);}
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
					+ adsItem.cmdName + "'");
		}
	}

	public double readDouble(adsConfigItem adsItem) throws Exception {
		if(adsItem.vType!=varType.LREAL){throw new Exception("cannot read a LREAL from:"+adsItem.cmdName);}
		if(adsItem.iType==ioType.W){throw new Exception("cannot read to writeonly variable :"+adsItem.cmdName);}
		ByteBuffer buffer = readAdsItem(adsItem);
		return buffer.getDouble();
	}

	public adsConfigItem adsGetConfig(String cmdName) {
		adsConfigItem item;
		synchronized (adsVarMap) {
			item = adsVarMap.get(cmdName);
		}
		if (item == null) {
			System.out.println("Cannot find variable " + cmdName
					+ " in configuration File");
		}
		return item;
	}
	
	public void closeAds(){
		AdsCallDllFunction.adsPortClose();
	}

	public String setVariable(String varName, operation oper, String value)
			throws Exception {
		adsConfigItem adsItem;
		synchronized (adsVarMap) {
			adsItem = adsVarMap.get(varName);
		}
		if (adsItem != null) {
			if (adsItem.iType == ioType.R) {
				throw new Exception(varName + " is a Readonly variable");
			}
			switch (adsItem.vType) {
			case BIT:
				if (oper == operation.SET)
					write(adsItem, Integer.parseInt(value, 2));
				else
					throw new Exception("Cannot increment Boolean");
				break;
			case INT:
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
			case LREAL:
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
				throw new Exception("type not defined");
			}
			return varName + "=" + value;
		} else {
			return null;
		}
	}

	public String getVariable(String varName) throws Exception {
		adsConfigItem adsItem ;
		synchronized (adsVarMap) {
			adsItem= adsVarMap.get(varName);
		}
		
		if (adsItem != null) {
			if (adsItem.iType == ioType.W) {
				throw new Exception(varName + " is a Writeonly variable");
			}
			switch (adsItem.vType) {
			case BIT:
			case INT:
				return varName + "=" + readInt(adsItem);
			case LREAL:
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
			adsConnection config = new adsConnection("ADS_settings.txt");
			config.sendCmd("Plasma_ON=1");
			// config.sendCmd("Flamme_Error=0");
			config.sendCmd("nextStatus=574");
			config.sendCmd("nextStatus?");
			// config.testWriteInt(344);
			config.sendCmd("Flamme_Error?");
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
