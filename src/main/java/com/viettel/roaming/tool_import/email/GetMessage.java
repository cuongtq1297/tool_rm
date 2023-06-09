package com.viettel.roaming.tool_import.email;

import javax.mail.*;
import java.security.Security;
import java.util.Properties;

public class GetMessage {
    public static Message[] getMessageFromInboxFolder(String username, String password, String host, int port) throws Exception {
        Message[] messages = null;
        final String SSL_FACTORY = "javax.net.ssl.SSLSocketFactory";
        final String PROTOCOL = "pop3";
        try {
            Security.addProvider(new com.sun.net.ssl.internal.ssl.Provider());
            Properties properties = new Properties();
            properties.setProperty("mail.pop3.socketFactory.class", SSL_FACTORY);
            properties.setProperty("mail.pop3.socketFactory.fallback", "false");
            properties.put("mail.pop3.port", port);
            properties.put("mail.pop3.socketFactory.port", port);
            properties.put("mail.pop3.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
            properties.setProperty("mail.mime.address.strict", "false");
            URLName urln = new URLName(PROTOCOL, host, port, null, username, password);
            // Tạo session mail
            Session session = Session.getDefaultInstance(properties);

            // Kết nối tới hộp thư mail
            Store store = session.getStore(urln);
            store.connect();

            // Đọc thư mục inbox
            Folder inbox = store.getFolder("INBOX");
            inbox.open(Folder.READ_ONLY);

            // Đọc các email trong inbox
            messages = inbox.getMessages();
        } catch (Exception ex){

        }

        return messages;
    }
}
