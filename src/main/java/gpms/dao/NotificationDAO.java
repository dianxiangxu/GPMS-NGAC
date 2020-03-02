package gpms.dao;

import gpms.DAL.MongoDBConnector;
import gpms.model.ApprovalType;
import gpms.model.Delegation;
import gpms.model.DeleteType;
import gpms.model.GPMSCommonInfo;
import gpms.model.InvestigatorRefAndPosition;
import gpms.model.NotificationLog;
import gpms.model.PositionDetails;
import gpms.model.Proposal;
import gpms.model.RequiredSignaturesInfo;
import gpms.model.SignatureUserInfo;
import gpms.model.Status;
import gpms.model.SubmitType;
import gpms.model.UserAccount;
import gpms.model.UserProfile;
import gpms.rest.NotificationService;
import gpms.utils.EmailUtil;

import java.net.UnknownHostException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.ws.rs.core.MediaType;

import org.glassfish.jersey.media.sse.OutboundEvent;
import org.glassfish.jersey.media.sse.OutboundEvent.Builder;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.Morphia;
import org.mongodb.morphia.dao.BasicDAO;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;

import com.fasterxml.jackson.databind.JsonNode;
import com.mongodb.MongoClient;
import com.mongodb.MongoException;

public class NotificationDAO extends BasicDAO<NotificationLog, String> {
	private static final String DBNAME = "db_gpms";
	public static final String COLLECTION_NAME = "notification";
	private static Morphia morphia;
	private static Datastore ds;
	EmailUtil emailUtil;

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
		emailUtil = new EmailUtil();
	}

	public long findAllNotificationCountForAUser(GPMSCommonInfo userInfo)
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

	public List<NotificationLog> findAllNotificationInfoForAUser(
			GPMSCommonInfo userInfo) throws ParseException {
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
			notifications = getNotificationsForAdmin(notificationQuery,
					removeNotifyQuery);
		} else {
			notifications = getNotificationsForUser(userInfo,
					notificationQuery, removeNotifyQuery);
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
	private List<NotificationLog> getNotificationsForUser(
			GPMSCommonInfo userInfo, Query<NotificationLog> notificationQuery,
			Query<NotificationLog> removeNotifyQuery) {
		Datastore ds = getDatastore();
		int offset = 1;
		int limit = 10;
		List<NotificationLog> notifications = new ArrayList<NotificationLog>();
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
						userInfo.getUserProfileID()),
				notificationQuery.criteria("college").equal(
						userInfo.getUserCollege()),
				notificationQuery.criteria("department").equal(
						userInfo.getUserDepartment()),
				notificationQuery.criteria("position type").equal(
						userInfo.getUserPositionType()), notificationQuery
						.criteria("position title").equal(userPositionTitle));
		removeNotifyQuery.and(
				removeNotifyQuery.criteria("for admin").equal(false),
				removeNotifyQuery.criteria("viewed by user").equal(false),
				removeNotifyQuery.criteria("user profile id").equal(
						userInfo.getUserProfileID()),
				removeNotifyQuery.criteria("college").equal(
						userInfo.getUserCollege()),
				removeNotifyQuery.criteria("department").equal(
						userInfo.getUserDepartment()),
				removeNotifyQuery.criteria("position type").equal(
						userInfo.getUserPositionType()), removeNotifyQuery
						.criteria("position title").equal(userPositionTitle));
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
	private List<NotificationLog> getNotificationsForAdmin(
			Query<NotificationLog> notificationQuery,
			Query<NotificationLog> removeNotifyQuery) {
		Datastore ds = getDatastore();
		int offset = 1;
		int limit = 10;
		List<NotificationLog> notifications = new ArrayList<NotificationLog>();
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

	public void sendNotification(Delegation existingDelegation,
			GPMSCommonInfo userInfo, String notificationMessage,
			String notificationType, boolean isCritical) {
		String userProfileID = userInfo.getUserProfileID();
		String userName = userInfo.getUserName();
		String userCollege = userInfo.getUserCollege();
		String userDepartment = userInfo.getUserDepartment();
		String userPositionType = userInfo.getUserPositionType();
		String userPositionTitle = userInfo.getUserPositionTitle();
		saveNotificationWithoutPosition(existingDelegation,
				notificationMessage, notificationType, true, isCritical);
		// For Delegator
		saveNotificationWithPosition(notificationMessage, notificationType,
				isCritical, userProfileID, userName, userCollege,
				userDepartment, userPositionType, userPositionTitle);
		// For Delegatee
		saveNotificationWithPosition(notificationMessage, notificationType,
				isCritical, existingDelegation.getDelegateeId(),
				existingDelegation.getDelegateeUsername(),
				existingDelegation.getDelegateeCollege(),
				existingDelegation.getDelegateeDepartment(),
				existingDelegation.getDelegateePositionType(),
				userPositionTitle);

		// Broadcasting SSE
		OutboundEvent.Builder eventBuilder = new OutboundEvent.Builder();
		OutboundEvent event = eventBuilder.name("notification")
				.mediaType(MediaType.TEXT_PLAIN_TYPE).data(String.class, "1")
				.build();
	//	NotificationService.BROADCASTER.broadcast(event);
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
					+ ",<br/> Your account has been deleted to reactivate you can contact administrator: <a href='http://seal.boisestate.edu/GPMS/ContactUs.jsp' title='GPMS Contact Us' target='_blank'>Contact Us</a><br/><br/>Thank you, <br/> GPMS Team";
			emailUtil.sendMailWithoutAuth(userProfile.getWorkEmails().get(0),
					"You have been deleted " + userProfile.getFullName(),
					messageBody);
		}
		notifyAdmin(userProfile.getId().toString(), userAccount.getUserName(),
				"User", "Account is deleted.", true, true);
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
		String notificationMessage = new String();
		boolean isCritical = false;
		if (isActive) {
			notificationMessage = "Account is activated.";
			messageBody = "Hello "
					+ userProfile.getFullName()
					+ ",<br/><br/> Your account has been activated and you can login now using your credential: <a href='http://seal.boisestate.edu/GPMS/Login.jsp' title='GPMS Login' target='_blank'>Login Here</a><br/><br/>Thank you, <br/> GPMS Team";
			emailUtil.sendMailWithoutAuth(
					userProfile.getWorkEmails().get(0),
					"Successfully Activated your account "
							+ userProfile.getFullName(), messageBody);
		} else {
			notificationMessage = "Account is deactivated.";
			messageBody = "Hello "
					+ userProfile.getFullName()
					+ ",<br/> Your account has been deactivated to reactivate you can contact administrator: <a href='http://seal.boisestate.edu/GPMS/ContactUs.jsp' title='GPMS Contact Us' target='_blank'>Contact Us</a><br/><br/>Thank you, <br/> GPMS Team";
			emailUtil.sendMailWithoutAuth(userProfile.getWorkEmails().get(0),
					"You have been Deactivated " + userProfile.getFullName(),
					messageBody);
		}
		notifyAdmin(userProfile.getId().toString(), userAccount.getUserName(),
				"User", notificationMessage, true, isCritical);

		for (PositionDetails positions : userProfile.getDetails()) {
			notifyInvestigators(userProfile, "User", notificationMessage,
					isCritical, positions);
		}
		Builder eventBuilder = new Builder();
		OutboundEvent event = eventBuilder.name("notification")
				.mediaType(MediaType.TEXT_PLAIN_TYPE).data(String.class, "1")
				.build();
	//	NotificationService.BROADCASTER.broadcast(event);
	}

	/***
	 * Notify Admin Users for Proposal Modification
	 * 
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
		notification.setCritical(isCritical);
		notification.setType(notificationType);
		notification.setAction(notificationMessage);
		notification.setProposalId(proposalID);
		notification.setProposalTitle(projectTitle);
		notification.setForAdmin(true);
		save(notification);
	}

	/***
	 * Notify Admin Users for User Profile Modification
	 * 
	 * @param existingUserProfile
	 * @param action
	 * @param notificationType
	 * @param isCritical
	 */
	public void notifyAdmin(UserProfile existingUserProfile,
			String notificationType, String action, boolean isCritical) {
		NotificationLog notification = new NotificationLog();
		notification.setType(notificationType);
		notification.setAction(action);
		notification.setUserProfileId(existingUserProfile.getId().toString());
		notification.setUsername(existingUserProfile.getUserAccount()
				.getUserName());
		notification.setForAdmin(isCritical);
		save(notification);
	}

	public void notifyAdmin(String userProfileId, String userName,
			String notificationType, String action, boolean isForAdmin,
			boolean isCritical) {
		NotificationLog notification = new NotificationLog();
		notification.setType(notificationType);
		notification.setAction(action);
		notification.setUserProfileId(userProfileId);
		notification.setUsername(userName);
		notification.setForAdmin(isForAdmin);
		notification.setCritical(isCritical);
		save(notification);
	}

	private void saveNotificationWithoutPosition(Delegation existingDelegation,
			String notificationMessage, String notificationType,
			boolean isForAdmin, boolean isCritical) {
		NotificationLog notification = new NotificationLog();
		notification.setType(notificationType);
		notification.setAction(notificationMessage);
		notification.setUserProfileId(existingDelegation.getDelegateeId());
		notification.setUsername(existingDelegation.getDelegateeUsername());
		notification.setForAdmin(isForAdmin);
		notification.setCritical(isCritical);
		// notification.isViewedByUser(true);
		save(notification);
	}

	/**
	 * Notify Investigators for Proposal Modification
	 * 
	 * @param proposalID
	 * @param projectTitle
	 * @param notificationMessage
	 * @param notificationType
	 * @param isCritical
	 * @param investigator
	 */
	public void notifyInvestigators(String proposalID, String projectTitle,
			String notificationMessage, String notificationType,
			boolean isCritical, InvestigatorRefAndPosition investigator) {
		NotificationLog notification = new NotificationLog();
		notification.setCritical(isCritical);
		notification.setType(notificationType);
		notification.setAction(notificationMessage);
		notification.setProposalId(proposalID);
		notification.setProposalTitle(projectTitle);
		notification.setUserProfileId(investigator.getUserProfileId());
		notification.setUsername(investigator.getUserRef().getUserAccount()
				.getUserName());
		notification.setCollege(investigator.getCollege());
		notification.setDepartment(investigator.getDepartment());
		notification.setPositionType(investigator.getPositionType());
		notification.setPositionTitle(investigator.getPositionTitle());
		save(notification);
		//investigator.getUserRef().
		String email = investigator.getUserRef().getWorkEmails().get(0);
		//emailUtil.sendSimpleEmail(email, "GPMS-NGAC Notification", "Dear Sir/Madam,<br>GPMS-NGAC proposal : '"+projectTitle+"' has been updated.<br>Please login GPMS-NGAC for details.<br>Regards,<Br><b>GPMS-NGAC Team</b>");
	}

	public void createNotificationForAUser(String proposalID,
			String projectTitle, String notificationMessage,
			String notificationType, boolean isCritical, SignatureUserInfo user) {
		NotificationLog notification = new NotificationLog();
		notification.setCritical(isCritical);
		notification.setType(notificationType);
		notification.setAction(notificationMessage);
		notification.setProposalId(proposalID);
		notification.setProposalTitle(projectTitle);
		notification.setUserProfileId(user.getUserProfileId());
		notification.setUsername(user.getUserName());
		notification.setCollege(user.getCollege());
		notification.setDepartment(user.getDepartment());
		notification.setPositionType(user.getPositionType());
		notification.setPositionTitle(user.getPositionTitle());
		save(notification);
	}

	/***
	 * Notify Investigators for User Profile Modification
	 * 
	 * @param existingUserProfile
	 * @param notificationType
	 * @param action
	 * @param positions
	 */
	public void notifyInvestigators(UserProfile existingUserProfile,
			String notificationType, String action, boolean isCritical,
			PositionDetails positions) {
		NotificationLog notification = new NotificationLog();
		notification.setType(notificationType);
		notification.setAction(action);
		notification.setCritical(isCritical);
		notification.setUserProfileId(existingUserProfile.getId().toString());
		notification.setUsername(existingUserProfile.getUserAccount()
				.getUserName());
		notification.setCollege(positions.getCollege());
		notification.setDepartment(positions.getDepartment());
		notification.setPositionType(positions.getPositionType());
		notification.setPositionTitle(positions.getPositionTitle());
		save(notification);
	}

	private void saveNotificationWithPosition(String notificationMessage,
			String notificationType, boolean isCritical, String userProfileID,
			String userName, String userCollege, String userDepartment,
			String userPositionType, String userPositionTitle) {
		NotificationLog notification = new NotificationLog();
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
	}

	public void notifyAllExistingInvestigators(String proposalID,
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
		//NotificationService.BROADCASTER.broadcast(event);
	}

	/**
	 * @param existingProposal
	 * @param proposalID
	 * @param signatures
	 */
	public void notifyChairProposalSubmittedByPI(Proposal existingProposal,
			String proposalID, List<SignatureUserInfo> signatures) {
		existingProposal.setDateSubmitted(new Date());
		existingProposal.setSubmittedByPI(SubmitType.SUBMITTED);
		existingProposal.setChairApproval(ApprovalType.READYFORAPPROVAL);
		existingProposal.getProposalStatus().clear();
		existingProposal.getProposalStatus()
				.add(Status.WAITINGFORCHAIRAPPROVAL);
		for (SignatureUserInfo chair : signatures) {
			if (chair.getPositionTitle().equals("Department Chair")) {
				createNotificationForAUser(proposalID, existingProposal
						.getProjectInfo().getProjectTitle(),
						"Ready for Approval.", "Proposal", true, chair);
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
		for (SignatureUserInfo userToNotify : signatures) {
			if (userToNotify.getPositionTitle().equals("Department Chair")) {
				createNotificationForAUser(proposalID, existingProposal
						.getProjectInfo().getProjectTitle(),
						notificationMessage, "Proposal", false, userToNotify);
			}
		}
	}

	/**
	 * @param existingProposal
	 * @param proposalID
	 * @param signatures
	 * @param irbApprovalRequired
	 */
	public void notifyProposalApprovedByChair(Proposal existingProposal,
			String proposalID, List<SignatureUserInfo> signatures,
			boolean irbApprovalRequired) {
		existingProposal.setChairApproval(ApprovalType.APPROVED);
		existingProposal
				.setBusinessManagerApproval(ApprovalType.READYFORAPPROVAL);
		existingProposal.getProposalStatus().clear();
		existingProposal.getProposalStatus().add(
				Status.READYFORREVIEWBYBUSINESSMANAGER);
		for (SignatureUserInfo businessManager : signatures) {
			notifyBusinessManager(existingProposal, proposalID, businessManager);
		}

		if (irbApprovalRequired) {
			notifyProposalApprovedByChairToIRB(existingProposal, proposalID,
					signatures);
		}
	}

	/**
	 * @param existingProposal
	 * @param proposalID
	 * @param signatures
	 */
	public void notifyProposalApprovedByChairToIRB(Proposal existingProposal,
			String proposalID, List<SignatureUserInfo> signatures) {
		existingProposal.setIrbApproval(ApprovalType.READYFORAPPROVAL);
		existingProposal.getProposalStatus().add(Status.READYFORREVIEWBYIRB);
		for (SignatureUserInfo userToNotify : signatures) {
			if (userToNotify.getPositionTitle().equals("IRB")) {
				createNotificationForAUser(proposalID, existingProposal
						.getProjectInfo().getProjectTitle(),
						"Ready for Reviewal.", "Proposal", true, userToNotify);
			}
		}
	}

	/**
	 * @param existingProposal
	 * @param proposalID
	 * @param signatures
	 * @param notificationMessage
	 * @param isCritical
	 * @param notificationType
	 * @param coPICount
	 */
	public void notifyReturnedByChair(Proposal existingProposal,
			String proposalID, List<SignatureUserInfo> signatures,
			String notificationMessage, boolean isCritical,
			final String notificationType, int coPICount) {

		// Returned by Chair
		existingProposal.setChairApproval(ApprovalType.DISAPPROVED);
		existingProposal.setSubmittedByPI(SubmitType.NOTSUBMITTED);
		if (coPICount > 0) {
			existingProposal.setReadyForSubmissionByPI(false);
		}
		existingProposal.getSignatureInfo().clear();
		// Proposal Status
		existingProposal.getProposalStatus().clear();
		existingProposal.getProposalStatus().add(Status.RETURNEDBYCHAIR);
		for (SignatureUserInfo userToNotify : signatures) {
			if (userToNotify.getPositionTitle().equals("Department Chair")) {
				createNotificationForAUser(proposalID, existingProposal
						.getProjectInfo().getProjectTitle(),
						notificationMessage, notificationType, isCritical,
						userToNotify);
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
		for (SignatureUserInfo userToNotify : signatures) {
			if (userToNotify.getPositionTitle().equals("Business Manager")) {
				createNotificationForAUser(proposalID, existingProposal
						.getProjectInfo().getProjectTitle(),
						notificationMessage, "Proposal", false, userToNotify);
			}
		}
	}

	/***
	 * 
	 * @param existingProposal
	 * @param proposalID
	 * @param businessManager
	 */
	public void notifyBusinessManager(Proposal existingProposal,
			String proposalID, SignatureUserInfo businessManager) {
		if (businessManager.getPositionTitle().equals("Business Manager")) {
			createNotificationForAUser(proposalID, existingProposal
					.getProjectInfo().getProjectTitle(), "Ready for Reviewal.",
					"Proposal", true, businessManager);
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
		for (SignatureUserInfo userToNotify : signatures) {
			if (userToNotify.getPositionTitle().equals("IRB")) {
				createNotificationForAUser(proposalID, existingProposal
						.getProjectInfo().getProjectTitle(),
						notificationMessage, "Proposal", false, userToNotify);
			}
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
			if (dean.getPositionTitle().equals("Dean")) {
				createNotificationForAUser(proposalID, existingProposal
						.getProjectInfo().getProjectTitle(),
						"Ready for Approval.", "Proposal", true, dean);
			}
		}
	}

	/**
	 * @param existingProposal
	 * @param proposalID
	 * @param signatures
	 * @param notificationMessage
	 * @param isCritical
	 * @param notificationType
	 * @param coPICount
	 */
	public void notifyDisapprovedByBusinessManager(Proposal existingProposal,
			String proposalID, List<SignatureUserInfo> signatures,
			String notificationMessage, boolean isCritical,
			final String notificationType, int coPICount) {
		// Disapproved by Business Manager
		existingProposal.setBusinessManagerApproval(ApprovalType.DISAPPROVED);
		existingProposal.setSubmittedByPI(SubmitType.NOTSUBMITTED);
		existingProposal.setIrbApproval(ApprovalType.NOTREADYFORAPPROVAL);
		if (coPICount > 0) {
			existingProposal.setReadyForSubmissionByPI(false);
		}
		existingProposal.getSignatureInfo().clear();
		// Proposal Status
		existingProposal.getProposalStatus().clear();
		existingProposal.getProposalStatus().add(
				Status.DISAPPROVEDBYBUSINESSMANAGER);
		for (SignatureUserInfo userToNotify : signatures) {
			if (userToNotify.getPositionTitle().equals("Business Manager")
					|| userToNotify.getPositionTitle().equals(
							"Department Chair")) {
				createNotificationForAUser(proposalID, existingProposal
						.getProjectInfo().getProjectTitle(),
						notificationMessage, notificationType, isCritical,
						userToNotify);
			} else if (userToNotify.getPositionTitle().equals("IRB")) {
				if (existingProposal.getIrbApproval() == ApprovalType.APPROVED) {
					createNotificationForAUser(proposalID, existingProposal
							.getProjectInfo().getProjectTitle(),
							notificationMessage, notificationType, isCritical,
							userToNotify);
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
		for (SignatureUserInfo userToNotify : signatures) {
			if (userToNotify.getPositionTitle().equals("Dean")) {
				createNotificationForAUser(proposalID, existingProposal
						.getProjectInfo().getProjectTitle(),
						notificationMessage, "Proposal", false, userToNotify);
			}
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
				notifyProposalApprovedByIRBAndDean(existingProposal,
						proposalID, researchadmin);
			}
		} else {
			if (existingProposal.getIrbApproval() == ApprovalType.APPROVED) {
				existingProposal
						.setResearchAdministratorApproval(ApprovalType.READYFORAPPROVAL);
				existingProposal.getProposalStatus().clear();
				existingProposal.getProposalStatus().add(
						Status.WAITINGFORRESEARCHADMINAPPROVAL);
				for (SignatureUserInfo researchadmin : signatures) {
					notifyProposalApprovedByIRBAndDean(existingProposal,
							proposalID, researchadmin);
				}
			}
		}
	}

	/**
	 * @param existingProposal
	 * @param proposalID
	 * @param signatures
	 * @param notificationMessage
	 * @param isCritical
	 * @param notificationType
	 * @param coPICount
	 */
	public void notifyReturnedByDean(Proposal existingProposal,
			String proposalID, List<SignatureUserInfo> signatures,
			String notificationMessage, boolean isCritical,
			final String notificationType, int coPICount) {
		// Returned by Dean
		existingProposal.setDeanApproval(ApprovalType.DISAPPROVED);
		existingProposal.setSubmittedByPI(SubmitType.NOTSUBMITTED);
		if (coPICount > 0) {
			existingProposal.setReadyForSubmissionByPI(false);
		}
		existingProposal.getSignatureInfo().clear();
		existingProposal.getProposalStatus().clear();
		existingProposal.getProposalStatus().add(Status.RETURNEDBYDEAN);
		for (SignatureUserInfo userToNotify : signatures) {
			if (userToNotify.getPositionTitle().equals("Dean")
					|| userToNotify.getPositionTitle().equals(
							"Business Manager")
					|| userToNotify.getPositionTitle().equals(
							"Department Chair")) {
				createNotificationForAUser(proposalID, existingProposal
						.getProjectInfo().getProjectTitle(),
						notificationMessage, notificationType, isCritical,
						userToNotify);
			} else if (userToNotify.getPositionTitle().equals("IRB")) {
				if (existingProposal.getIrbApproval() == ApprovalType.APPROVED) {
					createNotificationForAUser(proposalID, existingProposal
							.getProjectInfo().getProjectTitle(),
							notificationMessage, notificationType, isCritical,
							userToNotify);
				}
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
				notifyProposalApprovedByIRBAndDean(existingProposal,
						proposalID, researchadmin);
			}
		}
	}

	/**
	 * @param existingProposal
	 * @param proposalID
	 * @param signatures
	 * @param notificationMessage
	 * @param isCritical
	 * @param notificationType
	 * @param coPICount
	 */
	public void notifyDisapprovedByIRB(Proposal existingProposal,
			String proposalID, List<SignatureUserInfo> signatures,
			String notificationMessage, boolean isCritical,
			final String notificationType, int coPICount) {
		// Disapproved by IRB
		existingProposal.setIrbApproval(ApprovalType.DISAPPROVED);
		existingProposal.setSubmittedByPI(SubmitType.NOTSUBMITTED);
		if (coPICount > 0) {
			existingProposal.setReadyForSubmissionByPI(false);
		}
		existingProposal.getSignatureInfo().clear();
		// Proposal Status
		existingProposal.getProposalStatus().clear();
		existingProposal.getProposalStatus().add(Status.DISAPPROVEDBYIRB);

		for (SignatureUserInfo userToNotify : signatures) {
			if (userToNotify.getPositionTitle().equals("IRB")
					|| userToNotify.getPositionTitle().equals(
							"Department Chair")) {
				createNotificationForAUser(proposalID, existingProposal
						.getProjectInfo().getProjectTitle(),
						notificationMessage, notificationType, isCritical,
						userToNotify);
			} else if (userToNotify.getPositionTitle().equals(
					"Business Manager")) {
				if (existingProposal.getBusinessManagerApproval() == ApprovalType.APPROVED) {
					createNotificationForAUser(proposalID, existingProposal
							.getProjectInfo().getProjectTitle(),
							notificationMessage, notificationType, isCritical,
							userToNotify);
				}
			} else if (userToNotify.getPositionTitle().equals("Dean")) {
				if (existingProposal.getDeanApproval() == ApprovalType.APPROVED) {
					createNotificationForAUser(proposalID, existingProposal
							.getProjectInfo().getProjectTitle(),
							notificationMessage, notificationType, isCritical,
							userToNotify);
				}
			}
		}
	}

	/**
	 * @param existingProposal
	 * @param proposalID
	 * @param researchadmin
	 */
	public void notifyProposalApprovedByIRBAndDean(Proposal existingProposal,
			String proposalID, SignatureUserInfo researchadmin) {
		if (researchadmin.getPositionTitle().equals(
				"University Research Administrator")) {
			createNotificationForAUser(proposalID, existingProposal
					.getProjectInfo().getProjectTitle(), "Ready for Approval.",
					"Proposal", true, researchadmin);
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
		for (SignatureUserInfo userToNotify : signatures) {
			if (userToNotify.getPositionTitle().equals(
					"University Research Administrator")) {
				createNotificationForAUser(proposalID, existingProposal
						.getProjectInfo().getProjectTitle(),
						notificationMessage, "Proposal", false, userToNotify);
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
				createNotificationForAUser(proposalID, existingProposal
						.getProjectInfo().getProjectTitle(),
						"Ready for Approval.", "Proposal", true,
						researchdirector);
			}
		}
	}

	/**
	 * @param existingProposal
	 * @param proposalID
	 * @param signatures
	 * @param irbApprovalRequired
	 * @param notificationMessage
	 * @param isCritical
	 * @param notificationType
	 * @param coPICount
	 */
	public void notifyDisapprovedByResearchAdmin(Proposal existingProposal,
			String proposalID, List<SignatureUserInfo> signatures,
			boolean irbApprovalRequired, String notificationMessage,
			boolean isCritical, final String notificationType, int coPICount) {
		// Disapproved by Research
		// Administrator
		existingProposal
				.setResearchAdministratorApproval(ApprovalType.DISAPPROVED);
		existingProposal.setSubmittedByPI(SubmitType.NOTSUBMITTED);
		if (coPICount > 0) {
			existingProposal.setReadyForSubmissionByPI(false);
		}
		existingProposal.getSignatureInfo().clear();
		// Proposal Status
		existingProposal.getProposalStatus().clear();
		existingProposal.getProposalStatus().add(
				Status.DISAPPROVEDBYRESEARCHADMIN);
		for (SignatureUserInfo userToNotify : signatures) {
			if (userToNotify.getPositionTitle().equals(
					"University Research Administrator")
					|| userToNotify.getPositionTitle().equals("Dean")
					|| userToNotify.getPositionTitle().equals(
							"Business Manager")
					|| userToNotify.getPositionTitle().equals(
							"Department Chair")) {
				createNotificationForAUser(proposalID, existingProposal
						.getProjectInfo().getProjectTitle(),
						notificationMessage, notificationType, isCritical,
						userToNotify);
			} else if (userToNotify.getPositionTitle().equals("IRB")) {
				if (irbApprovalRequired) {
					createNotificationForAUser(proposalID, existingProposal
							.getProjectInfo().getProjectTitle(),
							notificationMessage, notificationType, isCritical,
							userToNotify);
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
	public void notifyAllResearchDirectors(Proposal existingProposal,
			String proposalID, List<SignatureUserInfo> signatures,
			String notificationMessage) {
		for (SignatureUserInfo userToNotify : signatures) {
			if (userToNotify.getPositionTitle().equals(
					"University Research Director")) {
				createNotificationForAUser(proposalID, existingProposal
						.getProjectInfo().getProjectTitle(),
						notificationMessage, "Proposal", false, userToNotify);
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
		// Approved by University Research Director and
		// Ready for submission by University Research
		// Administrator
		existingProposal.setResearchDirectorApproval(ApprovalType.APPROVED);
		existingProposal.getProposalStatus().clear();
		existingProposal.getProposalStatus().add(Status.READYFORSUBMISSION);
		for (SignatureUserInfo researchadmin : signatures) {
			if (researchadmin.getPositionTitle().equals(
					"University Research Administrator")) {
				createNotificationForAUser(proposalID, existingProposal
						.getProjectInfo().getProjectTitle(),
						"Ready for Submission.", "Proposal", true,
						researchadmin);
			}
		}
	}

	/**
	 * @param existingProposal
	 * @param proposalID
	 * @param signatures
	 * @param irbApprovalRequired
	 * @param notificationMessage
	 * @param isCritical
	 * @param notificationType
	 * @param coPICount
	 */
	public void notifyDisapprovedByDirector(Proposal existingProposal,
			String proposalID, List<SignatureUserInfo> signatures,
			boolean irbApprovalRequired, String notificationMessage,
			boolean isCritical, final String notificationType, int coPICount) {
		// Disapproved by University
		// Research
		// Director
		existingProposal.setResearchDirectorApproval(ApprovalType.DISAPPROVED);
		existingProposal.setSubmittedByPI(SubmitType.NOTSUBMITTED);
		if (coPICount > 0) {
			existingProposal.setReadyForSubmissionByPI(false);
		}
		existingProposal.getSignatureInfo().clear();
		// Proposal Status
		existingProposal.getProposalStatus().clear();
		existingProposal.getProposalStatus().add(
				Status.DISAPPROVEDBYRESEARCHDIRECTOR);
		for (SignatureUserInfo userToNotify : signatures) {
			if (userToNotify.getPositionTitle().equals(
					"University Research Director")
					|| userToNotify.getPositionTitle().equals(
							"University Research Administrator")
					|| userToNotify.getPositionTitle().equals("Dean")
					|| userToNotify.getPositionTitle().equals(
							"Business Manager")
					|| userToNotify.getPositionTitle().equals(
							"Department Chair")) {
				createNotificationForAUser(proposalID, existingProposal
						.getProjectInfo().getProjectTitle(),
						notificationMessage, notificationType, isCritical,
						userToNotify);
			} else if (userToNotify.getPositionTitle().equals("IRB")) {
				if (irbApprovalRequired) {
					createNotificationForAUser(proposalID, existingProposal
							.getProjectInfo().getProjectTitle(),
							notificationMessage, notificationType, isCritical,
							userToNotify);
				}
			}
		}
	}

	/**
	 * @param existingProposal
	 * @param proposalID
	 * @param signatures
	 */
	public void notifyProposalSubmittedByAdmin(Proposal existingProposal,
			String proposalID, List<SignatureUserInfo> signatures) {
		existingProposal
				.setResearchAdministratorSubmission(SubmitType.SUBMITTED);
		existingProposal.getProposalStatus().clear();
		existingProposal.getProposalStatus().add(
				Status.SUBMITTEDBYRESEARCHADMIN);
		for (SignatureUserInfo user : signatures) {
			createNotificationForAUser(proposalID, existingProposal
					.getProjectInfo().getProjectTitle(),
					"The Proposal is Submitted.", "Proposal", true, user);
		}
	}

	/**
	 * 
	 * @param existingProposal
	 * @param proposalID
	 * @param signatures
	 * @param irbApprovalRequired
	 * @param signByAllUsersInfo
	 * @param authorUserName
	 * @param proposalUserTitle
	 * @param notificationMessage
	 * @param currentProposalRoles
	 * @return
	 */
	public String updateForProposalApprove(Proposal existingProposal,
			String proposalID, List<SignatureUserInfo> signatures,
			boolean irbApprovalRequired,
			RequiredSignaturesInfo signByAllUsersInfo, String authorUserName,
			JsonNode proposalUserTitle, String notificationMessage,
			List<String> currentProposalRoles) {
		if (!proposalID.equals("0") && currentProposalRoles != null) {
			notificationMessage = "Approved" + " by " + authorUserName + ".";
			if (existingProposal.getChairApproval() == ApprovalType.READYFORAPPROVAL
					&& (proposalUserTitle.textValue()
							.equals("Department Chair") || proposalUserTitle
							.textValue().equals("Associate Chair"))) {
				if (signByAllUsersInfo.isSignedByAllChairs()) {
					notifyProposalApprovedByChair(existingProposal, proposalID,
							signatures, irbApprovalRequired);
				}
				notifyAllChairs(existingProposal, proposalID, signatures,
						notificationMessage);
			} else if (existingProposal.getBusinessManagerApproval() == ApprovalType.READYFORAPPROVAL
					&& (proposalUserTitle.textValue()
							.equals("Business Manager") || proposalUserTitle
							.textValue().equals(
									"Department Administrative Assistant"))) {
				if (signByAllUsersInfo.isSignedByAllBusinessManagers()) {
					notifyProposalApprovedByBusinessManager(existingProposal,
							proposalID, signatures);
				}
				notifyAllBusinessManagers(existingProposal, proposalID,
						signatures, notificationMessage);
			} else if (existingProposal.getDeanApproval() == ApprovalType.READYFORAPPROVAL
					&& (proposalUserTitle.textValue().equals("Dean") || proposalUserTitle
							.textValue().equals("Associate Dean"))) {
				if (signByAllUsersInfo.isSignedByAllDeans()) {
					notifyProposalApprovedByDean(existingProposal, proposalID,
							signatures, irbApprovalRequired);
				}
				notifyAllDeans(existingProposal, proposalID, signatures,
						notificationMessage);
			} else if (existingProposal.getIrbApproval() == ApprovalType.READYFORAPPROVAL
					&& proposalUserTitle.textValue().equals("IRB")
					&& irbApprovalRequired) {
				if (signByAllUsersInfo.isSignedByAllIRBs()) {
					notifyProposalApprovedByIRB(existingProposal, proposalID,
							signatures);
				}
				notifyAllIRBs(existingProposal, proposalID, signatures,
						notificationMessage);
			} else if (existingProposal.getResearchAdministratorApproval() == ApprovalType.READYFORAPPROVAL
					&& proposalUserTitle.textValue().equals(
							"University Research Administrator")) {
				if (signByAllUsersInfo.isSignedByAllResearchAdmins()) {
					notifyProposalApprovedByResearchAdmin(existingProposal,
							proposalID, signatures);
				}
				notifyAllResearchAdmins(existingProposal, proposalID,
						signatures, notificationMessage);
			} else if (existingProposal.getResearchDirectorApproval() == ApprovalType.READYFORAPPROVAL
					&& proposalUserTitle.textValue().equals(
							"University Research Director")) {
				if (signByAllUsersInfo.isSignedByAllResearchDirectors()) {
					notifyProposalApprovedByResearchDirector(existingProposal,
							proposalID, signatures);
				}
				notifyAllResearchDirectors(existingProposal, proposalID,
						signatures, notificationMessage);
			}
		}
		return notificationMessage;
	}

	/**
	 * @param existingProposal
	 * @param proposalID
	 * @param signatures
	 * @param signByAllUsersInfo
	 * @param authorUserName
	 * @param proposalUserTitle
	 * @param currentProposalRoles
	 * @return
	 */
	public String updateForProposalSubmit(Proposal existingProposal,
			String proposalID, List<SignatureUserInfo> signatures,
			RequiredSignaturesInfo signByAllUsersInfo, String authorUserName,
			JsonNode proposalUserTitle, List<String> currentProposalRoles) {
		String notificationMessage;
		if (!proposalID.equals("0") && currentProposalRoles != null) {
			if (existingProposal.getSubmittedByPI() == SubmitType.NOTSUBMITTED
					&& existingProposal.isReadyForSubmissionByPI()
					&& existingProposal.getDeletedByPI() == DeleteType.NOTDELETED
					&& currentProposalRoles.contains("PI")
					&& !proposalUserTitle.textValue().equals(
							"University Research Administrator")) {
				if (signByAllUsersInfo.isSignedByPI()
						&& signByAllUsersInfo.isSignedByAllCoPIs()) {
					notifyChairProposalSubmittedByPI(existingProposal,
							proposalID, signatures);
				} else {
					existingProposal.setReadyForSubmissionByPI(false);
					existingProposal.getProposalStatus().clear();
					existingProposal.getProposalStatus().add(
							Status.NOTSUBMITTEDBYPI);
				}
			} else if (existingProposal.getResearchAdministratorSubmission() == SubmitType.NOTSUBMITTED
					&& existingProposal.getResearchDirectorApproval() == ApprovalType.APPROVED
					&& !currentProposalRoles.contains("PI")
					&& proposalUserTitle.textValue().equals(
							"University Research Administrator")) {
				notifyProposalSubmittedByAdmin(existingProposal, proposalID,
						signatures);
			}
		}
		notificationMessage = "Submitted" + " by " + authorUserName + ".";
		return notificationMessage;
	}
}
