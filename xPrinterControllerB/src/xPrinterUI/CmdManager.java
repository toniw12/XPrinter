package xPrinterUI;

import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CmdManager  {
	Pattern pat= Pattern.compile("@([\\d]+)\\s*(Success:)?([\\w|\\.|\\[|\\]]+)\\s*=(.+)\\s*");
	TcpIpConnection connection;

	Map<String, VentilItem> paramListner = new TreeMap<String, VentilItem>();

	public CmdManager(String host,int port) {
		connection = new TcpIpConnection(this, host, port);
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
		paramListner.put(item.getItemName() , item);
		connection.write("addEventListener("+item.getItemName()+")");
	}
	

	public void addItemPoolingListner(VentilItem item) {
		sendCmd("addPoolingListener("+ item.getItemName()+")");
		addEventListner(item);
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
}
