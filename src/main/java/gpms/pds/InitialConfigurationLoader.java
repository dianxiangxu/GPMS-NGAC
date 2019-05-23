package gpms.pds;

import gov.nist.csd.pm.decider.PReviewDecider;
import gov.nist.csd.pm.exceptions.PMException;
import gov.nist.csd.pm.graph.Graph;
import gov.nist.csd.pm.graph.GraphSerializer;
import gov.nist.csd.pm.graph.MemGraph;
import gov.nist.csd.pm.graph.model.nodes.Node;
import gov.nist.csd.pm.graph.model.nodes.NodeType;
import gpms.rest.UserService;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.apache.log4j.Logger;

import static gov.nist.csd.pm.graph.model.nodes.NodeType.O;
import static gov.nist.csd.pm.graph.model.nodes.NodeType.OA;
import static gov.nist.csd.pm.graph.model.nodes.NodeType.U;

public class InitialConfigurationLoader {
	
	public static Random rand = new Random();
	
	private static Graph graph;
	private static final Logger log = Logger.getLogger(InitialConfigurationLoader.class
			.getName());
	
	
	public void init() 
	{
		if(graph ==null)
		{
			File file = getFileFromResources( "docs/initpds.json");
			String json;
			try {
				json = new String(Files.readAllBytes(Paths.get(file.getAbsolutePath())));
				try {
					graph = GraphSerializer.fromJson(new MemGraph(), json);
				} catch (PMException e) {
					log.debug("PM Exception");
					e.printStackTrace();
				}
			} catch (IOException e) {
				log.debug("PM IO Exception");
				e.printStackTrace();
			}		
			
			log.info("PM Configuration loaded successfully");
		}
		else
		{
			log.info("PM graph is already loaded");
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
	
	private File getFileFromResources( String fileName) {
		ClassLoader classLoader = this.getClass().getClassLoader();

		URL resource = classLoader.getResource(fileName);
		if (resource == null) {
			throw new IllegalArgumentException("file is not found!");
		} else {
			return new File(resource.getFile());
		}

	}

}
