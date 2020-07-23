package gpms.policy.customFunctions;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.NoSuchProviderException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
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
		    public Node exec(EventContext eventCtx, String userID, String processID, PDP pdp, Function function, FunctionEvaluator functionEvaluator) throws PMException {
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

		        Map<String,String>properties1 = child.getProperties();
		        String email = properties1.get("workEmail");
		        //System.out.println(email+"!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
		       
		        
		        try {
					sendSimpleEmail(email,"Obligation Email Test", "Obligation Email Test"  );
				} catch (AddressException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (MessagingException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
		    
		           	 
		        return child;
		    }
		    
		    
			public void sendSimpleEmail(String to,String subject, String body) throws AddressException, MessagingException {
				ExecutorService emailExecutor = Executors.newSingleThreadExecutor();
		        emailExecutor.execute(new Runnable() {
		            @Override
		            public void run() {
				//System.out.println(to);
				 Properties properties = new Properties();
			        properties.put("mail.smtp.host", "smtp.gmail.com");
			        properties.put("mail.smtp.port", "587");
			        properties.put("mail.smtp.auth", "true");
			        properties.put("mail.smtp.starttls.enable", "true"); //TLS
			        properties.put("mail.smtp.ssl.trust", "smtp.gmail.com");
				final String from = "gpmsngac2020@gmail.com";
		        final String password = "GPMSngac2020*";
		        String host = "smtp.gmail.com";
//		        Properties properties = System.getProperties();
		        
		        final String body1 = new String(body+"<br><b>[This email is from GPMS-NGAC development test purpose only.]</b>");
		        
		        final String to1 = new String("gpmsngac2020@gmail.com");
		        
		        
		        Session session = Session.getDefaultInstance(properties);
		        session.setDebug(true);

		             // creates a new e-mail message
		             Message msg = new MimeMessage(session);

		             try {
			             InternetAddress[] toAddresses = { new InternetAddress(from) };

						msg.setFrom(new InternetAddress(from));
						msg.setRecipients(Message.RecipientType.TO, toAddresses);
			             msg.setSubject(subject);
			             msg.setSentDate(new Date());
			             // set plain text message
			             msg.setText(body1);
			             Transport t = session.getTransport("smtp");
							t.connect(from, password);
				             t.sendMessage(msg, msg.getAllRecipients());
				             t.close();
					} catch (MessagingException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
		             


		    
		            }});
		        emailExecutor.shutdown();	
			
			}
	}

