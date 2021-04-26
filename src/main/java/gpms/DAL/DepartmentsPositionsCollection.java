package gpms.DAL;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.mongodb.morphia.Morphia;

import com.mongodb.MongoClient;

import gpms.dao.ProposalDAO;
import gpms.dao.UserAccountDAO;
import gpms.dao.UserProfileDAO;
import gpms.dataModel.GPMSCommonInfo;
import gpms.dataModel.UserAccount;
import gpms.dataModel.UserProfile;

/***
 * Available Department Positions Types and Titles for a User
 * 
 * @author milsonmunakami
 *
 */
public class DepartmentsPositionsCollection {
	private static final Map<String, HashMap<String, HashMap<String, ArrayList<String>>>> ht = new HashMap<String, HashMap<String, HashMap<String, ArrayList<String>>>>();

	public static HashMap<String, String> adminUsers =null;
	public static HashMap<String, String> departmentNames =null;
	public static HashMap<String, String> userIdNameMap =null;
	
	public static void init() {
		if(adminUsers == null) {
			adminUsers = new HashMap<String, String>();
			adminUsers.put("CSCHAIR","chaircomputerscience");
			adminUsers.put("CSBM","bmcomputerscience");
			adminUsers.put("CSDEAN","deancomputerscience");
			
			adminUsers.put("CECHAIR","chaircomputerengineering");
			adminUsers.put("CEBM","bmcomputerengineering1");
			adminUsers.put("CEDEAN","deancomputerengineering");
			
			adminUsers.put("ECECHAIR","chairelectricalengineering");
			adminUsers.put("ECEBM","bmelectricalengineering");
			adminUsers.put("ECEDEAN","deanelectricalengineering");
			
			adminUsers.put("PHYCHAIR","chairphysics1");
			adminUsers.put("PHYBM","bmphysics1");
			adminUsers.put("PHYDEAN","deanphysics1");
			
			adminUsers.put("CHECHAIR","chairchemistry");
			adminUsers.put("CHEBM","bmchemistry1");
			adminUsers.put("CHEDEAN","deanchemistry1");
			
			adminUsers.put("URA","racomputerscience");
			adminUsers.put("URD","directorcomputerscience");
			adminUsers.put("IRB","irbglobal");
			
		}
		if(departmentNames == null) {
			departmentNames = new HashMap<String, String>();			
			departmentNames.put("Computer Science","CS");
			departmentNames.put("Computer Engineering","CE");
			departmentNames.put("Electrical Engineering","ECE");
			departmentNames.put("Mechanical Engineering","MEC");//added
			departmentNames.put("Informational Technology","INFO");//added
			
			departmentNames.put("Physics","PHY");
			departmentNames.put("Chemistry","CHE");
			departmentNames.put("Economics","ECON");//added
			departmentNames.put("Mathematics","MATH");//added
			departmentNames.put("Biology","BIO");//added
			
			departmentNames.put("Dentistry","DENT");//added
			departmentNames.put("Medicine","MED");//added
			departmentNames.put("Nursing","NURS");//added
			departmentNames.put("Pharmacy","PHA");//added
			departmentNames.put("Health Studies","HSE");//added
			
			departmentNames.put("Early Childhood","EARLY");//added
			departmentNames.put("Elementary School","ELEM");//added
			departmentNames.put("High School","HSL");//added
			departmentNames.put("Middle School","MSL");//added
			departmentNames.put("Foreign Languages","FORL");//added
			
			departmentNames.put("Business Administration","BA");//added
			departmentNames.put("Accounting","ACC");//added
			departmentNames.put("Finance","FIN");//added
			departmentNames.put("Entrepreneurial Real Estate","ENTREP");//added
			departmentNames.put("Urban Policy Administration","URB");//added
			
			departmentNames.put("Law leading to the Master of Law","LLM");//added
			departmentNames.put("Juris Doctor","JD");//added
			departmentNames.put("Law leading Tax","LLMT");//added
			departmentNames.put("Family Law","FAM");//added
			departmentNames.put("Intellectual Property","INTL");//added
		}
		
		if(userIdNameMap == null) {
			userIdNameMap = new HashMap<String, String>();
			UserProfileDAO userProfileDAO = null;
			MongoClient mongoClient = null;
			Morphia morphia = null;
			String dbName = "db_gpms";
			
			mongoClient = MongoDBConnector.getMongo();
			morphia = new Morphia();
			morphia.map(UserProfile.class).map(UserAccount.class);
			userProfileDAO = new UserProfileDAO(mongoClient, morphia, dbName);
			
			GPMSCommonInfo userInfo = new GPMSCommonInfo();
			userInfo.setUserIsActive(true);
			
			List<UserProfile> deptUsers = userProfileDAO.findAllForAdminUserGrid("DEPT");
			////System.out.println("All Admins:");
			for(UserProfile user : deptUsers) {
				userIdNameMap.put( user.getUserAccount().getUserName(),user.getId().toString());
			}
			
			List<UserProfile> users = userProfileDAO.findAllForAdminUserGrid("UNIVERSITY");
			////System.out.println("All Admins:");
			for(UserProfile user : users) {
				userIdNameMap.put( user.getUserAccount().getUserName(),user.getId().toString());
			}
			
		}
		
		
	}
	
	public DepartmentsPositionsCollection() {
		ArrayList<String> tenuredTitles = new ArrayList<String>();
		tenuredTitles.add("Distinguished Professor");
		tenuredTitles.add("Professor");
		tenuredTitles.add("Associate Professor");
		tenuredTitles.add("Assistant Professor");

		ArrayList<String> nonTenuredTitles = new ArrayList<String>();
		nonTenuredTitles.add("Research Professor");
		nonTenuredTitles.add("Associate Research Professor");
		nonTenuredTitles.add("Assistant Research Professor");
		nonTenuredTitles.add("Clinical Professor");
		nonTenuredTitles.add("Clinical Associate Professor");
		nonTenuredTitles.add("Clinical Assistant Professor");
		nonTenuredTitles.add("Visiting Professor");
		nonTenuredTitles.add("Visiting Associate Professor");
		nonTenuredTitles.add("Visiting Assistant Professor");

		ArrayList<String> teachingFaculty = new ArrayList<String>();
		teachingFaculty.add("Lecturer");
		teachingFaculty.add("Senior Lecturer");
		teachingFaculty.add("Adjunct Professor");

		ArrayList<String> researchStaff = new ArrayList<String>();
		researchStaff.add("Research Associate");
		researchStaff.add("Research Scientist");
		researchStaff.add("Senior Research Scientist");

		ArrayList<String> professionalStaff = new ArrayList<String>();
		// professionalStaff.add("IRB");
		professionalStaff.add("Business Manager");
		// professionalStaff.add("University Research Administrator");
		professionalStaff.add("Department Administrative Assistant");

		ArrayList<String> administratorStaff = new ArrayList<String>();
		administratorStaff.add("Department Chair");
		administratorStaff.add("Associate Chair");
		administratorStaff.add("Dean");
		administratorStaff.add("Associate Dean");

		// ArrayList<String> universityAdministrator = new ArrayList<String>();
		// universityAdministrator.add("IRB");
		// universityAdministrator.add("University Research Administrator");
		// universityAdministrator.add("University Research Director");

		HashMap<String, ArrayList<String>> positionTypeUNIVERSAL = new HashMap<String, ArrayList<String>>(); //UNIVERSAL ADD
		positionTypeUNIVERSAL.put("Tenured/tenure-track faculty", tenuredTitles);
		positionTypeUNIVERSAL.put("Non-tenure-track research faculty",
				nonTenuredTitles);
		positionTypeUNIVERSAL.put("Teaching faculty", teachingFaculty);
		positionTypeUNIVERSAL.put("Research staff", researchStaff);
		positionTypeUNIVERSAL.put("Professional staff", professionalStaff);
		positionTypeUNIVERSAL.put("Administrator", administratorStaff);
		
		
		HashMap<String, ArrayList<String>> positionTypeCS = new HashMap<String, ArrayList<String>>();
		positionTypeCS.put("Tenured/tenure-track faculty", tenuredTitles);
		positionTypeCS.put("Non-tenure-track research faculty",
				nonTenuredTitles);
		positionTypeCS.put("Teaching faculty", teachingFaculty);
		positionTypeCS.put("Research staff", researchStaff);
		positionTypeCS.put("Professional staff", professionalStaff);
		positionTypeCS.put("Administrator", administratorStaff);
		// positionTypeCS.put("University administrator",
		// universityAdministrator);

		HashMap<String, ArrayList<String>> positionTypeEE = new HashMap<String, ArrayList<String>>();
		positionTypeEE.put("Tenured/tenure-track faculty", tenuredTitles);
		positionTypeEE.put("Non-tenure-track research faculty",
				nonTenuredTitles);
		positionTypeEE.put("Teaching faculty", teachingFaculty);
		positionTypeEE.put("Research staff", researchStaff);
		positionTypeEE.put("Professional staff", professionalStaff);
		positionTypeEE.put("Administrator", administratorStaff);

		HashMap<String, ArrayList<String>> positionTypeCE = new HashMap<String, ArrayList<String>>();
		positionTypeCE.put("Tenured/tenure-track faculty", tenuredTitles);
		positionTypeCE.put("Non-tenure-track research faculty",
				nonTenuredTitles);
		positionTypeCE.put("Teaching faculty", teachingFaculty);
		positionTypeCE.put("Research staff", researchStaff);
		positionTypeCE.put("Professional staff", professionalStaff);
		positionTypeCE.put("Administrator", administratorStaff);

		HashMap<String, ArrayList<String>> positionTypePhysics = new HashMap<String, ArrayList<String>>();
		positionTypePhysics.put("Tenured/tenure-track faculty", tenuredTitles);
		positionTypePhysics.put("Non-tenure-track research faculty",
				nonTenuredTitles);
		positionTypePhysics.put("Teaching faculty", teachingFaculty);
		positionTypePhysics.put("Research staff", researchStaff);
		positionTypePhysics.put("Professional staff", professionalStaff);
		positionTypePhysics.put("Administrator", administratorStaff);

		HashMap<String, ArrayList<String>> positionTypeChemistry = new HashMap<String, ArrayList<String>>();
		positionTypeChemistry
				.put("Tenured/tenure-track faculty", tenuredTitles);
		positionTypeChemistry.put("Non-tenure-track research faculty",
				nonTenuredTitles);
		positionTypeChemistry.put("Teaching faculty", teachingFaculty);
		positionTypeChemistry.put("Research staff", researchStaff);
		positionTypeChemistry.put("Professional staff", professionalStaff);
		positionTypeChemistry.put("Administrator", administratorStaff);

		HashMap<String, HashMap<String, ArrayList<String>>> departmentEngineering = new HashMap<String, HashMap<String, ArrayList<String>>>();
		departmentEngineering.put("Computer Science", positionTypeCS);
		departmentEngineering.put("Electrical Engineering", positionTypeEE);
		departmentEngineering.put("Computer Engineering", positionTypeCE);
		departmentEngineering.put("Mechanical Engineering", positionTypeCE);//added
		departmentEngineering.put("Informational Technology", positionTypeCE);//added

		HashMap<String, HashMap<String, ArrayList<String>>> departmentScience = new HashMap<String, HashMap<String, ArrayList<String>>>();
		departmentScience.put("Physics", positionTypePhysics);
		departmentScience.put("Chemistry", positionTypeChemistry);
		departmentScience.put("Economics", positionTypeChemistry);//added
		departmentScience.put("Mathematics", positionTypeChemistry);//added
		departmentScience.put("Biology", positionTypeChemistry);//added

		HashMap<String, HashMap<String, ArrayList<String>>> departmentHealth = new HashMap<String, HashMap<String, ArrayList<String>>>();
		departmentHealth.put("Dentistry", positionTypeUNIVERSAL);//added
		departmentHealth.put("Medicine", positionTypeUNIVERSAL);//added
		departmentHealth.put("Nursing", positionTypeUNIVERSAL);//added
		departmentHealth.put("Pharmacy", positionTypeUNIVERSAL);//added
		departmentHealth.put("Health Studies", positionTypeUNIVERSAL);//added
		
		HashMap<String, HashMap<String, ArrayList<String>>> departmentEDU = new HashMap<String, HashMap<String, ArrayList<String>>>();
		departmentEDU.put("Early Childhood", positionTypeUNIVERSAL);//added
		departmentEDU.put("Elementary School", positionTypeUNIVERSAL);//added
		departmentEDU.put("High School", positionTypeUNIVERSAL);//added
		departmentEDU.put("Middle School", positionTypeUNIVERSAL);//added
		departmentEDU.put("Foreign Languages", positionTypeUNIVERSAL);//added
		
		HashMap<String, HashMap<String, ArrayList<String>>> departmentMNGT = new HashMap<String, HashMap<String, ArrayList<String>>>();
		departmentMNGT.put("Business Administration", positionTypeUNIVERSAL);//added
		departmentMNGT.put("Accounting", positionTypeUNIVERSAL);//added
		departmentMNGT.put("Finance", positionTypeUNIVERSAL);//added
		departmentMNGT.put("Entrepreneurial Real Estate", positionTypeUNIVERSAL);//added
		departmentMNGT.put("Urban Policy Administration", positionTypeUNIVERSAL);//added
		
		HashMap<String, HashMap<String, ArrayList<String>>> departmentLAW = new HashMap<String, HashMap<String, ArrayList<String>>>();
		departmentLAW.put("Law leading to the Master of Law", positionTypeUNIVERSAL);//added
		departmentLAW.put("Juris Doctor", positionTypeUNIVERSAL);//added
		departmentLAW.put("Law leading Tax", positionTypeUNIVERSAL);//added
		departmentLAW.put("Family Law", positionTypeUNIVERSAL);//added
		departmentLAW.put("Intellectual Property", positionTypeUNIVERSAL);//added
		
		ht.put("Engineering", departmentEngineering);
		ht.put("Science", departmentScience);
		ht.put("Health Sciences", departmentHealth);
		ht.put("Education", departmentEDU);
		ht.put("Management", departmentMNGT);
		ht.put("Law", departmentLAW);


	}

	public Map<String, HashMap<String, HashMap<String, ArrayList<String>>>> getAvailableDepartmentsAndPositions() {
		return ht;
	}

	public Set<String> getCollegeKeys() {
		return ht.keySet();
	}

	public Set<String> getDepartmentKeys(String college) {
		return ht.get(college).keySet();
	}

	public Set<String> getPositionType(String college, String department) {
		return ht.get(college).get(department).keySet();
	}

	public List<String> getPositionTitle(String college, String department,
			String positionType) {
		return ht.get(college).get(department).get(positionType);
	}

}
