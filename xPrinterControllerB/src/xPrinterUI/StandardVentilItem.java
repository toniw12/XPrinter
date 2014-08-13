package xPrinterUI;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JToggleButton;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;


enum VentilItemType {
    Spinner,Label,CheckBox,CmdButton,TgButton,IoButton
}

class StandardVentilItem extends JPanel implements VentilItem, MouseWheelListener, ChangeListener, ItemListener, ActionListener, KeyListener{
	CmdManager manag;
	JSpinner spinner;
	JLabel label;
	JCheckBox checkBox;
	JButton button;
	JToggleButton tgButton;
	VentilItemType type;
	String name;
	public StandardVentilItem(CmdManager manag,String name,VentilItemType type){
		this.type=type;
		this.manag=manag;
		this.name=name;
		
		setLayout(new BorderLayout());
		setPreferredSize(new Dimension(100,30));
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
			label.setBorder(new TitledBorder(name));
			add(label,BorderLayout.CENTER);
			break;
		case CheckBox:
			checkBox = new JCheckBox();
			checkBox.addItemListener(this);
			add(checkBox,BorderLayout.CENTER);
			add(new JLabel(name),BorderLayout.WEST);

			break;
		case IoButton:
		case CmdButton:
			button = new JButton(name);
			button.addActionListener(this);
			button.addKeyListener(this);
			add(button,BorderLayout.CENTER);
			break;
		case TgButton:
			tgButton=new JToggleButton(name);
			tgButton.addActionListener(this);
			add(tgButton,BorderLayout.CENTER);
			break;
		}
		manag.addEventListner(this);
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
		manag.sendCmd(this);

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
			label.setText(val);
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
			break;
		case TgButton:
			tgButton.removeActionListener(this);
			if(Integer.parseInt( val)==1){
				//System.out.println("set selected True");
				tgButton.setSelected(true);
			}
			else{
				//System.out.println("set selected False");
				tgButton.setSelected(false);
			}
			tgButton.addActionListener(this);
			break;
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
		case CmdButton:
			return "1";
		case IoButton:
			return button.isSelected()?"1":"0";
		case TgButton:	
			return tgButton.isSelected()?"1":"0";
			
		}
		return "0";
	}

	public void itemStateChanged(ItemEvent e) {
		sendActualValue();
	}

	public void actionPerformed(ActionEvent e) {
		if(type==VentilItemType.CmdButton||type==VentilItemType.TgButton){
			sendActualValue();
			//System.out.println("send value "+getItemValue());
		}
	}

	public String getItemName() {
		return name;
	}

	public void keyPressed(KeyEvent arg0) {
		if(type==VentilItemType.IoButton){
			sendActualValue();
		}
	}


	public void keyReleased(KeyEvent arg0) {
		if(type==VentilItemType.IoButton){
			sendActualValue();
		}
	}


	@Override
	public void keyTyped(KeyEvent arg0) {
		// TODO Auto-generated method stub
		
	}
}
