package adsInterface;

import java.util.HashMap;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class variableListener extends comandInterpreter{
	Semaphore eventMapSema=new Semaphore(1);
	Map<String,Vector<adsInterface>> eventMap=new HashMap<String,Vector<adsInterface>>();
	public void addListener(String cmdName,adsInterface inter) throws InterruptedException{
		while(!eventMapSema.tryAcquire(100,TimeUnit.MILLISECONDS)){
			System.err.println("variableListener.addListener: Cannot aquire 'eventMapSema'");
		}
		Vector<adsInterface> interVec=eventMap.get(cmdName);
		if(interVec!=null){
			interVec.add(inter);
		}
		else{
			interVec=new Vector<adsInterface> ();
			interVec.add(inter);
			eventMap.put(cmdName, interVec);
		}
		eventMapSema.release();
	}
	
	
	public void removeListener(String cmdName,adsInterface inter) throws InterruptedException{
		
		while(!eventMapSema.tryAcquire(100,TimeUnit.MILLISECONDS)){
			System.err.println("variableListener.removeListener: Cannot aquire 'eventMapSema'");
		}
		Vector<adsInterface> interVec=eventMap.get(cmdName);
		if(interVec!=null){
			interVec.remove(inter);
		}
		eventMapSema.release();
	}

	public String setVariable(String varName, operation oper, String value)
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
}
