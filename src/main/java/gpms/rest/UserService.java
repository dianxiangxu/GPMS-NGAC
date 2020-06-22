package gpms.rest;

import gpms.DAL.DepartmentsPositionsCollection;
import gpms.DAL.MongoDBConnector;
import gpms.dao.NotificationDAO;
import gpms.dao.ProposalDAO;
import gpms.dao.UserAccountDAO;
import gpms.dao.UserProfileDAO;
import gpms.model.AuditLogCommonInfo;
import gpms.model.AuditLogInfo;
import gpms.model.GPMSCommonInfo;
import gpms.model.NotificationLog;
import gpms.model.PositionDetails;
import gpms.model.UserAccount;
import gpms.model.UserInfo;
import gpms.model.UserProfile;
import gpms.model.UserProposalCount;
import gpms.ngac.policy.NGACPolicyConfigurationLoader;
import gpms.ngac.policy.PDSOperations;
import gpms.utils.MultimapAdapter;
import gpms.utils.PasswordHash;
import gpms.utils.SerializationHelper;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

import java.io.File;
import java.net.URISyntaxException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.log4j.Logger;
import org.bson.types.ObjectId;
import org.glassfish.jersey.media.sse.OutboundEvent;
import org.mongodb.morphia.Morphia;

import com.ebay.xcelite.Xcelite;
import com.ebay.xcelite.sheet.XceliteSheet;
import com.ebay.xcelite.writer.SheetWriter;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Multimap;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mongodb.MongoClient;

@Path("/users")
@Api(value = "/users", description = "Manage Users")
@Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML,
		MediaType.APPLICATION_FORM_URLENCODED, MediaType.TEXT_PLAIN,
		MediaType.TEXT_HTML })
@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML,
		MediaType.TEXT_PLAIN, MediaType.TEXT_HTML })
public class UserService {
	MongoClient mongoClient = null;
	Morphia morphia = null;
	String dbName = "db_gpms";
	UserAccountDAO userAccountDAO = null;
	UserProfileDAO userProfileDAO = null;
	ProposalDAO proposalDAO = null;
	NotificationDAO notificationDAO = null;
	private DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
	private NGACPolicyConfigurationLoader nGACPolicyLoader;
	//private TaskConfigurationParser taskConfigurationParser;
	//private UserTaskPermissionRepo userTaskPermissionRepo;
	private PDSOperations pdsOperations;
	private static final Logger log = Logger.getLogger(UserService.class
			.getName());

	public UserService() {
		mongoClient = MongoDBConnector.getMongo();
		morphia = new Morphia();
		morphia.map(UserProfile.class).map(UserAccount.class);
		userAccountDAO = new UserAccountDAO(mongoClient, morphia, dbName);
		userProfileDAO = new UserProfileDAO(mongoClient, morphia, dbName);
		proposalDAO = new ProposalDAO(mongoClient, morphia, dbName);
		notificationDAO = new NotificationDAO(mongoClient, morphia, dbName);
		nGACPolicyLoader = new NGACPolicyConfigurationLoader();
		nGACPolicyLoader.init();
		pdsOperations = new PDSOperations();
		DepartmentsPositionsCollection.init();
	}

	@GET
	@Produces(MediaType.TEXT_PLAIN)
	@ApiOperation(value = "Test User Service", notes = "This API tests whether the service is working or not")
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "Success: { Hello World! }"),
			@ApiResponse(code = 400, message = "Failed: { \"error\":\"error description\", \"status\": \"FAIL\" }") })
	public Response testService() {
		try {
			log.info("UserService::testService started");
			return Response.status(Response.Status.OK).entity("Hello World!")
					.build();
		} catch (Exception e) {
			log.error("Could not connect the User Service error e=", e);
		}
		return Response
				.status(Response.Status.BAD_REQUEST)
				.entity("{\"error\": \"Could Not Find User Service\", \"status\": \"FAIL\"}")
				.build();
	}

	@POST
	@Path("/GetUserPositionDetailsForAProposal")
	@ApiOperation(value = "Get all User Position Details for a Proposal", notes = "This API gets all Investigator User Position Details of a Proposal")
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "Success: { Investigator Users And Positions }"),
			@ApiResponse(code = 400, message = "Failed: { \"error\":\"error description\", \"status\": \"FAIL\" }") })
	public Response getUserPositionDetailsForAProposal(
			@ApiParam(value = "Message", required = true, defaultValue = "", allowableValues = "", allowMultiple = false) String message) {
		try {
			log.info("UserService::getUserPositionDetailsForAProposal started");
			String profileIds = new String();
			String profiles[] = new String[0];
			List<ObjectId> userIds = new ArrayList<ObjectId>();
			ObjectMapper mapper = new ObjectMapper();
			JsonNode root = mapper.readTree(message);
			if (root != null && root.has("userIds")) {
				profileIds = root.get("userIds").textValue();
				profiles = profileIds.split(", ");
			}
			for (String profile : profiles) {
				ObjectId id = new ObjectId(profile);
				userIds.add(id);
			}
			final MultimapAdapter multimapAdapter = new MultimapAdapter();
			final Gson gson = new GsonBuilder().setPrettyPrinting()
					.registerTypeAdapter(Multimap.class, multimapAdapter)
					.create();
			return Response
					.status(Response.Status.OK)
					.entity(gson.toJson(userProfileDAO
							.findUserPositionDetailsForAProposal(userIds)))
					.build();
		} catch (Exception e) {
			log.error(
					"Could not find Investigator User Details for the proposal error e=",
					e);
		}
		return Response
				.status(Response.Status.BAD_REQUEST)
				.entity("{\"error\": \"Could Not Find Investigator User Details for the proposal\", \"status\": \"FAIL\"}")
				.build();
	}

	@POST
	@Path("/GetUsersList")
	@ApiOperation(value = "Get all Users", notes = "This API gets all active Users")
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "Success: { User Info }"),
			@ApiResponse(code = 400, message = "Failed: { \"error\":\"error description\", \"status\": \"FAIL\" }") })
	public Response produceUsersJSON(
			@ApiParam(value = "Message", required = true, defaultValue = "", allowableValues = "", allowMultiple = false) String message) {
		try {
			log.info("UserService::produceUsersJSON started");
			List<UserInfo> users = new ArrayList<UserInfo>();
			int offset = 0, limit = 0;
			ObjectMapper mapper = new ObjectMapper();
			JsonNode root = mapper.readTree(message);
			if (root != null && root.has("offset")) {
				offset = root.get("offset").intValue();
			}
			if (root != null && root.has("limit")) {
				limit = root.get("limit").intValue();
			}
			GPMSCommonInfo userInfo = new GPMSCommonInfo();
			if (root != null && root.has("userBindObj")) {
				JsonNode userObj = root.get("userBindObj");
				userInfo = GPMSCommonInfo.getUserBindInfo(userObj);
			}
			users = userProfileDAO.findAllUsersForGrid(offset, limit, userInfo);
			return Response
					.status(Response.Status.OK)
					.entity(mapper.writerWithDefaultPrettyPrinter()
							.writeValueAsString(users)).build();
		} catch (Exception e) {
			log.error("Could not find all Users error e=", e);
		}
		return Response
				.status(Response.Status.BAD_REQUEST)
				.entity("{\"error\": \"Could Not Find All Users\", \"status\": \"FAIL\"}")
				.build();
	}

	@POST
	@Path("/GetAdminUsersList")
	@ApiOperation(value = "Get Admin Users", notes = "This API gets all admin Users")
	public Response produceAdminUsersJSON(
			@ApiParam(value = "Message", required = true, defaultValue = "", allowableValues = "", allowMultiple = false) String message) {
		try {
			log.info("UserService::produceAdminUsersJSON started");
			List<UserInfo> users = new ArrayList<UserInfo>();
			int offset = 0, limit = 0;
			ObjectMapper mapper = new ObjectMapper();
			JsonNode root = mapper.readTree(message);
			if (root != null && root.has("offset")) {
				offset = root.get("offset").intValue();
			}
			if (root != null && root.has("limit")) {
				limit = root.get("limit").intValue();
			}
			GPMSCommonInfo userInfo = new GPMSCommonInfo();
			if (root != null && root.has("userBindObj")) {
				JsonNode userObj = root.get("userBindObj");
				userInfo = GPMSCommonInfo.getUserBindInfo(userObj);
			}
			users = userProfileDAO.findAllForAdminUserGrid(offset, limit,
					userInfo);
			return Response
					.status(Response.Status.OK)
					.entity(mapper.writerWithDefaultPrettyPrinter()
							.writeValueAsString(users)).build();
		} catch (Exception e) {
			log.error("Could not find all Admin Users error e=", e);
		}
		return Response
				.status(Response.Status.BAD_REQUEST)
				.entity("{\"error\": \"Could Not Find All Admin Users\", \"status\": \"FAIL\"}")
				.build();
	}

	@POST
	@Path("/UsersExportToExcel")
	@Produces(MediaType.TEXT_HTML)
	@ApiOperation(value = "Export all Users in a grid", notes = "This API exports all Users shown in a grid")
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "Success: { Excel Filename/ No Record}"),
			@ApiResponse(code = 400, message = "Failed: { \"error\":\"error description\", \"status\": \"FAIL\" }") })
	public Response exportUsersJSON(
			@ApiParam(value = "Message", required = true, defaultValue = "", allowableValues = "", allowMultiple = false) String message) {
		try {
			log.info("UserService::exportUsersJSON started");
			List<UserInfo> users = new ArrayList<UserInfo>();
			ObjectMapper mapper = new ObjectMapper();
			JsonNode root = mapper.readTree(message);
			GPMSCommonInfo userInfo = new GPMSCommonInfo();
			if (root != null && root.has("userBindObj")) {
				JsonNode userObj = root.get("userBindObj");
				userInfo = GPMSCommonInfo.getUserBindInfo(userObj);
			}
			users = userProfileDAO.findAllUsers(userInfo);
			String filename = new String();
			if (users.size() > 0) {
				filename = exportToExcelFile(users);
			} else {
				filename = mapper.writerWithDefaultPrettyPrinter()
						.writeValueAsString("No Record");
			}
			return Response.status(Response.Status.OK).entity(filename).build();
		} catch (Exception e) {
			log.error("Could not export User list error e=", e);
		}
		return Response
				.status(Response.Status.BAD_REQUEST)
				.entity("{\"error\": \"Could Not Export User List\", \"status\": \"FAIL\"}")
				.build();
	}

	@POST
	@Path("/AdminUsersExportToExcel")
	@Produces(MediaType.TEXT_HTML)
	@ApiOperation(value = "Export all Admin Users in a grid", notes = "This API exports all Admin Users shown in a grid")
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "Success: { Excel Filename/ No Record}"),
			@ApiResponse(code = 400, message = "Failed: { \"error\":\"error description\", \"status\": \"FAIL\" }") })
	public Response exportAdminUsersJSON(
			@ApiParam(value = "Message", required = true, defaultValue = "", allowableValues = "", allowMultiple = false) String message) {
		try {
			log.info("UserService::exportAdminUsersJSON started");
			List<UserInfo> users = new ArrayList<UserInfo>();
			ObjectMapper mapper = new ObjectMapper();
			JsonNode root = mapper.readTree(message);
			GPMSCommonInfo userInfo = new GPMSCommonInfo();
			if (root != null && root.has("userBindObj")) {
				JsonNode userObj = root.get("userBindObj");
				userInfo = GPMSCommonInfo.getUserBindInfo(userObj);
			}
			users = userProfileDAO.findAllAdminUsers(userInfo);
			String filename = new String();
			if (users.size() > 0) {
				exportToExcelFile(users);
			} else {
				filename = mapper.writerWithDefaultPrettyPrinter()
						.writeValueAsString("No Record");
			}
			return Response.status(Response.Status.OK).entity(filename).build();
		} catch (Exception e) {
			log.error("Could not export Admin User list error e=", e);
		}
		return Response
				.status(Response.Status.BAD_REQUEST)
				.entity("{\"error\": \"Could Not Export Admin User List\", \"status\": \"FAIL\"}")
				.build();
	}

	/**
	 * Exports to Excel File for Users and Admin Users
	 * 
	 * @param users
	 * @return
	 * @throws URISyntaxException
	 * @throws JsonProcessingException
	 */
	private String exportToExcelFile(List<UserInfo> users)
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

	@POST
	@Path("/GetUserDetailsByProfileId")
	@ApiOperation(value = "Get User Details By ProfileId", notes = "This API gets User Detail Information by ProfileId")
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "Success: { User Profile }"),
			@ApiResponse(code = 400, message = "Failed: { \"error\":\"error description\", \"status\": \"FAIL\" }") })
	public Response produceUserDetailsByProfileId(
			@ApiParam(value = "Message", required = true, defaultValue = "", allowableValues = "", allowMultiple = false) String message) {
		try {
			log.info("UserService::produceUserDetailsByProfileId started");
			UserProfile user = new UserProfile();
			String profileId = new String();
			ObjectMapper mapper = new ObjectMapper();
			JsonNode root = mapper.readTree(message);
			if (root != null && root.has("userId")) {
				profileId = root.get("userId").textValue();
			}
			if (profileId != null) {
				ObjectId id = new ObjectId(profileId);
				user = userProfileDAO.findUserDetailsByProfileID(id);
				return Response
						.status(Response.Status.OK)
						.entity(mapper.setDateFormat(formatter)
								.writerWithDefaultPrettyPrinter()
								.writeValueAsString(user)).build();
			}
		} catch (Exception e) {
			log.error(
					"Could not get User Detail Information by ProfileId error e=",
					e);
		}
		return Response
				.status(Response.Status.BAD_REQUEST)
				.entity("{\"error\": \"Could Not Get User Details By ProfileId\", \"status\": \"FAIL\"}")
				.build();
	}

	@POST
	@Path("/GetUserInfoByProfileId")
	@ApiOperation(value = "Get User Information By ProfileId", notes = "This API gets User Information by ProfileId")
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "Success: { User Profile }"),
			@ApiResponse(code = 400, message = "Failed: { \"error\":\"error description\", \"status\": \"FAIL\" }") })
	public Response produceUserInfoByProfileId(
			@ApiParam(value = "Message", required = true, defaultValue = "", allowableValues = "", allowMultiple = false) String message) {
		try {
			log.info("UserService::produceUserInfoByProfileId started");
			UserProfile user = new UserProfile();
			String profileId = new String();
			ObjectMapper mapper = new ObjectMapper();
			JsonNode root = mapper.readTree(message);
			if (root != null && root.has("userId")) {
				profileId = root.get("userId").textValue();
			}
			ObjectId id = new ObjectId(profileId);
			user = userProfileDAO.findUserInfoByProfileID(id);
			return Response
					.status(Response.Status.OK)
					.entity(mapper.writerWithDefaultPrettyPrinter()
							.writeValueAsString(
									user.getUserAccount().getPassword()))
					.build();
		} catch (Exception e) {
			log.error("Could not get User Information By ProfileId error e=", e);
		}
		return Response
				.status(Response.Status.BAD_REQUEST)
				.entity("{\"error\": \"Could Not Get User Information By ProfileId\", \"status\": \"FAIL\"}")
				.build();
	}

	@POST
	@Path("/GetUserAuditLogList")
	@ApiOperation(value = "Get User Audit Log Information", notes = "This API gets Audit Log information for a User")
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "Success: { Audit Log Info }"),
			@ApiResponse(code = 400, message = "Failed: { \"error\":\"error description\", \"status\": \"FAIL\" }") })
	public Response produceUserAuditLogJSON(
			@ApiParam(value = "Message", required = true, defaultValue = "", allowableValues = "", allowMultiple = false) String message) {
		try {
			log.info("UserService::produceUserAuditLogJSON started");
			List<AuditLogInfo> userAuditLogs = new ArrayList<AuditLogInfo>();
			int offset = 0, limit = 0;
			String profileId = new String();
			ObjectMapper mapper = new ObjectMapper();
			JsonNode root = mapper.readTree(message);
			if (root != null && root.has("offset")) {
				offset = root.get("offset").intValue();
			}
			if (root != null && root.has("limit")) {
				limit = root.get("limit").intValue();
			}
			if (root != null && root.has("userId")) {
				profileId = root.get("userId").textValue();
			}
			AuditLogCommonInfo auditLogInfo = new AuditLogCommonInfo();
			if (root != null && root.has("auditLogBindObj")) {
				JsonNode auditLogBindObj = root.get("auditLogBindObj");
				auditLogInfo = new AuditLogCommonInfo(auditLogBindObj);
			}
			ObjectId userId = new ObjectId(profileId);
			userAuditLogs = userProfileDAO.findAllProposalAuditLogForGrid(
					offset, limit, userId, auditLogInfo);
			return Response
					.status(Response.Status.OK)
					.entity(mapper.writerWithDefaultPrettyPrinter()
							.writeValueAsString(userAuditLogs)).build();
		} catch (Exception e) {
			log.error("Could not get User Audit Log information error e=", e);
		}
		return Response
				.status(Response.Status.BAD_REQUEST)
				.entity("{\"error\": \"Could Not Get User Audit Log Information\", \"status\": \"FAIL\"}")
				.build();
	}

	@POST
	@Path("/GetPositionDetailsHash")
	@ApiOperation(value = "Get All Available User Position Details as Database", notes = "This API gets all Available User Position Details as Database stored in DepartmentsPositionsCollection class")
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "Success: { Audit Log Info }"),
			@ApiResponse(code = 400, message = "Failed: { \"error\":\"error description\", \"status\": \"FAIL\" }") })
	public Response producePositionDetailsHash() {
		try {
			log.info("UserService::producePositionDetailsHash started");
			ObjectMapper mapper = new ObjectMapper();
			DepartmentsPositionsCollection dpc = new DepartmentsPositionsCollection();
			return Response
					.status(Response.Status.OK)
					.entity(mapper.writerWithDefaultPrettyPrinter()
							.writeValueAsString(
									dpc.getAvailableDepartmentsAndPositions()))
					.build();
		} catch (Exception e) {
			log.error("Could not load all User Position Details error e=", e);
		}
		return Response
				.status(Response.Status.BAD_REQUEST)
				.entity("{\"error\": \"Could Not Load All User Position Details\", \"status\": \"FAIL\"}")
				.build();
	}

	@POST
	@Path("/DeleteUserByUserID")
	@ApiOperation(value = "Delete User by UserID", notes = "This API deletes User by UserID")
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "Success: { True }"),
			@ApiResponse(code = 400, message = "Failed: { \"error\":\"error description\", \"status\": \"FAIL\" }") })
	public Response deleteUserByUserID(
			@ApiParam(value = "Message", required = true, defaultValue = "", allowableValues = "", allowMultiple = false) String message) {
		try {
			log.info("UserService::deleteUserByUserID started");
			String profileId = new String();
			ObjectMapper mapper = new ObjectMapper();
			JsonNode root = mapper.readTree(message);
			if (root != null && root.has("userId")) {
				profileId = root.get("userId").textValue();
			}
			GPMSCommonInfo userInfo = new GPMSCommonInfo();
			if (root != null && root.has("gpmsCommonObj")) {
				JsonNode commonObj = root.get("gpmsCommonObj");
				userInfo = new GPMSCommonInfo(commonObj);
			}
			ObjectId authorId = new ObjectId(userInfo.getUserProfileID());
			UserProfile authorProfile = userProfileDAO
					.findUserDetailsByProfileID(authorId);
			ObjectId id = new ObjectId(profileId);
			UserProfile userProfile = userProfileDAO
					.findUserDetailsByProfileID(id);
			userProfileDAO
					.deleteUserProfileByUserID(userProfile, authorProfile);
			UserAccount userAccount = userAccountDAO.findByID(userProfile
					.getUserAccount().getId());
			userAccount.setDeleted(true);
			userAccount.setActive(false);
			userAccountDAO.save(userAccount);
			notificationDAO.sendNotification(userProfile, userAccount);
			OutboundEvent.Builder eventBuilder = new OutboundEvent.Builder();
			OutboundEvent event = eventBuilder.name("notification")
					.mediaType(MediaType.TEXT_PLAIN_TYPE)
					.data(String.class, "1").build();
		//	NotificationService.BROADCASTER.broadcast(event);
			return Response
					.status(Response.Status.OK)
					.entity(mapper.writerWithDefaultPrettyPrinter()
							.writeValueAsString(true)).build();
		} catch (Exception e) {
			log.error("Could not delete User by UserID error e=", e);
		}
		return Response
				.status(Response.Status.BAD_REQUEST)
				.entity("{\"error\": \"Could Not Delete User by UserID\", \"status\": \"FAIL\"}")
				.build();
	}

	@POST
	@Path("/DeleteMultipleUsersByUserID")
	@ApiOperation(value = "Delete Multiple Users at once", notes = "This API deletes Multiple Users at once")
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "Success: { True }"),
			@ApiResponse(code = 400, message = "Failed: { \"error\":\"error description\", \"status\": \"FAIL\" }") })
	public Response deleteMultipleUsersByUserID(
			@ApiParam(value = "Message", required = true, defaultValue = "", allowableValues = "", allowMultiple = false) String message) {
		try {
			log.info("UserService::deleteMultipleUsersByUserID started");
			String profileIds = new String();
			String profiles[] = new String[0];
			ObjectMapper mapper = new ObjectMapper();
			JsonNode root = mapper.readTree(message);
			if (root != null && root.has("userIds")) {
				profileIds = root.get("userIds").textValue();
				profiles = profileIds.split(",");
			}
			GPMSCommonInfo userInfo = new GPMSCommonInfo();
			if (root != null && root.has("gpmsCommonObj")) {
				JsonNode commonObj = root.get("gpmsCommonObj");
				userInfo = new GPMSCommonInfo(commonObj);
			}
			ObjectId authorId = new ObjectId(userInfo.getUserProfileID());
			UserProfile authorProfile = userProfileDAO
					.findUserDetailsByProfileID(authorId);
			for (String profile : profiles) {
				ObjectId id = new ObjectId(profile);
				UserProfile userProfile = userProfileDAO
						.findUserDetailsByProfileID(id);
				userProfileDAO.deleteUserProfileByUserID(userProfile,
						authorProfile);
				UserAccount userAccount = userAccountDAO.findByID(userProfile
						.getUserAccount().getId());
				userAccount.setDeleted(true);
				userAccount.setActive(false);
				userAccountDAO.save(userAccount);
				notificationDAO.sendNotification(userProfile, userAccount);
			}
			OutboundEvent.Builder eventBuilder = new OutboundEvent.Builder();
			OutboundEvent event = eventBuilder.name("notification")
					.mediaType(MediaType.TEXT_PLAIN_TYPE)
					.data(String.class, "1").build();
			//NotificationService.BROADCASTER.broadcast(event);
			return Response
					.status(Response.Status.OK)
					.entity(mapper.writerWithDefaultPrettyPrinter()
							.writeValueAsString(true)).build();
		} catch (Exception e) {
			log.error("Could not delete all selected Users error e=", e);
		}
		return Response
				.status(Response.Status.BAD_REQUEST)
				.entity("{\"error\": \"Could Not Delete All Selected Users\", \"status\": \"FAIL\"}")
				.build();
	}

	@POST
	@Path("/UpdateUserIsActiveByUserID")
	@ApiOperation(value = "Update User's IsActive By UserID", notes = "This API updates User's IsActive field By UserID")
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "Success: { True }"),
			@ApiResponse(code = 400, message = "Failed: { \"error\":\"error description\", \"status\": \"FAIL\" }") })
	public Response updateUserIsActiveByUserID(
			@ApiParam(value = "Message", required = true, defaultValue = "", allowableValues = "", allowMultiple = false) String message) {
		try {
			log.info("UserService::updateUserIsActiveByUserID started");
			String profileId = new String();
			Boolean isActive = true;
			ObjectMapper mapper = new ObjectMapper();
			JsonNode root = mapper.readTree(message);
			if (root != null && root.has("userId")) {
				profileId = root.get("userId").textValue();
			}
			if (root != null && root.has("isActive")) {
				isActive = root.get("isActive").booleanValue();
			}
			GPMSCommonInfo userInfo = new GPMSCommonInfo();
			if (root != null && root.has("gpmsCommonObj")) {
				JsonNode commonObj = root.get("gpmsCommonObj");
				userInfo = new GPMSCommonInfo(commonObj);
			}
			ObjectId authorId = new ObjectId(userInfo.getUserProfileID());
			UserProfile authorProfile = userProfileDAO
					.findUserDetailsByProfileID(authorId);
			ObjectId id = new ObjectId(profileId);
			UserProfile userProfile = userProfileDAO
					.findUserDetailsByProfileID(id);
			userProfileDAO.activateUserProfileByUserID(userProfile,
					authorProfile, isActive);
			UserAccount userAccount = userProfile.getUserAccount();
			userAccount.setDeleted(!isActive);
			userAccount.setActive(isActive);
			userAccountDAO.save(userAccount);
			notificationDAO
					.sendNotification(isActive, userProfile, userAccount);
			return Response
					.status(Response.Status.OK)
					.entity(mapper.writerWithDefaultPrettyPrinter()
							.writeValueAsString(true)).build();
		} catch (Exception e) {
			log.error("Could not update User's IsActive field error e=", e);
		}
		return Response
				.status(Response.Status.BAD_REQUEST)
				.entity("{\"error\": \"Could Not Update User's IsActive Field\", \"status\": \"FAIL\"}")
				.build();
	}

	@POST
	@Path("/CheckUniqueUserName")
	@ApiOperation(value = "Check for Unique Username", notes = "This API checks if provided Username is Unique or not")
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "Success: { True/ False }"),
			@ApiResponse(code = 400, message = "Failed: { \"error\":\"error description\", \"status\": \"FAIL\" }") })
	public Response checkUniqueUserName(
			@ApiParam(value = "Message", required = true, defaultValue = "", allowableValues = "", allowMultiple = false) String message) {
		try {
			log.info("UserService::checkUniqueUserName started");
			String userID = new String();
			String newUserName = new String();
			String response = new String();
			ObjectMapper mapper = new ObjectMapper();
			JsonNode root = mapper.readTree(message);
			if (root != null && root.has("userUniqueObj")) {
				JsonNode userUniqueObj = root.get("userUniqueObj");
				if (userUniqueObj != null && userUniqueObj.has("UserID")) {
					userID = userUniqueObj.get("UserID").textValue();
				}
				if (userUniqueObj != null && userUniqueObj.has("NewUserName")) {
					newUserName = userUniqueObj.get("NewUserName").textValue();
				}
			}
			ObjectId id = new ObjectId();
			UserProfile userProfile = new UserProfile();
			if (!userID.equals("0")) {
				id = new ObjectId(userID);
				userProfile = userProfileDAO.findNextUserWithSameUserName(id,
						newUserName);
			} else {
				userProfile = userProfileDAO
						.findAnyUserWithSameUserName(newUserName);
			}
			if (userProfile != null) {
				response = mapper.writerWithDefaultPrettyPrinter()
						.writeValueAsString("false");
			} else {
				response = mapper.writerWithDefaultPrettyPrinter()
						.writeValueAsString("true");
			}
			return Response.status(Response.Status.OK).entity(response).build();
		} catch (Exception e) {
			log.error("Could not check for unique Username error e=", e);
		}
		return Response
				.status(Response.Status.BAD_REQUEST)
				.entity("{\"error\": \"Could Not Check For Unique Username\", \"status\": \"FAIL\"}")
				.build();
	}
	
	
	@POST
	@Path("/SavePolicy")
	@ApiOperation(value = "Save policy", notes = "Saves the current ABAC Policy")
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "Success: { True }"),
			@ApiResponse(code = 400, message = "Failed: { \"error\":\"error description\", \"status\": \"FAIL\" }") })
	public Response savePolicy(
			@ApiParam(value = "Message", required = true, defaultValue = "", allowableValues = "", allowMultiple = false) String message) {
		try {
			log.info("UserService::SavePolicy started");
			String profileId = new String();
			ObjectMapper mapper = new ObjectMapper();
			JsonNode root = mapper.readTree(message);
			/*
			 * if (root != null && root.has("userId")) { profileId =
			 * root.get("userId").textValue(); }
			 */
			GPMSCommonInfo userInfo = new GPMSCommonInfo();
			if (root != null && root.has("gpmsCommonObj")) {
				JsonNode commonObj = root.get("gpmsCommonObj");
				userInfo = new GPMSCommonInfo(commonObj);
			}
			
			//nGACPolicyLoader.savePolicy(null);
			
			return Response
					.status(Response.Status.OK)
					.entity(mapper.writerWithDefaultPrettyPrinter()
							.writeValueAsString(true)).build();
		} catch (Exception e) {
			log.error("Could not save the policy error e=", e);
		}
		return Response
				.status(Response.Status.BAD_REQUEST)
				.entity("{\"error\": \"Could Not Save the policy\", \"status\": \"FAIL\"}")
				.build();
	}
	

	@POST
	@Path("/CheckUniqueEmail")
	@ApiOperation(value = "Check for Unique Email Address", notes = "This API checks for unique Email Address")
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "Success: { True/ False }"),
			@ApiResponse(code = 400, message = "Failed: { \"error\":\"error description\", \"status\": \"FAIL\" }") })
	public Response checkUniqueEmail(
			@ApiParam(value = "Message", required = true, defaultValue = "", allowableValues = "", allowMultiple = false) String message) {
		try {
			log.info("UserService::checkUniqueEmail started");
			String userID = new String();
			String newEmail = new String();
			String response = new String();
			ObjectMapper mapper = new ObjectMapper();
			JsonNode root = mapper.readTree(message);
			if (root != null && root.has("userUniqueObj")) {
				JsonNode userUniqueObj = root.get("userUniqueObj");
				if (userUniqueObj != null && userUniqueObj.has("UserID")) {
					userID = userUniqueObj.get("UserID").textValue();
				}
				if (userUniqueObj != null && userUniqueObj.has("NewEmail")) {
					newEmail = userUniqueObj.get("NewEmail").textValue();
				}
			}
			ObjectId id = new ObjectId();
			UserProfile userProfile = new UserProfile();
			if (!userID.equals("0")) {
				id = new ObjectId(userID);
				userProfile = userProfileDAO.findNextUserWithSameEmail(id,
						newEmail);
			} else {
				userProfile = userProfileDAO.findAnyUserWithSameEmail(newEmail);
			}
			if (userProfile != null) {
				response = mapper.writerWithDefaultPrettyPrinter()
						.writeValueAsString("false");
			} else {
				response = mapper.writerWithDefaultPrettyPrinter()
						.writeValueAsString("true");
			}
			return Response.status(Response.Status.OK).entity(response).build();
		} catch (Exception e) {
			log.error("Could not check Unique Email Address error e=", e);
		}
		return Response
				.status(Response.Status.BAD_REQUEST)
				.entity("{\"error\": \"Could Not Check for Unique Email Address\", \"status\": \"FAIL\"}")
				.build();
	}

	@POST
	@Path("/signup")
	@ApiOperation(value = "Registering a New user", notes = "This API signups a new user")
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "Success: { True }"),
			@ApiResponse(code = 400, message = "Failed: { \"error\":\"error description\", \"status\": \"FAIL\" }") })
	public Response signUpUser(
			@ApiParam(value = "Message", required = true, defaultValue = "", allowableValues = "", allowMultiple = false) String message) {
		try {
			log.info("UserService::signUpUser started");
			String userID = new String();
			UserAccount newAccount = new UserAccount();
			UserProfile newProfile = new UserProfile();
			ObjectMapper mapper = new ObjectMapper();
			JsonNode root = mapper.readTree(message);
			if (root != null && root.has("userInfo")) {
				JsonNode userInfo = root.get("userInfo");
				if (userInfo != null && userInfo.has("UserID")) {
					userID = userInfo.get("UserID").textValue();
				}
				if (userID.equals("0")) {
					userProfileDAO.bindUserInfo(newAccount, newProfile,
							userInfo);
				}
				userAccountDAO.save(newAccount);
				userProfileDAO.signUpUser(newProfile);
				NotificationLog notification = new NotificationLog();
				notification.setType("User");
				notification.setAction("Signed up.");
				notification.setUserProfileId(newAccount.getId().toString());
				notification.setUsername(newAccount.getUserName());
				notification.setForAdmin(true);
				notificationDAO.save(notification);
			}
			OutboundEvent.Builder eventBuilder = new OutboundEvent.Builder();
			OutboundEvent event = eventBuilder.name("notification")
					.mediaType(MediaType.TEXT_PLAIN_TYPE)
					.data(String.class, "1").build();
		//	NotificationService.BROADCASTER.broadcast(event);
			return Response
					.status(Response.Status.OK)
					.entity(mapper.writerWithDefaultPrettyPrinter()
							.writeValueAsString(true)).build();

		} catch (Exception e) {
			log.error("Could not register a new user error e=", e);
		}
		return Response
				.status(Response.Status.BAD_REQUEST)
				.entity("{\"error\": \"Could Not Register A New User\", \"status\": \"FAIL\"}")
				.build();
	}

	@POST
	@Path("/login")
	@ApiOperation(value = "login user with valid username and password", notes = "This API allows to login a valid user with authorized username and password"
			+ "<p><u>Form Parameters</u><ul><li><b>username</b> is required</li><li><b>password</b> is required</li></ul>")
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "Success: { Redirect to Dashboard page }"),
			@ApiResponse(code = 400, message = "Failed: { \"error\":\"error description\", \"status\": \"FAIL\" }") })
	public Response login(
			@ApiParam(value = "username", required = true, defaultValue = "test", allowableValues = "", allowMultiple = false) @FormParam("username") String email,
			@ApiParam(value = "password", required = true, defaultValue = "password", allowableValues = "", allowMultiple = false) @FormParam("password") String password,
			@Context HttpServletRequest req) {
		try {
			if (req == null) {
				return Response
						.status(Response.Status.BAD_REQUEST)
						.entity("{\"error\": \"Could Not Find The User\", \"status\": \"FAIL\"}")
						.build();
			}
			List<UserProfile> userList = userProfileDAO.findAll();
			boolean isFound = false;
			if (userList.size() != 0) {
				for (UserProfile user : userList) {
					if (user.getUserAccount().getUserName().equals(email)
							|| user.getWorkEmails().contains(email)) {
						if (PasswordHash.validatePassword(password, user
								.getUserAccount().getPassword())
								&& !user.isDeleted()
								&& user.getUserAccount().isActive()
								&& !user.getUserAccount().isDeleted()) {
							isFound = true;

							userProfileDAO.setMySessionID(req, user.getId()
									.toString());
							//the following two line for initiating user permitted task list generation
							//UserTaskPermissionOperations.init();
							//UserTaskPermissionOperations.populateUsersApprovedTaskSet(user
							//			.getUserAccount().getUserName());
							
							java.net.URI location = new java.net.URI(
									"../Home.jsp");
							if (user.getUserAccount().isAdmin()) {
								location = new java.net.URI("../Dashboard.jsp");
							}
							return Response.seeOther(location).build();
						} else {
							isFound = false;
						}
					}
				}
			} else {
				isFound = false;
			}
			if (!isFound) {
				java.net.URI location = new java.net.URI(
						"../Login.jsp?msg=error");
				return Response.seeOther(location).build();
			}
		} catch (Exception e) {
			log.error("Could not find the User error e=", e);
		}
		return Response
				.status(Response.Status.BAD_REQUEST)
				.entity("{\"error\": \"Could Not Find The User\", \"status\": \"FAIL\"}")
				.build();
	}

	@POST
	@Path("/SetUserViewSession")
	@ApiOperation(value = "Set User Session based on selected position detail", notes = "This API sets the selected position detail as a session")
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "Success: { True }"),
			@ApiResponse(code = 400, message = "Failed: { \"error\":\"error description\", \"status\": \"FAIL\" }") })
	public Response setUserViewSession(
			@Context HttpServletRequest req,
			@ApiParam(value = "Message", required = true, defaultValue = "", allowableValues = "", allowMultiple = false) String message) {
		try {
			log.info("UserService::setUserViewSession started");
			userProfileDAO.deleteAllSession(req);
			ObjectMapper mapper = new ObjectMapper();
			JsonNode root = mapper.readTree(message);
			if (root != null && root.has("userId") && root.has("userName")
					&& root.has("isAdminUser")) {
				String profileId = root.get("userId").textValue();
				ObjectId id = new ObjectId(profileId);
				String userName = new String();
				Boolean isAdminUser = false;
				String college = new String();
				String department = new String();
				String positionType = new String();
				String positionTitle = new String();
				userName = root.get("userName").textValue();
				isAdminUser = Boolean.parseBoolean(root.get("isAdminUser")
						.textValue());
				if (root != null && root.has("college")) {
					college = root.get("college").textValue();
				}
				if (root != null && root.has("department")) {
					department = root.get("department").textValue();
				}
				if (root != null && root.has("positionType")) {
					positionType = root.get("positionType").textValue();
				}
				if (root != null && root.has("positionTitle")) {
					positionTitle = root.get("positionTitle").textValue();
				}
				UserProfile user = userProfileDAO.findMatchedUserDetails(id,
						userName, isAdminUser, college, department,
						positionType, positionTitle);
				if (user != null) {
					userProfileDAO.setUserCurrentSession(req, userName,
							isAdminUser, profileId, college, department,
							positionType, positionTitle);
				}
				return Response
						.status(Response.Status.OK)
						.entity(mapper.writerWithDefaultPrettyPrinter()
								.writeValueAsString(true)).build();
			}
		} catch (Exception e) {
			log.error(
					"Could not set User Session based on selected position detail error e=",
					e);
		}
		return Response
				.status(Response.Status.BAD_REQUEST)
				.entity("{\"error\": \"Could Not Set User Session Based On Selected Position Detail\", \"status\": \"FAIL\"}")
				.build();
	}

	@GET
	@Path("/logout")
	@ApiOperation(value = "Logout the User", notes = "This API logouts the user")
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "Success: { Redirect to Login page }"),
			@ApiResponse(code = 400, message = "Failed: { \"error\":\"error description\", \"status\": \"FAIL\" }") })
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	public Response logout(@Context HttpServletRequest req) {
		try {
			log.info("UserService::logout started");
			if (req == null) {
				log.error("Null request in context");
				return Response
						.status(Response.Status.BAD_REQUEST)
						.entity("{\"error\": \"Could Not Logout the User\", \"status\": \"FAIL\"}")
						.build();
			}
			userProfileDAO.deleteAllSession(req);
			return Response.seeOther(new java.net.URI("../Login.jsp")).build();
		} catch (Exception e) {
			log.error("Could not logout the user error e=", e);
		}
		return Response
				.status(Response.Status.BAD_REQUEST)
				.entity("{\"error\": \"Could Not Logout the User\", \"status\": \"FAIL\"}")
				.build();
	}

	@POST
	@Path("/GetAllUserDropdown")
	@ApiOperation(value = "Get All Users", notes = "This API gets all active Users to bind in dropdowns")
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "Success: { User Info }"),
			@ApiResponse(code = 400, message = "Failed: { \"error\":\"error description\", \"status\": \"FAIL\" }") })
	public Response getAllUsers() {
		try {
			log.info("UserService::getAllUsers started");
			HashMap<String, String> users = new HashMap<String, String>();
			List<UserProfile> userprofiles = userProfileDAO
					.findAllUsersWithPosition("CoPIEligible");
			for (UserProfile userProfile : userprofiles) {
				users.put(userProfile.getId().toString(),
						userProfile.getFullName());
			}
			ObjectMapper mapper = new ObjectMapper();
			return Response
					.status(Response.Status.OK)
					.entity(mapper.writerWithDefaultPrettyPrinter()
							.writeValueAsString(users)).build();
		} catch (Exception e) {
			log.error("Could not get all Users error e=", e);
		}
		return Response
				.status(Response.Status.BAD_REQUEST)
				.entity("{\"error\": \"Could Not Get All Users\", \"status\": \"FAIL\"}")
				.build();
	}
	@POST
	@Path("/GetAllSPUserDropdown")
	@ApiOperation(value = "Get All Users", notes = "This API gets all active Users to bind in dropdowns")
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "Success: { User Info }"),
			@ApiResponse(code = 400, message = "Failed: { \"error\":\"error description\", \"status\": \"FAIL\" }") })
	public Response getAllSPUsers() {
		try {
			log.info("UserService::getAllUsers started");
			HashMap<String, String> users = new HashMap<String, String>();
			List<UserProfile> userprofiles = userProfileDAO
					.findAllUsersWithPosition("SPEligible");
			for (UserProfile userProfile : userprofiles) {
				users.put(userProfile.getId().toString(),
						userProfile.getFullName());
			}
			ObjectMapper mapper = new ObjectMapper();
			return Response
					.status(Response.Status.OK)
					.entity(mapper.writerWithDefaultPrettyPrinter()
							.writeValueAsString(users)).build();
		} catch (Exception e) {
			log.error("Could not get all Users error e=", e);
		}
		return Response
				.status(Response.Status.BAD_REQUEST)
				.entity("{\"error\": \"Could Not Get All Users\", \"status\": \"FAIL\"}")
				.build();
	}

	@POST
	@Path("/GetCurrentPositionDetailsForPI")
	@ApiOperation(value = "Get Current Position Details For PI", notes = "This API gets current Position Details for PI")
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "Success: { Investigator Users And Positions }"),
			@ApiResponse(code = 400, message = "Failed: { \"error\":\"error description\", \"status\": \"FAIL\" }") })
	public Response getCurrentPositionDetailsForPI(
			@ApiParam(value = "Message", required = true, defaultValue = "", allowableValues = "", allowMultiple = false) String message) {
		try {
			log.info("UserService::getCurrentPositionDetailsForPI started");
			ObjectMapper mapper = new ObjectMapper();
			JsonNode root = mapper.readTree(message);
			GPMSCommonInfo userInfo = new GPMSCommonInfo();
			if (root != null && root.has("gpmsCommonObj")) {
				JsonNode commonObj = root.get("gpmsCommonObj");
				userInfo = new GPMSCommonInfo(commonObj);
			}
			ObjectId id = new ObjectId(userInfo.getUserProfileID());
			final MultimapAdapter multimapAdapter = new MultimapAdapter();
			final Gson gson = new GsonBuilder().setPrettyPrinting()
					.registerTypeAdapter(Multimap.class, multimapAdapter)
					.create();
			return Response
					.status(Response.Status.OK)
					.entity(gson.toJson(userProfileDAO
							.findCurrentPositionDetailsForPI(id, userInfo)))
					.build();
		} catch (Exception e) {
			log.error("Could not current Position Details for PI error e=", e);
		}
		return Response
				.status(Response.Status.BAD_REQUEST)
				.entity("{\"error\": \"Could Not Current Position Details For PI\", \"status\": \"FAIL\"}")
				.build();
	}

	@POST
	@Path("/GetAllPositionDetailsForAUser")
	@ApiOperation(value = "Get All Position Details For A User", notes = "This API gets all Position Details for a User")
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "Success: { User Info }"),
			@ApiResponse(code = 400, message = "Failed: { \"error\":\"error description\", \"status\": \"FAIL\" }") })
	public Response getAllPositionDetailsForAUser(
			@ApiParam(value = "Message", required = true, defaultValue = "", allowableValues = "", allowMultiple = false) String message) {
		try {
			log.info("UserService::getAllPositionDetailsForAUser started");
			String userId = new String();
			ObjectMapper mapper = new ObjectMapper();
			JsonNode root = mapper.readTree(message);
			if (root != null && root.has("userId")) {
				userId = root.get("userId").textValue();
			}
			ObjectId id = new ObjectId(userId);
			final MultimapAdapter multimapAdapter = new MultimapAdapter();
			final Gson gson = new GsonBuilder().setPrettyPrinting()
					.registerTypeAdapter(Multimap.class, multimapAdapter)
					.create();
			return Response
					.status(Response.Status.OK)
					.entity(gson.toJson(userProfileDAO
							.findAllPositionDetailsForAUser(id))).build();
		} catch (Exception e) {
			log.error("Could not get all Position Details for a User error e=",
					e);
		}
		return Response
				.status(Response.Status.BAD_REQUEST)
				.entity("{\"error\": \"Could Not Get All Position Details For A User\", \"status\": \"FAIL\"}")
				.build();
	}

	@POST
	@Path("/GetAllProposalCountForAUser")
	@ApiOperation(value = "Get All Proposal Count For A User", notes = "This API gets all proposal count for a User")
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "Success: { User Info }"),
			@ApiResponse(code = 400, message = "Failed: { \"error\":\"error description\", \"status\": \"FAIL\" }") })
	public Response getAllProposalCountForAUser(
			@ApiParam(value = "Message", required = true, defaultValue = "", allowableValues = "", allowMultiple = false) String message) {
		try {
			log.info("UserService::getAllProposalCountForAUser started");
			ObjectMapper mapper = new ObjectMapper();
			JsonNode root = mapper.readTree(message);
			GPMSCommonInfo userInfo = new GPMSCommonInfo();
			if (root != null && root.has("gpmsCommonObj")) {
				JsonNode commonObj = root.get("gpmsCommonObj");
				userInfo = new GPMSCommonInfo(commonObj);
			}
			UserProposalCount count = userProfileDAO
					.getUserProposalCounts(userInfo);
			System.out.println("COUNT: "+count.getTotalProposalCount());
			return Response
					.status(Response.Status.OK)
					.entity(mapper.writerWithDefaultPrettyPrinter()
							.writeValueAsString(count)).build();
		} catch (Exception e) {
			log.error("Could not get all proposal count for a User error e=", e);
		}
		return Response
				.status(Response.Status.BAD_REQUEST)
				.entity("{\"error\": \"Could Not Get All Proposal Count For A User\", \"status\": \"FAIL\"}")
				.build();
	}

	@POST
	@Path("/SaveUpdateUser")
	@ApiOperation(value = "Save a New User or Update an existing User", notes = "This API saves a New User or updates an existing User")
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "Success: { True }"),
			@ApiResponse(code = 400, message = "Failed: { \"error\":\"error description\", \"status\": \"FAIL\" }") })
	public Response saveUpdateUser(
			@ApiParam(value = "Message", required = true, defaultValue = "", allowableValues = "", allowMultiple = false) String message) {
		try {
			log.info("UserService::saveUpdateUser started");
			String userID = new String();
			UserAccount newAccount = new UserAccount();
			UserProfile newProfile = new UserProfile();
			UserAccount existingUserAccount = new UserAccount();
			UserProfile existingUserProfile = new UserProfile();
			UserProfile oldUserProfile = new UserProfile();
			ObjectMapper mapper = new ObjectMapper();
			JsonNode root = mapper.readTree(message);
			boolean isActiveUser = false;
			if (root != null && root.has("userInfo")) {
				JsonNode userInfo = root.get("userInfo");
				if (userInfo != null && userInfo.has("UserID")) {
					userID = userInfo.get("UserID").textValue();
					if (!userID.equals("0")) {
						ObjectId id = new ObjectId(userID);
						existingUserProfile = userProfileDAO
								.findUserDetailsByProfileID(id);
						oldUserProfile = SerializationHelper
								.cloneThroughSerialize(existingUserProfile);
					} else {
						newAccount.setAddedOn(new Date());
					}
				}
				existingUserAccount = userProfileDAO.addUserLoginDetails(
						userID, newAccount, existingUserAccount,
						existingUserProfile, userInfo);
				isActiveUser = userProfileDAO.addUserActiveStatus(userID,
						newAccount, newProfile, existingUserAccount,
						existingUserProfile, isActiveUser, userInfo);
				if (userID.equals("0")) {
					newProfile.setUserAccount(newAccount);
				}
				userProfileDAO.addGeneralUserInfo(userID, newProfile,
						existingUserProfile, userInfo);
				userProfileDAO.addAddressDetails(userID, newProfile,
						existingUserProfile, userInfo);
				userProfileDAO.addPhoneNumberDetails(userID, newProfile,
						existingUserProfile, userInfo);
				userProfileDAO.addEmailDetails(userID, newProfile,
						existingUserProfile, userInfo);
				if (userInfo != null && userInfo.has("SaveOptions")) {
					userProfileDAO.addUserPositionDetails(userID, newProfile,
							existingUserProfile, userInfo);
				} else if (userInfo != null && userInfo.has("positionTitle")) {
					userProfileDAO.addAdminUserDetails(userID, newProfile,
							existingUserProfile, userInfo);
				}
			}
			GPMSCommonInfo userInfo = new GPMSCommonInfo();
			if (root != null && root.has("gpmsCommonObj")) {
				JsonNode commonObj = root.get("gpmsCommonObj");
				userInfo = new GPMSCommonInfo(commonObj);
			}
			ObjectId authorId = new ObjectId(userInfo.getUserProfileID());
			UserProfile authorProfile = userProfileDAO
					.findUserDetailsByProfileID(authorId);
			sendNotification(userID, newAccount, newProfile,
					existingUserAccount, existingUserProfile, oldUserProfile,
					isActiveUser, authorProfile);
			return Response
					.status(Response.Status.OK)
					.entity(mapper.writerWithDefaultPrettyPrinter()
							.writeValueAsString(true)).build();
		} catch (Exception e) {
			log.error(
					"Could not save a New User or update an existing User error e=",
					e);
		}
		return Response
				.status(Response.Status.BAD_REQUEST)
				.entity("{\"error\": \"Could Not Save A New User OR Update AN Existing User\", \"status\": \"FAIL\"}")
				.build();
	}

	/**
	 * Sends Notification For User Save and Update
	 * 
	 * @param userID
	 * @param newAccount
	 * @param newProfile
	 * @param existingUserAccount
	 * @param existingUserProfile
	 * @param oldUserProfile
	 * @param isActiveUser
	 * @param authorProfile
	 */
	private void sendNotification(String userID, UserAccount newAccount,
			UserProfile newProfile, UserAccount existingUserAccount,
			UserProfile existingUserProfile, UserProfile oldUserProfile,
			boolean isActiveUser, UserProfile authorProfile) {
		if (!userID.equals("0")) {
			if (!oldUserProfile.equals(existingUserProfile)) {
				if (!oldUserProfile.getUserAccount()
						.equals(existingUserAccount)) {
					userAccountDAO.save(existingUserAccount);
				}
				userProfileDAO.updateUser(existingUserProfile, authorProfile);
				String action = new String();
				if (isActiveUser) {
					action = "Account is activated.";
				} else {
					action = "Account is updated.";
				}
				notificationDAO.notifyAdmin(existingUserProfile, "User",
						action, true);
				for (PositionDetails positions : existingUserProfile
						.getDetails()) {
					notificationDAO.notifyInvestigators(existingUserProfile,
							"User", "Account is updated.", false, positions);
				}
			}
		} else {
			userAccountDAO.save(newAccount);
			userProfileDAO.saveUser(newProfile, authorProfile);
			notificationDAO.notifyAdmin(newProfile, "User",
					"Account is created.", true);
			for (PositionDetails positions : newProfile.getDetails()) {
				notificationDAO.notifyInvestigators(newProfile, "User",
						"Account is created.", false, positions);
			}
		}
		OutboundEvent.Builder eventBuilder = new OutboundEvent.Builder();
		OutboundEvent event = eventBuilder.name("notification")
				.mediaType(MediaType.TEXT_PLAIN_TYPE).data(String.class, "1")
				.build();
		//NotificationService.BROADCASTER.broadcast(event);
	}
}
