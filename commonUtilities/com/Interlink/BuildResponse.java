package com.Interlink;


import java.sql.Connection;

import com.database.DBCommonUtilities;
import com.ibm.broker.javacompute.MbJavaComputeNode;
import com.ibm.broker.plugin.MbElement;
import com.ibm.broker.plugin.MbException;
import com.ibm.broker.plugin.MbMessage;
import com.ibm.broker.plugin.MbMessageAssembly;
import com.ibm.broker.plugin.MbOutputTerminal;
import com.ibm.broker.plugin.MbUserException;
import com.ibm.broker.plugin.MbXMLNSC;

public class BuildResponse extends MbJavaComputeNode {

	public void evaluate(MbMessageAssembly inAssembly) throws MbException {
		MbOutputTerminal out = getOutputTerminal("out");
		MbOutputTerminal alt = getOutputTerminal("alternate");
		MbMessage inMessage = inAssembly.getMessage();
		MbMessageAssembly outAssembly = null;
		MbMessageAssembly altAssembly = null;
		String valueconfirm = "";
		Boolean DBTRANSSTAT = true;
		Boolean DBTRANSMSG = true;
		 java.util.Date utilDate = new java.util.Date();
			java.sql.Timestamp sq = new java.sql.Timestamp(utilDate.getTime());
		try {
			// create new message as a copy of the input
			MbMessage outMessage = new MbMessage(inMessage);
			outAssembly = new MbMessageAssembly(inAssembly, outMessage);
			MbMessage altMessage = new MbMessage(inMessage);
			altAssembly = new MbMessageAssembly(inAssembly, altMessage);
			// ----------------------------------------------------------
			// Add user code below
			MbElement msgid =inAssembly.getGlobalEnvironment().getRootElement().getFirstElementByPath("./msgid");
			String TRANS_ID = msgid.getValueAsString();
		
			String JDBCProviders = (String) getUserDefinedAttribute("jdbcProviders");
			Connection conn = null;
			
			try{
			 conn = getJDBCType4Connection(JDBCProviders, JDBC_TransactionType.MB_TRANSACTION_AUTO);
			 DBCommonUtilities.InsertTRANSTAT(conn, TRANS_ID, "70", getBroker().getName().toString(), "QUE_MGR_NM"," QUE_NM"," QUE_MSG_ID",sq.toString());
			}catch(Exception EX){
				inAssembly.getGlobalEnvironment().getRootElement().createElementAsLastChild(MbElement.TYPE_NAME_VALUE, "TRANSDBcheck1", "false");
				DBTRANSSTAT =false;
			}
			
			String MSG_DATA = "";
			
			/*
			// If outbound payload is reporting an error, write “40” to STATUS table meaning “unrecoverable error from back end”

			IF INTERLINK AND position 1 for 1 of confirmation = “E”

			Write to TRANSACT_STAT:

			TRANSACT_ID = IIB unique id + "0000000"

			CR_DT_TM = system timestamp

			TRANS_STAT_CDE = 40 (unrecoverable back end error)*/
			
			MbElement blobdata =  inMessage.getRootElement().getLastChild();
			
			if (blobdata!=null)
			{
				byte[] xml = blobdata.toBitstream(null, null, "", 0, 0, 0);
				MSG_DATA = new String(xml);
				
			}
		String MSG_SZ_QTY = Integer.toString(MSG_DATA.length());
			valueconfirm = MSG_DATA.substring(0,1);
			
			if (valueconfirm.equals("E"))
			{
				try{
				DBCommonUtilities.InsertTRANSTAT(conn, TRANS_ID, "40", getBroker().getName().toString(), "QUE_MGR_NM", "QUE_NM"," QUE_MSG_ID",sq.toString());
				}catch(Exception ex){
					DBTRANSSTAT =false;
				}
			}
			
			try{
			DBCommonUtilities.InsertTRANMSG(conn, TRANS_ID, "O", "MQI", MSG_SZ_QTY, MSG_DATA,sq.toString());
			//updateTRAN_msg(conn,"70",msgid_str,"O",strpayload);
			}catch(Exception E){
				DBTRANSMSG = false;
			}
			
			
			/*
			String confirmationhr = strpayload.substring(61,63);
			String TRANSACT_TYPE = strpayload.substring(81,83);
			String TRANS_NUM = strpayload.substring(59,64);
			String CUST_PO = strpayload.substring(95,116);
			String TRANS_NUM1 = strpayload.substring(9,14);
			String CUST_PO1 = strpayload.substring(89,110);*/
			
			/*IF (INTERLINK && position 1 for 1 of confirmation NOT EQUAL “E”) || (InterLink && position 61 for 2 of confirmation EQUAL “HR”)

Write to TRANSACT_CONN:

                             IF INTERLINK:

TRANSACT_ID = IIB unique id + "0000000"

CR_DT_TM = system timestamp

CONN_ID = Position 5 for a length of 4

TRANSACT_TYPE:

              IF position 81 for length of 2 = “R9” or “RT”

                             TRANSACT_TYPE = “RT”

              ELSE

              IF position 81 for length of 2 = “SC” or “MF”

                             TRANSACT_TYPE = “SC”

              ELSE

                             TRANSACT_TYPE = “OS”

              END-IF

              END-IF

TRANS_NUM = Position 59 for a length of 5

CUST_PO:

IF position 81 for length of 2 = “OS”

CUST_PO = Position 95 for a length of 22

ELSE

              CUST_PO = NULL

END-IF

                             ELSE

                             IF InterLink:

TRANSACT_ID = IIB unique id + "0000000"

CR_DT_TM = system timestamp

CONN_ID = NULL

TRANSACT_TYPE = NULL             

TRANS_NUM = Position 9 for a length of 5

CUST_PO = Position 89 for a length of 22

END-IF

                             END-IF

END-IF         
*/
			/*if (!valueconfirm.equals("E") && confirmationhr.equals("HR"))
			{
				
			}*/
			
			
			
			if(!DBTRANSMSG || !DBTRANSSTAT){
				inAssembly.getGlobalEnvironment().getRootElement().createElementAsLastChild(MbElement.TYPE_NAME_VALUE, "TRANSDBcheck1", "false");
				  altAssembly.getMessage().getRootElement().getLastChild().delete();
					MbElement parser=altAssembly.getMessage().getRootElement().createElementAsLastChild(MbXMLNSC.PARSER_NAME);
				    //MbElement root=parser.createElementAsLastChild(MbElement.TYPE_NAME_VALUE,"Root",null);
					// java.util.Date utilDate = new java.util.Date();
						//java.sql.Timestamp sq = new java.sql.Timestamp(utilDate.getTime());
				  //DBRootTag = inAssembly.getGlobalEnvironment().getRootElement().createElementAsLastChild(MbXMLNSC.PARSER_NAME);
				   MbElement  BuildXML = parser.createElementAsLastChild(MbElement.TYPE_NAME,"ErrorRootTag",null);
				    BuildXML.createElementAsLastChild(MbElement.TYPE_NAME_VALUE ,"TRANS-ID",TRANS_ID);
				    
				    if(!DBTRANSMSG)
				    {
				    BuildXML.createElementAsLastChild(MbElement.TYPE_NAME_VALUE ,"MSG_TY_CDE","O");
				    BuildXML.createElementAsLastChild(MbElement.TYPE_NAME_VALUE ,"SVC_CDE","MQU");
				    BuildXML.createElementAsLastChild(MbElement.TYPE_NAME_VALUE ,"MSG_SZ_QTY",MSG_SZ_QTY);
				    BuildXML.createElementAsLastChild(MbElement.TYPE_NAME_VALUE ,"MSG_DATA",MSG_DATA);
				    BuildXML.createElementAsLastChild(MbElement.TYPE_NAME_VALUE ,"MSG_DT_TM",sq.toString());
				    BuildXML.createElementAsLastChild(MbElement.TYPE_NAME_VALUE ,"tranMSG",false);
				    }
				
				    BuildXML.createElementAsLastChild(MbElement.TYPE_NAME_VALUE ,"TRANS_STAT_CDE","70");
				    BuildXML.createElementAsLastChild(MbElement.TYPE_NAME_VALUE ,"APPL_SRV_NDE_ID",getBroker().getName().toString());
				    BuildXML.createElementAsLastChild(MbElement.TYPE_NAME_VALUE ,"QUE_MGR_NM","QUE_MGR_NM");
				    BuildXML.createElementAsLastChild(MbElement.TYPE_NAME_VALUE ,"QUE_NM","QUE_NM");
				    BuildXML.createElementAsLastChild(MbElement.TYPE_NAME_VALUE ,"QUE_MSG_ID","QUE_MSG_ID");
				   
					BuildXML.createElementAsLastChild(MbElement.TYPE_NAME_VALUE ,"CR_DT_TM",sq.toString());
					BuildXML.createElementAsLastChild(MbElement.TYPE_NAME_VALUE ,"LAST_UPD_DT_TM",sq.toString());
				   // BuildXML.createElementAsLastChild(MbElement.TYPE_NAME_VALUE ,"tranMSG",false);
				    //BuildXML.createElementAsLastChild(MbElement.TYPE_NAME_VALUE ,"tranHDR",false);
				    BuildXML.createElementAsLastChild(MbElement.TYPE_NAME_VALUE, "tranSTAT", false);
				    if(outAssembly.getMessage().getRootElement().getFirstElementByPath("./HTTPInputHeader")!=null)
				    outAssembly.getMessage().getRootElement().getFirstElementByPath("./HTTPInputHeader").delete();
				    alt.propagate(altAssembly);
				    inAssembly.getGlobalEnvironment().getRootElement().getFirstElementByPath("/TRANSDBcheck1").setValue("true");
			}
			
			
			
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
		
		
		outAssembly.getMessage().getRootElement().getFirstElementByPath("MQMD").delete();
		MbElement pro = outAssembly.getMessage().getRootElement().getFirstChild();

		 MbElement outHttpHeader= pro.createElementAfter("HTTPReplyHeader");
		 
		 outHttpHeader.createElementAsLastChild(MbElement.TYPE_NAME_VALUE, "Content-Type", "Text/HTML");
			out.propagate(outAssembly);

	}
	
	
}
