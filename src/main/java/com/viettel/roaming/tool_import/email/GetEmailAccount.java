package com.viettel.roaming.tool_import.email;

import com.viettel.roaming.tool_import.database.GetConnection;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.viettel.roaming.tool_import.bo.EmailAccount;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class GetEmailAccount {
    private static final Logger logger = LogManager.getLogger(GetEmailAccount.class);
    public static List<EmailAccount> getAccount() {
        List<EmailAccount> lstAccount = new ArrayList<EmailAccount>();
        Connection connection = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            connection = GetConnection.connect();
            String sql = "select * from email.email_account where status = '1'";
            ps = connection.prepareStatement(sql);
            rs = ps.executeQuery();
            while (rs.next()){
                EmailAccount emailAccount = new EmailAccount();
                Long accountId = rs.getLong("email_account_id");
                String username = rs.getString("username");
                String password = rs.getString("password");
                String host = rs.getString("host");
                int port = rs.getInt("port");
                emailAccount.setUserName(username);
                emailAccount.setPassword(password);
                emailAccount.setHost(host);
                emailAccount.setPort(port);
                emailAccount.setAccountId(accountId);
                lstAccount.add(emailAccount);
            }
        } catch (Exception ex){
            logger.error("error get email account " + ex);
        }
        return lstAccount;
    }
}
