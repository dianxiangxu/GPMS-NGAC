package gpms.pds;

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
	private static Graph graph;
	private static final Logger log = Logger.getLogger(NGACPolicyConfigurationLoader.class.getName());

	public void init() {
		if (graph == null) {
			File file = getFileFromResources("docs/initpds.json");
			String json;
			try {
				json = new String(Files.readAllBytes(Paths.get(file.getAbsolutePath())));
				try {
					graph = GraphSerializer.fromJson(new MemGraph(), json);
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

	public static Graph getGraph() {
		return graph;
	}

	public void setGraph(Graph graph) {
		this.graph = graph;
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
