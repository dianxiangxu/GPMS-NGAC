package gpms.accesscontrol;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import org.wso2.balana.Balana;
import org.wso2.balana.ObligationResult;
import org.wso2.balana.PDP;
import org.wso2.balana.PDPConfig;
import org.wso2.balana.ParsingException;
import org.wso2.balana.ctx.AbstractRequestCtx;
import org.wso2.balana.ctx.AbstractResult;
import org.wso2.balana.ctx.AttributeAssignment;
import org.wso2.balana.ctx.RequestCtxFactory;
import org.wso2.balana.ctx.ResponseCtx;
import org.wso2.balana.ctx.Status;
import org.wso2.balana.ctx.xacml3.Result;
import org.wso2.balana.finder.impl.FileBasedPolicyFinderModule;
import org.wso2.balana.xacml3.Advice;

import com.google.common.collect.Multimap;

/***
 * XACML PEP for Accesscontrol implementation to handle Request/Response
 * 
 * @author milsonmunakami
 *
 */
public class Accesscontrol {
	private static Balana balana = null;
	private AbstractResult ar;
	AttributeSpreadSheet attrSpreadSheet = null;
	private static String policyLocation = new String();

	public Accesscontrol() {
		try {
			String file = "/XACMLAttributeDictionary.xls";
			InputStream inputStream = this.getClass().getResourceAsStream(file);
			String policyFolderName = "/policy";
			policyLocation = this.getClass().getResource(policyFolderName)
					.toURI().getPath();
			this.attrSpreadSheet = new AttributeSpreadSheet(inputStream);
		} catch (Exception e) {
			System.err.println("Can not locate policy repository");
		}
	}

	/***
	 * Initialization of Balana
	 */
	public static void initBalana() {
		if (balana == null) {
			synchronized (Accesscontrol.class) {
				if (balana == null) {
					System.setProperty(
							FileBasedPolicyFinderModule.POLICY_DIR_PROPERTY,
							policyLocation);
					balana = Balana.getInstance();
				}
			}
		}
	}

	/***
	 * Gets the response from Balana with Status code
	 * 
	 * @param request
	 *            XACML request formatted
	 * @return
	 */
	private ResponseCtx getResponse(String request) {
		initBalana();
		PDP pdp = getPDPNewInstance();
		System.out
				.println("\n======================== XACML Request ====================");
		System.out.println(request);
		System.out
				.println("===========================================================");
		AbstractRequestCtx requestCtx;
		ResponseCtx responseCtx;
		try {
			requestCtx = RequestCtxFactory.getFactory().getRequestCtx(
					request.replaceAll(">\\s+<", "><"));
			responseCtx = pdp.evaluate(requestCtx);
		} catch (ParsingException e) {
			String error = "Invalid request  : " + e.getMessage();
			ArrayList<String> code = new ArrayList<String>();
			code.add(Status.STATUS_SYNTAX_ERROR);
			Status status = new Status(code, error);
			// As invalid request, by default XACML 3.0 response is created.
			responseCtx = new ResponseCtx(new Result(
					AbstractResult.DECISION_INDETERMINATE, status));
		}
		return responseCtx;
	}

	/***
	 * Gets the Decision from the Balana for given Attributes
	 * 
	 * @param attrMap
	 *            Attributes get from the client
	 * @return Decision in String format
	 */
	public String getXACMLdecision(
			HashMap<String, Multimap<String, String>> attrMap) {
		String request = createXACMLRequest(attrMap);
		ResponseCtx response = getResponse(request.replaceAll(">\\s+<", "><"));
		if (response != null) {
			System.out
					.println("\n======================== XACML Response ====================");
			System.out.println(response.encode());
			System.out
					.println("===========================================================");
			Set<AbstractResult> set = response.getResults();
			Iterator<AbstractResult> it = set.iterator();
			int intDecision = AbstractResult.DECISION_NOT_APPLICABLE;
			while (it.hasNext()) {
				ar = it.next();
				intDecision = ar.getDecision();
				printObligations();
				printAdvice();
				System.out
						.println("===========================================================");
				if (intDecision == AbstractResult.DECISION_INDETERMINATE_DENY
						|| intDecision == AbstractResult.DECISION_INDETERMINATE_PERMIT
						|| intDecision == AbstractResult.DECISION_INDETERMINATE_DENY_OR_PERMIT) {
					intDecision = AbstractResult.DECISION_INDETERMINATE;
				}
				// WARNING: We currently take the first decision as the Multiple
				// decisions may be returned
				System.out.println("Decision:" + intDecision + " that is: "
						+ AbstractResult.DECISIONS[intDecision]);
				break;
			}
			return AbstractResult.DECISIONS[intDecision];
		} else {
			System.out.println("Response received PDP is Null");
		}
		return null;
	}

	/**
	 * Prints Advice in response
	 */
	private void printAdvice() {
		System.out
				.println("===========================================================");
		System.out
				.println("\n======================== Printing Advices ====================");
		List<Advice> advices = ar.getAdvices();
		for (Advice advice : advices) {
			if (advice instanceof org.wso2.balana.xacml3.Advice) {
				List<AttributeAssignment> assignments = ((org.wso2.balana.xacml3.Advice) advice)
						.getAssignments();
				for (AttributeAssignment assignment : assignments) {
					System.out.println("Advice :  " + assignment.getContent()
							+ "\n");
				}
			}
		}
	}

	/**
	 * Prints Obligations in response
	 */
	private void printObligations() {
		System.out
				.println("\n======================== Printing Obligations ====================");
		List<ObligationResult> obligations = ar.getObligations();
		for (ObligationResult obligation : obligations) {
			if (obligation instanceof org.wso2.balana.xacml3.Obligation) {
				List<AttributeAssignment> assignments = ((org.wso2.balana.xacml3.Obligation) obligation)
						.getAssignments();
				for (AttributeAssignment assignment : assignments) {
					System.out.println("Obligation :  "
							+ assignment.getContent() + "\n");
				}
			}
		}
	}

	/***
	 * Gets Decision from Balana with Obligations with XACML Profile
	 * 
	 * @param attrMap
	 *            attributes from client
	 * @param contentProfile
	 *            XACML Profile in <Content> element
	 * @return Decision Result
	 */
	public Set<AbstractResult> getXACMLdecisionWithObligations(
			HashMap<String, Multimap<String, String>> attrMap,
			StringBuffer contentProfile) {
		String request = createXACMLRequestWithProfile(attrMap, contentProfile);
		ResponseCtx response = getResponse(request);
		if (response != null) {
			System.out
					.println("\n======================== XACML Response ====================");
			System.out.println(response.encode());
			System.out
					.println("===========================================================");
			Set<AbstractResult> set = response.getResults();
			return set;
		} else {
			System.out.println("Response received PDP is Null");
		}
		return null;
	}

	/***
	 * Creates a Balana PDP instance
	 * 
	 * @return PDP
	 */
	private PDP getPDPNewInstance() {
		try {
			PDPConfig pdpConfig = balana.getPdpConfig();
			pdpConfig = new PDPConfig(pdpConfig.getAttributeFinder(),
					pdpConfig.getPolicyFinder(), pdpConfig.getResourceFinder(),
					true);
			return new PDP(pdpConfig);
		} catch (Exception e) {
			return null;
		}
	}

	/***
	 * Creates XACML Request
	 * 
	 * @param attributesMap
	 *            attributes from client
	 * @return Formatted XACML Request
	 */
	private String createXACMLRequest(
			HashMap<String, Multimap<String, String>> attributesMap) {
		StringBuffer subjectAttr = new StringBuffer();
		StringBuffer resourceAttr = new StringBuffer();
		StringBuffer actionAttr = new StringBuffer();
		StringBuffer environmentAttr = new StringBuffer();
		for (Entry<String, Multimap<String, String>> entry : attributesMap
				.entrySet()) {
			Set<String> keySet = entry.getValue().keySet();
			Iterator<String> keyIterator = keySet.iterator();
			switch (entry.getKey()) {
			case "Subject":
				generateAttribute(subjectAttr, entry, keyIterator);
				break;
			case "Resource":
				generateAttribute(resourceAttr, entry, keyIterator);
				break;
			case "Action":
				generateAttribute(actionAttr, entry, keyIterator);
				break;
			case "Environment":
				generateAttribute(environmentAttr, entry, keyIterator);
				break;
			default:
				break;
			}
		}
		StringBuffer finalRequest = new StringBuffer();
		finalRequest
				.append("<Request xmlns=\"urn:oasis:names:tc:xacml:3.0:core:schema:wd-17\" CombinedDecision=\"false\" ReturnPolicyIdList=\"false\">")
				.append(subjectAttr).append(resourceAttr).append(actionAttr)
				.append(environmentAttr).append("</Request>");
		return finalRequest.toString();
	}

	/**
	 * Creates formatted XACML Attribute elements
	 * 
	 * @param attr
	 *            StringBuffer to store Attribute Category
	 * @param entry
	 *            Attribute Map
	 * @param keyIterator
	 *            Iterator that goes through the map
	 */
	private void generateAttribute(StringBuffer attr,
			Entry<String, Multimap<String, String>> entry,
			Iterator<String> keyIterator) {
		boolean isFirstAttr = true;
		while (keyIterator.hasNext()) {
			String key = (String) keyIterator.next();
			Collection<String> values = entry.getValue().get(key);
			AttributeRecord attrRecord = this.attrSpreadSheet
					.findAttributeRecord(key);
			if (attrRecord != null) {
				for (String value : values) {
					if (attrRecord.getValues().contains(value)) {
						System.out.println(key + " :::::: " + value);
						if (isFirstAttr) {
							attr.append("<Attributes Category=\""
									+ attrRecord.getCategory().toString()
									+ "\">");
						}
						attr.append("<Attribute AttributeId=\""
								+ attrRecord.getFullAttributeName().toString()
								+ "\" IncludeInResult=\"false\">"
								+ "<AttributeValue DataType=\""
								+ attrRecord.getDataType().toString() + "\">"
								+ value + "</AttributeValue></Attribute>");
						isFirstAttr = false;
					}
				}
			}
		}
		attr.append("</Attributes>");
	}

	/***
	 * Generates Request having Obligations with XACML Profile
	 * 
	 * @param attributesMap
	 *            Attributes Map
	 * @param contentProfile
	 *            XACML Content profile
	 * @return
	 */
	private String createXACMLRequestWithProfile(
			HashMap<String, Multimap<String, String>> attributesMap,
			StringBuffer contentProfile) {
		StringBuffer subjectAttr = new StringBuffer();
		StringBuffer resourceAttr = new StringBuffer();
		StringBuffer actionAttr = new StringBuffer();
		StringBuffer environmentAttr = new StringBuffer();
		for (Entry<String, Multimap<String, String>> entry : attributesMap
				.entrySet()) {
			Set<String> keySet = entry.getValue().keySet();
			Iterator<String> keyIterator = keySet.iterator();
			switch (entry.getKey()) {
			case "Subject":
				generateAttribute(subjectAttr, entry, keyIterator);
				break;
			case "Resource":
				generateAttributeWithContent(contentProfile, resourceAttr,
						entry, keySet, keyIterator);
				break;
			case "Action":
				generateAttribute(actionAttr, entry, keyIterator);
				break;
			case "Environment":
				generateAttribute(environmentAttr, entry, keyIterator);
				break;
			default:
				break;
			}
		}
		StringBuffer finalRequest = new StringBuffer();
		finalRequest
				.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?><Request xmlns=\"urn:oasis:names:tc:xacml:3.0:core:schema:wd-17\" CombinedDecision=\"false\" ReturnPolicyIdList=\"false\">")
				.append(subjectAttr).append(resourceAttr).append(actionAttr)
				.append(environmentAttr).append("</Request>");
		return finalRequest.toString();
	}

	/**
	 * Generates Resource Attributes with <Content> profile element
	 * 
	 * @param contentProfile
	 *            profile content
	 * @param attr
	 *            StringBuffer to store Attribute Category
	 * @param entry
	 *            Attribute Map
	 * @param keySet
	 *            key elements
	 * @param keyIterator
	 *            Iterator that goes through the map
	 */
	private void generateAttributeWithContent(StringBuffer contentProfile,
			StringBuffer attr, Entry<String, Multimap<String, String>> entry,
			Set<String> keySet, Iterator<String> keyIterator) {
		boolean isFirstResource = true;
		while (keyIterator.hasNext()) {
			String key = (String) keyIterator.next();
			Collection<String> values = entry.getValue().get(key);
			AttributeRecord attrRecord = this.attrSpreadSheet
					.findAttributeRecord(key);
			if (attrRecord != null) {
				for (String value : values) {
					if (attrRecord.getValues().contains(value)) {
						System.out.println(key + " :::::: " + value);
						if (isFirstResource) {
							attr.append("<Attributes Category=\""
									+ attrRecord.getCategory().toString()
									+ "\">");
							if (contentProfile.length() != 0) {
								attr.append(contentProfile);
							}
						}
						attr.append("<Attribute AttributeId=\""
								+ attrRecord.getFullAttributeName().toString()
								+ "\" IncludeInResult=\"false\">"
								+ "<AttributeValue DataType=\""
								+ attrRecord.getDataType().toString() + "\">"
								+ value + "</AttributeValue></Attribute>");
						isFirstResource = false;
					}
				}
			}
		}
		if (keySet.isEmpty()) {
			attr.append("<Attributes Category=\"urn:oasis:names:tc:xacml:3.0:attribute-category:resource\">");
			if (contentProfile.length() != 0) {
				attr.append(contentProfile);
			}
		}
		attr.append("</Attributes>");
	}

	/***
	 * Gets XACML Decision for Multiple Decision Points (MDP) used for
	 * Delegation
	 * 
	 * @param attrMap
	 *            Attribute Map
	 * @return XACML Decision Result
	 */
	public Set<AbstractResult> getXACMLdecisionForMDP(
			HashMap<String, Multimap<String, String>> attrMap) {
		String request = createXACMLRequestForMDP(attrMap);
		ResponseCtx response = getResponse(request);
		if (response != null) {
			System.out
					.println("\n======================== XACML Response ====================");
			System.out.println(response.encode());
			System.out
					.println("===========================================================");
			Set<AbstractResult> set = response.getResults();
			return set;
		} else {
			System.out.println("Response received PDP is Null");
		}
		return null;
	}

	/***
	 * Generates Request for Multiple Decision Points (MDP) without XACML
	 * profile
	 * 
	 * @param attrMap
	 *            Attribute Map
	 * @return XACML Request
	 */
	private String createXACMLRequestForMDP(
			HashMap<String, Multimap<String, String>> attrMap) {
		StringBuffer subjectAttr = new StringBuffer();
		StringBuffer resourceAttr = new StringBuffer();
		StringBuffer actionAttr = new StringBuffer();
		StringBuffer environmentAttr = new StringBuffer();
		for (Entry<String, Multimap<String, String>> entry : attrMap.entrySet()) {
			Set<String> keySet = entry.getValue().keySet();
			Iterator<String> keyIterator = keySet.iterator();
			switch (entry.getKey()) {
			case "Subject":
				generateAttribute(subjectAttr, entry, keyIterator);
				break;
			case "Resource":
				generateAttribute(resourceAttr, entry, keyIterator);
				break;
			case "Action":
				generateSingleActionAttribute(actionAttr, entry, keyIterator);
				break;
			case "Environment":
				generateAttribute(environmentAttr, entry, keyIterator);
				break;
			default:
				break;
			}
		}
		StringBuffer finalRequest = new StringBuffer();
		finalRequest
				.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?><Request xmlns=\"urn:oasis:names:tc:xacml:3.0:core:schema:wd-17\" CombinedDecision=\"false\" ReturnPolicyIdList=\"false\">")
				.append(subjectAttr).append(resourceAttr).append(actionAttr)
				.append(environmentAttr).append("</Request>");
		return finalRequest.toString();
	}

	/***
	 * Gets XACML Decision for Multiple Decision Points (MDP) with XACML profile
	 * in <Content> element
	 * 
	 * @param attrMap
	 *            Attribute Map
	 * @param contentProfile
	 *            XACML Content profile
	 * @return XACML Decision Result
	 */
	public Set<AbstractResult> getXACMLdecisionForMDPWithProfile(
			HashMap<String, Multimap<String, String>> attrMap,
			StringBuffer contentProfile) {
		String request = createXACMLRequestForMDPWithProfile(attrMap,
				contentProfile);
		ResponseCtx response = getResponse(request);
		if (response != null) {
			System.out
					.println("\n======================== XACML Response ====================");
			System.out.println(response.encode());
			System.out
					.println("===========================================================");
			Set<AbstractResult> set = response.getResults();
			return set;
		} else {
			System.out.println("Response received PDP is Null");
		}
		return null;
	}

	/***
	 * Generates Request for Multiple Decision Points (MDP) with XACML profile
	 * 
	 * @param attrMap
	 *            Attribute Map
	 * @param contentProfile
	 *            XACML Content profile
	 * @return XACML Request
	 */
	private String createXACMLRequestForMDPWithProfile(
			HashMap<String, Multimap<String, String>> attrMap,
			StringBuffer contentProfile) {
		StringBuffer subjectAttr = new StringBuffer();
		StringBuffer resourceAttr = new StringBuffer();
		StringBuffer actionAttr = new StringBuffer();
		StringBuffer environmentAttr = new StringBuffer();
		for (Entry<String, Multimap<String, String>> entry : attrMap.entrySet()) {
			Set<String> keySet = entry.getValue().keySet();
			Iterator<String> keyIterator = keySet.iterator();
			switch (entry.getKey()) {
			case "Subject":
				generateAttribute(subjectAttr, entry, keyIterator);
				break;
			case "Resource":
				generateAttributeWithContent(contentProfile, resourceAttr,
						entry, keySet, keyIterator);
				break;
			case "Action":
				generateSingleActionAttribute(actionAttr, entry, keyIterator);
				break;
			case "Environment":
				generateAttribute(environmentAttr, entry, keyIterator);
				break;
			default:
				break;
			}
		}
		StringBuffer finalRequest = new StringBuffer();
		finalRequest
				.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?><Request xmlns=\"urn:oasis:names:tc:xacml:3.0:core:schema:wd-17\" CombinedDecision=\"false\" ReturnPolicyIdList=\"false\">")
				.append(subjectAttr).append(resourceAttr).append(actionAttr)
				.append(environmentAttr).append("</Request>");
		return finalRequest.toString();
	}

	/**
	 * Generates Request for Single Action Attribute
	 * 
	 * @param actionAttr
	 *            Action Attribute
	 * @param entry
	 *            Attribute Map
	 * @param keyIterator
	 *            Iterator that goes through the map
	 */
	private void generateSingleActionAttribute(StringBuffer actionAttr,
			Entry<String, Multimap<String, String>> entry,
			Iterator<String> keyIterator) {
		while (keyIterator.hasNext()) {
			String key = (String) keyIterator.next();
			Collection<String> values = entry.getValue().get(key);
			AttributeRecord attrRecord = this.attrSpreadSheet
					.findAttributeRecord(key);
			if (attrRecord != null) {
				for (String value : values) {
					if (attrRecord.getValues().contains(value)) {
						System.out.println(key + " :::::: " + value);
						actionAttr.append("<Attributes Category=\""
								+ attrRecord.getCategory().toString() + "\">");
						actionAttr.append("<Attribute AttributeId=\""
								+ attrRecord.getFullAttributeName().toString()
								+ "\" IncludeInResult=\"true\">"
								+ "<AttributeValue DataType=\""
								+ attrRecord.getDataType().toString() + "\">"
								+ value + "</AttributeValue></Attribute>");
						actionAttr.append("</Attributes>");
					}
				}
			}
		}
	}

}
