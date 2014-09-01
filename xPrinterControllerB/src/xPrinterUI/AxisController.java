package xPrinterUI;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputMethodEvent;
import java.awt.event.InputMethodListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.JToggleButton;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

enum SliderControllerType{
	stepSize,override,velocity
}

class SliderController extends JSlider implements VentilItem, ChangeListener {
	String name;
	CmdManager manag;
	SliderControllerType type;
	Dictionary<Integer, JLabel> dict = new Hashtable<Integer, JLabel>(); 
	public SliderController(CmdManager manag,String name,SliderControllerType type){
		super(JSlider.HORIZONTAL);
		this.name=name;
		this.manag=manag;
		this.type=type;
		addChangeListener(this);
		setPreferredSize(new Dimension(250,50));
		setBorder(BorderFactory.createTitledBorder(name));
		manag.addEventListner(this);
		//manag.addItemPoolingListner(this);
		switch(type){
		case stepSize:
			int i=0;
			dict.put(i++, new JLabel("0.01"));
			dict.put(i++, new JLabel("0.02"));
			dict.put(i++, new JLabel("0.05"));
			dict.put(i++, new JLabel("0.1"));
			dict.put(i++, new JLabel("0.2"));
			dict.put(i++, new JLabel("0.5"));
			dict.put(i++, new JLabel("1"));
			dict.put(i++, new JLabel("2"));
			dict.put(i++, new JLabel("5"));
			setPaintLabels(true);
			setSnapToTicks(true);
			setMinorTickSpacing(1);
			setLabelTable(dict );
			setMaximum(dict.size()-1);
			break;
		case override:
			setMaximum(100);
			break;
		case velocity:
			setMaximum(500);
			break;
		}	
	}

	public void sendActualValue() {
		manag.sendCmd(this);
	}

	public void recievedValue(String val) {
		setItemValue(val);
	}

	public void setItemValue(String val) {
		double dVal=Double.parseDouble(val);
		int index=0;
		if(type==SliderControllerType.stepSize){
			Enumeration<JLabel>  elems= dict.elements();
			index=dict.size();
			while(elems.hasMoreElements()){
				index--;
				if(dVal==Double.parseDouble( elems.nextElement().getText())){
					break;
				}		
			}
		}
		else{
			index=(int)dVal;
		}
		removeChangeListener(this);
		setValue(index);
		addChangeListener(this);
	}

	public void stateChanged(ChangeEvent arg0) {
		if(!getValueIsAdjusting()||(type==SliderControllerType.override)){
			sendActualValue();
		}	
	}

	public String getItemValue() {
		if(type==SliderControllerType.stepSize){
			return dict.get(getValue()).getText();
		}
		else{
			return getValue()+"";
		}
	}

	public String getItemName() {		
		return name;
	}	
}

class AxisControllerItem extends JPanel implements VentilItem, ActionListener, MouseWheelListener, KeyListener, DocumentListener{
	AxisController controller;
	JToggleButton axisSelect;
	JTextField axisPos;
	JButton setPos;

	int index;
	String name;
	String textEntered="";
	boolean releasedButton=true;
	CmdManager manag;
	public AxisControllerItem(AxisController controller,CmdManager manag,String name,int index){
		this.name=name;
		this.index=index;
		this.manag=manag;
		this.controller=controller;
		axisSelect=new JToggleButton(name);
		axisSelect.addMouseWheelListener(this);
		axisSelect.addKeyListener(this);
		axisSelect.addActionListener(this);
		axisPos=new JTextField();
		axisPos.getDocument().addDocumentListener(this);
		axisPos.addActionListener(this);
		axisPos.setPreferredSize(new Dimension(100,25));
		axisSelect.setPreferredSize(new Dimension(80,25));
		setPos=new JButton("SetPos");
		setPos.addActionListener(this);
		manag.addItemPoolingListner(this);
		setLayout(new FlowLayout());
		add(axisPos);
		add(axisSelect);
		add(setPos);
	}

	public void actionPerformed(ActionEvent arg0) {
		try{
		if(arg0.getSource()==axisSelect){
			if (axisSelect.isSelected()) {
				controller.axisSelect(index);
			} else {
				controller.axisSelect(-1);
			}
		}
		else if(arg0.getSource()==axisPos){
			controller.validatePos(index, Calculator.Calculator.convert(textEntered));
		}
		else if (arg0.getSource()==setPos){
			controller.setPos(index, Calculator.Calculator.convert(textEntered));
		}
		}
		catch (NumberFormatException e){
			System.err.println("Cannot convert '"+ textEntered+"' to Double");
		}
		textEntered="";
	}

	public void mouseWheelMoved(MouseWheelEvent arg0) {	
		textEntered="";
		controller.incrementPos(index, (double) arg0.getWheelRotation()*-1);
	}

	public void keyPressed(KeyEvent arg0) {
		textEntered="";
		if(releasedButton){
			switch(arg0.getKeyCode()){
			case  KeyEvent.VK_PLUS:
			case  KeyEvent.VK_ADD:
			case  49:
				controller.incrementPos(index, 1);
				break;
			case  KeyEvent.VK_MINUS:
			case  KeyEvent.VK_SUBTRACT:
				controller.incrementPos(index, -1);
				break;
			}
			releasedButton=false;
		}	
	}
	
	public void actualizePos(double newPos){
		if(!axisPos.isFocusOwner()){
			
			axisPos.setText(newPos+"");
		}
	}

	public void keyReleased(KeyEvent arg0) {
		releasedButton=true;
	}

	public void keyTyped(KeyEvent arg0) {}
	
	public void setSelected(boolean selected){
		textEntered="";
		axisSelect.removeActionListener(this);
		axisSelect.setSelected(selected);
		axisSelect.addActionListener(this);
	}

	public void setValue(String val) {
		actualizePos(Double.parseDouble(val));
	}

	public String getItemValue(){
		return axisPos.getText();
	}

	public void recievedValue(String val) {
		setValue(val);
	}

	public void setItemValue(String val) {
		setValue(val);
	}

	public String getItemName() {
		return name;
	}
	
	public void axisPosUpdate(){
		if(axisPos.isFocusOwner()){
			textEntered=axisPos.getText();
		}
	}

	public void changedUpdate(DocumentEvent arg0) {}
	public void insertUpdate(DocumentEvent e) {axisPosUpdate();}
	public void removeUpdate(DocumentEvent e) {axisPosUpdate();}
}

public class AxisController extends JPanel{
	AxisControllerItem[] axisItems;
	String axisNames[];
	int axisSelected=-1;
	CmdManager manag;
	SliderController stepSize;
	SliderController velocity;
	SliderController override;
	
	StandardVentilItem axisActive;
	StandardVentilItem axisOwner;
	
	public AxisController(String axisNames[],CmdManager manag){
		this.manag=manag;
		this.axisNames=axisNames;

		GridBagLayout gridLayout=new GridBagLayout();
		GridBagConstraints c = new GridBagConstraints();
		setLayout(gridLayout);
		
		axisActive=new StandardVentilItem(manag, "MotionActive", VentilItemType.TgButton);
		axisOwner=new StandardVentilItem(manag, "Move_owner", VentilItemType.Label);
		manag.removeEventListner(axisOwner);
		
		axisItems=new AxisControllerItem[axisNames.length];
		c.gridy++;
		add(axisActive,c);
		c.gridy++;
		
		add(axisOwner,c);
		c.gridy++;
		
		int index;
		for( index=0;index<axisNames.length;index++){
			axisItems[index]=new AxisControllerItem(this,manag,axisNames[index],index);
			add(axisItems[index],c);
			c.gridy++;
		}

		stepSize=new SliderController(manag,"StepSize",SliderControllerType.stepSize);
		override=new SliderController(manag,"Override",SliderControllerType.override);
		velocity=new SliderController(manag,"Velocity",SliderControllerType.velocity);

		c.gridy++;
		add(stepSize,c);
		c.gridy++;
		add(override,c);
		c.gridy++;
		add(velocity,c);
	}
	
	public void incrementPos(int index,double pos){
		double posInc=pos* Double.parseDouble( stepSize.getItemValue());
		if(axisSelected==index){
			manag.sendCmd(axisNames[index]+"+="+posInc+" F"+velocity.getValue());
			//System.out.println(axisNames[index]+"+="+posInc+" F"+velocity.getValue());
		}
	}
	
	public double getStepSize(){
		return Double.parseDouble(stepSize.getItemValue());
	}
	
	public void validatePos(int index,double pos){
		manag.sendCmd(axisNames[index]+pos+" F"+velocity.getValue());
		//System.out.println(axisNames[index]+pos+" F"+velocity.getValue());
	}
	
	public void setPos(int index,double pos){
		manag.sendCmd("G29 "+axisNames[index]+pos+" F"+velocity.getValue());
		//System.out.println("G29 "+axisNames[index]+pos+" F"+velocity.getValue());
	}
	public void axisSelect(int index){
		if (index >= 0) {
			for (int i = 0; i < axisNames.length; i++) {
				axisItems[i].setSelected(i == index);
			}
		}
		axisSelected=index;
	}

	public static void main(String[] args) {
		String[] axisNames={"X","Y","Z"};
		AxisController axisController =new AxisController(axisNames,null);

		JFrame frame=new JFrame("Position Controller");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.getContentPane().add(axisController);
		frame.pack();
		frame.setVisible(true);
	}

}
