package gpms.dao;

import gpms.DAL.MongoDBConnector;
import gpms.dataModel.AuditLog;
import gpms.dataModel.AuditLogCommonInfo;
import gpms.dataModel.AuditLogInfo;
import gpms.dataModel.Delegation;
import gpms.dataModel.DelegationCommonInfo;
import gpms.dataModel.DelegationInfo;
import gpms.dataModel.GPMSCommonInfo;
import gpms.dataModel.UserAccount;
import gpms.dataModel.UserDetail;
import gpms.dataModel.UserProfile;
//import gpms.rest.DelegationService;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang3.RandomStringUtils;
import org.bson.types.ObjectId;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.Morphia;
import org.mongodb.morphia.dao.BasicDAO;
import org.mongodb.morphia.query.Query;
//import org.wso2.balana.ctx.AbstractResult;
//import org.wso2.balana.ctx.AttributeAssignment;
//import org.wso2.balana.xacml3.Advice;
import org.xml.sax.SAXException;

import com.ebay.xcelite.Xcelite;
import com.ebay.xcelite.sheet.XceliteSheet;
import com.ebay.xcelite.writer.SheetWriter;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.MongoClient;
import com.mongodb.MongoException;

public class DelegationDAO extends BasicDAO<Delegation, String> {
	private static final String DBNAME = "db_gpms";
	public static final String COLLECTION_NAME = "delegation";

	private static Morphia morphia;
	private static Datastore ds;
	private AuditLog audit = new AuditLog();
	private DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");

	private static Morphia getMorphia() throws UnknownHostException,
			MongoException {
		if (morphia == null) {
			morphia = new Morphia().map(Delegation.class)
					.map(UserAccount.class);
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

	public DelegationDAO(MongoClient mongo, Morphia morphia, String dbName) {
		super(mongo, morphia, dbName);
	}

	public List<DelegationInfo> findAllUserDelegationsForGrid(int offset,
			int limit, DelegationCommonInfo delegationInfo,
			GPMSCommonInfo userInfo) throws ParseException {
		Datastore ds = getDatastore();
		String delegatee = delegationInfo.getDelegatee();
		String createdFrom = delegationInfo.getCreatedFrom();
		String createdTo = delegationInfo.getCreatedTo();
		String delegatedAction = delegationInfo.getDelegatedAction();
		Boolean isRevoked = delegationInfo.getIsRevoked();
		String delegatorID = userInfo.getUserProfileID();
		String delegatedCollege = userInfo.getUserCollege();
		String delegatedDepartment = userInfo.getUserDepartment();
		String delegatedPositionType = userInfo.getUserPositionType();
		String delegatedPositionTitle = userInfo.getUserPositionTitle();
		List<DelegationInfo> delegations = new ArrayList<DelegationInfo>();
		Query<Delegation> delegationQuery = ds.createQuery(Delegation.class);
		// For Filter in Grid
		if (delegatorID != null) {
			delegationQuery.criteria("delegator user id").equal(delegatorID);
		}
		if (delegatedCollege != null) {
			delegationQuery.criteria("delegated college").equal(
					delegatedCollege);
		}
		if (delegatedDepartment != null) {
			delegationQuery.criteria("delegated department").equal(
					delegatedDepartment);
		}
		if (delegatedPositionType != null) {
			delegationQuery.criteria("delegated position type").equal(
					delegatedPositionType);
		}
		if (delegatedPositionTitle != null) {
			delegationQuery.criteria("delegated position title").equal(
					delegatedPositionTitle);
		}
		if (delegatee != null) {
			delegationQuery.criteria("delegatee").containsIgnoreCase(delegatee);
		}
		if (createdFrom != null && !createdFrom.isEmpty()) {
			Date delegatedStartsFrom = formatter.parse(createdFrom);
			delegationQuery.criteria("created on").greaterThanOrEq(
					delegatedStartsFrom);
		}
		if (createdTo != null && !createdTo.isEmpty()) {
			Date delegatedStartsTo = formatter.parse(createdTo);
			delegationQuery.criteria("created on").lessThanOrEq(
					delegatedStartsTo);
		}
		if (delegatedAction != null) {
			delegationQuery.criteria("actions").contains(delegatedAction);
		}
		if (isRevoked != null) {
			delegationQuery.criteria("revoked").equal(isRevoked);
		}
		int rowTotal = delegationQuery.asList().size();
		List<Delegation> allDelegations = delegationQuery.offset(offset - 1)
				.limit(limit).order("-audit log.activity on").asList();

		for (Delegation userDelegation : allDelegations) {
			DelegationInfo delegation = generateDelegationInfo(rowTotal,
					userDelegation);
			delegations.add(delegation);
		}
		return delegations;
	}

	/**
	 * Generates Delegation Info
	 * 
	 * @param rowTotal
	 * @param userDelegation
	 * @return
	 */
	private DelegationInfo generateDelegationInfo(int rowTotal,
			Delegation userDelegation) {
		DelegationInfo delegation = new DelegationInfo();
		delegation.setRowTotal(rowTotal);
		delegation.setId(userDelegation.getId().toString());
		delegation.setDelegatee(userDelegation.getDelegatee());
		delegation.setDelegateeEmail(userDelegation.getDelegateeEmail());
		delegation.setDelegateePositionTitle(userDelegation
				.getDelegateePositionTitle());
		delegation.setPositionTitle(userDelegation.getDelegatedPositionTitle());
		delegation.setDelegatedActions(userDelegation.getActions());
		delegation.setDelegationReason(userDelegation.getReason());
		delegation.setDateCreated(userDelegation.getCreatedOn());
		delegation.setDelegatedFrom(userDelegation.getFrom());
		delegation.setDelegatedTo(userDelegation.getTo());
		Date lastAudited = null;
		String lastAuditedBy = new String();
		String lastAuditAction = new String();
		int auditLogCount = userDelegation.getAuditLog().size();
		if (userDelegation.getAuditLog() != null && auditLogCount != 0) {
			AuditLog auditLog = userDelegation.getAuditLog().get(
					auditLogCount - 1);
			lastAudited = auditLog.getActivityDate();
			lastAuditedBy = auditLog.getUserProfile().getFullName();
			lastAuditAction = auditLog.getAction();
		}
		delegation.setLastAudited(lastAudited);
		delegation.setLastAuditedBy(lastAuditedBy);
		delegation.setLastAuditAction(lastAuditAction);
		delegation.setRevoked(userDelegation.isRevoked());
		return delegation;
	}

	public List<DelegationInfo> findAllUserDelegationsForExport(
			DelegationCommonInfo delegationInfo, GPMSCommonInfo userInfo)
			throws ParseException {
		Datastore ds = getDatastore();
		String delegatee = delegationInfo.getDelegatee();
		String createdFrom = delegationInfo.getCreatedFrom();
		String createdTo = delegationInfo.getCreatedTo();
		String delegatedAction = delegationInfo.getDelegatedAction();
		Boolean isRevoked = delegationInfo.getIsRevoked();
		String delegatorID = userInfo.getUserProfileID();
		String delegatedCollege = userInfo.getUserCollege();
		String delegatedDepartment = userInfo.getUserDepartment();
		String delegatedPositionType = userInfo.getUserPositionType();
		String delegatedPositionTitle = userInfo.getUserPositionTitle();
		List<DelegationInfo> delegations = new ArrayList<DelegationInfo>();
		Query<Delegation> delegationQuery = ds.createQuery(Delegation.class);
		if (delegatorID != null) {
			delegationQuery.criteria("delegator user id").equal(delegatorID);
		}
		if (delegatedCollege != null) {
			delegationQuery.criteria("delegated college").equal(
					delegatedCollege);
		}
		if (delegatedDepartment != null) {
			delegationQuery.criteria("delegated department").equal(
					delegatedDepartment);
		}
		if (delegatedPositionType != null) {
			delegationQuery.criteria("delegated position type").equal(
					delegatedPositionType);
		}
		if (delegatedPositionTitle != null) {
			delegationQuery.criteria("delegated position title").equal(
					delegatedPositionTitle);
		}
		if (delegatee != null) {
			delegationQuery.criteria("delegatee").containsIgnoreCase(delegatee);
		}
		if (createdFrom != null && !createdFrom.isEmpty()) {
			Date delegatedStartsFrom = formatter.parse(createdFrom);
			delegationQuery.criteria("created on").greaterThanOrEq(
					delegatedStartsFrom);
		}
		if (createdTo != null && !createdTo.isEmpty()) {
			Date delegatedStartsTo = formatter.parse(createdTo);
			delegationQuery.criteria("created on").lessThanOrEq(
					delegatedStartsTo);
		}
		if (delegatedAction != null) {
			delegationQuery.criteria("actions").contains(delegatedAction);
		}
		if (isRevoked != null) {
			delegationQuery.criteria("revoked").equal(isRevoked);
		}
		int rowTotal = delegationQuery.asList().size();
		List<Delegation> allDelegations = delegationQuery.order(
				"-audit log.activity on").asList();
		for (Delegation userDelegation : allDelegations) {
			DelegationInfo delegation = generateDelegationInfo(rowTotal,
					userDelegation);

			delegations.add(delegation);
		}
		return delegations;
	}

	public Delegation findDelegationByDelegationID(ObjectId id) {
		Datastore ds = getDatastore();
		return ds.createQuery(Delegation.class).field("_id").equal(id).get();
	}

	/**
	 * Saves Delegation
	 * 
	 * @param userInfo
	 * @param authorProfile
	 * @param delegatorName
	 * @param newDelegation
	 * @return
	 * @throws SAXException
	 * @throws IOException
	 * @throws JsonProcessingException
	 */
//	public void saveDelegation(GPMSCommonInfo userInfo,
//			UserProfile authorProfile, String delegatorName,
//			Delegation newDelegation) throws SAXException, IOException,
//			JsonProcessingException {
//		String policyId = new String();
//		policyId = createDynamicPolicy(userInfo.getUserProfileID(),
//				delegatorName, DelegationService.policyLocation, newDelegation);
//		newDelegation.setDelegationPolicyId(policyId);
//		saveDelegation(newDelegation, authorProfile);
//	}

	public void saveDelegation(Delegation newDelegation,
			UserProfile authorProfile) {
		Datastore ds = getDatastore();
		audit = new AuditLog(authorProfile, "Created delegation by "
				+ authorProfile.getUserAccount().getUserName(), new Date());
		newDelegation.getAuditLog().add(audit);
		ds.save(newDelegation);
	}

	/**
	 * Updates Delegation
	 * 
	 * @param userInfo
	 * @param authorProfile
	 * @param delegatorName
	 * @param existingDelegation
	 * @throws SAXException
	 * @throws IOException
	 */
//	public void updateDelegation(GPMSCommonInfo userInfo,
//			UserProfile authorProfile, String delegatorName,
//			Delegation existingDelegation) throws SAXException, IOException {
//		String policyId = new String();
//		policyId = createDynamicPolicy(userInfo.getUserProfileID(),
//				delegatorName, DelegationService.policyLocation,
//				existingDelegation);
//		existingDelegation.setDelegationPolicyId(policyId);
//		updateDelegation(existingDelegation, authorProfile);
//	}

	public void updateDelegation(Delegation existingDelegation,
			UserProfile authorProfile) {
		try {
			Datastore ds = getDatastore();
			audit = new AuditLog(authorProfile, "Updated delegation by "
					+ authorProfile.getUserAccount().getUserName(), new Date());
			existingDelegation.getAuditLog().add(audit);
			ds.save(existingDelegation);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public boolean revokeDelegation(Delegation existingDelegation,
			UserProfile authorProfile) {
		try {
			Datastore ds = getDatastore();
			audit = new AuditLog(authorProfile, "Revoked delegation by "
					+ authorProfile.getUserAccount().getUserName(), new Date());
			existingDelegation.getAuditLog().add(audit);
			existingDelegation.setRevoked(true);
			ds.save(existingDelegation);
			return true;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	/**
	 * Generates Delegation Details
	 *
	 * @param userInfo
	 * @param delegationID
	 * @param newDelegation
	 * @param existingDelegation
	 * @param delegationInfo
	 * @throws Exception
	 * @throws ParseException
	 */
	public void generateDelegationDetails(GPMSCommonInfo userInfo,
			String delegationID, UserProfile delegateeProfile,
			Delegation newDelegation, Delegation existingDelegation,
			JsonNode delegationInfo) throws Exception, ParseException {
		addDelegatedActions(delegationID, newDelegation, existingDelegation,
				delegationInfo);
		if (delegateeProfile != null) {
			addDelegateeInfo(userInfo, delegationID, delegateeProfile,
					newDelegation, delegationInfo);
		}
		addDelegationDuration(delegationID, newDelegation, existingDelegation,
				delegationInfo);
		addDelegationReason(delegationID, newDelegation, existingDelegation,
				delegationInfo);
	}

	/**
	 * @param delegationID
	 * @param newDelegation
	 * @param existingDelegation
	 * @param delegationInfo
	 * @throws Exception
	 */
	public void addDelegationReason(String delegationID,
			Delegation newDelegation, Delegation existingDelegation,
			JsonNode delegationInfo) throws Exception {
		if (delegationInfo != null && delegationInfo.has("DelegationReason")) {
			final String reason = delegationInfo.get("DelegationReason")
					.textValue().trim().replaceAll("\\<[^>]*>", "");
			if (validateNotEmptyValue(reason)) {
				if (!delegationID.equals("0")) {
					if (!existingDelegation.getReason().equals(
							delegationInfo.get("DelegationReason").textValue())) {
						existingDelegation.setReason(reason);
					}
				} else {
					newDelegation.setReason(reason);
				}
			} else {
				throw new Exception("The Delegation Reason can not be Empty");
			}
		}
	}

	/**
	 * Adds Delegatee Info
	 * 
	 * @param userInfo
	 * @param delegationID
	 * @param newDelegation
	 * @param delegationInfo
	 * @throws Exception
	 */
	public void addDelegateeInfo(GPMSCommonInfo userInfo, String delegationID,
			UserProfile delegateeProfile, Delegation newDelegation,
			JsonNode delegationInfo) throws Exception {
		if (delegationInfo != null && delegationInfo.has("DelegateeId")) {
			String delegateeId = delegationInfo.get("DelegateeId").textValue();
			if (delegationID.equals("0")) {
				newDelegation.setDelegateeId(delegateeId);
				newDelegation.setDelegateeUsername(delegateeProfile
						.getUserAccount().getUserName());
				newDelegation.setDelegateeEmail(delegateeProfile
						.getWorkEmails().get(0));
				newDelegation.setDelegatedCollege(userInfo.getUserCollege());
				newDelegation.setDelegatedDepartment(userInfo
						.getUserDepartment());
				newDelegation.setDelegatedPositionType(userInfo
						.getUserPositionType());
				newDelegation.setDelegatedPositionTitle(userInfo
						.getUserPositionTitle());
			}
		}
		if (delegationInfo != null && delegationInfo.has("Delegatee")) {
			final String delegatee = delegationInfo.get("Delegatee")
					.textValue().trim().replaceAll("\\<[^>]*>", "");
			if (validateNotEmptyValue(delegatee) && delegationID.equals("0")) {
				newDelegation.setDelegatee(delegatee);
			} else {
				throw new Exception("The Delegatee can not be Empty");
			}
		}
		if (delegationInfo != null && delegationInfo.has("DelegateeCollege")) {
			final String college = delegationInfo.get("DelegateeCollege")
					.textValue().trim().replaceAll("\\<[^>]*>", "");
			if (validateNotEmptyValue(college) && delegationID.equals("0")) {
				newDelegation.setDelegateeCollege(college);
			}
		}
		if (delegationInfo != null && delegationInfo.has("DelegateeDepartment")) {
			final String department = delegationInfo.get("DelegateeDepartment")
					.textValue().trim().replaceAll("\\<[^>]*>", "");
			if (validateNotEmptyValue(department) && delegationID.equals("0")) {
				newDelegation.setDelegateeDepartment(department);
			}
		}
		if (delegationInfo != null
				&& delegationInfo.has("DelegateePositionType")) {
			final String positionType = delegationInfo
					.get("DelegateePositionType").textValue().trim()
					.replaceAll("\\<[^>]*>", "");
			if (validateNotEmptyValue(positionType) && delegationID.equals("0")) {
				newDelegation.setDelegateePositionType(positionType);
			}
		}
		if (delegationInfo != null
				&& delegationInfo.has("DelegateePositionTitle")) {
			final String positionTitle = delegationInfo
					.get("DelegateePositionTitle").textValue().trim()
					.replaceAll("\\<[^>]*>", "");
			if (validateNotEmptyValue(positionTitle)) {
				if (delegationID.equals("0")) {
					newDelegation.setDelegateePositionTitle(positionTitle);
				}
			}
		}
	}

	/**
	 * Adds Delegated Actions
	 * 
	 * @param delegationID
	 * @param newDelegation
	 * @param existingDelegation
	 * @param delegationInfo
	 * @throws Exception
	 */
	public void addDelegatedActions(String delegationID,
			Delegation newDelegation, Delegation existingDelegation,
			JsonNode delegationInfo) throws Exception {
		if (delegationInfo != null && delegationInfo.has("DelegatedAction")) {
			final JsonNode delegatedActions = delegationInfo
					.get("DelegatedAction");
			if (delegatedActions.isArray()) {
				if (delegatedActions.size() > 0) {
					if (delegationID.equals("0")) {
						for (final JsonNode action : delegatedActions) {
							newDelegation.getActions().add(action.textValue());
						}
					} else {
						existingDelegation.getActions().clear();
						for (final JsonNode action : delegatedActions) {
							existingDelegation.getActions().add(
									action.textValue());
						}
					}
				} else {
					throw new Exception(
							"The Delegation Action can not be Empty");
				}
			}
		}
	}

	/**
	 * Adds Delegation Duration
	 * 
	 * @param delegationID
	 * @param newDelegation
	 * @param existingDelegation
	 * @param delegationInfo
	 * @throws ParseException
	 * @throws Exception
	 */
	public void addDelegationDuration(String delegationID,
			Delegation newDelegation, Delegation existingDelegation,
			JsonNode delegationInfo) throws ParseException, Exception {
		if (delegationInfo != null && delegationInfo.has("DelegationFrom")) {
			Date fromDate = formatter.parse(delegationInfo
					.get("DelegationFrom").textValue().trim()
					.replaceAll("\\<[^>]*>", ""));
			if (validateNotEmptyValue(fromDate.toString())) {
				if (!delegationID.equals("0")) {
					if (!existingDelegation.getFrom().equals(
							delegationInfo.get("DelegationFrom").textValue())) {
						existingDelegation.setFrom(fromDate);
					}
				} else {
					newDelegation.setFrom(fromDate);
				}
			} else {
				throw new Exception(
						"The Delegation Start From Date can not be Empty");
			}
		}
		if (delegationInfo != null && delegationInfo.has("DelegationTo")) {
			Date toDate = formatter.parse(delegationInfo.get("DelegationTo")
					.textValue().trim().replaceAll("\\<[^>]*>", ""));
			if (validateNotEmptyValue(toDate.toString())) {
				if (!delegationID.equals("0")) {
					if (!existingDelegation.getTo().equals(
							delegationInfo.get("DelegationTo").textValue())) {
						existingDelegation.setTo(toDate);
					}
				} else {
					newDelegation.setTo(toDate);
				}
			} else {
				throw new Exception("The Delegation End Date can not be Empty");
			}
		}
	}

	public boolean validateNotEmptyValue(String value) {
		if (!value.equalsIgnoreCase("")) {
			return true;
		} else {
			return false;
		}
	}

	public String createDynamicPolicy(String delegatorId, String delegatorName,
			String policyLocation, Delegation existingDelegation)
			throws SAXException, IOException {
		return null;
	}

//	/**
//	 * Gets Delegable User Details From Advice response
//	 * 
//	 * @param userDetails
//	 *            UserDetail
//	 * @param result
//	 */
//	public void getDelegableUserDetailsFromAdvice(List<UserDetail> userDetails,
//			AbstractResult result) {
//		List<Advice> advices = result.getAdvices();
//		for (Advice advice : advices) {
//			if (advice instanceof org.wso2.balana.xacml3.Advice) {
//				UserDetail userDeatil = new UserDetail();
//				List<AttributeAssignment> assignments = ((org.wso2.balana.xacml3.Advice) advice)
//						.getAssignments();
//				for (AttributeAssignment assignment : assignments) {
//					switch (assignment.getAttributeId().toString()) {
//					case "userId":
//						userDeatil.setUserProfileId(assignment.getContent());
//						break;
//					case "userfullName":
//						userDeatil.setFullName(assignment.getContent());
//						break;
//					case "userName":
//						userDeatil.setUserName(assignment.getContent());
//						break;
//					case "userEmail":
//						userDeatil.setEmail(assignment.getContent());
//						break;
//					case "userCollege":
//						userDeatil.setCollege(assignment.getContent());
//						break;
//					case "userDepartment":
//						userDeatil.setDepartment(assignment.getContent());
//						break;
//					case "userPositionType":
//						userDeatil.setPositionType(assignment.getContent());
//						break;
//					case "userPositionTitle":
//						userDeatil.setPositionTitle(assignment.getContent());
//						break;
//					default:
//						break;
//					}
//				}
//				userDetails.add(userDeatil);
//			}
//		}
//	}

	/**
	 * Generates Content Profile for Admin Users
	 * 
	 * @param userProfileID
	 *            User Profile Id
	 * @param userCollege
	 *            User's College
	 * @param userDepartment
	 *            User's Department
	 * @return
	 * @throws UnknownHostException
	 */
	public StringBuffer generateContentProfile(List<UserDetail> delegableUsers)
			throws UnknownHostException {
		StringBuffer contentProfile = new StringBuffer();
		contentProfile.append("<Content>");
		contentProfile.append("<ak:record xmlns:ak=\"http://akpower.org\">");
		for (UserDetail userDetail : delegableUsers) {
			contentProfile.append("<ak:user>");
			contentProfile.append("<ak:userid>");
			contentProfile.append(userDetail.getUserProfileId());
			contentProfile.append("</ak:userid>");
			contentProfile.append("<ak:fullname>");
			contentProfile.append(userDetail.getFullName());
			contentProfile.append("</ak:fullname>");
			contentProfile.append("<ak:username>");
			contentProfile.append(userDetail.getUserName());
			contentProfile.append("</ak:username>");
			contentProfile.append("<ak:email>");
			contentProfile.append(userDetail.getEmail());
			contentProfile.append("</ak:email>");
			contentProfile.append("<ak:college>");
			contentProfile.append(userDetail.getCollege());
			contentProfile.append("</ak:college>");
			contentProfile.append("<ak:department>");
			contentProfile.append(userDetail.getDepartment());
			contentProfile.append("</ak:department>");
			contentProfile.append("<ak:positionType>");
			contentProfile.append(userDetail.getPositionType());
			contentProfile.append("</ak:positionType>");
			contentProfile.append("<ak:positiontitle>");
			contentProfile.append(userDetail.getPositionTitle());
			contentProfile.append("</ak:positiontitle>");
			contentProfile.append("</ak:user>");
		}
		contentProfile.append("</ak:record>");
		contentProfile.append("</Content>");
		contentProfile
				.append("<Attribute AttributeId=\"urn:oasis:names:tc:xacml:3.0:profile:multiple:content-selector\" IncludeInResult=\"false\">");
		contentProfile
				.append("<AttributeValue DataType=\"urn:oasis:names:tc:xacml:3.0:data-type:xpathExpression\" XPathCategory=\"urn:oasis:names:tc:xacml:3.0:attribute-category:resource\">//ak:record//ak:user</AttributeValue>");
		contentProfile.append("</Attribute>");
		return contentProfile;
	}

	/**
	 * Generates Content Profile for Delegation
	 * 
	 * @param authorFullName
	 * @param delegationId
	 * @param existingDelegation
	 * @param authorProfile
	 * @return
	 */
	public StringBuffer generateContentDelegationProfile(String delegationId,
			Delegation existingDelegation, UserProfile authorProfile) {
		StringBuffer contentProfile = new StringBuffer();
		String authorFullName = authorProfile.getFullName();
		contentProfile.append("<Content>");
		contentProfile.append("<ak:record xmlns:ak=\"http://akpower.org\">");
		contentProfile.append("<ak:delegation>");
		contentProfile.append("<ak:delegationid>");
		contentProfile.append(delegationId);
		contentProfile.append("</ak:delegationid>");
		contentProfile.append("<ak:delegationpolicyid>");
		contentProfile.append(existingDelegation.getDelegationPolicyId());
		contentProfile.append("</ak:delegationpolicyid>");
		contentProfile.append("<ak:revoked>");
		contentProfile.append(existingDelegation.isRevoked());
		contentProfile.append("</ak:revoked>");
		contentProfile.append("<ak:delegator>");
		contentProfile.append("<ak:id>");
		contentProfile.append(authorProfile.getId().toString());
		contentProfile.append("</ak:id>");
		contentProfile.append("<ak:fullname>");
		contentProfile.append(authorFullName);
		contentProfile.append("</ak:fullname>");
		contentProfile.append("<ak:email>");
		contentProfile.append(authorProfile.getWorkEmails().get(0));
		contentProfile.append("</ak:email>");
		contentProfile.append("</ak:delegator>");
		contentProfile.append("<ak:delegatee>");
		contentProfile.append("<ak:id>");
		contentProfile.append(existingDelegation.getDelegateeId());
		contentProfile.append("</ak:id>");
		contentProfile.append("<ak:fullname>");
		contentProfile.append(existingDelegation.getDelegatee());
		contentProfile.append("</ak:fullname>");
		contentProfile.append("<ak:email>");
		contentProfile.append(existingDelegation.getDelegateeEmail());
		contentProfile.append("</ak:email>");
		contentProfile.append("</ak:delegatee>");
		contentProfile.append("</ak:delegation>");
		contentProfile.append("</ak:record>");
		contentProfile.append("</Content>");
		contentProfile
				.append("<Attribute AttributeId=\"urn:oasis:names:tc:xacml:3.0:content-selector\" IncludeInResult=\"false\">");
		contentProfile
				.append("<AttributeValue XPathCategory=\"urn:oasis:names:tc:xacml:3.0:attribute-category:resource\" DataType=\"urn:oasis:names:tc:xacml:3.0:data-type:xpathExpression\">//ak:record/ak:delegation</AttributeValue>");
		contentProfile.append("</Attribute>");
		return contentProfile;
	}

	/***
	 * Exports to Excel File for Delegation and its Audit Logs
	 * 
	 * @param delegations
	 * @param delegationAuditLogs
	 * @return
	 * @throws URISyntaxException
	 * @throws JsonProcessingException
	 */
	public String exportToExcelFile(List<DelegationInfo> delegations,
			List<AuditLogInfo> delegationAuditLogs) throws URISyntaxException,
			JsonProcessingException {
		ObjectMapper mapper = new ObjectMapper();
		String filename = new String();
		Xcelite xcelite = new Xcelite();
		if (delegations != null) {
			XceliteSheet sheet = xcelite.createSheet("Delegations");
			SheetWriter<DelegationInfo> writer = sheet
					.getBeanWriter(DelegationInfo.class);
			writer.write(delegations);
		} else {
			XceliteSheet sheet = xcelite.createSheet("AuditLogs");
			SheetWriter<AuditLogInfo> writer = sheet
					.getBeanWriter(AuditLogInfo.class);
			writer.write(delegationAuditLogs);
		}

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

	// AuditLog Functions

	/***
	 * Finds All Logs in a AuditLog Grid for a Delegation
	 * 
	 * @param offset
	 * @param limit
	 * @param id
	 * @param auditLogInfo
	 * @return
	 * @throws ParseException
	 */
	public List<AuditLogInfo> findAllDelegationAuditLogForGrid(int offset,
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
		Query<Delegation> delegationQuery = ds.createQuery(Delegation.class);
		Delegation q = delegationQuery.field("_id").equal(id).get();

		List<AuditLogInfo> allAuditLogs = new ArrayList<AuditLogInfo>();
		if (q.getAuditLog() != null && q.getAuditLog().size() != 0) {
			for (AuditLog delegationAudit : q.getAuditLog()) {
				AuditLogInfo delegationAuditLog = new AuditLogInfo();
				boolean isActionMatch = isAuditLogActionFieldProvided(
						auditLogInfo.getAction(), delegationAudit);
				boolean isAuditedByMatch = isAuditLogAuditedByFieldProvided(
						auditLogInfo.getAuditedBy(), delegationAudit);
				boolean isActivityDateFromMatch = isAuditLogActivityDateFromProvided(
						auditLogInfo.getActivityOnFrom(), delegationAudit);
				boolean isActivityDateToMatch = isAuditLogActivityDateToProvided(
						auditLogInfo.getActivityOnTo(), delegationAudit);

				if (isActionMatch && isAuditedByMatch
						&& isActivityDateFromMatch && isActivityDateToMatch) {
					delegationAuditLog.setUserName(delegationAudit
							.getUserProfile().getUserAccount().getUserName());
					delegationAuditLog.setUserFullName(delegationAudit
							.getUserProfile().getFullName());
					delegationAuditLog.setAction(delegationAudit.getAction());
					delegationAuditLog.setActivityDate(delegationAudit
							.getActivityDate());
					allAuditLogs.add(delegationAuditLog);
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
	 * @param delegationAudit
	 * @return
	 * @throws ParseException
	 */
	private boolean isAuditLogActivityDateToProvided(String activityOnTo,
			AuditLog delegationAudit) throws ParseException {
		if (activityOnTo != null) {
			Date activityDateTo = formatter.parse(activityOnTo);
			if (delegationAudit.getActivityDate().compareTo(activityDateTo) > 0) {
				return false;
			} else if (delegationAudit.getActivityDate().compareTo(
					activityDateTo) < 0) {
				return true;
			} else if (delegationAudit.getActivityDate().compareTo(
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
	 * @param delegationAudit
	 * @return
	 * @throws ParseException
	 */
	private boolean isAuditLogActivityDateFromProvided(String activityOnFrom,
			AuditLog delegationAudit) throws ParseException {
		if (activityOnFrom != null) {
			Date activityDateFrom = formatter.parse(activityOnFrom);
			if (delegationAudit.getActivityDate().compareTo(activityDateFrom) > 0) {
				return true;
			} else if (delegationAudit.getActivityDate().compareTo(
					activityDateFrom) < 0) {
				return false;
			} else if (delegationAudit.getActivityDate().compareTo(
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
	 * @param delegationAudit
	 * @return
	 */
	private boolean isAuditLogAuditedByFieldProvided(String auditedBy,
			AuditLog delegationAudit) {
		if (auditedBy != null) {
			if (delegationAudit.getUserProfile().getUserAccount().getUserName()
					.toLowerCase().contains(auditedBy.toLowerCase())) {
				return true;
			} else if (delegationAudit.getUserProfile().getFirstName()
					.toLowerCase().contains(auditedBy.toLowerCase())) {
				return true;
			} else if (delegationAudit.getUserProfile().getMiddleName()
					.toLowerCase().contains(auditedBy.toLowerCase())) {
				return true;
			} else if (delegationAudit.getUserProfile().getLastName()
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
	 * @param delegationAudit
	 * @return
	 */
	private boolean isAuditLogActionFieldProvided(String action,
			AuditLog delegationAudit) {
		if (action != null) {
			if (delegationAudit.getAction().toLowerCase()
					.contains(action.toLowerCase())) {
				return true;
			}
		} else {
			return true;
		}
		return false;
	}
}
