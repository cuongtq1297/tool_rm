package com.viettel.roaming.tool_import.process;

import com.sun.mail.pop3.POP3Message;
import com.sun.mail.util.BASE64DecoderStream;
import com.viettel.roaming.tool_import.import_data.ImportEmailDfd;
import com.viettel.roaming.tool_import.import_data.ImportEmailHur2;
import com.viettel.roaming.tool_import.email.CheckEmail;
import com.viettel.roaming.tool_import.email.InsertEmail;
import com.viettel.roaming.tool_import.database.GetConnection;
import com.viettel.roaming.tool_import.email.GetEmailAccount;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.viettel.roaming.tool_import.email.GetMessage;
import com.viettel.roaming.tool_import.bo.EmailAccount;
import com.viettel.roaming.tool_import.bo.EmailConfig;
import com.viettel.roaming.tool_import.email.FilterEmail;
import com.viettel.roaming.tool_import.email.GetEmailConfig;

import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.Multipart;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.*;

public class Hur2EmailProcess {
    private static final Logger logger = LogManager.getLogger(Hur2EmailProcess.class);
    private static final String TYPE_NAME = "HUR2";
    private static final String FOLDER = "C:/Users/CuongTQ/Desktop/email/tool_import_email/attachment_folder/";

    public static void Hur2EmailProcess() throws Exception {
        try {
            // Lay account
            List<EmailAccount> accountList = GetEmailAccount.getAccount();
            // lay danh sach config
            List<EmailConfig> lstEmailConfig = GetEmailConfig.getEmailConfigNew(TYPE_NAME);
            for (EmailAccount account : accountList) {
                Message[] messages = GetMessage.getMessageFromInboxFolder(account.getUserName(), account.getPassword(), account.getHost(), account.getPort());
                System.out.println("co " + messages.length + " thu");
                for (int i = 0; i < messages.length; i++) {
                    boolean checkRecord = false;
                    boolean isMulti = false;
                    Message message = messages[i];
                    String messageId = ((POP3Message) message).getMessageID().replace("<", "").replace(">", "");
                    // check da xu ly
                    checkRecord = CheckEmail.checkRecord(messageId);
                    isMulti = message.getContent() instanceof Multipart;
                    if (checkRecord && isMulti) {
                        String senderMail = message.getFrom()[0].toString();
                        String subjectMail = message.getSubject();
                        String receiverMail = account.getUserName();
                        Date receivedDate = message.getSentDate();
                        Multipart multipart = (Multipart) message.getContent();
                        List<String> fileNames = new ArrayList<String>();
                        List<BodyPart> bodyParts = new ArrayList<BodyPart>();
                        for (int j = 0; j < multipart.getCount(); j++) {
                            BodyPart bodyPart = multipart.getBodyPart(j);
                            String fileName = bodyPart.getFileName();
                            // kiểm tra phần có phải là file đính kèm được gửi từ email hay không
                            if (bodyPart.getDisposition() != null && bodyPart.getDisposition().equalsIgnoreCase("attachment")) {
                                fileNames.add(fileName);
                                bodyParts.add(bodyPart);
                                // Lưu tệp
//                                File file = new File(FOLDER + bodyPart.getFileName());
//                                if (file.exists()) {
//                                    logger.info("file đã tồn tại trong thư mục");
//                                } else {
//                                    InputStream inputStream = bodyPart.getInputStream();
//                                    FileOutputStream outputStream = new FileOutputStream(file);
//                                    byte[] buffer = new byte[4096];
//                                    int bytesRead;
//                                    while ((bytesRead = inputStream.read(buffer)) != -1) {
//                                        outputStream.write(buffer, 0, bytesRead);
//                                    }
//                                    outputStream.close();
//                                    inputStream.close();
//                                }
                            }
                        }
                        String fileNameLst = "";
                        for (int a = 0; a < fileNames.size(); a++) {
                            fileNameLst += fileNames.get(a);
                            if (a < fileNames.size() - 1) {
                                fileNameLst += ";";
                            }
                        }

                        EmailConfig emailConfig = FilterEmail.checkSenderSubject(senderMail, subjectMail, lstEmailConfig);
                        if (emailConfig.getEmailConfigId() != null) {
                            boolean insertPending = InsertEmail.insertPending(senderMail, subjectMail, receiverMail, fileNameLst, emailConfig.getEmailConfigId(), TYPE_NAME, messageId, FOLDER, receivedDate, emailConfig.getPartnerCode());
                            if (insertPending) {
                                boolean checkAttachment = FilterEmail.checkAttachment(fileNames, emailConfig);
                                if (checkAttachment) {

                                    for (BodyPart bodyPart : bodyParts) {
                                        String attachmentContent = "";
                                        InputStream is = bodyPart.getInputStream();
                                        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                                        byte[] buffer = new byte[1024];
                                        int bytesRead;
                                        while ((bytesRead = is.read(buffer)) != -1) {
                                            byteArrayOutputStream.write(buffer, 0, bytesRead);
                                        }
                                        attachmentContent = new String(byteArrayOutputStream.toByteArray(), StandardCharsets.UTF_8);
                                        boolean resultImport = ImportEmailHur2.importData(attachmentContent, emailConfig.getEmailConfigId());
                                        if (resultImport) {
                                            InsertEmail.updateStatusSuccess(messageId, "1", "1",emailConfig.getTypeName(),emailConfig.getEmailConfigId(),emailConfig.getPartnerCode());
                                        } else {
                                            InsertEmail.updateStatusFail(messageId, "2","0");
                                        }
                                    }
                                } else {
                                    // update attachment khong hop le
                                    InsertEmail.updateStatusFail(messageId, "2", null);
                                }
                            }
                        } else {
                            InsertEmail.insertEmailNotValid(senderMail, subjectMail, receiverMail, fileNameLst, messageId, receivedDate, FOLDER);
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Error in process : " + e);
        }
    }


    public static void main(String[] args) throws Exception {
        Connection connection = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        String timeConfig = "";
        try {
            connection = GetConnection.connect();
            String sql = "Select time_config from email.email_database_connection where type_name = ?";
            stmt = connection.prepareStatement(sql);
            stmt.setString(1, TYPE_NAME);
            rs = stmt.executeQuery();
            if (rs.next()) {
                timeConfig = rs.getString("time_config");
            }
        } catch (Exception e) {
            logger.error(e.getMessage() + e);
        }
        String[] time = timeConfig.split(":");
        int h = Integer.parseInt(time[0]);
        int m = Integer.parseInt(time[1]);
        int s = Integer.parseInt(time[2]);
        while (true){
            Hur2EmailProcess();
            Thread.sleep(h * 3600000 + m * 60000 + s * 1000);
        }
    }
}
