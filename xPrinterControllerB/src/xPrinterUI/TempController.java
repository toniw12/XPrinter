package xPrinterUI;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.JLabel;
import javax.swing.JPanel;


class TabDes{
	public TabDes(String name,String label,VentilItemType type){
		this.name=name;
		this.label=label;
		this.type=type;
	}
	String name;
	String label;
	VentilItemType type;
}

class TempController extends JPanel{
	private static final long serialVersionUID = 1L;
	static final int numTempController=16;
	
	String[] regulatorNames={"Test Regler",null,null,"Kein Regler"};
	

	
	TabDes tab[]={new TabDes("temperaturRegler.tempSoll[%d]", "Soll [°C]", VentilItemType.Spinner),
			new TabDes("temperaturRegler.tempIst[%d]", "Ist [°C]", VentilItemType.Label),
			new TabDes("temperaturRegler.PWMval[%d]", "PWM [%]", VentilItemType.Label)};
	
	JLabel title[];
	JLabel labelsL[];
	StandardVentilItem ventilIt[][];

	CmdManager manag;
	public TempController(CmdManager manag){
		
		int len=tab.length;
		this.manag=manag;
		this.setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.HORIZONTAL;
		c.insets=new Insets(5, 10, 5, 10);
		c.gridwidth=1;
		title = new JLabel[numTempController];
		labelsL =new JLabel[len];
		ventilIt=new StandardVentilItem[len][numTempController];
		c.weightx=0.01;
		int posInd=0;
		for (String name: regulatorNames) {
			if(name!=null){
				title[posInd] = new JLabel();
				c.gridx = posInd + 2;
				if(name != null){
					title[posInd].setText(name);
				}
				c.gridy = 0;
				this.add(title[posInd], c);
				posInd++;
			}
		}
		c.gridx = 0;
		for (int j = 0; j < len; j++) {
			c.gridy = j + 1;
			labelsL[j]=new JLabel(tab[j].label);
			this.add(labelsL[j], c);
		}
		for (int j = 0; j < len; j++) {
			int regInd=0;
			posInd=0;
			for (String name: regulatorNames) {
				if(name!=null){
					ventilIt[j][posInd] = new StandardVentilItem(manag,String.format(tab[j].name,regInd),tab[j].type,"");
					c.gridx = posInd + 2;
					c.gridy=j+1;
					this.add(ventilIt[j][posInd], c);
					posInd++;
				}
				regInd++;
			}
		}
	}
}


