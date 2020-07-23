package gpms.policy;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;

import org.apache.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;




public class TaskConfigurationParser {
	
	private static ArrayList<TaskDefinition> tasks;
	private static final Logger log = Logger.getLogger(TaskConfigurationParser.class.getName());

	public void init() {
		if (tasks == null) {
			tasks = new ArrayList<TaskDefinition>();
			try {
				File file = getFileFromResources("docs/defined_tasks.json");
				 Object obj = new JSONParser().parse(new FileReader(file)); 
		         JSONObject jo = (JSONObject) obj;
		         
		         JSONArray ja = (JSONArray) jo.get("tasks"); 
		         
		         Iterator itr = ja.iterator(); 
		         
		         while (itr.hasNext()) { 
		             JSONObject taskObj = (JSONObject) itr.next(); 
		             
		             TaskDefinition taskDefinition = new TaskDefinition();
		            
		             String name = (String) taskObj.get("name"); 
		             String code = (String) taskObj.get("code"); 
		             
		             taskDefinition.setName(name);
		             taskDefinition.setCode(code);    
		          
		             
		             JSONArray permissionArr = (JSONArray) taskObj.get("permissions"); 
		             
		             Iterator itrArr = permissionArr.iterator();  
		             while (itrArr.hasNext()) { 
		                 JSONObject permissionObj = (JSONObject) itrArr.next();
		                 
		                 AttributePermission permission = new AttributePermission();
		                 
		                 String permissionAttName = (String) permissionObj.get("attribute_name"); 
		                 String permissionAttType = (String) permissionObj.get("type"); 
		                 
		                 permission.setAttributeName(permissionAttName);
		                 permission.setAttributeType(permissionAttType);
		                 permission.setAttributeNameKey();
		                
		                 
		                 JSONArray accessRightsArr = (JSONArray) permissionObj.get("access_right_set"); 
		                 
		                 Iterator itrAccessRights = accessRightsArr.iterator();  
		                 while (itrAccessRights.hasNext()) { 
		                	 String accessRight = (String) itrAccessRights.next();
		                	 permission.addAccessRight(accessRight);		                	 
		                 } //end while
		                 taskDefinition.addPermission(permission);
		                 
		             } //end while
		             
		             tasks.add(taskDefinition);
		         }  //end while
			} catch(FileNotFoundException fnf) {
				log.info("task definition file not found");	        	 
	         } catch(IOException io) {
	        	 log.info("IO error");	       
	         } catch(ParseException pe) {
	        	 log.info("Parser Exception");	       
	         }	  
			
			for(TaskDefinition task : tasks) {
		       	 log.info(task.toString());
		        }
		}
		
		
	}

	public static ArrayList<TaskDefinition> getTasks() {
		return tasks;
	}

	public static void setTasks(ArrayList<TaskDefinition> tasks) {
		TaskConfigurationParser.tasks = tasks;
	}
	
	private File getFileFromResources(String fileName) {
		ClassLoader classLoader = this.getClass().getClassLoader();

		URL resource = classLoader.getResource(fileName);
		if (resource == null) {
			throw new IllegalArgumentException("file is not found!");
		} else {
			return new File(resource.getFile());
		}

	}
	      
    
	
	

}
