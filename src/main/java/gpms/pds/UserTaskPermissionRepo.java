package gpms.pds;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class UserTaskPermissionRepo {
	
	private static HashMap<String, Set<String>> approvedTaskSet;
	
	public static void init() {
		if(approvedTaskSet ==null)
			approvedTaskSet = new HashMap<String, Set<String>>();
	}
	
	public static void add(String userName, String taskName) {
		if(approvedTaskSet.get(userName) != null) {
			HashSet<String> set = (HashSet<String>) approvedTaskSet.get(userName);
			set.add(taskName);
			approvedTaskSet.put(userName, set);
		}
		else {
			HashSet<String> set = new HashSet<String>();
			set.add(taskName);
			approvedTaskSet.put(userName, set);
		}
	}
	
	public static void clear(String userName) {
		if(approvedTaskSet.get(userName) != null) {
			HashSet<String> set = (HashSet<String>) approvedTaskSet.get(userName);
			set.clear();
			approvedTaskSet.put(userName, set);
		}
	}
	

}
