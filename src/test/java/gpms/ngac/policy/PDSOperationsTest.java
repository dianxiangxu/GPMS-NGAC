package gpms.ngac.policy;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import javax.validation.constraints.AssertTrue;

import org.junit.Before;
import org.junit.Test;

import gpms.model.GPMSCommonInfo;


public class PDSOperationsTest {

	NGACPolicyConfigurationLoader loader;
	PDSOperations pdsOperations;
	GPMSCommonInfo userInfo;
	
	@Before
    public void setUp() throws Exception {
		loader = new NGACPolicyConfigurationLoader();
		loader.init();
		pdsOperations = new PDSOperations(loader.getPolicy());
		
		//User Info:GPMSCommonInfo [userProfileID=5cddc20d2edd2f0d3c61c120, userName=nazmul, 
		//userIsAdmin=false, userCollege=Engineering, userDepartment=Computer Science, 
		//userPositionType=Tenured/tenure-track faculty, userPositionTitle=Assistant Professor,
		//userIsActive=null]
		userInfo = new GPMSCommonInfo();
		userInfo.setUserIsActive(true);
		userInfo.setUserName("nazmul");
		userInfo.setUserPositionType("Tenured/tenure-track faculty");
		userInfo.setUserPositionTitle("Assistant Professor");
		userInfo.setUserIsAdmin(false);
		userInfo.setUserCollege("Engineering");
		userInfo.setUserDepartment("Computer Science");
	}
	
	@Test
    public void test1() {
       			
		assertTrue(pdsOperations.hasPermissionToCreateAProposal(userInfo));
	}
	
	@Test
    public void test2() {
       		
		userInfo.setUserName("12345");
		assertFalse(pdsOperations.hasPermissionToCreateAProposal(userInfo));
	}
}
