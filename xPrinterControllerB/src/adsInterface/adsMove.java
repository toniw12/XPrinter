package adsInterface;

import java.nio.ByteBuffer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.beckhoff.jni.Convert;
import de.beckhoff.jni.tcads.AdsSymbolEntry;

public class adsMove extends comandInterpreter implements Runnable {
	Object moveSema = new Object();
	static Pattern gcodePattern = Pattern
			.compile("([A-Z])(\\+=|-=)?([-+]?[0-9]*\\.?[0-9]+)");
	adsConnection ads;
	static final String[] axisNames={"X","Y","Z"};
	static final int numAxis = axisNames.length;
	double currentPos[] = new double[numAxis];
	double velocity = 10;
	double configVelocity = 10;
	double stepSize = 0.01;
	double override = 0;
	int nextStatus = 0;
	long lastCmdTime;
	Thread ownerThread = null;
	AdsSymbolEntry[] actPosAdsSetting = new AdsSymbolEntry[numAxis];
	AdsSymbolEntry actStateAdsSetting;
	AdsSymbolEntry axisActiveAdsSetting;
	boolean movePermited = false;

	AdsSymbolEntry actItemAdsSetting;
	AdsSymbolEntry overrideAdsSetting;
	AdsSymbolEntry inMotionAdsSetting;
	AdsSymbolEntry actStatusAdsSetting, nextStatusAdsSetting;

	public adsMove(adsConnection ads) throws Exception {
		this.ads = ads;
		this.ads = ads;

		actItemAdsSetting = ads.getAdsEntry("GVL.inFifoItemIo");
		for(int i=0;i<numAxis;i++){
			actPosAdsSetting[i] = ads.getAdsEntry("GVL."+ axisNames[i].toLowerCase() +"Axis.NcToPlc.ActPos");
		}
		inMotionAdsSetting = ads.getAdsEntry("GVL.inMotion");
		overrideAdsSetting = ads.getAdsEntry("GVL.Override");
		actStateAdsSetting = ads.getAdsEntry("GVL.state");
		axisActiveAdsSetting = ads.getAdsEntry("GVL.axisActive");
		actStatusAdsSetting = ads.getAdsEntry("GVL.inFifoStat");
		nextStatusAdsSetting = ads.getAdsEntry("GVL.inFifoNStat");
		nextStatus = getStatus();

		new Thread(this).start();
	}

	public String getVariable(String varName) throws Exception {
		for(int i=0;i<numAxis;i++){
			if(varName.equals(axisNames[i])){
				return String.format(axisNames[i]+"=%.3f", ads.readDouble(actPosAdsSetting[i]));
			}
		}
		if(varName.equals("Move_owner")){
			if (ownerThread == null) {
				return varName + "=Free";
			} else if (ownerThread == Thread.currentThread()) {
				return varName + "=You";
			} else {
				return varName + "=Someone else";
			}
		}
		return null;
	}

	public String setVariable(String varName, operation oper, String value)
			throws Exception {

		try {
			switch (varName) {
			case "Velocity":
				configVelocity = Double.parseDouble(value);
				break;
			case "StepSize":
				stepSize = Double.parseDouble(value);
				break;
			case "Override":
				override = Double.parseDouble(value);
				ads.write(overrideAdsSetting, override);
				break;
			case "MotionActive":
				if (value.equals("1")) {
					synchronized (moveSema) {
						if (!movePermited) {
							int timeTry = 0;
							ads.write(axisActiveAdsSetting, true);
							System.out
									.println("Move: Waiting state Becomes 3...");
							while (ads.readInt(actStateAdsSetting) != 3) {
								Thread.sleep(100);
								timeTry += 100;
								if (timeTry >= 800) {
									return "Waiting state 3 timeout";
								}
							}
							System.out.print("Move: Reading actual position");
							for (int i = 0; i < numAxis; i++) {
								currentPos[i] = ads.readDouble(actPosAdsSetting[i]);
							}
							movePermited = true;
						}
					}
				} else {
					ads.write(axisActiveAdsSetting, false);
					movePermited = false;
				}
				break;
			default:
				return null;
			}
		} catch (NumberFormatException e) {
			throw new Exception("Cannot convert '" + value + "' to a double");
		}
		if (oper != operation.SET) {
			throw new Exception("Cannot increment " + varName);
		}
		System.out.println(varName + "=" + value);
		return varName + "=" + value;

	}

	public String sendCmd(String cmd,int cmdId) throws Exception {
		super.sendCmd(cmd,cmdId);
		String retString;
		if ((retString = super.sendCmd(cmd,cmdId)) != null) {
			return retString;
		}
		Matcher m = gcodePattern.matcher(cmd);
		double nextPos[] = new double[numAxis];
		boolean incPos[] = new boolean[numAxis];
		boolean setPos=false;
		short instrType=0;
		boolean breakEnable=false;
		double axisVelocity[] = new double[numAxis];
		for (int i = 0; i < numAxis; i++) {
			nextPos[i] = Double.NaN;
		}
		double new_velocity = velocity;
		boolean wholeMatch = false;
		while (m.find()) {
			int axisN = -1;
			String axis = m.group(1).trim();
			double val = Double.parseDouble(m.group(3));
			for(int i=0;i<numAxis;i++){
				if(axis.equals(axisNames[i])){
					axisN=i;
				}
			}
			if(axisN==-1){
				switch (axis) {
	
				case "G":
					if(val==29){
						setPos=true;
					}
					break;
				case "D":
					if(val==1){
						breakEnable=true;
					}
					break;
				case "F":
					new_velocity = val;
					if (m.group(2) != null) {
						return "cannot increment or decrement velocity";
					}
					break;
				default:
					throw new Exception("Axis " + axis + " is not a valid axis");
				}
			}
			else{
				if (m.group(2) == null) {
					nextPos[axisN] = val;
				} else {
					switch (m.group(2)) {
					case "+=":
						nextPos[axisN] = val;
						incPos[axisN] = true;
						break;
					case "-=":
						nextPos[axisN] = -val;
						incPos[axisN] = true;
						break;
					}
				}
			}
			wholeMatch = m.hitEnd();
		}

		if (!wholeMatch) {
			return null;
		}
		synchronized (moveSema) {

			if (ownerThread == null) {
				ownerThread = Thread.currentThread();
			} else if (ownerThread != Thread.currentThread()) {
				throw new Exception(
						"The motion Card is used by an oher Client try later");
			}
			if (!movePermited) {
				throw new Exception("Motion not activated");
			}
			velocity = new_velocity;
			double distance = 0;
			ByteBuffer adsBuffer = ByteBuffer.allocate(2 * Double.SIZE
					* numAxis);

			for (int i = 0; i < numAxis; i++) {
				if (!Double.isNaN(nextPos[i])) {
					if (incPos[i]) {
						nextPos[i] += currentPos[i];
					}

					distance += (currentPos[i] - nextPos[i])
							* (currentPos[i] - nextPos[i]);
				}
			}
			distance = Math.sqrt(distance);
			if(setPos){
				System.out.print("SetPos ");
			}
			else{
				System.out.print("Goto ");
			}
			for (int i = 0; i < numAxis; i++) {

				if (!Double.isNaN(nextPos[i]) && nextPos[i] != currentPos[i]) {
					axisVelocity[i] = Math.abs(nextPos[i] - currentPos[i])
							/ distance * velocity;
					currentPos[i] = nextPos[i];
					System.out.print("  [" + i + "]:" + currentPos[i] + "->"
							+ axisVelocity[i]);
				} else {
					axisVelocity[i] = 10;
				}
			}
			
			if(setPos){
				instrType=2;
				for (int i = 0; i < numAxis; i++) {
					axisVelocity[i] = 10;
				}
			}
			else{
				if(breakEnable){
					instrType=3;
				}
				else{
					instrType=1;
				}
			}

			System.out.println();

			
			
			for (int i = 0; i < numAxis; i++) {
				adsBuffer.put(Convert.DoubleToByteArr(currentPos[i]));
			}
			for (int i = 0; i < numAxis; i++) {
				adsBuffer.put(Convert.DoubleToByteArr(axisVelocity[i]));
			}
			adsBuffer.put(Convert.ShortToByteArr(instrType));
			
			ads.write(actItemAdsSetting, adsBuffer.array(),
					adsBuffer.position());

			nextStatus = nextStatus == 0 ? 1 : 0;
			setNextStatus(nextStatus);
			waitStatus(nextStatus);
			lastCmdTime = System.currentTimeMillis();
			if(cmdId>=0){
				System.out.println("Wait ACK");
				do{
					Thread.sleep(100);
				}while(ads.readBit(inMotionAdsSetting)&&movePermited);
				System.out.println("Movement done");
				return "Movement=done";
			}
		}
		return "gcode=OK";

	}

	void waitStatus(int status) throws Exception {
		int readStatus;
		do {
			try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			readStatus = ads.readInt(actStatusAdsSetting);

		} while (status != readStatus);
	}

	int getStatus() throws Exception {
		return ads.readInt(actStatusAdsSetting);
	}

	void setNextStatus(int status) throws Exception {
		ads.write(nextStatusAdsSetting, status);
	}

	public void run() {
		while (true) {
			try {
				Thread.sleep(200);
				synchronized (moveSema) {
					if (ownerThread != null) {
						boolean inMotion = ads.readBit(inMotionAdsSetting);
						if (inMotion) {
							lastCmdTime = System.currentTimeMillis();
						} else {
							if ((System.currentTimeMillis() - lastCmdTime) >= 2000) {
								ownerThread = null;
							}
						}
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	public static void main(String args[]) {
		try {
			adsConnection config = new adsConnection();
			adsMove move = new adsMove(config);
			System.out.println("Move returned:" + move.sendCmd("X2Y2F5",-1));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
