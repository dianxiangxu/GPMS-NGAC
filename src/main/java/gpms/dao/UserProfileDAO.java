package gpms.dao;

import gpms.DAL.MongoDBConnector;
import gpms.model.Address;
import gpms.model.AuditLog;
import gpms.model.AuditLogCommonInfo;
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
import gpms.ngac.policy.NGACPolicyConfigurationLoader;
import gpms.ngac.policy.PDSOperations;
import gpms.utils.EmailUtil;

import static gov.nist.csd.pm.pip.graph.model.nodes.NodeType.U;

import java.io.File;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang3.RandomStringUtils;
import org.bson.types.ObjectId;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.Morphia;
import org.mongodb.morphia.dao.BasicDAO;
import org.mongodb.morphia.query.Query;

import com.ebay.xcelite.Xcelite;
import com.ebay.xcelite.sheet.XceliteSheet;
import com.ebay.xcelite.writer.SheetWriter;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.mongodb.MongoClient;
import com.mongodb.MongoException;

import gov.nist.csd.pm.exceptions.PMException;
import gov.nist.csd.pm.pip.graph.Graph;
import gov.nist.csd.pm.pip.graph.GraphSerializer;
import gov.nist.csd.pm.pip.graph.MemGraph;
import gov.nist.csd.pm.pip.graph.model.nodes.Node;
import static gov.nist.csd.pm.pip.graph.model.nodes.NodeType.UA;

public class UserProfileDAO extends BasicDAO<UserProfile, String> {
	private static final String DBNAME = "db_gpms";
	public static final String COLLECTION_NAME = "userprofile";

	private static Morphia morphia;
	private static Datastore ds;
	private AuditLog audit = new AuditLog();
	private DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");

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

	public List<UserProfile> findAll() throws UnknownHostException {
		Datastore ds = getDatastore();
		return ds.createQuery(UserProfile.class).asList();
	}

	public List<UserInfo> findAllUsers(GPMSCommonInfo userInfo)
			throws UnknownHostException {
		Datastore ds = getDatastore();
		String userName = userInfo.getUserName();
		String college = userInfo.getUserCollege();
		String department = userInfo.getUserDepartment();
		String positionType = userInfo.getUserPositionType();
		String positionTitle = userInfo.getUserPositionTitle();
		Boolean isActive = userInfo.getUserIsActive();
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

	public List<UserInfo> findAllAdminUsers(GPMSCommonInfo userInfo)
			throws UnknownHostException {
		Datastore ds = getDatastore();
		String userName = userInfo.getUserName();
		String positionTitle = userInfo.getUserPositionTitle();
		Boolean isActive = userInfo.getUserIsActive();
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

	public List<UserProfile> findAllUsersWithPosition(String position)
			throws UnknownHostException {
		Datastore ds = getDatastore();
//		Query<UserProfile> profileQuery = ds.createQuery(UserProfile.class);
//		profileQuery
//				.and(profileQuery.criteria("deleted").equal(false),
//						profileQuery.criteria("details").notEqual(null),
//						profileQuery
//								.or(profileQuery.criteria(
//										"details.position type")
//										.equalIgnoreCase(
//												"Tenured/tenure-track faculty"),
//										profileQuery
//												.criteria(
//														"details.position type")
//												.equalIgnoreCase(
//														"Non-tenure-track research faculty"),
//										profileQuery.criteria(
//												"details.position type")
//												.equalIgnoreCase(
//														"Teaching faculty")));
		
		List<UserProfile> userProfiles = new ArrayList<UserProfile>();
		try {
			Set<String> elegibleUserNames = PDSOperations.getElegibleUsers(position);
			for(String userName : elegibleUserNames) {
				userProfiles.add(findAnyUserWithSameUserName(userName));
			}
		} catch (PMException e) {
			e.printStackTrace();
		}
		return userProfiles;

		
//		for(UserProfile up : profileQuery.retrievedFields(true, "_id", "first name",
//				"middle name", "last name").asList()) {
////			System.out.println(up.getId());
////			System.out.println(up.getFirstName());
////
////			System.out.println(up.getMiddleName());
////
////			System.out.println(up.getLastName());
//
//
//		}
//		return profileQuery.retrievedFields(true, "_id", "first name",
//				"middle name", "last name").asList();
	}
	
	
	
	
	
	
	
	private List<String> getPositionTypesWithAccessPI() throws PMException{
		List<String> arrayOfElegiblePIs = new ArrayList<String>();
		NGACPolicyConfigurationLoader loader = new NGACPolicyConfigurationLoader();
		loader.init();
		String proposalCreationPolicy = loader.jsonProposalCreation;
		Graph graph = new MemGraph();
		GraphSerializer.fromJson(graph, proposalCreationPolicy);
		Node piNode = graph.getNode("PI-Eligible Faculty");
		
		Map<String, String> visited = new HashMap<String, String>();
		visited.put("isVisited", "yes");

		Stack<Node> stack = new Stack<Node>();
		stack.push(piNode);
		System.out.println(piNode);

		while (!stack.isEmpty()) {

			Node newRoot = stack.pop();
			Set<String> children= graph.getChildren(piNode.getName());
		
			for (String userAttNode : children) {
				Node child = graph.getNode(userAttNode);
				if (!child.getProperties().equals(visited)) {
					stack.push(child);
				}
			}
			if (newRoot.getProperties().equals(visited)
					|| newRoot.getType() != UA) {

				continue;
			}
			arrayOfElegiblePIs.add(newRoot.getName());
			graph.updateNode(newRoot.getName(), visited);

		}
		
		return null;
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
	public List<UserInfo> findAllUsersForGrid(int offset, int limit,
			GPMSCommonInfo userInfo) throws UnknownHostException {
		Boolean isActive = userInfo.getUserIsActive();
		Datastore ds = getDatastore();
		List<UserInfo> users = new ArrayList<UserInfo>();
		Query<UserProfile> profileQuery = ds.createQuery(UserProfile.class);
		Query<UserAccount> accountQuery = ds.createQuery(UserAccount.class);
		if (userInfo.getUserName() != null) {
			accountQuery.criteria("username").containsIgnoreCase(
					userInfo.getUserName());
		}
		if (isActive != null) {
			accountQuery.criteria("active").equal(isActive);
		}
		profileQuery.criteria("user id").in(accountQuery.asKeyList());

		if (userInfo.getUserCollege() != null) {
			profileQuery.criteria("details.college").equal(
					userInfo.getUserCollege());
		}
		if (userInfo.getUserDepartment() != null) {
			profileQuery.criteria("details.department").equal(
					userInfo.getUserDepartment());
		}
		if (userInfo.getUserPositionType() != null) {
			profileQuery.criteria("details.position type").equal(
					userInfo.getUserPositionType());
		} else {
			List<String> positionTypes = new ArrayList<String>();
			positionTypes.add("University administrator");
			profileQuery.criteria("details.position type").hasNoneOf(
					positionTypes);
		}
		if (userInfo.getUserPositionTitle() != null) {
			profileQuery.criteria("details.position title").equal(
					userInfo.getUserPositionTitle());
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
	public List<UserProfile> findAllForAdminUserGrid(String type) {
		Datastore ds = getDatastore();
		Boolean isActive = true;
		List<UserInfo> users = new ArrayList<UserInfo>();
		Query<UserProfile> profileQuery = ds.createQuery(UserProfile.class);
		Query<UserAccount> accountQuery = ds.createQuery(UserAccount.class);
		
		if (isActive != null) {
			accountQuery.criteria("active").equal(isActive);
		}
		profileQuery.criteria("user id").in(accountQuery.asKeyList());
		List<String> positionTitles = new ArrayList<String>();
		if (type.equals("DEPT")) {
			positionTitles.add("Department Chair");
			positionTitles.add("Dean");
			positionTitles.add("Business Manager");
		}
		else if(type.equals("UNIVERSITY")) {
			positionTitles.add("IRB");
			positionTitles.add("University Research Administrator");
			positionTitles.add("University Research Director");
		}
		profileQuery.criteria("details.position title").in(positionTitles);
		
		List<UserProfile> userProfiles = profileQuery.asList();
		
		return userProfiles;
	}
	

	public List<UserInfo> findAllForAdminUserGrid(int offset, int limit,
			GPMSCommonInfo userInfo) {
		Datastore ds = getDatastore();
		Boolean isActive = userInfo.getUserIsActive();
		List<UserInfo> users = new ArrayList<UserInfo>();
		Query<UserProfile> profileQuery = ds.createQuery(UserProfile.class);
		Query<UserAccount> accountQuery = ds.createQuery(UserAccount.class);
		if (userInfo.getUserName() != null) {
			accountQuery.criteria("username").containsIgnoreCase(
					userInfo.getUserName());
		}
		if (isActive != null) {
			accountQuery.criteria("active").equal(isActive);
		}
		profileQuery.criteria("user id").in(accountQuery.asKeyList());
		List<String> positionTypes = new ArrayList<String>();
		positionTypes.add("University administrator");
		profileQuery.criteria("details.position type").in(positionTypes);
		if (userInfo.getUserPositionTitle() != null) {
			profileQuery.criteria("details.position title").equal(
					userInfo.getUserPositionTitle());
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
		user.setDeleted(userProfile.getUserAccount().isDeleted());
		user.setActivated(userProfile.getUserAccount().isActive());
		user.setAdminUser(userProfile.getUserAccount().isAdmin());
		return getRecentUserProfileAuditLog(userProfile, user);
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
				+ ",<br/><br/> You have successfully created an account. As soon as administrator will activate and assign you on positions you will get an email and then only you can login. If you want to activate as soon as possible please contact administrator: <a href='http://seal.boisestate.edu/GPMS/ContactUs.jsp' title='GPMS Contact Us' target='_blank'>Contact Us</a><br/><br/>Thank you, <br/> GPMS Team";
		EmailUtil emailUtil = new EmailUtil();
		emailUtil.sendMailWithoutAuth(newProfile.getWorkEmails().get(0),
				"Successfully created an account " + newProfile.getFullName(),
				messageBody);
	}

	/**
	 * Binds User Info From user Sign up from
	 * 
	 * @param newAccount
	 * @param newProfile
	 * @param userInfo
	 * @throws ParseException
	 */
	public void bindUserInfo(UserAccount newAccount, UserProfile newProfile,
			JsonNode userInfo) throws ParseException {
		String userEmail = new String();
		newAccount.setAddedOn(new Date());
		if (userInfo != null && userInfo.has("UserName")) {
			String loginUserName = userInfo.get("UserName").textValue();
			newAccount.setUserName(loginUserName);
		}
		if (userInfo != null && userInfo.has("Password")) {
			newAccount.setPassword(userInfo.get("Password").textValue());
		}
		newProfile.setUserAccount(newAccount);
		if (userInfo != null && userInfo.has("FirstName")) {
			newProfile.setFirstName(userInfo.get("FirstName").textValue());
		}
		if (userInfo != null && userInfo.has("MiddleName")) {
			newProfile.setMiddleName(userInfo.get("MiddleName").textValue());
		}
		if (userInfo != null && userInfo.has("LastName")) {
			newProfile.setLastName(userInfo.get("LastName").textValue());
		}
		if (userInfo != null && userInfo.has("DOB")) {
			Date dob = formatter.parse(userInfo.get("DOB").textValue());
			newProfile.setDateOfBirth(dob);
		}
		if (userInfo != null && userInfo.has("Gender")) {
			newProfile.setGender(userInfo.get("Gender").textValue());
		}
		Address newAddress = new Address();
		if (userInfo != null && userInfo.has("Street")) {
			newAddress.setStreet(userInfo.get("Street").textValue());
		}
		if (userInfo != null && userInfo.has("Apt")) {
			newAddress.setApt(userInfo.get("Apt").textValue());
		}
		if (userInfo != null && userInfo.has("City")) {
			newAddress.setCity(userInfo.get("City").textValue());
		}
		if (userInfo != null && userInfo.has("State")) {
			newAddress.setState(userInfo.get("State").textValue());
		}
		if (userInfo != null && userInfo.has("Zip")) {
			newAddress.setZipcode(userInfo.get("Zip").textValue());
		}
		if (userInfo != null && userInfo.has("Country")) {
			newAddress.setCountry(userInfo.get("Country").textValue());
		}
		newProfile.getAddresses().add(newAddress);
		if (userInfo != null && userInfo.has("MobileNumber")) {
			newProfile.getMobileNumbers().add(
					userInfo.get("MobileNumber").textValue());
		}
		if (userInfo != null && userInfo.has("WorkEmail")) {
			userEmail = userInfo.get("WorkEmail").textValue();
			newProfile.getWorkEmails().add(userEmail);
		}
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
					+ ",<br/><br/> You have been activated and you can login now using your credential: <a href='http://seal.boisestate.edu/GPMS/Login.jsp' title='GPMS Login' target='_blank'>Login Here</a><br/><br/>Thank you, <br/> GPMS Team";
			emailUtil.sendMailWithoutAuth(
					newProfile.getWorkEmails().get(0),
					"Successfully Activated your account "
							+ newProfile.getFullName(), messageBody);
		} else {
			messageBody = "Hello "
					+ newProfile.getFullName()
					+ ",<br/> You are not activated yet to activate please contact administrator: <a href='http://seal.boisestate.edu/GPMS/ContactUs.jsp' title='GPMS Contact Us' target='_blank'>Contact Us</a><br/><br/>Thank you, <br/> GPMS Team";
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
					+ ",<br/><br/> Your account has been activated and you can login now using your credential: <a href='http://seal.boisestate.edu/GPMS/Login.jsp' title='GPMS Login' target='_blank'>Login Here</a><br/><br/>Thank you, <br/> GPMS Team";
			emailUtil.sendMailWithoutAuth(existingUserProfile.getWorkEmails()
					.get(0), "Successfully Activated your account "
					+ existingUserProfile.getFullName(), messageBody);
		} else {
			messageBody = "Hello "
					+ existingUserProfile.getFullName()
					+ ",<br/> Your account has been deactivated to reactivate you can contact administrator: <a href='http://seal.boisestate.edu/GPMS/ContactUs.jsp' title='GPMS Contact Us' target='_blank'>Contact Us</a><br/><br/>Thank you, <br/> GPMS Team";
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
					+ ",<br/> Your account has been deleted to reactivate you can contact administrator: <a href='http://seal.boisestate.edu/GPMS/ContactUs.jsp' title='GPMS Contact Us' target='_blank'>Contact Us</a><br/><br/>Thank you, <br/> GPMS Team";
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
			ObjectId id, GPMSCommonInfo userInfo) {
		Datastore ds = getDatastore();
		String userCollege = userInfo.getUserCollege();
		String userDepartment = userInfo.getUserDepartment();
		String userPositionType = userInfo.getUserPositionType();
		String userPositionTitle = userInfo.getUserPositionTitle();
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

	public UserProposalCount getUserProposalCounts(GPMSCommonInfo userInfo) {
		Datastore ds = getDatastore();
		String userProfileId = userInfo.getUserProfileID();
		String college = userInfo.getUserCollege();
		String department = userInfo.getUserDepartment();
		String positionType = userInfo.getUserPositionType();
		String positionTitle = userInfo.getUserPositionTitle();
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

	/**
	 * Adds User Login Details
	 * 
	 * @param userID
	 * @param newAccount
	 * @param existingUserAccount
	 * @param existingUserProfile
	 * @param userInfo
	 * @return
	 */
	public UserAccount addUserLoginDetails(String userID,
			UserAccount newAccount, UserAccount existingUserAccount,
			UserProfile existingUserProfile, JsonNode userInfo) {
		if (userInfo != null && userInfo.has("UserName")) {
			String userNameOf = userInfo.get("UserName").textValue();
			if (!userID.equals("0") && existingUserProfile != null) {
				existingUserAccount = existingUserProfile.getUserAccount();
				if (!existingUserAccount.getUserName().equals(userNameOf)) {
					existingUserAccount = null;
				}
			} else {
				newAccount.setUserName(userNameOf);
			}
		}
		if (userInfo != null && userInfo.has("Password")) {
			if (!userID.equals("0")) {
				if (!existingUserAccount.getPassword().equals(
						userInfo.get("Password").textValue())) {
					existingUserAccount.setPassword(userInfo.get("Password")
							.textValue());
				}
			} else {
				newAccount.setPassword(userInfo.get("Password").textValue());
			}
		}
		return existingUserAccount;
	}

	// Sessions

	/**
	 * Binds User Info From login user Session
	 * 
	 * @param session
	 * @throws ServletException
	 */
	public GPMSCommonInfo bindUserInfoFromSession(HttpSession session)
			throws ServletException {
		GPMSCommonInfo userInfo = new GPMSCommonInfo();
		if (session.getAttribute("userProfileId") == null
				|| session.getAttribute("userCollege") == null
				|| session.getAttribute("userDepartment") == null
				|| session.getAttribute("userPositionType") == null
				|| session.getAttribute("userPositionTitle") == null) {
			throw new ServletException("User Session can't be null or empty");
		}
		if (session.getAttribute("userProfileId") != null) {
			userInfo.setUserProfileID((String) session
					.getAttribute("userProfileId"));
		}
		if (session.getAttribute("userCollege") != null) {
			userInfo.setUserCollege((String) session
					.getAttribute("userCollege"));
		}
		if (session.getAttribute("userDepartment") != null) {
			userInfo.setUserDepartment((String) session
					.getAttribute("userDepartment"));
		}
		if (session.getAttribute("userPositionType") != null) {
			userInfo.setUserPositionType((String) session
					.getAttribute("userPositionType"));
		}
		if (session.getAttribute("userPositionTitle") != null) {
			userInfo.setUserPositionTitle((String) session
					.getAttribute("userPositionTitle"));
		}
		if (session.getAttribute("isAdmin") != null) {
			userInfo.setUserIsAdmin((Boolean) session.getAttribute("isAdmin"));
		}

		return userInfo;
	}

	public void setUserCurrentSession(HttpServletRequest req, String userName,
			boolean admin, String userId, String college, String department,
			String positionType, String positionTitle) {
		HttpSession session = req.getSession();
		if (session.getAttribute("userProfileId") == null) {
			session.setAttribute("userProfileId", userId);
		}
		if (session.getAttribute("gpmsUserName") == null) {
			session.setAttribute("gpmsUserName", userName);
		}
		if (session.getAttribute("isAdmin") == null) {
			session.setAttribute("isAdmin", admin);
		}
		if (session.getAttribute("userCollege") == null) {
			session.setAttribute("userCollege", college);
		}
		if (session.getAttribute("userDepartment") == null) {
			session.setAttribute("userDepartment", department);
		}
		if (session.getAttribute("userPositionType") == null) {
			session.setAttribute("userPositionType", positionType);
		}
		if (session.getAttribute("userPositionTitle") == null) {
			session.setAttribute("userPositionTitle", positionTitle);
		}
	}

	public void deleteAllSession(HttpServletRequest req) {
		HttpSession session = req.getSession();
		session.removeAttribute("userProfileId");
		session.removeAttribute("gpmsUserName");
		session.removeAttribute("isAdmin");
		session.removeAttribute("userCollege");
		session.removeAttribute("userDepartment");
		session.removeAttribute("userPositionType");
		session.removeAttribute("userPositionTitle");
		session.invalidate();
	}

	public void setMySessionID(HttpServletRequest req, String sessionValue) {
		try {
			if (req == null) {
				System.out.println("Null request in context");
			}
			HttpSession session = req.getSession();
			if (session.getAttribute("userProfileId") == null) {
				// id = System.currentTimeMillis();
				session.setAttribute("userProfileId", sessionValue);
			}
			UserProfile existingUserProfile = new UserProfile();
			if (!sessionValue.equals("null")) {
				ObjectId id = new ObjectId(sessionValue);
				existingUserProfile = findUserDetailsByProfileID(id);
			}
			if (session.getAttribute("gpmsUserName") == null) {
				session.setAttribute("gpmsUserName", existingUserProfile
						.getUserAccount().getUserName());
			}
			if (session.getAttribute("isAdmin") == null) {
				session.setAttribute("isAdmin", existingUserProfile
						.getUserAccount().isAdmin());
			}
			for (PositionDetails userDetails : existingUserProfile.getDetails()) {
				if (userDetails.isAsDefault()) {
					if (session.getAttribute("userPositionType") == null) {
						session.setAttribute("userPositionType",
								userDetails.getPositionType());
					}
					if (session.getAttribute("userPositionTitle") == null) {
						session.setAttribute("userPositionTitle",
								userDetails.getPositionTitle());
					}
					if (session.getAttribute("userDepartment") == null) {
						session.setAttribute("userDepartment",
								userDetails.getDepartment());
					}
					if (session.getAttribute("userCollege") == null) {
						session.setAttribute("userCollege",
								userDetails.getCollege());
					}
				}
			} 
		//	UserTaskPermissionOperations.init();
		//	UserTaskPermissionOperations.populateUsersApprovedTaskSet(existingUserProfile
		//				.getUserAccount().getUserName());
			
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
	}

	public String getMySessionId(HttpServletRequest req) {
		HttpSession session = req.getSession();
		if (session.getAttribute("userProfileId") != null) {
			return (String) session.getAttribute("userProfileId");
		}
		if (session.getAttribute("gpmsUserName") != null) {
			return (String) session.getAttribute("gpmsUserName");
		}
		if (session.getAttribute("userPositionType") != null) {
			return (String) session.getAttribute("userPositionType");
		}
		if (session.getAttribute("userPositionTitle") != null) {
			return (String) session.getAttribute("userPositionTitle");
		}
		if (session.getAttribute("userDepartment") != null) {
			return (String) session.getAttribute("userDepartment");
		}
		if (session.getAttribute("userCollege") != null) {
			return (String) session.getAttribute("userCollege");
		}
		if (session.getAttribute("isAdmin") != null) {
			return (String) session.getAttribute("isAdmin");
		}
		return null;
	}

	/**
	 * Adds User's Active Status
	 * 
	 * @param userID
	 * @param newAccount
	 * @param newProfile
	 * @param existingUserAccount
	 * @param existingUserProfile
	 * @param isActiveUser
	 * @param userInfo
	 * @return
	 */
	public boolean addUserActiveStatus(String userID, UserAccount newAccount,
			UserProfile newProfile, UserAccount existingUserAccount,
			UserProfile existingUserProfile, boolean isActiveUser,
			JsonNode userInfo) {
		if (userInfo != null && userInfo.has("IsActive")) {
			if (!userID.equals("0")) {
				if (existingUserAccount.isActive() != userInfo.get("IsActive")
						.booleanValue()) {
					existingUserAccount.setActive(userInfo.get("IsActive")
							.booleanValue());
					isActiveUser = userInfo.get("IsActive").booleanValue();
				}
			} else {
				newAccount.setActive(userInfo.get("IsActive").booleanValue());
			}
			if (!userID.equals("0")) {
				if (existingUserAccount.isDeleted() != !userInfo
						.get("IsActive").booleanValue()) {
					existingUserAccount.setDeleted(!userInfo.get("IsActive")
							.booleanValue());
				}
			} else {
				newAccount.setDeleted(!userInfo.get("IsActive").booleanValue());
			}
			if (!userID.equals("0")) {
				if (existingUserProfile.isDeleted() != !userInfo
						.get("IsActive").booleanValue()) {
					existingUserProfile.setDeleted(!userInfo.get("IsActive")
							.booleanValue());
				}
			} else {
				newProfile.setDeleted(!userInfo.get("IsActive").booleanValue());
			}
		}
		return isActiveUser;
	}

	/**
	 * Adds General User Info
	 * 
	 * @param userID
	 * @param newProfile
	 * @param existingUserProfile
	 * @param userInfo
	 * @throws ParseException
	 */
	public void addGeneralUserInfo(String userID, UserProfile newProfile,
			UserProfile existingUserProfile, JsonNode userInfo)
			throws ParseException {
		if (userInfo != null && userInfo.has("FirstName")) {
			if (!userID.equals("0")) {
				if (!existingUserProfile.getFirstName().equals(
						userInfo.get("FirstName").textValue())) {
					existingUserProfile.setFirstName(userInfo.get("FirstName")
							.textValue());
				}
			} else {
				newProfile.setFirstName(userInfo.get("FirstName").textValue());
			}
		}
		if (userInfo != null && userInfo.has("MiddleName")) {
			if (!userID.equals("0")) {
				if (!existingUserProfile.getMiddleName().equals(
						userInfo.get("MiddleName").textValue())) {
					existingUserProfile.setMiddleName(userInfo
							.get("MiddleName").textValue());
				}
			} else {
				newProfile
						.setMiddleName(userInfo.get("MiddleName").textValue());
			}
		}
		if (userInfo != null && userInfo.has("LastName")) {
			if (!userID.equals("0")) {
				if (!existingUserProfile.getLastName().equals(
						userInfo.get("LastName").textValue())) {
					existingUserProfile.setLastName(userInfo.get("LastName")
							.textValue());
				}
			} else {
				newProfile.setLastName(userInfo.get("LastName").textValue());
			}
		}
		if (userInfo != null && userInfo.has("DOB")) {
			Date dob = formatter.parse(userInfo.get("DOB").textValue());
			if (!userID.equals("0")) {
				if (!existingUserProfile.getDateOfBirth().equals(dob)) {
					existingUserProfile.setDateOfBirth(dob);
				}
			} else {
				newProfile.setDateOfBirth(dob);
			}
		}
		if (userInfo != null && userInfo.has("Gender")) {
			if (!userID.equals("0")) {
				if (!existingUserProfile.getGender().equals(
						userInfo.get("Gender").textValue())) {
					existingUserProfile.setGender(userInfo.get("Gender")
							.textValue());
				}
			} else {
				newProfile.setGender(userInfo.get("Gender").textValue());
			}
		}
	}

	/**
	 * Adds Admin User's Details
	 * 
	 * @param userID
	 * @param newProfile
	 * @param existingUserProfile
	 * @param userInfo
	 */
	public void addAdminUserDetails(String userID, UserProfile newProfile,
			UserProfile existingUserProfile, JsonNode userInfo) {
		PositionDetails newDetails = new PositionDetails();
		newDetails.setPositionType("University administrator");
		newDetails.setPositionTitle(userInfo.get("positionTitle").textValue());
		newDetails.setAsDefault(true);
		if (!userID.equals("0")) {
			existingUserProfile.getDetails().clear();
			existingUserProfile.getDetails().add(newDetails);
		} else {
			newProfile.getDetails().add(newDetails);
		}
	}

	/**
	 * Adds User's Position Details
	 * 
	 * @param userID
	 * @param newProfile
	 * @param existingUserProfile
	 * @param userInfo
	 */
	public void addUserPositionDetails(String userID, UserProfile newProfile,
			UserProfile existingUserProfile, JsonNode userInfo) {
		if (!userID.equals("0")) {
			existingUserProfile.getDetails().clear();
		}
		String[] rows = userInfo.get("SaveOptions").textValue().split("#!#");
		for (String col : rows) {
			String[] cols = col.split("!#!");
			PositionDetails newDetails = new PositionDetails();
			newDetails.setCollege(cols[0]);
			newDetails.setDepartment(cols[1]);
			newDetails.setPositionType(cols[2]);
			newDetails.setPositionTitle(cols[3]);
			newDetails.setAsDefault(Boolean.parseBoolean(cols[4]));
			if (!userID.equals("0")) {
				existingUserProfile.getDetails().add(newDetails);
			} else {
				newProfile.getDetails().add(newDetails);
			}
		}
	}

	/**
	 * Adds User's Email Details
	 * 
	 * @param userID
	 * @param newProfile
	 * @param existingUserProfile
	 * @param userInfo
	 */
	public void addEmailDetails(String userID, UserProfile newProfile,
			UserProfile existingUserProfile, JsonNode userInfo) {
		if (userInfo != null && userInfo.has("WorkEmail")) {
			if (!userID.equals("0")) {
				boolean alreadyExist = false;
				for (String workEmail : existingUserProfile.getWorkEmails()) {
					if (workEmail.equals(userInfo.get("WorkEmail").textValue())) {
						alreadyExist = true;
						break;
					}
				}
				if (!alreadyExist) {
					existingUserProfile.getWorkEmails().clear();
					existingUserProfile.getWorkEmails().add(
							userInfo.get("WorkEmail").textValue());
				}
			} else {
				newProfile.getWorkEmails().add(
						userInfo.get("WorkEmail").textValue());
			}
		}
		if (userInfo != null && userInfo.has("PersonalEmail")) {
			if (!userID.equals("0")) {
				boolean alreadyExist = false;
				for (String personalEmail : existingUserProfile
						.getPersonalEmails()) {
					if (personalEmail.equals(userInfo.get("PersonalEmail")
							.textValue())) {
						alreadyExist = true;
						break;
					}
				}
				if (!alreadyExist) {
					existingUserProfile.getPersonalEmails().clear();
					existingUserProfile.getPersonalEmails().add(
							userInfo.get("PersonalEmail").textValue());
				}
			} else {
				newProfile.getPersonalEmails().add(
						userInfo.get("PersonalEmail").textValue());
			}
		}
	}

	/**
	 * Adds User's Phone Number Details
	 * 
	 * @param userID
	 * @param newProfile
	 * @param existingUserProfile
	 * @param userInfo
	 */
	public void addPhoneNumberDetails(String userID, UserProfile newProfile,
			UserProfile existingUserProfile, JsonNode userInfo) {
		if (userInfo != null && userInfo.has("OfficeNumber")) {
			if (!userID.equals("0")) {
				boolean alreadyExist = false;
				for (String officeNo : existingUserProfile.getOfficeNumbers()) {
					if (officeNo.equals(userInfo.get("OfficeNumber")
							.textValue())) {
						alreadyExist = true;
						break;
					}
				}
				if (!alreadyExist) {
					existingUserProfile.getOfficeNumbers().clear();
					existingUserProfile.getOfficeNumbers().add(
							userInfo.get("OfficeNumber").textValue());
				}
			} else {
				newProfile.getOfficeNumbers().add(
						userInfo.get("OfficeNumber").textValue());
			}
		}
		if (userInfo != null && userInfo.has("MobileNumber")) {
			if (!userID.equals("0")) {
				boolean alreadyExist = false;
				for (String mobileNo : existingUserProfile.getMobileNumbers()) {
					if (mobileNo.equals(userInfo.get("MobileNumber")
							.textValue())) {
						alreadyExist = true;
						break;
					}
				}
				if (!alreadyExist) {
					existingUserProfile.getMobileNumbers().clear();
					existingUserProfile.getMobileNumbers().add(
							userInfo.get("MobileNumber").textValue());
				}
			} else {
				newProfile.getMobileNumbers().add(
						userInfo.get("MobileNumber").textValue());
			}
		}
		if (userInfo != null && userInfo.has("HomeNumber")) {
			if (!userID.equals("0")) {
				boolean alreadyExist = false;
				for (String homeNo : existingUserProfile.getHomeNumbers()) {
					if (homeNo.equals(userInfo.get("HomeNumber").textValue())) {
						alreadyExist = true;
						break;
					}
				}
				if (!alreadyExist) {
					existingUserProfile.getHomeNumbers().clear();
					existingUserProfile.getHomeNumbers().add(
							userInfo.get("HomeNumber").textValue());
				}
			} else {
				newProfile.getHomeNumbers().add(
						userInfo.get("HomeNumber").textValue());
			}
		}
		if (userInfo != null && userInfo.has("OtherNumber")) {
			if (!userID.equals("0")) {
				boolean alreadyExist = false;
				for (String otherNo : existingUserProfile.getOtherNumbers()) {
					if (otherNo.equals(userInfo.get("OtherNumber").textValue())) {
						alreadyExist = true;
						break;
					}
				}
				if (!alreadyExist) {
					existingUserProfile.getOtherNumbers().clear();
					existingUserProfile.getOtherNumbers().add(
							userInfo.get("OtherNumber").textValue());
				}
			} else {
				newProfile.getOtherNumbers().add(
						userInfo.get("OtherNumber").textValue());
			}
		}
	}

	/**
	 * Adds User's Address Details
	 * 
	 * @param userID
	 * @param newProfile
	 * @param existingUserProfile
	 * @param userInfo
	 */
	public void addAddressDetails(String userID, UserProfile newProfile,
			UserProfile existingUserProfile, JsonNode userInfo) {
		Address newAddress = new Address();
		if (userInfo != null && userInfo.has("Street")) {
			newAddress.setStreet(userInfo.get("Street").textValue());
		}
		if (userInfo != null && userInfo.has("Apt")) {
			newAddress.setApt(userInfo.get("Apt").textValue());
		}
		if (userInfo != null && userInfo.has("City")) {
			newAddress.setCity(userInfo.get("City").textValue());
		}
		if (userInfo != null && userInfo.has("State")) {
			newAddress.setState(userInfo.get("State").textValue());
		}
		if (userInfo != null && userInfo.has("Zip")) {
			newAddress.setZipcode(userInfo.get("Zip").textValue());
		}
		if (userInfo != null && userInfo.has("Country")) {
			newAddress.setCountry(userInfo.get("Country").textValue());
		}
		if (!userID.equals("0")) {
			boolean alreadyExist = false;
			for (Address address : existingUserProfile.getAddresses()) {
				if (address.equals(newAddress)) {
					alreadyExist = true;
					break;
				}
			}
			if (!alreadyExist) {
				existingUserProfile.getAddresses().clear();
				existingUserProfile.getAddresses().add(newAddress);
			}
		} else {
			newProfile.getAddresses().add(newAddress);
		}
	}

	/**
	 * Exports to Excel File for Users and Admin Users
	 * 
	 * @param users
	 * @return
	 * @throws URISyntaxException
	 * @throws JsonProcessingException
	 */
	public String exportToExcelFile(List<UserInfo> users)
			throws URISyntaxException, JsonProcessingException {
		ObjectMapper mapper = new ObjectMapper();
		String filename = new String();
		Xcelite xcelite = new Xcelite();
		XceliteSheet sheet = xcelite.createSheet("Users");
		SheetWriter<UserInfo> writer = sheet.getBeanWriter(UserInfo.class);
		writer.write(users);
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy_MM_dd");
		Date date = new Date();
		String fileName = String.format(
				"%s.%s",
				RandomStringUtils.randomAlphanumeric(8) + "_"
						+ dateFormat.format(date), "xlsx");
		String downloadLocation = this.getClass().getResource("/uploads")
				.toURI().getPath();
		xcelite.write(new File(downloadLocation + fileName));
		filename = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(
				fileName);
		return filename;
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

	// Audit Log Functions

	/**
	 * Gets the Recent User Profile AuditLog
	 * 
	 * @param userProfile
	 * @param user
	 * @return
	 */
	private UserInfo getRecentUserProfileAuditLog(UserProfile userProfile,
			UserInfo user) {
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
		return user;
	}

	/***
	 * Finds All Logs in a AuditLog Grid for a User
	 * 
	 * @param offset
	 * @param limit
	 * @param id
	 * @param auditLogInfo
	 * @return
	 * @throws ParseException
	 */
	public List<AuditLogInfo> findAllProposalAuditLogForGrid(int offset,
			int limit, ObjectId id, AuditLogCommonInfo auditLogInfo)
			throws ParseException {
		return getAuditListBasedOnPaging(offset, limit,
				getSortedAuditLogResults(auditLogInfo, id));
	}

	/***
	 * Gets Audit Logs list based On User provided Paging size
	 * 
	 * @param offset
	 * @param limit
	 * @param allAuditLogs
	 * @return
	 */
	private List<AuditLogInfo> getAuditListBasedOnPaging(int offset, int limit,
			List<AuditLogInfo> allAuditLogs) {
		int rowTotal = 0;
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

	/***
	 * Gets Sorted Audit Logs List
	 * 
	 * @param auditLogInfo
	 * @param id
	 * @return
	 * @throws ParseException
	 */
	public List<AuditLogInfo> getSortedAuditLogResults(
			AuditLogCommonInfo auditLogInfo, ObjectId id) throws ParseException {
		Datastore ds = getDatastore();
		Query<UserProfile> profileQuery = ds.createQuery(UserProfile.class);
		UserProfile q = profileQuery.field("_id").equal(id).get();
		List<AuditLogInfo> allAuditLogs = new ArrayList<AuditLogInfo>();
		if (q.getAuditLog() != null && q.getAuditLog().size() != 0) {
			for (AuditLog userProfileAudit : q.getAuditLog()) {
				AuditLogInfo proposalAuditLog = new AuditLogInfo();
				boolean isActionMatch = isAuditLogActionFieldProvided(
						auditLogInfo.getAction(), userProfileAudit);
				boolean isAuditedByMatch = isAuditLogAuditedByFieldProvided(
						auditLogInfo.getAuditedBy(), userProfileAudit);
				boolean isActivityDateFromMatch = isAuditLogActivityDateFromProvided(
						auditLogInfo.getActivityOnFrom(), userProfileAudit);
				boolean isActivityDateToMatch = isAuditLogActivityDateToProvided(
						auditLogInfo.getActivityOnTo(), userProfileAudit);

				if (isActionMatch && isAuditedByMatch
						&& isActivityDateFromMatch && isActivityDateToMatch) {
					proposalAuditLog.setUserName(userProfileAudit
							.getUserProfile().getUserAccount().getUserName());
					proposalAuditLog.setUserFullName(userProfileAudit
							.getUserProfile().getFullName());
					proposalAuditLog.setAction(userProfileAudit.getAction());
					proposalAuditLog.setActivityDate(userProfileAudit
							.getActivityDate());
					allAuditLogs.add(proposalAuditLog);
				}
			}
		}
		Collections.sort(allAuditLogs);
		return allAuditLogs;
	}

	/***
	 * Is Audit Log Activity Date To Provided
	 * 
	 * @param activityOnTo
	 * @param userProfileAudit
	 * @return
	 * @throws ParseException
	 */
	private boolean isAuditLogActivityDateToProvided(String activityOnTo,
			AuditLog userProfileAudit) throws ParseException {
		if (activityOnTo != null) {
			Date activityDateTo = formatter.parse(activityOnTo);
			if (userProfileAudit.getActivityDate().compareTo(activityDateTo) > 0) {
				return false;
			} else if (userProfileAudit.getActivityDate().compareTo(
					activityDateTo) < 0) {
				return true;
			} else if (userProfileAudit.getActivityDate().compareTo(
					activityDateTo) == 0) {
				return true;
			}
		} else {
			return true;
		}
		return false;
	}

	/***
	 * Is Audit Log Activity Date From Provided
	 * 
	 * @param activityOnFrom
	 * @param userProfileAudit
	 * @return
	 * @throws ParseException
	 */
	private boolean isAuditLogActivityDateFromProvided(String activityOnFrom,
			AuditLog userProfileAudit) throws ParseException {
		if (activityOnFrom != null) {
			Date activityDateFrom = formatter.parse(activityOnFrom);
			if (userProfileAudit.getActivityDate().compareTo(activityDateFrom) > 0) {
				return true;
			} else if (userProfileAudit.getActivityDate().compareTo(
					activityDateFrom) < 0) {
				return false;
			} else if (userProfileAudit.getActivityDate().compareTo(
					activityDateFrom) == 0) {
				return true;
			}
		} else {
			return true;
		}
		return false;
	}

	/***
	 * Is Audit Log Audited By Provided
	 * 
	 * @param auditedBy
	 * @param userProfileAudit
	 * @return
	 */
	private boolean isAuditLogAuditedByFieldProvided(String auditedBy,
			AuditLog userProfileAudit) {
		if (auditedBy != null) {
			if (userProfileAudit.getUserProfile().getUserAccount()
					.getUserName().toLowerCase()
					.contains(auditedBy.toLowerCase())) {
				return true;
			} else if (userProfileAudit.getUserProfile().getFirstName()
					.toLowerCase().contains(auditedBy.toLowerCase())) {
				return true;
			} else if (userProfileAudit.getUserProfile().getMiddleName()
					.toLowerCase().contains(auditedBy.toLowerCase())) {
				return true;
			} else if (userProfileAudit.getUserProfile().getLastName()
					.toLowerCase().contains(auditedBy.toLowerCase())) {
				return true;
			}
		} else {
			return true;
		}
		return false;
	}

	/***
	 * Is Audit Log Action Provided
	 * 
	 * @param action
	 * @param userProfileAudit
	 * @return
	 */
	private boolean isAuditLogActionFieldProvided(String action,
			AuditLog userProfileAudit) {
		if (action != null) {
			if (userProfileAudit.getAction().toLowerCase()
					.contains(action.toLowerCase())) {
				return true;
			}
		} else {
			return true;
		}
		return false;
	}
}
