package com.viettel.roaming.tool_import.email;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.viettel.roaming.tool_import.database.GetConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;

public class InsertEmail {
    private static final Logger logger = LogManager.getLogger(InsertEmail.class);

    public static boolean insertPending(String senderMail, String subjectMail, String receiverMail, String attachment, Long emailConfigId, String typeName, String messageId, String folder, Date receivedDate, String partnerCode) throws Exception {
        boolean result = false;
        boolean existFailRecord = false;
        boolean exist = true;
        Connection connection = null;
        PreparedStatement ps = null;
        try {
            exist = checkRecord(messageId, "0");
            existFailRecord = checkRecord(messageId, "2");
            connection = GetConnection.connect();
            connection.setAutoCommit(false);
            if (!exist && !existFailRecord) {
                String sql = "insert into email.email_process_results (email_config_id, sender_mail, receiver_mail, subject_mail, attachment, type_name, message_id ,status ,process_time, attachment_folder,received_time,partner_code) " +
                        "values(?,?,?,?,?,?,?,'0',NOW(),?,?,?)";
                ps = connection.prepareStatement(sql);
                ps.setLong(1, emailConfigId);
                ps.setString(2, senderMail);
                ps.setString(3, receiverMail);
                ps.setString(4, subjectMail);
                ps.setString(5, attachment);
                ps.setString(6, typeName);
                ps.setString(7, messageId);
                ps.setString(8, folder);
                java.sql.Date sqlDate = new java.sql.Date(receivedDate.getTime());
                ps.setDate(9, sqlDate);
                ps.setString(10, partnerCode);
                if (ps.executeUpdate() == 1) {
                    result = true;
                }
                connection.commit();
            } else if (existFailRecord) {
                String updateSql = "update email.email_process_results set status = '0' where message_id = ?";
                ps = connection.prepareStatement(updateSql);
                ps.setString(1, messageId);
                if (ps.executeUpdate() == 1) {
                    result = true;
                }
                connection.commit();
            }
        } catch (Exception e) {
            logger.error("insert pending fail +\n" + e);
        }
        return result;
    }

    public static void insertEmailNotValid(String senderMail, String subjectMail, String receiverMail, String attachment, String messageId, Date receivedDate, String folder) throws Exception {
        boolean exist = true;
        Connection connection = null;
        PreparedStatement ps = null;
        try {
            exist = checkRecord(messageId, "2");
            if (!exist) {
                connection = GetConnection.connect();
                connection.setAutoCommit(false);
                String sql = "insert into email.email_process_results ( sender_mail, receiver_mail, subject_mail, attachment, message_id ,status ,process_time, attachment_folder,received_time,note) " +
                        "values(?, ?, ?, ?, ?, ?,NOW() , ?, ?, ?)";
                ps = connection.prepareStatement(sql);
                ps.setString(1, senderMail);
                ps.setString(2, receiverMail);
                ps.setString(3, subjectMail);
                ps.setString(4, attachment);
                ps.setString(5, messageId);
                ps.setString(6, "2");
                ps.setString(7, folder);
                java.sql.Date sqlDate = new java.sql.Date(receivedDate.getTime());
                ps.setDate(8, sqlDate);
                ps.setString(9, "Email không thỏa mãn cấu hình khai báo");
                ps.executeUpdate();
                connection.commit();
            }
        } catch (Exception e) {
            logger.error(e);
        }
    }


    public static boolean checkRecord(String messageId, String status) throws SQLException {
        boolean exist = false;
        Connection connection = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            connection = GetConnection.connect();
            String checkPendingRecord = "select 1 from email.email_process_results where message_id = ? and status = ?";
            ps = connection.prepareStatement(checkPendingRecord);
            ps.setString(1, messageId);
            ps.setString(2, status);
            rs = ps.executeQuery();
            if (rs.next()) {
                exist = true;
            }
        } catch (Exception e) {
            logger.error("insert pending email information fail" + e);
        } finally {
            connection.close();
            ps.close();
            rs.close();
        }
        return exist;
    }

    public static void updateStatusSuccess(String messageId, String status, String attachStatus, String typeName, Long emailConfigId, String partnerCode) throws Exception {
        Connection connection = null;
        PreparedStatement ps = null;
        try {
            connection = GetConnection.connect();
            connection.setAutoCommit(false);
            String importSql = "UPDATE email.email_process_results " +
                    "SET status = ?, import_time = sysdate(), attachment_validity = ?, note = null,type_name = ?,email_config_id = ?, partner_code = ? " +
                    "WHERE message_id = ?  ";
            ps = connection.prepareStatement(importSql);
            ps.setString(1, status);
            ps.setString(2, attachStatus);
            ps.setString(3, typeName);
            ps.setLong(4, emailConfigId);
            ps.setString(5, partnerCode);
            ps.setString(6, messageId);
            ps.executeUpdate();
            connection.commit();
        } catch (Exception e) {
            logger.error("update fail" + e);
        } finally {
            connection.close();
            ps.close();
        }
    }

    public static void updateStatusFail(String messageId, String status, String attachStatus) throws Exception {
        Connection connection = null;
        PreparedStatement ps = null;
        try {
            connection = GetConnection.connect();
            connection.setAutoCommit(false);
            String importSql = "UPDATE email.email_process_results SET status = ?, attachment_validity = ? WHERE message_id = ?  ";
            ps = connection.prepareStatement(importSql);
            ps.setString(1, status);
            ps.setString(2, attachStatus);
            ps.setString(3, messageId);
            ps.executeUpdate();
            connection.commit();
        } catch (Exception e) {
            logger.error("update fail" + e);
        } finally {
            connection.close();
            ps.close();
        }
    }
}
