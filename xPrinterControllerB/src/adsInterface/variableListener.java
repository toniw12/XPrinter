package adsInterface;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PipedWriter;
import java.io.PrintStream;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import de.beckhoff.jni.tcads.AdsSymbolEntry;

class NullOutputStream extends OutputStream {
	public void write(int b) throws IOException {
	}
}

/* 
 * The class variableListner pools all variable in the poolingList
 * It also reads the fifo of the measured positions
 */

public class variableListener extends comandInterpreter implements Runnable{
	adsConnection ads;
	Map<String,Vector<adsInterface>> eventMap=new HashMap<String,Vector<adsInterface>>();
	Vector<String> pollingList=new Vector<String>();

	PrintStream poolingOutput;
	PipedInputStream  poolingStream;
	
	AdsSymbolEntry measureStatAdsSetting;
	AdsSymbolEntry measureNStatAdsSetting;
	AdsSymbolEntry measureItemIoAdsSetting;
	
	public variableListener(adsConnection ads){
		this.ads=ads;
		new Thread(this).start();
		PipedOutputStream  pw = new PipedOutputStream ();
		poolingStream = new PipedInputStream ();
		poolingOutput=new PrintStream(pw);
		if (ads != null) {
			measureStatAdsSetting = ads.getAdsEntry("GVL.outFifoStat");
			measureNStatAdsSetting = ads.getAdsEntry("GVL.outFifoNStat");
			measureItemIoAdsSetting = ads.getAdsEntry("GVL.outFifoItemIo");

			if (measureStatAdsSetting == null || measureNStatAdsSetting == null
					|| measureItemIoAdsSetting == null) {
				System.out
						.println("Cannot get Status Settings please controlle the TwinCAT project");
				System.exit(1);
			}
		}
		
		try {
			pw.connect(poolingStream);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public InputStream getPoolingStream(){
		return poolingStream;
	}
	
	public OutputStream getOutputStream(){
		return new NullOutputStream();
	}
	
	public void addPoolingListener(String cmdName){
		synchronized (pollingList) {
			if(!pollingList.contains(cmdName)){
				pollingList.add(cmdName);
			}
		}
	}
	
	public void removePoolingListener(String cmdName){
		synchronized (pollingList) {
			pollingList.remove(cmdName);
		}
	}
	
	public synchronized void addListener(String cmdName,adsInterface inter) throws InterruptedException{
		Vector<adsInterface> interVec=eventMap.get(cmdName);
		if(interVec!=null){
			interVec.add(inter);
		}
		else{
			interVec=new Vector<adsInterface> ();
			interVec.add(inter);
			eventMap.put(cmdName, interVec);
		}
	}
	
	
	public synchronized  void removeListener(String cmdName,adsInterface inter) throws InterruptedException{
		Vector<adsInterface> interVec=eventMap.get(cmdName);
		if(interVec!=null){
			interVec.remove(inter);
		}
	}

	public synchronized  String setVariable(String varName, operation oper, String value)
			throws Exception {
		if(oper==operation.SET){
			Vector<adsInterface> interVec=eventMap.get(varName);
			if(interVec!=null){
				for(adsInterface inter:interVec){
					inter.sendEventMsg(varName+"="+value);
				}
			}
		}
		return null;
	}


	public void run() {
		boolean measureFifoState=false;
		boolean measureFifoReadState=false;
		while(true){
			try {
				Thread.sleep(200);
				for(String cmdName:pollingList){
					poolingOutput.println(cmdName+"?");
				}
				
				if (ads != null) {
					// Read Mesurement Fifo
					while ((measureFifoReadState = ads
							.readBit(measureNStatAdsSetting)) != measureFifoState) {
						ByteBuffer buffer = ads.read(measureItemIoAdsSetting);
						sendCmd("Measure=[" + buffer.getDouble() + ","
								+ buffer.getDouble() + "," + buffer.getDouble()
								+ "]");
						measureFifoState = measureFifoReadState;
						ads.write(measureStatAdsSetting, measureFifoState);
						Thread.sleep(5);
					}
				}
				
				poolingOutput.flush();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
}
