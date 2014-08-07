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

	JPanel test;

	Box bv = Box.createVerticalBox();
	String hostName = "localhost";
	int portNumber = 27015;
	CmdManager manag;
	JTabbedPane tabbedPane = new JTabbedPane();
	String[] axisNames={"X","Y"};
	AxisController controller;

	int num = 2;

	public MainWindow(String args[]) {
		super("Ventil steerung");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		if(args.length>0){
			hostName=args[0];
		}
		if(args.length>1){
			portNumber=Integer.parseInt( args[1]);
		}
		manag=new CmdManager(hostName,portNumber);
		controller=new AxisController(axisNames,manag);
		JOptionPane.setRootFrame(this);

		test = new JPanel();
		BoxLayout box=new BoxLayout(test, BoxLayout.Y_AXIS);
		test.setLayout(box);
		
		test.add(new StandardVentilItem(manag,"Flamme_Reset",VentilItemType.CheckBox));
		test.add(new StandardVentilItem(manag,"Flamme_Start",VentilItemType.CheckBox));
		test.add(new StandardVentilItem(manag,"Plasma_OFF",VentilItemType.CheckBox));
		test.add(new StandardVentilItem(manag,"UV_PowerBit0",VentilItemType.CheckBox));
		test.add(new StandardVentilItem(manag,"UV_PowerBit1",VentilItemType.CheckBox));
		test.add(new StandardVentilItem(manag,"UV_PowerBit2",VentilItemType.CheckBox));
		test.add(new StandardVentilItem(manag,"UV_ShutterRun",VentilItemType.CheckBox));
		test.add(new StandardVentilItem(manag,"Corona_Soll",VentilItemType.Spinner));
		test.add(new StandardVentilItem(manag,"Corona_Left",VentilItemType.Button));
		test.add(new StandardVentilItem(manag,"Corona_Right",VentilItemType.Button));
		test.add(new StandardVentilItem(manag,"Corona_Info",VentilItemType.Button));
		test.add(new StandardVentilItem(manag,"xActPos",VentilItemType.Label));
		test.add(new StandardVentilItem(manag,"yActPos",VentilItemType.Label));
		
	
		tabbedPane.addTab("Beckhoff", test);
		tabbedPane.addTab("Position", controller);
		
		
		 getContentPane().add(tabbedPane, BorderLayout.CENTER);

		setVisible(true);
		
		setSize(new Dimension(300,350));

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
		new MainWindow(args);
	}
}
