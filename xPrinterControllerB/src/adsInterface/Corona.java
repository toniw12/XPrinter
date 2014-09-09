package adsInterface;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Vector;
import java.util.concurrent.Semaphore;
import java.io.*;

//import javax.comm.*;
import javax.swing.JOptionPane;

import adsInterface.comandInterpreter.operation;
import gnu.io.*;

/*
 * The corona class communicates over RS232 with the corona machine
 * if the String comPort is not null then it shows a inputDialog to select the Serial port
 * this class sends stores all redden and written variables
 * the variables are synchronized with the corona machine tree times per second
 */

public class Corona extends comandInterpreter implements Runnable {
	InputStream inputStream;
	OutputStream outputStream;
	Thread readThread;
	String reciveString = new String();
	int reciveIndex = 0;
	SerialPort serialPort;
	int coronaIst = -1;
	int coronaSoll = -1;
	boolean writeCoronaSoll=false;
	int coronaSollSet=0;
	byte coronaStatus=0;
	String coronaId="No Id recived";
	String[] display = new String[4];
	boolean isResponding=false;
	boolean coronaAnz=false;
	int[] coronaBtns={0,0,0,0,0,0};
	int timeOutCount=0;
	
	public Corona() {
		this(null);
	}

	public Corona(String comPort) {
		
		Vector<String> comNames = new Vector<String>();
		CommPortIdentifier serialPortId;
		// static CommPortIdentifier sSerialPortId;
		Enumeration<CommPortIdentifier> enumComm;
		// SerialPort serialPort; uartCom() {

		enumComm =  CommPortIdentifier.getPortIdentifiers();
		while (enumComm.hasMoreElements()) {
			serialPortId = enumComm.nextElement();

			if (serialPortId.getPortType() == CommPortIdentifier.PORT_SERIAL) {
				comNames.add(serialPortId.getName());
			}
		}
		Object[] possibilities = comNames.toArray();
		if(comPort==null){
		 comPort = (String) JOptionPane.showInputDialog(null, "",
				"Select com port for Corona", JOptionPane.PLAIN_MESSAGE, null,
				possibilities, "ham");
		}

		// If a string was returned, say so.
		if ((comPort != null) && (comPort.length() > 0)) {
			enumComm =  CommPortIdentifier.getPortIdentifiers();
			System.out.println("Corona: unse " + comPort);
			while (enumComm.hasMoreElements()) {
				serialPortId = enumComm.nextElement();

				if (serialPortId.getName().equals(comPort)) {
					System.out.println("Corona: port opend");
					try {

						serialPort = (SerialPort) serialPortId.open("my", 2000);
						//serialPort.notifyOnDataAvailable(true);

						serialPort.setSerialPortParams(19200,
								SerialPort.DATABITS_8, SerialPort.STOPBITS_1,
								SerialPort.PARITY_NONE);
						inputStream = serialPort.getInputStream();
						outputStream = serialPort.getOutputStream();

						readThread = new Thread(this);
						readThread.start();
						

					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}

		} else {
			System.out.println("Corona: Cannot find any serial port\n");
			System.exit(0);
		}
	}
	
	

	public void run() {
		byte[] buffer = new byte[100];
		
		while (true) {
			
			try {
				
				while (inputStream.available() > 0) {
					inputStream.read();
					System.err
							.println("Error some bytes arrived without query");
				}
				
				synchronized (this) {
					byte coronaSteuerByte = (byte) 0x80;
					for (int j = 0; j < coronaBtns.length; j++) {
						if (coronaBtns[j] > 0) {
							coronaBtns[j]--;
							coronaSteuerByte |= 0x01 << (j + 1);
						}
					}
					if (coronaAnz) {
						coronaSteuerByte |= 0x01 << 6;
					}

					coronaSteuerByte |= writeCoronaSoll ? 0x01 : 0x00;
					writeCorona(coronaSteuerByte, coronaSollSet);

					int numBytes = 0;
					int numTry = 5;
					while (numBytes < buffer.length && numTry > 0) {
						Thread.sleep(120);
						numBytes += inputStream.read(buffer, numBytes,
								buffer.length - numBytes);
						numTry--;
					}
					if (numTry == 0) {
						timeOutCount++;
						throw new Exception("Corona read timeout, read:"
								+ numBytes + " bytes");
					}
					timeOutCount=0;
					if (buffer[99] != '#') {
						throw new Exception("End characher is:"
								+ (int) buffer[99] + "and not" + (int) '#');
					}
					int checksum = 0;
					for (int j = 0; j < 13; j++) {
						checksum += (buffer[j] & 0xFF);
					}
					if ((checksum & 0xFF) == 0) {
						checksum = 1;
					}
					if ((int) buffer[13] != (checksum & 0xFF)) {
						throw new Exception("Checksum error recived:"
								+ buffer[13] + " calculate:"
								+ (checksum & 0xFF));
					}

					coronaId = new String(Arrays.copyOfRange(buffer, 0, 3));
					coronaStatus = buffer[4];
					coronaIst = Integer.parseInt(
							(new String(Arrays.copyOfRange(buffer, 5, 9))), 16);
					coronaSoll = Integer
							.parseInt(
									(new String(Arrays.copyOfRange(buffer, 9,
											13))), 16);
					for (int j = 0; j < display.length; j++) {
						display[j] = new String(Arrays.copyOfRange(buffer,
								j * 20 + 14, j * 20 + 14 + 20));
					}

					isResponding = true;
				}
				Thread.sleep(300);
				
			} catch (Exception e) {
				if(timeOutCount<2){
					System.err.println(e.getMessage());
				}
				isResponding=false;
			}
		}
	}

	private void write(byte[] bytes) {
		try {
			outputStream.write(bytes);
			/*for (byte b : bytes) {
				System.out.print(Integer.toHexString(b & 0xFF) + " ");
			}
			System.out.println();
			*/
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public String setVariable(String varName, operation oper, String value)throws Exception {
		synchronized (this) {
		if(oper!=operation.SET){
			throw new Exception("Cannot increment or decrement Corona parameters");
		}
		int coronaBtn=-1;
		switch(varName){
		case "Corona_Soll":
			try{
				coronaSollSet=Integer.parseInt(value);
				writeCoronaSoll=true;
			}
			catch(NumberFormatException e){
				throw new Exception("Cannot convert:" + value +" to Int");
			}
			break;
		case "Corona_Info":
			coronaBtn=0;
			break;
		case "Corona_Start":
			coronaBtn=1;
			break;
		case "Corona_Stop":
			coronaBtn=2;
			break;
		case "Corona_Rechts":
			coronaBtn=3;
			break;
		case "Corona_Links":
			coronaBtn=4;
			break;
		case "Corona_Anz":
			switch(value){
			case "0":
				coronaAnz=false;
				break;
			case "1":
				coronaAnz=true;
				break;
			}
			break;
		default:
			return null;
		}
		if(coronaBtn!=-1){
			try{
				coronaBtns[coronaBtn]+=Integer.parseInt(value,2);
			}
			catch(NumberFormatException e){
				throw new Exception("Cannot convert:" + value +" to Bit");
			}
		}
		}
		return varName+"="+value;
	}
	
	public String getVariable(String varName) throws Exception {
		String retString = null;
		synchronized (this) {
			int retVal = -1;
			switch (varName) {
			case "Corona_ID":
				retString = coronaId;
				break;
			case "Corona_Green":
				retVal = coronaStatus & 0x01;
				break;
			case "Corona_Red":
				retVal = (coronaStatus >> 1) & 0x01;
				break;
			case "Corona_Yelow":
				retVal = (coronaStatus >> 2) & 0x01;
				break;
			case "Corona_Run":
				retVal = (coronaStatus >> 3) & 0x01;
				break;
			case "Corona_Ist":
				retVal = coronaIst;
				break;
			case "Corona_Soll":
				retVal = coronaSoll;
				break;
			case "Corona_Disp":
				retString = display[0] + "\\n" + display[1] + "\\n"
						+ display[2] + "\\n" + display[3];
				break;
			default:
				// System.err.println("Command not found:"+varName);
			}
			if (retVal >= 0) {
				retString = "" + retVal;
			}
			if (retString != null) {
				retString = varName + "=" + retString;
			}

		}
		return retString;
	}
	private void writeCorona(byte steuerbyte, int leistungSoll) {
		byte[] sendData = new byte[12];
		sendData[0] = (byte) 0xFF;
		sendData[1] = (byte) 0x52;
		sendData[2] = (byte) (steuerbyte | 0x80);
		byte[] leistungByte = String.format("%04X", leistungSoll).getBytes();
		if (leistungByte.length != 4) {
			System.err.println("Error while converting leitungSoll:"
					+ new String(leistungByte));
		}
		System.arraycopy(leistungByte, 0, sendData, 3, 4);
		sendData[7] = 0;
		sendData[8] = 0;
		sendData[9] = 0;
		int checksum = 0;
		for (int i = 0; i < 10; i++) {
			checksum += sendData[i];
		}
		sendData[10] = (byte) (checksum & 0xFF);
		sendData[11] = 0x23;

		write(sendData);

	}


	public static void main(String args[]) {
		Corona corona = new Corona();
		


		// uart.write(buffer2);
		// uart.writeCorona((byte)0x01,0x864);
		try {
			Thread.sleep(1000);
			System.out.println(corona.sendCmd("Corona_Soll=320"));
			
			System.out.println(corona.sendCmd("Corona_Rechts=1"));

			System.out.println(corona.sendCmd("Corona_ID?"));
			System.out.println(corona.sendCmd("Corona_Green?"));
			System.out.println(corona.sendCmd("Corona_Red?"));
			System.out.println(corona.sendCmd("Corona_Yelow?"));
			System.out.println(corona.sendCmd("Corona_Run?"));
			System.out.println(corona.sendCmd("Corona_Disp?"));
			
			
			

			int i=10;
			
			while (true) {
				i+=10;
				System.out.println(corona.sendCmd("Corona_Soll="+i));
				System.out.println("Corona_Soll?"+corona.sendCmd("Corona_Soll?"));
				Thread.sleep(1000);
			}

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
