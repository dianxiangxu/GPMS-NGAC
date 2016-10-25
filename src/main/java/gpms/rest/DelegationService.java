package gpms.rest;

import gpms.DAL.MongoDBConnector;
import gpms.accesscontrol.Accesscontrol;
import gpms.dao.DelegationDAO;
import gpms.dao.NotificationDAO;
import gpms.dao.UserAccountDAO;
import gpms.dao.UserProfileDAO;
import gpms.model.AuditLogInfo;
import gpms.model.Delegation;
import gpms.model.DelegationCommonInfo;
import gpms.model.DelegationInfo;
import gpms.model.GPMSCommonInfo;
import gpms.model.NotificationLog;
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

import java.io.File;
import java.io.IOException;
import java.net.UnknownHostException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
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

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.log4j.Logger;
import org.bson.types.ObjectId;
import org.glassfish.jersey.media.sse.OutboundEvent;
import org.mongodb.morphia.Morphia;
import org.wso2.balana.ObligationResult;
import org.wso2.balana.ctx.AbstractResult;
import org.wso2.balana.ctx.Attribute;
import org.wso2.balana.ctx.AttributeAssignment;
import org.wso2.balana.ctx.xacml3.Result;
import org.wso2.balana.xacml3.Advice;
import org.wso2.balana.xacml3.Attributes;
import org.xml.sax.SAXException;

import com.ebay.xcelite.Xcelite;
import com.ebay.xcelite.sheet.XceliteSheet;
import com.ebay.xcelite.writer.SheetWriter;
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
	UserProfileDAO userProfileDAO = null;
	DelegationDAO delegationDAO = null;
	NotificationDAO notificationDAO = null;
	DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
	private static String policyLocation = new String();
	private static final Logger log = Logger.getLogger(DelegationService.class
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
			delegations = delegationDAO.findAllForUserDelegationGrid(offset,
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
			delegations = delegationDAO.findAllUserDelegations(
					delegationInfo.getDelegatee(),
					delegationInfo.getCreatedFrom(),
					delegationInfo.getCreatedTo(),
					delegationInfo.getDelegatedAction(),
					delegationInfo.isRevoked(), userInfo.getUserProfileID(),
					userInfo.getUserCollege(), userInfo.getUserDepartment(),
					userInfo.getUserPositionType(),
					userInfo.getUserPositionTitle());
			String filename = new String();
			if (delegations.size() > 0) {
				Xcelite xcelite = new Xcelite();
				XceliteSheet sheet = xcelite.createSheet("Delegations");
				SheetWriter<DelegationInfo> writer = sheet
						.getBeanWriter(DelegationInfo.class);
				writer.write(delegations);
				SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy_MM_dd");
				Date date = new Date();
				String fileName = String.format("%s.%s",
						RandomStringUtils.randomAlphanumeric(8) + "_"
								+ dateFormat.format(date), "xlsx");
				String downloadLocation = this.getClass()
						.getResource("/uploads").toURI().getPath();
				xcelite.write(new File(downloadLocation + fileName));
				filename = mapper.writerWithDefaultPrettyPrinter()
						.writeValueAsString(fileName);
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
			String action = new String();
			String auditedBy = new String();
			String activityOnFrom = new String();
			String activityOnTo = new String();
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
			if (root != null && root.has("auditLogBindObj")) {
				JsonNode auditLogBindObj = root.get("auditLogBindObj");
				if (auditLogBindObj != null && auditLogBindObj.has("Action")) {
					action = auditLogBindObj.get("Action").textValue();
				}
				if (auditLogBindObj != null && auditLogBindObj.has("AuditedBy")) {
					auditedBy = auditLogBindObj.get("AuditedBy").textValue();
				}
				if (auditLogBindObj != null
						&& auditLogBindObj.has("ActivityOnFrom")) {
					activityOnFrom = auditLogBindObj.get("ActivityOnFrom")
							.textValue();
				}
				if (auditLogBindObj != null
						&& auditLogBindObj.has("ActivityOnTo")) {
					activityOnTo = auditLogBindObj.get("ActivityOnTo")
							.textValue();
				}
			}
			ObjectId id = new ObjectId(delegationId);
			delegationAuditLogs = delegationDAO
					.findAllForDelegationAuditLogGrid(offset, limit, id,
							action, auditedBy, activityOnFrom, activityOnTo);
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
			String action = new String();
			String auditedBy = new String();
			String activityOnFrom = new String();
			String activityOnTo = new String();
			ObjectMapper mapper = new ObjectMapper();
			JsonNode root = mapper.readTree(message);
			if (root != null && root.has("delegationId")) {
				delegationId = root.get("delegationId").textValue();
			}
			if (root != null && root.has("auditLogBindObj")) {
				JsonNode auditLogBindObj = root.get("auditLogBindObj");
				if (auditLogBindObj != null && auditLogBindObj.has("Action")) {
					action = auditLogBindObj.get("Action").textValue();
				}
				if (auditLogBindObj != null && auditLogBindObj.has("AuditedBy")) {
					auditedBy = auditLogBindObj.get("AuditedBy").textValue();
				}
				if (auditLogBindObj != null
						&& auditLogBindObj.has("ActivityOnFrom")) {
					activityOnFrom = auditLogBindObj.get("ActivityOnFrom")
							.textValue();
				}
				if (auditLogBindObj != null
						&& auditLogBindObj.has("ActivityOnTo")) {
					activityOnTo = auditLogBindObj.get("ActivityOnTo")
							.textValue();
				}
			}
			ObjectId id = new ObjectId(delegationId);
			delegationAuditLogs = delegationDAO.findAllUserDelegationAuditLogs(
					id, action, auditedBy, activityOnFrom, activityOnTo);
			String filename = new String();
			if (delegationAuditLogs.size() > 0) {
				Xcelite xcelite = new Xcelite();
				XceliteSheet sheet = xcelite.createSheet("AuditLogs");
				SheetWriter<AuditLogInfo> writer = sheet
						.getBeanWriter(AuditLogInfo.class);
				writer.write(delegationAuditLogs);
				SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy_MM_dd");
				Date date = new Date();
				String fileName = String.format("%s.%s",
						RandomStringUtils.randomAlphanumeric(8) + "_"
								+ dateFormat.format(date), "xlsx");
				String downloadLocation = this.getClass()
						.getResource("/uploads").toURI().getPath();
				xcelite.write(new File(downloadLocation + fileName));
				filename = mapper.writerWithDefaultPrettyPrinter()
						.writeValueAsString(fileName);
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
			Accesscontrol ac = new Accesscontrol();
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
			StringBuffer contentProfile = generateContentProfile(
					userInfo.getUserProfileID(), userInfo.getUserCollege(),
					userInfo.getUserDepartment());
			Accesscontrol ac = new Accesscontrol();
			Set<AbstractResult> results = ac.getXACMLdecisionWithObligations(
					attrMap, contentProfile);
			List<UserDetail> userDetails = new ArrayList<UserDetail>();
			for (AbstractResult result : results) {
				if (AbstractResult.DECISION_PERMIT == result.getDecision()) {
					getDelegableUserDetailsFromAdvice(userDetails, result);
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

	/**
	 * Gets Delegable User Details From Advice response
	 * 
	 * @param userDetails
	 *            UserDetail
	 * @param result
	 */
	private void getDelegableUserDetailsFromAdvice(
			List<UserDetail> userDetails, AbstractResult result) {
		List<Advice> advices = result.getAdvices();
		for (Advice advice : advices) {
			if (advice instanceof org.wso2.balana.xacml3.Advice) {
				UserDetail userDeatil = new UserDetail();
				List<AttributeAssignment> assignments = ((org.wso2.balana.xacml3.Advice) advice)
						.getAssignments();
				for (AttributeAssignment assignment : assignments) {
					switch (assignment.getAttributeId().toString()) {
					case "userId":
						userDeatil.setUserProfileId(assignment.getContent());
						break;
					case "userfullName":
						userDeatil.setFullName(assignment.getContent());
						break;
					case "userName":
						userDeatil.setUserName(assignment.getContent());
						break;
					case "userEmail":
						userDeatil.setEmail(assignment.getContent());
						break;
					case "userCollege":
						userDeatil.setCollege(assignment.getContent());
						break;
					case "userDepartment":
						userDeatil.setDepartment(assignment.getContent());
						break;
					case "userPositionType":
						userDeatil.setPositionType(assignment.getContent());
						break;
					case "userPositionTitle":
						userDeatil.setPositionTitle(assignment.getContent());
						break;
					default:
						break;
					}
				}
				userDetails.add(userDeatil);
			}
		}
	}

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
	private StringBuffer generateContentProfile(String userProfileID,
			String userCollege, String userDepartment)
			throws UnknownHostException {
		StringBuffer contentProfile = new StringBuffer();
		contentProfile.append("<Content>");
		contentProfile.append("<ak:record xmlns:ak=\"http://akpower.org\">");
		ObjectId id = new ObjectId(userProfileID);
		List<UserDetail> delegableUsers = userProfileDAO
				.findAllUsersForDelegation(id, userCollege, userDepartment);
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
			String policyId = new String();
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
				generateDelegationDetails(userInfo, delegationID,
						newDelegation, existingDelegation, delegationInfo);

				String notificationMessage = new String();
				if (!delegationID.equals("0")) {
					if (!existingDelegation.equals(oldDelegation)) {
						try {
							// Create New policy Id
							policyId = createDynamicPolicy(
									userInfo.getUserProfileID(), delegatorName,
									policyLocation, existingDelegation);
							existingDelegation.setDelegationPolicyId(policyId);
							delegationDAO.updateDelegation(existingDelegation,
									authorProfile);
							notificationMessage = "Delegation Updated by "
									+ userInfo.getUserName() + ".";
							sendNotification(existingDelegation,
									userInfo.getUserProfileID(),
									userInfo.getUserName(),
									userInfo.getUserCollege(),
									userInfo.getUserDepartment(),
									userInfo.getUserPositionType(),
									userInfo.getUserPositionTitle(),
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
						policyId = createDynamicPolicy(
								userInfo.getUserProfileID(), delegatorName,
								policyLocation, newDelegation);
						newDelegation.setDelegationPolicyId(policyId);
						delegationDAO.saveDelegation(newDelegation,
								authorProfile);
						notificationMessage = "Delegation Added by "
								+ userInfo.getUserName() + ".";
						sendNotification(newDelegation,
								userInfo.getUserProfileID(),
								userInfo.getUserName(),
								userInfo.getUserCollege(),
								userInfo.getUserDepartment(),
								userInfo.getUserPositionType(),
								userInfo.getUserPositionTitle(),
								notificationMessage, "Delegation", false);
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
	private void generateDelegationDetails(GPMSCommonInfo userInfo,
			String delegationID, Delegation newDelegation,
			Delegation existingDelegation, JsonNode delegationInfo)
			throws Exception, ParseException {
		UserProfile delegateeProfile = new UserProfile();
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
		if (delegationInfo != null && delegationInfo.has("DelegateeId")) {
			String delegateeId = delegationInfo.get("DelegateeId").textValue();
			if (delegationID.equals("0")) {
				newDelegation.setDelegateeId(delegateeId);
				ObjectId id = new ObjectId(delegateeId);
				delegateeProfile = userProfileDAO
						.findUserDetailsByProfileID(id);
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

	private String createDynamicPolicy(String delegatorId,
			String delegatorName, String policyLocation,
			Delegation existingDelegation) throws SAXException, IOException {
		return WriteXMLUtil.saveDelegationPolicy(delegatorId, delegatorName,
				policyLocation, existingDelegation);
	}

	private void sendNotification(Delegation existingDelegation,
			String userProfileID, String userName, String userCollege,
			String userDepartment, String userPositionType,
			String userPositionTitle, String notificationMessage,
			String notificationType, boolean isCritical) {
		NotificationLog notification = new NotificationLog();
		// For Admin
		notification.setType(notificationType);
		notification.setAction(notificationMessage);
		notification.setUserProfileId(existingDelegation.getDelegateeId());
		notification.setUsername(existingDelegation.getDelegateeUsername());
		notification.setForAdmin(true);
		notification.setCritical(isCritical);
		// notification.isViewedByUser(true);
		notificationDAO.save(notification);
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
		notificationDAO.save(notification);
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
		notificationDAO.save(notification);
		// Broadcasting SSE
		OutboundEvent.Builder eventBuilder = new OutboundEvent.Builder();
		OutboundEvent event = eventBuilder.name("notification")
				.mediaType(MediaType.TEXT_PLAIN_TYPE).data(String.class, "1")
				.build();
		NotificationService.BROADCASTER.broadcast(event);
	}

	private boolean validateNotEmptyValue(String value) {
		if (!value.equalsIgnoreCase("")) {
			return true;
		} else {
			return false;
		}
	}

	@POST
	@Path("/RevokeDelegationByDelegationID")
	@ApiOperation(value = "Revoke Delegation by DelegationId", notes = "This API deletes Delegation by DelegationId")
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "Success: { True }"),
			@ApiResponse(code = 403, message = "Failed: { \"error\":\"error description\", \"status\": \"FAIL\" }") })
	public Response revokeDelegationByDelegationID(
			@ApiParam(value = "Message", required = true, defaultValue = "", allowableValues = "", allowMultiple = false) String message) {
		try {
			log.info("DelegationService::revokeDelegationByDelegationID started");
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
			String authorFullName = authorProfile.getFullName();
			String authorUserName = authorProfile.getUserAccount()
					.getUserName();
			StringBuffer contentProfile = generateContentDelegationProfile(
					delegationId, existingDelegation, authorProfile,
					authorFullName);
			Accesscontrol ac = new Accesscontrol();
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
									switch (assignment.getAttributeId()
											.toString()) {
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
										delegatorEmail = assignment
												.getContent();
										break;
									case "delegateeEmail":
										if (!assignment.getContent().equals("")) {
											emaillist.add(assignment
													.getContent());
										}
										break;
									}
								}
							}
						}
					}
					boolean isRevoked = delegationDAO.revokeDelegation(
							existingDelegation, authorProfile);
					if (isRevoked) {
						return sendNotificationAfterRevoke(mapper, userInfo,
								existingDelegation, authorUserName, emailUtil,
								emailSubject, emailBody, delegatorName,
								delegatorEmail, emaillist);
					} else {
						return Response
								.status(200)
								.type(MediaType.APPLICATION_JSON)
								.entity(mapper.writerWithDefaultPrettyPrinter()
										.writeValueAsString(true)).build();
					}
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
			log.error("Could not revoke the selected Delegation error e=", e);
		}
		return Response
				.status(403)
				.entity("{\"error\": \"Could not revoke the selected Delegation\", \"status\": \"FAIL\"}")
				.build();
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
	private Response sendNotificationAfterRevoke(ObjectMapper mapper,
			GPMSCommonInfo userInfo, Delegation existingDelegation,
			String authorUserName, EmailUtil emailUtil, String emailSubject,
			String emailBody, String delegatorName, String delegatorEmail,
			List<String> emaillist) throws JsonProcessingException {
		if (!emailSubject.equals("")) {
			emailUtil.sendMailMultipleUsersWithoutAuth(delegatorEmail,
					emaillist, emailSubject + delegatorName, emailBody);
		}
		String notificationMessage = "Delegation Revoked by " + authorUserName
				+ ".";
		sendNotification(existingDelegation, userInfo.getUserProfileID(),
				userInfo.getUserName(), userInfo.getUserCollege(),
				userInfo.getUserDepartment(), userInfo.getUserPositionType(),
				userInfo.getUserPositionTitle(), notificationMessage,
				"Delegation", true);
		// Delete the Delegation Dynamic Policy File here
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

	/**
	 * Generates Content Profile for Delegation
	 * 
	 * @param delegationId
	 * @param existingDelegation
	 * @param authorProfile
	 * @param authorFullName
	 * @return
	 */
	private StringBuffer generateContentDelegationProfile(String delegationId,
			Delegation existingDelegation, UserProfile authorProfile,
			String authorFullName) {
		StringBuffer contentProfile = new StringBuffer();
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
}
