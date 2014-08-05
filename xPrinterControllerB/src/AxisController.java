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
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.JToggleButton;

class AxisControllerItem extends JPanel implements  ActionListener, MouseWheelListener, KeyListener{
	AxisController controller;
	JToggleButton axisSelect;
	JTextField axisPos;
	JLabel axisNameLabel;
	int index;
	boolean releasedButton=true;
	public AxisControllerItem(AxisController controller,String axisName,int index){
		this.index=index;
		this.controller=controller;
		axisSelect=new JToggleButton(axisName);
		axisSelect.addMouseWheelListener(this);
		axisSelect.addKeyListener(this);
		axisSelect.addActionListener(this);
		axisPos=new JTextField();
		axisNameLabel=new JLabel(axisName);
		axisNameLabel.setFont(axisNameLabel.getFont().deriveFont(30));
		axisPos.addActionListener(this);
		axisPos.setPreferredSize(new Dimension(100,25));
		axisSelect.setPreferredSize(new Dimension(80,25));
		axisNameLabel.setPreferredSize(new Dimension(50,25));
		setLayout(new FlowLayout());
		add(axisNameLabel);
		add(axisPos);
		add(axisSelect);
	}

	@Override
	public void actionPerformed(ActionEvent arg0) {
		if(arg0.getSource()==axisSelect){
			if (axisSelect.isSelected()) {
				controller.axisSelect(index);
			} else {
				controller.axisSelect(-1);
			}
		}
		else if(arg0.getSource()==axisPos){
			controller.validatePos(index, Double.parseDouble(axisPos.getText()));
		}
	}

	public void mouseWheelMoved(MouseWheelEvent arg0) {	
		controller.incrementPos(index, (double) arg0.getWheelRotation()*-1);
	}

	public void keyPressed(KeyEvent arg0) {
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
		System.out.println("newPos:"+newPos);
		if(!axisPos.isFocusOwner()){
			System.out.println("SetPos");
			axisPos.setText(newPos+"");
		}
	}

	public void keyReleased(KeyEvent arg0) {
		releasedButton=true;
	}

	public void keyTyped(KeyEvent arg0) {}
	
	public void setSelected(boolean selected){
		axisSelect.removeActionListener(this);
		axisSelect.setSelected(selected);
		axisSelect.addActionListener(this);
	}


	public void sendActualValue() {

	}

	public void recievedValue(int val) {

	}


	public void setValue(int val) {
		
	}

	public int getValue(){
		return 0;
	}
}

public class AxisController extends JPanel{
	AxisControllerItem[] axisItems;
	String axisNames[];
	int axisSelected=-1;
	JSlider stepSize;

	JSlider override;
	Dictionary<Integer, JLabel> dict = new Hashtable<Integer, JLabel>(); 
	public AxisController(String axisNames[]){
		this.axisNames=axisNames;
		GridBagLayout gridLayout=new GridBagLayout();
		GridBagConstraints c = new GridBagConstraints();
		setLayout(gridLayout);
		axisItems=new AxisControllerItem[axisNames.length];
		
		int index=0;
		for(String axisName:axisNames){
			axisItems[index]=new AxisControllerItem(this,axisName,index);
			c.gridy=index;
			add(axisItems[index],c);
			index++;
		}
		
		
		int i=1;
		dict.put(i++, new JLabel("0.01"));
		dict.put(i++, new JLabel("0.02"));
		dict.put(i++, new JLabel("0.05"));
		dict.put(i++, new JLabel("0.1"));
		dict.put(i++, new JLabel("0.2"));
		dict.put(i++, new JLabel("0.5"));
		dict.put(i++, new JLabel("1"));
		dict.put(i++, new JLabel("2"));
		dict.put(i++, new JLabel("5"));
		
		 stepSize = new JSlider(JSlider.HORIZONTAL,1,dict.size(),1);
		 
		 override=new JSlider(JSlider.HORIZONTAL,1,100,1);
		 stepSize.setPreferredSize(new Dimension(250,50));
		 override.setPreferredSize(new Dimension(250,50));
		stepSize.setLabelTable(dict );
		stepSize.setPaintLabels(true);
		stepSize.setSnapToTicks(true);
		stepSize.setMinorTickSpacing(1);
		c.gridy=index++;
		stepSize.setBorder(BorderFactory.createTitledBorder("Step size"));
		
		override.setBorder(BorderFactory.createTitledBorder("Overrride"));
		add(stepSize,c);
		c.gridy=index++;
		add(override,c);
	}
	
	public void incrementPos(int index,double pos){
		double posInc=pos* Double.parseDouble( dict.get(stepSize.getValue()).getText());
		if(axisSelected==index){
			System.out.println(axisNames[index]+"+="+posInc);
		}
	}
	

	
	public void validatePos(int index,double pos){
		System.out.println(axisNames[index]+pos);
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
		AxisController axisController =new AxisController(axisNames);

		JFrame frame=new JFrame("Position Controller");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.getContentPane().add(axisController);
		frame.pack();
		frame.setVisible(true);
	}

}
