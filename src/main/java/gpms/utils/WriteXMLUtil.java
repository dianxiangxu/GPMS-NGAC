package gpms.utils;

import gpms.model.Delegation;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.List;

import org.apache.commons.lang3.RandomStringUtils;
import org.jdom2.Attribute;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.Namespace;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;

/***
 * XML utility to generate dynamic delegation
 * 
 * @author milsonmunakami
 *
 */
public class WriteXMLUtil {

	private static String delegationXMLFileName = File.separator
			+ "DelegationPolicy.xml";

	/***
	 * Saves the generated dynamic policy node
	 * 
	 * @param userProfileID
	 *            Delegator user profile Id
	 * @param delegatorName
	 *            Delegator Name
	 * @param policyLocation
	 *            Policy location Path
	 * @param existingDelegation
	 *            Existing Delegation Object
	 * @return XML Policy Node
	 */
	public static String saveDelegationPolicy(String userProfileID,
			String delegatorName, String policyLocation,
			Delegation existingDelegation) {
		String delegationPolicyId = existingDelegation.getDelegationPolicyId();
		String delegateeId = existingDelegation.getDelegateeId();
		String delegateeName = existingDelegation.getDelegatee();
		String departmentName = existingDelegation.getDelegateeDepartment();
		String positionTitle = existingDelegation.getDelegateePositionTitle();
		List<String> actions = existingDelegation.getActions();
		String delegationId = existingDelegation.getId().toString();
		DateFormat policyDateFormat = new SimpleDateFormat(
				"yyyy-MM-dd'T'HH:mm:ssXXX");
		final String fromDate = policyDateFormat.format(existingDelegation
				.getFrom());
		final String toDate = policyDateFormat.format(existingDelegation
				.getTo());
		SAXBuilder builder = new SAXBuilder();
		File xmlFile = createDelegationPolicyTemplate(policyLocation);
		Document doc = null;
		try {
			doc = (Document) builder.build(xmlFile);
		} catch (JDOMException | IOException e) {
			e.printStackTrace();
		}

		if (doc != null) {
			Element policySet = doc.getRootElement();

			if (delegationPolicyId == null || delegationPolicyId.isEmpty()) {
				return createPolicyNode(userProfileID, delegatorName,
						policyLocation, delegateeId, delegateeName,
						departmentName, positionTitle, actions, delegationId,
						fromDate, toDate, policySet, doc);
			} else {
				Namespace ns = Namespace
						.getNamespace("urn:oasis:names:tc:xacml:3.0:core:schema:wd-17");
				List<Element> policyElements = doc.getRootElement()
						.getChildren("Policy", ns);
				for (Element policy : policyElements) {
					String policyId = policy.getAttributeValue("PolicyId");
					if (policyId.equals(existingDelegation
							.getDelegationPolicyId())) {
						policy.getParent().removeContent(policy);
						return createPolicyNode(userProfileID, delegatorName,
								policyLocation, delegateeId, delegateeName,
								departmentName, positionTitle, actions,
								delegationId, fromDate, toDate, policySet, doc);
					}
				}
			}
		}
		return "";
	}

	/**
	 * Checks if policy set template exists else it will be created
	 * 
	 * @param delegatorName
	 * @param policyLocation
	 * @param delegateeName
	 * @param departmentName
	 * @param positionTitle
	 * @param actions
	 * @return
	 */
	private static File createDelegationPolicyTemplate(String policyLocation) {
		File xmlFile = new File(policyLocation + delegationXMLFileName);
		if (!xmlFile.exists()) {
			Namespace ns = Namespace
					.getNamespace("urn:oasis:names:tc:xacml:3.0:core:schema:wd-17");
			Element policySet = new Element("PolicySet", ns);
			Namespace XSI = Namespace.getNamespace("xsi",
					"http://www.w3.org/2001/XMLSchema-instance");
			Namespace XACML = Namespace.getNamespace("xacml",
					"urn:oasis:names:tc:xacml:3.0:core:schema:wd-17");
			policySet.addNamespaceDeclaration(XSI);
			policySet.addNamespaceDeclaration(XACML);
			policySet
					.setAttribute(
							"schemaLocation",
							"urn:oasis:names:tc:xacml:3.0:core:schema:wd-17 http://docs.oasis-open.org/xacml/3.0/xacml-core-v3-schema-wd-17.xsd",
							XSI);
			String policySetId = "PolicySet"
					+ RandomStringUtils.randomAlphanumeric(8);
			policySet.setAttribute(new Attribute("PolicySetId", policySetId));
			policySet
					.setAttribute(new Attribute("PolicyCombiningAlgId",
							"urn:oasis:names:tc:xacml:3.0:rule-combining-algorithm:permit-overrides"));
			policySet.setAttribute(new Attribute("Version", "1.0"));
			policySet.addContent(new Element("Description")
					.setText("PolicySet for Dynamic Delegation."));
			policySet.addContent(new Element("Target"));
			Document doc = new Document(policySet);
			CustomXMLOutputProcessor output = new CustomXMLOutputProcessor();
			try {
				output.process(new FileWriter(policyLocation
						+ delegationXMLFileName), Format.getPrettyFormat(), doc);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return xmlFile;
	}

	/***
	 * Creates Dynamic Policy Node
	 * 
	 * @param userProfileID
	 *            Delegator user profile Id
	 * @param delegatorName
	 *            Delegator Name
	 * @param policyLocation
	 *            Policy location Path
	 * @param delegateeId
	 *            Delegatee user profile Id
	 * @param delegateeName
	 *            Delegatee Name
	 * @param departmentName
	 *            Delegatee Department Name
	 * @param positionTitle
	 *            Delegatee Position Title
	 * @param actions
	 *            Delegated Actions
	 * @param delegationId
	 *            Delegation Id
	 * @param fromDate
	 *            Delegation Start Date
	 * @param toDate
	 *            Delegation End Date
	 * @param policySet
	 *            Delegation Policy Set Template
	 * @param doc
	 *            XML Document
	 * @return XML Policy Node
	 */
	private static String createPolicyNode(String userProfileID,
			String delegatorName, String policyLocation, String delegateeId,
			String delegateeName, String departmentName, String positionTitle,
			List<String> actions, String delegationId, final String fromDate,
			final String toDate, Element policySet, Document doc) {
		Element policy = new Element("Policy");
		String policyId = "Dynamic-Delegation-Policy-Rules-For-"
				+ delegateeName + "-of-" + departmentName + "-"
				+ RandomStringUtils.randomAlphanumeric(8);
		policyId = policyId.replaceAll("\\s", "-");
		policy.setAttribute(new Attribute("PolicyId", policyId));
		policy.setAttribute(new Attribute("RuleCombiningAlgId",
				"urn:oasis:names:tc:xacml:3.0:rule-combining-algorithm:permit-overrides"));
		policy.setAttribute(new Attribute("Version", "1.0"));
		policy.addContent(new Element("Description").setText(delegateeName
				+ " of " + departmentName + " with position title "
				+ positionTitle + " is delegated to " + actions + " by "
				+ delegatorName));
		policy.addContent(new Element("PolicyDefaults").setContent(new Element(
				"XPathVersion")
				.setText("http://www.w3.org/TR/1999/REC-xpath-19991116")));
		policy.addContent(new Element("Target"));

		// Rule1 for Editing Signature Part
		addRuleForEditingSignature(delegatorName, delegateeName,
				departmentName, positionTitle, policy);

		// Rule2 for Approving Proposal
		String ruleId = new String();
		String ruleDesc = new String();
		String emailBody = new String();

		ruleId = "DelegatedApproveProposalRule1For-" + positionTitle
				+ "-DelegatedBy-" + delegatorName;
		ruleDesc = delegateeName
				+ " of "
				+ departmentName
				+ " with position title "
				+ positionTitle
				+ " can \"Approve\" a \"Whole Proposal\" when Delegated by "
				+ delegatorName
				+ " with position title \"Department Chair\" and ApprovedByDepartmentChair = READYFORAPPROVAL and where condition check all department chairs are not approved.";
		emailBody = "Hello User,&amp;lt;br/&amp;gt;&amp;lt;br/&amp;gt;The proposal has been approved by Department Chair. Now it is waiting for another Department Chair approval. &amp;lt;br/&amp;gt;&amp;lt;br/&amp;gt;Thank you, &amp;lt;br/&amp;gt; GPMS Team";

		addRuleForApprovingProposal(delegateeId, positionTitle, actions,
				fromDate, toDate, policy, ruleId, ruleDesc, emailBody, "false",
				false, "");

		// Rule3 for Approving Proposal
		ruleId = "DelegatedApproveProposalRule2For-" + positionTitle
				+ "-DelegatedBy-" + delegatorName;
		ruleDesc = delegateeName
				+ " of "
				+ departmentName
				+ " with position title "
				+ positionTitle
				+ " can \"Approve\" a \"Whole Proposal\" when Delegated by "
				+ delegatorName
				+ " with position title \"Department Chair\" and ApprovedByDepartmentChair = READYFORAPPROVAL and where condition check all department chairs are not approved and no IRB is required.";
		emailBody = "Hello User,&lt;br/&gt;&lt;br/&gt;The proposal has been approved by all Department Chairs.&lt;br/&gt;&lt;br/&gt;Thank you, &lt;br/&gt; GPMS Team";
		addRuleForApprovingProposal(delegateeId, positionTitle, actions,
				fromDate, toDate, policy, ruleId, ruleDesc, emailBody, "true",
				true, "false");

		// Rule4 for Approving Proposal
		ruleId = "DelegatedApproveProposalRule3For-" + positionTitle
				+ "-DelegatedBy-" + delegatorName;
		ruleDesc = delegateeName
				+ " of "
				+ departmentName
				+ " with position title "
				+ positionTitle
				+ " can \"Approve\" a \"Whole Proposal\" when Delegated by "
				+ delegatorName
				+ " with position title \"Department Chair\" ApprovedByDepartmentChair = READYFORAPPROVAL and where condition check all department chairs are approved and IRB is required.";
		emailBody = "Hello User,&lt;br/&gt;&lt;br/&gt;The proposal has been approved by all Department Chairs.&lt;br/&gt;&lt;br/&gt;Thank you, &lt;br/&gt; GPMS Team";
		addRuleForApprovingProposal(delegateeId, positionTitle, actions,
				fromDate, toDate, policy, ruleId, ruleDesc, emailBody, "true",
				true, "true");

		// Rule5 for Revocation
		addRuleForRevocation(userProfileID, delegateeName, departmentName,
				positionTitle, delegationId, policy, policyId);

		// Rule6 for Presentation For Associate Chair
		addPresentationRuleForDelegatee(delegateeId, positionTitle, actions,
				fromDate, toDate, policy);

		// Rule7 for Presentation For Department Chair
		addPresentationRuleForDelegator(userProfileID, positionTitle, actions,
				fromDate, toDate, policy);

		// Append Policy to the Root PolicySet
		policySet.addContent(policy);

		CustomXMLOutputProcessor output = new CustomXMLOutputProcessor();
		try {
			output.process(new FileWriter(policyLocation
					+ delegationXMLFileName), Format.getPrettyFormat(), doc);
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println("File Saved!" + policyId);
		return policyId;
	}

	/**
	 * Adds Rule For Editing Signature Section
	 * 
	 * @param delegatorName
	 *            Delegator Name
	 * @param delegateeName
	 *            Delegatee Name
	 * @param departmentName
	 *            Delegatee Department Name
	 * @param positionTitle
	 *            Delegatee Position Title
	 * @param policy
	 *            XACML Policy element
	 */
	private static void addRuleForEditingSignature(String delegatorName,
			String delegateeName, String departmentName, String positionTitle,
			Element policy) {
		String ruleId = "DelegatedEditProposalSectionRuleFor-" + positionTitle
				+ "-DelegatedBy-" + delegatorName;
		String ruleDesc = delegateeName
				+ " of "
				+ departmentName
				+ " with position title "
				+ positionTitle
				+ " can \"Edit\" \"Certification/Signatures\" when Delegated by "
				+ delegatorName
				+ " with position title \"Department Chair\" and ApprovedByDepartmentChair = READYFORAPPROVAL";
		Element rule1 = generateSignatureSectionRule(positionTitle, ruleId,
				ruleDesc);
		policy.addContent(rule1);
	}

	/**
	 * Adds Rule For Approving Proposal when all department chairs are not
	 * approved one chair is trying to approve it
	 * 
	 * @param delegatorName
	 *            Delegator Name
	 * @param delegateeId
	 *            Delegatee user profile Id
	 * @param delegateeName
	 *            Delegatee Name
	 * @param departmentName
	 *            Delegatee Department Name
	 * @param positionTitle
	 *            Delegatee Position Title
	 * @param actions
	 *            Delegated Actions
	 * @param fromDate
	 *            Delegation Start Date
	 * @param toDate
	 *            Delegation End Date
	 * @param policy
	 *            XACML Policy element
	 */
	private static void addRuleForApprovingProposal(String delegateeId,
			String positionTitle, List<String> actions, final String fromDate,
			final String toDate, Element policy, String ruleId,
			String ruleDesc, String emailBody, String signedByAllChairs,
			boolean irbApprovalRequired, String irbApproval) {
		Element rule = generateRuleNoAction(positionTitle, ruleId, ruleDesc);
		addRuleConditions(delegateeId, actions, fromDate, toDate, rule,
				signedByAllChairs, irbApprovalRequired, irbApproval);
		addRuleObligations(emailBody, rule);
		policy.addContent(rule);
	}

	/**
	 * Adds Rule For Revocation
	 * 
	 * @param userProfileID
	 *            Delegator user profile Id
	 * @param delegateeName
	 *            Delegatee Name
	 * @param departmentName
	 *            Delegatee Department Name
	 * @param positionTitle
	 *            Delegatee Position Title
	 * @param delegationId
	 *            Delegation Id
	 * @param policy
	 *            XACML Policy Element
	 * @param policyId
	 *            XACML Policy Id
	 */
	private static void addRuleForRevocation(String userProfileID,
			String delegateeName, String departmentName, String positionTitle,
			String delegationId, Element policy, String policyId) {
		String ruleId = new String();
		String ruleDesc = new String();
		String emailBody = new String();
		ruleId = "Revoke " + policyId + " by Department Chair";
		ruleDesc = "\"Department Chair\" can \"Revoke\" delegation from "
				+ delegateeName + " of " + departmentName
				+ " with position title " + positionTitle;
		emailBody = "Hello User,&lt;br/&gt;&lt;br/&gt;You have been revoked from your delegation. &lt;br/&gt;&lt;br/&gt;Thank you, &lt;br/&gt; GPMS Team";
		Element rule = generateRevokeRule(userProfileID, delegationId,
				policyId, ruleId, ruleDesc);
		addRuleObligationsForRevocation(emailBody, rule);
		policy.addContent(rule);
	}

	/**
	 * @param userProfileID
	 *            Delegator user profile Id
	 * @param delegationId
	 *            Delegation Id
	 * @param policyId
	 *            XML Policy Id
	 * @param ruleId
	 *            XML Policy rule Id
	 * @param ruleDesc
	 *            XML Policy rule description
	 * @return XML Policy Revocation rule
	 */
	private static Element generateRevokeRule(String userProfileID,
			String delegationId, String policyId, String ruleId, String ruleDesc) {
		Element rule5 = new Element("Rule");
		rule5.setAttribute(new Attribute("Effect", "Permit"));

		rule5.setAttribute(new Attribute("RuleId", ruleId
				.replaceAll("\\s", "-")));
		rule5.addContent(new Element("Description").setText(ruleDesc));
		Element allOf5 = new Element("AllOf");
		allOf5.addContent(getMatch("Revoke",
				"urn:oasis:names:tc:xacml:1.0:action:proposal.action",
				"urn:oasis:names:tc:xacml:3.0:attribute-category:action"));
		Element target5 = new Element("Target").addContent(new Element("AnyOf")
				.addContent(allOf5));
		rule5.addContent(target5);
		Element function4 = new Element("Apply").setAttribute("FunctionId",
				"urn:oasis:names:tc:xacml:1.0:function:and");
		Element condition4 = new Element("Condition");
		function4.addContent(getCondition(
				"urn:oasis:names:tc:xacml:1.0:function:string-equal",
				"urn:oasis:names:tc:xacml:1.0:function:string-one-and-only",
				"urn:oasis:names:tc:xacml:3.0:attribute-category:resource",
				"http://www.w3.org/2001/XMLSchema#string",
				"//ak:delegationid/text()", delegationId));
		function4.addContent(getCondition(
				"urn:oasis:names:tc:xacml:1.0:function:string-equal",
				"urn:oasis:names:tc:xacml:1.0:function:string-one-and-only",
				"urn:oasis:names:tc:xacml:3.0:attribute-category:resource",
				"http://www.w3.org/2001/XMLSchema#string",
				"//ak:delegator/ak:id/text()", userProfileID));
		function4.addContent(getCondition(
				"urn:oasis:names:tc:xacml:1.0:function:string-equal",
				"urn:oasis:names:tc:xacml:1.0:function:string-one-and-only",
				"urn:oasis:names:tc:xacml:3.0:attribute-category:resource",
				"http://www.w3.org/2001/XMLSchema#string",
				"//ak:delegationpolicyid/text()", policyId));
		function4.addContent(getCondition(
				"urn:oasis:names:tc:xacml:1.0:function:boolean-equal",
				"urn:oasis:names:tc:xacml:1.0:function:boolean-one-and-only",
				"urn:oasis:names:tc:xacml:3.0:attribute-category:resource",
				"http://www.w3.org/2001/XMLSchema#boolean",
				"//ak:revoked/text()", "false"));
		condition4.addContent(function4);
		rule5.addContent(condition4);
		return rule5;
	}

	/**
	 * Adds Presentation Rule For Delegatee
	 * 
	 * @param delegateeId
	 *            Delegatee user profile Id
	 * @param positionTitle
	 *            Delegatee Position Title
	 * @param actions
	 *            Delegated Actions
	 * @param fromDate
	 *            Delegation Start Date
	 * @param toDate
	 *            Delegation End Date
	 * @param policy
	 *            XML Policy Element
	 */
	private static void addPresentationRuleForDelegatee(String delegateeId,
			String positionTitle, List<String> actions, final String fromDate,
			final String toDate, Element policy) {
		String ruleId;
		String ruleDesc;
		ruleId = actions + "ShowFor" + positionTitle;
		ruleDesc = "'" + positionTitle + "' can see '" + actions
				+ "' button when ApprovedByDepartmentChair = READYFORAPPROVAL";
		String effect = "Permit";
		Element rule6 = generatePresentationRule(effect, delegateeId,
				positionTitle, actions, fromDate, toDate, ruleId, ruleDesc);
		policy.addContent(rule6);
	}

	/**
	 * Adds Presentation Rule For Delegator
	 * 
	 * @param userProfileID
	 *            Delegator user profile Id
	 * @param positionTitle
	 *            Delegator Position Title
	 * @param actions
	 *            Delegated Actions
	 * @param fromDate
	 *            Delegation Start Date
	 * @param toDate
	 *            Delegation End Date
	 * @param policy
	 *            XML Policy Element
	 */
	private static void addPresentationRuleForDelegator(String userProfileID,
			String positionTitle, List<String> actions, final String fromDate,
			final String toDate, Element policy) {
		String ruleId;
		String ruleDesc;
		String effect;
		ruleId = actions + "HideForDepartmentChair";
		ruleDesc = "'Department Chair' can not see '"
				+ actions
				+ "' button when ApprovedByDepartmentChair = READYFORAPPROVAL and Delegated to = "
				+ positionTitle;
		effect = "Deny";
		Element rule7 = generatePresentationRule(effect, userProfileID,
				"Department Chair", actions, fromDate, toDate, ruleId, ruleDesc);
		policy.addContent(rule7);
	}

	/**
	 * Generates Rule With no action attribute
	 * 
	 * @param positionTitle
	 *            Delegator Position Title
	 * @param ruleId
	 *            XML Rule Id
	 * @param ruleDesc
	 *            XML Rule Description
	 * @return
	 */
	private static Element generateRuleNoAction(String positionTitle,
			String ruleId, String ruleDesc) {
		Element rule = new Element("Rule");
		rule.setAttribute(new Attribute("Effect", "Permit"));
		rule.setAttribute(new Attribute("RuleId", ruleId.replaceAll("\\s", "-")));
		rule.addContent(new Element("Description").setText(ruleDesc));
		Element allOf = new Element("AllOf");
		allOf.addContent(getMatch(positionTitle,
				"urn:oasis:names:tc:xacml:1.0:subject:position.title",
				"urn:oasis:names:tc:xacml:1.0:subject-category:access-subject"));
		allOf.addContent(getMatch("Whole Proposal",
				"urn:oasis:names:tc:xacml:1.0:resource:proposal.section",
				"urn:oasis:names:tc:xacml:3.0:attribute-category:resource"));
		allOf.addContent(getMatch(
				"READYFORAPPROVAL",
				"urn:oasis:names:tc:xacml:1.0:resource:ApprovedByDepartmentChair",
				"urn:oasis:names:tc:xacml:3.0:attribute-category:resource"));
		Element target = new Element("Target").addContent(new Element("AnyOf")
				.addContent(allOf));
		rule.addContent(target);
		return rule;
	}

	/**
	 * Generates Rule for Editing Signature Part
	 * 
	 * @param positionTitle
	 *            Delegator Position Title
	 * @param ruleId
	 *            Rule Id
	 * @param ruleDesc
	 *            Description of Rule
	 * @return
	 */
	private static Element generateSignatureSectionRule(String positionTitle,
			String ruleId, String ruleDesc) {
		Element rule = new Element("Rule");
		rule.setAttribute(new Attribute("Effect", "Permit"));

		rule.setAttribute(new Attribute("RuleId", ruleId.replaceAll("\\s", "-")));
		rule.addContent(new Element("Description").setText(ruleDesc));

		Element allOf = new Element("AllOf");
		allOf.addContent(getMatch(positionTitle,
				"urn:oasis:names:tc:xacml:1.0:subject:position.title",
				"urn:oasis:names:tc:xacml:1.0:subject-category:access-subject"));

		allOf.addContent(getMatch("Certification/Signatures",
				"urn:oasis:names:tc:xacml:1.0:resource:proposal.section",
				"urn:oasis:names:tc:xacml:3.0:attribute-category:resource"));

		allOf.addContent(getMatch(
				"READYFORAPPROVAL",
				"urn:oasis:names:tc:xacml:1.0:resource:ApprovedByDepartmentChair",
				"urn:oasis:names:tc:xacml:3.0:attribute-category:resource"));

		allOf.addContent(getMatch("Edit",
				"urn:oasis:names:tc:xacml:1.0:action:proposal.action",
				"urn:oasis:names:tc:xacml:3.0:attribute-category:action"));

		Element target = new Element("Target").addContent(new Element("AnyOf")
				.addContent(allOf));

		rule.addContent(target);
		return rule;
	}

	/**
	 * Generates Presentation Rule
	 * 
	 * @param userProfileId
	 *            Delegator user profile Id
	 * @param positionTitle
	 *            Delegator Position Title
	 * @param actions
	 *            Delegated Actions
	 * @param fromDate
	 *            Delegation Start Date
	 * @param toDate
	 *            Delegation End Date
	 * @param ruleId
	 *            Rule Id
	 * @param ruleDesc
	 *            Description of Rule
	 * @return
	 */
	private static Element generatePresentationRule(String effect,
			String userProfileId, String positionTitle, List<String> actions,
			final String fromDate, final String toDate, String ruleId,
			String ruleDesc) {
		Element rule = new Element("Rule");
		rule.setAttribute(new Attribute("Effect", effect));

		rule.setAttribute(new Attribute("RuleId", ruleId.replaceAll(
				"[\\[\\]\\s\\,]", "")));
		rule.addContent(new Element("Description").setText(ruleDesc));
		Element allOf = new Element("AllOf");
		allOf.addContent(getMatch(positionTitle,
				"urn:oasis:names:tc:xacml:1.0:subject:position.title",
				"urn:oasis:names:tc:xacml:1.0:subject-category:access-subject"));
		Element target = new Element("Target").addContent(new Element("AnyOf")
				.addContent(allOf));
		rule.addContent(target);

		generatePresentationRuleCondition(userProfileId, actions, fromDate,
				toDate, rule);
		return rule;
	}

	/**
	 * Generates Presentation Rule with Condition
	 * 
	 * @param delegateeId
	 *            Delegatee user profile Id
	 * @param actions
	 *            Delegated Actions
	 * @param fromDate
	 *            Delegation Start Date
	 * @param toDate
	 *            Delegation End Date
	 * @param rule
	 *            XACML Rule
	 */
	private static void generatePresentationRuleCondition(String delegateeId,
			List<String> actions, final String fromDate, final String toDate,
			Element rule) {
		Element function = new Element("Apply").setAttribute("FunctionId",
				"urn:oasis:names:tc:xacml:1.0:function:and");
		Element condition = new Element("Condition");
		function.addContent(getConditionActionBag(
				"urn:oasis:names:tc:xacml:1.0:function:string-at-least-one-member-of",
				"urn:oasis:names:tc:xacml:1.0:function:string-bag",
				"urn:oasis:names:tc:xacml:3.0:attribute-category:action",
				"urn:oasis:names:tc:xacml:1.0:action:proposal.action",
				"http://www.w3.org/2001/XMLSchema#string", actions));
		function.addContent(getCondition(
				"urn:oasis:names:tc:xacml:1.0:function:string-equal",
				"urn:oasis:names:tc:xacml:1.0:function:string-one-and-only",
				"urn:oasis:names:tc:xacml:3.0:attribute-category:resource",
				"http://www.w3.org/2001/XMLSchema#string",
				"//ak:approvedbydepartmentchair/text()", "READYFORAPPROVAL"));
		function.addContent(getCondition(
				"urn:oasis:names:tc:xacml:1.0:function:string-equal",
				"urn:oasis:names:tc:xacml:1.0:function:string-one-and-only",
				"urn:oasis:names:tc:xacml:3.0:attribute-category:resource",
				"http://www.w3.org/2001/XMLSchema#string",
				"//ak:authorprofile/ak:userid/text()", delegateeId));
		function.addContent(getCondition(
				"urn:oasis:names:tc:xacml:1.0:function:dateTime-greater-than-or-equal",
				"urn:oasis:names:tc:xacml:1.0:function:dateTime-one-and-only",
				"urn:oasis:names:tc:xacml:3.0:attribute-category:resource",
				"http://www.w3.org/2001/XMLSchema#dateTime",
				"//ak:currentdatetime/text()", fromDate));
		function.addContent(getCondition(
				"urn:oasis:names:tc:xacml:1.0:function:dateTime-less-than-or-equal",
				"urn:oasis:names:tc:xacml:1.0:function:dateTime-one-and-only",
				"urn:oasis:names:tc:xacml:3.0:attribute-category:resource",
				"http://www.w3.org/2001/XMLSchema#dateTime",
				"//ak:currentdatetime/text()", toDate));
		condition.addContent(function);
		rule.addContent(condition);
	}

	/**
	 * Adds Obligations to a Policy Rule
	 * 
	 * @param emailBody
	 *            Email Body Message
	 * @param rule
	 *            XACML Rule
	 */
	private static void addRuleObligations(String emailBody, Element rule) {
		// Obligations
		Element obligationExpression = new Element("ObligationExpressions");
		obligationExpression.addContent(getObligationExpressionAlert(
				"sendAlert", "Permit"));
		obligationExpression.addContent(getObligationExpressionSendEmail(
				"sendEmail", "Permit", emailBody));
		rule.addContent(obligationExpression);
	}

	/***
	 * Generates Obligation Expressions For Sending Email
	 * 
	 * @param obligationId
	 *            Obligation Id
	 * @param fullFillOn
	 *            Obligation FullFill On
	 * @param emailBody
	 *            Email Body Message
	 * @return
	 */
	private static Element getObligationExpressionSendEmail(
			String obligationId, String fullFillOn, String emailBody) {
		Element obligationExpression = new Element("ObligationExpression")
				.setAttribute("ObligationId", obligationId).setAttribute(
						"FulfillOn", fullFillOn);

		obligationExpression.addContent(getObligationAssignmentAttrValue(
				"obligationType", "postobligation"));

		obligationExpression.addContent(getObligationAssignmentAttrValue(
				"emailBody", emailBody));

		obligationExpression.addContent(getObligationAssignmentAttrValue(
				"emailSubject", "Your proposal has been approved by: "));

		obligationExpression.addContent(getObligationAssignmentAttrSelector(
				"authorName",
				"urn:oasis:names:tc:xacml:3.0:attribute-category:resource",
				"http://www.w3.org/2001/XMLSchema#string",
				"//ak:authorprofile/ak:fullname/text()"));

		obligationExpression.addContent(getObligationAssignmentAttrSelector(
				"piEmail",
				"urn:oasis:names:tc:xacml:3.0:attribute-category:resource",
				"http://www.w3.org/2001/XMLSchema#string",
				"//ak:pi/ak:workemail/text()"));

		obligationExpression.addContent(getObligationAssignmentAttrSelector(
				"copisEmail",
				"urn:oasis:names:tc:xacml:3.0:attribute-category:resource",
				"http://www.w3.org/2001/XMLSchema#string",
				"//ak:copi/ak:workemail/text()"));

		obligationExpression.addContent(getObligationAssignmentAttrSelector(
				"seniorsEmail",
				"urn:oasis:names:tc:xacml:3.0:attribute-category:resource",
				"http://www.w3.org/2001/XMLSchema#string",
				"//ak:senior/ak:workemail/text()"));

		obligationExpression.addContent(getObligationAssignmentAttrSelector(
				"chairsEmail",
				"urn:oasis:names:tc:xacml:3.0:attribute-category:resource",
				"http://www.w3.org/2001/XMLSchema#string",
				"//ak:chair/ak:workemail/text()"));

		obligationExpression.addContent(getObligationAssignmentAttrSelector(
				"managersEmail",
				"urn:oasis:names:tc:xacml:3.0:attribute-category:resource",
				"http://www.w3.org/2001/XMLSchema#string",
				"//ak:manager/ak:workemail/text()"));

		obligationExpression.addContent(getObligationAssignmentAttrSelector(
				"irbsEmail",
				"urn:oasis:names:tc:xacml:3.0:attribute-category:resource",
				"http://www.w3.org/2001/XMLSchema#string",
				"//ak:irb/ak:workemail/text()"));

		return obligationExpression;
	}

	/***
	 * Adds Obligations to a Revocation Policy Rule
	 * 
	 * @param emailBody
	 *            Email Body Message
	 * @param rule
	 *            XACML Rule
	 */
	private static void addRuleObligationsForRevocation(String emailBody,
			Element rule) {
		Element obligationExpression = new Element("ObligationExpressions");
		obligationExpression
				.addContent(getObligationExpressionForRevokeSendEmail(
						"sendEmail", "Permit", emailBody));
		rule.addContent(obligationExpression);
	}

	/***
	 * Generates Obligation Expressions For Sending Email during Revocation
	 * 
	 * @param obligationId
	 *            Obligation Id
	 * @param fullFillOn
	 *            Obligation FullFill On
	 * @param emailBody
	 *            Email Body Message
	 * @return
	 */
	private static Element getObligationExpressionForRevokeSendEmail(
			String obligationId, String fullFillOn, String emailBody) {
		Element obligationExpression = new Element("ObligationExpression")
				.setAttribute("ObligationId", obligationId).setAttribute(
						"FulfillOn", fullFillOn);
		obligationExpression.addContent(getObligationAssignmentAttrValue(
				"emailBody", emailBody));
		obligationExpression.addContent(getObligationAssignmentAttrValue(
				"emailSubject", "Your delegation is revoked by: "));
		obligationExpression.addContent(getObligationAssignmentAttrSelector(
				"delegatorName",
				"urn:oasis:names:tc:xacml:3.0:attribute-category:resource",
				"http://www.w3.org/2001/XMLSchema#string",
				"//ak:delegator/ak:fullname/text()"));
		obligationExpression.addContent(getObligationAssignmentAttrSelector(
				"delegatorEmail",
				"urn:oasis:names:tc:xacml:3.0:attribute-category:resource",
				"http://www.w3.org/2001/XMLSchema#string",
				"//ak:delegator/ak:email/text()"));
		obligationExpression.addContent(getObligationAssignmentAttrSelector(
				"delegateeName",
				"urn:oasis:names:tc:xacml:3.0:attribute-category:resource",
				"http://www.w3.org/2001/XMLSchema#string",
				"//ak:delegatee/ak:fullname/text()"));
		obligationExpression.addContent(getObligationAssignmentAttrSelector(
				"delegateeEmail",
				"urn:oasis:names:tc:xacml:3.0:attribute-category:resource",
				"http://www.w3.org/2001/XMLSchema#string",
				"//ak:delegatee/ak:email/text()"));
		return obligationExpression;
	}

	/***
	 * Adds Conditions to a Rule
	 * 
	 * @param delegateeId
	 *            Delegatee user profile Id
	 * @param actions
	 *            Delegated Actions
	 * @param fromDate
	 *            Delegation Start Date
	 * @param toDate
	 *            Delegation End Date
	 * @param rule
	 *            XACML Rule
	 * @param signedByAllChairs
	 *            Value signed by all chairs
	 * @param irbApprovalRequired
	 *            Is irb Approval Required
	 * @param irbApproval
	 *            Value irb Approval
	 */
	private static void addRuleConditions(String delegateeId,
			List<String> actions, final String fromDate, final String toDate,
			Element rule, String signedByAllChairs,
			boolean irbApprovalRequired, String irbApproval) {
		Element function = new Element("Apply").setAttribute("FunctionId",
				"urn:oasis:names:tc:xacml:1.0:function:and");
		Element condition = new Element("Condition");
		function.addContent(getConditionActionBag(
				"urn:oasis:names:tc:xacml:1.0:function:string-at-least-one-member-of",
				"urn:oasis:names:tc:xacml:1.0:function:string-bag",
				"urn:oasis:names:tc:xacml:3.0:attribute-category:action",
				"urn:oasis:names:tc:xacml:1.0:action:proposal.action",
				"http://www.w3.org/2001/XMLSchema#string", actions));
		function.addContent(getCondition(
				"urn:oasis:names:tc:xacml:1.0:function:boolean-equal",
				"urn:oasis:names:tc:xacml:1.0:function:boolean-one-and-only",
				"urn:oasis:names:tc:xacml:3.0:attribute-category:resource",
				"http://www.w3.org/2001/XMLSchema#boolean",
				"//ak:signedByAllChairs/text()", signedByAllChairs));
		if (irbApprovalRequired) {
			function.addContent(getCondition(
					"urn:oasis:names:tc:xacml:1.0:function:boolean-equal",
					"urn:oasis:names:tc:xacml:1.0:function:boolean-one-and-only",
					"urn:oasis:names:tc:xacml:3.0:attribute-category:resource",
					"http://www.w3.org/2001/XMLSchema#boolean",
					"//ak:irbApprovalRequired/text()", irbApproval));
		}
		function.addContent(getCondition(
				"urn:oasis:names:tc:xacml:1.0:function:string-equal",
				"urn:oasis:names:tc:xacml:1.0:function:string-one-and-only",
				"urn:oasis:names:tc:xacml:3.0:attribute-category:resource",
				"http://www.w3.org/2001/XMLSchema#string",
				"//ak:authorprofile/ak:userid/text()", delegateeId));
		function.addContent(getCondition(
				"urn:oasis:names:tc:xacml:1.0:function:dateTime-greater-than-or-equal",
				"urn:oasis:names:tc:xacml:1.0:function:dateTime-one-and-only",
				"urn:oasis:names:tc:xacml:3.0:attribute-category:resource",
				"http://www.w3.org/2001/XMLSchema#dateTime",
				"//ak:currentdatetime/text()", fromDate));
		function.addContent(getCondition(
				"urn:oasis:names:tc:xacml:1.0:function:dateTime-less-than-or-equal",
				"urn:oasis:names:tc:xacml:1.0:function:dateTime-one-and-only",
				"urn:oasis:names:tc:xacml:3.0:attribute-category:resource",
				"http://www.w3.org/2001/XMLSchema#dateTime",
				"//ak:currentdatetime/text()", toDate));
		condition.addContent(function);
		rule.addContent(condition);
	}

	private static Element getMatch(String attrVal, String attrId,
			String attrCategory) {
		Element match = new Element("Match").setAttribute("MatchId",
				"urn:oasis:names:tc:xacml:1.0:function:string-equal");
		match.addContent(new Element("AttributeValue").setAttribute("DataType",
				"http://www.w3.org/2001/XMLSchema#string").setText(attrVal));
		match.addContent(new Element("AttributeDesignator")
				.setAttribute("AttributeId", attrId)
				.setAttribute("Category", attrCategory)
				.setAttribute("DataType",
						"http://www.w3.org/2001/XMLSchema#string")
				.setAttribute("MustBePresent", "false"));
		return match;
	}

	private static Element getCondition(String functionId,
			String compareFunctionId, String attrSelectorCategory,
			String attrSelectorDataType, String attrSelectorPath,
			String attrValue) {
		Element conditionApply = new Element("Apply")
				.setAttribute("FunctionId", functionId)
				.addContent(
						new Element("Apply").setAttribute("FunctionId",
								compareFunctionId)
								.addContent(
										new Element("AttributeSelector")
												.setAttribute("Category",
														attrSelectorCategory)
												.setAttribute("DataType",
														attrSelectorDataType)
												.setAttribute("Path",
														attrSelectorPath)
												.setAttribute("MustBePresent",
														"false")))
				.addContent(
						new Element("AttributeValue").setAttribute("DataType",
								attrSelectorDataType).setText(attrValue));
		return conditionApply;
	}

	@SuppressWarnings("unused")
	private static Element getDateTimeCondition(String functionId,
			String compareFunctionId, String attrSelectorCategory,
			String attrSelectorDataType, String attrDesignatorAttrId,
			String attrValue) {
		Element conditionApply = new Element("Apply")
				.setAttribute("FunctionId", functionId)
				.addContent(
						new Element("Apply").setAttribute("FunctionId",
								compareFunctionId)
								.addContent(
										new Element("AttributeDesignator")
												.setAttribute("Category",
														attrSelectorCategory)
												.setAttribute("DataType",
														attrSelectorDataType)
												.setAttribute("AttributeId",
														attrDesignatorAttrId)
												.setAttribute("MustBePresent",
														"false")))
				.addContent(
						new Element("AttributeValue").setAttribute("DataType",
								attrSelectorDataType).setText(attrValue));
		return conditionApply;
	}

	private static Element getConditionActionBag(String functionId,
			String compareFunctionId, String attrDesignatorCategory,
			String attrDesignatorAttrId, String attrDataType,
			List<String> actions) {
		Element apply = new Element("Apply").setAttribute("FunctionId",
				compareFunctionId);
		for (String action : actions) {
			Element attributVal = new Element("AttributeValue").setAttribute(
					"DataType", attrDataType).setText(action);
			apply.addContent(attributVal);
		}
		Element conditionApply = new Element("Apply")
				.setAttribute("FunctionId", functionId)
				.addContent(apply)
				.addContent(
						new Element("AttributeDesignator")
								.setAttribute("Category",
										attrDesignatorCategory)
								.setAttribute("DataType", attrDataType)
								.setAttribute("AttributeId",
										attrDesignatorAttrId)
								.setAttribute("MustBePresent", "false"));
		return conditionApply;
	}

	private static Element getObligationExpressionAlert(String obligationId,
			String fullFillOn) {
		Element obligationExpression = new Element("ObligationExpression")
				.setAttribute("ObligationId", obligationId).setAttribute(
						"FulfillOn", fullFillOn);
		obligationExpression.addContent(getObligationAssignmentAttrValue(
				"obligationType", "preobligation"));
		obligationExpression.addContent(getObligationAssignmentAttrSelector(
				"signedByCurrentUser",
				"urn:oasis:names:tc:xacml:3.0:attribute-category:resource",
				"http://www.w3.org/2001/XMLSchema#string",
				"//ak:signedByCurrentUser/text()"));
		obligationExpression.addContent(getObligationAssignmentAttrValue(
				"alertMessage", "You need to sign the proposal first!"));
		return obligationExpression;
	}

	private static Element getObligationAssignmentAttrValue(String attrId,
			String attrValue) {
		Element attributeAssignmentExpression = new Element(
				"AttributeAssignmentExpression").setAttribute("AttributeId",
				attrId).addContent(
				new Element("AttributeValue").setAttribute("DataType",
						"http://www.w3.org/2001/XMLSchema#string").setText(
						attrValue));
		return attributeAssignmentExpression;
	}

	private static Element getObligationAssignmentAttrSelector(String attrId,
			String attrSelectorCategory, String attrSelectorDataType,
			String attrSelectorPath) {
		Element attributeAssignmentExpression = new Element(
				"AttributeAssignmentExpression").setAttribute("AttributeId",
				attrId).addContent(
				new Element("AttributeSelector")
						.setAttribute("Category", attrSelectorCategory)
						.setAttribute("DataType", attrSelectorDataType)
						.setAttribute("Path", attrSelectorPath)
						.setAttribute("MustBePresent", "false"));
		return attributeAssignmentExpression;
	}

	public static void deletePolicyIdFromXML(String policyLocation,
			String policyId) {
		SAXBuilder builder = new SAXBuilder();
		File xmlFile = new File(policyLocation + delegationXMLFileName);
		if (xmlFile.exists()) {
			Document doc = null;
			try {
				doc = (Document) builder.build(xmlFile);
			} catch (JDOMException | IOException e) {
				e.printStackTrace();
			}
			Namespace ns = Namespace
					.getNamespace("urn:oasis:names:tc:xacml:3.0:core:schema:wd-17");
			List<Element> policyElements = doc.getRootElement().getChildren(
					"Policy", ns);
			for (Element policy : policyElements) {
				String existingPolicyId = policy.getAttributeValue("PolicyId");
				if (existingPolicyId.equals(policyId)) {
					policy.getParent().removeContent(policy);
					CustomXMLOutputProcessor output = new CustomXMLOutputProcessor();
					try {
						output.process(new FileWriter(policyLocation
								+ delegationXMLFileName),
								Format.getPrettyFormat(), doc);
					} catch (IOException e) {
						e.printStackTrace();
					}
					System.out.println("File Saved Using revocation!");
					break;
				}
			}
		}
	}
}
