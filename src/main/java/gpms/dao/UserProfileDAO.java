package gpms.dao;

import gpms.DAL.MongoDBConnector;
import gpms.model.AuditLog;
import gpms.model.AuditLogInfo;
import gpms.model.Delegation;
import gpms.model.GPMSCommonInfo;
import gpms.model.InvestigatorUsersAndPositions;
import gpms.model.PositionDetails;
import gpms.model.Proposal;
import gpms.model.UserAccount;
import gpms.model.UserDetail;
import gpms.model.UserInfo;
import gpms.model.UserProfile;
import gpms.model.UserProposalCount;
import gpms.utils.EmailUtil;

import java.net.UnknownHostException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.bson.types.ObjectId;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.Morphia;
import org.mongodb.morphia.dao.BasicDAO;
import org.mongodb.morphia.query.Query;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.mongodb.MongoClient;
import com.mongodb.MongoException;

public class UserProfileDAO extends BasicDAO<UserProfile, String> {
	private static final String DBNAME = "db_gpms";
	public static final String COLLECTION_NAME = "userprofile";

	private static Morphia morphia;
	private static Datastore ds;
	private AuditLog audit = new AuditLog();

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

	public UserProfileDAO(MongoClient mongo, Morphia morphia, String dbName) {
		super(mongo, morphia, dbName);
	}

	/**
	 * 
	 * @return list of all users in the ds
	 * @throws UnknownHostException
	 */

	/**
	 * 
	 * @return list of all users with position details to get as PI, Co-PI and
	 *         Senior
	 * @throws UnknownHostException
	 */
	public List<UserProfile> findAllUserForInvestigator()
			throws UnknownHostException {
		Datastore ds = getDatastore();
		Query<UserProfile> profileQuery = ds.createQuery(UserProfile.class);
		profileQuery
				.and(profileQuery.criteria("details").notEqual(null),
						profileQuery
								.or(profileQuery.criteria(
										"details.position type")
										.equalIgnoreCase(
												"Tenured/tenure-track faculty"),
										profileQuery
												.criteria(
														"details.position type")
												.equalIgnoreCase(
														"Non-tenure-track research faculty"),
										profileQuery.criteria(
												"details.position type")
												.equalIgnoreCase(
														"Teaching faculty")));
		return profileQuery.asList();
	}

	public List<UserProfile> findAll() throws UnknownHostException {
		Datastore ds = getDatastore();
		return ds.createQuery(UserProfile.class).asList();
	}

	public List<UserProfile> findAllUsersWithPosition()
			throws UnknownHostException {
		Datastore ds = getDatastore();
		Query<UserProfile> profileQuery = ds.createQuery(UserProfile.class);
		profileQuery
				.and(profileQuery.criteria("deleted").equal(false),
						profileQuery.criteria("details").notEqual(null),
						profileQuery
								.or(profileQuery.criteria(
										"details.position type")
										.equalIgnoreCase(
												"Tenured/tenure-track faculty"),
										profileQuery
												.criteria(
														"details.position type")
												.equalIgnoreCase(
														"Non-tenure-track research faculty"),
										profileQuery.criteria(
												"details.position type")
												.equalIgnoreCase(
														"Teaching faculty")));
		return profileQuery.retrievedFields(true, "_id", "first name",
				"middle name", "last name").asList();
	}

	public List<UserProfile> findAllActiveUsers() throws UnknownHostException {
		Datastore ds = getDatastore();

		Query<UserProfile> profileQuery = ds.createQuery(UserProfile.class);
		Query<UserAccount> accountQuery = ds.createQuery(UserAccount.class);

		accountQuery.and(accountQuery.criteria("deleted").equal(false),
				accountQuery.criteria("active").equal(true));
		profileQuery.and(
				profileQuery.criteria("details").notEqual(null),
				profileQuery.and(profileQuery.criteria("user id").in(
						accountQuery.asKeyList())),
				profileQuery.criteria("deleted").equal(false));

		return profileQuery.retrievedFields(true, "_id", "first name",
				"middle name", "last name").asList();
	}

	/***
	 * Finds All records For User Grid
	 * 
	 * @param offset
	 * @param limit
	 * @param userInfo
	 * @return
	 * @throws UnknownHostException
	 */
	public List<UserInfo> findAllForUserGrid(int offset, int limit,
			GPMSCommonInfo userInfo) throws UnknownHostException {
		String userName = userInfo.getUserName();
		String college = userInfo.getUserCollege();
		String department = userInfo.getUserDepartment();
		String positionType = userInfo.getUserPositionType();
		String positionTitle = userInfo.getUserPositionTitle();
		Boolean isActive = userInfo.getUserIsActive();
		Datastore ds = getDatastore();
		List<UserInfo> users = new ArrayList<UserInfo>();
		Query<UserProfile> profileQuery = ds.createQuery(UserProfile.class);
		Query<UserAccount> accountQuery = ds.createQuery(UserAccount.class);
		if (userName != null) {
			accountQuery.criteria("username").containsIgnoreCase(userName);
		}
		if (isActive != null) {
			accountQuery.criteria("active").equal(isActive);
		}

		profileQuery.criteria("user id").in(accountQuery.asKeyList());

		if (college != null) {
			profileQuery.criteria("details.college").equal(college);
		}
		if (department != null) {
			profileQuery.criteria("details.department").equal(department);
		}
		if (positionType != null) {
			profileQuery.criteria("details.position type").equal(positionType);
		} else {
			List<String> positionTypes = new ArrayList<String>();
			positionTypes.add("University administrator");
			profileQuery.criteria("details.position type").hasNoneOf(
					positionTypes);
		}
		if (positionTitle != null) {
			profileQuery.criteria("details.position title")
					.equal(positionTitle);
		} else {
			List<String> positionTitles = new ArrayList<String>();
			positionTitles.add("IRB");
			positionTitles.add("University Research Administrator");
			positionTitles.add("University Research Director");

			profileQuery.criteria("details.position title").hasNoneOf(
					positionTitles);
		}
		int rowTotal = profileQuery.asList().size();
		List<UserProfile> userProfiles = profileQuery.offset(offset - 1)
				.limit(limit).order("-audit log.activity on").asList();
		for (UserProfile userProfile : userProfiles) {
			UserInfo user = generateUserInfo(rowTotal, userProfile);
			users.add(user);
		}
		return users;
	}

	public List<UserInfo> findAllForAdminUserGrid(int offset, int limit,
			String userName, String positionTitle, Boolean isActive) {
		Datastore ds = getDatastore();
		List<UserInfo> users = new ArrayList<UserInfo>();
		Query<UserProfile> profileQuery = ds.createQuery(UserProfile.class);
		Query<UserAccount> accountQuery = ds.createQuery(UserAccount.class);
		if (userName != null) {
			accountQuery.criteria("username").containsIgnoreCase(userName);
		}
		if (isActive != null) {
			accountQuery.criteria("active").equal(isActive);
		}
		profileQuery.criteria("user id").in(accountQuery.asKeyList());
		List<String> positionTypes = new ArrayList<String>();
		positionTypes.add("University administrator");
		profileQuery.criteria("details.position type").in(positionTypes);
		if (positionTitle != null) {
			profileQuery.criteria("details.position title")
					.equal(positionTitle);
		} else {
			List<String> positionTitles = new ArrayList<String>();
			positionTitles.add("IRB");
			positionTitles.add("University Research Administrator");
			positionTitles.add("University Research Director");
			profileQuery.criteria("details.position title").in(positionTitles);
		}
		int rowTotal = profileQuery.asList().size();
		List<UserProfile> userProfiles = profileQuery.offset(offset - 1)
				.limit(limit).order("-audit log.activity on").asList();
		for (UserProfile userProfile : userProfiles) {
			UserInfo user = generateUserInfo(rowTotal, userProfile);
			users.add(user);
		}
		return users;
	}

	public List<UserInfo> findAllUsers(String userName, String college,
			String department, String positionType, String positionTitle,
			Boolean isActive) throws UnknownHostException {
		Datastore ds = getDatastore();
		List<UserInfo> users = new ArrayList<UserInfo>();
		Query<UserProfile> profileQuery = ds.createQuery(UserProfile.class);
		Query<UserAccount> accountQuery = ds.createQuery(UserAccount.class);
		if (userName != null) {
			accountQuery.criteria("username").containsIgnoreCase(userName);
		}
		if (isActive != null) {
			accountQuery.criteria("active").equal(isActive);
		}
		profileQuery.criteria("user id").in(accountQuery.asKeyList());
		if (college != null) {
			profileQuery.criteria("details.college").equal(college);
		}
		if (department != null) {
			profileQuery.criteria("details.department").equal(department);
		}
		if (positionType != null) {
			profileQuery.criteria("details.position type").equal(positionType);
		}
		if (positionTitle != null) {
			profileQuery.criteria("details.position title")
					.equal(positionTitle);
		}
		List<UserProfile> userProfiles = profileQuery.order(
				"-audit log.activity on").asList();
		for (UserProfile userProfile : userProfiles) {
			UserInfo user = generateUserInfo(0, userProfile);
			users.add(user);
		}
		return users;
	}

	public List<UserDetail> findAllUsersForDelegation(ObjectId id,
			String userCollege, String userDepartment)
			throws UnknownHostException {
		Datastore ds = getDatastore();
		List<UserDetail> users = new ArrayList<UserDetail>();
		List<String> positionTypes = new ArrayList<String>();
		positionTypes.add("Administrator");
		positionTypes.add("Professional staff");
		Query<UserProfile> profileQuery = ds.createQuery(UserProfile.class)
				.retrievedFields(true, "_id", "first name", "middle name",
						"last name", "work email", "details", "user id");
		profileQuery.and(
				profileQuery.criteria("_id").notEqual(id),
				profileQuery.criteria("deleted").equal(false),
				profileQuery.criteria("details.college").contains(userCollege),
				profileQuery.criteria("details.department").contains(
						userDepartment),
				profileQuery.criteria("details.position type")
						.in(positionTypes));
		List<UserProfile> userProfiles = profileQuery.asList();
		for (UserProfile userProfile : userProfiles) {
			for (PositionDetails userPos : userProfile.getDetails()) {
				if (!isAlreadyDelegatee(userProfile.getId().toString(), userPos)) {
					UserDetail userDetail = getDelegationUserDetails(
							userProfile, userPos);
					users.add(userDetail);
				}
			}
		}
		return users;
	}

	/**
	 * Gets Delegation User Details
	 * 
	 * @param userProfile
	 * @param userPos
	 * @return
	 */
	private UserDetail getDelegationUserDetails(UserProfile userProfile,
			PositionDetails userPos) {
		UserDetail userDetail = new UserDetail();
		userDetail.setUserProfileId(userProfile.getId().toString());
		userDetail.setFullName(userProfile.getFullName());
		userDetail.setUserName(userProfile.getUserAccount().getUserName());
		userDetail.setEmail(userProfile.getWorkEmails().get(0));
		userDetail.setCollege(userPos.getCollege());
		userDetail.setDepartment(userPos.getDepartment());
		userDetail.setPositionType(userPos.getPositionType());
		userDetail.setPositionTitle(userPos.getPositionTitle());
		return userDetail;
	}

	private boolean isAlreadyDelegatee(String delegateeId,
			PositionDetails posDetails) {
		long delegationCount = ds.createQuery(Delegation.class)
				.field("revoked").equal(false).field("delegatee user id")
				.equal(delegateeId).field("delegatee college")
				.equal(posDetails.getCollege()).field("delegatee department")
				.equal(posDetails.getDepartment())
				.field("delegatee position type")
				.equal(posDetails.getPositionType())
				.field("delegatee position title")
				.equal(posDetails.getPositionTitle()).countAll();
		if (delegationCount != 0) {
			return true;
		}
		return false;
	}

	public List<UserInfo> findAllAdminUsers(String userName, String college,
			String department, String positionType, String positionTitle,
			Boolean isActive) throws UnknownHostException {
		Datastore ds = getDatastore();
		List<UserInfo> users = new ArrayList<UserInfo>();
		Query<UserProfile> profileQuery = ds.createQuery(UserProfile.class);
		Query<UserAccount> accountQuery = ds.createQuery(UserAccount.class);
		if (userName != null) {
			accountQuery.criteria("username").containsIgnoreCase(userName);
		}
		if (isActive != null) {
			accountQuery.criteria("active").equal(isActive);
		}
		profileQuery.criteria("user id").in(accountQuery.asKeyList());
		List<String> positionTypes = new ArrayList<String>();
		positionTypes.add("University administrator");
		profileQuery.criteria("details.position type").in(positionTypes);
		if (positionTitle != null) {
			profileQuery.criteria("details.position title")
					.equal(positionTitle);
		} else {
			List<String> positionTitles = new ArrayList<String>();
			positionTitles.add("IRB");
			positionTitles.add("University Research Administrator");
			positionTitles.add("University Research Director");
			profileQuery.criteria("details.position title").in(positionTitles);
		}
		List<UserProfile> userProfiles = profileQuery.order(
				"-audit log.activity on").asList();
		for (UserProfile userProfile : userProfiles) {
			UserInfo user = generateUserInfo(0, userProfile);
			users.add(user);
		}
		return users;
	}

	/**
	 * Generates User Info
	 * 
	 * @param rowTotal
	 * @param userProfile
	 * @return
	 */
	private UserInfo generateUserInfo(int rowTotal, UserProfile userProfile) {
		UserInfo user = new UserInfo();
		user.setRowTotal(rowTotal);
		user.setId(userProfile.getId().toString());
		user.setUserName(userProfile.getUserAccount().getUserName());
		user.setFullName(userProfile.getFullName());
		// user.setNoOfPIedProposal(countPIProposal(userProfile));
		// user.setNoOfCoPIedProposal(countCoPIProposal(userProfile));
		// user.setNoOfSenioredProposal(countSeniorPersonnel(userProfile));
		user.setAddedOn(userProfile.getUserAccount().getAddedOn());
		Date lastAudited = null;
		String lastAuditedBy = new String();
		String lastAuditAction = new String();
		int auditLogCount = userProfile.getAuditLog().size();
		if (userProfile.getAuditLog() != null && auditLogCount != 0) {
			AuditLog auditLog = userProfile.getAuditLog()
					.get(auditLogCount - 1);
			lastAudited = auditLog.getActivityDate();
			lastAuditedBy = auditLog.getUserProfile().getFullName();
			lastAuditAction = auditLog.getAction();
		}
		user.setLastAudited(lastAudited);
		user.setLastAuditedBy(lastAuditedBy);
		user.setLastAuditAction(lastAuditAction);
		user.setDeleted(userProfile.getUserAccount().isDeleted());
		user.setActivated(userProfile.getUserAccount().isActive());
		user.setAdminUser(userProfile.getUserAccount().isAdmin());
		return user;
	}

	public List<AuditLogInfo> findAllForUserAuditLogGrid(int offset, int limit,
			ObjectId userId, String action, String auditedBy,
			String activityOnFrom, String activityOnTo) throws ParseException,
			UnknownHostException {
		Datastore ds = getDatastore();
		Query<UserProfile> profileQuery = ds.createQuery(UserProfile.class);
		UserProfile q = profileQuery.field("_id").equal(userId).get();
		List<AuditLogInfo> allAuditLogs = new ArrayList<AuditLogInfo>();
		int rowTotal = 0;
		if (q.getAuditLog() != null && q.getAuditLog().size() != 0) {
			for (AuditLog userProfileAudit : q.getAuditLog()) {
				getUserAuditLogs(action, auditedBy, activityOnFrom,
						activityOnTo, allAuditLogs, userProfileAudit);
			}
		}
		Collections.sort(allAuditLogs);
		rowTotal = allAuditLogs.size();
		if (rowTotal > 0) {
			for (AuditLogInfo t : allAuditLogs) {
				t.setRowTotal(rowTotal);
			}
		}
		if (rowTotal >= (offset + limit - 1)) {
			return allAuditLogs.subList(offset - 1, offset + limit - 1);
		} else {
			return allAuditLogs.subList(offset - 1, rowTotal);
		}
	}

	/**
	 * Gets User Audit Logs
	 * 
	 * @param action
	 * @param auditedBy
	 * @param activityOnFrom
	 * @param activityOnTo
	 * @param allAuditLogs
	 * @param userProfileAudit
	 * @throws ParseException
	 */
	private void getUserAuditLogs(String action, String auditedBy,
			String activityOnFrom, String activityOnTo,
			List<AuditLogInfo> allAuditLogs, AuditLog userProfileAudit)
			throws ParseException {
		AuditLogInfo userAuditLog = new AuditLogInfo();
		boolean isActionMatch = false;
		boolean isAuditedByMatch = false;
		boolean isActivityDateFromMatch = false;
		boolean isActivityDateToMatch = false;
		if (action != null) {
			if (userProfileAudit.getAction().toLowerCase()
					.contains(action.toLowerCase())) {
				isActionMatch = true;
			}
		} else {
			isActionMatch = true;
		}
		if (auditedBy != null) {
			if (userProfileAudit.getUserProfile().getUserAccount()
					.getUserName().toLowerCase()
					.contains(auditedBy.toLowerCase())) {
				isAuditedByMatch = true;
			} else if (userProfileAudit.getUserProfile().getFirstName()
					.toLowerCase().contains(auditedBy.toLowerCase())) {
				isAuditedByMatch = true;
			} else if (userProfileAudit.getUserProfile().getMiddleName()
					.toLowerCase().contains(auditedBy.toLowerCase())) {
				isAuditedByMatch = true;
			} else if (userProfileAudit.getUserProfile().getLastName()
					.toLowerCase().contains(auditedBy.toLowerCase())) {
				isAuditedByMatch = true;
			}
		} else {
			isAuditedByMatch = true;
		}
		DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
		if (activityOnFrom != null) {
			Date activityDateFrom = formatter.parse(activityOnFrom);
			if (userProfileAudit.getActivityDate().compareTo(activityDateFrom) > 0) {
				isActivityDateFromMatch = true;
			} else if (userProfileAudit.getActivityDate().compareTo(
					activityDateFrom) < 0) {
				isActivityDateFromMatch = false;
			} else if (userProfileAudit.getActivityDate().compareTo(
					activityDateFrom) == 0) {
				isActivityDateFromMatch = true;
			}
		} else {
			isActivityDateFromMatch = true;
		}
		if (activityOnTo != null) {
			Date activityDateTo = formatter.parse(activityOnTo);
			if (userProfileAudit.getActivityDate().compareTo(activityDateTo) > 0) {
				isActivityDateToMatch = false;
			} else if (userProfileAudit.getActivityDate().compareTo(
					activityDateTo) < 0) {
				isActivityDateToMatch = true;
			} else if (userProfileAudit.getActivityDate().compareTo(
					activityDateTo) == 0) {
				isActivityDateToMatch = true;
			}
		} else {
			isActivityDateToMatch = true;
		}
		if (isActionMatch && isAuditedByMatch && isActivityDateFromMatch
				&& isActivityDateToMatch) {
			userAuditLog.setUserName(userProfileAudit.getUserProfile()
					.getUserAccount().getUserName());
			userAuditLog.setUserFullName(userProfileAudit.getUserProfile()
					.getFullName());
			userAuditLog.setAction(userProfileAudit.getAction());
			userAuditLog.setActivityDate(userProfileAudit.getActivityDate());
			allAuditLogs.add(userAuditLog);
		}
	}

	@SuppressWarnings("unused")
	private int countPIProposal(UserProfile userProfile) {
		Datastore ds = getDatastore();
		return ds.createQuery(Proposal.class)
				.field("investigator info.pi.user profile").equal(userProfile)
				.asList().size();
	}

	@SuppressWarnings("unused")
	private int countCoPIProposal(UserProfile userProfile) {
		Datastore ds = getDatastore();
		return ds.createQuery(Proposal.class)
				.field("investigator info.co_pi.user profile")
				.equal(userProfile).asList().size();
	}

	@SuppressWarnings("unused")
	private int countSeniorPersonnel(UserProfile userProfile) {
		Datastore ds = getDatastore();
		return ds.createQuery(Proposal.class)
				.field("investigator info.senior personnel.user profile")
				.equal(userProfile).asList().size();
	}

	public UserProfile findUserDetailsByProfileID(ObjectId id) {
		Datastore ds = getDatastore();
		return ds.createQuery(UserProfile.class).field("_id").equal(id).get();
	}

	public UserProfile findUserInfoByProfileID(ObjectId id) {
		Datastore ds = getDatastore();
		return ds.createQuery(UserProfile.class)
				.retrievedFields(true, "user id").field("_id").equal(id).get();
	}

	public UserProfile findByUserAccount(UserAccount userAccount) {
		Datastore ds = getDatastore();
		return ds.createQuery(UserProfile.class).field("user id")
				.equal(userAccount).get();
	}

	public void signUpUser(UserProfile newProfile) {
		Datastore ds = getDatastore();
		ds.save(newProfile);
		AuditLog audit = new AuditLog(newProfile,
				"Signed up new user account and profile of "
						+ newProfile.getUserAccount().getUserName(), new Date());
		newProfile.getAuditLog().add(audit);
		ds.save(newProfile);
		String messageBody = "Hello "
				+ newProfile.getFullName()
				+ ",<br/><br/> You have successfully created an account. As soon as administrator will activate and assign you on positions you will get an email and then only you can login. If you want to activate as soon as possible please contact administrator: <a href='http://seal.boisestate.edu:8080/GPMS/ContactUs.jsp' title='GPMS Contact Us' target='_blank'>Contact Us</a><br/><br/>Thank you, <br/> GPMS Team";
		EmailUtil emailUtil = new EmailUtil();
		emailUtil.sendMailWithoutAuth(newProfile.getWorkEmails().get(0),
				"Successfully created an account " + newProfile.getFullName(),
				messageBody);
	}

	public void saveUser(UserProfile newProfile, UserProfile authorProfile) {
		Datastore ds = getDatastore();
		audit = new AuditLog(authorProfile,
				"Created user account and profile of "
						+ newProfile.getUserAccount().getUserName(), new Date());
		newProfile.getAuditLog().add(audit);
		ds.save(newProfile);
		String messageBody = new String();
		EmailUtil emailUtil = new EmailUtil();
		if (newProfile.getUserAccount().isActive()) {
			messageBody = "Hello "
					+ newProfile.getFullName()
					+ ",<br/><br/> You have been activated and you can login now using your credential: <a href='http://seal.boisestate.edu:8080/GPMS/Login.jsp' title='GPMS Login' target='_blank'>Login Here</a><br/><br/>Thank you, <br/> GPMS Team";
			emailUtil.sendMailWithoutAuth(
					newProfile.getWorkEmails().get(0),
					"Successfully Activated your account "
							+ newProfile.getFullName(), messageBody);
		} else {
			messageBody = "Hello "
					+ newProfile.getFullName()
					+ ",<br/> You are not activated yet to activate please contact administrator: <a href='http://seal.boisestate.edu:8080/GPMS/ContactUs.jsp' title='GPMS Contact Us' target='_blank'>Contact Us</a><br/><br/>Thank you, <br/> GPMS Team";
			emailUtil.sendMailWithoutAuth(newProfile.getWorkEmails().get(0),
					"You are not activated yet " + newProfile.getFullName(),
					messageBody);
		}
	}

	public void updateUser(UserProfile existingUserProfile,
			UserProfile authorProfile) {
		Datastore ds = getDatastore();
		audit = new AuditLog(authorProfile,
				"Updated user account and profile of "
						+ existingUserProfile.getUserAccount().getUserName(),
				new Date());
		existingUserProfile.getAuditLog().add(audit);
		ds.save(existingUserProfile);
		String messageBody = new String();
		EmailUtil emailUtil = new EmailUtil();
		if (existingUserProfile.getUserAccount().isActive()) {
			messageBody = "Hello "
					+ existingUserProfile.getFullName()
					+ ",<br/><br/> Your account has been activated and you can login now using your credential: <a href='http://seal.boisestate.edu:8080/GPMS/Login.jsp' title='GPMS Login' target='_blank'>Login Here</a><br/><br/>Thank you, <br/> GPMS Team";
			emailUtil.sendMailWithoutAuth(existingUserProfile.getWorkEmails()
					.get(0), "Successfully Activated your account "
					+ existingUserProfile.getFullName(), messageBody);
		} else {
			messageBody = "Hello "
					+ existingUserProfile.getFullName()
					+ ",<br/> Your account has been deactivated to reactivate you can contact administrator: <a href='http://seal.boisestate.edu:8080/GPMS/ContactUs.jsp' title='GPMS Contact Us' target='_blank'>Contact Us</a><br/><br/>Thank you, <br/> GPMS Team";
			emailUtil.sendMailWithoutAuth(
					existingUserProfile.getWorkEmails().get(0),
					"You have been Deactivated "
							+ existingUserProfile.getFullName(), messageBody);
		}
	}

	public void deleteUserProfileByUserID(UserProfile userProfile,
			UserProfile authorProfile) {
		Datastore ds = getDatastore();
		audit = new AuditLog(authorProfile,
				"Deleted user profile and account of "
						+ userProfile.getFullName(), new Date());
		userProfile.getAuditLog().add(audit);
		userProfile.setDeleted(true);
		ds.save(userProfile);
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
	}

	public void activateUserProfileByUserID(UserProfile userProfile,
			UserProfile authorProfile, Boolean isActive) {
		Datastore ds = getDatastore();
		if (isActive) {
			audit = new AuditLog(authorProfile,
					"Activated user account and profile of "
							+ userProfile.getFullName(), new Date());
		} else {
			audit = new AuditLog(authorProfile,
					"Deactivated user account and profile of "
							+ userProfile.getFullName(), new Date());
		}
		userProfile.getAuditLog().add(audit);
		userProfile.setDeleted(!isActive);
		ds.save(userProfile);
	}

	public UserProfile findNextUserWithSameUserName(ObjectId id, String userName) {
		Datastore ds = getDatastore();
		Query<UserProfile> profileQuery = ds.createQuery(UserProfile.class);
		Query<UserAccount> accountQuery = ds.createQuery(UserAccount.class);
		accountQuery.criteria("username").equalIgnoreCase(userName);
		profileQuery.and(profileQuery.criteria("_id").notEqual(id),
				profileQuery.criteria("user id").in(accountQuery.asKeyList()));
		return profileQuery.get();
	}

	public UserProfile findAnyUserWithSameUserName(String newUserName) {
		Datastore ds = getDatastore();
		Query<UserProfile> profileQuery = ds.createQuery(UserProfile.class);
		Query<UserAccount> accountQuery = ds.createQuery(UserAccount.class);
		accountQuery.criteria("username").equalIgnoreCase(newUserName);
		profileQuery.criteria("user id").in(accountQuery.asKeyList());
		return profileQuery.get();
	}

	public UserProfile findNextUserWithSameEmail(ObjectId id, String newEmail) {
		Datastore ds = getDatastore();
		Query<UserProfile> profileQuery = ds.createQuery(UserProfile.class);
		profileQuery.and(
				profileQuery.criteria("_id").notEqual(id),
				profileQuery.or(
						profileQuery.criteria("work email").hasThisOne(
								newEmail.toString()),
						profileQuery.criteria("personal email").hasThisOne(
								newEmail.toString())));
		return profileQuery.get();
	}

	public UserProfile findAnyUserWithSameEmail(String newEmail) {
		Datastore ds = getDatastore();
		Query<UserProfile> profileQuery = ds.createQuery(UserProfile.class);
		profileQuery.or(
				profileQuery.criteria("work email").hasThisOne(
						newEmail.toString()),
				profileQuery.criteria("personal email").hasThisOne(
						newEmail.toString()));
		return profileQuery.get();
	}

	public List<InvestigatorUsersAndPositions> findCurrentPositionDetailsForPI(
			ObjectId id, String userCollege, String userDepartment,
			String userPositionType, String userPositionTitle) {
		Datastore ds = getDatastore();
		List<InvestigatorUsersAndPositions> userPositions = new ArrayList<InvestigatorUsersAndPositions>();
		Query<UserProfile> q = ds.createQuery(UserProfile.class)
				.retrievedFields(true, "_id", "first name", "middle name",
						"last name", "details", "mobile number");
		q.and(q.criteria("details").notEqual(null), q.criteria("_id").equal(id));
		List<UserProfile> userProfile = q.asList();
		for (UserProfile user : userProfile) {
			Multimap<String, Object> htUser = ArrayListMultimap.create();
			InvestigatorUsersAndPositions userPosition = new InvestigatorUsersAndPositions();
			userPosition.setId(user.getId().toString());
			userPosition.setFullName(user.getFullName());
			userPosition.setMobileNumber(user.getMobileNumbers().get(0));
			for (PositionDetails userDetails : user.getDetails()) {
				String college = userDetails.getCollege();
				String department = userDetails.getDepartment();
				String positionType = userDetails.getPositionType();
				String positionTitle = userDetails.getPositionTitle();
				if (((positionType
						.equalsIgnoreCase("Tenured/tenure-track faculty")
						|| positionType
								.equalsIgnoreCase("Non-tenure-track research faculty") || positionType
							.equalsIgnoreCase("Teaching faculty")) && positionType
						.equals(userPositionType))
						&& college.equals(userCollege)
						&& department.equals(userDepartment)
						&& positionTitle.equals(userPositionTitle)) {
					Multimap<String, Object> mapTypeTitle = ArrayListMultimap
							.create();
					Multimap<String, Object> mapDeptType = ArrayListMultimap
							.create();
					mapTypeTitle.put(positionType, positionTitle);
					mapDeptType.put(department, mapTypeTitle.asMap());

					htUser.put(college, mapDeptType.asMap());
					userPosition.setPositions(htUser);
				}
			}
			userPositions.add(userPosition);
		}
		return userPositions;
	}

	public List<InvestigatorUsersAndPositions> findAllPositionDetailsForAUser(
			ObjectId id) {
		Datastore ds = getDatastore();
		List<InvestigatorUsersAndPositions> userPositions = new ArrayList<InvestigatorUsersAndPositions>();
		Query<UserProfile> q = ds.createQuery(UserProfile.class)
				.retrievedFields(true, "_id", "first name", "middle name",
						"last name", "details", "mobile number");
		q.and(q.criteria("details").notEqual(null), q.criteria("_id").equal(id));
		List<UserProfile> userProfile = q.asList();
		for (UserProfile user : userProfile) {
			Multimap<String, Object> htUser = ArrayListMultimap.create();
			InvestigatorUsersAndPositions userPosition = new InvestigatorUsersAndPositions();
			userPosition.setId(user.getId().toString());
			userPosition.setFullName(user.getFullName());
			userPosition.setMobileNumber(user.getMobileNumbers().get(0));
			for (PositionDetails userDetails : user.getDetails()) {
				if (userDetails.getPositionType().equalsIgnoreCase(
						"Tenured/tenure-track faculty")
						|| userDetails.getPositionType().equalsIgnoreCase(
								"Non-tenure-track research faculty")
						|| userDetails.getPositionType().equalsIgnoreCase(
								"Teaching faculty")) {
					Multimap<String, Object> mapTypeTitle = ArrayListMultimap
							.create();
					Multimap<String, Object> mapDeptType = ArrayListMultimap
							.create();
					mapTypeTitle.put(userDetails.getPositionType(),
							userDetails.getPositionTitle());
					mapDeptType.put(userDetails.getDepartment(),
							mapTypeTitle.asMap());
					htUser.put(userDetails.getCollege(), mapDeptType.asMap());
					userPosition.setPositions(htUser);
				}
			}
			userPositions.add(userPosition);
		}
		return userPositions;
	}

	public List<InvestigatorUsersAndPositions> findUserPositionDetailsForAProposal(
			List<ObjectId> userIds) {
		Datastore ds = getDatastore();
		List<InvestigatorUsersAndPositions> userPositions = new ArrayList<InvestigatorUsersAndPositions>();
		Query<UserProfile> q = ds.createQuery(UserProfile.class)
				.retrievedFields(true, "_id", "first name", "middle name",
						"last name", "details", "mobile number");
		q.and(q.criteria("details").notEqual(null),
				q.criteria("_id").in(userIds));
		List<UserProfile> userProfile = q.asList();
		for (UserProfile user : userProfile) {
			Multimap<String, Object> htUser = ArrayListMultimap.create();
			InvestigatorUsersAndPositions userPosition = new InvestigatorUsersAndPositions();
			userPosition.setId(user.getId().toString());
			userPosition.setFullName(user.getFullName());
			userPosition.setMobileNumber(user.getMobileNumbers().get(0));
			for (PositionDetails userDetails : user.getDetails()) {
				if (userDetails.getPositionType().equalsIgnoreCase(
						"Tenured/tenure-track faculty")
						|| userDetails.getPositionType().equalsIgnoreCase(
								"Non-tenure-track research faculty")
						|| userDetails.getPositionType().equalsIgnoreCase(
								"Teaching faculty")) {
					Multimap<String, Object> mapTypeTitle = ArrayListMultimap
							.create();
					Multimap<String, Object> mapDeptType = ArrayListMultimap
							.create();
					mapTypeTitle.put(userDetails.getPositionType(),
							userDetails.getPositionTitle());
					mapDeptType.put(userDetails.getDepartment(),
							mapTypeTitle.asMap());
					htUser.put(userDetails.getCollege(), mapDeptType.asMap());
					userPosition.setPositions(htUser);
				}
			}
			userPositions.add(userPosition);
		}
		return userPositions;
	}

	public UserProfile findMatchedUserDetails(ObjectId id, String userName,
			Boolean isAdminUser, String college, String department,
			String positionType, String positionTitle) {
		Datastore ds = getDatastore();
		Query<UserAccount> accountQuery = ds.createQuery(UserAccount.class);
		accountQuery.and(accountQuery.criteria("deleted").equal(false),
				accountQuery.criteria("active").equal(true), accountQuery
						.criteria("username").equal(userName), accountQuery
						.criteria("admin").equal(isAdminUser));
		Query<UserProfile> profileQuery = ds.createQuery(UserProfile.class)
				.retrievedFields(true, "_id", "user id", "details.college",
						"details.department", "details.position type",
						"details.position title");
		if (isAdminUser) {
			profileQuery.and(
					profileQuery.criteria("_id").equal(id),
					profileQuery.and(profileQuery.criteria("user id").in(
							accountQuery.asKeyList())),
					profileQuery.criteria("deleted").equal(false));
		} else {
			profileQuery.and(
					profileQuery.criteria("_id").equal(id),
					profileQuery.and(profileQuery.criteria("user id").in(
							accountQuery.asKeyList())),
					profileQuery.criteria("details").notEqual(null),
					profileQuery.criteria("details.college").equal(college),
					profileQuery.criteria("details.department").equal(
							department),
					profileQuery.criteria("details.positionType").equal(
							positionType),
					profileQuery.criteria("details.positionTitle").equal(
							positionTitle), profileQuery.criteria("deleted")
							.equal(false));
		}
		return profileQuery.get();
	}

	public UserProposalCount getUserProposalCounts(String userProfileId,
			String college, String department, String positionType,
			String positionTitle) {
		Datastore ds = getDatastore();
		UserProposalCount userProposalCount = new UserProposalCount();
		Query<Proposal> proposalQuery = ds.createQuery(Proposal.class);
		proposalQuery.or(proposalQuery.and(
				proposalQuery.criteria("investigator info.pi.user profile id")
						.equal(userProfileId),
				proposalQuery.criteria("investigator info.pi.college").equal(
						college),
				proposalQuery.criteria("investigator info.pi.department")
						.equal(department),
				proposalQuery.criteria("investigator info.pi.position type")
						.equal(positionType),
				proposalQuery.criteria("investigator info.pi.position title")
						.equal(positionTitle)), proposalQuery.and(
				proposalQuery.criteria(
						"investigator info.co_pi.user profile id").equal(
						userProfileId),
				proposalQuery.criteria("investigator info.co_pi.college")
						.equal(college),
				proposalQuery.criteria("investigator info.co_pi.department")
						.equal(department),
				proposalQuery.criteria("investigator info.co_pi.position type")
						.equal(positionType),
				proposalQuery
						.criteria("investigator info.co_pi.position title")
						.equal(positionTitle)), proposalQuery.and(
				proposalQuery.criteria(
						"investigator info.senior personnel.user profile id")
						.equal(userProfileId),
				proposalQuery.criteria(
						"investigator info.senior personnel.college").equal(
						college),
				proposalQuery.criteria(
						"investigator info.senior personnel.department").equal(
						department),
				proposalQuery.criteria(
						"investigator info.senior personnel.position type")
						.equal(positionType),
				proposalQuery.criteria(
						"investigator info.senior personnel.position title")
						.equal(positionTitle)));
		userProposalCount.setTotalProposalCount(proposalQuery.asList().size());
		proposalQuery = ds.createQuery(Proposal.class);
		proposalQuery.and(
				proposalQuery.criteria("investigator info.pi.user profile id")
						.equal(userProfileId),
				proposalQuery.criteria("investigator info.pi.college").equal(
						college),
				proposalQuery.criteria("investigator info.pi.department")
						.equal(department),
				proposalQuery.criteria("investigator info.pi.position type")
						.equal(positionType),
				proposalQuery.criteria("investigator info.pi.position title")
						.equal(positionTitle));
		userProposalCount.setPiCount(proposalQuery.asList().size());
		proposalQuery = ds.createQuery(Proposal.class);
		proposalQuery.and(
				proposalQuery.criteria(
						"investigator info.co_pi.user profile id").equal(
						userProfileId),
				proposalQuery.criteria("investigator info.co_pi.college")
						.equal(college),
				proposalQuery.criteria("investigator info.co_pi.department")
						.equal(department),
				proposalQuery.criteria("investigator info.co_pi.position type")
						.equal(positionType),
				proposalQuery
						.criteria("investigator info.co_pi.position title")
						.equal(positionTitle));
		userProposalCount.setCoPICount(proposalQuery.asList().size());
		proposalQuery = ds.createQuery(Proposal.class);
		proposalQuery.and(
				proposalQuery.criteria(
						"investigator info.senior personnel.user profile id")
						.equal(userProfileId),
				proposalQuery.criteria(
						"investigator info.senior personnel.college").equal(
						college),
				proposalQuery.criteria(
						"investigator info.senior personnel.department").equal(
						department),
				proposalQuery.criteria(
						"investigator info.senior personnel.position type")
						.equal(positionType),
				proposalQuery.criteria(
						"investigator info.senior personnel.position title")
						.equal(positionTitle));
		userProposalCount.setSeniorCount(proposalQuery.asList().size());
		return userProposalCount;
	}

	/***
	 * Gets Supervisory Personnel For a Proposal
	 * 
	 * @param College
	 * @param department
	 * @param positionTitle
	 * @param isAdmin
	 * @return
	 */
	public List<UserProfile> getSupervisoryPersonnels(String College,
			String department, String positionTitle, boolean isAdmin) {
		Datastore ds = getDatastore();
		Query<UserProfile> profileQuery = ds.createQuery(UserProfile.class);
		if (isAdmin) {
			profileQuery.and(
					profileQuery.criteria("details.college").equal(College),
					profileQuery.criteria("details.position title").equal(
							positionTitle));
		} else {
			profileQuery.and(
					profileQuery.criteria("details.department").equal(
							department),
					profileQuery.criteria("details.position title").equal(
							positionTitle),
					profileQuery.criteria("details.college").equal(College));
		}
		return profileQuery.asList();
	}

}
