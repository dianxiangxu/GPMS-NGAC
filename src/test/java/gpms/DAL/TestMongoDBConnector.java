package gpms.DAL;

import com.mongodb.MongoClient;

import junit.framework.TestCase;

/***
 * Tests the MongoDB Connection is successfully created
 * 
 * @author milsonmunakami
 *
 */
public class TestMongoDBConnector extends TestCase {

	@Override
	protected void setUp() throws Exception {
		super.setUp();
	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
	}

	public void testGetMongoDBInstance() {
		MongoClient mongoClient = MongoDBConnector.getMongo();
		assertNotNull(mongoClient);
	}
}
