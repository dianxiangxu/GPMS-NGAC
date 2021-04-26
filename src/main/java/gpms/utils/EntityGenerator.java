package gpms.utils;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;

import org.mongodb.morphia.Morphia;

import com.github.javafaker.Faker;
import com.mongodb.MongoClient;

import gpms.DAL.MongoDBConnector;
import gpms.dao.UserAccountDAO;
import gpms.dao.UserProfileDAO;
import gpms.dataModel.Address;
import gpms.dataModel.PositionDetails;
import gpms.dataModel.UserAccount;
import gpms.dataModel.UserProfile;

public class EntityGenerator {

	MongoClient mongoClient;
	String dbName = "db_gpms";
	UserAccountDAO userAccountDAO;
	UserProfileDAO userProfileDAO;
	
	
	public static void main(String[] args) throws Exception {
		Faker faker = new Faker();
		String firstName = faker.name().firstName();
		String lastName = faker.name().lastName();
		
		String streetAddress = faker.address().streetAddress();
		System.out.println(streetAddress);
		PositionDetails pd = new PositionDetails();
		pd.setCollege("Engineering TEST"); //change
		pd.setDepartment("Computer Engineering TEST"); //change
		pd.setPositionTitle("Department Chair TEST"); //change
		pd.setPositionType("Administrator TEST");;//Professional staff --Bussiness Manager
		pd.setAsDefault(true);
		System.out.println(pd);
		Morphia morphia = new Morphia();
		MongoClient mongoClient = MongoDBConnector.getMongo();
		UserAccountDAO userAccountDAO = new UserAccountDAO(mongoClient, morphia, "db_gpms");
		UserProfileDAO userProfileDAO = new UserProfileDAO(mongoClient, morphia, "db_gpms");
		UserAccount newAccount = new UserAccount();
		UserProfile newProfile = new UserProfile();
		newAccount.setAddedOn(new Date());
		String loginUserName = "usernameTEST3";//change
		newAccount.setUserName(loginUserName);
		String password = "gpmspassword";

		newAccount.setPassword(password);
		newAccount.setActive(true);
		newProfile.setUserAccount(newAccount);
		newProfile.setDetails(null);
		newProfile.setFirstName(firstName);
		newProfile.setMiddleName("A");	
		newProfile.setLastName(lastName);
		DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
		Date dob = formatter.parse("1977-07-07");
		newProfile.setDateOfBirth(dob);
		newProfile.setGender("Male");
		
		Address newAddress = new Address();
		newAddress.setStreet(streetAddress);		
		newAddress.setApt("");		
		newAddress.setCity("Kansas City");		
		newAddress.setState("Missouri");		
		newAddress.setZipcode("77777");		
		newAddress.setCountry("United States");
		
		newProfile.setAddresses(new ArrayList<Address>(
			    Arrays.asList(newAddress)));
		newProfile.setPersonalEmails(new ArrayList<String>(
			    Arrays.asList("gpmsngac2020@gmail.com")));
		newProfile.setWorkEmails(new ArrayList<String>(
			    Arrays.asList("gpmsngac20213@gmail.com")));
		newProfile.setDetails(new ArrayList<PositionDetails>(
			    Arrays.asList(pd)));						
		userAccountDAO.save(newAccount);
		userProfileDAO.signUpUser(newProfile);
	}
	
	
	
	public EntityGenerator() {

	}
}