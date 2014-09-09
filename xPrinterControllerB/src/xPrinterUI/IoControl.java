package xPrinterUI;

import javax.swing.BoxLayout;
import javax.swing.JPanel;

public class IoControl extends JPanel {
	CmdManager manag;
	public IoControl(CmdManager manag){
		this.manag=manag;
		BoxLayout ioControlLayout=new BoxLayout(this, BoxLayout.Y_AXIS);
		setLayout(ioControlLayout);
		//"add(new StandardVentilItem(manag,"&$A$1&A9&"."&B9&$A$1&",VentilItemType."&SI(EXACT(F9;"Q");"CheckBox";"Label")&")));"
		add(new StandardVentilItem(manag,"Plasma.ON",VentilItemType.CheckBox));
		add(new StandardVentilItem(manag,"Plasma.OFF",VentilItemType.CheckBox));
		add(new StandardVentilItem(manag,"Plasma.AirPressure",VentilItemType.Label));
		add(new StandardVentilItem(manag,"Plasma.Error",VentilItemType.Label));
		add(new StandardVentilItem(manag,"Flamme.Start",VentilItemType.CheckBox));
		add(new StandardVentilItem(manag,"Flamme.Reset",VentilItemType.CheckBox));
		add(new StandardVentilItem(manag,"Flamme.ON",VentilItemType.Label));
		add(new StandardVentilItem(manag,"Flamme.AirPressure",VentilItemType.Label));
		add(new StandardVentilItem(manag,"Flamme.GasPressure",VentilItemType.Label));
		add(new StandardVentilItem(manag,"Flamme.Error",VentilItemType.Label));
		add(new StandardVentilItem(manag,"UV.LampON",VentilItemType.CheckBox));
		add(new StandardVentilItem(manag,"UV.PowerBit0",VentilItemType.CheckBox));
		add(new StandardVentilItem(manag,"UV.PowerBit1",VentilItemType.CheckBox));
		add(new StandardVentilItem(manag,"UV.PowerBit2",VentilItemType.CheckBox));
		add(new StandardVentilItem(manag,"UV.ShutterRun",VentilItemType.CheckBox));
		add(new StandardVentilItem(manag,"UV.Ready",VentilItemType.Label));
		add(new StandardVentilItem(manag,"UV.LampFault",VentilItemType.Label));
		add(new StandardVentilItem(manag,"UV.ShutterOpen",VentilItemType.Label));
		add(new StandardVentilItem(manag,"UV.ShutterClosed",VentilItemType.Label));
		add(new StandardVentilItem(manag,"Substr.Heiz",VentilItemType.CheckBox));
		add(new StandardVentilItem(manag,"Substr.VakumPumpe",VentilItemType.CheckBox));
		add(new StandardVentilItem(manag,"FlZuf.Pumpe1",VentilItemType.CheckBox));
		add(new StandardVentilItem(manag,"FlZuf.Pumpe2",VentilItemType.CheckBox));
		add(new StandardVentilItem(manag,"FlZuf.Pumpe3",VentilItemType.CheckBox));
		add(new StandardVentilItem(manag,"FlZuf.Pumpe4",VentilItemType.CheckBox));
	}
}
