package adsInterface;

import java.io.*;
import java.net.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class adsInterface extends comandInterpreter implements Runnable {

	final static int port = 27015;
	static Pattern cmdPattern = Pattern
			.compile("\\A\\s*(\\@[0-9]+)\\s*");
	Socket socketClient;
	comandInterpreter[] cmdInerp;
	BufferedReader inputStream;
	PrintStream outputStream;
	variableListener listener;


	public String callFunc(String func, String[] args) throws Exception {
		switch(func){
		case "exit":
			System.out.println("Recived command 'Exit'");
			System.exit(0);
			return "Exit";
		case "addEventListener":
			listener.addListener(args[0], this);
			if(args.length!=1){
				throw new Exception("Give one String Argument");
			}
			return "eventListenerAdded";
		case "removeEventListener":
			listener.removeListener(args[0], this);
			if(args.length!=1){
				throw new Exception("Give one String Argument");
			}
			return "eventListenerRemoved";
		default:
				return null;
		}
	}

	public adsInterface(BufferedReader inputStream,PrintStream outputStream, comandInterpreter[] cmdInerp,variableListener listener) throws IOException {
		this.inputStream = inputStream;
		this.outputStream = outputStream;
		this.cmdInerp=cmdInerp;
		this.listener=listener;
	}
	
	public void sendEventMsg(String msg){
		try {
			synchronized (outputStream) {
				outputStream.println("@0 "+msg);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}


	public static void main(String[] args) {
		try {
			adsConnection ads=new adsConnection("ADS_settings.txt");
			comandInterpreter[] cmdInerp={ads,new adsMove(ads),new Corona()};
			variableListener listener=new variableListener();
			ServerSocket socketServeur = new ServerSocket(port);
			System.out.println("MultipleSocketServer Initialized");

			Runnable runnable = new adsInterface(new BufferedReader(new InputStreamReader(System.in)),new PrintStream(System.out), cmdInerp,listener);
			Thread thread = new Thread(runnable);
			thread.start();
			
			while (true) {
				Socket connection = socketServeur.accept();
				BufferedReader inputStream = new BufferedReader(new InputStreamReader(
						connection.getInputStream()));
				PrintStream	 outputStream = new PrintStream(connection.getOutputStream());
				runnable = new adsInterface(inputStream,outputStream, cmdInerp,listener);
				thread = new Thread(runnable);
				thread.start();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}	
	}

	public void run() {
		try {
			String inputLine=null;
			if(socketClient!=null){
				System.out.println("Connexion avec : "
						+ socketClient.getInetAddress());
			}
			
			synchronized (outputStream) {
				outputStream.println("Connection Opend");
			}

			while(true){
				int cmdId;
				boolean error=false;
				try{
				inputLine = inputStream.readLine();
				} catch (IOException e) {
					if(socketClient!=null){
						System.err.println("Error while read line from "+socketClient.getInetAddress());
					}
					return ;
				}
				if(inputLine==null){break;}
				String cmdLine;
				Matcher m = cmdPattern.matcher(inputLine);
				if(m.find()){
					String atId=m.group(1);
					if(atId==null){cmdId=-1;}
					else{cmdId=Integer.parseInt(atId.substring(1));}
					cmdLine=inputLine.substring(m.group(0).length());
				}
				else{
					cmdId=-1;
					cmdLine=inputLine;
				}
				String retMsg="";
				try {
					boolean comandInterpreted=false;
					retMsg = sendCmd(cmdLine);
					if(retMsg!=null) {
						comandInterpreted=true;
					}
					else{
						for(comandInterpreter interp:cmdInerp){
							retMsg = interp.sendCmd(cmdLine);
							if(retMsg!=null) {
								comandInterpreted=true;
								break;
							}
						}
					}
					if (!comandInterpreted){
						error=true;
						retMsg="Cannot decode '"+ cmdLine+"'";
					}
				} catch (Exception e) {
					error=true;
					retMsg= e.getMessage();
				}
				
				synchronized (outputStream) {
					if(error){
						System.out.println("Error: ("+cmdLine+"): "+(cmdId==-1?"":"@"+cmdId)+retMsg);
						outputStream.println(cmdId==-1?"":"@"+cmdId+" Error: "+retMsg);
					}
					else if(cmdId!=-1){
						outputStream.println("@"+cmdId+" "+retMsg);
						
					}
				}
				if(!error){
					listener.sendCmd(retMsg);
				}
			}
			if(socketClient!=null){
				System.out.println("Close connection with "+ socketClient.getInetAddress());
				socketClient.close();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}	
	}
}
