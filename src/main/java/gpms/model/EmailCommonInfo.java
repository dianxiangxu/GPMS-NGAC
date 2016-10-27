package gpms.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class EmailCommonInfo implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private String emailSubject = new String();
	private String emailBody = new String();
	private String authorName = new String();
	private String piEmail = new String();
	private List<String> emaillist = new ArrayList<String>();

	public EmailCommonInfo() {

	}

	public String getEmailSubject() {
		return emailSubject;
	}

	public void setEmailSubject(String emailSubject) {
		this.emailSubject = emailSubject;
	}

	public String getEmailBody() {
		return emailBody;
	}

	public void setEmailBody(String emailBody) {
		this.emailBody = emailBody;
	}

	public String getAuthorName() {
		return authorName;
	}

	public void setAuthorName(String authorName) {
		this.authorName = authorName;
	}

	public String getPiEmail() {
		return piEmail;
	}

	public void setPiEmail(String piEmail) {
		this.piEmail = piEmail;
	}

	public List<String> getEmaillist() {
		return emaillist;
	}

	public void setEmaillist(List<String> emaillist) {
		this.emaillist = emaillist;
	}

}
