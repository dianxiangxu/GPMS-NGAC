package gpms.dao;

import gpms.DAL.MongoDBConnector;
import gpms.model.UserAccount;

import java.net.UnknownHostException;

import org.bson.types.ObjectId;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.Morphia;
import org.mongodb.morphia.dao.BasicDAO;

import com.mongodb.MongoClient;
import com.mongodb.MongoException;

public class UserAccountDAO extends BasicDAO<UserAccount, String> {
	private static final String DBNAME = "db_gpms";
	public static final String COLLECTION_NAME = "useraccount";

	private static Morphia morphia;
	private static Datastore ds;

	// private AuditLog audit = new AuditLog();

	private static Morphia getMorphia() throws UnknownHostException,
			MongoException {
		if (morphia == null) {
			morphia = new Morphia().map(UserAccount.class);
		}
		return morphia;
	}

	@Override
	public Datastore getDatastore() {
		if (ds == null) {
			try {
				ds = getMorphia().createDatastore(MongoDBConnector.getMongo(),
						DBNAME);
				ds.ensureIndexes();
			} catch (UnknownHostException | MongoException e) {
				e.printStackTrace();
			}
		}
		return ds;
	}

	public UserAccountDAO(MongoClient mongo, Morphia morphia, String dbName) {
		super(mongo, morphia, dbName);
	}

	public UserAccount findByID(ObjectId id) {
		Datastore ds = getDatastore();
		return ds.createQuery(UserAccount.class).field("_id").equal(id).get();
	}

	/**
	 * This method will return a user account object that matches the username
	 * searched for
	 * 
	 * @param userName
	 *            the user account name to search for
	 * @return the username if it exists or null if it does not
	 */
	public UserAccount findByUserName(String userName) {
		Datastore ds = getDatastore();
		return ds.createQuery(UserAccount.class).field("username")
				.equal(userName).get();
	}

}