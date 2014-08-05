import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Vector;

import javax.swing.SwingUtilities;


interface uartComListner {
	public void newMessage(String s);
}

public class TcpIpConnection implements Runnable {

	CmdManager cmdManager;
	PrintWriter out;
	BufferedReader in;
	Socket socket;
	
	Vector<String> responseList=new Vector<String>();
	
	public TcpIpConnection(CmdManager cmdManager,String hostName,int portNumber) {

		this.cmdManager=cmdManager;
		try {
			 socket = new Socket(hostName, portNumber);
			out = new PrintWriter(socket.getOutputStream(), true);
			in = new BufferedReader(new InputStreamReader(
					socket.getInputStream()));
			Thread thread = new Thread(this);
			thread.start();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void write(String msg) {
		if(out!=null){
			out.println(msg);
		}
	}
	private void recievedCmd() {
	    SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				boolean hasMoreElements=true;
				while (hasMoreElements) {
					String cmd;
					synchronized (responseList) {
						hasMoreElements = !responseList.isEmpty();
						if (hasMoreElements) {
							cmd = responseList.get(0);
							responseList.remove(0);
						}
						else {cmd=null;}
						
					}
					if(cmd!=null){
						cmdManager.connRecived(cmd);
					}
					else{
					}
					
				}

			}
	    });

	}

	public void run() {
		try {
			 while (socket.isConnected()) {
				String line=in.readLine();
				synchronized (responseList) {
					responseList.add(line);
				}
				recievedCmd();
				
			}
			 System.err.println("Socket not connected");
			 socket.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

}
