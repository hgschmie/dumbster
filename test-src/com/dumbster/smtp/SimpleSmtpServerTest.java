/*
 * Dumbster - a dummy SMTP server
 * Copyright 2004 Jason Paul Kitchen
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.dumbster.smtp;

import org.junit.*;

import static org.junit.Assert.*;

import javax.mail.Session;
import javax.mail.Message;
import javax.mail.Transport;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.InternetAddress;
import java.util.Properties;
import java.util.Date;
import java.util.Iterator;

public class SimpleSmtpServerTest {
    private static final int SMTP_PORT = 1081;

    private SimpleSmtpServer server;

    @Before
    public void setup() {
        server = SimpleSmtpServer.start(SMTP_PORT);
    }

    @After
    public void teardown() {
        server.stop();
        server = null;
    }

    @Test
    public void testSend() {
        try {
            sendMessage(SMTP_PORT, "sender@here.com", "Test", "Test Body", "receiver@there.com");
        } catch (Exception e) {
            e.printStackTrace();
            fail("Unexpected exception: " + e);
        }

        assertTrue(server.getEmailCount() == 1);
        Iterator emailIter = server.getReceivedEmail();
        MailMessage email = (MailMessage) emailIter.next();
        assertEquals("Test", email.getFirstHeaderValue("Subject"));
        assertEquals("Test Body",email.getBody());
    }

    @Test
    public void testThreadedSend() throws InterruptedException {
        server.setThreaded(true);
        try {
            sendMessage(SMTP_PORT, "sender@here.com", "Test", "Test Body", "receiver@there.com");
        } catch (Exception e) {
            e.printStackTrace();
            fail("Unexpected exception: " + e);
        }
        Thread.sleep(250);
        assertTrue(server.getEmailCount() == 1);
        Iterator emailIter = server.getReceivedEmail();
        MailMessage email = (MailMessage) emailIter.next();
        assertEquals("Test", email.getFirstHeaderValue("Subject"));
        assertEquals("Test Body",email.getBody());
    }

    @Test
    public void testSendMessageWithCarriageReturn() {
        String bodyWithCR = "\n\nKeep these pesky carriage returns\n\n";
        try {
            sendMessage(SMTP_PORT, "sender@hereagain.com", "CRTest", bodyWithCR, "receivingagain@there.com");
        } catch (Exception e) {
            e.printStackTrace();
            fail("Unexpected exception: " + e);
        }

        assertEquals(1, server.getEmailCount());
        Iterator emailIter = server.getReceivedEmail();
        MailMessage email = (MailMessage) emailIter.next();
        assertEquals(bodyWithCR, email.getBody());
    }

    @Test
    public void testSendTwoMessagesSameConnection() {
        try {
            MimeMessage[] mimeMessages = new MimeMessage[2];
            Properties mailProps = getMailProperties(SMTP_PORT);
            Session session = Session.getInstance(mailProps, null);
 
            mimeMessages[0] = createMessage(session, "sender@whatever.com", "receiver@home.com", "Doodle1", "Bug1");
            mimeMessages[1] = createMessage(session, "sender@whatever.com", "receiver@home.com", "Doodle2", "Bug2");

            Transport transport = session.getTransport("smtp");
            transport.connect("localhost", SMTP_PORT, null, null);

            for (int i = 0; i < mimeMessages.length; i++) {
                MimeMessage mimeMessage = mimeMessages[i];
                transport.sendMessage(mimeMessage, mimeMessage.getAllRecipients());
            }

            transport.close();
        } catch (MessagingException e) {
            e.printStackTrace();
            fail("Unexpected exception: " + e);
        }

        assertEquals(2, server.getEmailCount());
    }

    @Test
    public void testSendTwoMsgsWithLogin() {
        try {
            String Server = "localhost";
            String From = "sender@here.com";
            String To = "receiver@there.com";
            String Subject = "Test";
            String body = "Test Body";

            Properties props = System.getProperties();

            if (Server != null) {
                props.put("mail.smtp.host", Server);
            }

            Session session = Session.getDefaultInstance(props, null);
            Message msg = new MimeMessage(session);

            if (From != null) {
                msg.setFrom(new InternetAddress(From));
            } else {
                msg.setFrom();
            }

            InternetAddress.parse(To, false);
            msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(To, false));
            msg.setSubject(Subject);

            msg.setText(body);
            msg.setHeader("X-Mailer", "musala");
            msg.setSentDate(new Date());
            msg.saveChanges();

            Transport transport = null;

            try {
                transport = session.getTransport("smtp");
                transport.connect(Server, SMTP_PORT, "ddd", "ddd");
                transport.sendMessage(msg, InternetAddress.parse(To, false));
                transport.sendMessage(msg, InternetAddress.parse("dimiter.bakardjiev@musala.com", false));

            } catch (javax.mail.MessagingException me) {
                me.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (transport != null) {
                    transport.close();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        assertEquals(2, server.getEmailCount());
        Iterator emailIter = server.getReceivedEmail();
        MailMessage email = (MailMessage) emailIter.next();
        assertEquals("Test", email.getFirstHeaderValue("Subject"));
        assertEquals("Test Body", email.getBody());
    }

    private Properties getMailProperties(int port) {
        Properties mailProps = new Properties();
        mailProps.setProperty("mail.smtp.host", "localhost");
        mailProps.setProperty("mail.smtp.port", "" + port);
        mailProps.setProperty("mail.smtp.sendpartial", "true");
        return mailProps;
    }


    private void sendMessage(int port, String from, String subject, String body, String to) throws MessagingException {
        Properties mailProps = getMailProperties(port);
        Session session = Session.getInstance(mailProps, null);
        //session.setDebug(true);

        MimeMessage msg = createMessage(session, from, to, subject, body);
        Transport.send(msg);
    }

    private MimeMessage createMessage(
            Session session, String from, String to, String subject, String body) throws MessagingException {
        MimeMessage msg = new MimeMessage(session);
        msg.setFrom(new InternetAddress(from));
        msg.setSubject(subject);
        msg.setSentDate(new Date());
        msg.setText(body);
        msg.setRecipient(Message.RecipientType.TO, new InternetAddress(to));
        return msg;
    }
}
