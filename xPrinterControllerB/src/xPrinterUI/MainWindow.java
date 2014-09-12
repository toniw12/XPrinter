package xPrinterUI;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.InputMethodListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.text.DefaultFormatter;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;


public class MainWindow extends JFrame {
	IoControl ioContorl;
	TempController tempControl;
	AxisController controller;

	Box bv = Box.createVerticalBox();
	String hostName = "localhost";
	int portNumber = 27015;
	CmdManager manag;
	JTabbedPane tabbedPane = new JTabbedPane();
	String[] axisNames={"X","Y","Z"};
	


	public MainWindow(String arg) {
		super("Ventil steerung");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		System.out.println("arg=" +arg);
		
		if(!arg.equals("")){
			if (arg.contains(":")) {
			    String[] args=arg.split(":");
			    if(args.length==2){
			    	hostName=args[0];
			    	portNumber=Integer.parseInt( args[1]);
			    }
			    else{
			    	System.out.println("invalid MainWindow arguments");
			    	System.exit(1);
			    }
			} else {
				hostName=arg;
			}
		}
		

		manag=new CmdManager(hostName,portNumber);
		ioContorl=new IoControl(manag);
		tempControl=new TempController(manag);
		controller=new AxisController(axisNames,manag);
		tabbedPane.addTab("Beckhoff", ioContorl);
		tabbedPane.addTab("Temperatur", tempControl);
		tabbedPane.addTab("Position", controller);
		
		
		 getContentPane().add(tabbedPane, BorderLayout.CENTER);

		setVisible(true);
		
		setSize(new Dimension(350,500));

		addWindowListener(new WindowListener() {
			public void windowClosed(WindowEvent arg0) {
			}

			public void windowActivated(WindowEvent arg0) {
			}

			public void windowClosing(WindowEvent arg0) {

			}

			public void windowDeactivated(WindowEvent arg0) {
			}

			public void windowDeiconified(WindowEvent arg0) {
			}

			public void windowIconified(WindowEvent arg0) {
			}

			public void windowOpened(WindowEvent arg0) {
			}
		});
	}

	
	public static void main(String[] args) {
		
		if(args.length==1){
			new MainWindow(args[0]);
		}
		else{
			new MainWindow("");
		}
		
		
	}
}
