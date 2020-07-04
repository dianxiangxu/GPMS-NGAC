package gpms.rest;

import gpms.DAL.MongoDBConnector;
import gpms.accesscontrol.BalanaConnector;
import gpms.dao.DelegationDAO;
import gpms.dao.NotificationDAO;
import gpms.dao.ProposalDAO;
import gpms.dao.UserAccountDAO;
import gpms.dao.UserProfileDAO;
import gpms.model.ApprovalType;
import gpms.model.AuditLogCommonInfo;
import gpms.model.AuditLogInfo;
import gpms.model.EmailCommonInfo;
import gpms.model.GPMSCommonInfo;
import gpms.model.InvestigatorInfo;
import gpms.model.InvestigatorRefAndPosition;
import gpms.model.Proposal;
import gpms.model.ProposalCommonInfo;
import gpms.model.ProposalInfo;
import gpms.model.ProposalStatusInfo;
import gpms.model.RequiredSignaturesInfo;
import gpms.model.SignatureInfo;
import gpms.model.SignatureUserInfo;
import gpms.model.Status;
import gpms.model.SubmitType;
import gpms.model.UserAccount;
import gpms.model.UserInfo;
import gpms.model.UserProfile;
import gpms.ngac.policy.Constants;
import gpms.ngac.policy.NGACPolicyConfigurationLoader;
import gpms.ngac.policy.PDSOperations;
import gpms.ngac.policy.ProposalDataSheet;
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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
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

import gov.nist.csd.pm.exceptions.PMException;
import gov.nist.csd.pm.operations.OperationSet;
import gov.nist.csd.pm.pdp.decider.PReviewDecider;
import gov.nist.csd.pm.pip.graph.Graph;
import gov.nist.csd.pm.pip.graph.GraphSerializer;
import gov.nist.csd.pm.pip.graph.MemGraph;
import gov.nist.csd.pm.pip.prohibitions.MemProhibitions;
import gov.nist.csd.pm.pip.prohibitions.Prohibitions;
import gov.nist.csd.pm.pip.prohibitions.ProhibitionsSerializer;
import gov.nist.csd.pm.pdp.PDP;

@Path("/proposals")
@Api(value = "/proposals", description = "Manage Proposals")
@Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.APPLICATION_FORM_URLENCODED,
		MediaType.TEXT_PLAIN, MediaType.TEXT_HTML })
@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_PLAIN, MediaType.TEXT_HTML })
public class ProposalService {
	MongoClient mongoClient = null;
	Morphia morphia = null;
	String dbName = "db_gpms";
	UserAccountDAO userAccountDAO = null;
	public UserProfileDAO userProfileDAO = null;
	public ProposalDAO proposalDAO = null;
	DelegationDAO delegationDAO = null;
	public NotificationDAO notificationDAO = null;
	private PDSOperations pdsOperations;
	NGACPolicyConfigurationLoader loader;
	public DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
	private static final Logger log = Logger.getLogger(ProposalService.class.getName());
	static Prohibitions prohibitions = new MemProhibitions();

	public ProposalService() {
		mongoClient = MongoDBConnector.getMongo();
		morphia = new Morphia();
		morphia.map(UserProfile.class).map(UserAccount.class);
		userAccountDAO = new UserAccountDAO(mongoClient, morphia, dbName);
		userProfileDAO = new UserProfileDAO(mongoClient, morphia, dbName);
		proposalDAO = new ProposalDAO(mongoClient, morphia, dbName);
		delegationDAO = new DelegationDAO(mongoClient, morphia, dbName);
		notificationDAO = new NotificationDAO(mongoClient, morphia, dbName);
		pdsOperations = new PDSOperations();
		loader = new NGACPolicyConfigurationLoader();
	}

	@GET
	@Produces(MediaType.TEXT_PLAIN)
	@ApiOperation(value = "Test Proposal Service", notes = "This API tests whether the service is working or not")
	@ApiResponses(value = { @ApiResponse(code = 200, message = "Success: { Hello World! }"),
			@ApiResponse(code = 400, message = "Failed: { \"error\":\"error description\", \"status\": \"FAIL\" }") })
	public Response testService() {
		try {
			log.info("ProposalService::testService started");
			return Response.status(Response.Status.OK).entity("Hello World!").build();
		} catch (Exception e) {
			log.error("Could not connect the Proposal Service error e=", e);
		}
		return Response.status(Response.Status.BAD_REQUEST)
				.entity("{\"error\": \"Could Not Find Proposal Service\", \"status\": \"FAIL\"}").build();
	}

	@POST
	@Path("/GetProposalStatusList")
	@ApiOperation(value = "Get all Proposal Status", notes = "This API gets all Proposal Status for dropdown")
	@ApiResponses(value = { @ApiResponse(code = 200, message = "Success: { Proposal Status Info }"),
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
			return Response.status(Response.Status.OK)
					.entity(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(proposalStatusList)).build();
		} catch (Exception e) {
			log.error("Could not find all Proposal Status error e=", e);
		}
		return Response.status(Response.Status.BAD_REQUEST)
				.entity("{\"error\": \"Could Not Find All Proposal Status\", \"status\": \"FAIL\"}").build();
	}

	@POST
	@Path("/GetProposalsList")
	@ApiOperation(value = "Get all Proposals", notes = "This API gets all Proposals")
	@ApiResponses(value = { @ApiResponse(code = 200, message = "Success: { Proposal Info }"),
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
			proposals = proposalDAO.findAllProposalsForGrid(offset, limit, proposalInfo);
			return Response.status(Response.Status.OK)
					.entity(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(proposals)).build();
		} catch (Exception e) {
			log.error("Could not find all Proposals error e=", e);
		}
		return Response.status(Response.Status.BAD_REQUEST)
				.entity("{\"error\": \"Could Not Find All Proposals\", \"status\": \"FAIL\"}").build();
	}

	@POST
	@Path("/GetUserProposalsList")
	@ApiOperation(value = "Get all Users", notes = "This API get all Proposals of a User")
	@ApiResponses(value = { @ApiResponse(code = 200, message = "Success: { Proposal Info }"),
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
			proposals = proposalDAO.findUserProposalGrid(offset, limit, proposalInfo, userInfo);
			return Response.status(Response.Status.OK)
					.entity(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(proposals)).build();
		} catch (Exception e) {
			log.error("Could not find all Proposals of a User error e=", e);
		}
		return Response.status(Response.Status.BAD_REQUEST)
				.entity("{\"error\": \"Could Not Find All Proposals Of A User\", \"status\": \"FAIL\"}").build();
	}

	@POST
	@Path("/AllProposalsExportToExcel")
	@Produces(MediaType.TEXT_HTML)
	@ApiOperation(value = "Export all Proposals in a grid", notes = "This API exports all Proposals shown in a grid For Admin")
	@ApiResponses(value = { @ApiResponse(code = 200, message = "Success: { Excel Filename/ No Record}"),
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
				filename = mapper.writerWithDefaultPrettyPrinter().writeValueAsString("No Record");
			}
			return Response.status(Response.Status.OK).entity(filename).build();
		} catch (Exception e) {
			log.error("Could not export Proposal list error e=", e);
		}
		return Response.status(Response.Status.BAD_REQUEST)
				.entity("{\"error\": \"Could Not Export Proposal List\", \"status\": \"FAIL\"}").build();
	}

	@POST
	@Path("/ProposalsExportToExcel")
	@Produces(MediaType.TEXT_HTML)
	@ApiOperation(value = "Export all Proposals in a grid", notes = "This API exports all Proposals shown in a grid")
	@ApiResponses(value = { @ApiResponse(code = 200, message = "Success: { Excel Filename/ No Record}"),
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
			proposals = proposalDAO.findAllUserProposalsForExport(proposalInfo, userInfo);
			String filename = new String();
			if (proposals.size() > 0) {
				filename = proposalDAO.exportToExcelFile(proposals, null);
			} else {
				filename = mapper.writerWithDefaultPrettyPrinter().writeValueAsString("No Record");
			}
			return Response.status(Response.Status.OK).entity(filename).build();
		} catch (Exception e) {
			log.error("Could not export all Proposals error e=", e);
		}
		return Response.status(Response.Status.BAD_REQUEST)
				.entity("{\"error\": \"Could Not Export All Proposals\", \"status\": \"FAIL\"}").build();
	}

	@POST
	@Path("/DeleteProposalByAdmin")
	@ApiOperation(value = "Delete Proposal by Admin", notes = "This API deletes Proposal by Admin")
	@ApiResponses(value = { @ApiResponse(code = 200, message = "Success: { True }"),
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
			Proposal existingProposal = proposalDAO.findProposalByProposalID(id);
			ObjectId authorId = new ObjectId(userInfo.getUserProfileID());
			UserProfile authorProfile = userProfileDAO.findUserDetailsByProfileID(authorId);
			String authorUserName = authorProfile.getUserAccount().getUserName();
			boolean isDeleted = proposalDAO.deleteProposalByAdmin(existingProposal, authorProfile);
			if (isDeleted) {
				sendNotification(id, existingProposal, authorUserName);
				return Response.status(Response.Status.OK)
						.entity(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(true)).build();
			}
		} catch (Exception e) {
			log.error("Could not delete Proposal error e=", e);
		}
		return Response.status(Response.Status.BAD_REQUEST)
				.entity("{\"error\": \"Could Not Delete Proposal\", \"status\": \"FAIL\"}").build();
	}

	@POST
	@Path("/DeleteProposalByProposalID")
	@ApiOperation(value = "Delete Proposal by ProposalId", notes = "This API deletes Proposal by ProposalId")
	@ApiResponses(value = { @ApiResponse(code = 200, message = "Success: { True }"),
			@ApiResponse(code = 403, message = "Failed: { \"error\":\"error description\", \"status\": \"FAIL\" }") })
	public Response deleteProposalByProposalID(
			@ApiParam(value = "Message", required = true, defaultValue = "", allowableValues = "", allowMultiple = false) String message) {

		try {
			log.info("ProposalService::deleteProposalByProposalID started");
			ObjectMapper mapper = new ObjectMapper();
			JsonNode root = mapper.readTree(message);
			if (root != null && root.has("policyInfo")) {
				JsonNode policyInfo = root.get("policyInfo");
				if (policyInfo != null && policyInfo.isArray() && policyInfo.size() > 0) {
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
						proposalUserTitle = root.get("proposalUserTitle").textValue();
					}
					GPMSCommonInfo userInfo = new GPMSCommonInfo();
					if (root != null && root.has("gpmsCommonObj")) {
						JsonNode commonObj = root.get("gpmsCommonObj");
						userInfo = new GPMSCommonInfo(commonObj);
					}
					ObjectId id = new ObjectId(proposalId);
					Proposal existingProposal = proposalDAO.findProposalByProposalID(id);
					List<SignatureUserInfo> signatures = proposalDAO.findAllUsersToBeNotified(id,
							existingProposal.isIrbApprovalRequired());
					ObjectId authorId = new ObjectId(userInfo.getUserProfileID());
					UserProfile authorProfile = userProfileDAO.findUserDetailsByProfileID(authorId);
					String authorUserName = authorProfile.getUserAccount().getUserName();
					ProposalDataSheet projectProposal = new ProposalDataSheet();
					String objectAtt = Constants.PDSs_OA_UA_LBL;
					projectProposal.setProposal(existingProposal);
					setPolicyState(projectProposal, existingProposal, null, userInfo.getUserName(), false);

					String decisionString = projectProposal.getPolicyDecisionAnyType(pdsOperations,
							userInfo.getUserName(), "U", "Delete", "PDSWhole");
					boolean isDeleted = false;
					if (decisionString.equals("Permit")) {
						isDeleted = proposalDAO.deleteProposal(existingProposal, proposalRoles, proposalUserTitle,
								authorProfile);

						if (isDeleted == false) {
							decisionString = projectProposal.getPolicyDecisionAnyType(pdsOperations,
									userInfo.getUserName(), "U", "Delete", "PDSWhole");
							return Response.status(403).type(MediaType.APPLICATION_JSON)
									.entity("Your permission is: " + "Deny").build();
						}

						log.info("D:" + decisionString + "Access Granted" + isDeleted);
						return Response.status(200).type(MediaType.APPLICATION_JSON)
								.entity(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(true)).build();
					} else {
						log.info("D:" + decisionString + "Access Denied" + isDeleted);
						return Response.status(403).type(MediaType.APPLICATION_JSON)
								.entity("Your permission is: " + decisionString).build();
					}

//					StringBuffer contentProfile = proposalDAO.generateContentProfile(proposalId, existingProposal,
//							signatures, authorProfile);
//					HashMap<String, Multimap<String, String>> attrMap = proposalDAO.generateAttributes(policyInfo);
//					BalanaConnector ac = new BalanaConnector();
//					Set<AbstractResult> set = ac.getXACMLdecisionWithObligations(attrMap, contentProfile);
//					Iterator<AbstractResult> it = set.iterator();
//					int intDecision = AbstractResult.DECISION_NOT_APPLICABLE;
//					while (it.hasNext()) {
//						AbstractResult ar = it.next();
//						intDecision = ar.getDecision();
//						if (intDecision == AbstractResult.DECISION_INDETERMINATE_DENY
//								|| intDecision == AbstractResult.DECISION_INDETERMINATE_PERMIT
//								|| intDecision == AbstractResult.DECISION_INDETERMINATE_DENY_OR_PERMIT) {
//							intDecision = AbstractResult.DECISION_INDETERMINATE;
//						}
//						//System.out.println(
//								"Decision:" + intDecision + " that is: " + AbstractResult.DECISIONS[intDecision]);
//						if (AbstractResult.DECISIONS[intDecision].equals("Permit")) {
//							List<ObligationResult> obligations = ar.getObligations();
//							//boolean isDeleted = proposalDAO.deleteProposalWithObligations(proposalRoles,
//							//		proposalUserTitle, existingProposal, authorProfile, authorUserName, obligations);
//							if (isDeleted) {
//								if (proposalRoles.equals("PI")
//										&& !proposalUserTitle.equals("University Research Director")) {
//									for (Iterator<SignatureUserInfo> iterator = signatures.iterator(); iterator
//											.hasNext();) {
//										SignatureUserInfo userToNotify = iterator.next();
//										if (userToNotify.getPositionTitle().equals("University Research Director")
//												|| userToNotify.getPositionTitle()
//														.equals("University Research Administrator")
//												|| userToNotify.getPositionTitle().equals("IRB")
//												|| userToNotify.getPositionTitle().equals("Dean")
//												|| userToNotify.getPositionTitle().equals("Business Manager")
//												|| userToNotify.getPositionTitle().equals("Department Chair")) {
//											iterator.remove();
//										}
//									}
//								}
//								sendDeleteNotification(proposalRoles, proposalUserTitle, existingProposal,
//										authorUserName, signatures);
//							}
//							return Response.status(200).type(MediaType.APPLICATION_JSON)
//									.entity(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(true)).build();
//						} else {
//							return Response.status(403).type(MediaType.APPLICATION_JSON)
//									.entity("Your permission is: " + AbstractResult.DECISIONS[intDecision]).build();
//						}
//					}
				} else {
					return Response.status(403).type(MediaType.APPLICATION_JSON)
							.entity("No User Permission Attributes are send!").build();
				}
			}
		} catch (Exception e) {
			log.error("Could not delete the selected Proposal error e=", e);
		}
		return Response.status(403)
				.entity("{\"error\": \"No User Permission Attributes are send!\", \"status\": \"FAIL\"}").build();

	}

	@POST
	@Path("/DeleteMultipleProposalsByAdmin")
	@ApiOperation(value = "Delete Multiple Proposals by Admin", notes = "This API deletes multiple Proposals by Admin")
	@ApiResponses(value = { @ApiResponse(code = 200, message = "Success: { True }"),
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
			UserProfile authorProfile = userProfileDAO.findUserDetailsByProfileID(authorId);
			String authorUserName = authorProfile.getUserAccount().getUserName();
			for (String proposalId : proposals) {
				ObjectId id = new ObjectId(proposalId);
				Proposal existingProposal = proposalDAO.findProposalByProposalID(id);
				boolean isDeleted = proposalDAO.deleteProposalByAdmin(existingProposal, authorProfile);
				if (isDeleted) {
					sendNotification(id, existingProposal, authorUserName);
				}
			}
			return Response.status(Response.Status.OK)
					.entity(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(true)).build();
		} catch (Exception e) {
			log.error("Could not delete Multiple Proposals error e=", e);
		}
		return Response.status(Response.Status.BAD_REQUEST)
				.entity("{\"error\": \"Could Not Delete Multiple Proposals\", \"status\": \"FAIL\"}").build();
	}

	@POST
	@Path("/GetAvailableActionsByProposalId")
	@ApiOperation(value = "Get Proposal Details by ProposalId", notes = "This API gets Proposal Details by ProposalId")
	@ApiResponses(value = { @ApiResponse(code = 200, message = "Success: { Proposal }"),
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
				subjectMap.put("position.title", userInfo.getUserPositionTitle());
				attrMap.put("Subject", subjectMap);
			} else {
				subjectMap.put("proposal.role", proposalRoles);
				attrMap.put("Subject", subjectMap);
			}

			List<String> actions = new ArrayList<String>();

			log.info("Proposal Role :" + proposalRoles);
			log.info("User info :" + userInfo);
			log.info("proposal ID:" + proposalId);

			ObjectId id = new ObjectId(proposalId);
			Proposal existingProposal = proposalDAO.findProposalByProposalID(id);

			// more roles : Professional staff(BM)|Administrator(Dean.chair)|University
			// administrator(irb/ra/rd)
			if (proposalRoles.equalsIgnoreCase("PI") || proposalRoles.equalsIgnoreCase("Co-PI")
					|| proposalRoles.equalsIgnoreCase("SP")
					|| userInfo.getUserPositionType().equalsIgnoreCase("Administrator")
					|| userInfo.getUserPositionType().equalsIgnoreCase("Professional staff")
					|| userInfo.getUserPositionType().equalsIgnoreCase("University administrator")) {
				ProposalDataSheet projectProposal = new ProposalDataSheet();
				long proposalNgacId = existingProposal.getNgacId();
				log.info("NN: Proposal NGAC Id:" + proposalNgacId);

				Graph proposalPolicy = null;

				setPolicyState(projectProposal, existingProposal, proposalPolicy, userInfo.getUserName(), false);

				String stage = projectProposal.getApprovalStage();
				log.info("Stage:" + stage);
				if (projectProposal.isProposalSubmitted()) {
					if (!stage.equals(Constants.ZOMBIE_STATE)) {
						actions = projectProposal.getPermittedActions(pdsOperations, userInfo.getUserName(),
								"PDSWhole");
					}
				} else if (!stage.equals(Constants.ZOMBIE_STATE)) {
					actions = projectProposal.getPermittedActions(pdsOperations, userInfo.getUserName(), "PDSWhole");
				}
			} else {

				StringBuffer contentProfile = proposalDAO.generateProposalContentProfile(proposalId, userInfo,
						existingProposal);
				if (attrMap.get("Resource") == null) {
					attrMap.put("Resource", resourceMap);
				}
				actions = proposalDAO.generateMDPDecision(attrMap, actionMap, contentProfile);

			}

			log.info("Permitted Actions:" + actions);
			return Response.status(Response.Status.OK).entity(
					mapper.setDateFormat(formatter).writerWithDefaultPrettyPrinter().writeValueAsString(actions))
					.build();
		} catch (Exception e) {
			log.error("Could not find Proposal Details by ProposalId error e=", e);
		}
		return Response.status(Response.Status.BAD_REQUEST)
				.entity("{\"error\": \"Could Not Find Proposal Details By ProposalId\", \"status\": \"FAIL\"}").build();
	}

	@POST
	@Path("/GetProposalDetailsByProposalId")
	@ApiOperation(value = "Get Proposal Details by ProposalId", notes = "This API gets Proposal Details by ProposalId")
	@ApiResponses(value = { @ApiResponse(code = 200, message = "Success: { Proposal }"),
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
			return Response.status(Response.Status.OK).entity(
					mapper.setDateFormat(formatter).writerWithDefaultPrettyPrinter().writeValueAsString(proposal))
					.build();
		} catch (Exception e) {
			log.error("Could not find Proposal Details by ProposalId error e=", e);
		}
		return Response.status(Response.Status.BAD_REQUEST)
				.entity("{\"error\": \"Could Not Find Proposal Details By ProposalId\", \"status\": \"FAIL\"}").build();
	}

	@POST
	@Path("/GetProposalAuditLogList")
	@ApiOperation(value = "Get Proposal Audit Log List", notes = "This API gets Proposal Audit Log List")
	@ApiResponses(value = { @ApiResponse(code = 200, message = "Success: { AuditLog Info }"),
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
			proposalAuditLogs = proposalDAO.findAllProposalAuditLogForGrid(offset, limit, id, auditLogInfo);
			return Response.status(Response.Status.OK)
					.entity(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(proposalAuditLogs)).build();
		} catch (Exception e) {
			log.error("Could not find Proposal Audit Log List error e=", e);
		}
		return Response.status(Response.Status.BAD_REQUEST)
				.entity("{\"error\": \"Could Not Find Proposal Audit Log List\", \"status\": \"FAIL\"}").build();
	}

	@POST
	@Path("/ProposalLogsExportToExcel")
	@ApiOperation(value = "Export all Proposal Logs in a grid", notes = "This API exports all Proposal Logs shown in a grid")
	@ApiResponses(value = { @ApiResponse(code = 200, message = "Success: { Excel Filename/ No Record}"),
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
			proposalAuditLogs = proposalDAO.getSortedAuditLogResults(auditLogInfo, id);
			String filename = new String();
			if (proposalAuditLogs.size() > 0) {
				filename = proposalDAO.exportToExcelFile(null, proposalAuditLogs);
			} else {
				filename = mapper.writerWithDefaultPrettyPrinter().writeValueAsString("No Record");
			}
			return Response.status(Response.Status.OK).entity(filename).build();
		} catch (Exception e) {
			log.error("Could not export Proposal Logs error e=", e);
		}
		return Response.status(Response.Status.BAD_REQUEST)
				.entity("{\"error\": \"Could Not Proposal Logs List\", \"status\": \"FAIL\"}").build();
	}

	@POST
	@Path("/CheckUniqueProjectTitle")
	@ApiOperation(value = "Check for Unique Project Title", notes = "This API checks if provided Project Title is Unique or not")
	@ApiResponses(value = { @ApiResponse(code = 200, message = "Success: { True/ False }"),
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
				if (proposalUniqueObj != null && proposalUniqueObj.has("ProposalID")) {
					proposalID = proposalUniqueObj.get("ProposalID").textValue();
				}
				if (proposalUniqueObj != null && proposalUniqueObj.has("NewProjectTitle")) {
					String projectTitle = proposalUniqueObj.get("NewProjectTitle").textValue().trim()
							.replaceAll("\\<[^>]*>", "");
					log.info("Proposal Id: " + proposalID + ", Title:" + projectTitle);
					if (proposalDAO.validateNotEmptyValue(projectTitle)) {
						newProjectTitle = projectTitle;
					} else {
						return Response.status(403).entity("The Project Title can not be Empty").build();
					}
				}
			}
			Proposal proposal = new Proposal();
			if (!proposalID.equals("0")) {
				ObjectId id = new ObjectId(proposalID);
				proposal = proposalDAO.findNextProposalWithSameProjectTitle(id, newProjectTitle);
			} else {
				proposal = proposalDAO.findAnyProposalWithSameProjectTitle(newProjectTitle);
			}
			if (proposal != null) {
				response = mapper.writerWithDefaultPrettyPrinter().writeValueAsString("false");
			} else {
				response = mapper.writerWithDefaultPrettyPrinter().writeValueAsString("true");
			}
			return Response.status(Response.Status.OK).entity(response).build();

		} catch (Exception e) {
			log.error("Could not check for unique Project Title error e=", e);
		}
		return Response.status(Response.Status.BAD_REQUEST)
				.entity("{\"error\": \"Check For Unique Project Title\", \"status\": \"FAIL\"}").build();
	}

	@POST
	@Path("/GetAllSignatureForAProposal")
	@ApiOperation(value = "Get all Signatures For A Proposal", notes = "This API gets all Signatures for a Proposal")
	@ApiResponses(value = { @ApiResponse(code = 200, message = "Success: { Signature Info }"),
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
				irbApprovalRequired = Boolean.parseBoolean(root.get("irbApprovalRequired").textValue());
			}
			ObjectId id = new ObjectId(proposalId);
			List<SignatureInfo> signatures = proposalDAO.getSignaturesOfAProposal(id, irbApprovalRequired,
					proposalDAO.findProposalByProposalID(id).getPolicyGraph());
			return Response.status(Response.Status.OK)
					.entity(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(signatures)).build();
		} catch (Exception e) {
			log.error("Could not find all Signatures for a Proposal error e=", e);
		}
		return Response.status(Response.Status.BAD_REQUEST)
				.entity("{\"error\": \"Could Not Find All Signatures For A Proposal\", \"status\": \"FAIL\"}").build();
	}

	@POST
	@Path("/UpdateProposalStatus")
	@ApiOperation(value = "Update Proposal Status", notes = "This API updates Proposal Status")
	@ApiResponses(value = { @ApiResponse(code = 200, message = "Success: { True }"),
			@ApiResponse(code = 400, message = "Failed: { \"error\":\"error description\", \"status\": \"FAIL\" }") })
	public Response updateProposalStatus(
			@ApiParam(value = "Message", required = true, defaultValue = "", allowableValues = "", allowMultiple = false) String message) {
		try {
			log.info("ProposalService::updateProposalStatus started");
			ObjectMapper mapper = new ObjectMapper();
			JsonNode root = mapper.readTree(message);
			if (root != null && root.has("policyInfo")) {
				JsonNode policyInfo = root.get("policyInfo");
				if (policyInfo != null && policyInfo.isArray() && policyInfo.size() > 0) {
					String proposalId = new String();
					String proposalUserTitle = new String();
					String buttonType = new String();
					if (root != null && root.has("proposalId")) {
						proposalId = root.get("proposalId").textValue();
					}
					if (root != null && root.has("proposalUserTitle")) {
						proposalUserTitle = root.get("proposalUserTitle").textValue();
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
					Proposal existingProposal = proposalDAO.findProposalByProposalID(id);
					List<SignatureUserInfo> signatures = proposalDAO.findAllUsersToBeNotified(id,
							existingProposal.isIrbApprovalRequired());
					ObjectId authorId = new ObjectId(userInfo.getUserProfileID());
					UserProfile authorProfile = userProfileDAO.findUserDetailsByProfileID(authorId);
					String authorUserName = authorProfile.getUserAccount().getUserName();
					StringBuffer contentProfile = proposalDAO.generateContentProfile(proposalId, existingProposal,
							signatures, authorProfile);
					BalanaConnector ac = new BalanaConnector();
					HashMap<String, Multimap<String, String>> attrMap = proposalDAO.generateAttributes(policyInfo);

					log.info("Action:" + buttonType);
					String action = new String();
					String subject = new String();
					String object = new String();
					for (JsonNode node : policyInfo) {
						String attributeValue = node.path("attributeValue").asText();
						String attributeType = node.path("attributeType").asText();
						switch (attributeType) {
						case "Action":
							action = attributeValue;
							break;
						case "Subject":
							log.info("ATT NAME:" + node.path("attributeName"));
							subject = attributeValue;
							log.info("ATT VALUE:" + subject);
							break;
						case "Resource":
							object = attributeValue;
							break;
						default:
							break;
						}
					}

					log.info("Action:" + action);
					log.info("Subject:" + subject);
					log.info("Resource:" + object);
					String decision = "";

					if (action.equals("Archive")) {

						ProposalDataSheet projectProposal = new ProposalDataSheet();
						long proposalNgacId = existingProposal.getNgacId();
						log.info("NN: Proposal NGAC Id:" + proposalNgacId);

						Graph proposalPolicy = null;
						setPolicyState(projectProposal, existingProposal, proposalPolicy, userInfo.getUserName(),
								false);

						String stage = projectProposal.getApprovalStage();
						log.info("Stage:" + stage);

						decision = projectProposal.getPolicyDecision(pdsOperations, userInfo.getUserName(), action,
								Constants.APPROVAL_CONTENT);
						log.info("D:" + decision);

					}
					if (action.equals("Withdraw")) {

						ProposalDataSheet projectProposal = new ProposalDataSheet();
						long proposalNgacId = existingProposal.getNgacId();
						log.info("NN: Proposal NGAC Id:" + proposalNgacId);

						Graph proposalPolicy = null;
						setPolicyState(projectProposal, existingProposal, proposalPolicy, userInfo.getUserName(),
								false);

						String stage = projectProposal.getApprovalStage();
						log.info("Stage:" + stage);

						decision = projectProposal.getPolicyDecision(pdsOperations, userInfo.getUserName(), action,
								Constants.APPROVAL_CONTENT);
						log.info("D:" + decision);

					}
//					Set<AbstractResult> set = ac
//							.getXACMLdecisionWithObligations(attrMap,
//									contentProfile);
//					Iterator<AbstractResult> it = set.iterator();
//					int intDecision = AbstractResult.DECISION_NOT_APPLICABLE;
//					while (it.hasNext()) {
//						AbstractResult ar = it.next();
//						intDecision = ar.getDecision();
//						if (intDecision == AbstractResult.DECISION_INDETERMINATE_DENY
//								|| intDecision == AbstractResult.DECISION_INDETERMINATE_PERMIT
//								|| intDecision == AbstractResult.DECISION_INDETERMINATE_DENY_OR_PERMIT) {
//							intDecision = AbstractResult.DECISION_INDETERMINATE;
//						}
//						//System.out.println("Decision:" + intDecision
//								+ " that is: "
//								+ AbstractResult.DECISIONS[intDecision]);
//						if (AbstractResult.DECISIONS[intDecision]
//								.equals("Permit")) 
					if (decision.equals("Permit")) {
						// List<ObligationResult> obligations = ar
						// .getObligations();
						List<ObligationResult> obligations = new ArrayList<ObligationResult>();
						String changeDone = proposalDAO.updateProposalStatusWithObligations(proposalId, buttonType,
								proposalUserTitle, existingProposal, authorProfile, authorUserName, obligations);
						String notificationMessage = changeDone + " by " + authorUserName + ".";
						if (changeDone.equals("Withdrawn")) {
							for (Iterator<SignatureUserInfo> iterator = signatures.iterator(); iterator.hasNext();) {
								SignatureUserInfo userToNotify = iterator.next();
								if (userToNotify.getPositionTitle().equals("University Research Director")) {
									iterator.remove();
								}
							}
							broadCastNotification(existingProposal.getId().toString(),
									existingProposal.getProjectInfo().getProjectTitle(), notificationMessage,
									"Proposal", signatures, true);
						} else if (changeDone.equals("Archived")) {
							broadCastNotification(existingProposal.getId().toString(),
									existingProposal.getProjectInfo().getProjectTitle(), notificationMessage,
									"Proposal", signatures, true);
						}
						return Response.status(200).type(MediaType.APPLICATION_JSON)
								.entity(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(true)).build();
					} else {
						return Response.status(403).type(MediaType.APPLICATION_JSON)
								.entity("Your permission is: " + decision).build();
					}
					// } ///end while
				} else {
					return Response.status(403).type(MediaType.APPLICATION_JSON)
							.entity("No User Permission Attributes are send!").build();
				}
			}
		} catch (Exception e) {
			log.error("Could not update Proposal Status error e=", e);
		}
		return Response.status(403)
				.entity("{\"error\": \"No User Permission Attributes are send!\", \"status\": \"FAIL\"}").build();
	}

	@POST
	@Path("/SaveUpdateProposalByAdmin")
	@ApiOperation(value = "Save a New Proposal or Update an existing Proposal by Admin", notes = "This API saves a New User or updates an existing Proposal by Admin")
	@ApiResponses(value = { @ApiResponse(code = 200, message = "Success: { True }"),
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
			UserProfile authorProfile = userProfileDAO.findUserDetailsByProfileID(authorId);
			String proposalId = new String();
			Proposal existingProposal = new Proposal();
			Proposal oldProposal = new Proposal();
			if (root != null && root.has("proposalInfo")) {
				JsonNode proposalInfo = root.get("proposalInfo");
				if (proposalInfo != null && proposalInfo.has("ProposalID")) {
					proposalId = proposalInfo.get("ProposalID").textValue();
					if (!proposalId.equals("0")) {
						ObjectId id = new ObjectId(proposalId);
						existingProposal = proposalDAO.findProposalByProposalID(id);
						oldProposal = SerializationHelper.cloneThroughSerialize(existingProposal);
					}
				}
				proposalDAO.getAppendixDetails(proposalId, existingProposal, oldProposal, proposalInfo);
				getInvestigatorInfoDetails(proposalId, existingProposal, oldProposal, proposalInfo);
				Boolean irbApprovalRequired = proposalDAO.getComplianceDetails(proposalId, existingProposal,
						proposalInfo);
				EmailCommonInfo emailDetails = proposalDAO.saveProposalWithoutObligations(message, proposalId,
						existingProposal, oldProposal, authorProfile, irbApprovalRequired);

				if (sendSaveUpdateNotification(userInfo.getUserName(), message, null, proposalId, existingProposal,
						oldProposal, authorProfile, true, null, irbApprovalRequired, null, emailDetails, "")) {
					return Response.status(200).type(MediaType.APPLICATION_JSON)
							.entity(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(true)).build();
				}
			} else {
				return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON)
						.entity("No Proposal Info is send!").build();
			}
		} catch (Exception e) {
			log.error("Could not save a New User or update an existing Proposal error e=", e);
		}

		return Response.status(403).entity(
				"{\"error\": \"Could Not Save A New User OR Update AN Existing Proposal\", \"status\": \"FAIL\"}")
				.build();
	}

	private void setPolicyState(ProposalDataSheet projectProposal, Proposal existingProposal, Graph proposalPolicy,
			String userName, boolean firstSave) throws Exception {
		Graph test = new MemGraph();
		if (existingProposal != null && existingProposal.getPolicyGraph() != null
				&& existingProposal.getPolicyGraph().length() > 20) {
			GraphSerializer.fromJson(test, existingProposal.getPolicyGraph());
		}

		projectProposal.setProposal(existingProposal);
		log.info("NGAC ID found :" + existingProposal.getNgacId());

		log.info("222222222");

		if (PDSOperations.proposalPolicies.get(existingProposal.getNgacId()) != null) {
			projectProposal.setProposalPolicy(PDSOperations.proposalPolicies.get(existingProposal.getNgacId()));
			proposalPolicy = projectProposal.getProposalPolicy();
			log.info("33333333");
			if (test.getNodes().size() > 10) {
				projectProposal.setProposalPolicy(test);
				log.info("USING GRAPH FROM DATABASE");
			} else {
				projectProposal.setProposalPolicy(proposalPolicy);
			}
		} else {
			log.info("Policy re-created.");
			log.info("44444444444");

			proposalPolicy = pdsOperations.getBacicNGACPolicy();
			proposalPolicy = loader.createAProposalGraph(proposalPolicy);
			proposalPolicy = loader.createAprovalGraph(proposalPolicy);

			if (test.getNodes().size() > 10) {
				projectProposal.setProposalPolicy(test);
				log.info("USING GRAPH FROM DATABASE");
			} else {
				projectProposal.setProposalPolicy(proposalPolicy);
			}

		}

		boolean isPostSubmissionStage = false;

		if (projectProposal.isProposalSubmitted()) {
			isPostSubmissionStage = true;
			log.info("55555555555");
		}
		if (!firstSave) {
			// System.out.println("77777777777777777777777777777");

			projectProposal.clearIngestigator();
			projectProposal.updatePI(isPostSubmissionStage);
			projectProposal.updateCoPI(userName, isPostSubmissionStage);
			projectProposal.updateSP(userName, isPostSubmissionStage);
		}

		if (isPostSubmissionStage) {
			log.info("666666666666");
			// projectProposal.generatePostSubmissionProhibitions();
			projectProposal.updatePostSubmissionchanges(proposalDAO);
			projectProposal.associateApprovalRelation(projectProposal.getApprovalStage());
		}
		proposalPolicy = projectProposal.getProposalPolicy();
		PDSOperations.proposalPolicies.put(existingProposal.getNgacId(), proposalPolicy);
		if (existingProposal.getProhibitions().length() > 30) {
			log.info("USING PROHIBITIONS FROM DATABASE");

			prohibitions = new MemProhibitions();
			projectProposal
					.setProhibitions(ProhibitionsSerializer.fromJson(prohibitions, existingProposal.getProhibitions()));
		}

//		for(String s: test.getChildren("PI")) {
//			log.info("CHILDREN OF PI IN SET POLICY: "+s);
//		}
	}

	@POST
	@Path("/SaveUpdateProposal")
	@ApiOperation(value = "Save a New Proposal or Update an existing Proposal", notes = "This API saves a New Proposal or updates an existing Proposal")
	@ApiResponses(value = { @ApiResponse(code = 200, message = "Success: { True }"),
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
			UserProfile authorProfile = userProfileDAO.findUserDetailsByProfileID(authorId);
			String proposalId = new String();
			String proposalNgacId = new String();
			Proposal existingProposal = new Proposal();
			ProposalDataSheet projectProposal = new ProposalDataSheet();
			Proposal oldProposal = new Proposal();
			StringBuffer contentProfile = new StringBuffer();
			BalanaConnector ac = new BalanaConnector();
			if (root != null && root.has("proposalInfo")) {
				JsonNode proposalInfo = root.get("proposalInfo");
				log.info(proposalInfo);
				if (proposalInfo != null && proposalInfo.has("ProposalID")) {
					proposalId = proposalInfo.get("ProposalID").textValue();
					log.info("NN: Proposal Id:" + proposalId);
					proposalNgacId = proposalInfo.get("proposalNgacId").textValue();
					log.info("NN: Proposal NGAC Id:" + proposalNgacId);
					if (!proposalId.equals("0")) {
						ObjectId id = new ObjectId(proposalId);
						existingProposal = proposalDAO.findProposalByProposalID(id);
						oldProposal = SerializationHelper.cloneThroughSerialize(existingProposal);
					}
				}
				String proposalRoles = "";
				if (root != null && root.has("proposalRoles")) {
					proposalRoles = root.get("proposalRoles").textValue();
				}
				Graph proposalPolicy = null;
				boolean firstSave = false;

				projectProposal.setProposal(existingProposal);
				if (existingProposal.getNgacId() == 0l) {
					firstSave = true;
					existingProposal.setNgacId(Long.parseLong(proposalNgacId));
				}

				setPolicyState(projectProposal, existingProposal, proposalPolicy, userInfo.getUserName(), firstSave);

				proposalDAO.getAppendixDetails(proposalId, existingProposal, oldProposal, proposalInfo);
				getInvestigatorInfoDetails(proposalId, existingProposal, oldProposal, proposalInfo);
				Boolean irbApprovalRequired = proposalDAO.getComplianceDetails(proposalId, existingProposal,
						proposalInfo);
				boolean signedByCurrentUser = proposalDAO.getSignatureDetails(userInfo, proposalId, existingProposal,
						proposalInfo);
				if (root != null && root.has("policyInfo")) {
					JsonNode policyInfo = root.get("policyInfo");
					if (policyInfo != null && policyInfo.isArray() && policyInfo.size() > 0) {
						HashMap<String, Multimap<String, String>> attrMap = proposalDAO.generateAttributes(policyInfo);
						String action = new String();
						String subject = new String();
						String object = new String();
						for (JsonNode node : policyInfo) {
							String attributeValue = node.path("attributeValue").asText();
							String attributeType = node.path("attributeType").asText();
							switch (attributeType) {
							case "Action":
								action = attributeValue;
								break;
							case "Subject":
								log.info("ATT NAME:" + node.path("attributeName"));
								subject = attributeValue;
								log.info("ATT VALUE:" + subject);
								break;
							case "Resource":
								object = attributeValue;
								break;
							default:
								break;
							}
						}

						List<SignatureUserInfo> signatures = new ArrayList<SignatureUserInfo>();
						RequiredSignaturesInfo requiredSignatures = new RequiredSignaturesInfo();
						signatures = proposalDAO.generateProposalContentProfile(authorProfile, proposalId,
								existingProposal, signedByCurrentUser, contentProfile, irbApprovalRequired,
								requiredSignatures);
						log.info("INSIDE SAVE UPDATE:" + attrMap);
						log.info("Action NOW:" + action + "|Subject:" + subject + "| object:" + object);

						for (SignatureUserInfo sign : signatures) {
							log.info(sign.getDepartment() + "|" + sign.getPositionTitle() + "|" + sign.getUserName());
						}

						String decisionString = "";
						EmailCommonInfo emailDetails = new EmailCommonInfo();
						int intDecision = AbstractResult.DECISION_NOT_APPLICABLE;

						if (action.equals("Save") || action.equals("Submit")) {
							String objectAtt = "PDSWhole";
							String acRight = action;
							// objectAtt = projectProposal.setSection(proposalSection);
							log.info("objectAtt:" + objectAtt);
							if (action.equals("Submit") && userInfo.getUserPositionTitle()
									.equalsIgnoreCase("University Research Administrator")) {
								objectAtt = "PDSWhole";
								PDP pdp = pdsOperations.raSubmit(userInfo.getUserName(),
										existingProposal.getPolicyGraph());
								existingProposal.setPolicyGraph(GraphSerializer.toJson(pdp.getPAP().getGraphPAP()));

								existingProposal.setProhibitions(
										ProhibitionsSerializer.toJson(pdp.getPAP().getProhibitionsPAP()));
							}
							if (action.equals("Save")) {
								String decisionStringWriteCopi = projectProposal.getPolicyDecisionAnyType(pdsOperations,
										userInfo.getUserName(), "U", "write", "CoPIEditable");
								String decisionStringWritePI = projectProposal.getPolicyDecisionAnyType(pdsOperations,
										userInfo.getUserName(), "U", "write", "PIEditable");
								if (decisionStringWriteCopi.equals("Permit")
										|| decisionStringWritePI.equals("Permit")) {
									decisionString = "Permit";
								}
							} else {
								decisionString = projectProposal.getPolicyDecisionAnyType(pdsOperations,
										userInfo.getUserName(), "U", acRight, objectAtt);
							}
							log.info("D:" + decisionString);
							if (action.equals("Submit") && !userInfo.getUserPositionTitle()
									.equalsIgnoreCase("University Research Administrator")) {
								if (!projectProposal.isReadyForSubMission()) {
									return Response.status(403).type(MediaType.APPLICATION_JSON)
											.entity("Proposal is not ready to be submitted.").build();

								}
								projectProposal.updateCoPI(userInfo.getUserName(), true);
								projectProposal.updateSP(userInfo.getUserName(), true);

								existingProposal
										.setPolicyGraph(GraphSerializer.toJson(projectProposal.getProposalPolicy()));
								PDP pdp = pdsOperations.submitAProposal(userInfo.getUserName(),
										existingProposal.getPolicyGraph(), irbApprovalRequired);
								existingProposal.setPolicyGraph(GraphSerializer.toJson(pdp.getPAP().getGraphPAP()));

								existingProposal.setProhibitions(
										ProhibitionsSerializer.toJson(pdp.getPAP().getProhibitionsPAP()));

							}
							if (action.equals("Save")) {
								projectProposal.updateCoPI(userInfo.getUserName(), true);
								projectProposal.updateSP(userInfo.getUserName(), true);

								existingProposal
										.setPolicyGraph(GraphSerializer.toJson(projectProposal.getProposalPolicy()));
							}
						} else if (action.equals("Approve") || action.equals("Disapprove")) {
							List<String> actions = null;
							String objectAtt = Constants.APPROVAL_CONTENT;
							String acRight = action;
							// objectAtt = projectProposal.setSection(proposalSection);
							log.info("objectAtt:" + objectAtt);
							actions = projectProposal.getPermittedActions(pdsOperations, userInfo.getUserName(),
									Constants.APPROVAL_CONTENT);

							log.info("STATE OF APPROVAL:" + projectProposal.getApprovalStage());
							if (action.equals("Approve")) {
								approveAction(projectProposal, existingProposal, userInfo);
							}

							if (action.equals("Disapprove")) {
								disapproveAction(projectProposal, existingProposal, userInfo);
							}

							if (actions.contains(action))
								decisionString = "Permit";
							else
								decisionString = "Deny";
							log.info("D:" + decisionString);
						}
						if (decisionString.equals("Permit")) {

							log.info("Pre :Action:" + action + "|Role:" + proposalRoles + "|Name:"
									+ userInfo.getUserName() + "|Stage:" + projectProposal.getApprovalStage());
							if (sendSaveUpdateNotification(userInfo.getUserName(), message, projectProposal, proposalId,
									existingProposal, oldProposal, authorProfile, false, signatures,
									irbApprovalRequired, requiredSignatures, emailDetails, action)) {

								log.info("User Info:" + userInfo.toString());

								if (action.equals("Submit") && !userInfo.getUserPositionTitle()
										.equalsIgnoreCase("University Research Administrator")) {
									projectProposal.updatePI(true);
									projectProposal.updateCoPI(userInfo.getUserName(), true);
									projectProposal.updateSP(userInfo.getUserName(), true);
									existingProposal.setPolicyGraph(
											GraphSerializer.toJson(projectProposal.getProposalPolicy()));
									PDSOperations.proposalPolicies.put(existingProposal.getNgacId(), proposalPolicy);
								}

								return Response.status(200).type(MediaType.APPLICATION_JSON)
										.entity(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(true))
										.build();
							}
						} else {
							return Response.status(403).type(MediaType.APPLICATION_JSON)
									.entity("Your permission is: " + AbstractResult.DECISIONS[intDecision]).build();
						}
						// } end while
					}
				}
			} else {
				return Response.status(403).type(MediaType.APPLICATION_JSON).entity("No Proposal Info is send!")
						.build();
			}
			return Response.status(403).type(MediaType.APPLICATION_JSON)
					.entity("The chosen Entity does not have the rights to be that Entity!").build();
		} catch (Exception e) {
			e.printStackTrace();
			log.error("Could not save a New Proposal or update an existing Proposal error e=", e);
		}
		return Response.status(403).entity(
				"{\"error\": \"Could Not Save A New Proposal OR Update AN Existing Proposal\", \"status\": \"FAIL\"}")
				.build();
	}

	private void approveAction(ProposalDataSheet projectProposal, Proposal existingProposal, GPMSCommonInfo userInfo)
			throws PMException {
		if (projectProposal.getApprovalStage().equals(Constants.STATE_CHAIR)) {
			PDP pdp = pdsOperations.chairApprove(userInfo.getUserName(), existingProposal.getPolicyGraph());
			existingProposal.setPolicyGraph(GraphSerializer.toJson(pdp.getPAP().getGraphPAP()));
			Map<String, OperationSet> assoc = pdp.getPAP().getGraphPAP().getTargetAssociations("PDSWhole");
			for (String s : assoc.keySet()) {
				// System.out.println("ASSOCIATIONS: " + s);
			}
		}

		else if (projectProposal.getApprovalStage().equals(Constants.STATE_BM)) {
			PDP pdp = pdsOperations.bmApprove(userInfo.getUserName(), existingProposal.getPolicyGraph());
			existingProposal.setPolicyGraph(GraphSerializer.toJson(pdp.getPAP().getGraphPAP()));
			Map<String, OperationSet> assoc = pdp.getPAP().getGraphPAP().getTargetAssociations("PDSWhole");
			for (String s : assoc.keySet()) {
				// System.out.println("ASSOCIATIONS: " + s);
			}
		}

		else if (projectProposal.getApprovalStage().equals(Constants.STATE_DEAN)) {
			PDP pdp = pdsOperations.deanApprove(userInfo.getUserName(), existingProposal.getPolicyGraph());
			existingProposal.setPolicyGraph(GraphSerializer.toJson(pdp.getPAP().getGraphPAP()));
			Map<String, OperationSet> assoc = pdp.getPAP().getGraphPAP().getTargetAssociations("PDSWhole");
			for (String s : assoc.keySet()) {
				// System.out.println("ASSOCIATIONS: " + s);
			}
		}

		else if (userInfo.getUserName().equals("irbUser")) {
			PDP pdp = pdsOperations.irbApprove(userInfo.getUserName(), existingProposal.getPolicyGraph());
			existingProposal.setPolicyGraph(GraphSerializer.toJson(pdp.getPAP().getGraphPAP()));
			Map<String, OperationSet> assoc = pdp.getPAP().getGraphPAP().getTargetAssociations("PDSWhole");
			for (String s : assoc.keySet()) {
				// System.out.println("ASSOCIATIONS: " + s);
			}
		}

		else if (projectProposal.getApprovalStage().equals(Constants.STATE_RA)) {
			PDP pdp = pdsOperations.raApprove(userInfo.getUserName(), existingProposal.getPolicyGraph());
			existingProposal.setPolicyGraph(GraphSerializer.toJson(pdp.getPAP().getGraphPAP()));
			Map<String, OperationSet> assoc = pdp.getPAP().getGraphPAP().getTargetAssociations("PDSWhole");
			for (String s : assoc.keySet()) {
				// System.out.println("ASSOCIATIONS: " + s);
			}
		}
		if (projectProposal.getApprovalStage().equals(Constants.STATE_RD)) {
			PDP pdp = pdsOperations.rdApprove(userInfo.getUserName(), existingProposal.getPolicyGraph());
			existingProposal.setPolicyGraph(GraphSerializer.toJson(pdp.getPAP().getGraphPAP()));
			Map<String, OperationSet> assoc = pdp.getPAP().getGraphPAP().getTargetAssociations("PDSWhole");
			for (String s : assoc.keySet()) {
				// System.out.println("ASSOCIATIONS: " + s);
			}
		}

	}

	private void disapproveAction(ProposalDataSheet projectProposal, Proposal existingProposal, GPMSCommonInfo userInfo)
			throws PMException {

		if (projectProposal.getApprovalStage().equals(Constants.STATE_CHAIR)) {
			PDP pdp = pdsOperations.chairDisapprove(userInfo.getUserName(), existingProposal.getPolicyGraph());
			prohibitions = new MemProhibitions();
			existingProposal.setPolicyGraph(GraphSerializer.toJson(pdp.getPAP().getGraphPAP()));
			existingProposal.setProhibitions(ProhibitionsSerializer.toJson(pdp.getPAP().getProhibitionsPAP()));
			for (String s : pdp.getPAP().getGraphPAP().getChildren("PI")) {
				log.info("CHILDREN OF PI: " + s);
			}
		}

		if (projectProposal.getApprovalStage().equals(Constants.STATE_BM)) {
			PDP pdp = pdsOperations.bmDisapprove(userInfo.getUserName(), existingProposal.getPolicyGraph());
			prohibitions = new MemProhibitions();
			existingProposal.setPolicyGraph(GraphSerializer.toJson(pdp.getPAP().getGraphPAP()));
			existingProposal.setProhibitions(ProhibitionsSerializer.toJson(pdp.getPAP().getProhibitionsPAP()));
			for (String s : pdp.getPAP().getGraphPAP().getChildren("PI")) {
				log.info("CHILDREN OF PI: " + s);
			}
		}

		if (projectProposal.getApprovalStage().equals(Constants.STATE_DEAN)) {
			PDP pdp = pdsOperations.deanDisapprove(userInfo.getUserName(), existingProposal.getPolicyGraph());
			prohibitions = new MemProhibitions();
			existingProposal.setPolicyGraph(GraphSerializer.toJson(pdp.getPAP().getGraphPAP()));
			existingProposal.setProhibitions(ProhibitionsSerializer.toJson(pdp.getPAP().getProhibitionsPAP()));
			for (String s : pdp.getPAP().getGraphPAP().getChildren("PI")) {
				log.info("CHILDREN OF PI: " + s);
			}
		}

		if (userInfo.getUserName().equals("irbUser")) {
			PDP pdp = pdsOperations.irbDisapprove(userInfo.getUserName(), existingProposal.getPolicyGraph());
			prohibitions = new MemProhibitions();
			existingProposal.setPolicyGraph(GraphSerializer.toJson(pdp.getPAP().getGraphPAP()));
			existingProposal.setProhibitions(ProhibitionsSerializer.toJson(pdp.getPAP().getProhibitionsPAP()));
			for (String s : pdp.getPAP().getGraphPAP().getChildren("PI")) {
				log.info("CHILDREN OF PI: " + s);
			}
		}

		if (projectProposal.getApprovalStage().equals(Constants.STATE_RA)) {
			PDP pdp = pdsOperations.raDisapprove(userInfo.getUserName(), existingProposal.getPolicyGraph());
			prohibitions = new MemProhibitions();
			existingProposal.setPolicyGraph(GraphSerializer.toJson(pdp.getPAP().getGraphPAP()));
			existingProposal.setProhibitions(ProhibitionsSerializer.toJson(pdp.getPAP().getProhibitionsPAP()));
			for (String s : pdp.getPAP().getGraphPAP().getChildren("PI")) {
				log.info("CHILDREN OF PI: " + s);
			}
		}
		if (projectProposal.getApprovalStage().equals(Constants.STATE_RD)) {
			PDP pdp = pdsOperations.rdDisapprove(userInfo.getUserName(), existingProposal.getPolicyGraph());
			prohibitions = new MemProhibitions();
			existingProposal.setPolicyGraph(GraphSerializer.toJson(pdp.getPAP().getGraphPAP()));
			existingProposal.setProhibitions(ProhibitionsSerializer.toJson(pdp.getPAP().getProhibitionsPAP()));
			for (String s : pdp.getPAP().getGraphPAP().getChildren("PI")) {
				log.info("CHILDREN OF PI: " + s);
			}
		}
	}

	@POST
	@Path("/CheckPermissionForAProposal")
	@Produces(MediaType.APPLICATION_JSON)
	@ApiOperation(value = "Check permission for a Proposal", notes = "This API checks permission for a Proposal")
	@ApiResponses(value = { @ApiResponse(code = 200, message = "Success: { True }"),
			@ApiResponse(code = 403, message = "Failed: { \"error\":\"error description\", \"status\": \"FAIL\" }") })
	public Response checkUserPermissionForAProposal(
			@ApiParam(value = "Message", required = true, defaultValue = "", allowableValues = "", allowMultiple = false) String message) {
		try {
			log.info("ProposalService::checkUserPermissionForAProposal started");
			ObjectMapper mapper = new ObjectMapper();
			JsonNode root = mapper.readTree(message);
			log.info("Root:" + root);

			if (root != null && root.has("policyInfo")) {
				JsonNode policyInfo = root.get("policyInfo");
				if (policyInfo != null && policyInfo.isArray() && policyInfo.size() > 0) {
					HashMap<String, Multimap<String, String>> attrMap = proposalDAO.generateAttributes(policyInfo);

					log.info("Nazmul : " + attrMap);

					GPMSCommonInfo userInfo = new GPMSCommonInfo();
					if (root != null && root.has("gpmsCommonObj")) {
						JsonNode commonObj = root.get("gpmsCommonObj");
						userInfo = new GPMSCommonInfo(commonObj);
					}
					String proposalId = "";
					String proposalSection = "";
					String action = "";
					if (root != null && root.has("proposalId")) {
						JsonNode proposalIdNode = root.get("proposalId");
						proposalId = proposalIdNode.textValue();
						JsonNode proposalSectionNode = root.get("ProposalSection");
						proposalSection = proposalSectionNode.textValue();
						JsonNode proposalActionNode = root.get("Action");
						action = proposalActionNode.textValue().trim();

					}

					log.info("User Info : " + userInfo.getUserName());
					log.info("ProposalId : " + proposalId);
					log.info("Section : " + proposalSection);
					log.info("Action : " + action);
					// log.info("NGAC : "+action);

					ObjectId id = new ObjectId(proposalId);
					Proposal existingProposal = proposalDAO.findProposalByProposalID(id);

					log.info("NGAC :" + existingProposal.getNgacId());

					ProposalDataSheet projectProposal = new ProposalDataSheet();
					// projectProposal.setProposal(existingProposal);

					for (Entry<Long, Graph> mapElement : PDSOperations.proposalPolicies.entrySet()) {
						long key = (long) mapElement.getKey();
						log.info("key:" + key);

					}
					Graph proposalPolicy = null;

					setPolicyState(projectProposal, existingProposal, proposalPolicy, userInfo.getUserName(), false);
					for (String s : projectProposal.getProposalPolicy().getChildren("PI")) {
						log.info("CheckUserPERMISSION: " + s);
					}

					String decision = "";

					//// System.out.println(projectProposal.getProposal().getPolicyGraph());

					if (action.equalsIgnoreCase("Edit")) {
						String objectAtt = "";
						String acRight = "write";
						objectAtt = projectProposal.setSection(proposalSection);
						if (objectAtt.equalsIgnoreCase("Investigator Info")) {
							if (projectProposal.getPolicyDecision(pdsOperations, userInfo.getUserName(), "add-sp", "SP")
									.equals("Permit")
									|| projectProposal
											.getPolicyDecision(pdsOperations, userInfo.getUserName(), "delete-sp", "SP")
											.equals("Permit")
									|| projectProposal.getPolicyDecision(pdsOperations, userInfo.getUserName(),
											"add-copi", "CoPI").equals("Permit")
									|| projectProposal.getPolicyDecision(pdsOperations, userInfo.getUserName(),
											"delete-copi", "CoPI").equals("Permit")) {
								decision = "Permit";
							} else {
								decision = "Deny";
							}
							decision = projectProposal.getPolicyDecision(pdsOperations, userInfo.getUserName(),
									"add-sp", "SP");
							log.info("D:" + decision);
						} else {
							log.info("objectAtt WRITE :" + objectAtt);
							decision = projectProposal.getPolicyDecision(pdsOperations, userInfo.getUserName(), acRight,
									objectAtt);
							if (decision.equals("Deny")&& objectAtt.equals(Constants.SIGNATURE_INFO_OA_LBL)) {
								if (projectProposal
										.getPolicyDecision(pdsOperations, userInfo.getUserName(), acRight,
												"ChairApproval")
										.equals("Permit")
										|| projectProposal.getPolicyDecision(pdsOperations, userInfo.getUserName(),
												acRight, "BMApproval").equals("Permit")
										|| projectProposal.getPolicyDecision(pdsOperations, userInfo.getUserName(),
												acRight, "IRBApproval").equals("Permit")
										|| projectProposal.getPolicyDecision(pdsOperations, userInfo.getUserName(),
												acRight, "DeanApproval").equals("Permit")
										|| projectProposal.getPolicyDecision(pdsOperations, userInfo.getUserName(),
												acRight, "RAApproval").equals("Permit")
										|| projectProposal.getPolicyDecision(pdsOperations, userInfo.getUserName(),
												acRight, "RDApproval").equals("Permit")) {
									decision = "Permit";
								}
							}
							log.info("D:" + decision);
						}
					} else if (action.equalsIgnoreCase("Add Co-PI")) {
						String objectAtt = "CoPI";
						String acRight = "add-copi";
						decision = projectProposal.getPolicyDecision(pdsOperations, userInfo.getUserName(), acRight,
								objectAtt);
						/*
						 * Boolean hasPermission = pdsOperations.hasPermissionToAddAsCoPI(
						 * projectProposal.getProposalPolicy(), userInfo.getUserName(), "",
						 * projectProposal.getProhibitions()); if (hasPermission) decision = "Permit";
						 * else decision = "Deny";
						 */
					} else if (action.equalsIgnoreCase("Add Senior Personnel")) {
						String objectAtt = "SP";
						String acRight = "add-sp";
						decision = projectProposal.getPolicyDecision(pdsOperations, userInfo.getUserName(), acRight,
								objectAtt);
//						Boolean hasPermission = pdsOperations.hasPermissionToAddAsSP(
//								projectProposal.getProposalPolicy(), userInfo.getUserName(), "",
//								projectProposal.getProhibitions());
//						if (hasPermission)
//							decision = "Permit";
//						else
//							decision = "Deny";
					} else if (action.equalsIgnoreCase("Delete")
							&& proposalSection.equalsIgnoreCase("InvestigatorInformation.Co-PI")) {
						Boolean hasPermission = pdsOperations.hasPermissionToDeleteCoPI(
								projectProposal.getProposalPolicy(), userInfo.getUserName(),
								projectProposal.getProhibitions());
						if (hasPermission)
							decision = "Permit";
						else
							decision = "Deny";
					} else if (action.equalsIgnoreCase("Delete")
							&& proposalSection.equalsIgnoreCase("InvestigatorInformation.Senior-Personnel")) {
						Boolean hasPermission = pdsOperations.hasPermissionToDeleteSP(
								projectProposal.getProposalPolicy(), userInfo.getUserName(),
								projectProposal.getProhibitions());
						if (hasPermission)
							decision = "Permit";
						else
							decision = "Deny";

					} else if (action.equalsIgnoreCase("ViewLog")) {
						decision = projectProposal.getPolicyDecision(pdsOperations, userInfo.getUserName(), "ViewLog",
								"Audit Log");
						log.info("Decisiong for viewing log: " + decision);
					} else if (action.equalsIgnoreCase("read")) {
						decision = projectProposal.getPolicyDecision(pdsOperations, userInfo.getUserName(), "read",
								"PDSSections");
						log.info("Decisiong for viewing PDSs: " + decision);
					} else {
						BalanaConnector ac = new BalanaConnector();
						decision = ac.getXACMLdecision(attrMap);
					}
					log.info("Decision :" + decision);
					if (decision.equals("Permit")) {
						return Response.status(200).type(MediaType.APPLICATION_JSON)
								.entity(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(true)).build();
					} else {
						return Response.status(403).type(MediaType.APPLICATION_JSON)
								.entity("Your permission is: " + decision).build();
					}
				} else {
					return Response.status(403).type(MediaType.APPLICATION_JSON)
							.entity("No User Permission Attributes are send!").build();
				}
			}
		} catch (Exception e) {
			log.error("Could not Check Permission for a Proposal error e=", e);
		}
		return Response.status(403)
				.entity("{\"error\": \"No User Permission Attributes are send!\", \"status\": \"FAIL\"}").build();
	}

	@POST
	@Path("/CheckPermissionForCreateProposal")
	@Produces(MediaType.APPLICATION_JSON)
	@ApiOperation(value = "Check permission for creating a Proposal", notes = "This API checks permission for creating A Proposal")
	@ApiResponses(value = { @ApiResponse(code = 200, message = "Success: { True }"),
			@ApiResponse(code = 403, message = "Failed: { \"error\":\"error description\", \"status\": \"FAIL\" }") })
	public Response checkUserPermissionForCreateProposal(
			@ApiParam(value = "Message", required = true, defaultValue = "", allowableValues = "", allowMultiple = false) String message) {
		try {
			log.info("ProposalService::checkUserPermissionForCreateProposal started");
			ObjectMapper mapper = new ObjectMapper();
			JsonNode root = mapper.readTree(message);

			String decision = "";

			GPMSCommonInfo userInfo = null;
			if (root != null && root.has("gpmsCommonObj")) {
				JsonNode commonObj = root.get("gpmsCommonObj");
				log.info(commonObj);
				userInfo = new GPMSCommonInfo(commonObj);
				log.info("User Info:" + userInfo.toString());

			}

			if (root != null && root.has("policyInfo")) {
				JsonNode policyInfo = root.get("policyInfo");
				if (policyInfo != null && policyInfo.isArray() && policyInfo.size() > 0) {
					HashMap<String, Multimap<String, String>> attrMap = proposalDAO.generateAttributes(policyInfo);
					HashMap<String, String> attrMapPm = proposalDAO.generateAttributesForPM(policyInfo);
					log.info("Policy Info:" + policyInfo);
					log.info("Attribute Map:" + attrMap);
					log.info("Attribute Map PM:" + attrMapPm);

					// pdsOperations.testUsersAccessights_Proposal_not_created();
					boolean hasPermission = false;
					hasPermission = pdsOperations.hasPermissionToCreateAProposal(userInfo.getUserName(),
							new MemProhibitions());
					ObjectId od = new ObjectId(userInfo.getUserProfileID());
					String email = "";
					if (userProfileDAO.findUserDetailsByProfileID(od).getWorkEmails().size() > 0) {

						email = userProfileDAO.findUserDetailsByProfileID(od).getWorkEmails().get(0);
					}

					if (hasPermission) {
						long proposalId = pdsOperations.createAProposal(userInfo.getUserName(),
								userInfo.getUserDepartment(), email);

						log.info("New policy id:" + proposalId + "|"
								+ PDSOperations.proposalPolicies.get(proposalId).getNodes().size());

						// pdsOperations.testUsersAccessights_Proposal_created(PDSOperations.proposalPolicies.get(proposalId));

						return Response.status(200).type(MediaType.APPLICATION_JSON)
								.entity(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(proposalId + ""))
								.build();
					} else {
						return Response.status(403).type(MediaType.APPLICATION_JSON)
								.entity("Your permission is: " + "Deny").build();
					}

				} else {
					return Response.status(403).type(MediaType.APPLICATION_JSON)
							.entity("Your permission is: " + decision).build();
				}
			}
		} catch (Exception e) {
			log.error("Could not Check Permission for a Proposal Creation. Error e=", e);
			e.printStackTrace();
		}
		return Response.status(403)
				.entity("{\"error\": \"No User Permission Attributes are send!\", \"status\": \"FAIL\"}").build();
	}

	public void broadCastNotification(String proposalID, String projectTitle, String notificationMessage,
			String notificationType, List<SignatureUserInfo> signatures, boolean isCritical) {
		notificationDAO.notifyAdmin(proposalID, projectTitle, notificationMessage, notificationType, isCritical);
		for (SignatureUserInfo user : signatures) {
			notificationDAO.createNotificationForAUser(proposalID, projectTitle, notificationMessage, notificationType,
					isCritical, user);
		}
		// Broadcasting SSE
		Builder eventBuilder = new Builder();
		OutboundEvent event = eventBuilder.name("notification").mediaType(MediaType.TEXT_PLAIN_TYPE)
				.data(String.class, "1").build();
		// NotificationService.BROADCASTER.broadcast(event);
	}

	/***
	 * Sends Notification
	 * 
	 * @param id
	 * @param existingProposal
	 * @param authorUserName
	 * @throws ParseException
	 */
	public void sendNotification(ObjectId id, Proposal existingProposal, String authorUserName) throws ParseException {
		EmailUtil emailUtil = new EmailUtil();
		String emailSubject = new String();
		String emailBody = new String();
		String piEmail = new String();
		List<String> emaillist = new ArrayList<String>();
		emailSubject = "The proposal has been deleted by: " + authorUserName;
		emailBody = "Hello User,<br/><br/>The proposal has been deleted by Admin.<br/><br/>Thank you, <br/> GPMS Team";
		List<SignatureUserInfo> signatures = proposalDAO.findAllUsersToBeNotified(id,
				existingProposal.isIrbApprovalRequired());
		for (SignatureUserInfo signatureInfo : signatures) {
			emaillist.add(signatureInfo.getEmail());
		}
		emailUtil.sendMailMultipleUsersWithoutAuth(piEmail, emaillist, emailSubject, emailBody);

		String projectTitle = existingProposal.getProjectInfo().getProjectTitle();
		String notificationMessage = "Deleted by " + authorUserName + ".";

		broadCastNotification(existingProposal.getId().toString(), projectTitle, notificationMessage, "Proposal",
				signatures, true);
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
	public void sendDeleteNotification(String proposalRoles, String proposalUserTitle, Proposal existingProposal,
			String authorUserName, List<SignatureUserInfo> signatures) throws JsonProcessingException {
		String notificationMessage = "Deleted by " + authorUserName + ".";
		if ((proposalRoles.equals("PI") && !proposalUserTitle.equals("University Research Director"))
				|| (!proposalRoles.equals("PI") && proposalUserTitle.equals("University Research Director"))) {
			broadCastNotification(existingProposal.getId().toString(),
					existingProposal.getProjectInfo().getProjectTitle(), notificationMessage, "Proposal", signatures,
					true);
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
	public boolean notifyUsersProposalStatusUpdate(Proposal existingProposal, Proposal oldProposal,
			UserProfile authorProfile, String proposalID, String authorUserName) {
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
			notificationDAO.notifyAllExistingInvestigators(existingProposal.getId().toString(),
					existingProposal.getProjectInfo().getProjectTitle(), existingProposal, notificationMessage,
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
	public boolean notifyUsersProposalStatusUpdate(Proposal existingProposal, Proposal oldProposal,
			UserProfile authorProfile, String proposalID, List<SignatureUserInfo> signatures,
			boolean irbApprovalRequired, RequiredSignaturesInfo signByAllUsersInfo, String authorUserName,
			JsonNode root, JsonNode proposalUserTitle) {
		String notificationMessage = new String();
		boolean isCritical = false;
		final String notificationType = "Proposal";
		if (proposalUserTitle != null) {
			// For Proposal Roles : PI, Co-PI, Senior
			JsonNode proposalRoles = root.get("proposalRoles");
			List<String> currentProposalRoles = new ArrayList<String>();
			if (!proposalRoles.asText().equals("")) {
				currentProposalRoles = Arrays.asList(proposalRoles.textValue().split(", "));
			}
			JsonNode buttonType = root.get("buttonType");
			// For Proposal Status
			if (buttonType != null) {
				switch (buttonType.textValue()) {
				case "Save":
					notificationMessage = proposalDAO.updateForProposalSave(existingProposal, proposalID,
							signByAllUsersInfo, authorUserName, notificationMessage, currentProposalRoles);
					break;
				case "Submit":
					notificationMessage = notificationDAO.updateForProposalSubmit(existingProposal, proposalID,
							signatures, signByAllUsersInfo, authorUserName, proposalUserTitle, currentProposalRoles);
					break;
				case "Approve":
					notificationMessage = notificationDAO.updateForProposalApprove(existingProposal, proposalID,
							signatures, irbApprovalRequired, signByAllUsersInfo, authorUserName, proposalUserTitle,
							notificationMessage, currentProposalRoles);
					break;
				case "Disapprove":
					if (!proposalID.equals("0") && currentProposalRoles != null) {
						notificationMessage = "Disapproved" + " by " + authorUserName + ".";
						isCritical = true;
						int coPICount = existingProposal.getInvestigatorInfo().getCo_pi().size();
						if (existingProposal.getChairApproval() == ApprovalType.READYFORAPPROVAL
								&& (proposalUserTitle.textValue().equals("Department Chair")
										|| proposalUserTitle.textValue().equals("Associate Chair"))) {
							notificationDAO.notifyReturnedByChair(existingProposal, proposalID, signatures,
									notificationMessage, isCritical, notificationType, coPICount);
						} else if (existingProposal.getBusinessManagerApproval() == ApprovalType.READYFORAPPROVAL
								&& (proposalUserTitle.textValue().equals("Business Manager") || proposalUserTitle
										.textValue().equals("Department Administrative Assistant"))) {
							notificationDAO.notifyDisapprovedByBusinessManager(existingProposal, proposalID, signatures,
									notificationMessage, isCritical, notificationType, coPICount);
						} else if (existingProposal.getDeanApproval() == ApprovalType.READYFORAPPROVAL
								&& (proposalUserTitle.textValue().equals("Dean")
										|| proposalUserTitle.textValue().equals("Associate Dean"))) {
							notificationDAO.notifyReturnedByDean(existingProposal, proposalID, signatures,
									notificationMessage, isCritical, notificationType, coPICount);
						} else if (existingProposal.getIrbApproval() == ApprovalType.READYFORAPPROVAL
								&& proposalUserTitle.textValue().equals("IRB")) {
							notificationDAO.notifyDisapprovedByIRB(existingProposal, proposalID, signatures,
									notificationMessage, isCritical, notificationType, coPICount);
						} else if (existingProposal.getResearchAdministratorApproval() == ApprovalType.READYFORAPPROVAL
								&& proposalUserTitle.textValue().equals("University Research Administrator")) {
							notificationDAO.notifyDisapprovedByResearchAdmin(existingProposal, proposalID, signatures,
									irbApprovalRequired, notificationMessage, isCritical, notificationType, coPICount);
						} else if (existingProposal.getResearchDirectorApproval() == ApprovalType.READYFORAPPROVAL
								&& proposalUserTitle.textValue().equals("University Research Director")) {
							notificationDAO.notifyDisapprovedByDirector(existingProposal, proposalID, signatures,
									irbApprovalRequired, notificationMessage, isCritical, notificationType, coPICount);
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
			// log.info("Here:"+existingProposal.toString());
			proposalDAO.saveProposal(existingProposal, authorProfile);
			proposalIsChanged = true;
		}
		if (proposalIsChanged) {
			notificationDAO.notifyAllExistingInvestigators(existingProposal.getId().toString(),
					existingProposal.getProjectInfo().getProjectTitle(), existingProposal, notificationMessage,
					notificationType, isCritical);
		}
		return proposalIsChanged;
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
	 * @param requiredSignatures
	 * @param emailDetails
	 * @return
	 * @throws JsonProcessingException
	 */
	public boolean sendSaveUpdateNotification(String userName, String message, ProposalDataSheet projectProposal,
			String proposalId, Proposal existingProposal, Proposal oldProposal, UserProfile authorProfile,
			boolean isAdminUser, List<SignatureUserInfo> signatures, Boolean irbApprovalRequired,
			RequiredSignaturesInfo requiredSignatures, EmailCommonInfo emailDetails, String action)
			throws JsonProcessingException {
		ObjectMapper mapper = new ObjectMapper();
		String emailSubject = "";
		String emailBody = "";
		String authorName = "";
		String piEmail = "";
		List<String> emaillist = new ArrayList<String>();

		if (!emailDetails.getPiEmail().isEmpty()) {
			emailSubject = emailDetails.getEmailSubject();
			emailBody = emailDetails.getEmailBody();
			authorName = emailDetails.getAuthorName();
			piEmail = emailDetails.getPiEmail();

			emaillist = emailDetails.getEmaillist();
		}
		boolean proposalIsChanged = false;
		try {
			if (proposalId.equals("0")) {

				log.info("Existing Proposal : inside sendSaveUpdateNotification ");
				projectProposal.updateCoPI(userName, false);
				projectProposal.updateSP(userName, false);

				proposalIsChanged = saveProposal(message, existingProposal, null, authorProfile, isAdminUser,
						proposalId, null, irbApprovalRequired, null, action);
			} else {
				proposalIsChanged = saveProposal(message, existingProposal, oldProposal, authorProfile, isAdminUser,
						proposalId, signatures, irbApprovalRequired, requiredSignatures, action);
			}
		} catch (Exception e) {
//			return Response.status(403).type(MediaType.APPLICATION_JSON)
//					.entity(e.getMessage()).build();
			e.printStackTrace();
			return false;
		}
		if (proposalIsChanged) {

			log.info("After Save:");
			if (projectProposal != null) {
				projectProposal.setProposal(existingProposal);
				try {
					if (!projectProposal.isProposalSubmitted()) {
						projectProposal.clearIngestigator();
						projectProposal.updatePI(false);
						try {
							projectProposal.updateCoPI(userName, false);
							projectProposal.updateSP(userName, false);

						} catch (Exception e) {
							e.printStackTrace();
							return false;
						}

						// projectProposal.updateCoPI(userName,false);
						// projectProposal.updateSP(userName,false);
					}
//					if(projectProposal.isProposalSubmitted()) {
//						projectProposal.generatePostSubmissionProhibitions();
//						projectProposal.updatePostSubmissionchanges();
//					}

				} catch (PMException e) {
					log.info("Exception : inside sendSaveUpdateNotification");
					e.printStackTrace();
					return false;
				}
			} else {
				log.info("Inside sendSaveUpdateNotification: projectProposal is NULL ");
			}
			if (!emailSubject.equals("")) {
				EmailUtil emailUtil = new EmailUtil();
				emailUtil.sendMailMultipleUsersWithoutAuth(piEmail, emaillist, emailSubject + authorName, emailBody);
			}
//			return Response
//					.status(200)
//					.type(MediaType.APPLICATION_JSON)
//					.entity(mapper.writerWithDefaultPrettyPrinter()
//							.writeValueAsString(true)).build();
			return true;
		} else {
//			return Response
//					.status(200)
//					.type(MediaType.APPLICATION_JSON)
//					.entity(mapper.writerWithDefaultPrettyPrinter()
//							.writeValueAsString(true)).build();
			return true;
		}
	}

	public boolean saveProposal(String message, Proposal existingProposal, Proposal oldProposal,
			UserProfile authorProfile, boolean isAdminUser, String proposalID, List<SignatureUserInfo> signatures,
			boolean irbApprovalRequired, RequiredSignaturesInfo requiredSignatures, String action)
			throws UnknownHostException, Exception, ParseException, IOException, JsonParseException,
			JsonMappingException {
		String authorUserName = authorProfile.getFullName();
		// log.info("Hello:"+message);
		ObjectMapper mapper = new ObjectMapper();
		JsonNode root = mapper.readTree(message);
		JsonNode proposalInfo = null;
		boolean proposalIsChanged = false;
		if (root != null && root.has("proposalInfo")) {
			proposalInfo = root.get("proposalInfo");
			proposalDAO.getProjectInfo(existingProposal, proposalID, proposalInfo);
			proposalDAO.getSponsorAndBudgetInfo(existingProposal, proposalID, proposalInfo);
			proposalDAO.getCostShareInfo(existingProposal, proposalID, proposalInfo);
			proposalDAO.getUniversityCommitments(existingProposal, proposalID, proposalInfo);
			proposalDAO.getConflictOfInterest(existingProposal, proposalID, proposalInfo);
			proposalDAO.getAdditionalInfo(existingProposal, proposalID, proposalInfo);
			proposalDAO.getCollaborationInfo(existingProposal, proposalID, proposalInfo);
			proposalDAO.getConfidentialInfo(existingProposal, proposalID, proposalInfo);
			if (!isAdminUser) {
				// OSP Section
				JsonNode proposalUserTitle = root.get("proposalUserTitle");
				if ((proposalUserTitle.textValue().equals("University Research Administrator")
						|| proposalUserTitle.textValue().equals("University Research Director"))
						&& !proposalID.equals("0") && !action.equals("Disapprove")) {
					proposalDAO.getOSPSectionInfo(existingProposal, proposalInfo);
				} else {
					if (!proposalID.equals("0")) {
						existingProposal.setOspSectionInfo(oldProposal.getOspSectionInfo());
					}
				}
				proposalIsChanged = notifyUsersProposalStatusUpdate(existingProposal, oldProposal, authorProfile,
						proposalID, signatures, irbApprovalRequired, requiredSignatures, authorUserName, root,
						proposalUserTitle);
			} else {
				proposalIsChanged = notifyUsersProposalStatusUpdate(existingProposal, oldProposal, authorProfile,
						proposalID, authorUserName);
			}
		}
		return proposalIsChanged;
	}

	public void getInvestigatorInfoDetails(String proposalId, Proposal existingProposal, Proposal oldProposal,
			JsonNode proposalInfo) {
		InvestigatorInfo addedInvestigators = new InvestigatorInfo();
		InvestigatorInfo deletedInvestigators = new InvestigatorInfo();
		if (proposalInfo != null && proposalInfo.has("InvestigatorInfo")) {
			if (!proposalId.equals("0")) {
				existingProposal.getInvestigatorInfo().getCo_pi().clear();
				existingProposal.getInvestigatorInfo().getSeniorPersonnel().clear();
			}
			String[] rows = proposalInfo.get("InvestigatorInfo").textValue().split("#!#");
			InvestigatorInfo newInvestigatorInfo = new InvestigatorInfo();
			for (String col : rows) {
				String[] cols = col.split("!#!");
				InvestigatorRefAndPosition investigatorRefAndPosition = new InvestigatorRefAndPosition();
				ObjectId id = new ObjectId(cols[1]);
				UserProfile userRef = userProfileDAO.findUserDetailsByProfileID(id);
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
						if (!existingProposal.getInvestigatorInfo().getCo_pi().contains(investigatorRefAndPosition)) {
							existingProposal.getInvestigatorInfo().getCo_pi().add(investigatorRefAndPosition);

							if (!addedInvestigators.getCo_pi().contains(investigatorRefAndPosition)) {
								addedInvestigators.getCo_pi().add(investigatorRefAndPosition);
							}
						}
					} else {
						newInvestigatorInfo.getCo_pi().add(investigatorRefAndPosition);
					}
					break;
				case "2":
					if (!proposalId.equals("0")) {
						if (!existingProposal.getInvestigatorInfo().getSeniorPersonnel()
								.contains(investigatorRefAndPosition)) {
							existingProposal.getInvestigatorInfo().getSeniorPersonnel().add(investigatorRefAndPosition);
							if (!addedInvestigators.getSeniorPersonnel().contains(investigatorRefAndPosition)) {
								addedInvestigators.getSeniorPersonnel().add(investigatorRefAndPosition);
							}
						}
					} else {
						newInvestigatorInfo.getSeniorPersonnel().add(investigatorRefAndPosition);
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
				proposalDAO.updateInvestigatorList(existingProposal, oldProposal, addedInvestigators,
						deletedInvestigators);
			}
		}
	}
}