package SL_CommonErrorHandling;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;

import com.ibm.broker.javacompute.MbJavaComputeNode;
import com.ibm.broker.plugin.MbElement;
import com.ibm.broker.plugin.MbException;
import com.ibm.broker.plugin.MbMessage;
import com.ibm.broker.plugin.MbMessageAssembly;
import com.ibm.broker.plugin.MbOutputTerminal;
import com.ibm.broker.plugin.MbUserException;
import com.ibm.broker.plugin.MbNode.JDBC_TransactionType;

public class CommonErrorHandling_DatabaseUpdate extends MbJavaComputeNode {

	public void evaluate(MbMessageAssembly inAssembly) throws MbException {
		MbOutputTerminal out = getOutputTerminal("out");
		MbOutputTerminal alt = getOutputTerminal("alternate");
		PreparedStatement preparedStatement = null;
		MbMessage inMessage = inAssembly.getMessage();
		MbMessageAssembly outAssembly = null;
		try {
			// create new message as a copy of the input
			MbMessage outMessage = new MbMessage(inMessage);
			outAssembly = new MbMessageAssembly(inAssembly, outMessage);
			// ----------------------------------------------------------
			// Add user code below
			Connection connection=null;
			String JDBCProviders = (String) getUserDefinedAttribute("jdbcProviders");
			try{
				connection = this.getJDBCType4Connection(JDBCProviders, JDBC_TransactionType.MB_TRANSACTION_AUTO);
			} catch (Exception e) {
			// TODO: handle exception
				throw new MbUserException(this, "evaluate()", "", "","DB connection Error",null);
			}
			
			String insertTableSQL = "INSERT INTO IIB_ERROR_EXCEPTION VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
			preparedStatement = connection.prepareStatement(insertTableSQL);
			MbElement parser = inAssembly.getMessage().getRootElement().getLastChild();
			
			preparedStatement.setString(1,parser.getFirstElementByPath("./Root/messageID").getValueAsString());
			preparedStatement.setString(2,parser.getFirstElementByPath("./Root/correlationID").getValueAsString());
			preparedStatement.setString(3,parser.getFirstElementByPath("./Root/intergrationNode").getValueAsString());
			preparedStatement.setString(4,parser.getFirstElementByPath("./Root/integrationServer").getValueAsString());
			preparedStatement.setString(5,parser.getFirstElementByPath("./Root/integrationQueuemanager").getValueAsString());
			preparedStatement.setString(6,parser.getFirstElementByPath("./Root/msgflowname").getValueAsString());
			preparedStatement.setTimestamp(7, new Timestamp(System.currentTimeMillis()));
			preparedStatement.setString(8,parser.getFirstElementByPath("./Root/errorLog").getValueAsString());
			preparedStatement.setString(9,parser.getFirstElementByPath("./Root/errorMessage").getValueAsString());
			//preparedStatement.setString(9,"errorMessage");
			preparedStatement.setString(10,parser.getFirstElementByPath("./Root/AppName").getValueAsString());
			preparedStatement.setString(11, parser.getFirstElementByPath("./Root/input").getValueAsString());
			preparedStatement.setString(12,parser.getFirstElementByPath("./Root/errorDescription").getValueAsString());
			preparedStatement.setString(13,"");
			preparedStatement.setString(14,"");
			preparedStatement.executeUpdate();
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
		}finally{
			try {
				preparedStatement.close();
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		// The following should only be changed
		// if not propagating message to the 'out' terminal
		out.propagate(outAssembly);

	}

}
