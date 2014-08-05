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
	Map<String, StandardVentilItem> valListner = new TreeMap<String, StandardVentilItem>();
	Map<String, StandardVentilItem> paramListner = new TreeMap<String, StandardVentilItem>();
	Map<String, StandardVentilItem> poolingListner = new TreeMap<String, StandardVentilItem>();

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

	public void addValListner(StandardVentilItem item) {	
		valListner.put(item.name , item);
	}

	public void addItemParamListner(StandardVentilItem item) {
		connection.write("addEventListener("+item.name+")");
		paramListner.put(item.name , item);
		//requestValue(item.name);
	}

	public void addItemPoolingListner(StandardVentilItem item) {
		poolingListner.put(item.getFullName(), item);
		addItemParamListner(item);
	}

	public void uartReciveCmd(String in) {
		CmdItem cmd = decodeCmd(in);
		StandardVentilItem item;
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
		int randomVal = randomGenerator.nextInt();
		Iterator<Map.Entry<String, StandardVentilItem>> it = poolingListner.entrySet()
				.iterator();
		while (it.hasNext()) {
			StandardVentilItem item;
			Map.Entry<String, StandardVentilItem> pairs = (Map.Entry<String, StandardVentilItem>) it
					.next();
			item = pairs.getValue();
			sendCmd(item.getFullName() + "?");
		}
	}

}
