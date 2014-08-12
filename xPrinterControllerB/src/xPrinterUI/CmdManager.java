package xPrinterUI;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JOptionPane;
import javax.swing.Timer;

public class CmdManager implements ActionListener {
	Pattern pat= Pattern.compile("@([\\d]+)\\s*(Success:)?([\\w]+)\\s*=(.+)\\s*");
	TcpIpConnection connection;

	Random randomGenerator = new Random();
	Map<String, VentilItem> paramListner = new TreeMap<String, VentilItem>();
	Map<String, VentilItem> poolingListner = new TreeMap<String, VentilItem>();

	public CmdManager(String host,int port) {
		connection = new TcpIpConnection(this, host, port);
		Timer timer = new Timer(200, this);
		timer.start();
	}

	public void sendCmd(String cmd) {
		connection.write( cmd);
	}
	public void sendCmd(VentilItem cmd) {
		if(paramListner.containsKey(cmd.getItemName())){
			connection.write(cmd.getItemName()  + "=" + cmd.getItemValue());
		}
		else{
			connection.write("@1 " + cmd.getItemName()  + "=" + cmd.getItemValue());
		}
	}
	
	public void requestValue(String name){
		connection.write("@1 " + name+"?");
	}

	public void removeEventListner(VentilItem item){
		connection.write("removeEventListener("+item.getItemName()+")");
	}

	public void addEventListner(VentilItem item){
		addItemParamListner(item);
		connection.write("addEventListener("+item.getItemName()+")");
	}
	
	public void addItemParamListner(VentilItem item) {
		paramListner.put(item.getItemName() , item);
		//requestValue(item.name);
	}

	public void addItemPoolingListner(VentilItem item) {
		poolingListner.put(item.getItemName(), item);
		addItemParamListner(item);
	}

	public void connRecived(String in) {
		String name, cmd;
		if (in.startsWith("#")) {
			return;
		} else {
			Matcher mat = pat.matcher(in);
			if (mat.find()) {
				name = mat.group(3);
				cmd = mat.group(4);
			} else {
				System.err.println("Cannot decode '" + in + "'");
				return;
			}
		}
		VentilItem item;
		item = paramListner.get(name);
		if (item != null) {
			item.recievedValue(cmd);
		}
	}

	public void actionPerformed(ActionEvent e) {
		Iterator<Map.Entry<String, VentilItem>> it = poolingListner.entrySet()
				.iterator();
		while (it.hasNext()) {
			VentilItem item;
			Map.Entry<String,VentilItem> pairs = (Map.Entry<String,VentilItem>) it
					.next();
			item = pairs.getValue();
			requestValue( item.getItemName() );
		}
	}
}
