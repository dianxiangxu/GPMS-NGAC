package gpms.utils;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

/***
 * Handles Email
 * 
 * @author milsonmunakami
 *
 */
public class EmailUtil {

	private static String filePath = new String();
	// Server host
	final String smtpHostServer = "localhost";
	Properties properties = System.getProperties();

	public static String getFilePath() {
		return filePath;
	}

	public void setFilePath(String filePath) {
		EmailUtil.filePath = filePath;
	}

	public EmailUtil() {
		// Setup mail server
		properties.setProperty("mail.smtp.host", smtpHostServer);
	}

	public EmailUtil(String attachmentFile) throws Exception {
		// Setup mail server
		properties.setProperty("mail.smtp.host", smtpHostServer);
		this.setFilePath(this.getClass()
				.getResource("/uploads" + attachmentFile).toURI().getPath());
	}

	/***
	 * Sends Email using SMTP without authentication
	 * 
	 * @param emailID
	 *            Recipient Email ID
	 * @param subject
	 * @param body
	 */
	public void sendMailWithoutAuth(String emailID, String subject, String body) {
		Session session = Session.getInstance(properties, null);
		sendEmail(session, emailID, subject, body);
	}

	/***
	 * Sends Email to multiple users without authorization
	 * 
	 * @param primaryEmail
	 * @param emailList
	 *            Email lists for BCC
	 * @param subject
	 *            Subject of the email
	 * @param body
	 *            Body message of the email
	 */
	public void sendMailMultipleUsersWithoutAuth(String primaryEmail,
			List<String> emailList, String subject, String body) {
		try {
			Session session = Session.getInstance(properties, null);
			MimeMessage msg = new MimeMessage(session);

			msg.addHeader("Content-type", "text/HTML; charset=UTF-8");
			msg.addHeader("format", "flowed");
			msg.addHeader("Content-Transfer-Encoding", "8bit");

			// Sender Email Address
			msg.setFrom(new InternetAddress("do-not-reply@seal.boisestate.edu",
					"do-not-reply@seal.boisestate.edu"));

			msg.setSubject(subject, "UTF-8");
			msg.setText(body, "utf-8", "html");
			msg.setSentDate(new Date());
			msg.setRecipients(Message.RecipientType.TO,
					InternetAddress.parse(primaryEmail, false));
			for (String email : emailList) {
				msg.addRecipient(Message.RecipientType.BCC,
						new InternetAddress(email));
			}

			Transport.send(msg);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/***
	 * Sends Email with TLS Authentication
	 * 
	 * @param toEmail
	 *            Recipient Email ID
	 * @param subject
	 *            Subject of the email
	 * @param body
	 *            Body message of the email
	 */
	public void sendMailWithGmailTLS(String toEmail, String subject, String body) {
		// Valid gmail ID
		final String fromEmail = "noreplygpms@gmail.com";
		// Valid gmail password
		final String password = "gpmstest";

		// Must enable less secure Apps
		// https://www.google.com/settings/u/1/security/lesssecureapps
		System.out.println("TLSEmail Start");
		properties = new Properties();
		// SMTP Host
		properties.put("mail.smtp.host", "smtp.gmail.com");
		// TLS Port
		properties.put("mail.smtp.port", "587");
		properties.put("mail.smtp.auth", "true");
		properties.put("mail.smtp.starttls.enable", "true");

		// create Authenticator to authenticate the Session
		Authenticator auth = new Authenticator() {
			protected PasswordAuthentication getPasswordAuthentication() {
				return new PasswordAuthentication(fromEmail, password);
			}
		};
		System.out.println("Session created");
		Session session = Session.getInstance(properties, auth);
		sendEmail(session, toEmail, subject, body);
	}

	/***
	 * Sends Email with SSL Authentication
	 * 
	 * @param toEmail
	 *            Recipient Email ID
	 * @param subject
	 *            Subject of the email
	 * @param body
	 *            Body message of the email
	 */
	public void sendMailWithGmailSSL(String toEmail, String subject, String body) {
		// Valid gmail ID
		final String fromEmail = "noreplygpms@gmail.com";
		// Valid gmail password
		final String password = "gpmstest";
		System.out.println("SSLEmail Start");
		properties = new Properties();
		// SMTP Host
		properties.put("mail.smtp.host", "smtp.gmail.com");
		// SSL Port
		properties.put("mail.smtp.socketFactory.port", "465");
		properties.put("mail.smtp.socketFactory.class",
				"javax.net.ssl.SSLSocketFactory");
		properties.put("mail.smtp.auth", "true");
		// SMTP Port
		properties.put("mail.smtp.port", "465");

		Authenticator auth = new Authenticator() {
			protected PasswordAuthentication getPasswordAuthentication() {
				return new PasswordAuthentication(fromEmail, password);
			}
		};

		Session session = Session.getDefaultInstance(properties, auth);
		System.out.println("Session created");
		sendEmail(session, toEmail, subject, body);
	}

	/**
	 * Sends a simple HTML format email
	 * 
	 * @param toEmail
	 *            Recipient Email ID
	 * @param subject
	 *            Subject of the email
	 * @param body
	 *            Body message of the email
	 */
	public void sendEmail(Session session, String toEmail, String subject,
			String body) {
		try {
			MimeMessage msg = new MimeMessage(session);
			msg.addHeader("Content-type", "text/HTML; charset=UTF-8");
			msg.addHeader("format", "flowed");
			msg.addHeader("Content-Transfer-Encoding", "8bit");
			msg.setFrom(new InternetAddress("do-not-reply@seal.boisestate.edu",
					"do-not-reply@seal.boisestate.edu"));
			msg.setSubject(subject, "UTF-8");
			msg.setText(body, "utf-8", "html");
			msg.setSentDate(new Date());
			msg.setRecipients(Message.RecipientType.TO,
					InternetAddress.parse(toEmail, false));
			Transport.send(msg);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/***
	 * Sends email with attachment
	 * 
	 * @param toEmail
	 *            Recipient Email ID
	 * @param subject
	 *            Subject of the email
	 * @param body
	 *            Body message of the email
	 * @param attachName
	 *            Attachment File Name
	 * @throws IOException
	 */
	public void sendAttachmentEmail(String toEmail, String subject,
			String body, String attachName) throws IOException {
		try {
			Session session = Session.getDefaultInstance(properties);
			System.out.println("Session created");

			MimeMessage msg = new MimeMessage(session);
			msg.addHeader("Content-type", "text/HTML; charset=UTF-8");
			msg.addHeader("format", "flowed");
			msg.addHeader("Content-Transfer-Encoding", "8bit");

			msg.setFrom(new InternetAddress("do-not-reply@seal.boisestate.edu",
					"do-not-reply@seal.boisestate.edu"));
			msg.setReplyTo(InternetAddress.parse(
					"do-not-reply@seal.boisestate.edu", false));
			msg.setSubject(subject, "UTF-8");
			msg.setSentDate(new Date());
			msg.setRecipients(Message.RecipientType.TO,
					InternetAddress.parse(toEmail, false));

			// Create the message body part
			MimeBodyPart messageBodyPart = new MimeBodyPart();
			messageBodyPart.setText(body, "utf-8", "html");
			Multipart multipart = new MimeMultipart();
			multipart.addBodyPart(messageBodyPart);

			// Format attachment part
			messageBodyPart = new MimeBodyPart();
			messageBodyPart.attachFile(getFilePath());
			messageBodyPart.setFileName(attachName);
			multipart.addBodyPart(messageBodyPart);

			msg.setContent(multipart);
			Transport.send(msg);
			System.out.println("Email Sent Successfully with attachment!!");
		} catch (MessagingException e) {
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
	}

	/***
	 * Sends Image attachment in email
	 * 
	 * @param toEmail
	 *            Recipient Email ID
	 * @param subject
	 *            Subject of the email
	 * @param body
	 *            Body message of the email
	 * @param attachName
	 *            Attachment File Name
	 */
	public void sendImageEmail(String toEmail, String subject, String body,
			String attachName) {
		try {
			// Get the default Session object.
			Session session = Session.getDefaultInstance(properties);
			System.out.println("Session created");
			MimeMessage msg = new MimeMessage(session);
			msg.addHeader("Content-type", "text/HTML; charset=UTF-8");
			msg.addHeader("format", "flowed");
			msg.addHeader("Content-Transfer-Encoding", "8bit");
			msg.setFrom(new InternetAddress("do-not-reply@seal.boisestate.edu",
					"do-not-reply@seal.boisestate.edu"));
			msg.setReplyTo(InternetAddress.parse(
					"do-not-reply@seal.boisestate.edu", false));
			msg.setSubject(subject, "UTF-8");
			msg.setSentDate(new Date());
			msg.setRecipients(Message.RecipientType.TO,
					InternetAddress.parse(toEmail, false));

			// Create the message body part
			MimeBodyPart messageBodyPart = new MimeBodyPart();
			messageBodyPart.setText(body, "utf-8", "html");
			Multipart multipart = new MimeMultipart();
			multipart.addBodyPart(messageBodyPart);

			// Add Image attachment in the email body
			messageBodyPart = new MimeBodyPart();
			DataSource source = new FileDataSource(getFilePath());
			messageBodyPart.setDataHandler(new DataHandler(source));
			messageBodyPart.setFileName(attachName);
			messageBodyPart.setHeader("Content-ID", "image_id");
			multipart.addBodyPart(messageBodyPart);

			// Displaying image in the email body
			messageBodyPart = new MimeBodyPart();
			messageBodyPart.setContent("<h1>Attached Image</h1>"
					+ "<img src='cid:image_id'>", "text/html");
			multipart.addBodyPart(messageBodyPart);

			msg.setContent(multipart);
			Transport.send(msg);
			System.out.println("Email Sent Successfully with image!!");
		} catch (MessagingException e) {
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
	}
}
