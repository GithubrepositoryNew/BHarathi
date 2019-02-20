package SL_CommonErrorHandling;

import java.util.Calendar;
import java.util.Date;

import com.ibm.broker.javacompute.MbJavaComputeNode;
import com.ibm.broker.plugin.MbElement;
import com.ibm.broker.plugin.MbException;
import com.ibm.broker.plugin.MbMessage;
import com.ibm.broker.plugin.MbMessageAssembly;
import com.ibm.broker.plugin.MbOutputTerminal;
import com.ibm.broker.plugin.MbUserException;
import com.ibm.broker.plugin.MbXMLNSC;

public class CommonErrorHandling_FailureHandling extends MbJavaComputeNode {

	public void evaluate(MbMessageAssembly inAssembly) throws MbException {
		MbOutputTerminal out = getOutputTerminal("out");
		//MbOutputTerminal alt = getOutputTerminal("alternate");

		MbMessage inMessage = inAssembly.getMessage();
		MbMessageAssembly outAssembly = null;
		try {
			// create new message as a copy of the input
			MbMessage outMessage = new MbMessage(inMessage);
			outAssembly = new MbMessageAssembly(inAssembly, outMessage);
			// ----------------------------------------------------------
			// Add user code below
			
			MbMessage exceptionmsg= outAssembly.getExceptionList();
			MbElement outroot =exceptionmsg.getRootElement();
			MbElement exceptionmsg_copy= outroot.getLastChild().copy();
			outAssembly.getMessage().getRootElement().getLastChild().detach();

			MbElement outxmlnsc= outAssembly.getMessage().getRootElement().createElementAsLastChild(MbXMLNSC.PARSER_NAME);
			MbElement XMLNSCroot = outxmlnsc.createElementAsLastChild(MbElement.TYPE_NAME,"Root",null);
			XMLNSCroot.copyElementTree(exceptionmsg_copy);
			MbElement Mbinputpayload = XMLNSCroot.createElementAsLastChild(MbElement.TYPE_NAME,"InputPayload",null);
			String inputpayload ="";
			
			try
			{
				MbElement inpayload=inAssembly.getMessage().getRootElement().getLastChild(); 
				Mbinputpayload.copyElementTree(inpayload);
				//byte[] xml = inpayload.toBitstream(null, null, "XMLNSC", 0, 0, 0);
				//inputpayload = new String(xml);
			}catch(Exception e)
			{
				inputpayload = "";
			}
			
			
			
			MbMessage LocalEnv = inAssembly.getLocalEnvironment();
			MbElement Destination = LocalEnv.getRootElement().getFirstElementByPath("./Destination");
			MbElement DestinationMb =null;
			if (Destination !=null)
				DestinationMb = LocalEnv.getRootElement().getFirstElementByPath("./Destination");
			else
				DestinationMb = LocalEnv.getRootElement().createElementAsLastChild(MbElement.TYPE_NAME, "Destination", null);
			
			 MbElement File = DestinationMb.createElementAsLastChild(MbElement.TYPE_NAME, "File", null);
			
			 Date date = new Date();
			 Calendar cal = Calendar.getInstance();
			 cal.setTime(date);
			 int week = cal.get(Calendar.WEEK_OF_YEAR);
			 int year = cal.get(Calendar.YEAR);
			 File.createElementAsLastChild(MbElement.TYPE_NAME, "Name", "IIB_CommonErrorHandling_"+week+"_"+year+".txt");
			// End of user code
			// ----------------------------------------------------------
		} catch (MbException e) {
			// Re-throw to allow Broker handling of MbException
			throw e;
		} catch (RuntimeException e) {
			// Re-throw to allow Broker handling of RuntimeException
			throw e;
		} catch (Exception e) {
			// Consider replacing Exception with type(s) thrown by user code
			// Example handling ensures all exceptions are re-thrown to be handled in the flow
			throw new MbUserException(this, "evaluate()", "", "", e.toString(),
					null);
		}
		// The following should only be changed
		// if not propagating message to the 'out' terminal
		out.propagate(outAssembly);

	}

}
