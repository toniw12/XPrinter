package adsInterface;

import java.util.HashMap;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class variableListener extends comandInterpreter implements Runnable{


	Map<String,Vector<adsInterface>> eventMap=new HashMap<String,Vector<adsInterface>>();
	Vector<String> pollingList=new Vector<String>();
	
	public variableListener(){
		new Thread(this).run();
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
		
	}
}
