//package gpms.rest;
//
//import gpms.DAL.MongoDBConnector;
//import gpms.dao.DelegationDAO;
//import gpms.dao.NotificationDAO;
//import gpms.dao.ProposalDAO;
//import gpms.dao.UserAccountDAO;
//import gpms.dao.UserProfileDAO;
//import gpms.model.GPMSCommonInfo;
//import gpms.model.NotificationLog;
//import gpms.model.UserAccount;
//import gpms.model.UserProfile;
//import io.swagger.annotations.Api;
//import io.swagger.annotations.ApiOperation;
//import io.swagger.annotations.ApiParam;
//import io.swagger.annotations.ApiResponse;
//import io.swagger.annotations.ApiResponses;
//
//import java.io.IOException;
//import java.text.DateFormat;
//import java.text.ParseException;
//import java.text.SimpleDateFormat;
//import java.util.List;
//
//import javax.inject.Singleton;
//import javax.servlet.ServletException;
//import javax.servlet.http.HttpServletRequest;
//import javax.servlet.http.HttpServletResponse;
//import javax.servlet.http.HttpSession;
//import javax.ws.rs.Consumes;
//import javax.ws.rs.GET;
//import javax.ws.rs.POST;
//import javax.ws.rs.Path;
//import javax.ws.rs.Produces;
//import javax.ws.rs.core.Context;
//import javax.ws.rs.core.MediaType;
//import javax.ws.rs.core.Response;
//
//import org.apache.log4j.Logger;
//import org.glassfish.jersey.media.sse.EventOutput;
//import org.glassfish.jersey.media.sse.OutboundEvent;
//import org.glassfish.jersey.media.sse.SseBroadcaster;
//import org.glassfish.jersey.media.sse.SseFeature;
//import org.mongodb.morphia.Morphia;
//
//import com.fasterxml.jackson.core.JsonProcessingException;
//import com.fasterxml.jackson.databind.JsonNode;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.mongodb.MongoClient;
//
//@Singleton
//@Path("/notifications")
//@Api(value = "/notifications", description = "Manage Notifications")
//@Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
//@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
//public class NotificationService {
//	MongoClient mongoClient = null;
//	Morphia morphia = null;
//	String dbName = "db_gpms";
//	UserAccountDAO userAccountDAO = null;
//	UserProfileDAO userProfileDAO = null;
//	ProposalDAO proposalDAO = null;
//	DelegationDAO delegationDAO = null;
//	NotificationDAO notificationDAO = null;
//	DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
//	private static final Logger log = Logger
//			.getLogger(NotificationService.class.getName());
//	public static final SseBroadcaster BROADCASTER = new SseBroadcaster();
//
//	public NotificationService() {
//		mongoClient = MongoDBConnector.getMongo();
//		morphia = new Morphia();
//		morphia.map(UserProfile.class).map(UserAccount.class);
//		userAccountDAO = new UserAccountDAO(mongoClient, morphia, dbName);
//		userProfileDAO = new UserProfileDAO(mongoClient, morphia, dbName);
//		proposalDAO = new ProposalDAO(mongoClient, morphia, dbName);
//		delegationDAO = new DelegationDAO(mongoClient, morphia, dbName);
//		notificationDAO = new NotificationDAO(mongoClient, morphia, dbName);
//	}
//
//	@POST
//	@Path("/NotificationGetAllCount")
//	@ApiOperation(value = "Get all Notifications Count", notes = "This API gets all Notifications count")
//	@ApiResponses(value = {
//			@ApiResponse(code = 200, message = "Success: { Notifications Count }"),
//			@ApiResponse(code = 400, message = "Failed: { \"error\":\"error description\", \"status\": \"FAIL\" }") })
//	public Response notificationGetAllCountForAUser(
//			@ApiParam(value = "Message", required = true, defaultValue = "", allowableValues = "", allowMultiple = false) String message) {
//		try {
//			log.info("NotificationService::notificationGetAllCountForAUser started");
//			ObjectMapper mapper = new ObjectMapper();
//			JsonNode root = mapper.readTree(message);
//			GPMSCommonInfo userInfo = new GPMSCommonInfo();
//			if (root != null && root.has("gpmsCommonObj")) {
//				JsonNode commonObj = root.get("gpmsCommonObj");
//				userInfo = new GPMSCommonInfo(commonObj);
//			}
//			return Response
//					.status(Response.Status.OK)
//					.entity(Long.toString(notificationDAO
//							.findAllNotificationCountForAUser(userInfo))).build();
//		} catch (Exception e) {
//			log.error("Could not find Notifications count error e=", e);
//		}
//		return Response
//				.status(Response.Status.BAD_REQUEST)
//				.entity("{\"error\": \"Could Not Find Notifications Count\", \"status\": \"FAIL\"}")
//				.build();
//	}
//
//	@GET
//	@Path("/NotificationGetRealTimeCount")
//	@Produces(SseFeature.SERVER_SENT_EVENTS)
//	@ApiOperation(value = "Get all Realtime Notifications", notes = "This API gets all realtime Notifications")
//	@ApiResponses(value = { @ApiResponse(code = 200, message = "Success: { EventOutput }") })
//	public EventOutput notificationGetRealTimeCountForAUser(
//			@Context HttpServletRequest request,
//			@Context HttpServletResponse response)
//			throws JsonProcessingException, IOException, ParseException,
//			ServletException {
//		log.info("NotificationService::notificationGetRealTimeCountForAUser started");
//		if (request == null) {
//			throw new ServletException("Request can't be null or empty");
//		}
//		EventOutput eventOutput = new EventOutput();
//		HttpSession session = request.getSession();
//		GPMSCommonInfo userInfo = userProfileDAO
//				.bindUserInfoFromSession(session);
//		long notificationCount = notificationDAO
//				.findAllNotificationCountForAUser(userInfo);
//		OutboundEvent.Builder eventBuilder = new OutboundEvent.Builder();
//		eventBuilder.name("notification");
//		eventBuilder.data(String.class, Long.toString(notificationCount));
//		OutboundEvent event = eventBuilder.build();
//		eventOutput.write(event);
//		BROADCASTER.add(eventOutput);
//		return eventOutput;
//	}
//
//	@POST
//	@Path("/NotificationGetAll")
//	@ApiOperation(value = "Get all notifications", notes = "This API gets all notifications")
//	@ApiResponses(value = {
//			@ApiResponse(code = 200, message = "Success: { Notification Log }"),
//			@ApiResponse(code = 400, message = "Failed: { \"error\":\"error description\", \"status\": \"FAIL\" }") })
//	public Response notificationGetAllForAUser(
//			@ApiParam(value = "Message", required = true, defaultValue = "", allowableValues = "", allowMultiple = false) String message) {
//		try {
//			log.info("NotificationService::notificationGetAllForAUser started");
//			ObjectMapper mapper = new ObjectMapper();
//			JsonNode root = mapper.readTree(message);
//			GPMSCommonInfo userInfo = new GPMSCommonInfo();
//			if (root != null && root.has("gpmsCommonObj")) {
//				JsonNode commonObj = root.get("gpmsCommonObj");
//				userInfo = new GPMSCommonInfo(commonObj);
//			}
//			List<NotificationLog> notifications = notificationDAO
//					.findAllNotificationInfoForAUser(userInfo);
//			return Response
//					.status(Response.Status.OK)
//					.entity(mapper.writerWithDefaultPrettyPrinter()
//							.writeValueAsString(notifications)).build();
//		} catch (Exception e) {
//			log.error("Could not find all Notifications error e=", e);
//		}
//		return Response
//				.status(Response.Status.BAD_REQUEST)
//				.entity("{\"error\": \"Could Not Find All Notifications\", \"status\": \"FAIL\"}")
//				.build();
//	}
//}
