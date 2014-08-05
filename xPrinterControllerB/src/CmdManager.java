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
	Pattern pat= Pattern.compile("@([\\d]+)\\s*(Success:)?([\\w]+)\\s*=([\\w]+)\\s*");
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
		connection.write("@1 " + cmd);
	}
	public void sendCmd(CmdItem cmd) {
		connection.write("@1 " + cmd.name  + "=" + cmd.value);
	}
	
	public void requestValue(String name){
		connection.write("@1 " + name+"?");
	}


	public void addItemParamListner(VentilItem item) {
		connection.write("addEventListener("+item.getItemName()+")");
		paramListner.put(item.getItemName() , item);
		//requestValue(item.name);
	}

	public void addItemPoolingListner(VentilItem item) {
		poolingListner.put(item.getItemName(), item);
		addItemParamListner(item);
	}

	public void connRecived(String in) {
		CmdItem cmd = decodeCmd(in);
		VentilItem item;
		if (cmd != null) {
			String cmdName = cmd.name;
			item = paramListner.get(cmdName);
			if (item != null) {
				item.recievedValue(cmd.value);
			}
		} else {
			System.err.println("cannot decode comand:" + in);
		}
	}


	public CmdItem decodeCmd(String in) {
		if (in.startsWith("#")) {
			System.err.println("CPU: '" + in + "'");
			return null;
		} else {
			Matcher mat = pat.matcher(in);
			String name;
			int value;
			if (mat.find()) {
				
				if (mat.end() != in.length()) {
					System.err.println("Length dont match " + mat.end() + " "
							+ in.length());
					return null;
				}

				name = mat.group(3);
				return new CmdItem(name, mat.group(4));

			} else
				System.err.println("Cannot decode '" + in + "'");
			return null;
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
