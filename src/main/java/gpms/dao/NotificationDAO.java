package gpms.dao;

import gpms.DAL.MongoDBConnector;
import gpms.model.GPMSCommonInfo;
import gpms.model.NotificationLog;
import gpms.model.UserAccount;
import gpms.model.UserProfile;

import java.net.UnknownHostException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.Morphia;
import org.mongodb.morphia.dao.BasicDAO;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;

import com.mongodb.MongoClient;
import com.mongodb.MongoException;

public class NotificationDAO extends BasicDAO<NotificationLog, String> {
	private static final String DBNAME = "db_gpms";
	public static final String COLLECTION_NAME = "notification";
	private static Morphia morphia;
	private static Datastore ds;
	DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");

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
	}

	public long findAllNotificationCountAUser(GPMSCommonInfo userInfo)
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

	public List<NotificationLog> findAllNotificationForAUser(
			GPMSCommonInfo userInfo) throws ParseException {
		Datastore ds = getDatastore();
		int offset = 1;
		int limit = 10;
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
			notifications = getNotificationsForAdmin(offset, limit, ds,
					notificationQuery, removeNotifyQuery);
		} else {
			notifications = getNotificationsForUser(offset, limit, userInfo,
					ds, notificationQuery, removeNotifyQuery);
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
	private List<NotificationLog> getNotificationsForUser(int offset,
			int limit, GPMSCommonInfo userInfo, Datastore ds,
			Query<NotificationLog> notificationQuery,
			Query<NotificationLog> removeNotifyQuery) {
		List<NotificationLog> notifications = new ArrayList<NotificationLog>();
		String userProfileId = userInfo.getUserProfileID();
		String userCollege = userInfo.getUserCollege();
		String userDepartment = userInfo.getUserDepartment();
		String userPositionType = userInfo.getUserPositionType();
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
						userProfileId),
				notificationQuery.criteria("college").equal(userCollege),
				notificationQuery.criteria("department").equal(userDepartment),
				notificationQuery.criteria("position type").equal(
						userPositionType),
				notificationQuery.criteria("position title").equal(
						userPositionTitle));
		removeNotifyQuery.and(
				removeNotifyQuery.criteria("for admin").equal(false),
				removeNotifyQuery.criteria("viewed by user").equal(false),
				removeNotifyQuery.criteria("user profile id").equal(
						userProfileId),
				removeNotifyQuery.criteria("college").equal(userCollege),
				removeNotifyQuery.criteria("department").equal(userDepartment),
				removeNotifyQuery.criteria("position type").equal(
						userPositionType),
				removeNotifyQuery.criteria("position title").equal(
						userPositionTitle));
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
	private List<NotificationLog> getNotificationsForAdmin(int offset,
			int limit, Datastore ds, Query<NotificationLog> notificationQuery,
			Query<NotificationLog> removeNotifyQuery) {
		List<NotificationLog> notifications;
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

}
