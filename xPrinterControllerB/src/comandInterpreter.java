package adsInterface;

import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class comandInterpreter {
	static Pattern cmdSetPattern = Pattern
			.compile("\\A\\s*([\\w]+)(=|\\+=|-=)(.+)\\z");
	static Pattern cmdGetPattern = Pattern.compile("\\A\\s*([\\w]+)\\?\\z");
	static Pattern cmdCallPattern = Pattern
			.compile("\\A\\s*([\\w]+)\\((.*)\\)\\z");
	static Pattern cmdArgPattern = Pattern.compile("(.+)(,)?[a&b]?");//For fixing Matcher.hitEnd does not always work with slices

	enum operation {
		SET, INCREMENT, DECREMENT
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

	public String setVariable(String varName, operation oper, String value)throws Exception {
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
		return null;
	}

	public String getVariable(String varName) throws Exception{
		//System.out.println("Get " + varName);
		return null;
	}

	public String sendCmd(String cmd) throws Exception {
		Matcher mSet = cmdSetPattern.matcher(cmd);
		if (mSet.matches()) {
			operation oper = operation.SET;
			switch (mSet.group(2)) {
			case "+=":
				oper = operation.INCREMENT;
				break;
			case "-=":
				oper = operation.INCREMENT;
				break;
			}
			return setVariable(mSet.group(1), oper, mSet.group(3));
		}
		Matcher mGet = cmdGetPattern.matcher(cmd);
		if (mGet.matches()) {
			return getVariable(mGet.group(1));
		}
		Matcher mCall = cmdCallPattern.matcher(cmd);
		if (mCall.matches()) {
			Matcher mArg = cmdArgPattern.matcher(mCall.group(2));
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
			return callFunc(mCall.group(1), args );
		}
		return null;
	}

	public static void main(String args[]) {
		comandInterpreter interp = new comandInterpreter();
		try {
			interp.sendCmd("myVar?");
			interp.sendCmd("myFunc(54,64 asdf)");
			interp.sendCmd("myVar=43.34");
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
