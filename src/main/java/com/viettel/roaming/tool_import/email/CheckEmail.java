package com.viettel.roaming.tool_import.email;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.viettel.roaming.tool_import.database.GetConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class CheckEmail {
    private static final Logger logger = LogManager.getLogger(CheckEmail.class);

    public static boolean checkRecord(String messageId) throws Exception{
        boolean result = false;
        Connection connection = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            connection = GetConnection.connect();
            connection.setAutoCommit(false);
            String checkSql = "select * from email.email_process_results " +
                    "where message_id = ? and status = '1'";
            ps = connection.prepareStatement(checkSql);
            ps.setString(1,messageId);
            rs = ps.executeQuery();
            if(!rs.next()){
                result = true;
            }
        } catch (Exception e){
            logger.error("check email fail" + e);
        } finally {
            connection.close();
            ps.close();
            rs.close();
        }
        return result;
    }
}
