package gpms.DAL;

import org.apache.log4j.Logger;

import com.mongodb.DB;
import com.mongodb.Mongo;
import com.mongodb.MongoClient;
import com.mongodb.MongoException;

/***
 * MongoDBConnector providing the database connection.
 * 
 * @author milsonmunakami
 *
 */
public class MongoDBConnector {
	private static final Logger logger = Logger
			.getLogger(MongoDBConnector.class.getName());

	public static final String DB_NAME = "db_gpms";

	private static final String host = "localhost";
	private static final int port = 27017;

	private static MongoClient mongo = null;

	static Mongo connection = null;
	static DB db = null;

	/***
	 * Creates MongoDB Client
	 * 
	 * @return
	 */
	public static MongoClient getMongo() {
		if (mongo == null) {
			try {
				mongo = new MongoClient(host, port);
				logger.debug("New MongoDB Connection created with [" + host
						+ "] and [" + port + "]");
			} catch (MongoException e) {
				logger.error(e.getMessage());
			}
		}
		return mongo;
	}

}
