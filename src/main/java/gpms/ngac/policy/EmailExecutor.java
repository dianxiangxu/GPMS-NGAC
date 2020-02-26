package gpms.ngac.policy;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import gov.nist.csd.pm.epp.FunctionEvaluator;
import gov.nist.csd.pm.epp.events.AssignEvent;
import gov.nist.csd.pm.epp.events.AssignToEvent;
import gov.nist.csd.pm.epp.events.DeassignEvent;
import gov.nist.csd.pm.epp.events.DeassignFromEvent;
import gov.nist.csd.pm.epp.events.EventContext;
import gov.nist.csd.pm.epp.functions.FunctionExecutor;
import gov.nist.csd.pm.exceptions.PMException;
import gov.nist.csd.pm.pdp.PDP;
import gov.nist.csd.pm.pip.graph.model.nodes.Node;
import gov.nist.csd.pm.pip.obligations.evr.EVRException;
import gov.nist.csd.pm.pip.obligations.model.functions.Arg;
import gov.nist.csd.pm.pip.obligations.model.functions.Function;

public class EmailExecutor implements FunctionExecutor{
	final String smtpHostServer = "localhost";

		 @Override
		    public String getFunctionName() {
		        return "email";
		    }

		    @Override
		    public int numParams() {
		        return 1;
		    }

		    @Override
		    public Node exec(EventContext eventCtx, long userID, long processID, PDP pdp, Function function, FunctionEvaluator functionEvaluator) throws PMException {
		    	 Node child;
		         if(eventCtx instanceof AssignToEvent) {
		             child = ((AssignToEvent) eventCtx).getChildNode();
		         } else if (eventCtx instanceof AssignEvent) {
		             child = eventCtx.getTarget();
		         } else if (eventCtx instanceof DeassignFromEvent) {
		             child = ((DeassignFromEvent) eventCtx).getChildNode();
		         } else if (eventCtx instanceof DeassignEvent) {
		             child = eventCtx.getTarget();
		         } else {
		             throw new EVRException("invalid event context for function child_of_assign. Valid event contexts are AssignTo, " +
		                     "Assign, DeassignFrom, and Deassign");
		         }

		        Map<String,String>properties = child.getProperties();
		        String email = properties.get("workEmail");
		        System.out.println(email+"!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
		        sendSimpleEmail(email,"Obligation Email Test", "Obligation Email Test" );
		        return child;
		    }
		    
		    
			public void sendSimpleEmail(String to,String subject, String body) {
				
				final String from = "gpmsngac2020@gmail.com";
		        final String password = "GPMSngac2020*";
		        //String from = "";
		        String host = "smtp.gmail.com";
		        Properties properties = System.getProperties();
		        
		        body = body+"<br><b>[This email is from GPMS-NGAC development test purpose only.]</b>";
		        
		        to = "gpmsngac2020@gmail.com";
		        // Setup mail server
		        properties.put("mail.smtp.host", host);
		        properties.put("mail.smtp.port", "587");
		        properties.put("mail.smtp.auth", "true");
		        properties.put("mail.smtp.starttls.enable", "true"); //TLS
		        properties.put("mail.smtp.ssl.trust", "smtp.gmail.com");



		        // Get the Session object.// and pass username and password
		        Session session = Session.getInstance(properties, new javax.mail.Authenticator() {

		            protected PasswordAuthentication getPasswordAuthentication() {

		                return new PasswordAuthentication(from, password);

		            }

		        });

		        // Used to debug SMTP issues
		        session.setDebug(true);

		        try {
		            // Create a default MimeMessage object.
		        	
		        	MimeMessage message = new MimeMessage(session);
					message.addHeader("Content-type", "text/HTML; charset=UTF-8");
					message.addHeader("format", "flowed");
					message.addHeader("Content-Transfer-Encoding", "8bit");
					
		        	
		        	
		            //MimeMessage message = new MimeMessage(session);
					//msg.setFrom(new InternetAddress("do-not-reply@seal.boisestate.edu",
					//		"do-not-reply@seal.boisestate.edu"));
					//msg.setSubject(subject, "UTF-8");
					//msg.setText(body, "utf-8", "html");

		            // Set From: header field of the header.
		            message.setFrom(new InternetAddress(from));

		            // Set To: header field of the header.
		            message.addRecipient(Message.RecipientType.TO, new InternetAddress(to));
		            message.addRecipient(Message.RecipientType.BCC, new InternetAddress("gpmsngac2020@gmail.com"));
		           // message.addRecipient(Message.RecipientType.CC, new InternetAddress("dxu@umkc.edu"));

		            // Set Subject: header field
		            message.setSubject(subject, "UTF-8");

		            // Now set the actual message
		            message.setText(body,"utf-8", "html");

		            System.out.println("sending...");
		            // Send message
		            Transport.send(message);
		            System.out.println("Sent message successfully....");
		        } catch (MessagingException mex) {
		            mex.printStackTrace();
		        }

		    
		    
			}
	}

