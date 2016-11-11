package gpms.rest;

import gpms.DAL.MongoDBConnector;
import gpms.accesscontrol.BalanaConnector;
import gpms.dao.DelegationDAO;
import gpms.dao.NotificationDAO;
import gpms.dao.UserAccountDAO;
import gpms.dao.UserProfileDAO;
import gpms.model.AuditLogCommonInfo;
import gpms.model.AuditLogInfo;
import gpms.model.Delegation;
import gpms.model.DelegationCommonInfo;
import gpms.model.DelegationInfo;
import gpms.model.GPMSCommonInfo;
import gpms.model.UserAccount;
import gpms.model.UserDetail;
import gpms.model.UserProfile;
import gpms.utils.EmailUtil;
import gpms.utils.SerializationHelper;
import gpms.utils.WriteXMLUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.log4j.Logger;
import org.bson.types.ObjectId;
import org.mongodb.morphia.Morphia;
import org.wso2.balana.ObligationResult;
import org.wso2.balana.ctx.AbstractResult;
import org.wso2.balana.ctx.Attribute;
import org.wso2.balana.ctx.AttributeAssignment;
import org.wso2.balana.ctx.xacml3.Result;
import org.wso2.balana.xacml3.Attributes;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.mongodb.MongoClient;

@Path("/delegations")
@Api(value = "/delegations", description = "Manage Delegations")
@Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML,
		MediaType.APPLICATION_FORM_URLENCODED, MediaType.TEXT_PLAIN,
		MediaType.TEXT_HTML })
@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML,
		MediaType.TEXT_PLAIN, MediaType.TEXT_HTML })
public class DelegationService {
	MongoClient mongoClient = null;
	Morphia morphia = null;
	String dbName = "db_gpms";
	UserAccountDAO userAccountDAO = null;
	public UserProfileDAO userProfileDAO = null;
	public DelegationDAO delegationDAO = null;
	NotificationDAO notificationDAO = null;
	public DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
	public static String policyLocation = new String();
	public static final Logger log = Logger.getLogger(DelegationService.class
			.getName());

	public DelegationService() {
		mongoClient = MongoDBConnector.getMongo();
		morphia = new Morphia();
		morphia.map(UserProfile.class).map(UserAccount.class);
		userAccountDAO = new UserAccountDAO(mongoClient, morphia, dbName);
		userProfileDAO = new UserProfileDAO(mongoClient, morphia, dbName);
		delegationDAO = new DelegationDAO(mongoClient, morphia, dbName);
		delegationDAO = new DelegationDAO(mongoClient, morphia, dbName);
		notificationDAO = new NotificationDAO(mongoClient, morphia, dbName);
	}

	@GET
	@Produces(MediaType.TEXT_PLAIN)
	@ApiOperation(value = "Test Delegation Service", notes = "This API tests whether the service is working or not")
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "Success: { Hello World! }"),
			@ApiResponse(code = 400, message = "Failed: { \"error\":\"error description\", \"status\": \"FAIL\" }") })
	public Response testService() {
		try {
			log.info("DelegationService::testService started");
			return Response.status(Response.Status.OK).entity("Hello World!")
					.build();
		} catch (Exception e) {
			log.error("Could not connect the Delegation Service error e=", e);
		}
		return Response
				.status(Response.Status.BAD_REQUEST)
				.entity("{\"error\": \"Could Not Find Delegation Service\", \"status\": \"FAIL\"}")
				.build();
	}

	@POST
	@Path("/GetDelegationsList")
	@ApiOperation(value = "Get all Delegations", notes = "This API gets all Delegations")
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "Success: { Delegation Info }"),
			@ApiResponse(code = 400, message = "Failed: { \"error\":\"error description\", \"status\": \"FAIL\" }") })
	public Response produceUserDelegationsJSON(
			@ApiParam(value = "Message", required = true, defaultValue = "", allowableValues = "", allowMultiple = false) String message) {
		try {
			log.info("DelegationService::produceDelegationsJSON started");
			List<DelegationInfo> delegations = new ArrayList<DelegationInfo>();
			int offset = 0, limit = 0;
			ObjectMapper mapper = new ObjectMapper();
			JsonNode root = mapper.readTree(message);
			if (root != null && root.has("offset")) {
				offset = root.get("offset").intValue();
			}
			if (root != null && root.has("limit")) {
				limit = root.get("limit").intValue();
			}
			DelegationCommonInfo delegationInfo = new DelegationCommonInfo();
			if (root != null && root.has("delegationBindObj")) {
				JsonNode delegationObj = root.get("delegationBindObj");
				delegationInfo = new DelegationCommonInfo(delegationObj);
			}
			GPMSCommonInfo userInfo = new GPMSCommonInfo();
			if (root != null && root.has("gpmsCommonObj")) {
				JsonNode commonObj = root.get("gpmsCommonObj");
				userInfo = new GPMSCommonInfo(commonObj);
			}
			delegations = delegationDAO.findAllUserDelegationsForGrid(offset,
					limit, delegationInfo, userInfo);
			return Response
					.status(Response.Status.OK)
					.entity(mapper.writerWithDefaultPrettyPrinter()
							.writeValueAsString(delegations)).build();
		} catch (Exception e) {
			log.error("Could not find all Delegations of a User error e=", e);
		}
		return Response
				.status(Response.Status.BAD_REQUEST)
				.entity("{\"error\": \"Could Not Find All Delegations Of A User\", \"status\": \"FAIL\"}")
				.build();
	}

	@POST
	@Path("/DelegationsExportToExcel")
	@Produces(MediaType.TEXT_HTML)
	@ApiOperation(value = "Export all Delegations in a grid", notes = "This API exports all Delegations shown in a grid")
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "Success: { Excel Filename/ No Record}"),
			@ApiResponse(code = 400, message = "Failed: { \"error\":\"error description\", \"status\": \"FAIL\" }") })
	public Response exportDelegationsJSON(
			@ApiParam(value = "Message", required = true, defaultValue = "", allowableValues = "", allowMultiple = false) String message) {
		try {
			log.info("DelegationService::exportDelegationsJSON started");
			List<DelegationInfo> delegations = new ArrayList<DelegationInfo>();
			ObjectMapper mapper = new ObjectMapper();
			JsonNode root = mapper.readTree(message);
			DelegationCommonInfo delegationInfo = new DelegationCommonInfo();
			if (root != null && root.has("delegationBindObj")) {
				JsonNode delegationObj = root.get("delegationBindObj");
				delegationInfo = new DelegationCommonInfo(delegationObj);
			}
			GPMSCommonInfo userInfo = new GPMSCommonInfo();
			if (root != null && root.has("gpmsCommonObj")) {
				JsonNode commonObj = root.get("gpmsCommonObj");
				userInfo = new GPMSCommonInfo(commonObj);
			}
			delegations = delegationDAO.findAllUserDelegationsForExport(
					delegationInfo, userInfo);
			String filename = new String();
			if (delegations.size() > 0) {
				filename = delegationDAO.exportToExcelFile(delegations, null);
			} else {
				filename = mapper.writerWithDefaultPrettyPrinter()
						.writeValueAsString("No Record");
			}
			return Response.status(Response.Status.OK).entity(filename).build();
		} catch (Exception e) {
			log.error("Could not export all Delegations error e=", e);
		}
		return Response
				.status(Response.Status.BAD_REQUEST)
				.entity("{\"error\": \"Could Not Export All Delegations\", \"status\": \"FAIL\"}")
				.build();
	}

	@POST
	@Path("/GetDelegationAuditLogList")
	@ApiOperation(value = "Get Delegation Audit Log List", notes = "This API gets Delegation Audit Log List")
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "Success: { AuditLog Info }"),
			@ApiResponse(code = 400, message = "Failed: { \"error\":\"error description\", \"status\": \"FAIL\" }") })
	public Response produceDelegationAuditLogJSON(
			@ApiParam(value = "Message", required = true, defaultValue = "", allowableValues = "", allowMultiple = false) String message) {
		try {
			log.info("DelegationService::produceDelegationAuditLogJSON started");
			List<AuditLogInfo> delegationAuditLogs = new ArrayList<AuditLogInfo>();
			int offset = 0, limit = 0;
			String delegationId = new String();
			ObjectMapper mapper = new ObjectMapper();
			JsonNode root = mapper.readTree(message);
			if (root != null && root.has("offset")) {
				offset = root.get("offset").intValue();
			}
			if (root != null && root.has("limit")) {
				limit = root.get("limit").intValue();
			}
			if (root != null && root.has("delegationId")) {
				delegationId = root.get("delegationId").textValue();
			}
			AuditLogCommonInfo auditLogInfo = new AuditLogCommonInfo();
			if (root != null && root.has("auditLogBindObj")) {
				JsonNode auditLogBindObj = root.get("auditLogBindObj");
				auditLogInfo = new AuditLogCommonInfo(auditLogBindObj);
			}
			ObjectId id = new ObjectId(delegationId);
			delegationAuditLogs = delegationDAO
					.findAllDelegationAuditLogForGrid(offset, limit, id,
							auditLogInfo);
			return Response
					.status(Response.Status.OK)
					.entity(mapper.writerWithDefaultPrettyPrinter()
							.writeValueAsString(delegationAuditLogs)).build();
		} catch (Exception e) {
			log.error("Could not find Delegation Audit Log List error e=", e);
		}
		return Response
				.status(Response.Status.BAD_REQUEST)
				.entity("{\"error\": \"Could Not Find Delegation Audit Log List\", \"status\": \"FAIL\"}")
				.build();
	}

	@POST
	@Path("/DelegationLogsExportToExcel")
	@ApiOperation(value = "Export all Delegation Logs in a grid", notes = "This API exports all Delegation Logs shown in a grid")
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "Success: { Excel Filename/ No Record}"),
			@ApiResponse(code = 400, message = "Failed: { \"error\":\"error description\", \"status\": \"FAIL\" }") })
	public Response exportDelegationAuditLogJSON(
			@ApiParam(value = "Message", required = true, defaultValue = "", allowableValues = "", allowMultiple = false) String message) {
		try {
			log.info("DelegationService::exportDelegationAuditLogJSON started");
			List<AuditLogInfo> delegationAuditLogs = new ArrayList<AuditLogInfo>();
			String delegationId = new String();
			ObjectMapper mapper = new ObjectMapper();
			JsonNode root = mapper.readTree(message);
			if (root != null && root.has("delegationId")) {
				delegationId = root.get("delegationId").textValue();
			}
			AuditLogCommonInfo auditLogInfo = new AuditLogCommonInfo();
			if (root != null && root.has("auditLogBindObj")) {
				JsonNode auditLogBindObj = root.get("auditLogBindObj");
				auditLogInfo = new AuditLogCommonInfo(auditLogBindObj);
			}
			ObjectId id = new ObjectId(delegationId);
			delegationAuditLogs = delegationDAO.getSortedAuditLogResults(
					auditLogInfo, id);
			String filename = new String();
			if (delegationAuditLogs.size() > 0) {
				filename = delegationDAO.exportToExcelFile(null,
						delegationAuditLogs);
			} else {
				filename = mapper.writerWithDefaultPrettyPrinter()
						.writeValueAsString("No Record");
			}
			return Response.status(Response.Status.OK).entity(filename).build();
		} catch (Exception e) {
			log.error("Could not export Delegation Logs error e=", e);
		}
		return Response
				.status(Response.Status.BAD_REQUEST)
				.entity("{\"error\": \"Could Not Delegation Logs List\", \"status\": \"FAIL\"}")
				.build();
	}

	@POST
	@Path("/GetDelegationDetailsByDelegationId")
	@ApiOperation(value = "Get Delegation Details by DelegationId", notes = "This API gets Delegation Details by DelegationId")
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "Success: { Delegation }"),
			@ApiResponse(code = 400, message = "Failed: { \"error\":\"error description\", \"status\": \"FAIL\" }") })
	public Response produceDelegationDetailsByDelegationId(
			@ApiParam(value = "Message", required = true, defaultValue = "", allowableValues = "", allowMultiple = false) String message) {
		try {
			log.info("DelegationService::produceDelegationDetailsByDelegationId started");
			Delegation delegation = new Delegation();
			String delegationId = new String();
			ObjectMapper mapper = new ObjectMapper();
			JsonNode root = mapper.readTree(message);
			if (root != null && root.has("delegationId")) {
				delegationId = root.get("delegationId").textValue();
			}
			ObjectId id = new ObjectId(delegationId);
			delegation = delegationDAO.findDelegationByDelegationID(id);
			return Response
					.status(Response.Status.OK)
					.entity(mapper.setDateFormat(formatter)
							.writerWithDefaultPrettyPrinter()
							.writeValueAsString(delegation)).build();
		} catch (Exception e) {
			log.error(
					"Could not find Delegation Details by DelegationId error e=",
					e);
		}
		return Response
				.status(Response.Status.BAD_REQUEST)
				.entity("{\"error\": \"Could Not Find Delegation Details By DelegationId\", \"status\": \"FAIL\"}")
				.build();
	}

	@POST
	@Path("/GetDelegableActionsForAUser")
	@ApiOperation(value = "Get Delegable Actions For a logged in User", notes = "This API gets Delegable Multiple Response with Action for a given User")
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "Success: { Actions for Dropdown }"),
			@ApiResponse(code = 400, message = "Failed: { \"error\":\"error description\", \"status\": \"FAIL\" }") })
	public Response produceDelegableActionsForAUser(
			@ApiParam(value = "Message", required = true, defaultValue = "", allowableValues = "", allowMultiple = false) String message) {
		try {
			log.info("DelegationService::produceDelegableActionsForAUser started");
			ObjectMapper mapper = new ObjectMapper();
			JsonNode root = mapper.readTree(message);
			BalanaConnector ac = new BalanaConnector();
			HashMap<String, Multimap<String, String>> attrMap = new HashMap<String, Multimap<String, String>>();
			Multimap<String, String> subjectMap = ArrayListMultimap.create();
			Multimap<String, String> actionMap = ArrayListMultimap.create();
			GPMSCommonInfo userInfo = new GPMSCommonInfo();
			if (root != null && root.has("gpmsCommonObj")) {
				JsonNode commonObj = root.get("gpmsCommonObj");
				userInfo = new GPMSCommonInfo(commonObj);
				subjectMap.put("department", userInfo.getUserDepartment());
				attrMap.put("Subject", subjectMap);
				subjectMap.put("position.title",
						userInfo.getUserPositionTitle());
				attrMap.put("Subject", subjectMap);
			}
			List<String> attributeValue = Arrays.asList("Save", "Submit",
					"Approve", "Disapprove", "Withdraw", "Archive", "Delete");
			for (String action : attributeValue) {
				actionMap.put("proposal.action", action);
				attrMap.put("Action", actionMap);
			}
			Set<AbstractResult> results = ac.getXACMLdecisionForMDP(attrMap);
			List<String> actions = new ArrayList<String>();
			for (AbstractResult result : results) {
				if (AbstractResult.DECISION_PERMIT == result.getDecision()) {
					Set<Attributes> attributesSet = ((Result) result)
							.getAttributes();
					for (Attributes attributes : attributesSet) {
						for (Attribute attribute : attributes.getAttributes()) {
							actions.add(attribute.getValue().encode());
						}
					}
				}
			}
			Collections.sort(actions);
			return Response
					.status(Response.Status.OK)
					.entity(mapper.setDateFormat(formatter)
							.writerWithDefaultPrettyPrinter()
							.writeValueAsString(actions)).build();
		} catch (Exception e) {
			log.error(
					"Could not find Delegable Actions for supplied User error e=",
					e);
		}
		return Response
				.status(Response.Status.BAD_REQUEST)
				.entity("{\"error\": \"Could Not Find Delegable Actions for the given User\", \"status\": \"FAIL\"}")
				.build();
	}

	@POST
	@Path("/GetDelegableUsersForAUser")
	@ApiOperation(value = "Get Delegable Multiple Users Response for a User", notes = "This API gets Delegable Multiple Users Response for a User")
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "Success: { User Detail for Dropdown }"),
			@ApiResponse(code = 400, message = "Failed: { \"error\":\"error description\", \"status\": \"FAIL\" }") })
	public Response produceDelegableUsersForAUser(
			@ApiParam(value = "Message", required = true, defaultValue = "", allowableValues = "", allowMultiple = false) String message) {
		try {
			log.info("DelegationService::produceDelegableUsersForAUser started");
			ObjectMapper mapper = new ObjectMapper();
			JsonNode root = mapper.readTree(message);
			HashMap<String, Multimap<String, String>> attrMap = new HashMap<String, Multimap<String, String>>();
			Multimap<String, String> subjectMap = ArrayListMultimap.create();
			Multimap<String, String> resourceMap = ArrayListMultimap.create();
			GPMSCommonInfo userInfo = new GPMSCommonInfo();
			if (root != null && root.has("gpmsCommonObj")) {
				JsonNode commonObj = root.get("gpmsCommonObj");
				userInfo = new GPMSCommonInfo(commonObj);
				subjectMap.put("position.title",
						userInfo.getUserPositionTitle());
				subjectMap.put("department", userInfo.getUserDepartment());
				attrMap.put("Subject", subjectMap);
				if (attrMap.get("Resource") == null) {
					attrMap.put("Resource", resourceMap);
				}
			}
			ObjectId id = new ObjectId(userInfo.getUserProfileID());
			List<UserDetail> delegableUsers = userProfileDAO
					.findAllUsersForDelegation(id, userInfo.getUserCollege(),
							userInfo.getUserDepartment());

			StringBuffer contentProfile = delegationDAO
					.generateContentProfile(delegableUsers);
			BalanaConnector ac = new BalanaConnector();
			Set<AbstractResult> results = ac.getXACMLdecisionWithObligations(
					attrMap, contentProfile);
			List<UserDetail> userDetails = new ArrayList<UserDetail>();
			for (AbstractResult result : results) {
				if (AbstractResult.DECISION_PERMIT == result.getDecision()) {
					delegationDAO.getDelegableUserDetailsFromAdvice(
							userDetails, result);
				}
			}
			Collections.sort(userDetails);
			return Response
					.status(Response.Status.OK)
					.entity(mapper.setDateFormat(formatter)
							.writerWithDefaultPrettyPrinter()
							.writeValueAsString(userDetails)).build();
		} catch (Exception e) {
			log.error(
					"Could not find User Detail for supplied Action of a User error e=",
					e);
		}
		return Response
				.status(Response.Status.BAD_REQUEST)
				.entity("{\"error\": \"Could Not Find User Detail for supplied Action of a User\", \"status\": \"FAIL\"}")
				.build();
	}

	@POST
	@Path("/SaveUpdateDelegation")
	@ApiOperation(value = "Save a New Delegation or Update an existing Delegation", notes = "This API saves a New Delegation or updates an existing Delegation")
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "Success: { True }"),
			@ApiResponse(code = 403, message = "Failed: { \"error\":\"error description\", \"status\": \"FAIL\" }") })
	public Response saveUpdateDelegation(
			@ApiParam(value = "Message", required = true, defaultValue = "", allowableValues = "", allowMultiple = false) String message) {
		try {
			log.info("DelegationService::saveUpdateDelegation started");
			ObjectMapper mapper = new ObjectMapper();
			JsonNode root = mapper.readTree(message);
			GPMSCommonInfo userInfo = new GPMSCommonInfo();
			if (root != null && root.has("gpmsCommonObj")) {
				JsonNode commonObj = root.get("gpmsCommonObj");
				userInfo = new GPMSCommonInfo(commonObj);
			}
			ObjectId authorId = new ObjectId(userInfo.getUserProfileID());
			UserProfile authorProfile = userProfileDAO
					.findUserDetailsByProfileID(authorId);
			String delegatorName = authorProfile.getFullName();
			String delegationID = new String();
			Delegation newDelegation = new Delegation();
			Delegation existingDelegation = new Delegation();
			Delegation oldDelegation = new Delegation();
			try {
				policyLocation = this.getClass().getResource("/policy").toURI()
						.getPath();
			} catch (Exception ex) {
				throw new Exception("The Policy folder can not be Found!");
			}
			if (root != null && root.has("delegationInfo")) {
				JsonNode delegationInfo = root.get("delegationInfo");
				if (delegationInfo != null
						&& delegationInfo.has("DelegationId")) {
					delegationID = delegationInfo.get("DelegationId")
							.textValue();
					if (!delegationID.equals("0")) {
						ObjectId delegationId = new ObjectId(delegationID);
						existingDelegation = delegationDAO
								.findDelegationByDelegationID(delegationId);
						oldDelegation = SerializationHelper
								.cloneThroughSerialize(existingDelegation);
					} else {
						newDelegation.setUserProfile(authorProfile);
						newDelegation.setDelegatorId(userInfo
								.getUserProfileID());
					}
				}
				UserProfile delegateeProfile = null;
				if (delegationInfo != null && delegationInfo.has("DelegateeId")) {
					String delegateeId = delegationInfo.get("DelegateeId")
							.textValue();
					if (delegationID.equals("0")) {
						newDelegation.setDelegateeId(delegateeId);
						ObjectId id = new ObjectId(delegateeId);
						delegateeProfile = userProfileDAO
								.findUserDetailsByProfileID(id);
					}
				}
				delegationDAO.generateDelegationDetails(userInfo, delegationID,
						delegateeProfile, newDelegation, existingDelegation,
						delegationInfo);
				if (!delegationID.equals("0")) {
					if (!existingDelegation.equals(oldDelegation)) {
						try {
							delegationDAO.updateDelegation(userInfo,
									authorProfile, delegatorName,
									existingDelegation);
							String notificationMessage = "Delegation Updated by "
									+ userInfo.getUserName() + ".";
							notificationDAO.sendNotification(
									existingDelegation, userInfo,
									notificationMessage, "Delegation", false);
						} catch (Exception e) {
							return Response
									.status(403)
									.type(MediaType.APPLICATION_JSON)
									.entity("File delete permission is not enabled!")
									.build();
						}
					}
				} else {
					delegationDAO.save(newDelegation);
					try {
						delegationDAO.saveDelegation(userInfo, authorProfile,
								delegatorName, newDelegation);
						String notificationMessage = "Delegation Added by "
								+ userInfo.getUserName() + ".";
						notificationDAO.sendNotification(newDelegation,
								userInfo, notificationMessage, "Delegation",
								false);
						return Response
								.status(200)
								.type(MediaType.APPLICATION_JSON)
								.entity(mapper.writerWithDefaultPrettyPrinter()
										.writeValueAsString(true)).build();
					} catch (Exception e) {
						delegationDAO.delete(newDelegation);
						return Response
								.status(403)
								.type(MediaType.APPLICATION_JSON)
								.entity("File create permission is not enabled!")
								.build();
					}
				}
				return Response
						.status(200)
						.type(MediaType.APPLICATION_JSON)
						.entity(mapper.writerWithDefaultPrettyPrinter()
								.writeValueAsString(true)).build();
			} else {
				return Response.status(403).type(MediaType.APPLICATION_JSON)
						.entity("No Delegation Info is send!").build();
			}
		} catch (Exception e) {
			log.error(
					"Could not save a New Delegation or update an existing Delegation error e=",
					e);
		}
		return Response
				.status(403)
				.entity("{\"error\": \"Could Not Save A New Delegation OR Update an Existing Delegation\", \"status\": \"FAIL\"}")
				.build();
	}

	@POST
	@Path("/RevokeDelegationByDelegationID")
	@ApiOperation(value = "Revoke Delegation by DelegationId", notes = "This API deletes Delegation by DelegationId")
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "Success: { True }"),
			@ApiResponse(code = 403, message = "Failed: { \"error\":\"error description\", \"status\": \"FAIL\" }") })
	public Response revokeDelegationByDelegationID(String message) {
		try {
			DelegationService.log
					.info("DelegationService::revokeDelegationByDelegationID started");
			ObjectMapper mapper = new ObjectMapper();
			JsonNode root = mapper.readTree(message);
			String delegationId = new String();
			if (root != null && root.has("delegationId")) {
				delegationId = root.get("delegationId").textValue();
			}
			GPMSCommonInfo userInfo = new GPMSCommonInfo();
			if (root != null && root.has("gpmsCommonObj")) {
				JsonNode commonObj = root.get("gpmsCommonObj");
				userInfo = new GPMSCommonInfo(commonObj);
			}
			ObjectId id = new ObjectId(delegationId);
			Delegation existingDelegation = delegationDAO
					.findDelegationByDelegationID(id);
			ObjectId authorId = new ObjectId(userInfo.getUserProfileID());
			UserProfile authorProfile = userProfileDAO
					.findUserDetailsByProfileID(authorId);
			String authorUserName = authorProfile.getUserAccount()
					.getUserName();
			StringBuffer contentProfile = delegationDAO
					.generateContentDelegationProfile(delegationId,
							existingDelegation, authorProfile);
			BalanaConnector ac = new BalanaConnector();
			HashMap<String, Multimap<String, String>> attrMap = new HashMap<String, Multimap<String, String>>();
			Multimap<String, String> resourceMap = ArrayListMultimap.create();
			if (attrMap.get("Resource") == null) {
				attrMap.put("Resource", resourceMap);
			}
			Multimap<String, String> actionMap = ArrayListMultimap.create();
			actionMap.put("proposal.action", "Revoke");
			attrMap.put("Action", actionMap);
			Set<AbstractResult> set = ac.getXACMLdecisionWithObligations(
					attrMap, contentProfile);
			Iterator<AbstractResult> it = set.iterator();
			int intDecision = AbstractResult.DECISION_NOT_APPLICABLE;
			while (it.hasNext()) {
				AbstractResult ar = it.next();
				intDecision = ar.getDecision();
				if (intDecision == AbstractResult.DECISION_INDETERMINATE_DENY
						|| intDecision == AbstractResult.DECISION_INDETERMINATE_PERMIT
						|| intDecision == AbstractResult.DECISION_INDETERMINATE_DENY_OR_PERMIT) {
					intDecision = AbstractResult.DECISION_INDETERMINATE;
				}
				System.out.println("Decision:" + intDecision + " that is: "
						+ AbstractResult.DECISIONS[intDecision]);
				if (AbstractResult.DECISIONS[intDecision].equals("Permit")) {
					return revokeDelegation(userInfo, existingDelegation,
							authorProfile, authorUserName, ar);
				} else {
					return Response
							.status(403)
							.type(MediaType.APPLICATION_JSON)
							.entity("Your permission is: "
									+ AbstractResult.DECISIONS[intDecision])
							.build();
				}
			}
		} catch (Exception e) {
			DelegationService.log.error(
					"Could not revoke the selected Delegation error e=", e);
		}
		return Response
				.status(403)
				.entity("{\"error\": \"Could not revoke the selected Delegation\", \"status\": \"FAIL\"}")
				.build();
	}

	/**
	 * Revokes Delegation
	 * 
	 * @param mapper
	 * @param userInfo
	 * @param existingDelegation
	 * @param authorProfile
	 * @param authorUserName
	 * @param ar
	 * @return
	 * @throws JsonProcessingException
	 */
	public Response revokeDelegation(GPMSCommonInfo userInfo,
			Delegation existingDelegation, UserProfile authorProfile,
			String authorUserName, AbstractResult ar)
			throws JsonProcessingException {
		ObjectMapper mapper = new ObjectMapper();
		List<ObligationResult> obligations = ar.getObligations();
		EmailUtil emailUtil = new EmailUtil();
		String emailSubject = new String();
		String emailBody = new String();
		String delegatorName = new String();
		String delegatorEmail = new String();
		List<String> emaillist = new ArrayList<String>();
		if (obligations.size() > 0) {
			for (ObligationResult obligation : obligations) {
				if (obligation instanceof org.wso2.balana.xacml3.Obligation) {
					List<AttributeAssignment> assignments = ((org.wso2.balana.xacml3.Obligation) obligation)
							.getAssignments();
					for (AttributeAssignment assignment : assignments) {
						switch (assignment.getAttributeId().toString()) {
						case "emailSubject":
							emailSubject = assignment.getContent();
							break;
						case "emailBody":
							emailBody = assignment.getContent();
							break;
						case "delegatorName":
							delegatorName = assignment.getContent();
							break;
						case "delegatorEmail":
							delegatorEmail = assignment.getContent();
							break;
						case "delegateeEmail":
							if (!assignment.getContent().equals("")) {
								emaillist.add(assignment.getContent());
							}
							break;
						default:
							break;
						}
					}
				}
			}
		}
		boolean isRevoked = delegationDAO.revokeDelegation(existingDelegation,
				authorProfile);
		if (isRevoked) {
			return sendNotificationAfterRevoke(userInfo, existingDelegation,
					authorUserName, emailUtil, emailSubject, emailBody,
					delegatorName, delegatorEmail, emaillist);
		} else {
			return Response
					.status(200)
					.type(MediaType.APPLICATION_JSON)
					.entity(mapper.writerWithDefaultPrettyPrinter()
							.writeValueAsString(true)).build();
		}
	}

	/**
	 * Sends Notification After Revoke
	 * 
	 * @param mapper
	 * @param userInfo
	 * @param existingDelegation
	 * @param authorUserName
	 * @param emailUtil
	 * @param emailSubject
	 * @param emailBody
	 * @param delegatorName
	 * @param delegatorEmail
	 * @param emaillist
	 * @return
	 * @throws JsonProcessingException
	 */
	public Response sendNotificationAfterRevoke(GPMSCommonInfo userInfo,
			Delegation existingDelegation, String authorUserName,
			EmailUtil emailUtil, String emailSubject, String emailBody,
			String delegatorName, String delegatorEmail, List<String> emaillist)
			throws JsonProcessingException {
		ObjectMapper mapper = new ObjectMapper();
		if (!emailSubject.equals("")) {
			emailUtil.sendMailMultipleUsersWithoutAuth(delegatorEmail,
					emaillist, emailSubject + delegatorName, emailBody);
		}
		String notificationMessage = "Delegation Revoked by " + authorUserName
				+ ".";
		notificationDAO.sendNotification(existingDelegation, userInfo,
				notificationMessage, "Delegation", true);
		try {
			policyLocation = this.getClass().getResource("/policy").toURI()
					.getPath();
			WriteXMLUtil.deletePolicyIdFromXML(policyLocation,
					existingDelegation.getDelegationPolicyId());
		} catch (Exception e) {
			return Response.status(403).type(MediaType.APPLICATION_JSON)
					.entity("File delete permission is not enabled!").build();
		}
		return Response
				.status(200)
				.type(MediaType.APPLICATION_JSON)
				.entity(mapper.writerWithDefaultPrettyPrinter()
						.writeValueAsString(true)).build();
	}
}
