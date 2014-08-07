package adsInterface;

import java.nio.ByteBuffer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.beckhoff.jni.Convert;



public class adsMove extends comandInterpreter {
	Object moveSema=new Object();
	static Pattern gcodePattern = Pattern
			.compile("([A-Z])(\\+=|-=)?([-+]?[0-9]*\\.?[0-9]+)");
	adsConnection ads;
	static final int numAxis=2;
	double currentPos[] =new double[numAxis];
	double velocity=10;
	double configVelocity=10;
	double stepSize=0.01;
	double override=0;
	int nextStatus=0;
	Thread ownerThread=null;
	adsConfigItem[] actPosAdsSetting=new adsConfigItem[numAxis];

	
	adsConfigItem actItemAdsSetting;
	
	adsConfigItem		actStatusAdsSetting, nextStatusAdsSetting;

	public adsMove(adsConnection ads)throws Exception{
		this.ads = ads;
		this.ads = ads;
		
		actItemAdsSetting=ads.adsGetConfig("fifoItem");
		actPosAdsSetting[0]=ads.adsGetConfig("xActPos");
		actPosAdsSetting[1]=ads.adsGetConfig("yActPos");

		adsConfigItem actStateAdsSetting = ads.adsGetConfig("state");
		
		System.out.println("Move: Waiting state Becomes 3...");
		while(ads.readInt(actStateAdsSetting)!=3){
			Thread.sleep(100);
		}
		System.out.print("Move: Reading actual position");

		currentPos[0]=ads.readDouble(actPosAdsSetting[0]);
		currentPos[1]=ads.readDouble(actPosAdsSetting[1]);
		
		System.out.println(" X"+currentPos[0] +" Y"+currentPos[1]);
		
		actStatusAdsSetting = ads.adsGetConfig("actStatus");
		nextStatusAdsSetting = ads.adsGetConfig("nextStatus");
		nextStatus=getStatus();
		
		
	}
	
	public String getVariable(String varName) throws Exception{
		switch(varName){
		case "X":
			return String.format("X=%.3f", ads.readDouble( actPosAdsSetting[0]));
		case "Y":	
			return String.format("Y=%.3f", ads.readDouble( actPosAdsSetting[1]));
		default:
			return null;
		}
	}
	
	public String setVariable(String varName, operation oper, String value)throws Exception {

		try{
			switch (varName) {
			case "Velocity":
				configVelocity = Double.parseDouble(value);
				break;
			case "StepSize":
				stepSize = Double.parseDouble(value);
				break;
			case "Override":
				override = Double.parseDouble(value);
				break;
			default:
				return null;
			}
		}
		catch(NumberFormatException e){
			throw new Exception("Cannot convert '"+value+"' to a double");
		}
		if(oper!=operation.SET){
			throw new Exception("Cannot increment "+ varName);
		}
		System.out.println(varName+"="+value);
		return varName+"="+value;
	}
	
	public String sendCmd(String cmd) throws Exception {
		super.sendCmd(cmd);
		String retString;
		if((retString=super.sendCmd(cmd))!=null){
			return retString;
		}
		Matcher m = gcodePattern.matcher(cmd);
		double newNextPos[] =new double[numAxis];
		double axisVelocity[] =new double[numAxis];
		for(int i=0;i<numAxis;i++){
			newNextPos[i]=Double.NaN;
		}
		double new_velocity=velocity;
		boolean wholeMatch=false;
		while (m.find()) {
			int axisN=-1;
			char axis = m.group(1).trim().charAt(0);
			double val = Double.parseDouble(m.group(3));
			switch (axis) {
			case 'X':
				axisN=0;
				break;
			case 'Y':
				axisN=1;
				break;
			case 'F':
				new_velocity = val;
				if(m.group(2)!=null){
					return "cannot increment or decrement velocity";
				}
				break;
			default:
				throw new Exception("Axis " + axis + " is not a valid axis");
			}
			if(axisN!=-1){
				if(m.group(2)==null){
					newNextPos[axisN] = val;
				}
				else{
					switch (m.group(2)) {
					case "+=":
						newNextPos[axisN] = currentPos[axisN]+ val;
						break;
					case "-=":
						newNextPos[axisN] = currentPos[axisN]- val;
						break;
					}
				}
			}
			wholeMatch=m.hitEnd();
		}

		if(!wholeMatch){
			return null;
		}
		synchronized (moveSema) {
			velocity = new_velocity;
			double distance = 0;
			ByteBuffer adsBuffer = ByteBuffer.allocate(2*Double.SIZE*numAxis);

			for (int i = 0; i < numAxis; i++) {
				if (!Double.isNaN( newNextPos[i])) {
					distance += (currentPos[i] - newNextPos[i])
							* (currentPos[i] - newNextPos[i]);
				}
			}
			distance = Math.sqrt(distance);
			System.out.print("Goto ");
			for (int i = 0; i < numAxis; i++) {

				if (!Double.isNaN( newNextPos[i]) && newNextPos[i] != currentPos[i]) {
					axisVelocity[i] = Math.abs(newNextPos[i] - currentPos[i])
							/ distance * velocity;
					currentPos[i] = newNextPos[i];
					System.out.print("  [" + i + "]:" + currentPos[i] + "->"
							+ axisVelocity[i]);
				}
				else{
					axisVelocity[i]=10;
				}
			}
			
			System.out.println();
			
			for (int i = 0; i < numAxis; i++) {
				adsBuffer.put(Convert.DoubleToByteArr(currentPos[i]));
			}
			for (int i = 0; i < numAxis; i++) {
				adsBuffer.put(Convert.DoubleToByteArr(axisVelocity[i]));
			}
			
			ads.write(actItemAdsSetting, adsBuffer.array(),adsBuffer.position());
			
			nextStatus=nextStatus==0?1:0;
			setNextStatus(nextStatus);
			waitStatus(nextStatus);
		}
		return "Movement done";

	}



	void waitStatus(int status)throws Exception{
		int readStatus;
		do {
			try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			readStatus = ads.readInt(actStatusAdsSetting);

		} while (status != readStatus);
	}

	int getStatus()throws Exception{
		return ads.readInt(actStatusAdsSetting);
	}

	void setNextStatus(int status)throws Exception{
		ads.write(nextStatusAdsSetting, status);
	}

	public static void main(String args[]) {
		try {
			adsConnection config = new adsConnection("ADS_settings.txt");
			adsMove move = new adsMove(config);
			System.out.println("Move returned:"+move.sendCmd("X2Y2F5"));
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
