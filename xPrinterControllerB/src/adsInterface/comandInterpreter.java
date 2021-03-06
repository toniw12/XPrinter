package adsInterface;

import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/*
 * the comandInterpreter class is intended to extend a class that can interpret commands
 * the methods callFunc, setVariable, and getVariable are intended to be implemented on the child class
 * if they are not implemented they always return null
 */

public class comandInterpreter {
	static Pattern cmdSetPattern = Pattern
			.compile("\\A\\s*([\\w|\\.]+(\\[[0-9]+\\])?)(=|\\+=|-=)(.+)\\z");
	static Pattern cmdGetPattern = Pattern.compile("\\A\\s*([\\w|\\.]+(\\[[0-9]+\\])?)\\?\\z");
	static Pattern cmdCallPattern = Pattern
			.compile("\\A\\s*([\\w|\\.]+(\\[[0-9]+\\])?)\\((.*)\\)\\z"); 
	static Pattern cmdArgPattern = Pattern.compile("(.+)(,)?[a&b]?");//For fixing Matcher.hitEnd see:(https://bugs.openjdk.java.net/browse/JDK-5013885)

	enum operation {
		SET, INCREMENT, DECREMENT
	}

	public String callFunc(String func, String[] args,int cmdId)throws Exception {
		
		return callFunc(func,args);
	}
	
	public String callFunc(String func, String[] args)throws Exception {
		/*
		System.out.print("Call " + func + "(");
		for (String arg : args) {
			System.out.print(arg + ",");
		}
		System.out.println(")");
		*/
		return null;
	}

	public String setVariable(String varName, operation oper, String value,int cmdId)throws Exception {
		/*
		System.out.print("Set " + varName);
		switch (oper) {
		case SET:
			System.out.print("=");
			break;
		case INCREMENT:
			System.out.print("+=");
			break;
		case DECREMENT:
			System.out.print("-=");
			break;
		}
		System.out.println(value);
		*/
		return setVariable(varName,oper,value);
	}
	
	public String setVariable(String varName, operation oper, String value)throws Exception {
		return null;
	}

	public String getVariable(String varName,int cmdId) throws Exception{
		/*
		System.out.println("Get " + varName);
		*/
		return getVariable(varName);
	}
	
	public String getVariable(String varName) throws Exception{
		return null;
	}
	
	public String sendCmd(String cmd) throws Exception {
		return sendCmd(cmd,-1);
	}

	public String sendCmd(String cmd,int cmdId) throws Exception {
		Matcher mSet = cmdSetPattern.matcher(cmd);
		if (mSet.matches()) {
			operation oper = operation.SET;
			switch (mSet.group(3)) {
			case "+=":
				oper = operation.INCREMENT;
				break;
			case "-=":
				oper = operation.INCREMENT;
				break;
			}
			return setVariable(mSet.group(1), oper, mSet.group(4),cmdId);
		}
		Matcher mGet = cmdGetPattern.matcher(cmd);
		if (mGet.matches()) {
			return getVariable(mGet.group(1),cmdId);
		}
		Matcher mCall = cmdCallPattern.matcher(cmd);
		if (mCall.matches()) {
			Matcher mArg = cmdArgPattern.matcher(mCall.group(3));
			Vector<String> vString = new Vector<String>();
			while (mArg.find()) {
				vString.add(mArg.group(1));
				boolean grp2=mArg.group(2)!=null;
				boolean hitEnd=mArg.hitEnd();
				if(!(grp2 ^ hitEnd)){
					throw new Exception("Cannot decode arguments:'"
							+ mCall.group(2) + "'");
				}
			}
			String[] args=new String[vString.size()];
			vString.toArray(args);
			return callFunc(mCall.group(1), args,cmdId);
		}
		return null;
	}
	
	// the void main is only used for tests
	public static void main(String args[]) {
		comandInterpreter interp = new comandInterpreter();
		try {
			interp.sendCmd("my.Var[123]?",-1);
			interp.sendCmd("myFunc[0](54,64 asdf)",-1);
			interp.sendCmd("myVar[2]=43.34",-1);
			interp.sendCmd("exit()");
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
