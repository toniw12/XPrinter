package adsInterface;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import xPrinterUI.MainWindow;

import com.sanityinc.jargs.CmdLineParser;
import com.sanityinc.jargs.CmdLineParser.Option;
/*
 * the adsInterface class contains the main method
 * it crates a new thread for every tcp/ip connection
 */
public class adsInterface extends comandInterpreter implements Runnable {

	final static int port = 27015; //TCP/IP port
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
		case "addPoolingListener":
			listener.addPoolingListener(args[0]);
			if(args.length!=1){
				throw new Exception("Give one String Argument");
			}
			return "poolingListenerAdded";
		case "removePoolingListener":
			listener.removePoolingListener(args[0]);
			if(args.length!=1){
				throw new Exception("Give one String Argument");
			}
			return "poolingListenerRemoved";
		default:
				return null;
		}
	}

	public adsInterface(BufferedReader inputStream,PrintStream outputStream, comandInterpreter[] cmdInerp,variableListener listener) throws IOException {
		this.inputStream = inputStream;
		this.outputStream = outputStream;
		this.cmdInerp= new comandInterpreter[cmdInerp.length+1];
		System.arraycopy(cmdInerp, 0, this.cmdInerp, 1, cmdInerp.length);
		this.cmdInerp[0]=this;
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

    private static void printUsage() {
        System.err.println(
"Usage: OptionTest [{-c,--debug} comPort] [{-a,--ads} AdsToJava.dll] [{-m,--move}] [{-i,--User interface}]");
    }

	public static void main(String[] args) {

		CmdLineParser parser = new CmdLineParser();

		Option<String> coronaOpt = parser.addStringOption('c', "corona");
		Option<String> adsOpt = parser.addStringOption('a', "ads");
		Option<Boolean> moveOpt = parser.addBooleanOption('m', "move");
		Option<String> interfaceOpt = parser.addStringOption('i', "interface");

		try {
			parser.parse(args);
		} catch (CmdLineParser.OptionException e) {
			System.err.println(e.getMessage());
			printUsage();
			System.exit(2);
		}
		String adsStr=parser.getOptionValue(adsOpt);
		String coronaStr=parser.getOptionValue(coronaOpt);
		Boolean moveBool=parser.getOptionValue(moveOpt);
		String interfaceStr=parser.getOptionValue(interfaceOpt);
		
		if(interfaceStr!=null){
			new MainWindow(interfaceStr);
			// MainWindow will block until the end
			return;
		}
		
		try {
			//"C:/TwinCAT/AdsApi/AdsToJava/x64/AdsToJava.dll"
			adsConnection ads=null;
			
			Vector<comandInterpreter> cmdInerpVect=new Vector<comandInterpreter>();
			if(adsStr!=null){
				ads=new adsConnection(adsStr);
				cmdInerpVect.add(ads);
				if(coronaStr!=null){cmdInerpVect.add(new Corona(coronaStr));}
				if(moveBool!=null){cmdInerpVect.add(new adsMove(ads));}
			}
			
			if(coronaStr!=null){cmdInerpVect.add(new Corona(coronaStr));}
			
			comandInterpreter[] cmdInerp=(comandInterpreter[])cmdInerpVect.toArray(new comandInterpreter[cmdInerpVect.size()]);
			variableListener listener=new variableListener(ads);
			
			ServerSocket socketServeur = new ServerSocket(port);
			Runnable cmdLineInterface = new adsInterface(new BufferedReader(new InputStreamReader(System.in)),new PrintStream(System.out), cmdInerp,listener);
			new Thread(cmdLineInterface).start();
			Runnable pollingInterface = new adsInterface(new BufferedReader(new InputStreamReader(listener.getPoolingStream())),new PrintStream(listener.getOutputStream()), cmdInerp,listener);
			new Thread(pollingInterface).start();
			
			while (true) {
				Socket connection = socketServeur.accept();
				BufferedReader inputStream = new BufferedReader(new InputStreamReader(
						connection.getInputStream()));
				PrintStream	 outputStream = new PrintStream(connection.getOutputStream());
				Runnable runnable = new adsInterface(inputStream,outputStream, cmdInerp,listener);
				new Thread(runnable).start();
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
					for (comandInterpreter interp : cmdInerp) {
						retMsg = interp.sendCmd(cmdLine, cmdId);
						if (retMsg != null) {
							comandInterpreted = true;
							break;
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
						outputStream.println((cmdId==-1?"":("@"+cmdId))+" Error: "+retMsg);
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
