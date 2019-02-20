package SL_CommonErrorHandling;

import java.util.HashMap;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import javax.xml.bind.DatatypeConverter;

import com.ibm.broker.javacompute.MbJavaComputeNode;
import com.ibm.broker.plugin.MbBLOB;
import com.ibm.broker.plugin.MbElement;
import com.ibm.broker.plugin.MbException;
import com.ibm.broker.plugin.MbMessage;
import com.ibm.broker.plugin.MbMessageAssembly;
import com.ibm.broker.plugin.MbOutputTerminal;
import com.ibm.broker.plugin.MbUserException;
import com.ibm.broker.plugin.MbXMLNSC;

public class CommonErrorHandling_JavaCompute extends MbJavaComputeNode {
 String inputPayload=null;
 MbMessageAssembly inAssembly=null;
 MbMessageAssembly outAssembly=null;
 MbMessage inMessage=null;
 MbMessage outMessage=null;
 Date date = new Date();
 String eflag = "";
 public static String msgDigestId = "";
 public static String msgDlrId = "";
 public static String errorMsg = "";
 public static String env = "";
  
 @SuppressWarnings({ "rawtypes", "unused" })
 public void evaluate(MbMessageAssembly inAssembly) throws MbException {
  MbOutputTerminal out = getOutputTerminal("out");
  MbOutputTerminal alt = getOutputTerminal("alternate");

  MbMessage inMessage = inAssembly.getMessage();
  MbMessageAssembly outAssembly = null;
  MbMessageAssembly altAssembly = null;
  try {
	  
	  // create new message as a copy of the input
	   MbMessage outMessage = new MbMessage(inMessage);
	   outAssembly = new MbMessageAssembly(inAssembly, outMessage);
	   MbMessage outAssem = new MbMessage(inMessage);
	   altAssembly = new MbMessageAssembly(inAssembly, outAssem);

	// Elink Error Handling Start
	  Boolean elinkflag = false; //flag used to check for elink error handling
	  
	  //reading http header to check for elink interfaces
	  MbElement HttpInputHeadermb = inAssembly.getMessage().getRootElement().getFirstElementByPath("HTTPInputHeader");
		MbElement HttpCommand  = null;
		if(HttpInputHeadermb!=null)
		{
			HttpCommand = HttpInputHeadermb.getFirstElementByPath("X-Original-HTTP-Command");
		}
		//RoutingBased on 
		
		if(HttpCommand!=null)
		{
			String strHttpCommand = HttpCommand.getValueAsString();
			// exception from elink then we update elinkFlag to true
			if (strHttpCommand.contains("mailbox") || strHttpCommand.contains("unilink")||strHttpCommand.contains("interlink")) {
				
				elinkflag = true;//changing flag for elink exceptions
			}
		}
		
		//*******Error Message fields
		/*MbElement EnvironMentRoot = inAssembly.getGlobalEnvironment().getRootElement();
		MbElement uuidTranStat = EnvironMentRoot.getFirstElementByPath("unichar");*/
		
		MbElement msgid  = inAssembly.getLocalEnvironment().getRootElement().getFirstElementByPath("./Destination/HTTP/RequestIdentifier");
		
		String elinkMessageID=" ", elinkCorrelationID = "" ,elinkErrorLog = "", elinkInputPayload = "" , elinkErrorMessage = "" , elinkErrorDescription = "";
		if(msgid!=null)
		{
			//retrieve the message id / transaction id from environment
			elinkMessageID = msgid.getValueAsString();
		}
		
		// we need to set current date format in databse
		SimpleDateFormat elinksdfDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	    Date elinkNow = new Date();
		String strElinkDate = elinksdfDate.format(elinkNow);
		elinkErrorLog=strElinkDate;
		
		//retrieve the payload/query parameters/mime parser message 
		MbElement blobMessage =  inAssembly.getMessage().getRootElement().getLastChild().getLastChild();
		
		if(blobMessage!=null)
		{
			elinkInputPayload =  blobMessage.getValueAsString();
		}
		//Converting hexa value to ascii
		byte[] bytesPayLoad = DatatypeConverter.parseHexBinary(elinkInputPayload);
        elinkInputPayload = new String(bytesPayLoad);
        
        //Traverse in Recoverable exception list and get the error details in string format
        MbElement elinkRecoverableException=(MbElement)inAssembly.getExceptionList().getRootElement().getLastChild();
	    String mainElinkErrorDescription = getErrorDescription(elinkRecoverableException);
	    
		
		//*******error Message Fields
		if(elinkflag)//if flag is true then we need to create database error format description
		{
			
		//constructing out put response for Exception_DB message flow to insert in error table
		altAssembly.getMessage().getRootElement().getLastChild().delete();
		MbElement parser=altAssembly.getMessage().getRootElement().createElementAsLastChild(MbXMLNSC.PARSER_NAME);
	    MbElement root=parser.createElementAsLastChild(MbElement.TYPE_NAME_VALUE,"Root",null);
	    root.createElementAsLastChild(MbElement.TYPE_NAME_VALUE,"messageID",elinkMessageID);//we have only message id for elink
	    root.createElementAsLastChild(MbElement.TYPE_NAME_VALUE,"correlationID",elinkMessageID);//we have only message id for elink
	    root.createElementAsLastChild(MbElement.TYPE_NAME_VALUE,"intergrationNode",getBroker().getName().toString());
	    root.createElementAsLastChild(MbElement.TYPE_NAME_VALUE,"integrationServer", getExecutionGroup().getName().toString());
	    root.createElementAsLastChild(MbElement.TYPE_NAME_VALUE,"integrationQueuemanager", getBroker().getQueueManagerName().toString());
	    root.createElementAsLastChild(MbElement.TYPE_NAME_VALUE,"msgflowname",getMessageFlow().getName().toString());
	    //root.createElementAsLastChild(MbElement.TYPE_NAME_VALUE,"processTimeStamp",processTimeStamp);
	    root.createElementAsLastChild(MbElement.TYPE_NAME_VALUE,"AppName",getMessageFlow().getApplicationName().toString());
	    root.createElementAsLastChild(MbElement.TYPE_NAME_VALUE,"errorLog",elinkErrorLog);
	    root.createElementAsLastChild(MbElement.TYPE_NAME_VALUE, "input", elinkInputPayload);
	    root.createElementAsLastChild(MbElement.TYPE_NAME_VALUE,"errorMessage", mainElinkErrorDescription.substring(0,80));
	    root.createElementAsLastChild(MbElement.TYPE_NAME_VALUE,"errorDescription",mainElinkErrorDescription);
	  //constructing out put response for Exception_DB message flow to insert in error table
	    
	    //
		MbElement outRoot = outMessage.getRootElement();
		outRoot.getLastChild().delete();
		
		
		//Creating notification message format
	    MbElement outParser=outRoot.createElementAsLastChild(MbBLOB.PARSER_NAME);
	    //Creating Exception Body
	    MbElement inputHttpHeader = inAssembly.getMessage().getRootElement().getFirstElementByPath("HTTPInputHeader");

	    //Converting http headers into bitstream format
		byte[] httpHeaderAsBytes = inputHttpHeader.toBitstream(null, null,null, 0, 1208, 0);
		String httpHeaderAsText = new String(httpHeaderAsBytes);
		//Exception message construction
	    String Exception = "<html><BR>" + elinkInputPayload + "<BR>"
					+ httpHeaderAsText + "<BR><PRE>"
					+ mainElinkErrorDescription + "</PRE></html>";
	        outParser.createElementAsLastChild(MbElement.TYPE_NAME_VALUE,"BLOB",Exception.getBytes());
	    out.propagate(outAssembly);
	    //Creating notification message format
		}

		///
		//Setting message id from http request Identifier
		
		
		///
		
		
		
	// Elink error Handling Ends
	
   if(!elinkflag)
   {
    String MessageNumber=" ",Description=" ",Lable=" ",Flowname="";
             boolean bText =true;
             String Text="";
     
     List Number_Of_Elements=new ArrayList();
     int i = 0; 
     
     MbElement exceptionroot = inAssembly.getExceptionList().getRootElement().getFirstElementByPath("./RecoverableException");
     
     if (exceptionroot == null)
     {
      //donothing
     }
     else
     {
     MbElement RecoverableException=(MbElement)inAssembly.getExceptionList().getRootElement().getLastChild();
     String mainErrorDescription = getErrorDescription(RecoverableException);
     /*while(RecoverableException.getValueAsString()== null)
      {
       Number_Of_Elements=(List)RecoverableException.evaluateXPath("*");
       if(Number_Of_Elements.size()==2)
       {
           @SuppressWarnings("unchecked")
        List <MbElement> local =(List<MbElement>)RecoverableException.getParent().evaluateXPath("*");
           String find="Text";
           for ( i = 0; i < local.size(); i++)
           {
               if (find.equals(local.get(i).getName()))
                    break;
           } 
           
         while(i<local.size())
         {
          if (!RecoverableException.getFirstElementByPath("./Text").getValueAsString().isEmpty())
          {
           bText = false;
              Description = Description + ":" + RecoverableException.getFirstElementByPath("./Text").getValueAsString()+"=>"+Text;
          }
          
          if(i!=(local.size()-1))
          {
           if (!local.get(i).getNextSibling().getLastChild().getValueAsString().isEmpty())
           {
            Text=local.get(i).getNextSibling().getLastChild().getValueAsString();
           }
          }
          i++;
         }
       }
       else
       {
        if (!Description.contains(RecoverableException.getFirstElementByPath("./Text").getValueAsString()))
        Description=RecoverableException.getFirstElementByPath("./Text").getValueAsString()+","+" "+ Description;
       }
      
       if(Number_Of_Elements.size()>2)
       { 
        String Name=null;   
        MessageNumber=RecoverableException.getFirstElementByPath("./Number").getValueAsString();
        Name=RecoverableException.getFirstElementByPath("./Name").getValueAsString();
        if((Name.isEmpty())!=true)
        {   
         int EndIndex=RecoverableException.getFirstElementByPath("./Name").getValueAsString().indexOf("#");
         Flowname=RecoverableException.getFirstElementByPath("./Name").getValueAsString().substring(0, EndIndex);
        }  
        Name=RecoverableException.getFirstElementByPath("./Label").getValueAsString();
        if((Name.isEmpty())!=true)
         Lable=RecoverableException.getFirstElementByPath("./Label").getValueAsString();
       }
       RecoverableException=RecoverableException.getLastChild();
      
       }
      
      if (bText)
       Description = Description + ":=>"+Text;
     */
     }
    // Get the global environment
       MbMessage globalEnv = inAssembly.getGlobalEnvironment();
       String strcorrelationID = "", messageID="";
       MbElement Variables = globalEnv.getRootElement().getFirstElementByPath("MSGID");
        
    if(Variables !=null)
     messageID=Variables.getValueAsString();
    else
     messageID=UUID.randomUUID().toString();
    
    MbElement MBmsgDigestId = globalEnv.getRootElement().getFirstElementByPath("./digestID");
    MbElement mbmsgDlrId =  globalEnv.getRootElement().getFirstElementByPath("./dealerID");
    
    MbElement ReplyProtocol = inMessage.getRootElement().getFirstChild().getFirstElementByPath("./ReplyProtocol");
    
    MbElement correlationID = null;
    if (ReplyProtocol.getValueAsString().contains("SOAP"))
     correlationID = inAssembly.getLocalEnvironment().getRootElement().getFirstElementByPath("./Destination/SOAP/Reply/ReplyIdentifier");
    else if(ReplyProtocol.getValueAsString().equals("JMS"))
     correlationID = inAssembly.getMessage().getRootElement().getFirstElementByPath("./JMSTransport/Transport_Folders/Header_Values/JMSMessageID").getFirstChild();
    else if(ReplyProtocol.getValueAsString().equals("MQ"))
     correlationID = inAssembly.getMessage().getRootElement().getFirstElementByPath("./MQMD/MsgId");
    
       if (globalEnv.getRootElement().getFirstElementByPath("./Description") == null)
        errorMsg = "";
       else 
        errorMsg = globalEnv.getRootElement().getFirstElementByPath("./Description").getValueAsString();
       
       String UniqueID ="correlationID";
      
       if(correlationID !=null)
       {
        if (correlationID.getValueAsString().startsWith("ID:"))
         strcorrelationID = correlationID.getValueAsString().substring(3);
        else
         strcorrelationID = correlationID.getValueAsString();
     UniqueID = "correlationID";
       }
       else if (MBmsgDigestId != null) 
       {
        strcorrelationID =MBmsgDigestId.getValueAsString();
        UniqueID = "DigestID";
       }
    else 
        strcorrelationID= messageID;
       
       String DealerIdString = "";
       if (mbmsgDlrId !=null)
        DealerIdString = "<tr><td><i>DealerID</i></td><td>" + mbmsgDlrId.getValueAsString() + "</td></tr>";
       
       if (globalEnv.getRootElement().getFirstElementByPath("./Environment") == null)
        env = "";
       else
        env = "<tr><td><i>Environment</i></td><td>" + globalEnv.getRootElement().getFirstElementByPath("./Environment").getValueAsString() + "</td></tr>";
    
      Calendar cal = Calendar.getInstance();
        MbElement outRoot = outMessage.getRootElement();
        MbElement outParser=outRoot.createElementAsLastChild(MbBLOB.PARSER_NAME);

        String Exception= "<html><body><p style=font-size:18px;>***Integration Bus Generated E-mail***</p>"
          + "<table width= '750' border='2' cellspacing='4' cellpadding='8'>"
          + "<th colspan='2' bgcolor='#D8D8D8' style='align:CENTER;font-family:arial;font-size:16px;'>INTEGRATION BUS NOTIFICATION</th>"
          + "<tr><td style=font-size:16px;><i>FlowName</i></td>"
          + "<td width='70%'>" + Flowname + "</td></tr>"
          + "<tr bgcolor='#F8F8F8'><td><i>MessageNumber</i></td><td>" + MessageNumber + "</td></tr>"
          + "<tr><td><i>Error_Node_Name</i></td><td>" + Lable + "</td></tr>"
          + "<tr bgcolor='#F8F8F8'><td><i>DetailedDescription</i></td><td>"+ Description + "</td></tr>"
          +   "<tr><td><i>"+UniqueID+"</i></td><td>" + strcorrelationID + "</td></tr>"
          +   "<tr><td><i>Integration Server Name</i></td><td>" + getBroker().getName().toString() + "</td></tr>"
          +    DealerIdString
          +    env
          +   "<tr><td><i>TimeStamp</i></td><td>" + cal.getTime().toString() + "</td></tr>"
          + "</table>"
          + "<br/>"
          + "</body></html>";
        outParser.createElementAsLastChild(MbElement.TYPE_NAME_VALUE,"BLOB",Exception.getBytes());
                   
        outParser.getPreviousSibling().detach(); // deleting body from outassembly
        
        //Checking header name
        MbElement headers = outParser.getPreviousSibling();
        if (headers !=null)
        {
         if(headers.getName().equals("MQRFH2")) 
         {
          outParser.getPreviousSibling().detach(); //delete mqrfh2 headers
          outParser.getPreviousSibling().detach(); // deleting headers mqmd or JMS Headers or HTTPInputHeaders  
         }
         else {
          if (!headers.getName().equals("Properties"))
          outParser.getPreviousSibling().detach(); // deleting headers mqmd or JMS Headers or HTTPInputHeaders
      }
        }
           
     MbElement pro = outAssembly.getMessage().getRootElement().getFirstChild();
     MbElement mqmd= pro.createElementAfter("MQHMD");
     mqmd.createElementAsFirstChild(MbElement.TYPE_NAME_VALUE,"Format","MQHRF2");
     mqmd.createElementAsLastChild(MbElement.TYPE_NAME_VALUE,"CodedCharSetId",819);
     mqmd.createElementAsLastChild(MbElement.TYPE_NAME_VALUE,"Encoding",273);
     
     MbElement outroot = outAssembly.getMessage().getRootElement();
     MbElement body = outroot.getLastChild();
     MbElement rfh2 = body.createElementBefore("MQHRF2");
     
     rfh2.createElementAsFirstChild(MbElement.TYPE_NAME_VALUE, "Format","MQSTR");
     rfh2.createElementAsFirstChild(MbElement.TYPE_NAME_VALUE, "Version", new Integer(2));
     rfh2.createElementAsFirstChild(MbElement.TYPE_NAME_VALUE, "NameValueCCSID",new Integer(1208));
     MbElement usr = rfh2.createElementAsLastChild(MbElement.TYPE_NAME,"usr",null);
     usr.createElementAsLastChild(MbElement.TYPE_NAME_VALUE,"Email_Subject",Lable + " " + "MessageNumber : " + MessageNumber);
           
     String mainMaintStartTime = getUserDefinedAttribute("maintStartTime").toString().trim();
     String mainMaintEndTime =  getUserDefinedAttribute("mainEndTime").toString().trim();
     String mainEmailFlag = getUserDefinedAttribute("mainEmailFlag").toString();

     if(mainEmailFlag.contains("ON")) {
 // 5-1-2018 - CPOREDA - SEND EMAIL ALERT IF CURRENT DAY/TIME IS **NOT** IN A SERVICE MAINTENANCE WINDOW
          if(!inServiceMaintenanceWindow(Flowname, mainMaintStartTime, mainMaintEndTime)) {
               out.propagate(outAssembly); }
     } 
         
    String processTimeStamp=null,errorLog=null,errorMessage=null,errorDescription=null;

    SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    Date now = new Date();
    String strDate = sdfDate.format(now);
    errorLog=strDate;
    
    if (Description.length() > 99)
     errorMessage = Description.substring(0,80);
    else
     errorMessage = Description.substring(0,Description.length());
    
    errorDescription=Description;
     
    String inputpayload ="";
   
    try
    {
     MbElement inpayload=inAssembly.getMessage().getRootElement().getLastChild(); 
     byte[] xml = inpayload.toBitstream(null, null, "XMLNSC", 0, 0, 0);
     inputpayload = new String(xml);
    }catch(Exception e)
    {
     inputpayload = "";
    }
     
      MbElement last =altAssembly.getMessage().getRootElement().getLastChild(); 
      last.detach(); // deleting body from outassembly
      
      MbElement headersCHECK = altAssembly.getMessage().getRootElement().getLastChild();
       if (headersCHECK !=null)
       {
        if(headers.getName().equals("MQRFH2"))
        {
         altAssembly.getMessage().getRootElement().getLastChild().detach(); // deleting MQRFH2 headers
         altAssembly.getMessage().getRootElement().getLastChild().detach(); // deleting headers mqmd or JMS Headers or HTTPInputHeaders
        }
        else {
         if (!headers.getName().equals("Properties"))
         altAssembly.getMessage().getRootElement().getLastChild().detach(); // deleting headers mqmd or JMS Headers or HTTPInputHeaders
     }
       }
      
       MbElement parser=altAssembly.getMessage().getRootElement().createElementAsLastChild(MbXMLNSC.PARSER_NAME);
           MbElement root=parser.createElementAsLastChild(MbElement.TYPE_NAME_VALUE,"Root",null);
           root.createElementAsLastChild(MbElement.TYPE_NAME_VALUE,"messageID",messageID);
    root.createElementAsLastChild(MbElement.TYPE_NAME_VALUE,"correlationID",strcorrelationID);
    root.createElementAsLastChild(MbElement.TYPE_NAME_VALUE,"intergrationNode",getBroker().getName().toString());
    root.createElementAsLastChild(MbElement.TYPE_NAME_VALUE,"integrationServer", getExecutionGroup().getName().toString());
    root.createElementAsLastChild(MbElement.TYPE_NAME_VALUE,"integrationQueuemanager", getBroker().getQueueManagerName().toString());
    root.createElementAsLastChild(MbElement.TYPE_NAME_VALUE,"msgflowname",getMessageFlow().getName().toString());
    //root.createElementAsLastChild(MbElement.TYPE_NAME_VALUE,"processTimeStamp",processTimeStamp);
    root.createElementAsLastChild(MbElement.TYPE_NAME_VALUE,"AppName",getMessageFlow().getApplicationName().toString());
    root.createElementAsLastChild(MbElement.TYPE_NAME_VALUE,"errorLog",errorLog);
    root.createElementAsLastChild(MbElement.TYPE_NAME_VALUE, "input", inputpayload);
    root.createElementAsLastChild(MbElement.TYPE_NAME_VALUE,"errorMessage", errorMessage);
    root.createElementAsLastChild(MbElement.TYPE_NAME_VALUE,"errorDescription",errorDescription);
   }
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
     null);  // add failure terminal and crlf to e.toString and timestamp at begining
  }

  alt.propagate(altAssembly);
 }
 
 // 5-1-2018 - CPOREDA - inServiceMaintenanceWindow - IS "rightNow" IN A SERVICE MAINTENANCE WINDOW (#1 - HASHMAP, #2 - UDP)?
 public static boolean inServiceMaintenanceWindow(String flowName, String mainMaintStartTime, String mainMaintEndTime) throws ParseException { 
      Calendar currentTime = Calendar.getInstance();
      String currentDay = String.valueOf(currentTime.get(Calendar.DAY_OF_WEEK));
      String currentHour = "0" + String.valueOf(currentTime.get(Calendar.HOUR_OF_DAY));
      String currentMin = "0" + String.valueOf(currentTime.get(Calendar.MINUTE));

      // SET VARIABLE "rightNow" TO CURRENT DAY/TIME OF FORMAT D:HH:SS WHERE
      //   D IS NUMERIC DAY OF WEEK
      //   HH IS NUMERIC HOUR OF DAY LEFT-FILLED WITH ZERO IN THE CASE OF A ONE-DIGIT HOUR
      //   SS IS NUMERIC SECONDS OF THE HOUR LEFT-FILLED WITH ZERO IN THE CASE OF A ONE-DIGIT SECOND    
      StringBuffer rightNow = new StringBuffer();
      rightNow.append(currentDay).append(':').append(currentHour.substring(Math.max(currentHour.length() - 2, 0))).append(':').append(currentMin.substring(Math.max(currentMin.length() - 2, 0)));
    
      // GET HASHMAP OF SERVICE MAINTENANCE TIMES
      CommonErrorHandling_ServiceMaintTimes hashMapTimes = new CommonErrorHandling_ServiceMaintTimes();
      HashMap<String, String> serviceMaintTimesMap = (HashMap<String, String>) hashMapTimes.getServiceMaintTimesMap();
      Iterator<String> iterator = (Iterator<String>) hashMapTimes.keyIterator();
       
      while (iterator.hasNext()) { // # 1 - LOOP THROUGH HASHMAP AND RETURN TRUE IF rightNow IS IN A SERVICE-SPECIFIC MAINTENANCE WINDOW
         String keyVal = iterator.next().toString();
         if(keyVal.contains(flowName)){
            String serviceMaintTimes = serviceMaintTimesMap.get(keyVal).toString();
            String serviceMaintWindow = convertDayOfWeek(serviceMaintTimes);
            if(inServiceWindow(rightNow.toString(), currentDay, serviceMaintWindow)){
                return true; }}
      }
            
      String serviceMaintWindow = convertDayOfWeek(mainMaintStartTime + "," + mainMaintEndTime); // # 2 - ELSE, RETURN TRUE IF rightNow IS IN THE SERVICE MAINTENANCE WINDOW IN THE SERVICE'S UDP's (USER DEFINES PROPERTIES)
      if(inServiceWindow(rightNow.toString(), currentDay, serviceMaintWindow)){ 
          return true; 
      }
             
      return false; // # 3 - ELSE, RETURN FALSE IF rightNow IS **NOT** IN ANY OF THE ABOVE SERVICE MAINTENANCE WINDOWS
 }

 // 5-1-2018 - CPOREDA - convertDayOfWeek - CONVERT ALPHABETIC DAY OF WEEK TO NUMERIC DAY OF WEEK
 public static String convertDayOfWeek(String serviceMaintTimes) throws ParseException {
      // Examples: 
      //   if serviceMaintTimes is "Saturday:22:00,Sunday:07:46" this routine returns "7:22:00,1:07:46"
      //   if serviceMaintTimes is "Monday:21:15,Tuesday:02:00" this routine returns "2:21:50,3:02:00"
      SimpleDateFormat dayStr = new SimpleDateFormat("E");
      Calendar currentTime = Calendar.getInstance();
      StringBuffer convertedServiceMaintWindow = new StringBuffer();
   
      String[] serviceMaintTime = serviceMaintTimes.split(",", 2);
      String[] startDayOfWeek = serviceMaintTime[0].split(":", 2);
      String[] endDayOfWeek = serviceMaintTime[1].split(":", 2);
    
      currentTime.setTime(dayStr.parse(startDayOfWeek[0]));
      convertedServiceMaintWindow.append(currentTime.get(Calendar.DAY_OF_WEEK)).append(':').append(startDayOfWeek[1]);
      convertedServiceMaintWindow.append(',');
      currentTime.setTime(dayStr.parse(endDayOfWeek[0]));
      convertedServiceMaintWindow.append(currentTime.get(Calendar.DAY_OF_WEEK)).append(':').append(endDayOfWeek[1]);

      return convertedServiceMaintWindow.toString();
 }

 // 5-1-2018 - CPOREDA - inServiceWindow - IS "rightNow" IN THIS SERVICE MAINTENANCE WINDOW?
 // 6-15-2018 - CPOREDA - change currentDay=="7" to currentDay.equals("7") and change currentDay=="1" to currentDay.equals("1")
 public static boolean inServiceWindow(String rightNow, String currentDay, String serviceMaintWindow) throws ParseException { 
      String[] serviceMaintTime = serviceMaintWindow.split(",", 2);  
      StringBuffer compareStart = new StringBuffer(serviceMaintTime[0]);
      StringBuffer compareEnd = new StringBuffer(serviceMaintTime[1]);
             
      if(currentDay.equals("7") && compareEnd.substring(0,1).equals("1")){  // WHEN TODAY IS SATURDAY "7" AND HASHMAP FOUND FOR SUNDAY "1" TEMPORARILY CHANGE SUNDAY DAY OF WEEK TO "8" FOR COMPARE
          compareEnd.setCharAt(0, '8');
      } else {
      if(currentDay.equals("1") && compareStart.substring(0,1).equals("7")){ // WHEN TODAY IS SUNDAY "1" AND HASHMAP FOUND FOR SATURDAY "7" TEMPORARILY CHANGE SATURDAY DAY OF WEEK TO "0" FOR COMPARE
          compareStart.setCharAt(0, '0'); }
      } 
      // DO THE COMPARE ...
      if(rightNow.compareTo(compareStart.toString())>= 0 &&
             rightNow.compareTo(compareEnd.toString())<= 0){
          return true; // RETURN TRUE IF rightNow IS IN THE SERVICE MAINTENANCE WINDOW
      } else {
          return false; } // RETURN FALSE IF rightNow IS **NOT** IN THE SERVICE MAINTENANCE WINDOW
 }
 
 
 public static String getErrorDescription(MbElement RecoverableException)
 {
	 
	 String Description = " ";
	 try{
	      boolean bText = true;
		String Text = null;
		
		while(RecoverableException.getValueAsString()== null)
	      {
	       List Number_Of_Elements = (List)RecoverableException.evaluateXPath("*");
	       if(Number_Of_Elements.size()==2)
	       {
	           @SuppressWarnings("unchecked")
	        List <MbElement> local =(List<MbElement>)RecoverableException.getParent().evaluateXPath("*");
	           String find="Text";
	           int i;
			for ( i = 0; i < local.size(); i++)
	           {
	               if (find.equals(local.get(i).getName()))
	                    break;
	           } 
	           
	         while(i<local.size())
	         {
	          if (!RecoverableException.getFirstElementByPath("./Text").getValueAsString().isEmpty())
	          {
	           bText = false;
	              Description = Description + ":" + RecoverableException.getFirstElementByPath("./Text").getValueAsString()+"=>"+Text;
	          }
	          
	          if(i!=(local.size()-1))
	          {
	           if (!local.get(i).getNextSibling().getLastChild().getValueAsString().isEmpty())
	           {
	            Text=local.get(i).getNextSibling().getLastChild().getValueAsString();
	           }
	          }
	          i++;
	         }
	       }
	       else
	       {
	        if (!Description.contains(RecoverableException.getFirstElementByPath("./Text").getValueAsString()))
	        Description=RecoverableException.getFirstElementByPath("./Text").getValueAsString()+","+" "+ Description;
	       }
	      
	       if(Number_Of_Elements.size()>2)
	       { 
	        String Name=null;   
	        String MessageNumber = RecoverableException.getFirstElementByPath("./Number").getValueAsString();
	        Name=RecoverableException.getFirstElementByPath("./Name").getValueAsString();
	        if((Name.isEmpty())!=true)
	        {   
	         int EndIndex=RecoverableException.getFirstElementByPath("./Name").getValueAsString().indexOf("#");
	         String Flowname = RecoverableException.getFirstElementByPath("./Name").getValueAsString().substring(0, EndIndex);
	        }  
	        Name=RecoverableException.getFirstElementByPath("./Label").getValueAsString();
	        String Lable;
			if((Name.isEmpty())!=true)
	         Lable=RecoverableException.getFirstElementByPath("./Label").getValueAsString();
	       }
	       RecoverableException=RecoverableException.getLastChild();
	      
	       }
	      
	      if (bText)
	       Description = Description + ":=>"+Text;
	 }catch(Exception e)
	 {
		 
	 }
	 
	 
	return Description;
	 
 }
   
}