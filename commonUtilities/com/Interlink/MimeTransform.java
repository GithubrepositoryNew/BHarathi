package com.Interlink;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.xml.bind.DatatypeConverter;

import com.ibm.broker.javacompute.MbJavaComputeNode;
import com.ibm.broker.plugin.MbDFDL;
import com.ibm.broker.plugin.MbElement;
import com.ibm.broker.plugin.MbException;
import com.ibm.broker.plugin.MbMessage;
import com.ibm.broker.plugin.MbMessageAssembly;
import com.ibm.broker.plugin.MbOutputTerminal;
import com.ibm.broker.plugin.MbUserException;

public class MimeTransform extends MbJavaComputeNode {

	public void evaluate(MbMessageAssembly inAssembly) throws MbException {
		MbOutputTerminal out = getOutputTerminal("out");
		//MbOutputTerminal alt = getOutputTerminal("alternate");
		String submit1_str = "";
		MbMessage inMessage = inAssembly.getMessage();
		MbMessageAssembly outAssembly = null;
		try {
			// create new message as a copy of the input
			MbMessage outMessage = new MbMessage(inMessage);
			outAssembly = new MbMessageAssembly(inAssembly, outMessage);
			// ----------------------------------------------------------
			// Add user code below
            
            MbElement MIMESPARTS = outMessage.getRootElement().getLastChild().getFirstElementByPath("./Parts");
            //MbElement MIMEPARTS = MIMESPARTS.getFirstChild();
            
            @SuppressWarnings("deprecation")
			MbElement mbPart[] = MIMESPARTS.getAllElementsByPath("./Part");
           // Map<String, String> mapDetails = new HashMap<String, String>();
            int i=0;
         
            @SuppressWarnings("unused")
			String filename ="";
            HashMap<String, String> valuesMap = new HashMap<String, String>();
			 while(i<mbPart.length)
			 {
				 MbElement mbContent = mbPart[i].getFirstElementByPath("Content-Disposition"); //search for filename for attachments
				 MbElement mbValue = mbPart[i].getFirstElementByPath("./Data/BLOB/BLOB");
				 String keyValue = "";
				 if (mbValue!=null){
					 keyValue = new String(DatatypeConverter.parseHexBinary(mbValue.getValueAsString()));
					}
				 else{
					 keyValue = "";
				 }
				
				 //form-data; name="LocalFile"; filename="unilink_1k_wbmason.dat"
				 // String ContentValue = new String(DatatypeConverter.parseHexBinary(mbContent.getValueAsString()));
				 String mimename = mbContent.getValueAsString();
				
				 String text = mimename;
				 String match = "name=";
				 String fnmatch ="filename="; //LocalFile
				 int index = text.indexOf(match);
				 int matchLength = match.length();
				 index = index + matchLength;
				 String mimenameindex = text.substring(index);
				 int idex = mimenameindex.indexOf("\"",1);
				 String key = mimenameindex.substring(1, idex); // fetch keyvalue
				 valuesMap.put(key,keyValue);
				 //try
				 
				 MbElement Content_Transfer_Encoding = mbPart[i].getFirstElementByPath("Content-Transfer-Encoding");
				 i++;
				 if( Content_Transfer_Encoding!=null )
				 {
					 if(Content_Transfer_Encoding.getValueAsString().equals("binary"))
				 {
				 int  mimefilenameindexint = text.indexOf(fnmatch);
				 if(mimefilenameindexint>1)
				 {
					int matchfilenameLength = fnmatch.length();
					int lenght = mimefilenameindexint + matchfilenameLength;
						
					String mimefilenameindex = text.substring(lenght+1);
					filename =mimefilenameindex.replace("\"", "");;
				 }
				 }
				 } 
				 
			 }
			 
			// Getting a Set of Key-value pairs
			    @SuppressWarnings("rawtypes")
				Set entrySet = valuesMap.entrySet();
			 
			    // Obtaining an iterator for the entry set
			    @SuppressWarnings("rawtypes")
				Iterator it = entrySet.iterator();
			 
			    // Iterate through HashMap entries(Key-Value pairs)
			   // System.out.println("HashMap Key-Value Pairs : ");
			    String attachmentvalue = "";
			    String txtServer = "",RequestTransmission="",txtUID= "",txtPWD= "",cmdSubmit="",srcFolder= "",srcFile= "",txtType= "",ContentType= "",RequestType= "",txtConfirmationNumber="";
			   
			    while(it.hasNext()){
			       @SuppressWarnings("rawtypes")
				Map.Entry me = (Map.Entry)it.next();
			       
			       String key = me.getKey().toString();
			       
			       switch(key)
			       {  
			       case "submit1": 
			    	   submit1_str = me.getValue().toString();
			    	   break; 
			       case "cmdSubmit":
			    	   cmdSubmit = me.getValue().toString();
			    	   break;
			       case "LocalFile": 
			    	   attachmentvalue = me.getValue().toString();
			    	   break;  
			       case "txtServer":
			    	   txtServer = me.getValue().toString();
			    	   break;  
			    	   
			       case "txtUID": 
			    	   txtUID = me.getValue().toString();
			    	   break;  
			       case "txtPWD":
			    	   txtPWD = me.getValue().toString();
			    	   break; 
			    
			       case "srcFolder":
			    	   srcFolder = me.getValue().toString();
			    	   break;  
			    	   
			       case "srcFile": 
			    	   srcFile = me.getValue().toString();
			    	   break;  
			       case "txtType":
			    	   txtType = me.getValue().toString();
			    	   break; 
			    
			       case "ContentType":
			    	   ContentType = me.getValue().toString();
			    	   break; 
			    	   
			       case "RequestType":
			    	   RequestType = me.getValue().toString();
			    	   break; 
			       case "txtConfirmationNumber":
			    	   txtConfirmationNumber = me.getValue().toString();
			    	   break; 
			       case "RequestTransmission":
			    	   RequestTransmission = me.getValue().toString();
			    	   break;
			       default:
			    	   @SuppressWarnings("unused")
					String test = "";
			       }
			     
			   }
			    
			    
			    if (attachmentvalue==null || attachmentvalue.equals("")){
		    		attachmentvalue = RequestTransmission;
		    	}
			    
			    outMessage.getRootElement().getLastChild().delete();
			    MbElement dfdlpaser = outMessage.getRootElement().createElementAsLastChild(MbDFDL.PARSER_NAME);
			    MbElement ELINK =dfdlpaser.createElementAsLastChild(MbElement.TYPE_NAME,"ELINK",null);
			    MbElement ELINK_SEQ = ELINK.createElementAsLastChild(MbElement.TYPE_NAME,"ELINK_SEQ",null);
			    ELINK_SEQ.createElementAsLastChild(MbElement.TYPE_NAME_VALUE,"txtServer",txtServer);
			    ELINK_SEQ.createElementAsLastChild(MbElement.TYPE_NAME_VALUE,"txtUID",txtUID);
			    ELINK_SEQ.createElementAsLastChild(MbElement.TYPE_NAME_VALUE,"txtPWD",txtPWD);
			    ELINK_SEQ.createElementAsLastChild(MbElement.TYPE_NAME_VALUE,"srcFolder",srcFolder);
			    ELINK_SEQ.createElementAsLastChild(MbElement.TYPE_NAME_VALUE,"txtConfirmationNumber",txtConfirmationNumber);
			    ELINK_SEQ.createElementAsLastChild(MbElement.TYPE_NAME_VALUE,"srcFile",srcFile);
			    ELINK_SEQ.createElementAsLastChild(MbElement.TYPE_NAME_VALUE,"txtType",txtType);
			    ELINK_SEQ.createElementAsLastChild(MbElement.TYPE_NAME_VALUE,"ContentType",ContentType);
			    ELINK_SEQ.createElementAsLastChild(MbElement.TYPE_NAME_VALUE,"RequestType",RequestType);//changed this code for PUT
			    ELINK_SEQ.createElementAsLastChild(MbElement.TYPE_NAME_VALUE,"RequestTransmission",attachmentvalue);
			    ELINK_SEQ.createElementAsLastChild(MbElement.TYPE_NAME_VALUE,"cmdSubmit",cmdSubmit);
			    ELINK_SEQ.createElementAsLastChild(MbElement.TYPE_NAME_VALUE,"submit1",submit1_str);
				
			    outAssembly.getMessage().getRootElement().getFirstElementByPath("./HTTPInputHeader").delete();
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
		//if(!submit1_str.contains("Get"))
		//{
		out.propagate(outAssembly);
		//}else{
			//alt.propagate(outAssembly);
		//}

	}
}
