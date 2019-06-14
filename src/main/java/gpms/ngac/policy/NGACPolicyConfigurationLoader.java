package gpms.ngac.policy;

import gov.nist.csd.pm.exceptions.PMException;
import gov.nist.csd.pm.graph.Graph;
import gov.nist.csd.pm.graph.GraphSerializer;
import gov.nist.csd.pm.graph.MemGraph;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Random;

import org.apache.log4j.Logger;


/**
 * @author Md Nazmul Karim
 * @since May 20 2019
 * 
 * This class is used to load NGAC base configuration from a JSON file
 */
public class NGACPolicyConfigurationLoader {

	public static Random rand = new Random();

	//This graph holds NGAC policy
	private static Graph ngacPolicy;
	private static final Logger log = Logger.getLogger(NGACPolicyConfigurationLoader.class.getName());

	public void init() {
		if (ngacPolicy == null) {
			File file_super = getFileFromResources(Constants.POLICY_CONFIG_FILE_SUPER);
			File file_rbac = getFileFromResources(Constants.POLICY_CONFIG_FILE_RBAC);
			File file_pds = getFileFromResources(Constants.POLICY_CONFIG_FILE_PDS);
			File file_approval = getFileFromResources(Constants.POLICY_CONFIG_FILE_APPROVAL);
			File file_getFaculty = getFileFromResources(Constants.POLICY_CONFIG_FILE_GET_FACULTY);
			File file_cross_policy = getFileFromResources(Constants.POLICY_CONFIG_FILE_CROSS_POLICY);
			String jsonSuper;
			String jsonRbac;
			String jsonPds;
			String jsonApproval;
			String jsonGetFaculty;
			String jsonCrossPolicy;
			try {
				jsonSuper = new String(Files.readAllBytes(Paths.get(file_super.getAbsolutePath())));
				jsonRbac = new String(Files.readAllBytes(Paths.get(file_rbac.getAbsolutePath())));
				jsonPds = new String(Files.readAllBytes(Paths.get(file_pds.getAbsolutePath())));
				jsonApproval = new String(Files.readAllBytes(Paths.get(file_approval.getAbsolutePath())));
				jsonGetFaculty = new String(Files.readAllBytes(Paths.get(file_getFaculty.getAbsolutePath())));
				jsonCrossPolicy = new String(Files.readAllBytes(Paths.get(file_cross_policy.getAbsolutePath())));
				try {
					ngacPolicy = GraphSerializer.fromJson(new MemGraph(), jsonSuper);
					ngacPolicy = GraphSerializer.fromJson(ngacPolicy, jsonRbac);
					ngacPolicy = GraphSerializer.fromJson(ngacPolicy, jsonPds);
					ngacPolicy = GraphSerializer.fromJson(ngacPolicy, jsonApproval);
					ngacPolicy = GraphSerializer.fromJson(ngacPolicy, jsonGetFaculty);
					ngacPolicy = GraphSerializer.fromJson(ngacPolicy, jsonCrossPolicy);
				} catch (PMException e) {
					log.debug("PM Exception: InitialConfigurationLoader : while loading NGAC base configuration. "
							+ e.toString());
				}
			} catch (IOException e) {
				log.debug("I/O Exception : InitialConfigurationLoader : while loading NGAC base configuration."
						+ e.toString());
			}

			log.info("PM Configuration loaded successfully");
		} else {
			log.info("PM graph is already loaded.");
		}
	}

	public static Graph getPolicy() {
		return ngacPolicy;
	}

	public void setPolicy(Graph policy) {
		this.ngacPolicy = policy;
	}

	public static long getID() {
		return rand.nextLong();
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
