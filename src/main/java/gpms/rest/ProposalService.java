package gpms.rest;

import gpms.DAL.MongoDBConnector;
import gpms.accesscontrol.Accesscontrol;
import gpms.dao.DelegationDAO;
import gpms.dao.NotificationDAO;
import gpms.dao.ProposalDAO;
import gpms.dao.UserAccountDAO;
import gpms.dao.UserProfileDAO;
import gpms.model.ApprovalType;
import gpms.model.AuditLogCommonInfo;
import gpms.model.AuditLogInfo;
import gpms.model.DeleteType;
import gpms.model.EmailCommonInfo;
import gpms.model.GPMSCommonInfo;
import gpms.model.InvestigatorInfo;
import gpms.model.InvestigatorRefAndPosition;
import gpms.model.NotificationLog;
import gpms.model.Proposal;
import gpms.model.ProposalCommonInfo;
import gpms.model.ProposalInfo;
import gpms.model.ProposalStatusInfo;
import gpms.model.SignatureByAllUsers;
import gpms.model.SignatureInfo;
import gpms.model.SignatureUserInfo;
import gpms.model.Status;
import gpms.model.SubmitType;
import gpms.model.UserAccount;
import gpms.model.UserProfile;
import gpms.utils.EmailUtil;
import gpms.utils.SerializationHelper;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

import java.io.IOException;
import java.net.UnknownHostException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
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

import org.apache.log4j.Logger;
import org.bson.types.ObjectId;
import org.glassfish.jersey.media.sse.OutboundEvent;
import org.glassfish.jersey.media.sse.OutboundEvent.Builder;
import org.mongodb.morphia.Morphia;
import org.wso2.balana.ObligationResult;
import org.wso2.balana.ctx.AbstractResult;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.mongodb.MongoClient;

@Path("/proposals")
@Api(value = "/proposals", description = "Manage Proposals")
@Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML,
		MediaType.APPLICATION_FORM_URLENCODED, MediaType.TEXT_PLAIN,
		MediaType.TEXT_HTML })
@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML,
		MediaType.TEXT_PLAIN, MediaType.TEXT_HTML })
public class ProposalService {
	MongoClient mongoClient = null;
	Morphia morphia = null;
	String dbName = "db_gpms";
	UserAccountDAO userAccountDAO = null;
	public UserProfileDAO userProfileDAO = null;
	public ProposalDAO proposalDAO = null;
	DelegationDAO delegationDAO = null;
	public NotificationDAO notificationDAO = null;
	public DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
	private static final Logger log = Logger.getLogger(ProposalService.class
			.getName());

	public ProposalService() {
		mongoClient = MongoDBConnector.getMongo();
		morphia = new Morphia();
		morphia.map(UserProfile.class).map(UserAccount.class);
		userAccountDAO = new UserAccountDAO(mongoClient, morphia, dbName);
		userProfileDAO = new UserProfileDAO(mongoClient, morphia, dbName);
		proposalDAO = new ProposalDAO(mongoClient, morphia, dbName);
		delegationDAO = new DelegationDAO(mongoClient, morphia, dbName);
		notificationDAO = new NotificationDAO(mongoClient, morphia, dbName);
	}

	@GET
	@Produces(MediaType.TEXT_PLAIN)
	@ApiOperation(value = "Test Proposal Service", notes = "This API tests whether the service is working or not")
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "Success: { Hello World! }"),
			@ApiResponse(code = 400, message = "Failed: { \"error\":\"error description\", \"status\": \"FAIL\" }") })
	public Response testService() {
		try {
			log.info("ProposalService::testService started");
			return Response.status(Response.Status.OK).entity("Hello World!")
					.build();
		} catch (Exception e) {
			log.error("Could not connect the Proposal Service error e=", e);
		}
		return Response
				.status(Response.Status.BAD_REQUEST)
				.entity("{\"error\": \"Could Not Find Proposal Service\", \"status\": \"FAIL\"}")
				.build();
	}

	@POST
	@Path("/GetProposalStatusList")
	@ApiOperation(value = "Get all Proposal Status", notes = "This API gets all Proposal Status for dropdown")
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "Success: { Proposal Status Info }"),
			@ApiResponse(code = 400, message = "Failed: { \"error\":\"error description\", \"status\": \"FAIL\" }") })
	public Response getProposalStatusList() {
		try {
			log.info("ProposalService::getProposalStatusList started");
			ObjectMapper mapper = new ObjectMapper();
			List<ProposalStatusInfo> proposalStatusList = new ArrayList<ProposalStatusInfo>();
			for (Status status : Status.values()) {
				ProposalStatusInfo proposalStatus = new ProposalStatusInfo();
				proposalStatus.setStatusKey(status.name());
				proposalStatus.setStatusValue(status.toString());
				proposalStatusList.add(proposalStatus);
			}
			return Response
					.status(Response.Status.OK)
					.entity(mapper.writerWithDefaultPrettyPrinter()
							.writeValueAsString(proposalStatusList)).build();
		} catch (Exception e) {
			log.error("Could not find all Proposal Status error e=", e);
		}
		return Response
				.status(Response.Status.BAD_REQUEST)
				.entity("{\"error\": \"Could Not Find All Proposal Status\", \"status\": \"FAIL\"}")
				.build();
	}

	@POST
	@Path("/GetProposalsList")
	@ApiOperation(value = "Get all Proposals", notes = "This API gets all Proposals")
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "Success: { Proposal Info }"),
			@ApiResponse(code = 400, message = "Failed: { \"error\":\"error description\", \"status\": \"FAIL\" }") })
	public Response produceProposalsJSON(
			@ApiParam(value = "Message", required = true, defaultValue = "", allowableValues = "", allowMultiple = false) String message) {
		try {
			log.info("ProposalService::produceProposalsJSON started");
			List<ProposalInfo> proposals = new ArrayList<ProposalInfo>();
			int offset = 0, limit = 0;
			ObjectMapper mapper = new ObjectMapper();
			JsonNode root = mapper.readTree(message);
			if (root != null && root.has("offset")) {
				offset = root.get("offset").intValue();
			}
			if (root != null && root.has("limit")) {
				limit = root.get("limit").intValue();
			}
			ProposalCommonInfo proposalInfo = new ProposalCommonInfo();
			if (root != null && root.has("proposalBindObj")) {
				JsonNode proposalObj = root.get("proposalBindObj");
				proposalInfo = new ProposalCommonInfo(proposalObj);
			}
			proposals = proposalDAO.findAllForProposalGrid(offset, limit,
					proposalInfo);
			return Response
					.status(Response.Status.OK)
					.entity(mapper.writerWithDefaultPrettyPrinter()
							.writeValueAsString(proposals)).build();
		} catch (Exception e) {
			log.error("Could not find all Proposals error e=", e);
		}
		return Response
				.status(Response.Status.BAD_REQUEST)
				.entity("{\"error\": \"Could Not Find All Proposals\", \"status\": \"FAIL\"}")
				.build();
	}

	@POST
	@Path("/GetUserProposalsList")
	@ApiOperation(value = "Get all Users", notes = "This API get all Proposals of a User")
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "Success: { Proposal Info }"),
			@ApiResponse(code = 400, message = "Failed: { \"error\":\"error description\", \"status\": \"FAIL\" }") })
	public Response produceUserProposalsJSON(
			@ApiParam(value = "Message", required = true, defaultValue = "", allowableValues = "", allowMultiple = false) String message) {
		try {
			log.info("ProposalService::produceUserProposalsJSON started");
			List<ProposalInfo> proposals = new ArrayList<ProposalInfo>();
			int offset = 0, limit = 0;
			ObjectMapper mapper = new ObjectMapper();
			JsonNode root = mapper.readTree(message);
			if (root != null && root.has("offset")) {
				offset = root.get("offset").intValue();
			}
			if (root != null && root.has("limit")) {
				limit = root.get("limit").intValue();
			}
			ProposalCommonInfo proposalInfo = new ProposalCommonInfo();
			if (root != null && root.has("proposalBindObj")) {
				JsonNode proposalObj = root.get("proposalBindObj");
				proposalInfo = new ProposalCommonInfo(proposalObj);
			}
			GPMSCommonInfo userInfo = new GPMSCommonInfo();
			if (root != null && root.has("gpmsCommonObj")) {
				JsonNode commonObj = root.get("gpmsCommonObj");
				userInfo = new GPMSCommonInfo(commonObj);
			}
			proposals = proposalDAO.findUserProposalGrid(offset, limit,
					proposalInfo, userInfo);
			return Response
					.status(Response.Status.OK)
					.entity(mapper.writerWithDefaultPrettyPrinter()
							.writeValueAsString(proposals)).build();
		} catch (Exception e) {
			log.error("Could not find all Proposals of a User error e=", e);
		}
		return Response
				.status(Response.Status.BAD_REQUEST)
				.entity("{\"error\": \"Could Not Find All Proposals Of A User\", \"status\": \"FAIL\"}")
				.build();
	}

	@POST
	@Path("/AllProposalsExportToExcel")
	@Produces(MediaType.TEXT_HTML)
	@ApiOperation(value = "Export all Proposals in a grid", notes = "This API exports all Proposals shown in a grid For Admin")
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "Success: { Excel Filename/ No Record}"),
			@ApiResponse(code = 400, message = "Failed: { \"error\":\"error description\", \"status\": \"FAIL\" }") })
	public Response exportAllProposalsJSON(
			@ApiParam(value = "Message", required = true, defaultValue = "", allowableValues = "", allowMultiple = false) String message) {
		try {
			log.info("ProposalService::exportAllProposalsJSON started");
			List<ProposalInfo> proposals = new ArrayList<ProposalInfo>();
			ObjectMapper mapper = new ObjectMapper();
			JsonNode root = mapper.readTree(message);
			ProposalCommonInfo proposalInfo = new ProposalCommonInfo();
			if (root != null && root.has("proposalBindObj")) {
				JsonNode proposalObj = root.get("proposalBindObj");
				proposalInfo = new ProposalCommonInfo(proposalObj);
			}
			proposals = proposalDAO.findAllProposals(proposalInfo);
			String filename = new String();
			if (proposals.size() > 0) {
				filename = proposalDAO.exportToExcelFile(proposals, null);
			} else {
				filename = mapper.writerWithDefaultPrettyPrinter()
						.writeValueAsString("No Record");
			}
			return Response.status(Response.Status.OK).entity(filename).build();
		} catch (Exception e) {
			log.error("Could not export Proposal list error e=", e);
		}
		return Response
				.status(Response.Status.BAD_REQUEST)
				.entity("{\"error\": \"Could Not Export Proposal List\", \"status\": \"FAIL\"}")
				.build();
	}

	@POST
	@Path("/ProposalsExportToExcel")
	@Produces(MediaType.TEXT_HTML)
	@ApiOperation(value = "Export all Proposals in a grid", notes = "This API exports all Proposals shown in a grid")
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "Success: { Excel Filename/ No Record}"),
			@ApiResponse(code = 400, message = "Failed: { \"error\":\"error description\", \"status\": \"FAIL\" }") })
	public Response exportProposalsJSON(
			@ApiParam(value = "Message", required = true, defaultValue = "", allowableValues = "", allowMultiple = false) String message) {
		try {
			log.info("ProposalService::exportProposalsJSON started");
			List<ProposalInfo> proposals = new ArrayList<ProposalInfo>();
			ObjectMapper mapper = new ObjectMapper();
			JsonNode root = mapper.readTree(message);
			ProposalCommonInfo proposalInfo = new ProposalCommonInfo();
			if (root != null && root.has("proposalBindObj")) {
				JsonNode proposalObj = root.get("proposalBindObj");
				proposalInfo = new ProposalCommonInfo(proposalObj);
			}
			GPMSCommonInfo userInfo = new GPMSCommonInfo();
			if (root != null && root.has("gpmsCommonObj")) {
				JsonNode commonObj = root.get("gpmsCommonObj");
				userInfo = new GPMSCommonInfo(commonObj);
			}
			proposals = proposalDAO
					.findAllUserProposals(proposalInfo, userInfo);
			String filename = new String();
			if (proposals.size() > 0) {
				filename = proposalDAO.exportToExcelFile(proposals, null);
			} else {
				filename = mapper.writerWithDefaultPrettyPrinter()
						.writeValueAsString("No Record");
			}
			return Response.status(Response.Status.OK).entity(filename).build();
		} catch (Exception e) {
			log.error("Could not export all Proposals error e=", e);
		}
		return Response
				.status(Response.Status.BAD_REQUEST)
				.entity("{\"error\": \"Could Not Export All Proposals\", \"status\": \"FAIL\"}")
				.build();
	}

	@POST
	@Path("/DeleteProposalByAdmin")
	@ApiOperation(value = "Delete Proposal by Admin", notes = "This API deletes Proposal by Admin")
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "Success: { True }"),
			@ApiResponse(code = 400, message = "Failed: { \"error\":\"error description\", \"status\": \"FAIL\" }") })
	public Response deleteProposalByAdmin(
			@ApiParam(value = "Message", required = true, defaultValue = "", allowableValues = "", allowMultiple = false) String message) {
		try {
			log.info("ProposalService::deleteProposalByAdmin started");
			ObjectMapper mapper = new ObjectMapper();
			JsonNode root = mapper.readTree(message);
			String proposalId = new String();
			if (root != null && root.has("proposalId")) {
				proposalId = root.get("proposalId").textValue();
			}
			GPMSCommonInfo userInfo = new GPMSCommonInfo();
			if (root != null && root.has("gpmsCommonObj")) {
				JsonNode commonObj = root.get("gpmsCommonObj");
				userInfo = new GPMSCommonInfo(commonObj);
			}
			ObjectId id = new ObjectId(proposalId);
			Proposal existingProposal = proposalDAO
					.findProposalByProposalID(id);
			ObjectId authorId = new ObjectId(userInfo.getUserProfileID());
			UserProfile authorProfile = userProfileDAO
					.findUserDetailsByProfileID(authorId);
			String authorUserName = authorProfile.getUserAccount()
					.getUserName();
			boolean isDeleted = proposalDAO.deleteProposalByAdmin(
					existingProposal, authorProfile);
			if (isDeleted) {
				sendNotification(id, existingProposal, authorUserName);
				return Response
						.status(Response.Status.OK)
						.entity(mapper.writerWithDefaultPrettyPrinter()
								.writeValueAsString(true)).build();
			}
		} catch (Exception e) {
			log.error("Could not delete Proposal error e=", e);
		}
		return Response
				.status(Response.Status.BAD_REQUEST)
				.entity("{\"error\": \"Could Not Delete Proposal\", \"status\": \"FAIL\"}")
				.build();
	}

	@POST
	@Path("/DeleteProposalByProposalID")
	@ApiOperation(value = "Delete Proposal by ProposalId", notes = "This API deletes Proposal by ProposalId")
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "Success: { True }"),
			@ApiResponse(code = 403, message = "Failed: { \"error\":\"error description\", \"status\": \"FAIL\" }") })
	public Response deleteProposalByProposalID(
			@ApiParam(value = "Message", required = true, defaultValue = "", allowableValues = "", allowMultiple = false) String message) {
		try {
			log.info("ProposalService::deleteProposalByProposalID started");
			ObjectMapper mapper = new ObjectMapper();
			JsonNode root = mapper.readTree(message);
			if (root != null && root.has("policyInfo")) {
				JsonNode policyInfo = root.get("policyInfo");
				if (policyInfo != null && policyInfo.isArray()
						&& policyInfo.size() > 0) {
					String proposalId = new String();
					String proposalRoles = new String();
					String proposalUserTitle = new String();
					if (root != null && root.has("proposalId")) {
						proposalId = root.get("proposalId").textValue();
					}
					if (root != null && root.has("proposalRoles")) {
						proposalRoles = root.get("proposalRoles").textValue();
					}
					if (root != null && root.has("proposalUserTitle")) {
						proposalUserTitle = root.get("proposalUserTitle")
								.textValue();
					}
					GPMSCommonInfo userInfo = new GPMSCommonInfo();
					if (root != null && root.has("gpmsCommonObj")) {
						JsonNode commonObj = root.get("gpmsCommonObj");
						userInfo = new GPMSCommonInfo(commonObj);
					}
					ObjectId id = new ObjectId(proposalId);
					Proposal existingProposal = proposalDAO
							.findProposalByProposalID(id);
					List<SignatureUserInfo> signatures = proposalDAO
							.findSignaturesExceptInvestigator(id,
									existingProposal.isIrbApprovalRequired());
					ObjectId authorId = new ObjectId(
							userInfo.getUserProfileID());
					UserProfile authorProfile = userProfileDAO
							.findUserDetailsByProfileID(authorId);
					String authorUserName = authorProfile.getUserAccount()
							.getUserName();
					StringBuffer contentProfile = proposalDAO
							.generateContentProfile(proposalId,
									existingProposal, signatures, authorProfile);
					HashMap<String, Multimap<String, String>> attrMap = proposalDAO
							.generateAttributes(policyInfo);
					Accesscontrol ac = new Accesscontrol();
					Set<AbstractResult> set = ac
							.getXACMLdecisionWithObligations(attrMap,
									contentProfile);
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
						System.out.println("Decision:" + intDecision
								+ " that is: "
								+ AbstractResult.DECISIONS[intDecision]);
						if (AbstractResult.DECISIONS[intDecision]
								.equals("Permit")) {
							List<ObligationResult> obligations = ar
									.getObligations();
							boolean isDeleted = proposalDAO
									.deleteProposalWithObligations(
											proposalRoles, proposalUserTitle,
											existingProposal, authorProfile,
											authorUserName, obligations);
							if (isDeleted) {
								sendDeleteNotification(proposalRoles,
										proposalUserTitle, existingProposal,
										authorUserName);
							}
							return Response
									.status(200)
									.type(MediaType.APPLICATION_JSON)
									.entity(mapper
											.writerWithDefaultPrettyPrinter()
											.writeValueAsString(true)).build();
						} else {
							return Response
									.status(403)
									.type(MediaType.APPLICATION_JSON)
									.entity("Your permission is: "
											+ AbstractResult.DECISIONS[intDecision])
									.build();
						}
					}
				} else {
					return Response.status(403)
							.type(MediaType.APPLICATION_JSON)
							.entity("No User Permission Attributes are send!")
							.build();
				}
			}
		} catch (Exception e) {
			log.error("Could not delete the selected Proposal error e=", e);
		}
		return Response
				.status(403)
				.entity("{\"error\": \"No User Permission Attributes are send!\", \"status\": \"FAIL\"}")
				.build();
	}

	@POST
	@Path("/DeleteMultipleProposalsByAdmin")
	@ApiOperation(value = "Delete Multiple Proposals by Admin", notes = "This API deletes multiple Proposals by Admin")
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "Success: { True }"),
			@ApiResponse(code = 400, message = "Failed: { \"error\":\"error description\", \"status\": \"FAIL\" }") })
	public Response deleteMultipleProposalsByAdmin(
			@ApiParam(value = "Message", required = true, defaultValue = "", allowableValues = "", allowMultiple = false) String message) {
		try {
			log.info("ProposalService::deleteMultipleProposalsByAdmin started");
			String proposalIds = new String();
			String proposals[] = new String[0];
			ObjectMapper mapper = new ObjectMapper();
			JsonNode root = mapper.readTree(message);
			if (root != null && root.has("proposalIds")) {
				proposalIds = root.get("proposalIds").textValue();
				proposals = proposalIds.split(",");
			}
			GPMSCommonInfo userInfo = new GPMSCommonInfo();
			if (root != null && root.has("gpmsCommonObj")) {
				JsonNode commonObj = root.get("gpmsCommonObj");
				userInfo = new GPMSCommonInfo(commonObj);
			}
			ObjectId authorId = new ObjectId(userInfo.getUserProfileID());
			UserProfile authorProfile = userProfileDAO
					.findUserDetailsByProfileID(authorId);
			String authorUserName = authorProfile.getUserAccount()
					.getUserName();
			for (String proposalId : proposals) {
				ObjectId id = new ObjectId(proposalId);
				Proposal existingProposal = proposalDAO
						.findProposalByProposalID(id);
				boolean isDeleted = proposalDAO.deleteProposalByAdmin(
						existingProposal, authorProfile);
				if (isDeleted) {
					sendNotification(id, existingProposal, authorUserName);
				}
			}
			return Response
					.status(Response.Status.OK)
					.entity(mapper.writerWithDefaultPrettyPrinter()
							.writeValueAsString(true)).build();
		} catch (Exception e) {
			log.error("Could not delete Multiple Proposals error e=", e);
		}
		return Response
				.status(Response.Status.BAD_REQUEST)
				.entity("{\"error\": \"Could Not Delete Multiple Proposals\", \"status\": \"FAIL\"}")
				.build();
	}

	@POST
	@Path("/GetAvailableActionsByProposalId")
	@ApiOperation(value = "Get Proposal Details by ProposalId", notes = "This API gets Proposal Details by ProposalId")
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "Success: { Proposal }"),
			@ApiResponse(code = 400, message = "Failed: { \"error\":\"error description\", \"status\": \"FAIL\" }") })
	public Response produceAvailableActionsByProposalId(
			@ApiParam(value = "Message", required = true, defaultValue = "", allowableValues = "", allowMultiple = false) String message) {
		try {
			log.info("ProposalService::produceAvailableActionsByProposalId started");
			String proposalId = new String();
			String proposalRoles = new String();
			HashMap<String, Multimap<String, String>> attrMap = new HashMap<String, Multimap<String, String>>();
			Multimap<String, String> subjectMap = ArrayListMultimap.create();
			Multimap<String, String> resourceMap = ArrayListMultimap.create();
			Multimap<String, String> actionMap = ArrayListMultimap.create();
			ObjectMapper mapper = new ObjectMapper();
			JsonNode root = mapper.readTree(message);
			if (root != null && root.has("proposalId")) {
				proposalId = root.get("proposalId").textValue();
			}
			if (root != null && root.has("proposalRoles")) {
				proposalRoles = root.get("proposalRoles").textValue();
			}
			GPMSCommonInfo userInfo = new GPMSCommonInfo();
			if (root != null && root.has("gpmsCommonObj")) {
				JsonNode commonObj = root.get("gpmsCommonObj");
				userInfo = new GPMSCommonInfo(commonObj);
			}
			if (proposalRoles.equals("") && proposalRoles.isEmpty()) {
				subjectMap.put("position.title",
						userInfo.getUserPositionTitle());
				attrMap.put("Subject", subjectMap);
			} else {
				subjectMap.put("proposal.role", proposalRoles);
				attrMap.put("Subject", subjectMap);
			}
			ObjectId id = new ObjectId(proposalId);
			Proposal existingProposal = proposalDAO
					.findProposalByProposalID(id);
			StringBuffer contentProfile = proposalDAO
					.generateProposalContentProfile(proposalId, userInfo,
							existingProposal);
			if (attrMap.get("Resource") == null) {
				attrMap.put("Resource", resourceMap);
			}
			List<String> actions = proposalDAO.generateMDPDecision(attrMap,
					actionMap, contentProfile);
			return Response
					.status(Response.Status.OK)
					.entity(mapper.setDateFormat(formatter)
							.writerWithDefaultPrettyPrinter()
							.writeValueAsString(actions)).build();
		} catch (Exception e) {
			log.error("Could not find Proposal Details by ProposalId error e=",
					e);
		}
		return Response
				.status(Response.Status.BAD_REQUEST)
				.entity("{\"error\": \"Could Not Find Proposal Details By ProposalId\", \"status\": \"FAIL\"}")
				.build();
	}

	@POST
	@Path("/GetProposalDetailsByProposalId")
	@ApiOperation(value = "Get Proposal Details by ProposalId", notes = "This API gets Proposal Details by ProposalId")
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "Success: { Proposal }"),
			@ApiResponse(code = 400, message = "Failed: { \"error\":\"error description\", \"status\": \"FAIL\" }") })
	public Response produceProposalDetailsByProposalId(
			@ApiParam(value = "Message", required = true, defaultValue = "", allowableValues = "", allowMultiple = false) String message) {
		try {
			log.info("ProposalService::produceProposalDetailsByProposalId started");
			Proposal proposal = new Proposal();
			String proposalId = new String();
			ObjectMapper mapper = new ObjectMapper();
			JsonNode root = mapper.readTree(message);
			if (root != null && root.has("proposalId")) {
				proposalId = root.get("proposalId").textValue();
			}
			ObjectId id = new ObjectId(proposalId);
			proposal = proposalDAO.findProposalByProposalID(id);
			return Response
					.status(Response.Status.OK)
					.entity(mapper.setDateFormat(formatter)
							.writerWithDefaultPrettyPrinter()
							.writeValueAsString(proposal)).build();
		} catch (Exception e) {
			log.error("Could not find Proposal Details by ProposalId error e=",
					e);
		}
		return Response
				.status(Response.Status.BAD_REQUEST)
				.entity("{\"error\": \"Could Not Find Proposal Details By ProposalId\", \"status\": \"FAIL\"}")
				.build();
	}

	@POST
	@Path("/GetProposalAuditLogList")
	@ApiOperation(value = "Get Proposal Audit Log List", notes = "This API gets Proposal Audit Log List")
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "Success: { AuditLog Info }"),
			@ApiResponse(code = 400, message = "Failed: { \"error\":\"error description\", \"status\": \"FAIL\" }") })
	public Response produceProposalAuditLogJSON(
			@ApiParam(value = "Message", required = true, defaultValue = "", allowableValues = "", allowMultiple = false) String message) {
		try {
			log.info("ProposalService::produceProposalAuditLogJSON started");
			List<AuditLogInfo> proposalAuditLogs = new ArrayList<AuditLogInfo>();
			int offset = 0, limit = 0;
			String proposalId = new String();
			ObjectMapper mapper = new ObjectMapper();
			JsonNode root = mapper.readTree(message);
			if (root != null && root.has("offset")) {
				offset = root.get("offset").intValue();
			}
			if (root != null && root.has("limit")) {
				limit = root.get("limit").intValue();
			}
			if (root != null && root.has("proposalId")) {
				proposalId = root.get("proposalId").textValue();
			}
			AuditLogCommonInfo auditLogInfo = new AuditLogCommonInfo();
			if (root != null && root.has("auditLogBindObj")) {
				JsonNode auditLogBindObj = root.get("auditLogBindObj");
				auditLogInfo = new AuditLogCommonInfo(auditLogBindObj);
			}
			ObjectId id = new ObjectId(proposalId);
			proposalAuditLogs = proposalDAO.findAllForProposalAuditLogGrid(
					offset, limit, id, auditLogInfo);
			return Response
					.status(Response.Status.OK)
					.entity(mapper.writerWithDefaultPrettyPrinter()
							.writeValueAsString(proposalAuditLogs)).build();
		} catch (Exception e) {
			log.error("Could not find Proposal Audit Log List error e=", e);
		}
		return Response
				.status(Response.Status.BAD_REQUEST)
				.entity("{\"error\": \"Could Not Find Proposal Audit Log List\", \"status\": \"FAIL\"}")
				.build();
	}

	@POST
	@Path("/ProposalLogsExportToExcel")
	@ApiOperation(value = "Export all Proposal Logs in a grid", notes = "This API exports all Proposal Logs shown in a grid")
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "Success: { Excel Filename/ No Record}"),
			@ApiResponse(code = 400, message = "Failed: { \"error\":\"error description\", \"status\": \"FAIL\" }") })
	public Response exportProposalAuditLogJSON(
			@ApiParam(value = "Message", required = true, defaultValue = "", allowableValues = "", allowMultiple = false) String message) {
		try {
			log.info("ProposalService::exportProposalAuditLogJSON started");
			List<AuditLogInfo> proposalAuditLogs = new ArrayList<AuditLogInfo>();
			String proposalId = new String();
			ObjectMapper mapper = new ObjectMapper();
			JsonNode root = mapper.readTree(message);
			if (root != null && root.has("proposalId")) {
				proposalId = root.get("proposalId").textValue();
			}
			AuditLogCommonInfo auditLogInfo = new AuditLogCommonInfo();
			if (root != null && root.has("auditLogBindObj")) {
				JsonNode auditLogBindObj = root.get("auditLogBindObj");
				auditLogInfo = new AuditLogCommonInfo(auditLogBindObj);
			}
			ObjectId id = new ObjectId(proposalId);
			proposalAuditLogs = proposalDAO.findAllUserProposalAuditLogs(id,
					auditLogInfo);
			String filename = new String();
			if (proposalAuditLogs.size() > 0) {
				filename = proposalDAO.exportToExcelFile(null,
						proposalAuditLogs);
			} else {
				filename = mapper.writerWithDefaultPrettyPrinter()
						.writeValueAsString("No Record");
			}
			return Response.status(Response.Status.OK).entity(filename).build();
		} catch (Exception e) {
			log.error("Could not export Proposal Logs error e=", e);
		}
		return Response
				.status(Response.Status.BAD_REQUEST)
				.entity("{\"error\": \"Could Not Proposal Logs List\", \"status\": \"FAIL\"}")
				.build();
	}

	@POST
	@Path("/CheckUniqueProjectTitle")
	@ApiOperation(value = "Check for Unique Project Title", notes = "This API checks if provided Project Title is Unique or not")
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "Success: { True/ False }"),
			@ApiResponse(code = 400, message = "Failed: { \"error\":\"error description\", \"status\": \"FAIL\" }") })
	public Response checkUniqueProjectTitle(
			@ApiParam(value = "Message", required = true, defaultValue = "", allowableValues = "", allowMultiple = false) String message) {
		try {
			log.info("ProposalService::checkUniqueProjectTitle started");
			String proposalID = new String();
			String newProjectTitle = new String();
			String response = new String();
			ObjectMapper mapper = new ObjectMapper();
			JsonNode root = mapper.readTree(message);
			if (root != null && root.has("proposalUniqueObj")) {
				JsonNode proposalUniqueObj = root.get("proposalUniqueObj");
				if (proposalUniqueObj != null
						&& proposalUniqueObj.has("ProposalID")) {
					proposalID = proposalUniqueObj.get("ProposalID")
							.textValue();
				}
				if (proposalUniqueObj != null
						&& proposalUniqueObj.has("NewProjectTitle")) {
					String projectTitle = proposalUniqueObj
							.get("NewProjectTitle").textValue().trim()
							.replaceAll("\\<[^>]*>", "");
					if (proposalDAO.validateNotEmptyValue(projectTitle)) {
						newProjectTitle = projectTitle;
					} else {
						return Response.status(403)
								.entity("The Project Title can not be Empty")
								.build();
					}
				}
			}
			Proposal proposal = new Proposal();
			if (!proposalID.equals("0")) {
				ObjectId id = new ObjectId(proposalID);
				proposal = proposalDAO.findNextProposalWithSameProjectTitle(id,
						newProjectTitle);
			} else {
				proposal = proposalDAO
						.findAnyProposalWithSameProjectTitle(newProjectTitle);
			}
			if (proposal != null) {
				response = mapper.writerWithDefaultPrettyPrinter()
						.writeValueAsString("false");
			} else {
				response = mapper.writerWithDefaultPrettyPrinter()
						.writeValueAsString("true");
			}
			return Response.status(Response.Status.OK).entity(response).build();

		} catch (Exception e) {
			log.error("Could not check for unique Project Title error e=", e);
		}
		return Response
				.status(Response.Status.BAD_REQUEST)
				.entity("{\"error\": \"Check For Unique Project Title\", \"status\": \"FAIL\"}")
				.build();
	}

	@POST
	@Path("/GetAllSignatureForAProposal")
	@ApiOperation(value = "Get all Signatures For A Proposal", notes = "This API gets all Signatures for a Proposal")
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "Success: { Signature Info }"),
			@ApiResponse(code = 400, message = "Failed: { \"error\":\"error description\", \"status\": \"FAIL\" }") })
	public Response getAllSignatureForAProposal(
			@ApiParam(value = "Message", required = true, defaultValue = "", allowableValues = "", allowMultiple = false) String message) {
		try {
			log.info("ProposalService::getAllSignatureForAProposal started");
			String proposalId = new String();
			Boolean irbApprovalRequired = false;
			ObjectMapper mapper = new ObjectMapper();
			JsonNode root = mapper.readTree(message);
			if (root != null && root.has("proposalId")) {
				proposalId = root.get("proposalId").textValue();
			}
			if (root != null && root.has("irbApprovalRequired")) {
				irbApprovalRequired = Boolean.parseBoolean(root.get(
						"irbApprovalRequired").textValue());
			}
			ObjectId id = new ObjectId(proposalId);
			List<SignatureInfo> signatures = proposalDAO
					.findAllSignatureForAProposal(id, irbApprovalRequired);
			return Response
					.status(Response.Status.OK)
					.entity(mapper.writerWithDefaultPrettyPrinter()
							.writeValueAsString(signatures)).build();
		} catch (Exception e) {
			log.error("Could not find all Signatures for a Proposal error e=",
					e);
		}
		return Response
				.status(Response.Status.BAD_REQUEST)
				.entity("{\"error\": \"Could Not Find All Signatures For A Proposal\", \"status\": \"FAIL\"}")
				.build();
	}

	@POST
	@Path("/UpdateProposalStatus")
	@ApiOperation(value = "Update Proposal Status", notes = "This API updates Proposal Status")
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "Success: { True }"),
			@ApiResponse(code = 400, message = "Failed: { \"error\":\"error description\", \"status\": \"FAIL\" }") })
	public Response updateProposalStatus(
			@ApiParam(value = "Message", required = true, defaultValue = "", allowableValues = "", allowMultiple = false) String message) {
		try {
			log.info("ProposalService::updateProposalStatus started");
			ObjectMapper mapper = new ObjectMapper();
			JsonNode root = mapper.readTree(message);
			if (root != null && root.has("policyInfo")) {
				JsonNode policyInfo = root.get("policyInfo");
				if (policyInfo != null && policyInfo.isArray()
						&& policyInfo.size() > 0) {
					String proposalId = new String();
					String proposalUserTitle = new String();
					String buttonType = new String();
					if (root != null && root.has("proposalId")) {
						proposalId = root.get("proposalId").textValue();
					}
					if (root != null && root.has("proposalUserTitle")) {
						proposalUserTitle = root.get("proposalUserTitle")
								.textValue();
					}
					if (root != null && root.has("buttonType")) {
						buttonType = root.get("buttonType").textValue();
					}
					GPMSCommonInfo userInfo = new GPMSCommonInfo();
					if (root != null && root.has("gpmsCommonObj")) {
						JsonNode commonObj = root.get("gpmsCommonObj");
						userInfo = new GPMSCommonInfo(commonObj);
					}
					ObjectId id = new ObjectId(proposalId);
					Proposal existingProposal = proposalDAO
							.findProposalByProposalID(id);
					List<SignatureUserInfo> signatures = proposalDAO
							.findSignaturesExceptInvestigator(id,
									existingProposal.isIrbApprovalRequired());
					ObjectId authorId = new ObjectId(
							userInfo.getUserProfileID());
					UserProfile authorProfile = userProfileDAO
							.findUserDetailsByProfileID(authorId);
					String authorUserName = authorProfile.getUserAccount()
							.getUserName();
					StringBuffer contentProfile = proposalDAO
							.generateContentProfile(proposalId,
									existingProposal, signatures, authorProfile);
					Accesscontrol ac = new Accesscontrol();
					HashMap<String, Multimap<String, String>> attrMap = proposalDAO
							.generateAttributes(policyInfo);
					Set<AbstractResult> set = ac
							.getXACMLdecisionWithObligations(attrMap,
									contentProfile);
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
						System.out.println("Decision:" + intDecision
								+ " that is: "
								+ AbstractResult.DECISIONS[intDecision]);
						if (AbstractResult.DECISIONS[intDecision]
								.equals("Permit")) {
							List<ObligationResult> obligations = ar
									.getObligations();
							String changeDone = proposalDAO
									.updateProposalStatusWithObligations(
											proposalId, buttonType,
											proposalUserTitle,
											existingProposal, authorProfile,
											authorUserName, obligations);
							String notificationMessage = changeDone + " by "
									+ authorUserName + ".";
							if (changeDone.equals("Withdrawn")) {
								broadCastNotification(existingProposal.getId()
										.toString(), existingProposal
										.getProjectInfo().getProjectTitle(),
										notificationMessage, "Proposal", true,
										true, true, true, true, true, true,
										true, true, false);
							} else if (changeDone.equals("Archived")) {
								broadCastNotification(existingProposal.getId()
										.toString(), existingProposal
										.getProjectInfo().getProjectTitle(),
										notificationMessage, "Proposal", true,
										true, true, true, false, false, false,
										false, false, false);
							}
							return Response
									.status(200)
									.type(MediaType.APPLICATION_JSON)
									.entity(mapper
											.writerWithDefaultPrettyPrinter()
											.writeValueAsString(true)).build();
						} else {
							return Response
									.status(403)
									.type(MediaType.APPLICATION_JSON)
									.entity("Your permission is: "
											+ AbstractResult.DECISIONS[intDecision])
									.build();
						}
					}
				} else {
					return Response.status(403)
							.type(MediaType.APPLICATION_JSON)
							.entity("No User Permission Attributes are send!")
							.build();
				}
			}
		} catch (Exception e) {
			log.error("Could not update Proposal Status error e=", e);
		}
		return Response
				.status(403)
				.entity("{\"error\": \"No User Permission Attributes are send!\", \"status\": \"FAIL\"}")
				.build();
	}

	@POST
	@Path("/SaveUpdateProposalByAdmin")
	@ApiOperation(value = "Save a New Proposal or Update an existing Proposal by Admin", notes = "This API saves a New User or updates an existing Proposal by Admin")
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "Success: { True }"),
			@ApiResponse(code = 403, message = "Failed: { \"error\":\"error description\", \"status\": \"FAIL\" }") })
	public Response saveUpdateProposalByAdmin(
			@ApiParam(value = "Message", required = true, defaultValue = "", allowableValues = "", allowMultiple = false) String message) {
		try {
			log.info("ProposalService::saveUpdateProposalByAdmin started");
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
			String proposalId = new String();
			Proposal existingProposal = new Proposal();
			Proposal oldProposal = new Proposal();
			if (root != null && root.has("proposalInfo")) {
				JsonNode proposalInfo = root.get("proposalInfo");
				if (proposalInfo != null && proposalInfo.has("ProposalID")) {
					proposalId = proposalInfo.get("ProposalID").textValue();
					if (!proposalId.equals("0")) {
						ObjectId id = new ObjectId(proposalId);
						existingProposal = proposalDAO
								.findProposalByProposalID(id);
						oldProposal = SerializationHelper
								.cloneThroughSerialize(existingProposal);
					}
				}
				proposalDAO.getAppendixDetails(proposalId, existingProposal,
						oldProposal, proposalInfo);
				getInvestigatorInfoDetails(proposalId, existingProposal,
						oldProposal, proposalInfo);
				Boolean irbApprovalRequired = proposalDAO.getComplianceDetails(
						proposalId, existingProposal, proposalInfo);
				EmailCommonInfo emailDetails = proposalDAO
						.saveProposalWithoutObligations(message, proposalId,
								existingProposal, oldProposal, authorProfile,
								irbApprovalRequired);
				return sendSaveUpdateNotification(message, proposalId,
						existingProposal, oldProposal, authorProfile, true,
						null, irbApprovalRequired, null, emailDetails);
			} else {
				return Response.status(Response.Status.BAD_REQUEST)
						.type(MediaType.APPLICATION_JSON)
						.entity("No Proposal Info is send!").build();
			}
		} catch (Exception e) {
			log.error(
					"Could not save a New User or update an existing Proposal error e=",
					e);
		}

		return Response
				.status(403)
				.entity("{\"error\": \"Could Not Save A New User OR Update AN Existing Proposal\", \"status\": \"FAIL\"}")
				.build();
	}

	@POST
	@Path("/SaveUpdateProposal")
	@ApiOperation(value = "Save a New Proposal or Update an existing Proposal", notes = "This API saves a New Proposal or updates an existing Proposal")
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "Success: { True }"),
			@ApiResponse(code = 403, message = "Failed: { \"error\":\"error description\", \"status\": \"FAIL\" }") })
	public Response saveUpdateProposal(
			@ApiParam(value = "Message", required = true, defaultValue = "", allowableValues = "", allowMultiple = false) String message) {
		try {
			log.info("ProposalService::saveUpdateProposal started");
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
			String authorFullName = authorProfile.getFullName();
			String proposalId = new String();
			Proposal existingProposal = new Proposal();
			Proposal oldProposal = new Proposal();
			boolean signedByCurrentUser = false;
			StringBuffer contentProfile = new StringBuffer();
			Accesscontrol ac = new Accesscontrol();
			if (root != null && root.has("proposalInfo")) {
				JsonNode proposalInfo = root.get("proposalInfo");
				if (proposalInfo != null && proposalInfo.has("ProposalID")) {
					proposalId = proposalInfo.get("ProposalID").textValue();
					if (!proposalId.equals("0")) {
						ObjectId id = new ObjectId(proposalId);
						existingProposal = proposalDAO
								.findProposalByProposalID(id);
						oldProposal = SerializationHelper
								.cloneThroughSerialize(existingProposal);
					}
				}
				proposalDAO.getAppendixDetails(proposalId, existingProposal,
						oldProposal, proposalInfo);
				getInvestigatorInfoDetails(proposalId, existingProposal,
						oldProposal, proposalInfo);
				Boolean irbApprovalRequired = proposalDAO.getComplianceDetails(
						proposalId, existingProposal, proposalInfo);
				signedByCurrentUser = proposalDAO.getSignatureDetails(userInfo,
						proposalId, existingProposal, signedByCurrentUser,
						proposalInfo);
				if (root != null && root.has("policyInfo")) {
					JsonNode policyInfo = root.get("policyInfo");
					if (policyInfo != null && policyInfo.isArray()
							&& policyInfo.size() > 0) {
						HashMap<String, Multimap<String, String>> attrMap = proposalDAO
								.generateAttributes(policyInfo);
						List<SignatureUserInfo> signatures = new ArrayList<SignatureUserInfo>();
						SignatureByAllUsers signByAllUsersInfo = new SignatureByAllUsers();
						signatures = proposalDAO
								.generateProposalContentProfile(authorProfile,
										authorFullName, proposalId,
										existingProposal, signedByCurrentUser,
										contentProfile, irbApprovalRequired,
										signatures, signByAllUsersInfo);
						Set<AbstractResult> set = ac
								.getXACMLdecisionWithObligations(attrMap,
										contentProfile);
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
							System.out.println("Decision:" + intDecision
									+ " that is: "
									+ AbstractResult.DECISIONS[intDecision]);
							if (AbstractResult.DECISIONS[intDecision]
									.equals("Permit")) {
								List<ObligationResult> obligations = ar
										.getObligations();
								EmailCommonInfo emailDetails = proposalDAO
										.saveProposalWithObligations(obligations);
								return sendSaveUpdateNotification(message,
										proposalId, existingProposal,
										oldProposal, authorProfile, false,
										signatures, irbApprovalRequired,
										signByAllUsersInfo, emailDetails);
							} else {
								return Response
										.status(403)
										.type(MediaType.APPLICATION_JSON)
										.entity("Your permission is: "
												+ AbstractResult.DECISIONS[intDecision])
										.build();
							}
						}
					}
				}
			} else {
				return Response.status(403).type(MediaType.APPLICATION_JSON)
						.entity("No Proposal Info is send!").build();
			}
			return Response.status(403).type(MediaType.APPLICATION_JSON)
					.entity("No User Permission Attributes are send!").build();
		} catch (Exception e) {
			log.error(
					"Could not save a New Proposal or update an existing Proposal error e=",
					e);
		}
		return Response
				.status(403)
				.entity("{\"error\": \"Could Not Save A New Proposal OR Update AN Existing Proposal\", \"status\": \"FAIL\"}")
				.build();
	}

	@POST
	@Path("/CheckPermissionForAProposal")
	@Produces(MediaType.APPLICATION_JSON)
	@ApiOperation(value = "Check permission for a Proposal", notes = "This API checks permission for a Proposal")
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "Success: { True }"),
			@ApiResponse(code = 403, message = "Failed: { \"error\":\"error description\", \"status\": \"FAIL\" }") })
	public Response checkUserPermissionForAProposal(
			@ApiParam(value = "Message", required = true, defaultValue = "", allowableValues = "", allowMultiple = false) String message) {
		try {
			log.info("ProposalService::checkUserPermissionForAProposal started");
			ObjectMapper mapper = new ObjectMapper();
			JsonNode root = mapper.readTree(message);
			if (root != null && root.has("policyInfo")) {
				JsonNode policyInfo = root.get("policyInfo");
				if (policyInfo != null && policyInfo.isArray()
						&& policyInfo.size() > 0) {
					HashMap<String, Multimap<String, String>> attrMap = proposalDAO
							.generateAttributes(policyInfo);
					Accesscontrol ac = new Accesscontrol();
					String decision = ac.getXACMLdecision(attrMap);
					if (decision.equals("Permit")) {
						return Response
								.status(200)
								.type(MediaType.APPLICATION_JSON)
								.entity(mapper.writerWithDefaultPrettyPrinter()
										.writeValueAsString(true)).build();
					} else {
						return Response.status(403)
								.type(MediaType.APPLICATION_JSON)
								.entity("Your permission is: " + decision)
								.build();
					}
				} else {
					return Response.status(403)
							.type(MediaType.APPLICATION_JSON)
							.entity("No User Permission Attributes are send!")
							.build();
				}
			}
		} catch (Exception e) {
			log.error("Could not Check Permission for a Proposal error e=", e);
		}
		return Response
				.status(403)
				.entity("{\"error\": \"No User Permission Attributes are send!\", \"status\": \"FAIL\"}")
				.build();
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
			notificationDAO
					.notifyIRBs(existingProposal, proposalID, signatures);
		}
	}

	public void broadCastNotification(String proposalID, String projectTitle,
			String notificationMessage, String notificationType,
			boolean isCritical, Boolean needPI, Boolean needCoPI,
			Boolean needSenior, Boolean needChair, Boolean needManager,
			Boolean needDean, Boolean needIrb, Boolean needResearchadmin,
			Boolean needDirector) {
		ObjectId id = new ObjectId(proposalID);
		List<SignatureUserInfo> signatures = proposalDAO
				.findUsersExceptInvestigatorForAproposal(id, needPI, needCoPI,
						needSenior, needChair, needManager, needDean, needIrb,
						needResearchadmin, needDirector);
		NotificationLog notification;
		notificationDAO.notifyAdmin(proposalID, projectTitle,
				notificationMessage, notificationType, isCritical);
		for (SignatureUserInfo notifyUsers : signatures) {
			notification = new NotificationLog();
			if (isCritical) {
				notification.setCritical(true);
			}
			notification.setType(notificationType);
			notification.setAction(notificationMessage);
			notification.setProposalId(proposalID);
			notification.setProposalTitle(projectTitle);
			notification.setUserProfileId(notifyUsers.getUserProfileId());
			notification.setUsername(notifyUsers.getUserName());
			notification.setCollege(notifyUsers.getCollege());
			notification.setDepartment(notifyUsers.getDepartment());
			notification.setPositionType(notifyUsers.getPositionType());
			notification.setPositionTitle(notifyUsers.getPositionTitle());
			notificationDAO.save(notification);
		}
		// Broadcasting SSE
		Builder eventBuilder = new Builder();
		OutboundEvent event = eventBuilder.name("notification")
				.mediaType(MediaType.TEXT_PLAIN_TYPE).data(String.class, "1")
				.build();
		NotificationService.BROADCASTER.broadcast(event);
	}

	/***
	 * Sends Notification
	 * 
	 * @param id
	 * @param existingProposal
	 * @param authorUserName
	 */
	public void sendNotification(ObjectId id, Proposal existingProposal,
			String authorUserName) {
		EmailUtil emailUtil = new EmailUtil();
		String emailSubject = new String();
		String emailBody = new String();
		String piEmail = new String();
		List<String> emaillist = new ArrayList<String>();
		emailSubject = "The proposal has been deleted by: " + authorUserName;
		emailBody = "Hello User,<br/><br/>The proposal has been deleted by Admin.<br/><br/>Thank you, <br/> GPMS Team";
		piEmail = existingProposal.getInvestigatorInfo().getPi().getUserRef()
				.getWorkEmails().get(0);
		for (InvestigatorRefAndPosition copis : existingProposal
				.getInvestigatorInfo().getCo_pi()) {
			emaillist.add(copis.getUserRef().getWorkEmails().get(0));
		}
		for (InvestigatorRefAndPosition seniors : existingProposal
				.getInvestigatorInfo().getSeniorPersonnel()) {
			emaillist.add(seniors.getUserRef().getWorkEmails().get(0));
		}
		List<SignatureUserInfo> signatures = proposalDAO
				.findSignaturesExceptInvestigator(id,
						existingProposal.isIrbApprovalRequired());
		for (SignatureUserInfo signatureInfo : signatures) {
			emaillist.add(signatureInfo.getEmail());
		}
		emailUtil.sendMailMultipleUsersWithoutAuth(piEmail, emaillist,
				emailSubject, emailBody);
		String projectTitle = existingProposal.getProjectInfo()
				.getProjectTitle();
		String notificationMessage = "Deleted by " + authorUserName + ".";
		broadCastNotification(existingProposal.getId().toString(),
				projectTitle, notificationMessage, "Proposal", true, true,
				true, true, true, true, true, true, true, true);
	}

	/***
	 * Sends Delete Notification
	 * 
	 * @param proposalRoles
	 * @param proposalUserTitle
	 * @param existingProposal
	 * @param authorUserName
	 * @param emailDetails
	 * @return
	 * @throws JsonProcessingException
	 */
	public void sendDeleteNotification(String proposalRoles,
			String proposalUserTitle, Proposal existingProposal,
			String authorUserName) throws JsonProcessingException {
		String notificationMessage = "Deleted by " + authorUserName + ".";
		if (proposalRoles.equals("PI")
				&& !proposalUserTitle.equals("University Research Director")) {
			broadCastNotification(existingProposal.getId().toString(),
					existingProposal.getProjectInfo().getProjectTitle(),
					notificationMessage, "Proposal", true, true, true, true,
					false, false, false, false, false, false);
		} else if (!proposalRoles.equals("PI")
				&& proposalUserTitle.equals("University Research Director")) {
			broadCastNotification(existingProposal.getId().toString(),
					existingProposal.getProjectInfo().getProjectTitle(),
					notificationMessage, "Proposal", true, true, true, true,
					true, true, true, true, true, true);
		}
	}

	/***
	 * For Admin User only to notify Investigator Users
	 * 
	 * @param existingProposal
	 * @param oldProposal
	 * @param authorProfile
	 * @param proposalID
	 * @param authorUserName
	 * @return
	 */
	public boolean notifyUsersProposalStatusUpdate(Proposal existingProposal,
			Proposal oldProposal, UserProfile authorProfile, String proposalID,
			String authorUserName) {
		String notificationMessage = new String();
		boolean isCritical = false;
		boolean proposalIsChanged = false;
		if (!proposalID.equals("0")) {
			if (!existingProposal.equals(oldProposal)) {
				proposalDAO.updateProposal(existingProposal, authorProfile);
				notificationMessage = "Updated by " + authorUserName + ".";
				proposalIsChanged = true;
			}
		} else {
			proposalDAO.saveProposal(existingProposal, authorProfile);
			notificationMessage = "Saved by " + authorUserName + ".";
			proposalIsChanged = true;
		}

		if (proposalIsChanged) {
			notificationDAO.NotifyAllExistingInvestigators(existingProposal
					.getId().toString(), existingProposal.getProjectInfo()
					.getProjectTitle(), existingProposal, notificationMessage,
					"Proposal", isCritical);
		}
		return proposalIsChanged;
	}

	/**
	 * @param proposalIsChanged
	 * @param existingProposal
	 * @param oldProposal
	 * @param authorProfile
	 * @param proposalID
	 * @param signatures
	 * @param irbApprovalRequired
	 * @param signByAllUsersInfo
	 * @param authorUserName
	 * @param root
	 * @param proposalUserTitle
	 * @return
	 */
	public boolean notifyUsersProposalStatusUpdate(Proposal existingProposal,
			Proposal oldProposal, UserProfile authorProfile, String proposalID,
			List<SignatureUserInfo> signatures, boolean irbApprovalRequired,
			SignatureByAllUsers signByAllUsersInfo, String authorUserName,
			JsonNode root, JsonNode proposalUserTitle) {
		String notificationMessage = new String();
		boolean isCritical = false;
		if (proposalUserTitle != null) {
			// For Proposal Roles : PI, Co-PI, Senior
			JsonNode proposalRoles = root.get("proposalRoles");
			List<String> currentProposalRoles = new ArrayList<String>();
			if (!proposalRoles.asText().equals("")) {
				currentProposalRoles = Arrays.asList(proposalRoles.textValue()
						.split(", "));
			}
			JsonNode buttonType = root.get("buttonType");
			// For Proposal Status
			if (buttonType != null) {
				NotificationLog notification = new NotificationLog();
				switch (buttonType.textValue()) {
				case "Save":
					notificationMessage = proposalDAO.updateForProposalSave(
							existingProposal, proposalID, signByAllUsersInfo,
							authorUserName, notificationMessage,
							currentProposalRoles);
					break;
				case "Submit":
					notificationMessage = updateForProposalSubmit(
							existingProposal, proposalID, signatures,
							signByAllUsersInfo, authorUserName,
							proposalUserTitle, currentProposalRoles);
					break;
				case "Approve":
					notificationMessage = updateForProposalApprove(
							existingProposal, proposalID, signatures,
							irbApprovalRequired, signByAllUsersInfo,
							authorUserName, proposalUserTitle,
							notificationMessage, currentProposalRoles);
					break;
				case "Disapprove":
					if (!proposalID.equals("0") && currentProposalRoles != null) {
						notificationMessage = "Disapproved" + " by "
								+ authorUserName + ".";
						isCritical = true;
						int coPICount = existingProposal.getInvestigatorInfo()
								.getCo_pi().size();
						if (existingProposal.getChairApproval() == ApprovalType.READYFORAPPROVAL
								&& (proposalUserTitle.textValue().equals(
										"Department Chair") || proposalUserTitle
										.textValue().equals("Associate Chair"))) {
							// Returned by Chair
							existingProposal
									.setChairApproval(ApprovalType.DISAPPROVED);
							existingProposal
									.setSubmittedByPI(SubmitType.NOTSUBMITTED);
							if (coPICount > 0) {
								existingProposal
										.setReadyForSubmissionByPI(false);
							}
							existingProposal.getSignatureInfo().clear();
							// Proposal Status
							existingProposal.getProposalStatus().clear();
							existingProposal.getProposalStatus().add(
									Status.RETURNEDBYCHAIR);
							for (SignatureUserInfo userToNotify : signatures) {
								notification = new NotificationLog();
								if (userToNotify.getPositionTitle().equals(
										"Department Chair")) {
									notification.setCritical(true);
									notification.setType("Proposal");
									notification.setAction(notificationMessage);
									notification.setProposalId(proposalID);
									notification
											.setProposalTitle(existingProposal
													.getProjectInfo()
													.getProjectTitle());
									notification.setUserProfileId(userToNotify
											.getUserProfileId());
									notification.setUsername(userToNotify
											.getUserName());
									notification.setCollege(userToNotify
											.getCollege());
									notification.setDepartment(userToNotify
											.getDepartment());
									notification.setPositionType(userToNotify
											.getPositionType());
									notification.setPositionTitle(userToNotify
											.getPositionTitle());
								}
							}
						} else if (existingProposal
								.getBusinessManagerApproval() == ApprovalType.READYFORAPPROVAL
								&& (proposalUserTitle.textValue().equals(
										"Business Manager") || proposalUserTitle
										.textValue()
										.equals("Department Administrative Assistant"))) {
							// Disapproved by Business Manager
							existingProposal
									.setBusinessManagerApproval(ApprovalType.DISAPPROVED);
							existingProposal
									.setSubmittedByPI(SubmitType.NOTSUBMITTED);
							existingProposal
									.setIrbApproval(ApprovalType.NOTREADYFORAPPROVAL);
							if (coPICount > 0) {
								existingProposal
										.setReadyForSubmissionByPI(false);
							}
							existingProposal.getSignatureInfo().clear();
							// Proposal Status
							existingProposal.getProposalStatus().clear();
							existingProposal.getProposalStatus().add(
									Status.DISAPPROVEDBYBUSINESSMANAGER);
							for (SignatureUserInfo userToNotify : signatures) {
								notification = new NotificationLog();
								if (userToNotify.getPositionTitle().equals(
										"Business Manager")) {
									notification.setCritical(true);
									notification.setType("Proposal");
									notification.setAction(notificationMessage);
									notification.setProposalId(proposalID);
									notification
											.setProposalTitle(existingProposal
													.getProjectInfo()
													.getProjectTitle());
									notification.setUserProfileId(userToNotify
											.getUserProfileId());
									notification.setUsername(userToNotify
											.getUserName());
									notification.setCollege(userToNotify
											.getCollege());
									notification.setDepartment(userToNotify
											.getDepartment());
									notification.setPositionType(userToNotify
											.getPositionType());
									notification.setPositionTitle(userToNotify
											.getPositionTitle());
								} else if (userToNotify.getPositionTitle()
										.equals("Department Chair")) {
									notification.setCritical(true);
									notification.setType("Proposal");
									notification.setAction(notificationMessage);
									notification.setProposalId(proposalID);
									notification
											.setProposalTitle(existingProposal
													.getProjectInfo()
													.getProjectTitle());
									notification.setUserProfileId(userToNotify
											.getUserProfileId());
									notification.setUsername(userToNotify
											.getUserName());
									notification.setCollege(userToNotify
											.getCollege());
									notification.setDepartment(userToNotify
											.getDepartment());
									notification.setPositionType(userToNotify
											.getPositionType());
									notification.setPositionTitle(userToNotify
											.getPositionTitle());
								} else if (userToNotify.getPositionTitle()
										.equals("IRB")) {
									if (existingProposal.getIrbApproval() == ApprovalType.APPROVED) {
										notification.setCritical(true);
										notification.setType("Proposal");
										notification
												.setAction(notificationMessage);
										notification.setProposalId(proposalID);
										notification
												.setProposalTitle(existingProposal
														.getProjectInfo()
														.getProjectTitle());
										notification
												.setUserProfileId(userToNotify
														.getUserProfileId());
										notification.setUsername(userToNotify
												.getUserName());
										notification.setCollege(userToNotify
												.getCollege());
										notification.setDepartment(userToNotify
												.getDepartment());
										notification
												.setPositionType(userToNotify
														.getPositionType());
										notification
												.setPositionTitle(userToNotify
														.getPositionTitle());
									}
								}
								notificationDAO.save(notification);
							}
						} else if (existingProposal.getDeanApproval() == ApprovalType.READYFORAPPROVAL
								&& (proposalUserTitle.textValue()
										.equals("Dean") || proposalUserTitle
										.textValue().equals("Associate Dean"))) {
							// Returned by Dean
							existingProposal
									.setDeanApproval(ApprovalType.DISAPPROVED);
							existingProposal
									.setSubmittedByPI(SubmitType.NOTSUBMITTED);
							if (coPICount > 0) {
								existingProposal
										.setReadyForSubmissionByPI(false);
							}
							existingProposal.getSignatureInfo().clear();
							existingProposal.getProposalStatus().clear();
							existingProposal.getProposalStatus().add(
									Status.RETURNEDBYDEAN);
							for (SignatureUserInfo userToNotify : signatures) {
								notification = new NotificationLog();
								if (userToNotify.getPositionTitle().equals(
										"Dean")) {
									notification.setCritical(true);
									notification.setType("Proposal");
									notification.setAction(notificationMessage);
									notification.setProposalId(proposalID);
									notification
											.setProposalTitle(existingProposal
													.getProjectInfo()
													.getProjectTitle());
									notification.setUserProfileId(userToNotify
											.getUserProfileId());
									notification.setUsername(userToNotify
											.getUserName());
									notification.setCollege(userToNotify
											.getCollege());
									notification.setDepartment(userToNotify
											.getDepartment());
									notification.setPositionType(userToNotify
											.getPositionType());
									notification.setPositionTitle(userToNotify
											.getPositionTitle());
								} else if (userToNotify.getPositionTitle()
										.equals("Business Manager")) {
									notification.setCritical(true);
									notification.setType("Proposal");
									notification.setAction(notificationMessage);
									notification.setProposalId(proposalID);
									notification
											.setProposalTitle(existingProposal
													.getProjectInfo()
													.getProjectTitle());
									notification.setUserProfileId(userToNotify
											.getUserProfileId());
									notification.setUsername(userToNotify
											.getUserName());
									notification.setCollege(userToNotify
											.getCollege());
									notification.setDepartment(userToNotify
											.getDepartment());
									notification.setPositionType(userToNotify
											.getPositionType());
									notification.setPositionTitle(userToNotify
											.getPositionTitle());
								} else if (userToNotify.getPositionTitle()
										.equals("Department Chair")) {
									notification.setCritical(true);
									notification.setType("Proposal");
									notification.setAction(notificationMessage);
									notification.setProposalId(proposalID);
									notification
											.setProposalTitle(existingProposal
													.getProjectInfo()
													.getProjectTitle());
									notification.setUserProfileId(userToNotify
											.getUserProfileId());
									notification.setUsername(userToNotify
											.getUserName());
									notification.setCollege(userToNotify
											.getCollege());
									notification.setDepartment(userToNotify
											.getDepartment());
									notification.setPositionType(userToNotify
											.getPositionType());
									notification.setPositionTitle(userToNotify
											.getPositionTitle());
								} else if (userToNotify.getPositionTitle()
										.equals("IRB")) {
									if (existingProposal.getIrbApproval() == ApprovalType.APPROVED) {
										notification.setCritical(true);
										notification.setType("Proposal");
										notification
												.setAction(notificationMessage);
										notification.setProposalId(proposalID);
										notification
												.setProposalTitle(existingProposal
														.getProjectInfo()
														.getProjectTitle());
										notification
												.setUserProfileId(userToNotify
														.getUserProfileId());
										notification.setUsername(userToNotify
												.getUserName());
										notification.setCollege(userToNotify
												.getCollege());
										notification.setDepartment(userToNotify
												.getDepartment());
										notification
												.setPositionType(userToNotify
														.getPositionType());
										notification
												.setPositionTitle(userToNotify
														.getPositionTitle());
									}
								}
								notificationDAO.save(notification);
							}
						} else if (existingProposal.getIrbApproval() == ApprovalType.READYFORAPPROVAL
								&& proposalUserTitle.textValue().equals("IRB")) {
							// Disapproved by IRB
							existingProposal
									.setIrbApproval(ApprovalType.DISAPPROVED);
							existingProposal
									.setSubmittedByPI(SubmitType.NOTSUBMITTED);
							if (coPICount > 0) {
								existingProposal
										.setReadyForSubmissionByPI(false);
							}
							existingProposal.getSignatureInfo().clear();
							// Proposal Status
							existingProposal.getProposalStatus().clear();
							existingProposal.getProposalStatus().add(
									Status.DISAPPROVEDBYIRB);

							for (SignatureUserInfo userToNotify : signatures) {
								notification = new NotificationLog();
								if (userToNotify.getPositionTitle().equals(
										"IRB")) {
									notification.setCritical(true);
									notification.setType("Proposal");
									notification.setAction(notificationMessage);
									notification.setProposalId(proposalID);
									notification
											.setProposalTitle(existingProposal
													.getProjectInfo()
													.getProjectTitle());
									notification.setUserProfileId(userToNotify
											.getUserProfileId());
									notification.setUsername(userToNotify
											.getUserName());
									notification.setCollege(userToNotify
											.getCollege());
									notification.setDepartment(userToNotify
											.getDepartment());
									notification.setPositionType(userToNotify
											.getPositionType());
									notification.setPositionTitle(userToNotify
											.getPositionTitle());
								} else if (userToNotify.getPositionTitle()
										.equals("Department Chair")) {
									notification.setCritical(true);
									notification.setType("Proposal");
									notification.setAction(notificationMessage);
									notification.setProposalId(proposalID);
									notification
											.setProposalTitle(existingProposal
													.getProjectInfo()
													.getProjectTitle());
									notification.setUserProfileId(userToNotify
											.getUserProfileId());
									notification.setUsername(userToNotify
											.getUserName());
									notification.setCollege(userToNotify
											.getCollege());
									notification.setDepartment(userToNotify
											.getDepartment());
									notification.setPositionType(userToNotify
											.getPositionType());
									notification.setPositionTitle(userToNotify
											.getPositionTitle());
								} else if (userToNotify.getPositionTitle()
										.equals("Business Manager")) {
									if (existingProposal
											.getBusinessManagerApproval() == ApprovalType.APPROVED) {
										notification.setCritical(true);
										notification.setType("Proposal");
										notification
												.setAction(notificationMessage);
										notification.setProposalId(proposalID);
										notification
												.setProposalTitle(existingProposal
														.getProjectInfo()
														.getProjectTitle());
										notification
												.setUserProfileId(userToNotify
														.getUserProfileId());
										notification.setUsername(userToNotify
												.getUserName());
										notification.setCollege(userToNotify
												.getCollege());
										notification.setDepartment(userToNotify
												.getDepartment());
										notification
												.setPositionType(userToNotify
														.getPositionType());
										notification
												.setPositionTitle(userToNotify
														.getPositionTitle());
									}
								} else if (userToNotify.getPositionTitle()
										.equals("Dean")) {
									if (existingProposal.getDeanApproval() == ApprovalType.APPROVED) {
										notification.setCritical(true);
										notification.setType("Proposal");
										notification
												.setAction(notificationMessage);
										notification.setProposalId(proposalID);
										notification
												.setProposalTitle(existingProposal
														.getProjectInfo()
														.getProjectTitle());
										notification
												.setUserProfileId(userToNotify
														.getUserProfileId());
										notification.setUsername(userToNotify
												.getUserName());
										notification.setCollege(userToNotify
												.getCollege());
										notification.setDepartment(userToNotify
												.getDepartment());
										notification
												.setPositionType(userToNotify
														.getPositionType());
										notification
												.setPositionTitle(userToNotify
														.getPositionTitle());
									}
								}
								notificationDAO.save(notification);
							}
						} else if (existingProposal
								.getResearchAdministratorApproval() == ApprovalType.READYFORAPPROVAL
								&& proposalUserTitle.textValue().equals(
										"University Research Administrator")) {
							// Disapproved by Research
							// Administrator
							existingProposal
									.setResearchAdministratorApproval(ApprovalType.DISAPPROVED);
							existingProposal
									.setSubmittedByPI(SubmitType.NOTSUBMITTED);
							if (coPICount > 0) {
								existingProposal
										.setReadyForSubmissionByPI(false);
							}
							existingProposal.getSignatureInfo().clear();
							// Proposal Status
							existingProposal.getProposalStatus().clear();
							existingProposal.getProposalStatus().add(
									Status.DISAPPROVEDBYRESEARCHADMIN);
							for (SignatureUserInfo userToNotify : signatures) {
								notification = new NotificationLog();
								if (userToNotify.getPositionTitle().equals(
										"University Research Administrator")) {
									notification.setCritical(true);
									notification.setType("Proposal");
									notification.setAction(notificationMessage);
									notification.setProposalId(proposalID);
									notification
											.setProposalTitle(existingProposal
													.getProjectInfo()
													.getProjectTitle());
									notification.setUserProfileId(userToNotify
											.getUserProfileId());
									notification.setUsername(userToNotify
											.getUserName());
									notification.setCollege(userToNotify
											.getCollege());
									notification.setDepartment(userToNotify
											.getDepartment());
									notification.setPositionType(userToNotify
											.getPositionType());
									notification.setPositionTitle(userToNotify
											.getPositionTitle());
								} else if (userToNotify.getPositionTitle()
										.equals("Dean")) {
									notification.setCritical(true);
									notification.setType("Proposal");
									notification.setAction(notificationMessage);
									notification.setProposalId(proposalID);
									notification
											.setProposalTitle(existingProposal
													.getProjectInfo()
													.getProjectTitle());
									notification.setUserProfileId(userToNotify
											.getUserProfileId());
									notification.setUsername(userToNotify
											.getUserName());
									notification.setCollege(userToNotify
											.getCollege());
									notification.setDepartment(userToNotify
											.getDepartment());
									notification.setPositionType(userToNotify
											.getPositionType());
									notification.setPositionTitle(userToNotify
											.getPositionTitle());
								} else if (userToNotify.getPositionTitle()
										.equals("Business Manager")) {
									notification.setCritical(true);
									notification.setType("Proposal");
									notification.setAction(notificationMessage);
									notification.setProposalId(proposalID);
									notification
											.setProposalTitle(existingProposal
													.getProjectInfo()
													.getProjectTitle());
									notification.setUserProfileId(userToNotify
											.getUserProfileId());
									notification.setUsername(userToNotify
											.getUserName());
									notification.setCollege(userToNotify
											.getCollege());
									notification.setDepartment(userToNotify
											.getDepartment());
									notification.setPositionType(userToNotify
											.getPositionType());
									notification.setPositionTitle(userToNotify
											.getPositionTitle());
								} else if (userToNotify.getPositionTitle()
										.equals("Department Chair")) {
									notification.setCritical(true);
									notification.setType("Proposal");
									notification.setAction(notificationMessage);
									notification.setProposalId(proposalID);
									notification
											.setProposalTitle(existingProposal
													.getProjectInfo()
													.getProjectTitle());
									notification.setUserProfileId(userToNotify
											.getUserProfileId());
									notification.setUsername(userToNotify
											.getUserName());
									notification.setCollege(userToNotify
											.getCollege());
									notification.setDepartment(userToNotify
											.getDepartment());
									notification.setPositionType(userToNotify
											.getPositionType());
									notification.setPositionTitle(userToNotify
											.getPositionTitle());
								} else if (userToNotify.getPositionTitle()
										.equals("IRB")) {
									if (irbApprovalRequired) {
										notification.setCritical(true);
										notification.setType("Proposal");
										notification
												.setAction(notificationMessage);
										notification.setProposalId(proposalID);
										notification
												.setProposalTitle(existingProposal
														.getProjectInfo()
														.getProjectTitle());
										notification
												.setUserProfileId(userToNotify
														.getUserProfileId());
										notification.setUsername(userToNotify
												.getUserName());
										notification.setCollege(userToNotify
												.getCollege());
										notification.setDepartment(userToNotify
												.getDepartment());
										notification
												.setPositionType(userToNotify
														.getPositionType());
										notification
												.setPositionTitle(userToNotify
														.getPositionTitle());
									}
								}
								notificationDAO.save(notification);
							}
						} else if (existingProposal
								.getResearchDirectorApproval() == ApprovalType.READYFORAPPROVAL
								&& proposalUserTitle.textValue().equals(
										"University Research Director")) {
							// Disapproved by University
							// Research
							// Director
							existingProposal
									.setResearchDirectorApproval(ApprovalType.DISAPPROVED);
							existingProposal
									.setSubmittedByPI(SubmitType.NOTSUBMITTED);
							if (coPICount > 0) {
								existingProposal
										.setReadyForSubmissionByPI(false);
							}
							existingProposal.getSignatureInfo().clear();
							// Proposal Status
							existingProposal.getProposalStatus().clear();
							existingProposal.getProposalStatus().add(
									Status.DISAPPROVEDBYRESEARCHDIRECTOR);
							for (SignatureUserInfo userToNotify : signatures) {
								notification = new NotificationLog();
								if (userToNotify.getPositionTitle().equals(
										"University Research Director")) {
									notification.setCritical(true);
									notification.setType("Proposal");
									notification.setAction(notificationMessage);
									notification.setProposalId(proposalID);
									notification
											.setProposalTitle(existingProposal
													.getProjectInfo()
													.getProjectTitle());
									notification.setUserProfileId(userToNotify
											.getUserProfileId());
									notification.setUsername(userToNotify
											.getUserName());
									notification.setCollege(userToNotify
											.getCollege());
									notification.setDepartment(userToNotify
											.getDepartment());
									notification.setPositionType(userToNotify
											.getPositionType());
									notification.setPositionTitle(userToNotify
											.getPositionTitle());
								} else if (userToNotify
										.getPositionTitle()
										.equals("University Research Administrator")) {
									notification.setCritical(true);
									notification.setType("Proposal");
									notification.setAction(notificationMessage);
									notification.setProposalId(proposalID);
									notification
											.setProposalTitle(existingProposal
													.getProjectInfo()
													.getProjectTitle());
									notification.setUserProfileId(userToNotify
											.getUserProfileId());
									notification.setUsername(userToNotify
											.getUserName());
									notification.setCollege(userToNotify
											.getCollege());
									notification.setDepartment(userToNotify
											.getDepartment());
									notification.setPositionType(userToNotify
											.getPositionType());
									notification.setPositionTitle(userToNotify
											.getPositionTitle());
								} else if (userToNotify.getPositionTitle()
										.equals("Dean")) {
									notification.setCritical(true);
									notification.setType("Proposal");
									notification.setAction("Disapproved by "
											+ authorUserName);
									notification.setProposalId(proposalID);
									notification
											.setProposalTitle(existingProposal
													.getProjectInfo()
													.getProjectTitle());
									notification.setUserProfileId(userToNotify
											.getUserProfileId());
									notification.setUsername(userToNotify
											.getUserName());
									notification.setCollege(userToNotify
											.getCollege());
									notification.setDepartment(userToNotify
											.getDepartment());
									notification.setPositionType(userToNotify
											.getPositionType());
									notification.setPositionTitle(userToNotify
											.getPositionTitle());
								} else if (userToNotify.getPositionTitle()
										.equals("Business Manager")) {
									notification.setCritical(true);
									notification.setType("Proposal");
									notification.setAction("Disapproved by "
											+ authorUserName);
									notification.setProposalId(proposalID);
									notification
											.setProposalTitle(existingProposal
													.getProjectInfo()
													.getProjectTitle());
									notification.setUserProfileId(userToNotify
											.getUserProfileId());
									notification.setUsername(userToNotify
											.getUserName());
									notification.setCollege(userToNotify
											.getCollege());
									notification.setDepartment(userToNotify
											.getDepartment());
									notification.setPositionType(userToNotify
											.getPositionType());
									notification.setPositionTitle(userToNotify
											.getPositionTitle());
								} else if (userToNotify.getPositionTitle()
										.equals("Department Chair")) {
									notification.setCritical(true);
									notification.setType("Proposal");
									notification.setAction("Disapproved by "
											+ authorUserName);
									notification.setProposalId(proposalID);
									notification
											.setProposalTitle(existingProposal
													.getProjectInfo()
													.getProjectTitle());
									notification.setUserProfileId(userToNotify
											.getUserProfileId());
									notification.setUsername(userToNotify
											.getUserName());
									notification.setCollege(userToNotify
											.getCollege());
									notification.setDepartment(userToNotify
											.getDepartment());
									notification.setPositionType(userToNotify
											.getPositionType());
									notification.setPositionTitle(userToNotify
											.getPositionTitle());
								} else if (userToNotify.getPositionTitle()
										.equals("IRB")) {
									if (irbApprovalRequired) {
										notification.setCritical(true);
										notification.setType("Proposal");
										notification
												.setAction("Disapproved by "
														+ authorUserName);
										notification.setProposalId(proposalID);
										notification
												.setProposalTitle(existingProposal
														.getProjectInfo()
														.getProjectTitle());
										notification
												.setUserProfileId(userToNotify
														.getUserProfileId());
										notification.setUsername(userToNotify
												.getUserName());
										notification.setCollege(userToNotify
												.getCollege());
										notification.setDepartment(userToNotify
												.getDepartment());
										notification
												.setPositionType(userToNotify
														.getPositionType());
										notification
												.setPositionTitle(userToNotify
														.getPositionTitle());
									}
								}
								notificationDAO.save(notification);
							}
						}
					}
					break;
				default:
					break;
				}
			}
		}
		boolean proposalIsChanged = false;
		if (!proposalID.equals("0")) {
			if (!existingProposal.equals(oldProposal)) {
				proposalDAO.updateProposal(existingProposal, authorProfile);
				proposalIsChanged = true;
			}
		} else {
			proposalDAO.saveProposal(existingProposal, authorProfile);
			proposalIsChanged = true;
		}
		if (proposalIsChanged) {
			notificationDAO.NotifyAllExistingInvestigators(existingProposal
					.getId().toString(), existingProposal.getProjectInfo()
					.getProjectTitle(), existingProposal, notificationMessage,
					"Proposal", isCritical);
		}
		return proposalIsChanged;
	}

	/**
	 * @param existingProposal
	 * @param proposalID
	 * @param signatures
	 */
	public void notifyProposalSubmittedByAdmin(Proposal existingProposal,
			String proposalID, List<SignatureUserInfo> signatures) {
		NotificationLog notification;
		existingProposal
				.setResearchAdministratorSubmission(SubmitType.SUBMITTED);
		existingProposal.getProposalStatus().clear();
		existingProposal.getProposalStatus().add(
				Status.SUBMITTEDBYRESEARCHADMIN);
		for (SignatureUserInfo allUsers : signatures) {
			notification = new NotificationLog();
			notification.setCritical(true);
			notification.setType("Proposal");
			notification.setAction("The Proposal is Submitted");
			notification.setProposalId(proposalID);
			notification.setProposalTitle(existingProposal.getProjectInfo()
					.getProjectTitle());
			notification.setUserProfileId(allUsers.getUserProfileId());
			notification.setUsername(allUsers.getUserName());
			notification.setCollege(allUsers.getCollege());
			notification.setDepartment(allUsers.getDepartment());
			notification.setPositionType(allUsers.getPositionType());
			notification.setPositionTitle(allUsers.getPositionTitle());
			notificationDAO.save(notification);
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
			SignatureByAllUsers signByAllUsersInfo, String authorUserName,
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
				notificationDAO.notifyAllChairs(existingProposal, proposalID,
						signatures, notificationMessage);
			} else if (existingProposal.getBusinessManagerApproval() == ApprovalType.READYFORAPPROVAL
					&& (proposalUserTitle.textValue()
							.equals("Business Manager") || proposalUserTitle
							.textValue().equals(
									"Department Administrative Assistant"))) {
				if (signByAllUsersInfo.isSignedByAllBusinessManagers()) {
					notificationDAO.notifyProposalApprovedByBusinessManager(
							existingProposal, proposalID, signatures);
				}
				notificationDAO.notifyAllBusinessManagers(existingProposal,
						proposalID, signatures, notificationMessage);
			} else if (existingProposal.getDeanApproval() == ApprovalType.READYFORAPPROVAL
					&& (proposalUserTitle.textValue().equals("Dean") || proposalUserTitle
							.textValue().equals("Associate Dean"))) {
				if (signByAllUsersInfo.isSignedByAllDeans()) {
					notificationDAO.notifyProposalApprovedByDean(
							existingProposal, proposalID, signatures,
							irbApprovalRequired);
				}
				notificationDAO.notifyAllDeans(existingProposal, proposalID,
						signatures, notificationMessage);
			} else if (existingProposal.getIrbApproval() == ApprovalType.READYFORAPPROVAL
					&& proposalUserTitle.textValue().equals("IRB")
					&& irbApprovalRequired) {
				if (signByAllUsersInfo.isSignedByAllIRBs()) {
					notificationDAO.notifyProposalApprovedByIRB(
							existingProposal, proposalID, signatures);
				}
				notificationDAO.notifyAllIRBs(existingProposal, proposalID,
						signatures, notificationMessage);
			} else if (existingProposal.getResearchAdministratorApproval() == ApprovalType.READYFORAPPROVAL
					&& proposalUserTitle.textValue().equals(
							"University Research Administrator")) {
				if (signByAllUsersInfo.isSignedByAllResearchAdmins()) {
					notificationDAO.notifyProposalApprovedByResearchAdmin(
							existingProposal, proposalID, signatures);
				}
				notificationDAO.notifyAllResearchAdmins(existingProposal,
						proposalID, signatures, notificationMessage);
			} else if (existingProposal.getResearchDirectorApproval() == ApprovalType.READYFORAPPROVAL
					&& proposalUserTitle.textValue().equals(
							"University Research Director")) {
				if (signByAllUsersInfo.isSignedByAllResearchDirectors()) {
					notificationDAO.notifyProposalApprovedByResearchDirector(
							existingProposal, proposalID, signatures);
				}
				notificationDAO.notifyAllResearchDirectors(existingProposal,
						proposalID, signatures, notificationMessage);
			}
		}
		return notificationMessage;
	}

	/***
	 * Sends Save/ Update Proposal Notification
	 * 
	 * @param message
	 * @param proposalId
	 * @param existingProposal
	 * @param oldProposal
	 * @param authorProfile
	 * @param isAdminUser
	 * @param signatures
	 * @param irbApprovalRequired
	 * @param signByAllUsersInfo
	 * @param emailDetails
	 * @return
	 * @throws JsonProcessingException
	 */
	public Response sendSaveUpdateNotification(String message,
			String proposalId, Proposal existingProposal, Proposal oldProposal,
			UserProfile authorProfile, boolean isAdminUser,
			List<SignatureUserInfo> signatures, Boolean irbApprovalRequired,
			SignatureByAllUsers signByAllUsersInfo, EmailCommonInfo emailDetails)
			throws JsonProcessingException {
		ObjectMapper mapper = new ObjectMapper();
		String emailSubject = emailDetails.getEmailSubject();
		String emailBody = emailDetails.getEmailBody();
		String authorName = emailDetails.getAuthorName();
		String piEmail = emailDetails.getPiEmail();
		List<String> emaillist = emailDetails.getEmaillist();
		boolean proposalIsChanged = false;
		try {
			if (proposalId.equals("0")) {
				proposalIsChanged = saveProposal(message, existingProposal,
						null, authorProfile, isAdminUser, proposalId, null,
						irbApprovalRequired, null);
			} else {
				proposalIsChanged = saveProposal(message, existingProposal,
						oldProposal, authorProfile, isAdminUser, proposalId,
						signatures, irbApprovalRequired, signByAllUsersInfo);
			}
		} catch (Exception e) {
			return Response.status(403).type(MediaType.APPLICATION_JSON)
					.entity(e.getMessage()).build();
		}
		if (proposalIsChanged) {
			if (!emailSubject.equals("")) {
				EmailUtil emailUtil = new EmailUtil();
				emailUtil.sendMailMultipleUsersWithoutAuth(piEmail, emaillist,
						emailSubject + authorName, emailBody);
			}
			return Response
					.status(200)
					.type(MediaType.APPLICATION_JSON)
					.entity(mapper.writerWithDefaultPrettyPrinter()
							.writeValueAsString(true)).build();
		} else {
			return Response
					.status(200)
					.type(MediaType.APPLICATION_JSON)
					.entity(mapper.writerWithDefaultPrettyPrinter()
							.writeValueAsString(true)).build();
		}
	}

	public boolean saveProposal(String message, Proposal existingProposal,
			Proposal oldProposal, UserProfile authorProfile,
			boolean isAdminUser, String proposalID,
			List<SignatureUserInfo> signatures, boolean irbApprovalRequired,
			SignatureByAllUsers signByAllUsersInfo)
			throws UnknownHostException, Exception, ParseException,
			IOException, JsonParseException, JsonMappingException {
		String authorUserName = authorProfile.getFullName();
		ObjectMapper mapper = new ObjectMapper();
		JsonNode root = mapper.readTree(message);
		JsonNode proposalInfo = null;
		boolean proposalIsChanged = false;
		if (root != null && root.has("proposalInfo")) {
			proposalInfo = root.get("proposalInfo");
			proposalDAO.getProjectInfo(existingProposal, proposalID,
					proposalInfo);
			proposalDAO.getSponsorAndBudgetInfo(existingProposal, proposalID,
					proposalInfo);
			proposalDAO.getCostShareInfo(existingProposal, proposalID,
					proposalInfo);
			proposalDAO.getUniversityCommitments(existingProposal, proposalID,
					proposalInfo);
			proposalDAO.getConflictOfInterest(existingProposal, proposalID,
					proposalInfo);
			proposalDAO.getAdditionalInfo(existingProposal, proposalID,
					proposalInfo);
			proposalDAO.getCollaborationInfo(existingProposal, proposalID,
					proposalInfo);
			proposalDAO.getConfidentialInfo(existingProposal, proposalID,
					proposalInfo);
			if (!isAdminUser) {
				// OSP Section
				JsonNode proposalUserTitle = root.get("proposalUserTitle");
				if ((proposalUserTitle.textValue().equals(
						"University Research Administrator") || proposalUserTitle
						.textValue().equals("University Research Director"))
						&& !proposalID.equals("0")) {
					proposalDAO.getOSPSectionInfo(existingProposal,
							proposalInfo);
				} else {
					if (!proposalID.equals("0")) {
						existingProposal.setOspSectionInfo(oldProposal
								.getOspSectionInfo());
					}
				}
				proposalIsChanged = notifyUsersProposalStatusUpdate(
						existingProposal, oldProposal, authorProfile,
						proposalID, signatures, irbApprovalRequired,
						signByAllUsersInfo, authorUserName, root,
						proposalUserTitle);
			} else {
				proposalIsChanged = notifyUsersProposalStatusUpdate(
						existingProposal, oldProposal, authorProfile,
						proposalID, authorUserName);
			}
		}
		return proposalIsChanged;
	}

	/**
	 * @param existingProposal
	 * @param proposalID
	 * @param signatures
	 */
	public void notifyChairProposalSubmittedByPI(Proposal existingProposal,
			String proposalID, List<SignatureUserInfo> signatures) {
		NotificationLog notification;
		existingProposal.setDateSubmitted(new Date());
		existingProposal.setSubmittedByPI(SubmitType.SUBMITTED);
		existingProposal.setChairApproval(ApprovalType.READYFORAPPROVAL);
		existingProposal.getProposalStatus().clear();
		existingProposal.getProposalStatus()
				.add(Status.WAITINGFORCHAIRAPPROVAL);
		for (SignatureUserInfo chair : signatures) {
			if (chair.getPositionTitle().equals("Department Chair")) {
				notification = new NotificationLog();
				notification.setCritical(true);
				notification.setType("Proposal");
				notification.setAction("Ready for Approval");
				notification.setProposalId(proposalID);
				notification.setProposalTitle(existingProposal.getProjectInfo()
						.getProjectTitle());
				notification.setUserProfileId(chair.getUserProfileId());
				notification.setUsername(chair.getUserName());
				notification.setCollege(chair.getCollege());
				notification.setDepartment(chair.getDepartment());
				notification.setPositionType(chair.getPositionType());
				notification.setPositionTitle(chair.getPositionTitle());
				notificationDAO.save(notification);
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
		NotificationLog notification;
		if (businessManager.getPositionTitle().equals("Business Manager")) {
			notification = new NotificationLog();
			notification.setCritical(true);
			notification.setType("Proposal");
			notification.setAction("Ready for Reviewal");
			notification.setProposalId(proposalID);
			notification.setProposalTitle(existingProposal.getProjectInfo()
					.getProjectTitle());
			notification.setUserProfileId(businessManager.getUserProfileId());
			notification.setUsername(businessManager.getUserName());
			notification.setCollege(businessManager.getCollege());
			notification.setDepartment(businessManager.getDepartment());
			notification.setPositionType(businessManager.getPositionType());
			notification.setPositionTitle(businessManager.getPositionTitle());
			notificationDAO.save(notification);
		}
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
			SignatureByAllUsers signByAllUsersInfo, String authorUserName,
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

	/**
	 * @param proposalId
	 * @param existingProposal
	 * @param oldProposal
	 * @param proposalInfo
	 */
	public void getInvestigatorInfoDetails(String proposalId,
			Proposal existingProposal, Proposal oldProposal,
			JsonNode proposalInfo) {
		InvestigatorInfo addedInvestigators = new InvestigatorInfo();
		InvestigatorInfo existingInvestigators = new InvestigatorInfo();
		InvestigatorInfo deletedInvestigators = new InvestigatorInfo();
		if (proposalInfo != null && proposalInfo.has("InvestigatorInfo")) {
			if (!proposalId.equals("0")) {
				existingProposal.getInvestigatorInfo().getCo_pi().clear();
				existingProposal.getInvestigatorInfo().getSeniorPersonnel()
						.clear();
			}
			String[] rows = proposalInfo.get("InvestigatorInfo").textValue()
					.split("#!#");
			InvestigatorInfo newInvestigatorInfo = new InvestigatorInfo();
			for (String col : rows) {
				String[] cols = col.split("!#!");
				InvestigatorRefAndPosition investigatorRefAndPosition = new InvestigatorRefAndPosition();
				ObjectId id = new ObjectId(cols[1]);
				UserProfile userRef = userProfileDAO
						.findUserDetailsByProfileID(id);
				investigatorRefAndPosition.setUserRef(userRef);
				investigatorRefAndPosition.setUserProfileId(cols[1]);
				investigatorRefAndPosition.setCollege(cols[2]);
				investigatorRefAndPosition.setDepartment(cols[3]);
				investigatorRefAndPosition.setPositionType(cols[4]);
				investigatorRefAndPosition.setPositionTitle(cols[5]);
				switch (cols[0]) {
				case "0":
					if (proposalId.equals("0")) {
						newInvestigatorInfo.setPi(investigatorRefAndPosition);
					}
					break;
				case "1":
					if (!proposalId.equals("0")) {
						if (!existingProposal.getInvestigatorInfo().getCo_pi()
								.contains(investigatorRefAndPosition)) {
							existingProposal.getInvestigatorInfo().getCo_pi()
									.add(investigatorRefAndPosition);

							if (!addedInvestigators.getCo_pi().contains(
									investigatorRefAndPosition)) {
								addedInvestigators.getCo_pi().add(
										investigatorRefAndPosition);
							}
						}
					} else {
						newInvestigatorInfo.getCo_pi().add(
								investigatorRefAndPosition);
					}
					break;
				case "2":
					if (!proposalId.equals("0")) {
						if (!existingProposal.getInvestigatorInfo()
								.getSeniorPersonnel()
								.contains(investigatorRefAndPosition)) {
							existingProposal.getInvestigatorInfo()
									.getSeniorPersonnel()
									.add(investigatorRefAndPosition);
							if (!addedInvestigators.getSeniorPersonnel()
									.contains(investigatorRefAndPosition)) {
								addedInvestigators.getSeniorPersonnel().add(
										investigatorRefAndPosition);
							}
						}
					} else {
						newInvestigatorInfo.getSeniorPersonnel().add(
								investigatorRefAndPosition);
					}
					break;
				default:
					break;
				}
			}
			if (proposalId.equals("0")) {
				existingProposal.setInvestigatorInfo(newInvestigatorInfo);
				addedInvestigators = newInvestigatorInfo;
			} else {
				existingInvestigators = oldProposal.getInvestigatorInfo();
				for (InvestigatorRefAndPosition coPI : existingInvestigators
						.getCo_pi()) {
					if (!existingProposal.getInvestigatorInfo().getCo_pi()
							.contains(coPI)) {
						if (!deletedInvestigators.getCo_pi().contains(coPI)) {
							deletedInvestigators.getCo_pi().add(coPI);
							existingProposal.getInvestigatorInfo().getCo_pi()
									.remove(coPI);
						}
					} else {
						addedInvestigators.getCo_pi().remove(coPI);
					}
				}
				for (InvestigatorRefAndPosition senior : existingInvestigators
						.getSeniorPersonnel()) {
					if (!existingProposal.getInvestigatorInfo()
							.getSeniorPersonnel().contains(senior)) {
						if (!deletedInvestigators.getSeniorPersonnel()
								.contains(senior)) {
							deletedInvestigators.getSeniorPersonnel().add(
									senior);
							existingProposal.getInvestigatorInfo()
									.getSeniorPersonnel().remove(senior);
						}
					} else {
						addedInvestigators.getSeniorPersonnel().remove(senior);
					}
				}
				// Remove Signatures FOR Deleted Investigators
				for (InvestigatorRefAndPosition coPI : deletedInvestigators
						.getCo_pi()) {
					for (SignatureInfo sign : oldProposal.getSignatureInfo()) {
						if (coPI.getUserProfileId().equalsIgnoreCase(
								sign.getUserProfileId())) {
							existingProposal.getSignatureInfo().remove(sign);
						}
					}
				}
			}
		}
	}
}