package gpms.dao;

import gpms.DAL.MongoDBConnector;
import gpms.model.ApprovalType;
import gpms.model.Delegation;
import gpms.model.GPMSCommonInfo;
import gpms.model.InvestigatorRefAndPosition;
import gpms.model.NotificationLog;
import gpms.model.PositionDetails;
import gpms.model.Proposal;
import gpms.model.SignatureUserInfo;
import gpms.model.Status;
import gpms.model.UserAccount;
import gpms.model.UserProfile;
import gpms.rest.NotificationService;
import gpms.utils.EmailUtil;

import java.net.UnknownHostException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.core.MediaType;

import org.glassfish.jersey.media.sse.OutboundEvent;
import org.glassfish.jersey.media.sse.OutboundEvent.Builder;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.Morphia;
import org.mongodb.morphia.dao.BasicDAO;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;

import com.mongodb.MongoClient;
import com.mongodb.MongoException;

public class NotificationDAO extends BasicDAO<NotificationLog, String> {
	private static final String DBNAME = "db_gpms";
	public static final String COLLECTION_NAME = "notification";
	private static Morphia morphia;
	private static Datastore ds;

	private static Morphia getMorphia() throws UnknownHostException,
			MongoException {
		if (morphia == null) {
			morphia = new Morphia().map(UserProfile.class).map(
					UserAccount.class);
		}
		return morphia;
	}

	@Override
	public Datastore getDatastore() {
		if (ds == null) {
			try {
				ds = getMorphia().createDatastore(MongoDBConnector.getMongo(),
						DBNAME);
			} catch (UnknownHostException | MongoException e) {
				e.printStackTrace();
			}
		}
		ds.ensureIndexes();
		return ds;
	}

	public NotificationDAO(MongoClient mongo, Morphia morphia, String dbName) {
		super(mongo, morphia, dbName);
	}

	public long findAllNotificationCountAUser(GPMSCommonInfo userInfo)
			throws ParseException {
		Datastore ds = getDatastore();
		String userProfileId = userInfo.getUserProfileID();
		String userCollege = userInfo.getUserCollege();
		String userDepartment = userInfo.getUserDepartment();
		String userPositionType = userInfo.getUserPositionType();
		String userPositionTitle = userInfo.getUserPositionTitle();
		boolean isUserAdmin = userInfo.isUserIsAdmin();
		Query<NotificationLog> notificationQuery = ds
				.createQuery(NotificationLog.class);
		if (isUserAdmin) {
			notificationQuery.and(notificationQuery.criteria("for admin")
					.equal(true), notificationQuery.criteria("viewed by admin")
					.equal(false));
			return notificationQuery.countAll();
		} else {
			switch (userPositionTitle) {
			case "Associate Chair":
				userPositionTitle = "Department Chair";
				break;
			case "Department Administrative Assistant":
				userPositionTitle = "Business Manager";
				break;
			case "Associate Dean":
				userPositionTitle = "Dean";
				break;
			default:
				break;
			}
			notificationQuery.and(
					notificationQuery.criteria("viewed by user").equal(false),
					notificationQuery.criteria("user profile id").equal(
							userProfileId),
					notificationQuery.criteria("college").equal(userCollege),
					notificationQuery.criteria("department").equal(
							userDepartment),
					notificationQuery.criteria("position type").equal(
							userPositionType),
					notificationQuery.criteria("position title").equal(
							userPositionTitle),
					notificationQuery.criteria("for admin").equal(false));
			return notificationQuery.countAll();
		}
	}

	public List<NotificationLog> findAllNotificationForAUser(
			GPMSCommonInfo userInfo) throws ParseException {
		Datastore ds = getDatastore();
		int offset = 1;
		int limit = 10;
		List<NotificationLog> notifications = new ArrayList<NotificationLog>();
		Query<NotificationLog> notificationQuery = ds
				.createQuery(NotificationLog.class)
				.retrievedFields(true, "type", "action", "proposal id",
						"proposal title", "user profile id", "user name",
						"college", "department", "position type",
						"position title", "viewed by user", "viewed by admin",
						"activity on", "for admin", "critical")
				.order("-activity on");
		Query<NotificationLog> removeNotifyQuery = ds.createQuery(
				NotificationLog.class).retrievedFields(true, "type", "action",
				"proposal id", "proposal title", "user profile id",
				"user name", "college", "department", "position type",
				"position title", "viewed by user", "viewed by admin",
				"activity on", "for admin", "critical");
		if (userInfo.isUserIsAdmin()) {
			notifications = getNotificationsForAdmin(offset, limit, ds,
					notificationQuery, removeNotifyQuery);
		} else {
			notifications = getNotificationsForUser(offset, limit, userInfo,
					ds, notificationQuery, removeNotifyQuery);
		}
		return notifications;
	}

	/**
	 * Gets Notifications For a General User
	 * 
	 * @param offset
	 *            Offset of Query
	 * @param limit
	 *            Limit of Query
	 * @param userProfileId
	 *            User Profile Id
	 * @param userCollege
	 *            User's College
	 * @param userDepartment
	 *            User's Department
	 * @param userPositionType
	 *            User's Position Type
	 * @param userPositionTitle
	 *            User's Position Title
	 * @param ds
	 *            Datastore
	 * @param notificationQuery
	 *            Notification Fetch Query
	 * @param removeNotifyQuery
	 *            Remove Notification after Fetch complete Query
	 * @return
	 */
	private List<NotificationLog> getNotificationsForUser(int offset,
			int limit, GPMSCommonInfo userInfo, Datastore ds,
			Query<NotificationLog> notificationQuery,
			Query<NotificationLog> removeNotifyQuery) {
		List<NotificationLog> notifications = new ArrayList<NotificationLog>();
		String userProfileId = userInfo.getUserProfileID();
		String userCollege = userInfo.getUserCollege();
		String userDepartment = userInfo.getUserDepartment();
		String userPositionType = userInfo.getUserPositionType();
		String userPositionTitle = userInfo.getUserPositionTitle();
		switch (userPositionTitle) {
		case "Associate Chair":
			userPositionTitle = "Department Chair";
			break;
		case "Department Administrative Assistant":
			userPositionTitle = "Business Manager";
			break;
		case "Associate Dean":
			userPositionTitle = "Dean";
			break;
		default:
			break;
		}
		notificationQuery.and(
				notificationQuery.criteria("for admin").equal(false),
				notificationQuery.criteria("user profile id").equal(
						userProfileId),
				notificationQuery.criteria("college").equal(userCollege),
				notificationQuery.criteria("department").equal(userDepartment),
				notificationQuery.criteria("position type").equal(
						userPositionType),
				notificationQuery.criteria("position title").equal(
						userPositionTitle));
		removeNotifyQuery.and(
				removeNotifyQuery.criteria("for admin").equal(false),
				removeNotifyQuery.criteria("viewed by user").equal(false),
				removeNotifyQuery.criteria("user profile id").equal(
						userProfileId),
				removeNotifyQuery.criteria("college").equal(userCollege),
				removeNotifyQuery.criteria("department").equal(userDepartment),
				removeNotifyQuery.criteria("position type").equal(
						userPositionType),
				removeNotifyQuery.criteria("position title").equal(
						userPositionTitle));
		notifications = notificationQuery.offset(offset - 1).limit(limit)
				.asList();
		UpdateOperations<NotificationLog> ops = ds.createUpdateOperations(
				NotificationLog.class).set("viewed by user", true);
		ds.update(removeNotifyQuery, ops);
		return notifications;
	}

	/**
	 * Gets Notifications For an Admin
	 * 
	 * @param offset
	 *            Offset of Query
	 * @param limit
	 *            Limit of Query
	 * @param ds
	 *            Datastore
	 * @param notificationQuery
	 *            Notification Fetch Query
	 * @param removeNotifyQuery
	 *            Remove Notification after Fetch complete Query
	 * @return
	 */
	private List<NotificationLog> getNotificationsForAdmin(int offset,
			int limit, Datastore ds, Query<NotificationLog> notificationQuery,
			Query<NotificationLog> removeNotifyQuery) {
		List<NotificationLog> notifications;
		notificationQuery.criteria("for admin").equal(true);
		removeNotifyQuery.and(removeNotifyQuery.criteria("viewed by admin")
				.equal(false),
				removeNotifyQuery.criteria("for admin").equal(true));
		notifications = notificationQuery.offset(offset - 1).limit(limit)
				.asList();
		UpdateOperations<NotificationLog> ops = ds.createUpdateOperations(
				NotificationLog.class).set("viewed by admin", true);
		ds.update(removeNotifyQuery, ops);
		return notifications;
	}

	public void sendNotification(Delegation existingDelegation,
			GPMSCommonInfo userInfo, String notificationMessage,
			String notificationType, boolean isCritical) {
		String userProfileID = userInfo.getUserProfileID();
		String userName = userInfo.getUserName();
		String userCollege = userInfo.getUserCollege();
		String userDepartment = userInfo.getUserDepartment();
		String userPositionType = userInfo.getUserPositionType();
		String userPositionTitle = userInfo.getUserPositionTitle();
		NotificationLog notification = new NotificationLog();
		// For Admin
		notification.setType(notificationType);
		notification.setAction(notificationMessage);
		notification.setUserProfileId(existingDelegation.getDelegateeId());
		notification.setUsername(existingDelegation.getDelegateeUsername());
		notification.setForAdmin(true);
		notification.setCritical(isCritical);
		// notification.isViewedByUser(true);
		save(notification);
		// For Delegator
		notification = new NotificationLog();
		notification.setType(notificationType);
		notification.setAction(notificationMessage);
		notification.setUserProfileId(userProfileID);
		notification.setUsername(userName);
		notification.setCollege(userCollege);
		notification.setDepartment(userDepartment);
		notification.setPositionType(userPositionType);
		notification.setPositionTitle(userPositionTitle);
		notification.setCritical(isCritical);
		// notification.isViewedByUser(true);
		save(notification);
		// For Delegatee
		notification = new NotificationLog();
		notification.setType(notificationType);
		notification.setAction(notificationMessage);
		notification.setUserProfileId(existingDelegation.getDelegateeId());
		notification.setUsername(existingDelegation.getDelegateeUsername());
		notification.setCollege(existingDelegation.getDelegateeCollege());
		notification.setDepartment(existingDelegation.getDelegateeDepartment());
		notification.setPositionType(existingDelegation
				.getDelegateePositionType());
		notification.setPositionTitle(userPositionTitle);
		notification.setCritical(isCritical);
		// notification.isViewedByUser(true);
		save(notification);

		// Broadcasting SSE
		OutboundEvent.Builder eventBuilder = new OutboundEvent.Builder();
		OutboundEvent event = eventBuilder.name("notification")
				.mediaType(MediaType.TEXT_PLAIN_TYPE).data(String.class, "1")
				.build();
		NotificationService.BROADCASTER.broadcast(event);
	}

	@SuppressWarnings("unused")
	private void updateAllNotificationAsViewed(
			List<NotificationLog> notifications, boolean isUserAdmin) {
		Datastore ds = getDatastore();
		for (NotificationLog notificationLog : notifications) {
			if (isUserAdmin) {
				notificationLog.setViewedByAdmin(true);
			} else {
				notificationLog.setViewedByUser(true);
			}
			ds.save(notificationLog);
		}
	}

	/**
	 * Sends Notification for User Account is Deleted
	 * 
	 * @param userProfile
	 * @param userAccount
	 */
	public void sendNotification(UserProfile userProfile,
			UserAccount userAccount) {
		String messageBody = new String();
		EmailUtil emailUtil = new EmailUtil();
		if (userProfile.isDeleted()) {
			messageBody = "Hello "
					+ userProfile.getFullName()
					+ ",<br/> Your account has been deleted to reactivate you can contact administrator: <a href='http://seal.boisestate.edu:8080/GPMS/ContactUs.jsp' title='GPMS Contact Us' target='_blank'>Contact Us</a><br/><br/>Thank you, <br/> GPMS Team";
			emailUtil.sendMailWithoutAuth(userProfile.getWorkEmails().get(0),
					"You have been deleted " + userProfile.getFullName(),
					messageBody);
		}
		NotificationLog notification = new NotificationLog();
		notification.setType("User");
		notification.setAction("Account is deleted.");
		notification.setUserProfileId(userProfile.getId().toString());
		notification.setUsername(userAccount.getUserName());
		notification.setForAdmin(true);
		notification.setCritical(true);
		save(notification);
	}

	/**
	 * Sends Notification for User Account is Activated/ Deactivated
	 * 
	 * @param isActive
	 * @param userProfile
	 * @param userAccount
	 */
	public void sendNotification(Boolean isActive, UserProfile userProfile,
			UserAccount userAccount) {
		String messageBody = new String();
		EmailUtil emailUtil = new EmailUtil();
		NotificationLog notification = new NotificationLog();
		String notificationMessage = new String();
		boolean isCritical = false;
		if (isActive) {
			notificationMessage = "Account is activated.";
			messageBody = "Hello "
					+ userProfile.getFullName()
					+ ",<br/><br/> Your account has been activated and you can login now using your credential: <a href='http://seal.boisestate.edu:8080/GPMS/Login.jsp' title='GPMS Login' target='_blank'>Login Here</a><br/><br/>Thank you, <br/> GPMS Team";
			emailUtil.sendMailWithoutAuth(
					userProfile.getWorkEmails().get(0),
					"Successfully Activated your account "
							+ userProfile.getFullName(), messageBody);
		} else {
			notificationMessage = "Account is deactivated.";
			isCritical = true;

			messageBody = "Hello "
					+ userProfile.getFullName()
					+ ",<br/> Your account has been deactivated to reactivate you can contact administrator: <a href='http://seal.boisestate.edu:8080/GPMS/ContactUs.jsp' title='GPMS Contact Us' target='_blank'>Contact Us</a><br/><br/>Thank you, <br/> GPMS Team";
			emailUtil.sendMailWithoutAuth(userProfile.getWorkEmails().get(0),
					"You have been Deactivated " + userProfile.getFullName(),
					messageBody);
		}
		notification.setType("User");
		notification.setAction(notificationMessage);
		notification.setUserProfileId(userProfile.getId().toString());
		notification.setUsername(userAccount.getUserName());
		notification.setForAdmin(true);
		notification.setCritical(isCritical);
		save(notification);
		for (PositionDetails positions : userProfile.getDetails()) {
			notification = new NotificationLog();
			notification.setType("User");
			notification.setAction(notificationMessage);
			notification.setUserProfileId(userProfile.getId().toString());
			notification.setUsername(userAccount.getUserName());
			notification.setCollege(positions.getCollege());
			notification.setDepartment(positions.getDepartment());
			notification.setPositionType(positions.getPositionType());
			notification.setPositionTitle(positions.getPositionTitle());
			notification.setCritical(isCritical);
			save(notification);
		}
		Builder eventBuilder = new Builder();
		OutboundEvent event = eventBuilder.name("notification")
				.mediaType(MediaType.TEXT_PLAIN_TYPE).data(String.class, "1")
				.build();
		NotificationService.BROADCASTER.broadcast(event);
	}

	/**
	 * @param proposalID
	 * @param projectTitle
	 * @param notificationMessage
	 * @param notificationType
	 * @param isCritical
	 */
	public void notifyAdmin(String proposalID, String projectTitle,
			String notificationMessage, String notificationType,
			boolean isCritical) {
		NotificationLog notification = new NotificationLog();
		if (isCritical) {
			notification.setCritical(true);
		}
		notification.setType(notificationType);
		notification.setAction(notificationMessage);
		notification.setProposalId(proposalID);
		notification.setProposalTitle(projectTitle);
		notification.setForAdmin(true);
		save(notification);
	}

	/**
	 * @param proposalID
	 * @param projectTitle
	 * @param notificationMessage
	 * @param notificationType
	 * @param isCritical
	 * @param newPI
	 */
	public void notifyInvestigators(String proposalID, String projectTitle,
			String notificationMessage, String notificationType,
			boolean isCritical, InvestigatorRefAndPosition newPI) {
		NotificationLog notification;
		notification = new NotificationLog();
		if (isCritical) {
			notification.setCritical(true);
		}
		notification.setType(notificationType);
		notification.setAction(notificationMessage);
		notification.setProposalId(proposalID);
		notification.setProposalTitle(projectTitle);
		notification.setUserProfileId(newPI.getUserProfileId());
		notification.setUsername(newPI.getUserRef().getUserAccount()
				.getUserName());
		notification.setCollege(newPI.getCollege());
		notification.setDepartment(newPI.getDepartment());
		notification.setPositionType(newPI.getPositionType());
		notification.setPositionTitle(newPI.getPositionTitle());
		save(notification);
	}

	/**
	 * @param existingProposal
	 * @param proposalID
	 * @param signatures
	 */
	public void notifyIRBs(Proposal existingProposal, String proposalID,
			List<SignatureUserInfo> signatures) {
		NotificationLog notification;
		existingProposal.setIrbApproval(ApprovalType.READYFORAPPROVAL);
		existingProposal.getProposalStatus().add(Status.READYFORREVIEWBYIRB);
		for (SignatureUserInfo irb : signatures) {
			if (irb.getPositionTitle().equals("IRB")) {
				notification = new NotificationLog();
				notification.setCritical(true);
				notification.setType("Proposal");
				notification.setAction("Ready for Reviewal");
				notification.setProposalId(proposalID);
				notification.setProposalTitle(existingProposal.getProjectInfo()
						.getProjectTitle());
				notification.setUserProfileId(irb.getUserProfileId());
				notification.setUsername(irb.getUserName());
				notification.setCollege(irb.getCollege());
				notification.setDepartment(irb.getDepartment());
				notification.setPositionType(irb.getPositionType());
				notification.setPositionTitle(irb.getPositionTitle());
				save(notification);
			}
		}
	}

	/**
	 * @param existingProposal
	 * @param proposalID
	 * @param signatures
	 * @param notificationMessage
	 */
	public void notifyAllChairs(Proposal existingProposal, String proposalID,
			List<SignatureUserInfo> signatures, String notificationMessage) {
		NotificationLog notification;
		for (SignatureUserInfo userToNotify : signatures) {
			if (userToNotify.getPositionTitle().equals("Department Chair")) {
				notification = new NotificationLog();
				notification.setCritical(false);
				notification.setType("Proposal");
				notification.setAction(notificationMessage);
				notification.setProposalId(proposalID);
				notification.setProposalTitle(existingProposal.getProjectInfo()
						.getProjectTitle());
				notification.setUserProfileId(userToNotify.getUserProfileId());
				notification.setUsername(userToNotify.getUserName());
				notification.setCollege(userToNotify.getCollege());
				notification.setDepartment(userToNotify.getDepartment());
				notification.setPositionType(userToNotify.getPositionType());
				notification.setPositionTitle(userToNotify.getPositionTitle());

				save(notification);
			}
		}
	}

	/**
	 * @param existingProposal
	 * @param proposalID
	 * @param signatures
	 * @param notificationMessage
	 */
	public void notifyAllBusinessManagers(Proposal existingProposal,
			String proposalID, List<SignatureUserInfo> signatures,
			String notificationMessage) {
		NotificationLog notification;
		for (SignatureUserInfo userToNotify : signatures) {
			if (userToNotify.getPositionTitle().equals("Business Manager")) {
				notification = new NotificationLog();
				notification.setCritical(false);
				notification.setType("Proposal");
				notification.setAction(notificationMessage);
				notification.setProposalId(proposalID);
				notification.setProposalTitle(existingProposal.getProjectInfo()
						.getProjectTitle());
				notification.setUserProfileId(userToNotify.getUserProfileId());
				notification.setUsername(userToNotify.getUserName());
				notification.setCollege(userToNotify.getCollege());
				notification.setDepartment(userToNotify.getDepartment());
				notification.setPositionType(userToNotify.getPositionType());
				notification.setPositionTitle(userToNotify.getPositionTitle());

				save(notification);
			}
		}
	}

	public void NotifyAllExistingInvestigators(String proposalID,
			String projectTitle, Proposal existingProposal,
			String notificationMessage, String notificationType,
			boolean isCritical) {
		notifyAdmin(proposalID, projectTitle, notificationMessage,
				notificationType, isCritical);
		InvestigatorRefAndPosition newPI = existingProposal
				.getInvestigatorInfo().getPi();
		if (newPI.getUserProfileId() != null) {
			notifyInvestigators(proposalID, projectTitle, notificationMessage,
					notificationType, isCritical, newPI);
		}
		for (InvestigatorRefAndPosition copi : existingProposal
				.getInvestigatorInfo().getCo_pi()) {
			notifyInvestigators(proposalID, projectTitle, notificationMessage,
					notificationType, isCritical, copi);
		}
		for (InvestigatorRefAndPosition senior : existingProposal
				.getInvestigatorInfo().getSeniorPersonnel()) {
			notifyInvestigators(proposalID, projectTitle, notificationMessage,
					notificationType, isCritical, senior);
		}
		// Broadcasting SSE
		Builder eventBuilder = new Builder();
		OutboundEvent event = eventBuilder.name("notification")
				.mediaType(MediaType.TEXT_PLAIN_TYPE).data(String.class, "1")
				.build();
		NotificationService.BROADCASTER.broadcast(event);
	}

	/**
	 * @param existingProposal
	 * @param proposalID
	 * @param dean
	 */
	public void notifyDean(Proposal existingProposal, String proposalID,
			SignatureUserInfo dean) {
		NotificationLog notification;
		if (dean.getPositionTitle().equals("Dean")) {
			notification = new NotificationLog();
			notification.setCritical(true);
			notification.setType("Proposal");
			notification.setAction("Ready for Approval");
			notification.setProposalId(proposalID);
			notification.setProposalTitle(existingProposal.getProjectInfo()
					.getProjectTitle());
			notification.setUserProfileId(dean.getUserProfileId());
			notification.setUsername(dean.getUserName());
			notification.setCollege(dean.getCollege());
			notification.setDepartment(dean.getDepartment());
			notification.setPositionType(dean.getPositionType());
			notification.setPositionTitle(dean.getPositionTitle());
			save(notification);
		}
	}

	/**
	 * @param existingProposal
	 * @param proposalID
	 * @param signatures
	 */
	public void notifyProposalApprovedByBusinessManager(
			Proposal existingProposal, String proposalID,
			List<SignatureUserInfo> signatures) {
		// Reviewed by Business Manager
		existingProposal.setBusinessManagerApproval(ApprovalType.APPROVED);
		existingProposal.setDeanApproval(ApprovalType.READYFORAPPROVAL);
		existingProposal.getProposalStatus().remove(
				Status.READYFORREVIEWBYBUSINESSMANAGER);
		existingProposal.getProposalStatus().add(Status.WAITINGFORDEANAPPROVAL);
		for (SignatureUserInfo dean : signatures) {
			notifyDean(existingProposal, proposalID, dean);
		}
	}

	/**
	 * @param existingProposal
	 * @param proposalID
	 * @param researchadmin
	 */
	public void notifyResearchAdmin(Proposal existingProposal,
			String proposalID, SignatureUserInfo researchadmin) {
		NotificationLog notification;
		if (researchadmin.getPositionTitle().equals(
				"University Research Administrator")) {
			notification = new NotificationLog();
			notification.setCritical(true);
			notification.setType("Proposal");
			notification.setAction("Ready for Approval");
			notification.setProposalId(proposalID);
			notification.setProposalTitle(existingProposal.getProjectInfo()
					.getProjectTitle());
			notification.setUserProfileId(researchadmin.getUserProfileId());
			notification.setUsername(researchadmin.getUserName());
			notification.setCollege(researchadmin.getCollege());
			notification.setDepartment(researchadmin.getDepartment());
			notification.setPositionType(researchadmin.getPositionType());
			notification.setPositionTitle(researchadmin.getPositionTitle());
			save(notification);
		}
	}

	/**
	 * @param existingProposal
	 * @param proposalID
	 * @param signatures
	 * @param irbApprovalRequired
	 */
	public void notifyProposalApprovedByDean(Proposal existingProposal,
			String proposalID, List<SignatureUserInfo> signatures,
			boolean irbApprovalRequired) {
		// Approved by Dean
		existingProposal.setDeanApproval(ApprovalType.APPROVED);
		existingProposal.getProposalStatus().remove(
				Status.WAITINGFORDEANAPPROVAL);
		existingProposal.getProposalStatus().add(Status.APPROVEDBYDEAN);
		if (!irbApprovalRequired) {
			existingProposal
					.setResearchAdministratorApproval(ApprovalType.READYFORAPPROVAL);
			existingProposal.getProposalStatus().clear();
			existingProposal.getProposalStatus().add(
					Status.WAITINGFORRESEARCHADMINAPPROVAL);
			for (SignatureUserInfo researchadmin : signatures) {
				notifyResearchAdmin(existingProposal, proposalID, researchadmin);
			}
		} else {
			if (existingProposal.getIrbApproval() == ApprovalType.APPROVED) {
				existingProposal
						.setResearchAdministratorApproval(ApprovalType.READYFORAPPROVAL);
				existingProposal.getProposalStatus().clear();
				existingProposal.getProposalStatus().add(
						Status.WAITINGFORRESEARCHADMINAPPROVAL);
				for (SignatureUserInfo researchadmin : signatures) {
					notifyResearchAdmin(existingProposal, proposalID,
							researchadmin);
				}
			}
		}
	}

	/**
	 * @param existingProposal
	 * @param proposalID
	 * @param signatures
	 * @param notificationMessage
	 */
	public void notifyAllDeans(Proposal existingProposal, String proposalID,
			List<SignatureUserInfo> signatures, String notificationMessage) {
		NotificationLog notification;
		for (SignatureUserInfo userToNotify : signatures) {
			if (userToNotify.getPositionTitle().equals("Dean")) {
				notification = new NotificationLog();
				notification.setCritical(false);
				notification.setType("Proposal");
				notification.setAction(notificationMessage);
				notification.setProposalId(proposalID);
				notification.setProposalTitle(existingProposal.getProjectInfo()
						.getProjectTitle());
				notification.setUserProfileId(userToNotify.getUserProfileId());
				notification.setUsername(userToNotify.getUserName());
				notification.setCollege(userToNotify.getCollege());
				notification.setDepartment(userToNotify.getDepartment());
				notification.setPositionType(userToNotify.getPositionType());
				notification.setPositionTitle(userToNotify.getPositionTitle());

				save(notification);
			}
		}
	}

	/**
	 * @param existingProposal
	 * @param proposalID
	 * @param signatures
	 */
	public void notifyProposalApprovedByIRB(Proposal existingProposal,
			String proposalID, List<SignatureUserInfo> signatures) {
		// Approved by IRB
		existingProposal.setIrbApproval(ApprovalType.APPROVED);
		existingProposal.getProposalStatus().remove(Status.READYFORREVIEWBYIRB);
		existingProposal.getProposalStatus().add(Status.REVIEWEDBYIRB);

		if (existingProposal.getDeanApproval() == ApprovalType.APPROVED
				&& existingProposal.getBusinessManagerApproval() == ApprovalType.APPROVED) {
			existingProposal
					.setResearchAdministratorApproval(ApprovalType.READYFORAPPROVAL);

			// Proposal Status
			existingProposal.getProposalStatus().clear();
			existingProposal.getProposalStatus().add(
					Status.WAITINGFORRESEARCHADMINAPPROVAL);

			for (SignatureUserInfo researchadmin : signatures) {
				notifyResearchAdmin(existingProposal, proposalID, researchadmin);
			}
		}
	}

	/**
	 * @param existingProposal
	 * @param proposalID
	 * @param signatures
	 * @param notificationMessage
	 */
	public void notifyAllIRBs(Proposal existingProposal, String proposalID,
			List<SignatureUserInfo> signatures, String notificationMessage) {
		NotificationLog notification;
		for (SignatureUserInfo userToNotify : signatures) {
			if (userToNotify.getPositionTitle().equals("IRB")) {
				notification = new NotificationLog();
				notification.setCritical(false);
				notification.setType("Proposal");
				notification.setAction(notificationMessage);
				notification.setProposalId(proposalID);
				notification.setProposalTitle(existingProposal.getProjectInfo()
						.getProjectTitle());
				notification.setUserProfileId(userToNotify.getUserProfileId());
				notification.setUsername(userToNotify.getUserName());
				notification.setCollege(userToNotify.getCollege());
				notification.setDepartment(userToNotify.getDepartment());
				notification.setPositionType(userToNotify.getPositionType());
				notification.setPositionTitle(userToNotify.getPositionTitle());
				save(notification);
			}
		}
	}

	/**
	 * @param existingProposal
	 * @param proposalID
	 * @param signatures
	 * @param notificationMessage
	 */
	public void notifyAllResearchAdmins(Proposal existingProposal,
			String proposalID, List<SignatureUserInfo> signatures,
			String notificationMessage) {
		NotificationLog notification;
		for (SignatureUserInfo userToNotify : signatures) {
			if (userToNotify.getPositionTitle().equals(
					"University Research Administrator")) {
				notification = new NotificationLog();
				notification.setCritical(false);
				notification.setType("Proposal");
				notification.setAction(notificationMessage);
				notification.setProposalId(proposalID);
				notification.setProposalTitle(existingProposal.getProjectInfo()
						.getProjectTitle());
				notification.setUserProfileId(userToNotify.getUserProfileId());
				notification.setUsername(userToNotify.getUserName());
				notification.setCollege(userToNotify.getCollege());
				notification.setDepartment(userToNotify.getDepartment());
				notification.setPositionType(userToNotify.getPositionType());
				notification.setPositionTitle(userToNotify.getPositionTitle());
				save(notification);
			}
		}
	}

	/**
	 * @param existingProposal
	 * @param proposalID
	 * @param signatures
	 */
	public void notifyProposalApprovedByResearchAdmin(
			Proposal existingProposal, String proposalID,
			List<SignatureUserInfo> signatures) {
		NotificationLog notification;
		// Approved by University Research Administrator and Submitted to
		// University Research Director
		existingProposal
				.setResearchAdministratorApproval(ApprovalType.APPROVED);
		existingProposal
				.setResearchDirectorApproval(ApprovalType.READYFORAPPROVAL);
		existingProposal.getProposalStatus().clear();
		existingProposal.getProposalStatus().add(
				Status.WAITINGFORRESEARCHDIRECTORAPPROVAL);
		for (SignatureUserInfo researchdirector : signatures) {
			if (researchdirector.getPositionTitle().equals(
					"University Research Director")) {
				notification = new NotificationLog();
				notification.setCritical(true);
				notification.setType("Proposal");
				notification.setAction("Ready for Approval");
				notification.setProposalId(proposalID);
				notification.setProposalTitle(existingProposal.getProjectInfo()
						.getProjectTitle());
				notification.setUserProfileId(researchdirector
						.getUserProfileId());
				notification.setUsername(researchdirector.getUserName());
				notification.setCollege(researchdirector.getCollege());
				notification.setDepartment(researchdirector.getDepartment());
				notification
						.setPositionType(researchdirector.getPositionType());
				notification.setPositionTitle(researchdirector
						.getPositionTitle());
				save(notification);
			}
		}
	}

	/**
	 * @param existingProposal
	 * @param proposalID
	 * @param signatures
	 * @param notificationMessage
	 */
	public void notifyAllResearchDirectors(Proposal existingProposal,
			String proposalID, List<SignatureUserInfo> signatures,
			String notificationMessage) {
		NotificationLog notification;
		for (SignatureUserInfo userToNotify : signatures) {
			if (userToNotify.getPositionTitle().equals(
					"University Research Director")) {
				notification = new NotificationLog();
				notification.setCritical(false);
				notification.setType("Proposal");
				notification.setAction(notificationMessage);
				notification.setProposalId(proposalID);
				notification.setProposalTitle(existingProposal.getProjectInfo()
						.getProjectTitle());
				notification.setUserProfileId(userToNotify.getUserProfileId());
				notification.setUsername(userToNotify.getUserName());
				notification.setCollege(userToNotify.getCollege());
				notification.setDepartment(userToNotify.getDepartment());
				notification.setPositionType(userToNotify.getPositionType());
				notification.setPositionTitle(userToNotify.getPositionTitle());
				save(notification);
			}
		}
	}

	/**
	 * @param existingProposal
	 * @param proposalID
	 * @param signatures
	 */
	public void notifyProposalApprovedByResearchDirector(
			Proposal existingProposal, String proposalID,
			List<SignatureUserInfo> signatures) {
		NotificationLog notification;
		// Approved by University Research Director and
		// Ready for submission by University Research
		// Administrator
		existingProposal.setResearchDirectorApproval(ApprovalType.APPROVED);
		existingProposal.getProposalStatus().clear();
		existingProposal.getProposalStatus().add(Status.READYFORSUBMISSION);
		for (SignatureUserInfo researchadmin : signatures) {
			if (researchadmin.getPositionTitle().equals(
					"University Research Administrator")) {
				notification = new NotificationLog();
				notification.setCritical(true);
				notification.setType("Proposal");
				notification.setAction("Ready for Submission");
				notification.setProposalId(proposalID);
				notification.setProposalTitle(existingProposal.getProjectInfo()
						.getProjectTitle());
				notification.setUserProfileId(researchadmin.getUserProfileId());
				notification.setUsername(researchadmin.getUserName());
				notification.setCollege(researchadmin.getCollege());
				notification.setDepartment(researchadmin.getDepartment());
				notification.setPositionType(researchadmin.getPositionType());
				notification.setPositionTitle(researchadmin.getPositionTitle());
				save(notification);
			}
		}
	}
}
