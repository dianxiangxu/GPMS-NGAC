package gpms.ngac.policy;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;

/**
 * @author Md Nazmul Karim
 * 
 * This class is used to store permitted task lists for the users.
 * if Business needs to check if a user is permitted to do a task it
 * only need to check with this repo.
 * Whenever NGAC policy is changed this repo data should re-calculated.
 */
public class UserTaskPermissionRepo {
	
	private static final Logger log = Logger.getLogger(UserTaskPermissionRepo.class
			.getName());
	
	/*
	 *this hashMap will store approved task list against user; the key string is username
	 *and value is a set of task names; e.g., nazmul->["Create Proposal","Add CoPI"] 
	 *taskNames are identical to the defined_tasks.json names
	 * */
	private static HashMap<String, Set<String>> approvedTaskSet;
	
	
	/**
	 * Create object if only approvedTaskList is null
	 */
	public static void init() {
		if(approvedTaskSet ==null)
			approvedTaskSet = new HashMap<String, Set<String>>();
	}
	
	/**
	 * This method will store taskName in a set against key userName
	 * 
	 */
	public static void add(String userName, String taskName) {
		
		if(approvedTaskSet.get(userName) != null) {  // check if hash set already exists
			HashSet<String> set = (HashSet<String>) approvedTaskSet.get(userName);
			set.add(taskName);
			approvedTaskSet.put(userName, set);
		}
		else {   // other wise create a new HashSet and add taskName
			HashSet<String> set = new HashSet<String>();
			set.add(taskName);
			approvedTaskSet.put(userName, set);
		}
		log.info("Task added:"+taskName+" to user: "+userName);
	}
	
	
	/**
	 * @param userName
	 * This method clears task set of a user
	 */
	public static void clear(String userName) {
		if(approvedTaskSet.get(userName) != null) {
			HashSet<String> set = (HashSet<String>) approvedTaskSet.get(userName);
			set.clear();
			approvedTaskSet.put(userName, set);
			log.info("Task set cleared : "+userName);
		}
	}
	
	
	public static boolean checkForPermission(String userName, String taskName) {
		
		HashSet permisstedWorkList = (HashSet)approvedTaskSet.get(userName);
		
		if(permisstedWorkList.contains(taskName)) {
			log.info("checkForPermission : "+userName+" |"+taskName+"| true");
			return true;
		}
		return false;
	}
	

}
