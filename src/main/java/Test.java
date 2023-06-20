import com.sun.mail.util.BASE64DecoderStream;
import com.sun.mail.util.QPDecoderStream;
import com.viettel.roaming.tool_import.import_data.ImportTest;
import com.viettel.security.PassTranformer;

import javax.mail.*;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.Security;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

public class Test {
    public static void main(String[] args) throws Exception {
        Message[] messages = null;
        Properties props = new Properties();
        final String HOST = "pop3.viettel.com.vn";
        final String PROTOCOL = "pop3";
        final String PORT = "995";
        String username = "soncn2@viettel.com.vn";
        String pass = PassTranformer.decrypt("704c3d986412881dcb7b22c153171243");
        final String SSL_FACTORY = "javax.net.ssl.SSLSocketFactory";

        Security.addProvider(new com.sun.net.ssl.internal.ssl.Provider());
        props.setProperty("mail.pop3.socketFactory.class", SSL_FACTORY);
        props.setProperty("mail.pop3.socketFactory.fallback", "false");
        props.setProperty("mail.pop3.port", PORT);
        props.setProperty("mail.pop3.socketFactory.port", PORT);
        URLName urln = new URLName(PROTOCOL, HOST, Integer.parseInt(PORT), null, username, pass);

        Session session = Session.getInstance(props, null);
        Store store = session.getStore(urln);
        store.connect();

        // Đọc thư mục inbox
        Folder inbox = store.getFolder("INBOX");
        inbox.open(Folder.READ_ONLY);

        // Đọc các email trong inbox
        messages = inbox.getMessages();
        for(Message message : messages){
            if(message.getSubject().contains("HUR")){
                System.out.println(message.getSubject());
                Multipart multipart = (Multipart) message.getContent();
                for (int j = 0; j < multipart.getCount(); j++) {
                    BodyPart bodyPart = multipart.getBodyPart(j);
                    if (bodyPart.getDisposition() != null && bodyPart.getDisposition().equalsIgnoreCase("attachment")) {
                        InputStream is = bodyPart.getInputStream();
                        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                        byte[] buffer = new byte[1024];
                        int bytesRead;
                        while ((bytesRead = is.read(buffer)) != -1) {
                            byteArrayOutputStream.write(buffer, 0, bytesRead);
                        }
                        String attachmentContent = new String(byteArrayOutputStream.toByteArray(), StandardCharsets.UTF_8);
                        ImportTest.Import(attachmentContent);
                        System.out.println("het email");
                    }
                }
            }
        }
    }
}
