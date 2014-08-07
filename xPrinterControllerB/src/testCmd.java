package adsInterface;

public class testCmd extends comandInterpreter {

	public String setVariable(String varName, operation oper, String value){
		return varName+"="+value;
	}
	
	@Override
	public String getVariable(String varName) throws Exception {
		return varName+"=342";
	}
}
