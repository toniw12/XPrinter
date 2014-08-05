import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;


enum VentilItemType {
    Spinner,Label,CheckBox,Button
}

class StandardVentilItem extends JPanel implements VentilItem, MouseWheelListener, ChangeListener, ItemListener, ActionListener{
	CmdManager manag;
	JSpinner spinner;
	JLabel label;
	JCheckBox checkBox;
	JButton button;
	VentilItemType type;
	String name;
	public StandardVentilItem(CmdManager manag,String name,VentilItemType type){
		this.type=type;
		this.manag=manag;
		this.name=name;
		
		setLayout(new BorderLayout());
		setPreferredSize(new Dimension(100,20));
		switch (type) {
		case Spinner:
			spinner = new JSpinner();
			spinner.addChangeListener(this);
			spinner.addMouseWheelListener(this);
			add(spinner,BorderLayout.CENTER);
			add(new JLabel(name),BorderLayout.WEST);

			break;
		case Label:
			manag.addItemPoolingListner(this);
			label = new JLabel();
			label.setText("0");
			add(label,BorderLayout.CENTER);
			add(new JLabel(name),BorderLayout.WEST);
			break;
		case CheckBox:
			checkBox = new JCheckBox();
			checkBox.addItemListener(this);
			add(checkBox,BorderLayout.CENTER);
			add(new JLabel(name),BorderLayout.WEST);

			break;
		case Button:
			button = new JButton(name);
			button.addItemListener(this);
			button.addActionListener(this);
			add(button,BorderLayout.CENTER);
			break;
		}
		manag.addItemParamListner(this);
	}
	

	public void mouseWheelMoved(MouseWheelEvent e) {
		if(type==VentilItemType.Spinner){
			int val=Integer.parseInt(getItemValue());
			spinner.setValue(val-e.getWheelRotation()*(val/100+1));	
		}
	}
	public void stateChanged(ChangeEvent e) {
		//System.out.println( e);
		sendActualValue();
	}
	

	
	public void sendActualValue(){
		manag.sendCmd(new CmdItem(name,getItemValue()+""));

	}
	
	public void recievedValue(String val){
		setItemValue(val);
	}
	
	public void setItemValue(String val){
		switch (type) {
		case Spinner:
			if (val != getItemValue()+"") {
				spinner.removeChangeListener(this);
				spinner.setValue(Integer.parseInt( val));
				spinner.addChangeListener(this);
			}
			break;
		case Label:
			label.setText(val+"");
			break;
		case CheckBox:
			checkBox.removeItemListener(this);
			if(Integer.parseInt( val)==1){
				checkBox.setSelected(true);
			}
			else{
				checkBox.setSelected(false);
			}
			checkBox.addItemListener(this);

		}

	}
	public String getFullName(){
		return name;
	}
	
	public String getItemValue(){
		switch (type) {
		case Spinner:
			return ((Number)spinner.getValue()).intValue()+"";
		case Label:
			return label.getText();
		case CheckBox:
			int select=0;
			if(checkBox.isSelected()){
				select=1;
			}
			return select+"";
		case Button:

			return "1";

		}
		return "0";
	}

	public void itemStateChanged(ItemEvent e) {
		 
		sendActualValue();
	}

	public void actionPerformed(ActionEvent e) {
		
		sendActualValue();
		
	}

	public String getItemName() {
		return name;
	}
}
