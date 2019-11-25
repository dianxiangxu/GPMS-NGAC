package gpms.ngac.policy;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

public class NGACPolicyConfigurationLoaderTest {
	
	NGACPolicyConfigurationLoader loader;
	Exception ex;
	
	@Before
    public void setUp() throws Exception {
		loader = new  NGACPolicyConfigurationLoader();
		loader.init();
	}
	
	@Test
    public void test1() {       			
		assertNotNull(loader.getPolicy());		       
    }
	
	
//	@Test
//	public void test2() {
//		try {
//			loader.savePolicy("C:/data/test2.json");
//		} catch (Exception e) {
//			ex = e;
//		}
//		assertEquals(null, ex);
//	}

}
