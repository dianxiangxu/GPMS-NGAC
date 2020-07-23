package gpms.dao;

import gpms.DAL.MongoDBConnector;
import gpms.dataModel.AdditionalInfo;
import gpms.dataModel.Appendix;
import gpms.dataModel.ApprovalType;
import gpms.dataModel.ArchiveType;
import gpms.dataModel.AuditLog;
import gpms.dataModel.AuditLogCommonInfo;
import gpms.dataModel.AuditLogInfo;
import gpms.dataModel.BaseInfo;
import gpms.dataModel.BaseOptions;
import gpms.dataModel.BasePIEligibilityOptions;
import gpms.dataModel.CollaborationInfo;
import gpms.dataModel.CollegeDepartmentInfo;
import gpms.dataModel.ComplianceInfo;
import gpms.dataModel.ConfidentialInfo;
import gpms.dataModel.ConflictOfInterest;
import gpms.dataModel.CostShareInfo;
import gpms.dataModel.Delegation;
import gpms.dataModel.DeleteType;
import gpms.dataModel.EmailCommonInfo;
import gpms.dataModel.FundingSource;
import gpms.dataModel.GPMSCommonInfo;
import gpms.dataModel.InvestigatorInfo;
import gpms.dataModel.InvestigatorRefAndPosition;
import gpms.dataModel.OSPSectionInfo;
import gpms.dataModel.PositionDetails;
import gpms.dataModel.ProjectInfo;
import gpms.dataModel.ProjectLocation;
import gpms.dataModel.ProjectPeriod;
import gpms.dataModel.ProjectType;
import gpms.dataModel.Proposal;
import gpms.dataModel.ProposalCommonInfo;
import gpms.dataModel.ProposalInfo;
import gpms.dataModel.Recovery;
import gpms.dataModel.RequiredSignaturesInfo;
import gpms.dataModel.SignatureInfo;
import gpms.dataModel.SignatureUserInfo;
import gpms.dataModel.SponsorAndBudgetInfo;
import gpms.dataModel.Status;
import gpms.dataModel.SubmitType;
import gpms.dataModel.TypeOfRequest;
import gpms.dataModel.UniversityCommitments;
import gpms.dataModel.UserAccount;
import gpms.dataModel.UserProfile;
import gpms.dataModel.WithdrawType;
import gpms.policy.PDSOperations;
import gpms.utils.EmailUtil;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.lang3.RandomStringUtils;
import org.bson.types.ObjectId;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.Morphia;
import org.mongodb.morphia.dao.BasicDAO;
import org.mongodb.morphia.query.Query;
//import org.wso2.balana.ObligationResult;
//import org.wso2.balana.ctx.AbstractResult;
//import org.wso2.balana.ctx.Attribute;
//import org.wso2.balana.ctx.AttributeAssignment;
//import org.wso2.balana.ctx.xacml3.Result;
//import org.wso2.balana.xacml3.Attributes;

import com.ebay.xcelite.Xcelite;
import com.ebay.xcelite.sheet.XceliteSheet;
import com.ebay.xcelite.writer.SheetWriter;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.mongodb.MongoClient;
import com.mongodb.MongoException;

import gov.nist.csd.pm.exceptions.PMException;
import gov.nist.csd.pm.pip.graph.Graph;
import gov.nist.csd.pm.pip.graph.GraphSerializer;
import gov.nist.csd.pm.pip.graph.MemGraph;

public class ProposalDAO extends BasicDAO<Proposal, String> {
	private static final String DBNAME = "db_gpms";
	public static final String COLLECTION_NAME = "proposal";

	private static Morphia morphia;
	private static Datastore ds;
	private AuditLog audit = new AuditLog();
	private DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");

	private static Morphia getMorphia() throws UnknownHostException, MongoException {
		if (morphia == null) {
			morphia = new Morphia().map(Proposal.class);
		}
		return morphia;
	}

	@Override
	public Datastore getDatastore() {
		if (ds == null) {
			try {
				ds = getMorphia().createDatastore(MongoDBConnector.getMongo(), DBNAME);
			} catch (UnknownHostException | MongoException e) {
				e.printStackTrace();
			}
		}
		return ds;
	}

	public ProposalDAO(MongoClient mongo, Morphia morphia, String dbName) {
		super(mongo, morphia, dbName);
	}

	public Proposal findProposalByProposalID(ObjectId id) throws UnknownHostException {
		Datastore ds = getDatastore();
		return ds.createQuery(Proposal.class).field("_id").equal(id).get();
	}

	public Proposal findNextProposalWithSameProjectTitle(ObjectId id, String newProjectTitle) {
		Datastore ds = getDatastore();
		Query<Proposal> proposalQuery = ds.createQuery(Proposal.class);
		proposalQuery.and(proposalQuery.criteria("_id").notEqual(id),
				proposalQuery.criteria("project info.project title").equalIgnoreCase(newProjectTitle));
		return proposalQuery.get();
	}

	public Proposal findAnyProposalWithSameProjectTitle(String newProjectTitle) {
		Datastore ds = getDatastore();
		Query<Proposal> proposalQuery = ds.createQuery(Proposal.class);
		proposalQuery.criteria("project info.project title").equalIgnoreCase(newProjectTitle);
		return proposalQuery.get();
	}

	public void saveProposal(Proposal newProposal, UserProfile authorProfile) {
		Datastore ds = getDatastore();
		audit = new AuditLog(authorProfile, "Created proposal by " + authorProfile.getUserAccount().getUserName(),
				new Date());
		newProposal.getAuditLog().add(audit);
		ds.save(newProposal);
	}

	public void updateProposal(Proposal existingProposal, UserProfile authorProfile) {
		Datastore ds = getDatastore();
		audit = new AuditLog(authorProfile, "Updated proposal by " + authorProfile.getUserAccount().getUserName(),
				new Date());
		existingProposal.getAuditLog().add(audit);
		ds.save(existingProposal);
	}

	public boolean updateProposalStatus(Proposal existingProposal, UserProfile authorProfile) {
		Datastore ds = getDatastore();
		boolean isStatusUpdated = false;
		audit = new AuditLog(authorProfile, "Updated proposal by " + authorProfile.getUserAccount().getUserName(),
				new Date());
		existingProposal.getAuditLog().add(audit);
		ds.save(existingProposal);
		isStatusUpdated = true;
		return isStatusUpdated;
	}

	public boolean deleteProposalByAdmin(Proposal proposal, UserProfile authorProfile) {
		Datastore ds = getDatastore();
		proposal.setResearchDirectorDeletion(DeleteType.DELETED);
		proposal.getProposalStatus().clear();
		proposal.getProposalStatus().add(Status.DELETEDBYADMIN);
		AuditLog entry = new AuditLog(authorProfile,
				"Deleted Proposal by " + authorProfile.getUserAccount().getUserName(), new Date());
		proposal.getAuditLog().add(entry);
		ds.save(proposal);
		return true;
	}

	public boolean deleteProposal(Proposal proposal, String proposalRoles, String proposalUserTitle,
			UserProfile authorProfile) {
		Datastore ds = getDatastore();
		if (proposal.getSubmittedByPI() == SubmitType.NOTSUBMITTED && proposal.getDeletedByPI() == DeleteType.NOTDELETED
				&& proposalRoles.equals("PI") && !proposalUserTitle.equals("University Research Director")) {
			proposal.setDeletedByPI(DeleteType.DELETED);
			proposal.getProposalStatus().clear();
			proposal.getProposalStatus().add(Status.DELETEDBYPI);
			AuditLog entry = new AuditLog(authorProfile,
					"Deleted Proposal by " + authorProfile.getUserAccount().getUserName(), new Date());
			proposal.getAuditLog().add(entry);
			ds.save(proposal);
			return true;
		} else if (proposal.getResearchDirectorDeletion() == DeleteType.NOTDELETED
				&& proposal.getResearchDirectorApproval() == ApprovalType.READYFORAPPROVAL
				&& !proposalRoles.equals("PI") && proposalUserTitle.equals("University Research Director")) {
			proposal.setResearchDirectorDeletion(DeleteType.DELETED);
			proposal.setResearchDirectorApproval(ApprovalType.NOTREADYFORAPPROVAL);
			proposal.getProposalStatus().clear();
			proposal.getProposalStatus().add(Status.DELETEDBYRESEARCHDIRECTOR);
			AuditLog entry = new AuditLog(authorProfile,
					"Deleted Proposal by " + authorProfile.getUserAccount().getUserName(), new Date());
			proposal.getAuditLog().add(entry);
			ds.save(proposal);
			return true;
		}
		return false;
	}

	public boolean validateNotEmptyValue(String value) {
		if (!value.equalsIgnoreCase("")) {
			return true;
		} else {
			return false;
		}
	}

	public boolean verifyValidFileExtension(String extension) {
		List<String> list = Arrays.asList("jpg", "png", "gif", "jpeg", "bmp", "png", "pdf", "doc", "docx", "xls",
				"xlsx", "txt");
		if (list.contains(extension)) {
			return true;
		} else {
			return false;
		}
	}

	public boolean verifyValidFileSize(long fileSize) {
		if (fileSize <= 5 * 1024 * 1024) {
			return true;
		} else {
			return false;
		}
	}

	/***
	 * Finds All Proposals For Grid binding
	 * 
	 * @param offset
	 * @param limit
	 * @param proposalInfo
	 * @return
	 * @throws ParseException
	 */
	public List<ProposalInfo> findAllProposalsForGrid(int offset, int limit, ProposalCommonInfo proposalInfo)
			throws ParseException {
		////System.out.println("findAllProposalsForGrid!");

		Query<Proposal> proposalQuery = getUserProposalSearchCriteria(proposalInfo);
		List<Proposal> allCurrentLoginUserProposalsList = proposalQuery.offset(offset - 1).limit(limit)
				.order("-audit log.activity on").asList();
		return getUserProposalGrid(proposalQuery.asList().size(), allCurrentLoginUserProposalsList);
	}

	/***
	 * Gets User Proposal Search Criteria
	 * 
	 * @param proposalInfo
	 * @return
	 * @throws ParseException
	 */
	private Query<Proposal> getUserProposalSearchCriteria(ProposalCommonInfo proposalInfo) throws ParseException {
		////System.out.println("getUserProposalSearchCriteria!");

		Datastore ds = getDatastore();
		Query<Proposal> proposalQuery = ds.createQuery(Proposal.class);
		if (proposalInfo.getProjectTitle() != null) {
			proposalQuery.field("project info.project title").containsIgnoreCase(proposalInfo.getProjectTitle());
		}
		if (proposalInfo.getSubmittedOnFrom() != null && !proposalInfo.getSubmittedOnFrom().isEmpty()) {
			Date receivedOnF = formatter.parse(proposalInfo.getSubmittedOnFrom());
			proposalQuery.field("date submitted").greaterThanOrEq(receivedOnF);
		}
		if (proposalInfo.getSubmittedOnTo() != null && !proposalInfo.getSubmittedOnTo().isEmpty()) {
			Date receivedOnT = formatter.parse(proposalInfo.getSubmittedOnTo());
			proposalQuery.field("date submitted").lessThanOrEq(receivedOnT);
		}
		if (proposalInfo.getTotalCostsFrom() != null && proposalInfo.getTotalCostsFrom() != 0.0) {
			proposalQuery.field("sponsor and budget info.total costs")
					.greaterThanOrEq(proposalInfo.getTotalCostsFrom());
		}
		if (proposalInfo.getTotalCostsTo() != null && proposalInfo.getTotalCostsTo() != 0.0) {
			proposalQuery.field("sponsor and budget info.total costs").lessThanOrEq(proposalInfo.getTotalCostsTo());
		}
		if (proposalInfo.getProposalStatus() != null) {
			proposalQuery.field("proposal status").contains(proposalInfo.getProposalStatus());
		}
		String usernameBy = proposalInfo.getUsernameBy();
		if (usernameBy != null) {
			Query<UserProfile> profileQuery = ds.createQuery(UserProfile.class);
			profileQuery.or(profileQuery.criteria("first name").containsIgnoreCase(usernameBy),
					profileQuery.criteria("middle name").containsIgnoreCase(usernameBy),
					profileQuery.criteria("last name").containsIgnoreCase(usernameBy));
			proposalQuery.or(proposalQuery.criteria("investigator info.pi.user profile").in(profileQuery.asKeyList()),
					proposalQuery.criteria("investigator info.co_pi.user profile").in(profileQuery.asKeyList()),
					proposalQuery.criteria("investigator info.senior personnel.user profile")
							.in(profileQuery.asKeyList()));
		}
		return proposalQuery;
	}

	/***
	 * Gets All details needed for User Proposal Grid binding
	 * 
	 * @param rowTotal
	 * @param allCurrentLoginUserProposalsList
	 * @return
	 */
	private List<ProposalInfo> getUserProposalGrid(int rowTotal, List<Proposal> allCurrentLoginUserProposalsList) {
		////System.out.println("getUserProposalGrid!");

		List<ProposalInfo> proposalsGridInfoList = new ArrayList<ProposalInfo>();
		for (Proposal userProposal : allCurrentLoginUserProposalsList) {
			ProposalInfo proposalGridInfo = new ProposalInfo();
			// Proposal
			proposalGridInfo.setRowTotal(rowTotal);
			proposalGridInfo.setId(userProposal.getId().toString());
			// ProjectInfo
			setProposalProjectInfo(userProposal, proposalGridInfo);

			// SponsorAndBudgetInfo
			setProposalSponsorAndBudgetInfo(userProposal, proposalGridInfo);

			// Proposal Status
			setCurrentProposalStatusAttributes(userProposal, proposalGridInfo);

			// set audit log
			getRecentProposalAuditLog(userProposal, proposalGridInfo);

			setProposalPIInfo(userProposal, proposalGridInfo);

			setPropsalCoPIsInfo(userProposal, proposalGridInfo);

			setProposalSeniorsInfo(userProposal, proposalGridInfo);

			proposalsGridInfoList.add(proposalGridInfo);
		}
		return proposalsGridInfoList;
	}

	/***
	 * Sets Senior Personnels Info for a Proposal
	 * 
	 * @param userProposal
	 * @param proposalGridInfo
	 */
	private void setProposalSeniorsInfo(Proposal userProposal, ProposalInfo proposalGridInfo) {
		List<InvestigatorRefAndPosition> allSeniors = userProposal.getInvestigatorInfo().getSeniorPersonnel();
		for (InvestigatorRefAndPosition senior : allSeniors) {
			String seniorUser = senior.getUserProfileId();
			proposalGridInfo.getSeniorUsers().add(seniorUser);
			if (!proposalGridInfo.getAllUsers().contains(seniorUser)) {
				proposalGridInfo.getAllUsers().add(seniorUser);
			}
		}
	}

	/***
	 * Sets Seniors Co-PIs Info for a Proposal
	 * 
	 * @param userProposal
	 * @param proposalGridInfo
	 */
	private void setPropsalCoPIsInfo(Proposal userProposal, ProposalInfo proposalGridInfo) {
		List<InvestigatorRefAndPosition> allCoPI = userProposal.getInvestigatorInfo().getCo_pi();
		for (InvestigatorRefAndPosition coPI : allCoPI) {
			String coPIUser = coPI.getUserProfileId();
			proposalGridInfo.getCopiUsers().add(coPIUser);
			if (!proposalGridInfo.getAllUsers().contains(coPIUser)) {
				proposalGridInfo.getAllUsers().add(coPIUser);
			}
		}
	}

	/***
	 * Sets PI Info for a Proposal
	 * 
	 * @param userProposal
	 * @param proposalGridInfo
	 */
	private void setProposalPIInfo(Proposal userProposal, ProposalInfo proposalGridInfo) {
		String piUserId = userProposal.getInvestigatorInfo().getPi().getUserProfileId();
		proposalGridInfo.setPiUser(piUserId);
		if (!proposalGridInfo.getAllUsers().contains(piUserId)) {
			proposalGridInfo.getAllUsers().add(piUserId);
		}
	}

	/***
	 * Gets the Recent Proposal AuditLog
	 * 
	 * @param userProposal
	 * @param proposalGridInfo
	 */
	private void getRecentProposalAuditLog(Proposal userProposal, ProposalInfo proposalGridInfo) {
		Date lastAudited = null;
		String lastAuditedBy = new String();
		String lastAuditAction = new String();
		int auditLogCount = userProposal.getAuditLog().size();
		if (userProposal.getAuditLog() != null && auditLogCount != 0) {
			AuditLog auditLog = userProposal.getAuditLog().get(auditLogCount - 1);
			lastAudited = auditLog.getActivityDate();
			lastAuditedBy = auditLog.getUserProfile().getFullName();
			lastAuditAction = auditLog.getAction();
		}
		proposalGridInfo.setLastAudited(lastAudited);
		proposalGridInfo.setLastAuditedBy(lastAuditedBy);
		proposalGridInfo.setLastAuditAction(lastAuditAction);
	}

	/***
	 * Sets Current Proposal Status Attributes
	 * 
	 * @param userProposal
	 * @param proposalGridInfo
	 */
	private void setCurrentProposalStatusAttributes(Proposal userProposal, ProposalInfo proposalGridInfo) {
		for (Status status : userProposal.getProposalStatus()) {
			proposalGridInfo.getProposalStatus().add(status.toString());
		}
		// PI
		proposalGridInfo.setSubmittedByPI(userProposal.getSubmittedByPI());
		proposalGridInfo.setReadyForSubmissionByPI(userProposal.isReadyForSubmissionByPI());
		proposalGridInfo.setDeletedByPI(userProposal.getDeletedByPI());
		// Chair
		proposalGridInfo.setChairApproval(userProposal.getChairApproval());
		// Business Manager
		proposalGridInfo.setBusinessManagerApproval(userProposal.getBusinessManagerApproval());
		// IRB
		proposalGridInfo.setIrbApproval(userProposal.getIrbApproval());
		// Dean
		proposalGridInfo.setDeanApproval(userProposal.getDeanApproval());
		// University Research Administrator
		proposalGridInfo.setResearchAdministratorApproval(userProposal.getResearchAdministratorApproval());
		proposalGridInfo.setResearchAdministratorWithdraw(userProposal.getResearchAdministratorWithdraw());
		proposalGridInfo.setResearchAdministratorSubmission(userProposal.getResearchAdministratorSubmission());
		// University Research Director
		proposalGridInfo.setResearchDirectorApproval(userProposal.getResearchDirectorApproval());
		proposalGridInfo.setResearchDirectorDeletion(userProposal.getResearchDirectorDeletion());
		proposalGridInfo.setResearchDirectorArchived(userProposal.getResearchDirectorArchived());
		proposalGridInfo.setIrbApprovalRequired(userProposal.isIrbApprovalRequired());
		if (userProposal.getDeletedByPI().equals(DeleteType.DELETED)
				|| userProposal.getResearchDirectorDeletion().equals(DeleteType.DELETED)) {
			proposalGridInfo.setDeleted(true);
		}
	}

	/***
	 * Finds All Proposals For Grid Binding
	 * 
	 * @param proposalInfo
	 * @return
	 * @throws ParseException
	 */
	public List<ProposalInfo> findAllProposals(ProposalCommonInfo proposalInfo) throws ParseException {
		Query<Proposal> queryPropsal = getUserProposalSearchCriteria(proposalInfo);
		int rowTotal = queryPropsal.asList().size();
		List<Proposal> allProposals = queryPropsal.order("-audit log.activity on").asList();
		return getUserProposalGrid(rowTotal, allProposals);
	}

	/***
	 * Finds All Proposals For Grid Binding with Paging Size
	 * 
	 * @param offset
	 * @param limit
	 * @param proposalInfo
	 * @param userInfo
	 * @return
	 * @throws ParseException
	 */
	public List<ProposalInfo> findUserProposalGrid(int offset, int limit, ProposalCommonInfo proposalInfo,
			GPMSCommonInfo userInfo) throws ParseException {
		////System.out.println("findUserProposalGrid");
		List<ProposalInfo> proposals = new ArrayList<ProposalInfo>();
		Query<Proposal> proposalQuery = buildSearchQuery(proposalInfo, userInfo);
		//int rowTotal = proposalQuery.asList().size();
		int rowTotal = 0;
		List<Proposal> allProposals = proposalQuery.offset(offset - 1).limit(limit).order("-audit log.activity on")
				.asList();

		
		for (Proposal userProposal : allProposals) {
			if (!PDSOperations.getAccessDecisionInJSONGraph(userProposal.getPolicyGraph(),
					userInfo.getUserName(), "read", "PDSSections")) {
				continue;
			}
			rowTotal++;
		}
		
		for (Proposal userProposal : allProposals) {
			if (!PDSOperations.getAccessDecisionInJSONGraph(userProposal.getPolicyGraph(),
					userInfo.getUserName(), "read", "PDSSections")) {
				continue;
			}
			ProposalInfo proposal = getProposalInfoForGrid(userInfo, rowTotal, userProposal);
			////System.out.println("ROW TOTAL: "+rowTotal);
			proposals.add(proposal);
		}
		return proposals;
	}

	/***
	 * Gets Proposal Info For Grid binding
	 * 
	 * @param userInfo
	 * @param rowTotal
	 * @param userProposal
	 * @return
	 */
	private ProposalInfo getProposalInfoForGrid(GPMSCommonInfo userInfo, int rowTotal, Proposal userProposal) {
		ProposalInfo proposal = new ProposalInfo();
		proposal.setRowTotal(rowTotal);
		proposal.setId(userProposal.getId().toString());
		setProposalProjectInfo(userProposal, proposal);
		setProposalSponsorAndBudgetInfo(userProposal, proposal);
		setCurrentProposalStatusAttributes(userProposal, proposal);
		getRecentProposalAuditLog(userProposal, proposal);
		
		setProposalPIUser(userInfo.getUserProfileID(), userInfo.getUserCollege(), userInfo.getUserDepartment(),
				userInfo.getUserPositionType(), userInfo.getUserPositionTitle(), userProposal, proposal);
		setProposalCoPIUsers(userInfo.getUserProfileID(), userInfo.getUserCollege(), userInfo.getUserDepartment(),
				userInfo.getUserPositionType(), userInfo.getUserPositionTitle(), userProposal, proposal);
		setProposalSeniorUsers(userInfo.getUserProfileID(), userInfo.getUserCollege(), userInfo.getUserDepartment(),
				userInfo.getUserPositionType(), userInfo.getUserPositionTitle(), userProposal, proposal);
		return proposal;
	}

	/***
	 * Sets Proposal Sponsor And Budget Info
	 * 
	 * @param userProposal
	 * @param proposalGridInfo
	 */
	private void setProposalSponsorAndBudgetInfo(Proposal userProposal, ProposalInfo proposalGridInfo) {
		proposalGridInfo.setGrantingAgencies(userProposal.getSponsorAndBudgetInfo().getGrantingAgency());
		proposalGridInfo.setDirectCosts(userProposal.getSponsorAndBudgetInfo().getDirectCosts());
		proposalGridInfo.setFaCosts(userProposal.getSponsorAndBudgetInfo().getFaCosts());
		proposalGridInfo.setTotalCosts(userProposal.getSponsorAndBudgetInfo().getTotalCosts());
		proposalGridInfo.setFaRate(userProposal.getSponsorAndBudgetInfo().getFaRate());
		proposalGridInfo.setDateCreated(userProposal.getDateCreated());
		proposalGridInfo.setDateSubmitted(userProposal.getDateSubmitted());
		proposalGridInfo.setDueDate(userProposal.getProjectInfo().getDueDate());
		proposalGridInfo.setProjectPeriodFrom(userProposal.getProjectInfo().getProjectPeriod().getFrom());
		proposalGridInfo.setProjectPeriodTo(userProposal.getProjectInfo().getProjectPeriod().getTo());
	}

	/***
	 * Sets Proposal Project Info
	 * 
	 * @param userProposal
	 * @param proposalGridInfo
	 */
	private void setProposalProjectInfo(Proposal userProposal, ProposalInfo proposalGridInfo) {
		proposalGridInfo.setProjectTitle(userProposal.getProjectInfo().getProjectTitle());
		ProjectType projectPropsalType = userProposal.getProjectInfo().getProjectType();
		if (projectPropsalType.isResearchBasic()) {
			proposalGridInfo.setProjectType("Research-basic");
		} else if (projectPropsalType.isResearchApplied()) {
			proposalGridInfo.setProjectType("Research-applied");
		} else if (projectPropsalType.isResearchDevelopment()) {
			proposalGridInfo.setProjectType("Research-development");
		} else if (projectPropsalType.isInstruction()) {
			proposalGridInfo.setProjectType("Instruction");
		} else if (projectPropsalType.isOtherSponsoredActivity()) {
			proposalGridInfo.setProjectType("Other sponsored activity");
		}

		TypeOfRequest typeOfRequest = userProposal.getProjectInfo().getTypeOfRequest();
		if (typeOfRequest.isPreProposal()) {
			proposalGridInfo.getTypeOfRequest().add("Pre-proposal");
		} else if (typeOfRequest.isNewProposal()) {
			proposalGridInfo.getTypeOfRequest().add("New proposal");
		} else if (typeOfRequest.isContinuation()) {
			proposalGridInfo.getTypeOfRequest().add("Continuation");
		} else if (typeOfRequest.isSupplement()) {
			proposalGridInfo.getTypeOfRequest().add("Supplement");
		}

		ProjectLocation pl = userProposal.getProjectInfo().getProjectLocation();
		if (pl.isOffCampus()) {
			proposalGridInfo.setProjectLocation("Off-campus");
		} else if (pl.isOnCampus()) {
			proposalGridInfo.setProjectLocation("On-campus");
		}
	}

	/***
	 * Sets Senior Personnel for a Proposal
	 * 
	 * @param userId
	 * @param college
	 * @param department
	 * @param positionType
	 * @param positionTitle
	 * @param userProposal
	 * @param proposal
	 */
	private void setProposalSeniorUsers(String userId, String college, String department, String positionType,
			String positionTitle, Proposal userProposal, ProposalInfo proposal) {
		List<InvestigatorRefAndPosition> allSeniors = userProposal.getInvestigatorInfo().getSeniorPersonnel();
		for (InvestigatorRefAndPosition senior : allSeniors) {
			String seniorUser = senior.getUserProfileId();
			String seniorUserCollege = senior.getCollege();
			String seniorUserDepartment = senior.getDepartment();
			String seniorUserPositionType = senior.getPositionType();
			String seniorUserPositionTitle = senior.getPositionTitle();
			proposal.getSeniorUsers().add(seniorUser);
			if (!proposal.getAllUsers().contains(seniorUser)) {
				proposal.getAllUsers().add(seniorUser);
			}
			if (seniorUser.equals(userId) && seniorUserCollege.equals(college)
					&& seniorUserDepartment.equals(department) && seniorUserPositionType.equals(positionType)
					&& seniorUserPositionTitle.equals(positionTitle)) {
				proposal.getCurrentuserProposalRoles().add("Senior Personnel");
			}
		}
	}

	/***
	 * Sets Co-PIs for a Proposal
	 * 
	 * @param userId
	 * @param college
	 * @param department
	 * @param positionType
	 * @param positionTitle
	 * @param userProposal
	 * @param proposal
	 */
	private void setProposalCoPIUsers(String userId, String college, String department, String positionType,
			String positionTitle, Proposal userProposal, ProposalInfo proposal) {
		List<InvestigatorRefAndPosition> allCoPI = userProposal.getInvestigatorInfo().getCo_pi();
		for (InvestigatorRefAndPosition coPI : allCoPI) {
			String coPIUser = coPI.getUserProfileId();
			String coPIUserCollege = coPI.getCollege();
			String coPIUserDepartment = coPI.getDepartment();
			String coPIUserPositionType = coPI.getPositionType();
			String coPIUserPositionTitle = coPI.getPositionTitle();
			proposal.getCopiUsers().add(coPIUser);
			if (!proposal.getAllUsers().contains(coPIUser)) {
				proposal.getAllUsers().add(coPIUser);
			}
			if (coPIUser.equals(userId) && coPIUserCollege.equals(college) && coPIUserDepartment.equals(department)
					&& coPIUserPositionType.equals(positionType) && coPIUserPositionTitle.equals(positionTitle)) {
				proposal.getCurrentuserProposalRoles().add("Co-PI");
			}
		}
	}

	/***
	 * Sets PI for a Proposal
	 * 
	 * @param userId
	 * @param college
	 * @param department
	 * @param positionType
	 * @param positionTitle
	 * @param userProposal
	 * @param proposal
	 */
	private void setProposalPIUser(String userId, String college, String department, String positionType,
			String positionTitle, Proposal userProposal, ProposalInfo proposal) {
		String piUserId = userProposal.getInvestigatorInfo().getPi().getUserProfileId();
		String piUserCollege = userProposal.getInvestigatorInfo().getPi().getCollege();
		String piUserDepartment = userProposal.getInvestigatorInfo().getPi().getDepartment();
		String piUserPositionType = userProposal.getInvestigatorInfo().getPi().getPositionType();
		String piUserPositionTitle = userProposal.getInvestigatorInfo().getPi().getPositionTitle();
		proposal.setPiUser(piUserId);
		if (!proposal.getAllUsers().contains(piUserId)) {
			proposal.getAllUsers().add(piUserId);
		}
		if (piUserId.equals(userId) && piUserCollege.equals(college) && piUserDepartment.equals(department)
				&& piUserPositionType.equals(positionType) && piUserPositionTitle.equals(positionTitle)) {
			proposal.getCurrentuserProposalRoles().add("PI");
		}
	}

	/***
	 * Builds Query based on Search Criteria
	 * 
	 * @param proposalInfo
	 * @param userInfo
	 * @return
	 * @throws ParseException
	 */
	private Query<Proposal> buildSearchQuery(ProposalCommonInfo proposalInfo, GPMSCommonInfo userInfo)
			throws ParseException {
		Datastore ds = getDatastore();
		Query<Proposal> proposalQuery = ds.createQuery(Proposal.class);
		Query<UserProfile> profileQuery = ds.createQuery(UserProfile.class);
		if (proposalInfo.getProjectTitle() != null) {
			proposalQuery.field("project info.project title").containsIgnoreCase(proposalInfo.getProjectTitle());
		}
		if (proposalInfo.getSubmittedOnFrom() != null && !proposalInfo.getSubmittedOnFrom().isEmpty()) {
			Date receivedOnF = formatter.parse(proposalInfo.getSubmittedOnFrom());
			proposalQuery.field("date submitted").greaterThanOrEq(receivedOnF);
		}
		if (proposalInfo.getSubmittedOnTo() != null && !proposalInfo.getSubmittedOnTo().isEmpty()) {
			Date receivedOnT = formatter.parse(proposalInfo.getSubmittedOnTo());
			proposalQuery.field("date submitted").lessThanOrEq(receivedOnT);
		}
		if (proposalInfo.getTotalCostsFrom() != null && proposalInfo.getTotalCostsFrom() != 0.0) {
			proposalQuery.field("sponsor and budget info.total costs")
					.greaterThanOrEq(proposalInfo.getTotalCostsFrom());
		}
		if (proposalInfo.getTotalCostsTo() != null && proposalInfo.getTotalCostsTo() != 0.0) {
			proposalQuery.field("sponsor and budget info.total costs").lessThanOrEq(proposalInfo.getTotalCostsTo());
		}
		if (proposalInfo.getProposalStatus() != null) {
			proposalQuery.field("proposal status").contains(proposalInfo.getProposalStatus());
		}
		if (!userInfo.getUserPositionTitle().equals("IRB")
				&& !userInfo.getUserPositionTitle().equals("University Research Administrator")
				&& !userInfo.getUserPositionTitle().equals("University Research Director")) {
			if (userInfo.getUserPositionTitle().equals("Dean")
					|| userInfo.getUserPositionTitle().equals("Associate Dean")) {
				buildQueryForCollege(userInfo.getUserCollege(), proposalQuery);
			} else if (userInfo.getUserPositionTitle().equals("Business Manager")
					|| userInfo.getUserPositionTitle().equals("Department Administrative Assistant")
					|| userInfo.getUserPositionTitle().equals("Department Chair")
					|| userInfo.getUserPositionTitle().equals("Associate Chair")) {
				buildQueryForCollegeAndDepartment(userInfo.getUserCollege(), userInfo.getUserDepartment(),
						proposalQuery);
			} else {
				buildQueryForCollegeDepartmentAndPosition(userInfo.getUserProfileID(), userInfo.getUserCollege(),
						userInfo.getUserDepartment(), userInfo.getUserPositionType(), userInfo.getUserPositionTitle(),
						proposalQuery);
			}
		}
		if (proposalInfo.getUsernameBy() != null) {
			buildQueryForUserNameBy(proposalInfo.getUsernameBy(), proposalInfo.getUserRole(), proposalQuery,
					profileQuery);
		} else if (proposalInfo.getUsernameBy() == null && proposalInfo.getUserRole() != null) {
			buildQueryForUserProposalRole(proposalInfo.getUserRole(), userInfo.getUserProfileID(), proposalQuery);
		}
		return proposalQuery;
	}

	/***
	 * Builds Query based on User Proposal Role
	 * 
	 * @param userRole
	 * @param userId
	 * @param proposalQuery
	 */
	private void buildQueryForUserProposalRole(String userRole, String userId, Query<Proposal> proposalQuery) {
		switch (userRole) {
		case "PI":
			proposalQuery.criteria("investigator info.pi.user profile id").equal(userId);
			break;
		case "Co-PI":
			proposalQuery.criteria("investigator info.co_pi.user profile id").equal(userId);
			break;

		case "Senior Personnel":
			proposalQuery.criteria("investigator info.senior personnel.user profile id").equal(userId);
			break;

		default:
			break;
		}
	}

	/***
	 * Builds Query based on UserName
	 * 
	 * @param usernameBy
	 * @param userRole
	 * @param proposalQuery
	 * @param profileQuery
	 */
	private void buildQueryForUserNameBy(String usernameBy, String userRole, Query<Proposal> proposalQuery,
			Query<UserProfile> profileQuery) {
		profileQuery.or(profileQuery.criteria("first name").containsIgnoreCase(usernameBy),
				profileQuery.criteria("middle name").containsIgnoreCase(usernameBy),
				profileQuery.criteria("last name").containsIgnoreCase(usernameBy));
		if (userRole != null) {
			switch (userRole) {
			case "PI":
				proposalQuery.criteria("investigator info.pi.user profile").in(profileQuery.asKeyList());
				break;
			case "Co-PI":
				proposalQuery.criteria("investigator info.co_pi.user profile").in(profileQuery.asKeyList());
				break;
			case "Senior Personnel":
				proposalQuery.criteria("investigator info.senior personnel.user profile").in(profileQuery.asKeyList());
				break;
			default:
				break;
			}
		} else {
			proposalQuery.or(proposalQuery.criteria("investigator info.pi.user profile").in(profileQuery.asKeyList()),
					proposalQuery.criteria("investigator info.co_pi.user profile").in(profileQuery.asKeyList()),
					proposalQuery.criteria("investigator info.senior personnel.user profile")
							.in(profileQuery.asKeyList()));
		}
	}

	/***
	 * Builds Query based on College, Department And Position
	 * 
	 * @param userId
	 * @param college
	 * @param department
	 * @param positionType
	 * @param positionTitle
	 * @param proposalQuery
	 */
	private void buildQueryForCollegeDepartmentAndPosition(String userId, String college, String department,
			String positionType, String positionTitle, Query<Proposal> proposalQuery) {
		proposalQuery.or(
				proposalQuery.and(proposalQuery.criteria("investigator info.pi.user profile id").equal(userId),
						proposalQuery.criteria("investigator info.pi.college").equal(college),
						proposalQuery.criteria("investigator info.pi.department").equal(department),
						proposalQuery.criteria("investigator info.pi.position type").equal(positionType),
						proposalQuery.criteria("investigator info.pi.position title").equal(positionTitle)),
				proposalQuery.and(proposalQuery.criteria("investigator info.co_pi.user profile id").equal(userId),
						proposalQuery.criteria("investigator info.co_pi.college").equal(college),
						proposalQuery.criteria("investigator info.co_pi.department").equal(department),
						proposalQuery.criteria("investigator info.co_pi.position type").equal(positionType),
						proposalQuery.criteria("investigator info.co_pi.position title").equal(positionTitle)),
				proposalQuery.and(
						proposalQuery.criteria("investigator info.senior personnel.user profile id").equal(userId),
						proposalQuery.criteria("investigator info.senior personnel.college").equal(college),
						proposalQuery.criteria("investigator info.senior personnel.department").equal(department),
						proposalQuery.criteria("investigator info.senior personnel.position type").equal(positionType),
						proposalQuery.criteria("investigator info.senior personnel.position title")
								.equal(positionTitle)));
	}

	/***
	 * Builds Query based on College And Department
	 * 
	 * @param college
	 * @param department
	 * @param proposalQuery
	 */
	private void buildQueryForCollegeAndDepartment(String college, String department, Query<Proposal> proposalQuery) {
		proposalQuery.and(
				proposalQuery.or(proposalQuery.criteria("investigator info.pi.college").equal(college),
						proposalQuery.criteria("investigator info.co_pi.college").equal(college),
						proposalQuery.criteria("investigator info.senior personnel.college").equal(college)),
				proposalQuery.or(proposalQuery.criteria("investigator info.pi.department").equal(department),
						proposalQuery.criteria("investigator info.co_pi.department").equal(department),
						proposalQuery.criteria("investigator info.senior personnel.department").equal(department)));
	}

	/***
	 * Builds Query based on College
	 * 
	 * @param college
	 * @param proposalQuery
	 */
	private void buildQueryForCollege(String college, Query<Proposal> proposalQuery) {
		proposalQuery.or(proposalQuery.criteria("investigator info.pi.college").equal(college),
				proposalQuery.criteria("investigator info.co_pi.college").equal(college),
				proposalQuery.criteria("investigator info.senior personnel.college").equal(college));
	}

	/***
	 * Finds All User Proposals For Export
	 * 
	 * @param proposalInfo
	 * @param userInfo
	 * @return
	 * @throws ParseException
	 */
	public List<ProposalInfo> findAllUserProposalsForExport(ProposalCommonInfo proposalInfo, GPMSCommonInfo userInfo)
			throws ParseException {
		List<ProposalInfo> proposals = new ArrayList<ProposalInfo>();
		Query<Proposal> proposalQuery = buildSearchQuery(proposalInfo, userInfo);

		int rowTotal = proposalQuery.asList().size();
		List<Proposal> allProposals = proposalQuery.order("-audit log.activity on").asList();

		for (Proposal userProposal : allProposals) {
			if (!PDSOperations.getAccessDecisionInJSONGraph(userProposal.getPolicyGraph(),userInfo.getUserName(), "read",
					 "PDSSections")) {
				continue;
			}
			ProposalInfo proposal = getProposalInfoForGrid(userInfo, rowTotal, userProposal);
			proposals.add(proposal);
		}
		return proposals;
	}

	/***
	 * Gets all Signatures of a Proposal
	 * 
	 * @param id
	 * @param irbApprovalRequired
	 * @return
	 * @throws ParseException
	 */
	public List<SignatureInfo> getSignaturesOfAProposal(ObjectId id, boolean irbApprovalRequired, String graph)
			throws ParseException {
		Datastore ds = getDatastore();
		List<SignatureInfo> signatures = new ArrayList<SignatureInfo>();
		List<CollegeDepartmentInfo> investigators = new ArrayList<CollegeDepartmentInfo>();

		Query<Proposal> q1 = ds.createQuery(Proposal.class).field("_id").equal(id).retrievedFields(true, "_id",
				"investigator info", "signature info", "chair approval", "business manager approval", "dean approval");

		Proposal proposal = q1.get();
		final List<SignatureInfo> proposalSignatures = proposal.getSignatureInfo();

//		InvestigatorRefAndPosition PI = proposal.getInvestigatorInfo().getPi();
//		addPIAndCoPISignaturesToSignatureList(signatures, investigators, proposalSignatures, PI, "PI");
//		for (InvestigatorRefAndPosition coPI : proposal.getInvestigatorInfo().getCo_pi()) {
//			addPIAndCoPISignaturesToSignatureList(signatures, investigators, proposalSignatures, coPI, "Co-PI");
//		}
		Graph memGraph = new MemGraph();

		List<UserProfile> userProfiles = new ArrayList<UserProfile>();
		try {
			GraphSerializer.fromJson(memGraph, graph);
			addPIAndCoPISignaturesToSignatureList(signatures, investigators, proposalSignatures, proposal
					.getInvestigatorInfo().getPIByName(PDSOperations.getUserChildrenInGraph("PI", memGraph).get(0)),
					"PI");
			for (String userName : PDSOperations.getUserChildrenInGraph("CoPI", memGraph)) {
				////System.out.println(userName);
				addPIAndCoPISignaturesToSignatureList(signatures, investigators, proposalSignatures,
						proposal.getInvestigatorInfo().getCoPIByName(userName), "Co-PI");
			}

//getPIByName
//		List<String> positions = new ArrayList<String>();
//		positions.add("Department Chair");
//		positions.add("Business Manager");
//		positions.add("Dean");
//		positions.add("University Research Administrator");
//		positions.add("University Research Director");
//
//		if (irbApprovalRequired) {
//			positions.add("IRB");
//		}
//
//		Query<UserProfile> profileQuery = ds.createQuery(UserProfile.class).retrievedFields(true, "_id", "first name",
//				"middle name", "last name", "details");
//		profileQuery.and(profileQuery.criteria("deleted").equal(false),
//				profileQuery.criteria("details.position title").in(positions));
//		List<UserProfile> userProfile = profileQuery.asList();

			List<String> adminUAs = PDSOperations.getAcademicAdminUserAttributes();

			for (String parent : adminUAs) {
				List<String> adminUserNames = PDSOperations.getUserChildrenInGraph(parent, memGraph);
				for (String userName : adminUserNames) {
					userProfiles.add(getUserProfileByName(userName));
				}
			}
			for (String userName : PDSOperations.getAdministrationAdminUsers()) {
				userProfiles.add(getUserProfileByName(userName));
			}

		} catch (PMException e) {
			e.printStackTrace();
		}

		for (UserProfile user : userProfiles) {
			for (PositionDetails posDetails : user.getDetails()) {
				for (CollegeDepartmentInfo colDeptInfo : investigators) {
					addUserSignaturesWithPotentialDelegation(id, signatures, proposal, proposalSignatures, user,
							posDetails, colDeptInfo);
				}
				if (posDetails.getPositionTitle().equalsIgnoreCase("IRB") && irbApprovalRequired) {
					addAdminSignaturesWithoutDelegation(signatures, proposalSignatures, user,
							posDetails.getPositionTitle());
				} else if (posDetails.getPositionTitle().equalsIgnoreCase("University Research Administrator")) {
					addAdminSignaturesWithoutDelegation(signatures, proposalSignatures, user,
							posDetails.getPositionTitle());
				} else if (posDetails.getPositionTitle().equalsIgnoreCase("University Research Director")) {
					addAdminSignaturesWithoutDelegation(signatures, proposalSignatures, user,
							posDetails.getPositionTitle());
				}
			}
		}
		return signatures;
	}

	private UserProfile getUserProfileByName(String userName) {
		Query<UserProfile> profileQueryNew = ds.createQuery(UserProfile.class);
		Query<UserAccount> accountQuery = ds.createQuery(UserAccount.class);
		accountQuery.criteria("username").equalIgnoreCase(userName);
		profileQueryNew.criteria("user id").in(accountQuery.asKeyList());
		return profileQueryNew.get();
	}

	/***
	 * Adds Admin Users' Signatures Without Delegation
	 * 
	 * @param signatures
	 * @param proposalSignatures
	 * @param user
	 * @param adminPositionTitle
	 */
	private void addAdminSignaturesWithoutDelegation(List<SignatureInfo> signatures,
			final List<SignatureInfo> proposalSignatures, UserProfile user, String adminPositionTitle) {
		SignatureInfo signatureInfo = new SignatureInfo();

		boolean isAlreadySigned = false;
		for (SignatureInfo signature : proposalSignatures) {
			if (user.getId().toString().equals(signature.getUserProfileId())
					&& signature.getPositionTitle().equals(adminPositionTitle)) {
				signatureInfo = getUserSignatureInfo(signature.getUserProfileId(), signature.getFullName(),
						signature.getSignature(), signature.getSignedDate(), signature.getNote(),
						signature.getPositionTitle(), signature.isDelegated());

				if (!signatures.contains(signatureInfo)) {
					signatures.add(signatureInfo);
				}
				isAlreadySigned = true;
			}
		}
		if (!isAlreadySigned) {
			signatureInfo = getUserSignatureInfo(user.getId().toString(), user.getFullName(), "", null, "",
					adminPositionTitle, false);

			if (!signatures.contains(signatureInfo)) {
				signatures.add(signatureInfo);
			}
		}
	}

	/***
	 * Adds Admin Users' Signatures With Potential Delegation
	 * 
	 * @param id
	 * @param signatures
	 * @param proposal
	 * @param proposalSignatures
	 * @param user
	 * @param posDetails
	 * @param colDeptInfo
	 */
	private void addUserSignaturesWithPotentialDelegation(ObjectId id, List<SignatureInfo> signatures,
			Proposal proposal, final List<SignatureInfo> proposalSignatures, UserProfile user,
			PositionDetails posDetails, CollegeDepartmentInfo colDeptInfo) {
		if (posDetails.getCollege().equalsIgnoreCase(colDeptInfo.getCollege())
				&& posDetails.getDepartment().equalsIgnoreCase(colDeptInfo.getDepartment())
				&& posDetails.getPositionTitle().equalsIgnoreCase("Department Chair")) {
			addAdminUsersSignaturesToSignatureList(id, signatures, proposal, proposalSignatures, user, posDetails);
		} else if (posDetails.getCollege().equalsIgnoreCase(colDeptInfo.getCollege())
				&& posDetails.getDepartment().equalsIgnoreCase(colDeptInfo.getDepartment())
				&& posDetails.getPositionTitle().equalsIgnoreCase("Business Manager")) {
			addAdminUsersSignaturesToSignatureList(id, signatures, proposal, proposalSignatures, user, posDetails);
		} else if (posDetails.getCollege().equalsIgnoreCase(colDeptInfo.getCollege())
				&& posDetails.getPositionTitle().equalsIgnoreCase("Dean")) {
			addAdminUsersSignaturesToSignatureList(id, signatures, proposal, proposalSignatures, user, posDetails);
		}
	}

	/***
	 * Adds Admin Users' Signatures
	 * 
	 * @param id
	 * @param signatures
	 * @param proposal
	 * @param proposalSignatures
	 * @param user
	 * @param posDetails
	 * @param adminType
	 */
	private void addAdminUsersSignaturesToSignatureList(ObjectId id, List<SignatureInfo> signatures, Proposal proposal,
			final List<SignatureInfo> proposalSignatures, UserProfile user, PositionDetails posDetails) {
		ApprovalType userApproval = null;
		String adminType = posDetails.getPositionTitle();
		if (adminType.equals("Department Chair")) {
			userApproval = proposal.getChairApproval();
		} else if (adminType.equals("Dean")) {
			userApproval = proposal.getDeanApproval();
		} else if (adminType.equals("Business Manager")) {
			userApproval = proposal.getBusinessManagerApproval();
		}

		SignatureInfo signUserInfo = new SignatureInfo();
		boolean isAlreadySigned = false;

		for (SignatureInfo signature : proposalSignatures) {
			if ((user.getId().toString().equals(signature.getUserProfileId())
					&& signature.getPositionTitle().equals(adminType))
					|| (signature.getPositionTitle().equals(adminType) && userApproval == ApprovalType.APPROVED)) {
				signUserInfo = getUserSignatureInfo(signature.getUserProfileId(), signature.getFullName(),
						signature.getSignature(), signature.getSignedDate(), signature.getNote(),
						signature.getPositionTitle(), signature.isDelegated());
				if (!signatures.contains(signUserInfo)) {
					signatures.add(signUserInfo);
				}
				isAlreadySigned = true;
			}
		}

		if (!isAlreadySigned) {
			if (!isDelegator(user.getId().toString(), posDetails) && userApproval == ApprovalType.READYFORAPPROVAL) {
				signUserInfo = getUserSignatureInfo(user.getId().toString(), user.getFullName(), "", null, "",
						adminType, false);
				if (!signatures.contains(signUserInfo)) {
					signatures.add(signUserInfo);
				}
			} else if (isDelegator(user.getId().toString(), posDetails)) {
				addDelegatedUserSignatures(id, signatures, proposalSignatures, user, posDetails, adminType);
			} else {
				signUserInfo = getUserSignatureInfo(user.getId().toString(), user.getFullName(), "", null, "",
						adminType, false);

				if (!signatures.contains(signUserInfo)) {
					signatures.add(signUserInfo);
				}
			}
		}
	}

	/***
	 * Adds Delegated Users' Signatures For Admin Users
	 * 
	 * @param id
	 * @param signatures
	 * @param proposalSignatures
	 * @param user
	 * @param posDetails
	 * @param adminType
	 */
	private void addDelegatedUserSignatures(ObjectId id, List<SignatureInfo> signatures,
			final List<SignatureInfo> proposalSignatures, UserProfile user, PositionDetails posDetails,
			String adminType) {
		// here we used Transfer mode of Delegation
		List<SignatureInfo> delegatedChair = findDelegatedUsersForAUser(user.getId(), id.toString(),
				posDetails.getCollege(), posDetails.getDepartment(), posDetails.getPositionType(), adminType);
		boolean delegatedUserAlreadySigned = false;
		for (SignatureInfo delegateeInfo : delegatedChair) {
			delegatedUserAlreadySigned = false;
			for (SignatureInfo signature : proposalSignatures) {
				if (delegateeInfo.getUserProfileId().equals(signature.getUserProfileId())
						&& signature.getPositionTitle().equals(adminType)) {
					delegateeInfo = getUserSignatureInfo(signature.getUserProfileId(), signature.getFullName(),
							signature.getSignature(), signature.getSignedDate(), signature.getNote(),
							signature.getPositionTitle(), signature.isDelegated());
					if (!signatures.contains(delegateeInfo)) {
						signatures.add(delegateeInfo);
					}
					delegatedUserAlreadySigned = true;
				}
			}
			if (!delegatedUserAlreadySigned) {
				if (!signatures.contains(delegateeInfo)) {
					signatures.add(delegateeInfo);
				}
			}
		}
	}

	/***
	 * Adds PI And Co-PIs' Signatures
	 * 
	 * @param signatures
	 * @param investigators
	 * @param proposalSignatures
	 * @param investigatorRefPosition
	 * @param investigatorType
	 */
	private void addPIAndCoPISignaturesToSignatureList(List<SignatureInfo> signatures,
			List<CollegeDepartmentInfo> investigators, final List<SignatureInfo> proposalSignatures,
			InvestigatorRefAndPosition investigatorRefPosition, String investigatorType) {
		boolean isAlreadySigned = false;
		boolean isAlreadyExist = false;
		SignatureInfo signatureInfo = new SignatureInfo();
		for (SignatureInfo signature : proposalSignatures) {
			if (investigatorRefPosition.getUserProfileId().equals(signature.getUserProfileId())
					&& !investigatorRefPosition.getUserRef().isDeleted()
					&& signature.getPositionTitle().equals(investigatorType)) {
				signatureInfo = getUserSignatureInfo(signature.getUserProfileId(), signature.getFullName(),
						signature.getSignature(), signature.getSignedDate(), signature.getNote(),
						signature.getPositionTitle(), signature.isDelegated());
				for (SignatureInfo sign : signatures) {
					if (sign.getUserProfileId().equalsIgnoreCase(signatureInfo.getUserProfileId())) {
						isAlreadyExist = true;
						break;
					}
				}
				if (!isAlreadyExist) {
					signatures.add(signatureInfo);
				}
				isAlreadySigned = true;
			}
		}
		if (!isAlreadySigned && !investigatorRefPosition.getUserRef().isDeleted()) {
			for (SignatureInfo sign : signatures) {
				if (sign.getUserProfileId().equalsIgnoreCase(signatureInfo.getUserProfileId())) {
					isAlreadyExist = true;
					break;
				}
			}
			if (!isAlreadyExist) {
				signatures.add(getUserSignatureInfo(investigatorRefPosition.getUserProfileId().toString(),
						investigatorRefPosition.getUserRef().getFullName(), "", null, "", investigatorType, false));
			}
		}

		CollegeDepartmentInfo investRef = new CollegeDepartmentInfo();
		if (!investigatorRefPosition.getUserRef().isDeleted()) {
			investRef.setCollege(investigatorRefPosition.getCollege());
			investRef.setDepartment(investigatorRefPosition.getDepartment());
			investigators.add(investRef);
		}
	}

	/***
	 * Gets User's Signature details
	 * 
	 * @param userProfileId
	 * @param fullName
	 * @param signature
	 * @param date
	 * @param note
	 * @param positionTitle
	 * @param delegated
	 * @return
	 */
	private SignatureInfo getUserSignatureInfo(String userProfileId, String fullName, String signature, Date date,
			String note, String positionTitle, boolean delegated) {
		SignatureInfo signatureInfo = new SignatureInfo();
		signatureInfo.setUserProfileId(userProfileId);
		signatureInfo.setFullName(fullName);
		signatureInfo.setSignature(signature);
		signatureInfo.setSignedDate(date);
		signatureInfo.setNote(note);
		signatureInfo.setPositionTitle(positionTitle);
		signatureInfo.setDelegated(delegated);
		return signatureInfo;
	}

	/***
	 * Checks if the current User is Delegator or not
	 * 
	 * @param delegatorId
	 * @param posDetails
	 * @return
	 */
	private boolean isDelegator(String delegatorId, PositionDetails posDetails) {
		Datastore ds = getDatastore();
		long delegationCount = ds.createQuery(Delegation.class).field("revoked").equal(false).field("delegator user id")
				.equal(delegatorId).field("delegated college").equal(posDetails.getCollege())
				.field("delegated department").equal(posDetails.getDepartment()).field("delegated position type")
				.equal(posDetails.getPositionType()).field("delegated position title")
				.equal(posDetails.getPositionTitle()).countAll();
		if (delegationCount > 0) {
			return true;
		}
		return false;
	}

	/***
	 * Finds all Delegated Users (delegatee) For A given User
	 * 
	 * @param userId
	 * @param proposalId
	 * @param positionCollege
	 * @param positionDeptartment
	 * @param positionType
	 * @param positionTitle
	 * @return
	 */
	public List<SignatureInfo> findDelegatedUsersForAUser(ObjectId userId, String proposalId, String positionCollege,
			String positionDeptartment, String positionType, String positionTitle) {
		Datastore ds = getDatastore();
		List<SignatureInfo> signatures = new ArrayList<SignatureInfo>();
		Query<Delegation> delegationQuery = ds.createQuery(Delegation.class);
		Query<UserProfile> profileQuery = ds.createQuery(UserProfile.class).field("_id").equal(userId).field("deleted")
				.equal(false).retrievedFields(true, "_id");
		delegationQuery.or(delegationQuery.criteria("delegator user profile").in(profileQuery.asKeyList())
				.criteria("revoked").equal(false).criteria("delegatee college").equal(positionCollege)
				.criteria("delegatee department").equal(positionDeptartment).criteria("delegatee position type")
				.equal(positionType).criteria("delegated position title").equal(positionTitle).criteria("proposal id")
				.equal("").criteria("from").lessThanOrEq(new Date()).criteria("to").greaterThanOrEq(new Date()),
				delegationQuery.criteria("delegator user profile").in(profileQuery.asKeyList()).criteria("revoked")
						.equal(false).criteria("delegatee college").equal(positionCollege)
						.criteria("delegatee department").equal(positionDeptartment).criteria("delegatee position type")
						.equal(positionType).criteria("delegated position title").equal(positionTitle)
						.criteria("proposal id").containsIgnoreCase(proposalId).criteria("from")
						.lessThanOrEq(new Date()).criteria("to").greaterThanOrEq(new Date()));
		List<Delegation> delegates = delegationQuery.asList();
		for (Delegation delegation : delegates) {
			SignatureInfo signature = getUserSignatureInfo(delegation.getDelegateeId(), delegation.getDelegatee(), "",
					null, "", positionTitle, true);
			if (!signatures.contains(signature)) {
				signatures.add(signature);
			}
		}
		return signatures;
	}

	/***
	 * Finds all Delegated Users (delegatee) Signature details
	 * 
	 * @param userId
	 * @param proposalId
	 * @param posDetails
	 * @return
	 */
	public List<SignatureUserInfo> findDelegatedUsersSignatures(ObjectId userId, String proposalId,
			PositionDetails posDetails) {
		Datastore ds = getDatastore();
		List<SignatureUserInfo> signatures = new ArrayList<SignatureUserInfo>();
		Query<Delegation> delegationQuery = ds.createQuery(Delegation.class);
		Query<UserProfile> profileQuery = ds.createQuery(UserProfile.class).field("_id").equal(userId)
				.retrievedFields(true, "_id");
		delegationQuery.or(
				delegationQuery.and(delegationQuery.criteria("delegator user profile").in(profileQuery.asKeyList()),
						delegationQuery.criteria("revoked").equal(false),
						delegationQuery.criteria("delegatee college").equalIgnoreCase(posDetails.getCollege()),
						delegationQuery.criteria("delegatee department").equalIgnoreCase(posDetails.getDepartment()),
						delegationQuery.criteria("delegatee position type")
								.equalIgnoreCase(posDetails.getPositionType()),
						delegationQuery.criteria("delegated position title").equalIgnoreCase(
								posDetails.getPositionTitle()),
						delegationQuery.criteria("proposal id").equal("")).criteria("from").lessThanOrEq(new Date())
						.criteria("to").greaterThanOrEq(new Date()),
				delegationQuery.and(delegationQuery.criteria("delegator user profile").in(profileQuery.asKeyList()),
						delegationQuery.criteria("revoked").equal(false),
						delegationQuery.criteria("delegatee college").equalIgnoreCase(posDetails.getCollege()),
						delegationQuery.criteria("delegatee department").equalIgnoreCase(posDetails.getDepartment()),
						delegationQuery.criteria("delegatee position type")
								.equalIgnoreCase(posDetails.getPositionType()),
						delegationQuery.criteria("delegated position title")
								.equalIgnoreCase(posDetails.getPositionTitle()),
						delegationQuery.criteria("proposal id").containsIgnoreCase(proposalId)).criteria("from")
						.lessThanOrEq(new Date()).criteria("to").greaterThanOrEq(new Date()));
		List<Delegation> delegates = delegationQuery.asList();
		for (Delegation delegation : delegates) {
			ObjectId delegateeUserProfileId = new ObjectId(delegation.getDelegateeId());
			UserProfile user = ds.createQuery(UserProfile.class).field("_id").equal(delegateeUserProfileId)
					.retrievedFields(true, "_id", "first name", "middle name", "last name", "work email", "user id")
					.get();
			addSignaturesToSignatureList(signatures, user, posDetails, true);
		}
		return signatures;
	}

	/***
	 * Finds User Signature Info Except Investigators for Generating Proposal
	 * Content Profile
	 * 
	 * @param id
	 * @param irbApprovalRequired
	 * @return
	 */
	public List<SignatureUserInfo> findSignaturesExceptInvestigator(ObjectId id, Boolean irbApprovalRequired) {
		Datastore ds = getDatastore();
		List<SignatureUserInfo> signatures = new ArrayList<SignatureUserInfo>();
		Query<Proposal> q1 = ds.createQuery(Proposal.class).field("_id").equal(id).retrievedFields(true, "_id",
				"investigator info", "signature info");
		Proposal proposal = q1.get();
		List<CollegeDepartmentInfo> investigators = addInvestigatorsInfo(proposal);
		List<UserProfile> userProfile = findAdminUsersForAProposal(irbApprovalRequired, ds);
		for (UserProfile user : userProfile) {
			for (PositionDetails posDetails : user.getDetails()) {
				for (CollegeDepartmentInfo colDeptInfo : investigators) {
					addSignaturesWithPotentialDelegation(id, signatures, user, posDetails, colDeptInfo);
				}
				if (posDetails.getPositionTitle().equalsIgnoreCase("IRB") && irbApprovalRequired) {
					addSignaturesToSignatureList(signatures, user, posDetails, false);
				} else if (posDetails.getPositionTitle().equalsIgnoreCase("University Research Administrator")) {
					addSignaturesToSignatureList(signatures, user, posDetails, false);
				} else if (posDetails.getPositionTitle().equalsIgnoreCase("University Research Director")) {
					addSignaturesToSignatureList(signatures, user, posDetails, false);
				}
			}
		}
		return signatures;
	}

	/***
	 * Gets all the investigators position details to find out admin users'
	 * positions filtering
	 * 
	 * @param proposal
	 * @return
	 */
	private List<CollegeDepartmentInfo> addInvestigatorsInfo(Proposal proposal) {
		List<CollegeDepartmentInfo> investigators = new ArrayList<CollegeDepartmentInfo>();
		// Adding PI
		InvestigatorRefAndPosition PI = proposal.getInvestigatorInfo().getPi();
		CollegeDepartmentInfo investRef = new CollegeDepartmentInfo();
		if (!PI.getUserRef().isDeleted()) {
			investRef.setCollege(PI.getCollege());
			investRef.setDepartment(PI.getDepartment());
			investigators.add(investRef);
		}
		for (InvestigatorRefAndPosition coPIs : proposal.getInvestigatorInfo().getCo_pi()) {
			// Adding Co-PIs
			if (!coPIs.getUserRef().isDeleted()) {
				investRef = new CollegeDepartmentInfo();
				investRef.setCollege(coPIs.getCollege());
				investRef.setDepartment(coPIs.getDepartment());
				if (!investigators.contains(investRef)) {
					investigators.add(investRef);
				}
			}
		}
		return investigators;
	}

	/***
	 * Finds all Admin Users For A Proposal
	 * 
	 * @param irbApprovalRequired
	 * @param ds
	 * @return
	 */
	private List<UserProfile> findAdminUsersForAProposal(Boolean irbApprovalRequired, Datastore ds) {
		List<String> positions = new ArrayList<String>();
		positions.add("Department Chair");
		positions.add("Business Manager");
		positions.add("Dean");
		positions.add("University Research Administrator");
		positions.add("University Research Director");
		if (irbApprovalRequired) {
			positions.add("IRB");
		}
		Query<UserProfile> profileQuery = ds.createQuery(UserProfile.class).retrievedFields(true, "_id", "first name",
				"middle name", "last name", "details", "work email");
		profileQuery.and(profileQuery.criteria("deleted").equal(false),
				profileQuery.criteria("details.position title").in(positions));
		List<UserProfile> userProfile = profileQuery.asList();
		return userProfile;
	}

	/***
	 * Finds All Users needed to be Notified
	 * 
	 * @param id
	 * @param irbApprovalRequired
	 * @return
	 */
	public List<SignatureUserInfo> findAllUsersToBeNotified(ObjectId id, Boolean irbApprovalRequired) {
		Datastore ds = getDatastore();
		List<SignatureUserInfo> signatures = new ArrayList<SignatureUserInfo>();
		Query<Proposal> q1 = ds.createQuery(Proposal.class).field("_id").equal(id).retrievedFields(true, "_id",
				"investigator info", "signature info");
		Proposal proposal = q1.get();
		List<CollegeDepartmentInfo> investigators = addInvestigatorsInfo(proposal, signatures);
		List<UserProfile> userProfile = findAdminUsersForAProposal(irbApprovalRequired, ds);
		for (UserProfile user : userProfile) {
			for (PositionDetails posDetails : user.getDetails()) {
				for (CollegeDepartmentInfo colDeptInfo : investigators) {
					addSignaturesWithPotentialDelegation(id, signatures, user, posDetails, colDeptInfo);
				}
				if (posDetails.getPositionTitle().equalsIgnoreCase("IRB") && irbApprovalRequired) {
					addSignaturesToSignatureList(signatures, user, posDetails, false);
				} else if (posDetails.getPositionTitle().equalsIgnoreCase("University Research Administrator")) {
					addSignaturesToSignatureList(signatures, user, posDetails, false);
				} else if (posDetails.getPositionTitle().equalsIgnoreCase("University Research Director")) {
					addSignaturesToSignatureList(signatures, user, posDetails, false);
				}
			}
		}
		return signatures;
	}

	/***
	 * Adds Investigators Info of A Proposal
	 * 
	 * @param proposal
	 * @return
	 */
	private List<CollegeDepartmentInfo> addInvestigatorsInfo(Proposal proposal, List<SignatureUserInfo> signatures) {
		List<CollegeDepartmentInfo> investigators = new ArrayList<CollegeDepartmentInfo>();
		// Adding PI
		InvestigatorRefAndPosition PI = proposal.getInvestigatorInfo().getPi();
		CollegeDepartmentInfo investRef = new CollegeDepartmentInfo();
		PositionDetails posDetails = new PositionDetails();
		if (!PI.getUserRef().isDeleted()) {
			investRef.setCollege(PI.getCollege());
			investRef.setDepartment(PI.getDepartment());
			investigators.add(investRef);

			posDetails.setCollege(PI.getCollege());
			posDetails.setDepartment(PI.getDepartment());
			posDetails.setPositionType(PI.getPositionType());
			posDetails.setPositionTitle(PI.getPositionTitle());
			addSignaturesToSignatureList(signatures, PI.getUserRef(), posDetails, false);
		}
		for (InvestigatorRefAndPosition coPI : proposal.getInvestigatorInfo().getCo_pi()) {
			// Adding Co-PIs
			if (!coPI.getUserRef().isDeleted()) {
				investRef = new CollegeDepartmentInfo();
				investRef.setCollege(coPI.getCollege());
				investRef.setDepartment(coPI.getDepartment());
				if (!investigators.contains(investRef)) {
					investigators.add(investRef);
				}

				posDetails.setCollege(coPI.getCollege());
				posDetails.setDepartment(coPI.getDepartment());
				posDetails.setPositionType(coPI.getPositionType());
				posDetails.setPositionTitle(coPI.getPositionTitle());
				addSignaturesToSignatureList(signatures, coPI.getUserRef(), posDetails, false);
			}
		}
		for (InvestigatorRefAndPosition senior : proposal.getInvestigatorInfo().getSeniorPersonnel()) {
			// Adding Seniors No need to add Admin users based on Senior Users
			if (!senior.getUserRef().isDeleted()) {
				posDetails.setCollege(senior.getCollege());
				posDetails.setDepartment(senior.getDepartment());
				posDetails.setPositionType(senior.getPositionType());
				posDetails.setPositionTitle(senior.getPositionTitle());
				addSignaturesToSignatureList(signatures, senior.getUserRef(), posDetails, false);
			}
		}
		return investigators;
	}

	/***
	 * Adds Signatures With Potential Delegation For Admin Users
	 * 
	 * @param id
	 * @param signatures
	 * @param user
	 * @param posDetails
	 * @param colDeptInfo
	 */
	private void addSignaturesWithPotentialDelegation(ObjectId id, List<SignatureUserInfo> signatures, UserProfile user,
			PositionDetails posDetails, CollegeDepartmentInfo colDeptInfo) {
		if (posDetails.getCollege().equalsIgnoreCase(colDeptInfo.getCollege())
				&& posDetails.getDepartment().equalsIgnoreCase(colDeptInfo.getDepartment())
				&& posDetails.getPositionTitle().equalsIgnoreCase("Department Chair")) {
			if (!isDelegator(user.getId().toString(), posDetails)) {
				addSignaturesToSignatureList(signatures, user, posDetails, false);
			} else {
				addSignaturesOfDelegatee(id, signatures, user, posDetails);
			}
		} else if (posDetails.getCollege().equalsIgnoreCase(colDeptInfo.getCollege())
				&& posDetails.getDepartment().equalsIgnoreCase(colDeptInfo.getDepartment())
				&& posDetails.getPositionTitle().equalsIgnoreCase("Business Manager")) {
			if (!isDelegator(user.getId().toString(), posDetails)) {
				addSignaturesToSignatureList(signatures, user, posDetails, false);
			} else {
				addSignaturesOfDelegatee(id, signatures, user, posDetails);
			}
		} else if (posDetails.getCollege().equalsIgnoreCase(colDeptInfo.getCollege())
				&& posDetails.getDepartment().equalsIgnoreCase(colDeptInfo.getDepartment())
				&& posDetails.getPositionTitle().equalsIgnoreCase("Dean")) {
			if (!isDelegator(user.getId().toString(), posDetails)) {
				addSignaturesToSignatureList(signatures, user, posDetails, false);
			} else {
				addSignaturesOfDelegatee(id, signatures, user, posDetails);
			}
		}
	}

	/***
	 * Adds Signature details of Delegatee Users
	 * 
	 * @param id
	 * @param signatures
	 * @param user
	 * @param posDetails
	 * @param adminType
	 */
	private void addSignaturesOfDelegatee(ObjectId id, List<SignatureUserInfo> signatures, UserProfile user,
			PositionDetails posDetails) {
		List<SignatureUserInfo> delegatee = findDelegatedUsersSignatures(user.getId(), id.toString(), posDetails);
		for (SignatureUserInfo delegateeInfo : delegatee) {
			if (!signatures.contains(delegateeInfo)) {
				signatures.add(delegateeInfo);
			}
		}
	}

	/***
	 * Adds Signature details To Signature List
	 * 
	 * @param signatures
	 * @param user
	 * @param posDetails
	 * @param isDelegated
	 */
	private void addSignaturesToSignatureList(List<SignatureUserInfo> signatures, UserProfile user,
			PositionDetails posDetails, boolean isDelegated) {
		SignatureUserInfo signatureInfo = new SignatureUserInfo();
		signatureInfo.setUserProfileId(user.getId().toString());
		signatureInfo.setFullName(user.getFullName());
		signatureInfo.setUserName(user.getUserAccount().getUserName());
		signatureInfo.setEmail(user.getWorkEmails().get(0));
		signatureInfo.setCollege(posDetails.getCollege());
		signatureInfo.setDepartment(posDetails.getDepartment());
		signatureInfo.setPositionType(posDetails.getPositionType());
		signatureInfo.setPositionTitle(posDetails.getPositionTitle());
		signatureInfo.setSignature("");
		signatureInfo.setNote("");
		// signatureInfo.setPositionTitle(adminType);
		signatureInfo.setDelegated(isDelegated);
		if (!signatures.contains(signatureInfo)) {
			signatures.add(signatureInfo);
		}
	}

	// Proposal Details Info

	/***
	 * Gets Proposal Title
	 * 
	 * @param existingProposal
	 * @param proposalID
	 * @param newProjectInfo
	 * @param projectInfo
	 * @throws Exception
	 */
	public void getProposalTitle(Proposal existingProposal, String proposalID, ProjectInfo newProjectInfo,
			JsonNode projectInfo) throws Exception {
		if (projectInfo != null && projectInfo.has("ProjectTitle")) {
			final String proposalTitle = projectInfo.get("ProjectTitle").textValue().trim().replaceAll("\\<[^>]*>", "");
			if (validateNotEmptyValue(proposalTitle)) {
				if (!proposalID.equals("0")) {
					if (!existingProposal.getProjectInfo().getProjectTitle().equals(proposalTitle)) {
						existingProposal.getProjectInfo().setProjectTitle(proposalTitle);
					}
				} else {
					newProjectInfo.setProjectTitle(proposalTitle);
				}
			} else {
				throw new Exception("The Proposal Title can not be Empty");
			}
		}
	}

	/***
	 * Gets Proposal Type
	 * 
	 * @param existingProposal
	 * @param proposalID
	 * @param newProjectInfo
	 * @param projectInfo
	 */
	public void getProposalType(Proposal existingProposal, String proposalID, ProjectInfo newProjectInfo,
			JsonNode projectInfo) {
		if (projectInfo != null && projectInfo.has("ProjectType")) {
			ProjectType projectType = new ProjectType();
			switch (projectInfo.get("ProjectType").textValue()) {
			case "1":
				projectType.setResearchBasic(true);
				break;
			case "2":
				projectType.setResearchApplied(true);
				break;
			case "3":
				projectType.setResearchDevelopment(true);
				break;
			case "4":
				projectType.setInstruction(true);
				break;
			case "5":
				projectType.setOtherSponsoredActivity(true);
				break;
			default:
				break;
			}
			if (!proposalID.equals("0")) {
				if (!existingProposal.getProjectInfo().getProjectType().equals(projectType)) {
					existingProposal.getProjectInfo().setProjectType(projectType);
				}
			} else {
				newProjectInfo.setProjectType(projectType);
			}
		}
	}

	/***
	 * Gets Proposal Type Of Request
	 * 
	 * @param existingProposal
	 * @param proposalID
	 * @param newProjectInfo
	 * @param projectInfo
	 */
	public void getTypeOfRequest(Proposal existingProposal, String proposalID, ProjectInfo newProjectInfo,
			JsonNode projectInfo) {
		if (projectInfo != null && projectInfo.has("TypeOfRequest")) {
			TypeOfRequest typeOfRequest = new TypeOfRequest();
			switch (projectInfo.get("TypeOfRequest").textValue()) {
			case "1":
				typeOfRequest.setPreProposal(true);
				break;
			case "2":
				typeOfRequest.setNewProposal(true);
				break;
			case "3":
				typeOfRequest.setContinuation(true);
				break;
			case "4":
				typeOfRequest.setSupplement(true);
				break;
			default:
				break;
			}
			if (!proposalID.equals("0")) {
				if (!existingProposal.getProjectInfo().getTypeOfRequest().equals(typeOfRequest)) {
					existingProposal.getProjectInfo().setTypeOfRequest(typeOfRequest);
				}
			} else {
				newProjectInfo.setTypeOfRequest(typeOfRequest);
			}
		}
	}

	/***
	 * Gets Project Location
	 * 
	 * @param existingProposal
	 * @param proposalID
	 * @param newProjectInfo
	 * @param projectInfo
	 */
	public void getProjectLocation(Proposal existingProposal, String proposalID, ProjectInfo newProjectInfo,
			JsonNode projectInfo) {
		if (projectInfo != null && projectInfo.has("ProjectLocation")) {
			ProjectLocation projectLocation = new ProjectLocation();
			switch (projectInfo.get("ProjectLocation").textValue()) {
			case "1":
				projectLocation.setOffCampus(true);
				break;
			case "2":
				projectLocation.setOnCampus(true);
				break;
			default:
				break;
			}
			if (!proposalID.equals("0")) {
				if (!existingProposal.getProjectInfo().getProjectLocation().equals(projectLocation)) {
					existingProposal.getProjectInfo().setProjectLocation(projectLocation);
				}
			} else {
				newProjectInfo.setProjectLocation(projectLocation);
			}
		}
	}

	/***
	 * Gets Due Date
	 * 
	 * @param existingProposal
	 * @param proposalID
	 * @param newProjectInfo
	 * @param projectInfo
	 * @throws ParseException
	 * @throws Exception
	 */
	public void getDueDate(Proposal existingProposal, String proposalID, ProjectInfo newProjectInfo,
			JsonNode projectInfo) throws ParseException, Exception {
		if (projectInfo != null && projectInfo.has("DueDate")) {
			Date dueDate = formatter.parse(projectInfo.get("DueDate").textValue().trim().replaceAll("\\<[^>]*>", ""));
			if (validateNotEmptyValue(dueDate.toString())) {
				if (!proposalID.equals("0")) {
					if (!existingProposal.getProjectInfo().getDueDate().equals(dueDate)) {
						existingProposal.getProjectInfo().setDueDate(dueDate);
					}
				} else {
					newProjectInfo.setDueDate(dueDate);
				}
			} else {
				throw new Exception("The Due Date can not be Empty");
			}
		}
	}

	/***
	 * Gets Project Period
	 * 
	 * @param existingProposal
	 * @param proposalID
	 * @param newProjectInfo
	 * @param projectInfo
	 * @throws ParseException
	 * @throws Exception
	 */
	public void getProjectPeriod(Proposal existingProposal, String proposalID, ProjectInfo newProjectInfo,
			JsonNode projectInfo) throws ParseException, Exception {
		ProjectPeriod projectPeriod = new ProjectPeriod();
		if (projectInfo != null && projectInfo.has("ProjectPeriodFrom")) {
			Date periodFrom = formatter
					.parse(projectInfo.get("ProjectPeriodFrom").textValue().trim().replaceAll("\\<[^>]*>", ""));
			if (validateNotEmptyValue(periodFrom.toString())) {
				projectPeriod.setFrom(periodFrom);
			} else {
				throw new Exception("The Project Period From can not be Empty");
			}
		}
		if (projectInfo != null && projectInfo.has("ProjectPeriodTo")) {
			Date periodTo = formatter
					.parse(projectInfo.get("ProjectPeriodTo").textValue().trim().replaceAll("\\<[^>]*>", ""));
			if (validateNotEmptyValue(periodTo.toString())) {
				projectPeriod.setTo(periodTo);
			} else {
				throw new Exception("The Project Period To can not be Empty");
			}
		}
		if (!proposalID.equals("0")) {
			if (!existingProposal.getProjectInfo().getProjectPeriod().equals(projectPeriod)) {
				existingProposal.getProjectInfo().setProjectPeriod(projectPeriod);
			}
		} else {
			newProjectInfo.setProjectPeriod(projectPeriod);
		}
	}

	/***
	 * Gets Project Info
	 * 
	 * @param existingProposal
	 * @param proposalID
	 * @param proposalInfo
	 * @throws Exception
	 * @throws ParseException
	 */
	public void getProjectInfo(Proposal existingProposal, String proposalID, JsonNode proposalInfo)
			throws Exception, ParseException {
		ProjectInfo newProjectInfo = new ProjectInfo();
		if (proposalInfo != null && proposalInfo.has("ProjectInfo")) {
			JsonNode projectInfo = proposalInfo.get("ProjectInfo");
			getProposalTitle(existingProposal, proposalID, newProjectInfo, projectInfo);
			getProposalType(existingProposal, proposalID, newProjectInfo, projectInfo);
			getTypeOfRequest(existingProposal, proposalID, newProjectInfo, projectInfo);
			getProjectLocation(existingProposal, proposalID, newProjectInfo, projectInfo);
			getDueDate(existingProposal, proposalID, newProjectInfo, projectInfo);
			getProjectPeriod(existingProposal, proposalID, newProjectInfo, projectInfo);
		}
		// ProjectInfo
		if (proposalID.equals("0")) {
			existingProposal.setProjectInfo(newProjectInfo);
		}
	}

	/***
	 * Gets Sponsor And Budget Info
	 * 
	 * @param existingProposal
	 * @param proposalID
	 * @param proposalInfo
	 * @throws Exception
	 */
	public void getSponsorAndBudgetInfo(Proposal existingProposal, String proposalID, JsonNode proposalInfo)
			throws Exception {
		SponsorAndBudgetInfo newSponsorAndBudgetInfo = new SponsorAndBudgetInfo();
		if (proposalInfo != null && proposalInfo.has("SponsorAndBudgetInfo")) {
			JsonNode sponsorAndBudgetInfo = proposalInfo.get("SponsorAndBudgetInfo");
			if (sponsorAndBudgetInfo != null && sponsorAndBudgetInfo.has("GrantingAgency")) {
				for (String grantingAgency : sponsorAndBudgetInfo.get("GrantingAgency").textValue().trim()
						.replaceAll("\\<[^>]*>", "").split(", ")) {
					if (validateNotEmptyValue(grantingAgency)) {
						newSponsorAndBudgetInfo.getGrantingAgency().add(grantingAgency);
					} else {
						// throw new Exception( "The Granting Agency can not be Empty");
					}
				}
			}
			if (sponsorAndBudgetInfo != null && sponsorAndBudgetInfo.has("DirectCosts")) {
				final String directCost = sponsorAndBudgetInfo.get("DirectCosts").textValue().trim()
						.replaceAll("\\<[^>]*>", "");
				if (validateNotEmptyValue(directCost)) {
					newSponsorAndBudgetInfo.setDirectCosts(Double.parseDouble(directCost));
				} else {
					// throw new Exception("The Direct Costs can not be Empty");
				}
			}
			if (sponsorAndBudgetInfo != null && sponsorAndBudgetInfo.has("FACosts")) {
				final String FACosts = sponsorAndBudgetInfo.get("FACosts").textValue().trim().replaceAll("\\<[^>]*>",
						"");
				if (validateNotEmptyValue(FACosts)) {
					newSponsorAndBudgetInfo.setFaCosts(Double.parseDouble(FACosts));
				} else {
					// throw new Exception("The FA Costs can not be Empty");
				}
			}
			if (sponsorAndBudgetInfo != null && sponsorAndBudgetInfo.has("TotalCosts")) {
				final String totalCosts = sponsorAndBudgetInfo.get("TotalCosts").textValue().trim()
						.replaceAll("\\<[^>]*>", "");
				if (validateNotEmptyValue(totalCosts)) {
					newSponsorAndBudgetInfo.setTotalCosts(Double.parseDouble(totalCosts));
				} else {
					// throw new Exception("The Total Costs can not be Empty");
				}
			}
			if (sponsorAndBudgetInfo != null && sponsorAndBudgetInfo.has("FARate")) {
				final String FARate = sponsorAndBudgetInfo.get("FARate").textValue().trim().replaceAll("\\<[^>]*>", "");
				if (validateNotEmptyValue(FARate)) {
					newSponsorAndBudgetInfo.setFaRate(Double.parseDouble(FARate));
				} else {
					// throw new Exception("The FA Rate can not be Empty");
				}
			}
		}

		// SponsorAndBudgetInfo
		if (!proposalID.equals("0")) {
			if (!existingProposal.getSponsorAndBudgetInfo().equals(newSponsorAndBudgetInfo)) {
				existingProposal.setSponsorAndBudgetInfo(newSponsorAndBudgetInfo);
			}
		} else {
			existingProposal.setSponsorAndBudgetInfo(newSponsorAndBudgetInfo);
		}
	}

	/***
	 * Gets Cost Share Info
	 * 
	 * @param existingProposal
	 * @param proposalID
	 * @param proposalInfo
	 */
	public void getCostShareInfo(Proposal existingProposal, String proposalID, JsonNode proposalInfo) {
		CostShareInfo newCostShareInfo = new CostShareInfo();
		if (proposalInfo != null && proposalInfo.has("CostShareInfo")) {
			JsonNode costShareInfo = proposalInfo.get("CostShareInfo");
			if (costShareInfo != null && costShareInfo.has("InstitutionalCommitted")) {
				switch (costShareInfo.get("InstitutionalCommitted").textValue()) {
				case "1":
					newCostShareInfo.setInstitutionalCommitted(true);
					break;
				case "2":
					newCostShareInfo.setInstitutionalCommitted(false);
					break;
				default:
					break;
				}
			}

			if (costShareInfo != null && costShareInfo.has("ThirdPartyCommitted")) {
				switch (costShareInfo.get("ThirdPartyCommitted").textValue()) {
				case "1":
					newCostShareInfo.setThirdPartyCommitted(true);
					break;
				case "2":
					newCostShareInfo.setThirdPartyCommitted(false);
					break;
				default:
					break;
				}
			}
		}
		// CostShareInfo
		if (!proposalID.equals("0")) {
			if (!existingProposal.getCostShareInfo().equals(newCostShareInfo)) {
				existingProposal.setCostShareInfo(newCostShareInfo);
			}
		} else {
			existingProposal.setCostShareInfo(newCostShareInfo);
		}
	}

	/***
	 * Gets University Commitments
	 * 
	 * @param existingProposal
	 * @param proposalID
	 * @param proposalInfo
	 */
	public void getUniversityCommitments(Proposal existingProposal, String proposalID, JsonNode proposalInfo) {
		UniversityCommitments newUnivCommitments = new UniversityCommitments();
		if (proposalInfo != null && proposalInfo.has("UnivCommitments")) {
			JsonNode univCommitments = proposalInfo.get("UnivCommitments");
			if (univCommitments != null && univCommitments.has("NewRenovatedFacilitiesRequired")) {
				switch (univCommitments.get("NewRenovatedFacilitiesRequired").textValue()) {
				case "1":
					newUnivCommitments.setNewRenovatedFacilitiesRequired(true);
					break;
				case "2":
					newUnivCommitments.setNewRenovatedFacilitiesRequired(false);
					break;
				default:
					break;
				}
			}
			if (univCommitments != null && univCommitments.has("RentalSpaceRequired")) {
				switch (univCommitments.get("RentalSpaceRequired").textValue()) {
				case "1":
					newUnivCommitments.setRentalSpaceRequired(true);
					break;
				case "2":
					newUnivCommitments.setRentalSpaceRequired(false);
					break;
				default:
					break;
				}
			}
			if (univCommitments != null && univCommitments.has("InstitutionalCommitmentRequired")) {
				switch (univCommitments.get("InstitutionalCommitmentRequired").textValue()) {
				case "1":
					newUnivCommitments.setInstitutionalCommitmentRequired(true);
					break;
				case "2":
					newUnivCommitments.setInstitutionalCommitmentRequired(false);
					break;
				default:
					break;
				}
			}
		}
		// UnivCommitments
		if (!proposalID.equals("0")) {
			if (!existingProposal.getUniversityCommitments().equals(newUnivCommitments)) {
				existingProposal.setUniversityCommitments(newUnivCommitments);
			}
		} else {
			existingProposal.setUniversityCommitments(newUnivCommitments);
		}
	}

	/***
	 * Gets Conflict Of Interest
	 * 
	 * @param existingProposal
	 * @param proposalID
	 * @param proposalInfo
	 */
	public void getConflictOfInterest(Proposal existingProposal, String proposalID, JsonNode proposalInfo) {
		ConflictOfInterest newConflictOfInterest = new ConflictOfInterest();
		if (proposalInfo != null && proposalInfo.has("ConflicOfInterestInfo")) {
			JsonNode conflicOfInterestInfo = proposalInfo.get("ConflicOfInterestInfo");
			if (conflicOfInterestInfo != null && conflicOfInterestInfo.has("FinancialCOI")) {
				switch (conflicOfInterestInfo.get("FinancialCOI").textValue()) {
				case "1":
					newConflictOfInterest.setFinancialCOI(true);
					break;
				case "2":
					newConflictOfInterest.setFinancialCOI(false);
					break;
				default:
					break;
				}
			}
			if (conflicOfInterestInfo != null && conflicOfInterestInfo.has("ConflictDisclosed")) {
				switch (conflicOfInterestInfo.get("ConflictDisclosed").textValue()) {
				case "1":
					newConflictOfInterest.setConflictDisclosed(true);
					break;
				case "2":
					newConflictOfInterest.setConflictDisclosed(false);
					break;
				default:
					break;
				}
			}
			if (conflicOfInterestInfo != null && conflicOfInterestInfo.has("DisclosureFormChange")) {
				switch (conflicOfInterestInfo.get("DisclosureFormChange").textValue()) {
				case "1":
					newConflictOfInterest.setDisclosureFormChange(true);
					break;
				case "2":
					newConflictOfInterest.setDisclosureFormChange(false);
					break;
				default:
					break;
				}
			}
		}
		// ConflicOfInterestInfo
		if (!proposalID.equals("0")) {
			if (!existingProposal.getConflicOfInterest().equals(newConflictOfInterest)) {
				existingProposal.setConflicOfInterest(newConflictOfInterest);
			}
		} else {
			existingProposal.setConflicOfInterest(newConflictOfInterest);
		}
	}

	/***
	 * Gets Additional Info
	 * 
	 * @param existingProposal
	 * @param proposalID
	 * @param proposalInfo
	 */
	public void getAdditionalInfo(Proposal existingProposal, String proposalID, JsonNode proposalInfo) {
		AdditionalInfo newAdditionalInfo = new AdditionalInfo();
		if (proposalInfo != null && proposalInfo.has("AdditionalInfo")) {
			JsonNode additionalInfo = proposalInfo.get("AdditionalInfo");
			if (additionalInfo != null && additionalInfo.has("AnticipatesForeignNationalsPayment")) {
				switch (additionalInfo.get("AnticipatesForeignNationalsPayment").textValue()) {
				case "1":
					newAdditionalInfo.setAnticipatesForeignNationalsPayment(true);
					break;
				case "2":
					newAdditionalInfo.setAnticipatesForeignNationalsPayment(false);
					break;
				default:
					break;
				}
			}
			if (additionalInfo != null && additionalInfo.has("AnticipatesCourseReleaseTime")) {
				switch (additionalInfo.get("AnticipatesCourseReleaseTime").textValue()) {
				case "1":
					newAdditionalInfo.setAnticipatesCourseReleaseTime(true);
					break;
				case "2":
					newAdditionalInfo.setAnticipatesCourseReleaseTime(false);
					break;
				default:
					break;
				}
			}
			if (additionalInfo != null && additionalInfo.has("RelatedToCenterForAdvancedEnergyStudies")) {
				switch (additionalInfo.get("RelatedToCenterForAdvancedEnergyStudies").textValue()) {
				case "1":
					newAdditionalInfo.setRelatedToCenterForAdvancedEnergyStudies(true);
					break;
				case "2":
					newAdditionalInfo.setRelatedToCenterForAdvancedEnergyStudies(false);
					break;
				default:
					break;
				}
			}
		}
		// AdditionalInfo
		if (!proposalID.equals("0")) {
			if (!existingProposal.getAdditionalInfo().equals(newAdditionalInfo)) {
				existingProposal.setAdditionalInfo(newAdditionalInfo);
			}
		} else {
			existingProposal.setAdditionalInfo(newAdditionalInfo);
		}
	}

	/***
	 * Gets Collaboration Info
	 * 
	 * @param existingProposal
	 * @param proposalID
	 * @param proposalInfo
	 * @throws Exception
	 */
	public void getCollaborationInfo(Proposal existingProposal, String proposalID, JsonNode proposalInfo)
			throws Exception {
		CollaborationInfo newCollaborationInfo = new CollaborationInfo();
		if (proposalInfo != null && proposalInfo.has("CollaborationInfo")) {
			JsonNode collaborationInfo = proposalInfo.get("CollaborationInfo");
			if (collaborationInfo != null && collaborationInfo.has("InvolveNonFundedCollab")) {
				switch (collaborationInfo.get("InvolveNonFundedCollab").textValue()) {
				case "1":
					newCollaborationInfo.setInvolveNonFundedCollab(true);
					if (collaborationInfo != null && collaborationInfo.has("Collaborators")) {
						final String collabarationName = collaborationInfo.get("Collaborators").textValue().trim()
								.replaceAll("\\<[^>]*>", "");
						if (validateNotEmptyValue(collabarationName)) {
							newCollaborationInfo.setInvolvedCollaborators(collabarationName);
						} else {
							throw new Exception("Collaborators can not be Empty");
						}
					}
					break;
				case "2":
					newCollaborationInfo.setInvolveNonFundedCollab(false);
					break;
				default:
					break;
				}
			}
		}
		// CollaborationInfo
		if (!proposalID.equals("0")) {
			if (!existingProposal.getCollaborationInfo().equals(newCollaborationInfo)) {
				existingProposal.setCollaborationInfo(newCollaborationInfo);
			}
		} else {
			existingProposal.setCollaborationInfo(newCollaborationInfo);
		}
	}

	/***
	 * Gets Confidential Info
	 * 
	 * @param existingProposal
	 * @param proposalID
	 * @param proposalInfo
	 * @throws Exception
	 */
	public void getConfidentialInfo(Proposal existingProposal, String proposalID, JsonNode proposalInfo)
			throws Exception {
		ConfidentialInfo newConfidentialInfo = new ConfidentialInfo();
		if (proposalInfo != null && proposalInfo.has("ConfidentialInfo")) {
			JsonNode confidentialInfo = proposalInfo.get("ConfidentialInfo");
			if (confidentialInfo != null && confidentialInfo.has("ContainConfidentialInformation")) {
				switch (confidentialInfo.get("ContainConfidentialInformation").textValue()) {
				case "1":
					newConfidentialInfo.setContainConfidentialInformation(true);
					if (confidentialInfo != null && confidentialInfo.has("OnPages")) {
						final String onPages = confidentialInfo.get("OnPages").textValue().trim()
								.replaceAll("\\<[^>]*>", "");
						if (validateNotEmptyValue(onPages)) {
							newConfidentialInfo.setOnPages(onPages);
						} else {
							throw new Exception("The Pages can not be Empty");
						}
					}
					if (confidentialInfo != null && confidentialInfo.has("Patentable")) {
						newConfidentialInfo.setPatentable(confidentialInfo.get("Patentable").booleanValue());
					}
					if (confidentialInfo != null && confidentialInfo.has("Copyrightable")) {
						newConfidentialInfo.setCopyrightable(confidentialInfo.get("Copyrightable").booleanValue());
					}
					break;
				case "2":
					newConfidentialInfo.setContainConfidentialInformation(false);
					break;
				default:
					break;
				}
			}
			if (confidentialInfo != null && confidentialInfo.has("InvolveIntellectualProperty")) {
				switch (confidentialInfo.get("InvolveIntellectualProperty").textValue()) {
				case "1":
					newConfidentialInfo.setInvolveIntellectualProperty(true);
					break;
				case "2":
					newConfidentialInfo.setInvolveIntellectualProperty(false);
					break;
				default:
					break;
				}
			}
		}
		// ConfidentialInfo
		if (!proposalID.equals("0")) {
			if (!existingProposal.getConfidentialInfo().equals(newConfidentialInfo)) {
				existingProposal.setConfidentialInfo(newConfidentialInfo);
			}
		} else {
			existingProposal.setConfidentialInfo(newConfidentialInfo);
		}
	}

	/***
	 * Gets Funding Source
	 * 
	 * @param existingProposal
	 * @param oSPSectionInfo
	 */
	public void getFundingSource(Proposal existingProposal, JsonNode oSPSectionInfo) {
		FundingSource newFundingSource = new FundingSource();
		if (oSPSectionInfo != null && oSPSectionInfo.has("Federal")) {
			newFundingSource.setFederal(oSPSectionInfo.get("Federal").booleanValue());
		}
		if (oSPSectionInfo != null && oSPSectionInfo.has("FederalFlowThrough")) {
			newFundingSource.setFederalFlowThrough(oSPSectionInfo.get("FederalFlowThrough").booleanValue());
		}
		if (oSPSectionInfo != null && oSPSectionInfo.has("StateOfIdahoEntity")) {
			newFundingSource.setStateOfIdahoEntity(oSPSectionInfo.get("StateOfIdahoEntity").booleanValue());
		}
		if (oSPSectionInfo != null && oSPSectionInfo.has("PrivateForProfit")) {
			newFundingSource.setPrivateForProfit(oSPSectionInfo.get("PrivateForProfit").booleanValue());
		}
		if (oSPSectionInfo != null && oSPSectionInfo.has("NonProfitOrganization")) {
			newFundingSource.setNonProfitOrganization(oSPSectionInfo.get("NonProfitOrganization").booleanValue());
		}
		if (oSPSectionInfo != null && oSPSectionInfo.has("NonIdahoStateEntity")) {
			newFundingSource.setNonIdahoStateEntity(oSPSectionInfo.get("NonIdahoStateEntity").booleanValue());
		}
		if (oSPSectionInfo != null && oSPSectionInfo.has("CollegeOrUniversity")) {
			newFundingSource.setCollegeOrUniversity(oSPSectionInfo.get("CollegeOrUniversity").booleanValue());
		}
		if (oSPSectionInfo != null && oSPSectionInfo.has("LocalEntity")) {
			newFundingSource.setLocalEntity(oSPSectionInfo.get("LocalEntity").booleanValue());
		}
		if (oSPSectionInfo != null && oSPSectionInfo.has("NonIdahoLocalEntity")) {
			newFundingSource.setNonIdahoLocalEntity(oSPSectionInfo.get("NonIdahoLocalEntity").booleanValue());
		}
		if (oSPSectionInfo != null && oSPSectionInfo.has("TirbalGovernment")) {
			newFundingSource.setTirbalGovernment(oSPSectionInfo.get("TirbalGovernment").booleanValue());
		}
		if (oSPSectionInfo != null && oSPSectionInfo.has("Foreign")) {
			newFundingSource.setForeign(oSPSectionInfo.get("Foreign").booleanValue());
		}
		// Funding Source
		if (!existingProposal.getOspSectionInfo().getFundingSource().equals(newFundingSource)) {
			existingProposal.getOspSectionInfo().setFundingSource(newFundingSource);
		}
	}

	/***
	 * Gets List Agency
	 * 
	 * @param existingProposal
	 * @param oSPSectionInfo
	 * @throws Exception
	 */
	public void getListAgency(Proposal existingProposal, JsonNode oSPSectionInfo) throws Exception {
		// List Agency
		if (oSPSectionInfo != null && oSPSectionInfo.has("ListAgency")) {
			String agencies = oSPSectionInfo.get("ListAgency").textValue().trim().replaceAll("\\<[^>]*>", "");
			if (validateNotEmptyValue(agencies)) {
				if (!existingProposal.getOspSectionInfo().getListAgency().equals(agencies)) {
					existingProposal.getOspSectionInfo().setListAgency(agencies);
				}
			} else {
				throw new Exception("The Agency List can not be Empty");
			}
		}
	}

	/***
	 * Gets Recovery Details
	 * 
	 * @param existingProposal
	 * @param oSPSectionInfo
	 */
	public void getRecoveryDetails(Proposal existingProposal, JsonNode oSPSectionInfo) {
		Recovery newRecovery = new Recovery();
		if (oSPSectionInfo != null && oSPSectionInfo.has("FullRecovery")) {
			newRecovery.setFullRecovery(oSPSectionInfo.get("FullRecovery").booleanValue());
		}
		if (oSPSectionInfo != null && oSPSectionInfo.has("NoRecoveryNormalSponsorPolicy")) {
			newRecovery.setNoRecoveryNormalSponsorPolicy(
					oSPSectionInfo.get("NoRecoveryNormalSponsorPolicy").booleanValue());
		}
		if (oSPSectionInfo != null && oSPSectionInfo.has("NoRecoveryInstitutionalWaiver")) {
			newRecovery.setNoRecoveryInstitutionalWaiver(
					oSPSectionInfo.get("NoRecoveryInstitutionalWaiver").booleanValue());
		}
		if (oSPSectionInfo != null && oSPSectionInfo.has("LimitedRecoveryNormalSponsorPolicy")) {
			newRecovery.setLimitedRecoveryNormalSponsorPolicy(
					oSPSectionInfo.get("LimitedRecoveryNormalSponsorPolicy").booleanValue());
		}
		if (oSPSectionInfo != null && oSPSectionInfo.has("LimitedRecoveryInstitutionalWaiver")) {
			newRecovery.setLimitedRecoveryInstitutionalWaiver(
					oSPSectionInfo.get("LimitedRecoveryInstitutionalWaiver").booleanValue());
		}
		// Recovery
		if (!existingProposal.getOspSectionInfo().getRecovery().equals(newRecovery)) {
			existingProposal.getOspSectionInfo().setRecovery(newRecovery);
		}
	}

	/***
	 * Gets Base Info
	 * 
	 * @param existingProposal
	 * @param oSPSectionInfo
	 */
	public void getBaseInfo(Proposal existingProposal, JsonNode oSPSectionInfo) {
		BaseInfo newBaseInfo = new BaseInfo();
		if (oSPSectionInfo != null && oSPSectionInfo.has("MTDC")) {
			newBaseInfo.setMtdc(oSPSectionInfo.get("MTDC").booleanValue());
		}
		if (oSPSectionInfo != null && oSPSectionInfo.has("TDC")) {
			newBaseInfo.setTdc(oSPSectionInfo.get("TDC").booleanValue());
		}
		if (oSPSectionInfo != null && oSPSectionInfo.has("TC")) {
			newBaseInfo.setTc(oSPSectionInfo.get("TC").booleanValue());
		}
		if (oSPSectionInfo != null && oSPSectionInfo.has("Other")) {
			newBaseInfo.setOther(oSPSectionInfo.get("Other").booleanValue());
		}
		if (oSPSectionInfo != null && oSPSectionInfo.has("NotApplicable")) {
			newBaseInfo.setNotApplicable(oSPSectionInfo.get("NotApplicable").booleanValue());
		}
		// Base Info
		if (!existingProposal.getOspSectionInfo().getBaseInfo().equals(newBaseInfo)) {
			existingProposal.getOspSectionInfo().setBaseInfo(newBaseInfo);
		}
	}

	/***
	 * Gets Salary Details
	 * 
	 * @param existingProposal
	 * @param newOSPSectionInfo
	 * @param oSPSectionInfo
	 * @throws Exception
	 */
	public void getSalaryDetails(Proposal existingProposal, OSPSectionInfo newOSPSectionInfo, JsonNode oSPSectionInfo)
			throws Exception {
		if (oSPSectionInfo != null && oSPSectionInfo.has("IsPISalaryIncluded")) {
			switch (oSPSectionInfo.get("IsPISalaryIncluded").textValue()) {
			case "1":
				newOSPSectionInfo.setPiSalaryIncluded(true);
				break;
			case "2":
				newOSPSectionInfo.setPiSalaryIncluded(false);
				break;
			default:
				break;
			}
		}
		// PI Salary Included
		if (existingProposal.getOspSectionInfo().isPiSalaryIncluded() != newOSPSectionInfo.isPiSalaryIncluded()) {
			existingProposal.getOspSectionInfo().setPiSalaryIncluded(newOSPSectionInfo.isPiSalaryIncluded());
		}
		if (oSPSectionInfo != null && oSPSectionInfo.has("PISalary")) {
			// PI Salary
			String PISalary = oSPSectionInfo.get("PISalary").textValue().trim().replaceAll("\\<[^>]*>", "");
			if (validateNotEmptyValue(PISalary)) {
				if (existingProposal.getOspSectionInfo().getPiSalary() != Double.parseDouble(PISalary)) {
					existingProposal.getOspSectionInfo().setPiSalary(Double.parseDouble(PISalary));
				}
			} else {
				throw new Exception("The PI Salary can not be Empty");
			}
		}
		if (oSPSectionInfo != null && oSPSectionInfo.has("PIFringe")) {
			// PI Fringe
			String PiFringe = oSPSectionInfo.get("PIFringe").textValue().trim().replaceAll("\\<[^>]*>", "");
			if (validateNotEmptyValue(PiFringe)) {
				if (existingProposal.getOspSectionInfo().getPiFringe() != Double.parseDouble(PiFringe)) {
					existingProposal.getOspSectionInfo().setPiFringe(Double.parseDouble(PiFringe));
				}
			} else {
				throw new Exception("The PI Fringe can not be Empty");
			}
		}
		if (oSPSectionInfo != null && oSPSectionInfo.has("DepartmentId")) {
			// Department Id
			String departmentId = oSPSectionInfo.get("DepartmentId").textValue().trim().replaceAll("\\<[^>]*>", "");
			if (validateNotEmptyValue(departmentId)) {
				if (!existingProposal.getOspSectionInfo().getDepartmentId().equals(departmentId)) {
					existingProposal.getOspSectionInfo().setDepartmentId(departmentId);
				}
			} else {
				throw new Exception("The Department Id can not be Empty");
			}
		}
	}

	/***
	 * Gets Program Details
	 * 
	 * @param existingProposal
	 * @param oSPSectionInfo
	 * @throws Exception
	 */
	public void getProgramDetails(Proposal existingProposal, JsonNode oSPSectionInfo) throws Exception {
		// CFDA No
		if (oSPSectionInfo != null && oSPSectionInfo.has("CFDANo")) {
			String CFDANo = oSPSectionInfo.get("CFDANo").textValue().trim().replaceAll("\\<[^>]*>", "");
			if (validateNotEmptyValue(CFDANo)) {
				if (!existingProposal.getOspSectionInfo().getCfdaNo().equals(CFDANo)) {
					existingProposal.getOspSectionInfo().setCfdaNo(CFDANo);
				}
			} else {
				throw new Exception("The CFDA No can not be Empty");
			}
		}

		// Program No
		if (oSPSectionInfo != null && oSPSectionInfo.has("ProgramNo")) {
			String programNo = oSPSectionInfo.get("ProgramNo").textValue().trim().replaceAll("\\<[^>]*>", "");
			if (validateNotEmptyValue(programNo)) {
				if (!existingProposal.getOspSectionInfo().getProgramNo().equals(programNo)) {
					existingProposal.getOspSectionInfo().setProgramNo(programNo);
				}
			} else {
				throw new Exception("The Program No can not be Empty");
			}
		}

		// Program Title
		if (oSPSectionInfo != null && oSPSectionInfo.has("ProgramTitle")) {
			String programTitle = oSPSectionInfo.get("ProgramTitle").textValue().trim().replaceAll("\\<[^>]*>", "");
			if (validateNotEmptyValue(programTitle)) {
				if (!existingProposal.getOspSectionInfo().getProgramTitle().equals(programTitle)) {
					existingProposal.getOspSectionInfo().setProgramTitle(programTitle);
				}
			} else {
				throw new Exception("The Program Title can not be Empty");
			}
		}
	}

	/***
	 * Gets Institutional Cost Details
	 * 
	 * @param existingProposal
	 * @param oSPSectionInfo
	 */
	public void getInstitutionalCostDetails(Proposal existingProposal, JsonNode oSPSectionInfo) {
		BaseOptions newBaseOptions = new BaseOptions();
		if (oSPSectionInfo != null && oSPSectionInfo.has("InstitutionalCostDocumented")) {
			switch (oSPSectionInfo.get("InstitutionalCostDocumented").textValue()) {
			case "1":
				newBaseOptions.setYes(true);
				break;
			case "2":
				newBaseOptions.setNo(true);
				break;
			case "3":
				newBaseOptions.setNotApplicable(true);
				break;
			default:
				break;
			}
		}
		// Institutional Cost Documented
		if (!existingProposal.getOspSectionInfo().getInstitutionalCostDocumented().equals(newBaseOptions)) {
			existingProposal.getOspSectionInfo().setInstitutionalCostDocumented(newBaseOptions);
		}
		newBaseOptions = new BaseOptions();
		if (oSPSectionInfo != null && oSPSectionInfo.has("ThirdPartyCostDocumented")) {
			switch (oSPSectionInfo.get("ThirdPartyCostDocumented").textValue()) {
			case "1":
				newBaseOptions.setYes(true);
				break;
			case "2":
				newBaseOptions.setNo(true);
				break;
			case "3":
				newBaseOptions.setNotApplicable(true);
				break;
			default:
				break;
			}
		}
		// Third Party Cost Documented
		if (!existingProposal.getOspSectionInfo().getThirdPartyCostDocumented().equals(newBaseOptions)) {
			existingProposal.getOspSectionInfo().setThirdPartyCostDocumented(newBaseOptions);
		}
	}

	/***
	 * Gets Sub-Recipients Details
	 * 
	 * @param existingProposal
	 * @param newOSPSectionInfo
	 * @param oSPSectionInfo
	 * @throws Exception
	 */
	public void getSubRecipientsDetails(Proposal existingProposal, OSPSectionInfo newOSPSectionInfo,
			JsonNode oSPSectionInfo) throws Exception {
		if (oSPSectionInfo != null && oSPSectionInfo.has("IsAnticipatedSubRecipients")) {
			switch (oSPSectionInfo.get("IsAnticipatedSubRecipients").textValue()) {
			case "1":
				newOSPSectionInfo.setAnticipatedSubRecipients(true);
				if (oSPSectionInfo != null && oSPSectionInfo.has("AnticipatedSubRecipientsNames")) {
					final String anticipatedSubRecipients = oSPSectionInfo.get("AnticipatedSubRecipientsNames")
							.textValue().trim().replaceAll("\\<[^>]*>", "");
					if (validateNotEmptyValue(anticipatedSubRecipients)) {
						newOSPSectionInfo.setAnticipatedSubRecipientsNames(anticipatedSubRecipients);
					} else {
						throw new Exception("The Anticipated SubRecipients Names can not be Empty");
					}
				}
				break;
			case "2":
				newOSPSectionInfo.setAnticipatedSubRecipients(false);
				break;
			default:
				break;
			}
		}
		// Is Anticipated SubRecipients
		if (existingProposal.getOspSectionInfo().isAnticipatedSubRecipients() != newOSPSectionInfo
				.isAnticipatedSubRecipients()) {
			existingProposal.getOspSectionInfo()
					.setAnticipatedSubRecipients(newOSPSectionInfo.isAnticipatedSubRecipients());
		}
		// Anticipated SubRecipients Names
		if (existingProposal.getOspSectionInfo().getAnticipatedSubRecipientsNames() != null) {
			if (!existingProposal.getOspSectionInfo().getAnticipatedSubRecipientsNames()
					.equals(newOSPSectionInfo.getAnticipatedSubRecipientsNames())) {
				existingProposal.getOspSectionInfo()
						.setAnticipatedSubRecipientsNames(newOSPSectionInfo.getAnticipatedSubRecipientsNames());
			}
		} else {
			existingProposal.getOspSectionInfo()
					.setAnticipatedSubRecipientsNames(newOSPSectionInfo.getAnticipatedSubRecipientsNames());
		}
	}

	/***
	 * Gets Base PI Eligibility Options
	 * 
	 * @param existingProposal
	 * @param oSPSectionInfo
	 */
	public void getBasePIEligibilityOptions(Proposal existingProposal, JsonNode oSPSectionInfo) {
		BasePIEligibilityOptions newBasePIEligibilityOptions = new BasePIEligibilityOptions();
		if (oSPSectionInfo != null && oSPSectionInfo.has("PIEligibilityWaiver")) {
			switch (oSPSectionInfo.get("PIEligibilityWaiver").textValue()) {
			case "1":
				newBasePIEligibilityOptions.setYes(true);
				break;
			case "2":
				newBasePIEligibilityOptions.setNo(true);
				break;
			case "3":
				newBasePIEligibilityOptions.setNotApplicable(true);
				break;
			case "4":
				newBasePIEligibilityOptions.setThisProposalOnly(true);
				break;
			case "5":
				newBasePIEligibilityOptions.setBlanket(true);
				break;
			default:
				break;
			}
		}
		// Base PI Eligibility Options
		if (!existingProposal.getOspSectionInfo().getPiEligibilityWaiver().equals(newBasePIEligibilityOptions)) {
			existingProposal.getOspSectionInfo().setPiEligibilityWaiver(newBasePIEligibilityOptions);
		}
	}

	/***
	 * Gets Conflict Of Interest Forms
	 * 
	 * @param existingProposal
	 * @param oSPSectionInfo
	 */
	public void getConflictOfInterestForms(Proposal existingProposal, JsonNode oSPSectionInfo) {
		BaseOptions newBaseOptions = new BaseOptions();
		if (oSPSectionInfo != null && oSPSectionInfo.has("ConflictOfInterestForms")) {
			switch (oSPSectionInfo.get("ConflictOfInterestForms").textValue()) {
			case "1":
				newBaseOptions.setYes(true);
				break;
			case "2":
				newBaseOptions.setNo(true);
				break;
			case "3":
				newBaseOptions.setNotApplicable(true);
				break;
			default:
				break;
			}
		}
		// Conflict Of Interest Forms
		if (!existingProposal.getOspSectionInfo().getConflictOfInterestForms().equals(newBaseOptions)) {
			existingProposal.getOspSectionInfo().setConflictOfInterestForms(newBaseOptions);
		}
	}

	/***
	 * Gets Excluded Party List Checked
	 * 
	 * @param existingProposal
	 * @param oSPSectionInfo
	 */
	public void getExcludedPartyListChecked(Proposal existingProposal, JsonNode oSPSectionInfo) {
		BaseOptions newBaseOptions = new BaseOptions();
		if (oSPSectionInfo != null && oSPSectionInfo.has("ExcludedPartyListChecked")) {
			switch (oSPSectionInfo.get("ExcludedPartyListChecked").textValue()) {
			case "1":
				newBaseOptions.setYes(true);
				break;
			case "2":
				newBaseOptions.setNo(true);
				break;
			case "3":
				newBaseOptions.setNotApplicable(true);
				break;
			default:
				break;
			}
		}
		// Excluded Party List Checked
		if (!existingProposal.getOspSectionInfo().getExcludedPartyListChecked().equals(newBaseOptions)) {
			existingProposal.getOspSectionInfo().setExcludedPartyListChecked(newBaseOptions);
		}
	}

	/***
	 * Gets Appendix Details
	 * 
	 * @param proposalId
	 * @param existingProposal
	 * @param oldProposal
	 * @param proposalInfo
	 * @throws IOException
	 * @throws JsonParseException
	 * @throws JsonMappingException
	 */
	public void getAppendixDetails(String proposalId, Proposal existingProposal, Proposal oldProposal,
			JsonNode proposalInfo) throws IOException, JsonParseException, JsonMappingException {
		ObjectMapper mapper = new ObjectMapper();
		// Appendix Info
		if (proposalInfo != null && proposalInfo.has("AppendixInfo")) {
			List<Appendix> appendixInfo = Arrays
					.asList(mapper.readValue(proposalInfo.get("AppendixInfo").toString(), Appendix[].class));
			if (appendixInfo.size() != 0) {
				String UPLOAD_PATH = new String();
				try {
					UPLOAD_PATH = this.getClass().getResource("/uploads").toURI().getPath();
				} catch (URISyntaxException e) {
					e.printStackTrace();
				}
				List<String> existingFiles = new ArrayList<String>();
				if (!proposalId.equals("0")) {
					boolean alreadyExist = false;
					for (Appendix appendix : oldProposal.getAppendices()) {
						for (Appendix appendixObj : appendixInfo) {
							if (appendix.getFilename().equalsIgnoreCase(appendixObj.getFilename())
									&& appendix.getTitle().equalsIgnoreCase(
											appendixObj.getTitle().trim().replaceAll("\\<[^>]*>", ""))) {
								alreadyExist = true;
								existingFiles.add(appendixObj.getFilename());
								break;
							}
						}
						if (!alreadyExist) {
							existingProposal.getAppendices().remove(appendix);
						}
					}
					for (Appendix uploadFile : appendixInfo) {
						String fileName = uploadFile.getFilename();
						if (!existingFiles.contains(fileName)) {
							File file = new File(UPLOAD_PATH + fileName);
							String extension = "";
							int i = fileName.lastIndexOf('.');
							if (i > 0) {
								extension = fileName.substring(i + 1);
								if (verifyValidFileExtension(extension)) {
									uploadFile.setExtension(extension);
								} else {
									Response.status(403).entity(extension
											+ " is not allowed. Allowed extensions: jpg,png,gif,jpeg,bmp,png,pdf,doc,docx,xls,xlsx,txt")
											.build();
								}
							}
							long fileSize = file.length();
							if (verifyValidFileSize(fileSize)) {
								uploadFile.setFilesize(fileSize);
							} else {
								Response.status(403).entity("The uploaded file is larger than 5MB").build();
							}
							uploadFile.setFilepath("/uploads/" + fileName);
							String fileTitle = uploadFile.getTitle().trim().replaceAll("\\<[^>]*>", "");
							if (validateNotEmptyValue(fileTitle)) {
								uploadFile.setTitle(fileTitle);
							} else {
								Response.status(403).entity("The Uploaded File's Title can not be Empty").build();
							}
							existingProposal.getAppendices().add(uploadFile);
						}
					}
				} else {
					for (Appendix uploadFile : appendixInfo) {
						String fileName = uploadFile.getFilename();
						File file = new File(UPLOAD_PATH + fileName);
						String extension = "";
						int i = fileName.lastIndexOf('.');
						if (i > 0) {
							extension = fileName.substring(i + 1);
							if (verifyValidFileExtension(extension)) {
								uploadFile.setExtension(extension);
							} else {
								Response.status(403).entity(extension
										+ " is not allowed. Allowed extensions: jpg,png,gif,jpeg,bmp,png,pdf,doc,docx,xls,xlsx,txt")
										.build();
							}
						}
						long fileSize = file.length();
						if (verifyValidFileSize(fileSize)) {
							uploadFile.setFilesize(fileSize);
						} else {
							Response.status(403).entity("The uploaded file is larger than 5MB").build();
						}
						uploadFile.setFilesize(fileSize);
						uploadFile.setFilepath("/uploads/" + fileName);
						String fileTitle = uploadFile.getTitle().trim().replaceAll("\\<[^>]*>", "");
						if (validateNotEmptyValue(fileTitle)) {
							uploadFile.setTitle(fileTitle);
						} else {
							Response.status(403).entity("The Uploaded File's Title can not be Empty").build();
						}
						existingProposal.getAppendices().add(uploadFile);
					}
				}
			} else {
				existingProposal.getAppendices().clear();
			}
		}
	}

	/***
	 * Gets Signature Details
	 * 
	 * @param userInfo
	 * @param proposalId
	 * @param existingProposal
	 * @param proposalInfo
	 * @return
	 * @throws ParseException
	 */
	public boolean getSignatureDetails(GPMSCommonInfo userInfo, String proposalId, Proposal existingProposal,
			JsonNode proposalInfo) throws ParseException {
		if (proposalInfo != null && proposalInfo.has("SignatureInfo")) {
			String[] rows = proposalInfo.get("SignatureInfo").textValue().split("#!#");
			List<SignatureInfo> newSignatureInfo = new ArrayList<SignatureInfo>();
			List<SignatureInfo> allSignatureInfo = new ArrayList<SignatureInfo>();
			List<SignatureInfo> removeSignatureInfo = new ArrayList<SignatureInfo>();
			DateFormat format = new SimpleDateFormat("yyyy/MM/dd hh:mm:ss a");
			for (String col : rows) {
				String[] cols = col.split("!#!");
				SignatureInfo signatureInfo = new SignatureInfo();
				signatureInfo.setUserProfileId(cols[0]);
				final String signatureText = cols[1].replaceAll("\\<[^>]*>", "");
				if (validateNotEmptyValue(signatureText)) {
					signatureInfo.setSignature(signatureText);
				} else {
					Response.status(403).entity("The Signature can not be Empty").build();
				}
				final String signedDate = cols[2].trim().replaceAll("\\<[^>]*>", "");
				if (validateNotEmptyValue(signedDate)) {
					signatureInfo.setSignedDate(format.parse(signedDate));
				} else {
					Response.status(403).entity("The Signed Date can not be Empty").build();
				}
				final String noteText = cols[3].replaceAll("\\<[^>]*>", "");
				if (validateNotEmptyValue(noteText)) {
					signatureInfo.setNote(noteText);
				} else {
					Response.status(403).entity("The Note can not be Empty").build();
				}
				signatureInfo.setFullName(cols[4]);
				signatureInfo.setPositionTitle(cols[5]);
				signatureInfo.setDelegated(Boolean.parseBoolean(cols[6]));
				allSignatureInfo.add(signatureInfo);
				if (!proposalId.equals("0")) {
					boolean alreadyExist = false;
					for (SignatureInfo sign : existingProposal.getSignatureInfo()) {
						if (sign.equals(signatureInfo)) {
							alreadyExist = true;
							break;
						} else {
							if (sign.getUserProfileId().equals(cols[0])) {
								removeSignatureInfo.add(sign);
							}
						}
					}
					if (!alreadyExist) {
						newSignatureInfo.add(signatureInfo);
					}
				}
				for (SignatureInfo removeSign : removeSignatureInfo) {
					existingProposal.getSignatureInfo().remove(removeSign);
				}
			}
			if (!proposalId.equals("0")) {
				if (!existingProposal.getSignatureInfo().equals(allSignatureInfo)) {
					for (SignatureInfo signatureInfo : newSignatureInfo) {
						existingProposal.getSignatureInfo().add(signatureInfo);
					}
				}
			} else {
				existingProposal.setSignatureInfo(allSignatureInfo);
			}
			for (SignatureInfo sign : existingProposal.getSignatureInfo()) {
				if (sign.getUserProfileId().equals(userInfo.getUserProfileID())
						&& !sign.getSignature().trim().equals("")) {
					return true;
				}
			}
		}
		return false;
	}

	/***
	 * Gets Involve Use Of Human Subjects Info
	 * 
	 * @param newComplianceInfo
	 * @param irbApprovalRequired
	 * @param complianceInfo
	 * @return
	 */
	public Boolean getInvolveUseOfHumanSubjectsInfo(ComplianceInfo newComplianceInfo, Boolean irbApprovalRequired,
			JsonNode complianceInfo) {
		if (complianceInfo != null && complianceInfo.has("InvolveUseOfHumanSubjects")) {
			switch (complianceInfo.get("InvolveUseOfHumanSubjects").textValue()) {
			case "1":
				newComplianceInfo.setInvolveUseOfHumanSubjects(true);
				irbApprovalRequired = true;
				if (complianceInfo != null && complianceInfo.has("IRBPending")) {
					switch (complianceInfo.get("IRBPending").textValue()) {
					case "1":
						newComplianceInfo.setIrbPending(false);
						if (complianceInfo != null && complianceInfo.has("IRB")) {
							final String IRBNo = complianceInfo.get("IRB").textValue().trim().replaceAll("\\<[^>]*>",
									"");
							if (validateNotEmptyValue(IRBNo)) {
								newComplianceInfo.setIrb(IRBNo);
							} else {
								Response.status(403).entity("The IRB # can not be Empty").build();
							}
						}
						break;
					case "2":
						newComplianceInfo.setIrbPending(true);
						break;
					default:
						break;
					}
				}
				break;
			case "2":
				newComplianceInfo.setInvolveUseOfHumanSubjects(false);
				break;
			default:
				break;
			}
		}
		return irbApprovalRequired;
	}

	/***
	 * Gets Involve Use Of Vertebrate Animals Info
	 * 
	 * @param newComplianceInfo
	 * @param irbApprovalRequired
	 * @param complianceInfo
	 * @return
	 */
	public Boolean getInvolveUseOfVertebrateAnimalsInfo(ComplianceInfo newComplianceInfo, Boolean irbApprovalRequired,
			JsonNode complianceInfo) {
		if (complianceInfo != null && complianceInfo.has("InvolveUseOfVertebrateAnimals")) {
			switch (complianceInfo.get("InvolveUseOfVertebrateAnimals").textValue()) {
			case "1":
				newComplianceInfo.setInvolveUseOfVertebrateAnimals(true);
				irbApprovalRequired = true;
				if (complianceInfo != null && complianceInfo.has("IACUCPending")) {
					switch (complianceInfo.get("IACUCPending").textValue()) {
					case "1":
						newComplianceInfo.setIacucPending(false);
						if (complianceInfo != null && complianceInfo.has("IACUC")) {
							final String IACUCNo = complianceInfo.get("IACUC").textValue().trim()
									.replaceAll("\\<[^>]*>", "");
							if (validateNotEmptyValue(IACUCNo)) {
								newComplianceInfo.setIacuc(IACUCNo);
							} else {
								Response.status(403).entity("The IACUC # can not be Empty").build();
							}
						}
						break;
					case "2":
						newComplianceInfo.setIacucPending(true);
						break;
					default:
						break;
					}
				}
				break;
			case "2":
				newComplianceInfo.setInvolveUseOfVertebrateAnimals(false);
				break;
			default:
				break;
			}
		}
		return irbApprovalRequired;
	}

	/***
	 * Gets Involve Biosafety Concerns Info
	 * 
	 * @param newComplianceInfo
	 * @param irbApprovalRequired
	 * @param complianceInfo
	 * @return
	 */
	public Boolean getInvolveBiosafetyConcernsInfo(ComplianceInfo newComplianceInfo, Boolean irbApprovalRequired,
			JsonNode complianceInfo) {
		if (complianceInfo != null && complianceInfo.has("InvolveBiosafetyConcerns")) {
			switch (complianceInfo.get("InvolveBiosafetyConcerns").textValue()) {
			case "1":
				newComplianceInfo.setInvolveBiosafetyConcerns(true);
				irbApprovalRequired = true;
				if (complianceInfo != null && complianceInfo.has("IBCPending")) {
					switch (complianceInfo.get("IBCPending").textValue()) {
					case "1":
						newComplianceInfo.setIbcPending(false);
						if (complianceInfo != null && complianceInfo.has("IBC")) {
							final String IBCNo = complianceInfo.get("IBC").textValue().trim().replaceAll("\\<[^>]*>",
									"");

							if (validateNotEmptyValue(IBCNo)) {
								newComplianceInfo.setIbc(IBCNo);
							} else {
								Response.status(403).entity("The IBC # can not be Empty").build();
							}
						}
						break;
					case "2":
						newComplianceInfo.setIbcPending(true);
						break;
					default:
						break;
					}
				}
				break;
			case "2":
				newComplianceInfo.setInvolveBiosafetyConcerns(false);
				break;
			default:
				break;
			}
		}
		return irbApprovalRequired;
	}

	/***
	 * Gets Involve Environmental Health And Safety Concerns Info
	 * 
	 * @param newComplianceInfo
	 * @param irbApprovalRequired
	 * @param complianceInfo
	 * @return
	 */
	public Boolean getInvolveEnvironmentalHealthAndSafetyConcernsInfo(ComplianceInfo newComplianceInfo,
			Boolean irbApprovalRequired, JsonNode complianceInfo) {
		if (complianceInfo != null && complianceInfo.has("InvolveEnvironmentalHealthAndSafetyConcerns")) {
			switch (complianceInfo.get("InvolveEnvironmentalHealthAndSafetyConcerns").textValue()) {
			case "1":
				newComplianceInfo.setInvolveEnvironmentalHealthAndSafetyConcerns(true);
				irbApprovalRequired = true;
				break;
			case "2":
				newComplianceInfo.setInvolveEnvironmentalHealthAndSafetyConcerns(false);
				break;
			default:
				break;
			}
		}
		return irbApprovalRequired;
	}

	/***
	 * Gets Compliance Details
	 * 
	 * @param proposalId
	 * @param existingProposal
	 * @param proposalInfo
	 * @return
	 */
	public Boolean getComplianceDetails(String proposalId, Proposal existingProposal, JsonNode proposalInfo) {
		ComplianceInfo newComplianceInfo = new ComplianceInfo();
		Boolean irbApprovalRequired = false;
		if (proposalInfo != null && proposalInfo.has("ComplianceInfo")) {
			JsonNode complianceInfo = proposalInfo.get("ComplianceInfo");
			irbApprovalRequired = getInvolveUseOfHumanSubjectsInfo(newComplianceInfo, irbApprovalRequired,
					complianceInfo);
			irbApprovalRequired = getInvolveUseOfVertebrateAnimalsInfo(newComplianceInfo, irbApprovalRequired,
					complianceInfo);
			irbApprovalRequired = getInvolveBiosafetyConcernsInfo(newComplianceInfo, irbApprovalRequired,
					complianceInfo);
			irbApprovalRequired = getInvolveEnvironmentalHealthAndSafetyConcernsInfo(newComplianceInfo,
					irbApprovalRequired, complianceInfo);
		}
		// ComplianceInfo
		if (!proposalId.equals("0")) {
			if (!existingProposal.getComplianceInfo().equals(newComplianceInfo)) {
				existingProposal.setComplianceInfo(newComplianceInfo);
				existingProposal.setIrbApprovalRequired(irbApprovalRequired);
			}
		} else {
			existingProposal.setComplianceInfo(newComplianceInfo);
			existingProposal.setIrbApprovalRequired(irbApprovalRequired);
		}
		return irbApprovalRequired;
	}

	/***
	 * Gets OSP Section Info
	 * 
	 * @param existingProposal
	 * @param proposalInfo
	 * @throws Exception
	 */
	public void getOSPSectionInfo(Proposal existingProposal, JsonNode proposalInfo) throws Exception {
		// OSP Section Info Only for University Research
		// Administrator
		// or University Research Director
		OSPSectionInfo newOSPSectionInfo = new OSPSectionInfo();
		if (proposalInfo != null && proposalInfo.has("OSPSectionInfo")) {
			JsonNode oSPSectionInfo = proposalInfo.get("OSPSectionInfo");
			getListAgency(existingProposal, oSPSectionInfo);
			getFundingSource(existingProposal, oSPSectionInfo);
			getProgramDetails(existingProposal, oSPSectionInfo);
			getRecoveryDetails(existingProposal, oSPSectionInfo);
			getBaseInfo(existingProposal, oSPSectionInfo);
			getSalaryDetails(existingProposal, newOSPSectionInfo, oSPSectionInfo);
			getInstitutionalCostDetails(existingProposal, oSPSectionInfo);
			getSubRecipientsDetails(existingProposal, newOSPSectionInfo, oSPSectionInfo);
			getBasePIEligibilityOptions(existingProposal, oSPSectionInfo);
			getConflictOfInterestForms(existingProposal, oSPSectionInfo);
			getExcludedPartyListChecked(existingProposal, oSPSectionInfo);
		}
	}

	/***
	 * Updates For Proposal Save
	 * 
	 * @param existingProposal
	 * @param proposalID
	 * @param requiredSignatures
	 * @param authorUserName
	 * @param notificationMessage
	 * @param currentProposalRoles
	 * @return
	 */
	public String updateForProposalSave(Proposal existingProposal, String proposalID,
			RequiredSignaturesInfo requiredSignatures, String authorUserName, String notificationMessage,
			List<String> currentProposalRoles) {
		// Change status to ready to submitted by PI
		if (proposalID.equals("0")) {
			notificationMessage = "Saved by " + authorUserName + ".";
			if (existingProposal.getInvestigatorInfo().getCo_pi().size() == 0) {
				existingProposal.setReadyForSubmissionByPI(true);
				existingProposal.getProposalStatus().clear();
				existingProposal.getProposalStatus().add(Status.READYFORSUBMITBYPI);
			}
		} else if (!proposalID.equals("0") && currentProposalRoles != null) {
			if ((currentProposalRoles.contains("PI")
					|| (currentProposalRoles.contains("Co-PI") && !existingProposal.isReadyForSubmissionByPI()))
					&& existingProposal.getSubmittedByPI() == SubmitType.NOTSUBMITTED) {
				if (requiredSignatures.isSignedByPI() && requiredSignatures.isSignedByAllCoPIs()) {
					existingProposal.setReadyForSubmissionByPI(true);
					existingProposal.getProposalStatus().clear();
					existingProposal.getProposalStatus().add(Status.READYFORSUBMITBYPI);
				} else {
					existingProposal.setReadyForSubmissionByPI(false);
					existingProposal.getProposalStatus().clear();
					existingProposal.getProposalStatus().add(Status.NOTSUBMITTEDBYPI);
				}
				notificationMessage = "Updated by " + authorUserName + ".";
			}
		}
		return notificationMessage;
	}

	/***
	 * Saves Proposal With Obligations
	 * 
	 * @param obligations
	 * @return
	 * @throws JsonProcessingException
	 */
//	public EmailCommonInfo saveProposalWithObligations(List<ObligationResult> obligations)
//			throws JsonProcessingException {
//		EmailCommonInfo emailDetails = new EmailCommonInfo();
//		getObligationsDetails(obligations, emailDetails);
//		return emailDetails;
//	}

	/***
	 * Saves Proposal Without Obligations
	 * 
	 * @param message
	 * @param proposalId
	 * @param existingProposal
	 * @param oldProposal
	 * @param authorProfile
	 * @param irbApprovalRequired
	 * @return
	 * @throws IOException
	 */
	public EmailCommonInfo saveProposalWithoutObligations(String message, String proposalId, Proposal existingProposal,
			Proposal oldProposal, UserProfile authorProfile, Boolean irbApprovalRequired) throws IOException {
		ObjectMapper mapper = new ObjectMapper();
		JsonNode root = mapper.readTree(message);
		JsonNode buttonType = root.get("buttonType");
		String authorUserName = authorProfile.getUserAccount().getUserName();
		EmailCommonInfo emailDetails = new EmailCommonInfo();
		emailDetails.setAuthorName(authorUserName);
		if (buttonType != null) {
			switch (buttonType.textValue()) {
			case "Save":
				if (proposalId.equals("0")) {
					emailDetails.setEmailSubject("The proposal has been created by: " + authorUserName);
					emailDetails.setEmailBody(
							"Hello User,<br/><br/>The proposal has been created by Admin.<br/><br/>Thank you, <br/> GPMS Team");
				} else if (!proposalId.equals("0")) {
					emailDetails.setEmailSubject("The proposal has been updated by: " + authorUserName);
					emailDetails.setEmailBody(
							"Hello User,<br/><br/>The proposal has been updated by Admin.<br/><br/>Thank you, <br/> GPMS Team");
				}
				break;
			default:
				break;
			}
		}
		emailDetails.setPiEmail(existingProposal.getInvestigatorInfo().getPi().getUserRef().getWorkEmails().get(0));
		for (InvestigatorRefAndPosition copis : existingProposal.getInvestigatorInfo().getCo_pi()) {
			emailDetails.getEmaillist().add(copis.getUserRef().getWorkEmails().get(0));
		}
		for (InvestigatorRefAndPosition seniors : existingProposal.getInvestigatorInfo().getSeniorPersonnel()) {
			emailDetails.getEmaillist().add(seniors.getUserRef().getWorkEmails().get(0));
		}
		return emailDetails;
	}

	/***
	 * Checking whether Signed By All Users or not
	 * 
	 * @param contentProfile
	 * @param requiredSigns
	 * @param signedByCurrentUser
	 */
	public void checkForSignedByAllUsers(StringBuffer contentProfile, RequiredSignaturesInfo requiredSigns,
			boolean signedByCurrentUser) {
		boolean signedByPI = requiredSigns.getExistingPISign().containsAll(requiredSigns.getRequiredPISign());
		requiredSigns.setSignedByPI(signedByPI);
		boolean signedByAllCoPIs = requiredSigns.getExistingCoPISigns()
				.containsAll(requiredSigns.getRequiredCoPISigns());
		requiredSigns.setSignedByAllCoPIs(signedByAllCoPIs);
		boolean signedByAllChairs = requiredSigns.getExistingChairSigns()
				.containsAll(requiredSigns.getRequiredChairSigns());
		requiredSigns.setSignedByAllChairs(signedByAllChairs);
		boolean signedByAllBusinessManagers = requiredSigns.getExistingBusinessManagerSigns()
				.containsAll(requiredSigns.getRequiredBusinessManagerSigns());
		requiredSigns.setSignedByAllBusinessManagers(signedByAllBusinessManagers);
		boolean signedByAllDeans = requiredSigns.getExistingDeanSigns()
				.containsAll(requiredSigns.getRequiredDeanSigns());
		requiredSigns.setSignedByAllDeans(signedByAllDeans);
		boolean signedByAllIRBs = requiredSigns.getExistingIRBSigns().containsAll(requiredSigns.getRequiredIRBSigns());
		requiredSigns.setSignedByAllIRBs(signedByAllIRBs);
		boolean signedByAllResearchAdmins = requiredSigns.getExistingResearchAdminSigns()
				.containsAll(requiredSigns.getRequiredResearchAdminSigns());
		requiredSigns.setSignedByAllResearchAdmins(signedByAllResearchAdmins);
		boolean signedByAllResearchDirectors = requiredSigns.getExistingResearchDirectorSigns()
				.containsAll(requiredSigns.getRequiredResearchDirectorSigns());
		requiredSigns.setSignedByAllResearchDirectors(signedByAllResearchDirectors);
		contentProfile.append("<ak:signedByCurrentUser>");
		contentProfile.append(signedByCurrentUser);
		contentProfile.append("</ak:signedByCurrentUser>");
		requiredSigns.setSignedByPI(signedByPI);
		contentProfile.append("<ak:signedByPI>");
		contentProfile.append(signedByPI);
		contentProfile.append("</ak:signedByPI>");
		requiredSigns.setSignedByAllCoPIs(signedByAllCoPIs);
		contentProfile.append("<ak:signedByAllCoPIs>");
		contentProfile.append(signedByAllCoPIs);
		contentProfile.append("</ak:signedByAllCoPIs>");
		requiredSigns.setSignedByAllChairs(signedByAllChairs);
		contentProfile.append("<ak:signedByAllChairs>");
		contentProfile.append(signedByAllChairs);
		contentProfile.append("</ak:signedByAllChairs>");
		requiredSigns.setSignedByAllBusinessManagers(signedByAllBusinessManagers);
		contentProfile.append("<ak:signedByAllBusinessManagers>");
		contentProfile.append(signedByAllBusinessManagers);
		contentProfile.append("</ak:signedByAllBusinessManagers>");
		requiredSigns.setSignedByAllDeans(signedByAllDeans);
		contentProfile.append("<ak:signedByAllDeans>");
		contentProfile.append(signedByAllDeans);
		contentProfile.append("</ak:signedByAllDeans>");
		requiredSigns.setSignedByAllIRBs(signedByAllIRBs);
		contentProfile.append("<ak:signedByAllIRBs>");
		contentProfile.append(signedByAllIRBs);
		contentProfile.append("</ak:signedByAllIRBs>");
		requiredSigns.setSignedByAllResearchAdmins(signedByAllResearchAdmins);
		contentProfile.append("<ak:signedByAllResearchAdmins>");
		contentProfile.append(signedByAllResearchAdmins);
		contentProfile.append("</ak:signedByAllResearchAdmins>");
		requiredSigns.setSignedByAllResearchDirectors(signedByAllResearchDirectors);
		contentProfile.append("<ak:signedByAllResearchDirectors>");
		contentProfile.append(signedByAllResearchDirectors);
		contentProfile.append("</ak:signedByAllResearchDirectors>");
	}

	/***
	 * Gets Existing Signatures For a Proposal
	 * 
	 * @param contentProfile
	 * @param existingProposal
	 * @param requiredSigns
	 * @param signedByCurrentUser
	 */
	public void getExistingSignaturesForProposal(StringBuffer contentProfile, Proposal existingProposal,
			RequiredSignaturesInfo requiredSigns, boolean signedByCurrentUser) {

		for (SignatureInfo sign : existingProposal.getSignatureInfo()) {
			if (sign.getPositionTitle().equals("PI")) {
				requiredSigns.getExistingPISign().add(sign.getUserProfileId());
			} else if (sign.getPositionTitle().equals("Co-PI")) {
				requiredSigns.getExistingCoPISigns().add(sign.getUserProfileId());
			} else if (sign.getPositionTitle().equals("Department Chair")) {
				requiredSigns.getExistingChairSigns().add(sign.getUserProfileId());
			} else if (sign.getPositionTitle().equals("Business Manager")) {
				requiredSigns.getExistingBusinessManagerSigns().add(sign.getUserProfileId());
			} else if (sign.getPositionTitle().equals("Dean")) {
				requiredSigns.getExistingDeanSigns().add(sign.getUserProfileId());
			} else if (sign.getPositionTitle().equals("IRB")) {
				requiredSigns.getExistingIRBSigns().add(sign.getUserProfileId());
			} else if (sign.getPositionTitle().equals("University Research Administrator")) {
				requiredSigns.getExistingResearchAdminSigns().add(sign.getUserProfileId());
			} else if (sign.getPositionTitle().equals("University Research Director")) {
				requiredSigns.getExistingResearchDirectorSigns().add(sign.getUserProfileId());
			}
		}
	}

	public Set<String> getExistingSignatureProfileIdForAProposal(Proposal existingProposal, Set<String> profileIds,
			String tag) {

		for (SignatureInfo sign : existingProposal.getSignatureInfo()) {
			if (sign.getPositionTitle().equals(tag)) {
				profileIds.add(sign.getUserProfileId());
			}
		}
		return profileIds;

		//
//		for (SignatureInfo sign : existingProposal.getSignatureInfo()) {
//			if (sign.getPositionTitle().equals("PI")) {
//				requiredSigns.getExistingPISign().add(sign.getUserProfileId());
//			} else if (sign.getPositionTitle().equals("Co-PI")) {
//				requiredSigns.getExistingCoPISigns().add(
//						sign.getUserProfileId());
//			} else if (sign.getPositionTitle().equals("Department Chair")) {
//				requiredSigns.getExistingChairSigns().add(
//						sign.getUserProfileId());
//			} else if (sign.getPositionTitle().equals("Business Manager")) {
//				requiredSigns.getExistingBusinessManagerSigns().add(
//						sign.getUserProfileId());
//			} else if (sign.getPositionTitle().equals("Dean")) {
//				requiredSigns.getExistingDeanSigns().add(
//						sign.getUserProfileId());
//			} else if (sign.getPositionTitle().equals("IRB")) {
//				requiredSigns.getExistingIRBSigns()
//						.add(sign.getUserProfileId());
//			} else if (sign.getPositionTitle().equals(
//					"University Research Administrator")) {
//				requiredSigns.getExistingResearchAdminSigns().add(
//						sign.getUserProfileId());
//			} else if (sign.getPositionTitle().equals(
//					"University Research Director")) {
//				requiredSigns.getExistingResearchDirectorSigns().add(
//						sign.getUserProfileId());
//			}
//		}
	}

	/***
	 * Updates Withdrawn Status for a Proposal
	 * 
	 * @param proposalId
	 * @param proposalUserTitle
	 * @param existingProposal
	 * @param authorProfile
	 * @param authorUserName
	 * @param emailDetails
	 * @return
	 * @throws JsonProcessingException
	 */
	public boolean updateWithdrawnStatus(String proposalId, String proposalUserTitle, Proposal existingProposal,
			UserProfile authorProfile, String authorUserName, EmailCommonInfo emailDetails)
			throws JsonProcessingException {
		boolean isStatusUpdated = false;
		if (!proposalId.equals("0")) {
			if (existingProposal.getResearchAdministratorWithdraw() == WithdrawType.NOTWITHDRAWN
					&& existingProposal.getResearchAdministratorApproval() == ApprovalType.READYFORAPPROVAL
					&& proposalUserTitle.equals("University Research Administrator")) {
				existingProposal.setResearchAdministratorWithdraw(WithdrawType.WITHDRAWN);
				existingProposal.setResearchAdministratorApproval(ApprovalType.NOTREADYFORAPPROVAL);
				existingProposal.getProposalStatus().clear();
				existingProposal.getProposalStatus().add(Status.WITHDRAWBYRESEARCHADMIN);
				isStatusUpdated = updateProposalStatus(existingProposal, authorProfile);
			}
		}
		return isStatusUpdated;
	}

	/***
	 * Updates Archived Status for a Proposal
	 * 
	 * @param proposalId
	 * @param proposalUserTitle
	 * @param existingProposal
	 * @param authorProfile
	 * @param authorUserName
	 * @param emailDetails
	 * @return
	 * @throws JsonProcessingException
	 */
	public boolean updateArchivedStatus(String proposalId, String proposalUserTitle, Proposal existingProposal,
			UserProfile authorProfile, String authorUserName, EmailCommonInfo emailDetails)
			throws JsonProcessingException {
		boolean isStatusUpdated = false;
		if (!proposalId.equals("0")) {
			if (existingProposal.getResearchDirectorArchived() == ArchiveType.NOTARCHIVED
					&& existingProposal.getResearchAdministratorSubmission() == SubmitType.SUBMITTED
					&& proposalUserTitle.equals("University Research Director")) {
				existingProposal.setResearchDirectorArchived(ArchiveType.ARCHIVED);
				existingProposal.setResearchDirectorApproval(ApprovalType.NOTREADYFORAPPROVAL);
				existingProposal.getProposalStatus().clear();
				existingProposal.getProposalStatus().add(Status.ARCHIVEDBYRESEARCHDIRECTOR);
				isStatusUpdated = updateProposalStatus(existingProposal, authorProfile);
			}
		}
		return isStatusUpdated;
	}

	/***
	 * Gets Obligations Details with all Email Information
	 * 
	 * @param obligations
	 * @param emailDetails
	 * @return
	 */
//	public Response getObligationsDetails(List<ObligationResult> obligations, EmailCommonInfo emailDetails) {
//		if (obligations.size() > 0) {
//			List<ObligationResult> preObligations = new ArrayList<ObligationResult>();
//			List<ObligationResult> postObligations = new ArrayList<ObligationResult>();
//			List<ObligationResult> ongoingObligations = new ArrayList<ObligationResult>();
//			for (ObligationResult obligation : obligations) {
//				categorizeObligationTypes(preObligations, postObligations, ongoingObligations, obligation);
//			}
//			// Performs Preobligations
//			Boolean preCondition = true;
//			String alertMessage = new String();
//			if (preObligations.size() != 0) {
//				preCondition = false;
//				//System.out.println("\n======================== Printing Obligations ====================");
//				for (ObligationResult obligation : preObligations) {
//					if (obligation instanceof org.wso2.balana.xacml3.Obligation) {
//						List<AttributeAssignment> assignments = ((org.wso2.balana.xacml3.Obligation) obligation)
//								.getAssignments();
//						String obligationType = "preobligation";
//						for (AttributeAssignment assignment : assignments) {
//							switch (assignment.getAttributeId().toString()) {
//							case "signedByCurrentUser":
//								preCondition = Boolean.parseBoolean(assignment.getContent());
//								break;
//							case "alertMessage":
//								alertMessage = assignment.getContent();
//								break;
//							default:
//								break;
//							}
//						}
//						//System.out.println(obligationType + " is RUNNING");
//						if (!preCondition) {
//							break;
//						}
//					}
//				}
//			}
//
//			if (preCondition) {
//				// Performs Postobligations
//				for (ObligationResult obligation : postObligations) {
//					if (obligation instanceof org.wso2.balana.xacml3.Obligation) {
//						List<AttributeAssignment> assignments = ((org.wso2.balana.xacml3.Obligation) obligation)
//								.getAssignments();
//						String obligationType = "postobligation";
//						for (AttributeAssignment assignment : assignments) {
//							switch (assignment.getAttributeId().toString()) {
//							case "authorName":
//								emailDetails.setAuthorName(assignment.getContent());
//								break;
//							case "emailSubject":
//								emailDetails.setEmailSubject(assignment.getContent());
//								break;
//							case "emailBody":
//								emailDetails.setEmailBody(assignment.getContent());
//								break;
//							case "piEmail":
//								emailDetails.setPiEmail(assignment.getContent());
//								break;
//							case "copisEmail":
//							case "seniorsEmail":
//							case "chairsEmail":
//							case "managersEmail":
//							case "deansEmail":
//							case "irbsEmail":
//							case "administratorsEmail":
//							case "directorsEmail":
//								if (!assignment.getContent().equals("")) {
//									emailDetails.getEmaillist().add(assignment.getContent());
//								}
//								break;
//							default:
//								break;
//							}
//						}
//						//System.out.println(obligationType + " is RUNNING");
//					}
//				}
//			} else {
//				return Response.status(403).type(MediaType.APPLICATION_JSON).entity(alertMessage).build();
//			}
//		}
//		return Response.status(403)
//				.entity("{\"error\": \"Error while geting Obligations Information!\", \"status\": \"FAIL\"}").build();
//	}

//	/***
//	 * Categorizes different Obligation Types
//	 * 
//	 * @param preObligations
//	 * @param postObligations
//	 * @param ongoingObligations
//	 * @param obligation
//	 */
//	public void categorizeObligationTypes(List<ObligationResult> preObligations, List<ObligationResult> postObligations,
//			List<ObligationResult> ongoingObligations, ObligationResult obligation) {
//		if (obligation instanceof org.wso2.balana.xacml3.Obligation) {
//			List<AttributeAssignment> assignments = ((org.wso2.balana.xacml3.Obligation) obligation).getAssignments();
//			String obligationType = "postobligation";
//			for (AttributeAssignment assignment : assignments) {
//				if (assignment.getAttributeId().toString().equalsIgnoreCase("obligationType")) {
//					obligationType = assignment.getContent();
//					break;
//				}
//			}
//			if (obligationType.equals("preobligation")) {
//				preObligations.add(obligation);
//				//System.out.println(obligationType + " is FOUND");
//			} else if (obligationType.equals("postobligation")) {
//				postObligations.add(obligation);
//				//System.out.println(obligationType + " is FOUND");
//			} else {
//				ongoingObligations.add(obligation);
//				//System.out.println(obligationType + " is FOUND");
//			}
//		}
//	}

	/***
	 * Updates Proposal Status With Obligations
	 * 
	 * @param proposalId
	 * @param buttonType
	 * @param proposalUserTitle
	 * @param existingProposal
	 * @param authorProfile
	 * @param authorUserName
	 * @param obligations
	 * @return
	 * @throws JsonProcessingException
	 */
	public String updateProposalStatusWithObligations(String proposalId, String buttonType, String proposalUserTitle,
			Proposal existingProposal, UserProfile authorProfile, String authorUserName
			) throws JsonProcessingException {
		boolean isStatusUpdated = false;
		String changeDone = new String();
		EmailCommonInfo emailDetails = new EmailCommonInfo();
		if (buttonType != null && buttonType != "") {
			switch (buttonType) {
			case "Withdraw":
				isStatusUpdated = updateWithdrawnStatus(proposalId, proposalUserTitle, existingProposal, authorProfile,
						authorUserName, emailDetails);
				changeDone = "Withdrawn";
				break;
			case "Archive":
				isStatusUpdated = updateArchivedStatus(proposalId, proposalUserTitle, existingProposal, authorProfile,
						authorUserName, emailDetails);
				changeDone = "Archived";
				break;
			default:
				break;
			}
		}
		if (isStatusUpdated) {
			String emailSubject = emailDetails.getEmailSubject();
			String emailBody = emailDetails.getEmailBody();
			String authorName = emailDetails.getAuthorName();
			String piEmail = emailDetails.getPiEmail();
			List<String> emaillist = emailDetails.getEmaillist();
			if (!emailSubject.equals("")) {
				// EmailUtil emailUtil = new EmailUtil();
				// emailUtil.sendMailMultipleUsersWithoutAuth(piEmail, emaillist,emailSubject +
				// authorName, emailBody);
			}
		}
		return changeDone;
	}

	/***
	 * Deletes Proposal With Obligations
	 * 
	 * @param proposalRoles
	 * @param proposalUserTitle
	 * @param existingProposal
	 * @param authorProfile
	 * @param authorUserName
	 * @param obligations
	 * @return
	 * @throws JsonProcessingException
	 */
//	public boolean deleteProposalWithObligations(String proposalRoles, String proposalUserTitle,
//			Proposal existingProposal, UserProfile authorProfile, String authorUserName,
//			List<ObligationResult> obligations) throws JsonProcessingException {
//		EmailCommonInfo emailDetails = new EmailCommonInfo();
//		getObligationsDetails(obligations, emailDetails);
//		boolean isDeleted = deleteProposal(existingProposal, proposalRoles, proposalUserTitle, authorProfile);
//		if (isDeleted) {
//			String emailSubject = emailDetails.getEmailSubject();
//			String emailBody = emailDetails.getEmailBody();
//			String authorName = emailDetails.getAuthorName();
//			String piEmail = emailDetails.getPiEmail();
//			List<String> emaillist = emailDetails.getEmaillist();
//			if (!emailSubject.equals("")) {
//				// EmailUtil emailUtil = new EmailUtil();
//				// emailUtil.sendMailMultipleUsersWithoutAuth(piEmail, emaillist,emailSubject +
//				// authorName, emailBody);
//			}
//		}
//		return isDeleted;
//	}

	/***
	 * Generates Attributes based on policy info
	 * 
	 * @param policyInfo
	 * @return
	 */
	public HashMap<String, String> generateAttributesForPM(JsonNode policyInfo) {
		HashMap<String, String> attrMap = new HashMap<String, String>();
		Multimap<String, String> subjectMap = ArrayListMultimap.create();
		Multimap<String, String> resourceMap = ArrayListMultimap.create();
		Multimap<String, String> actionMap = ArrayListMultimap.create();
		Multimap<String, String> environmentMap = ArrayListMultimap.create();
		for (JsonNode node : policyInfo) {
			String attributeName = node.path("attributeName").asText();
			String attributeValue = node.path("attributeValue").asText();
			String attributeType = node.path("attributeType").asText();
			attrMap.put(attributeName, attributeValue);
		}

		return attrMap;
	}

	/***
	 * Generates Attributes based on policy info
	 * 
	 * @param policyInfo
	 * @return
	 */
	public HashMap<String, Multimap<String, String>> generateAttributes(JsonNode policyInfo) {
		HashMap<String, Multimap<String, String>> attrMap = new HashMap<String, Multimap<String, String>>();
		Multimap<String, String> subjectMap = ArrayListMultimap.create();
		Multimap<String, String> resourceMap = ArrayListMultimap.create();
		Multimap<String, String> actionMap = ArrayListMultimap.create();
		Multimap<String, String> environmentMap = ArrayListMultimap.create();
		for (JsonNode node : policyInfo) {
			String attributeName = node.path("attributeName").asText();
			String attributeValue = node.path("attributeValue").asText();
			String attributeType = node.path("attributeType").asText();
			switch (attributeType) {
			case "Subject":
				subjectMap.put(attributeName, attributeValue);
				attrMap.put("Subject", subjectMap);
				break;
			case "Resource":
				resourceMap.put(attributeName, attributeValue);
				attrMap.put("Resource", resourceMap);
				break;
			case "Action":
				actionMap.put(attributeName, attributeValue);
				attrMap.put("Action", actionMap);
				break;
			case "Environment":
				environmentMap.put(attributeName, attributeValue);
				attrMap.put("Environment", environmentMap);
				break;
			default:
				break;
			}
		}
		return attrMap;
	}

	/***
	 * Exports to Excel File for Proposals and its Audit Logs
	 * 
	 * @param proposals
	 * @param proposalAuditLogs
	 * @return
	 * @throws URISyntaxException
	 * @throws JsonProcessingException
	 */
	public String exportToExcelFile(List<ProposalInfo> proposals, List<AuditLogInfo> proposalAuditLogs)
			throws URISyntaxException, JsonProcessingException {
		ObjectMapper mapper = new ObjectMapper();
		String filename = new String();
		Xcelite xcelite = new Xcelite();
		if (proposals != null) {
			XceliteSheet sheet = xcelite.createSheet("Proposals");
			SheetWriter<ProposalInfo> writer = sheet.getBeanWriter(ProposalInfo.class);
			writer.write(proposals);
		} else {
			XceliteSheet sheet = xcelite.createSheet("AuditLogs");
			SheetWriter<AuditLogInfo> writer = sheet.getBeanWriter(AuditLogInfo.class);
			writer.write(proposalAuditLogs);
		}
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy_MM_dd");
		Date date = new Date();
		String fileName = String.format("%s.%s",
				RandomStringUtils.randomAlphanumeric(8) + "_" + dateFormat.format(date), "xlsx");
		String downloadLocation = this.getClass().getResource("/uploads").toURI().getPath();
		xcelite.write(new File(downloadLocation + fileName));
		filename = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(fileName);
		return filename;
	}

	// Content Profiles

	/***
	 * Generates Content Profile For All Users
	 * 
	 * @param existingProposal
	 * @param contentProfile
	 * @param signatures
	 * @param requiredSignatures
	 */
	public void generateContentProfileForAllUsers(Proposal existingProposal, StringBuffer contentProfile,
			List<SignatureUserInfo> signatures, RequiredSignaturesInfo requiredSignatures) {
		if (!existingProposal.getInvestigatorInfo().getPi().getUserRef().isDeleted()) {
			generatePIContentProfile(contentProfile, existingProposal.getInvestigatorInfo().getPi());
			requiredSignatures.getRequiredPISign()
					.add(existingProposal.getInvestigatorInfo().getPi().getUserProfileId());
		}
		for (InvestigatorRefAndPosition copis : existingProposal.getInvestigatorInfo().getCo_pi()) {
			if (!copis.getUserRef().isDeleted()) {
				generateCoPIContentProfile(contentProfile, copis);
				requiredSignatures.getRequiredCoPISigns().add(copis.getUserProfileId());
			}
		}
		for (InvestigatorRefAndPosition seniors : existingProposal.getInvestigatorInfo().getSeniorPersonnel()) {
			if (!seniors.getUserRef().isDeleted()) {
				generateSeniorContentProfile(contentProfile, seniors);
			}
		}
		for (SignatureUserInfo signatureInfo : signatures) {
			switch (signatureInfo.getPositionTitle()) {
			case "Department Chair":
				generateChairContentProfile(contentProfile, signatureInfo);
				requiredSignatures.getRequiredChairSigns().add(signatureInfo.getUserProfileId());
				break;
			case "Business Manager":
				generateManagerContentProfile(contentProfile, signatureInfo);
				requiredSignatures.getRequiredBusinessManagerSigns().add(signatureInfo.getUserProfileId());
				break;
			case "Dean":
				generateDeanContentProfile(contentProfile, signatureInfo);
				requiredSignatures.getRequiredDeanSigns().add(signatureInfo.getUserProfileId());
				break;
			case "IRB":
				generateIRBContentProfile(contentProfile, signatureInfo);
				requiredSignatures.getRequiredIRBSigns().add(signatureInfo.getUserProfileId());
				break;
			case "University Research Administrator":
				generateResearchAdminContentProfile(contentProfile, signatureInfo);
				requiredSignatures.getRequiredResearchAdminSigns().add(signatureInfo.getUserProfileId());
				break;
			case "University Research Director":
				generateDirectorContentProfile(contentProfile, signatureInfo);
				requiredSignatures.getRequiredResearchDirectorSigns().add(signatureInfo.getUserProfileId());
				break;
			default:
				break;
			}
		}
	}

	/***
	 * Generates Users In Proposal Content Profile
	 * 
	 * @param authorProfile
	 * @param proposalId
	 * @param existingProposal
	 * @param signedByCurrentUser
	 * @param contentProfile
	 * @param signatures
	 * @param requiredSignatures
	 */
	public void generateUsersInProposalContentProfile(UserProfile authorProfile, String proposalId,
			Proposal existingProposal, boolean signedByCurrentUser, StringBuffer contentProfile,
			List<SignatureUserInfo> signatures, RequiredSignaturesInfo requiredSignatures) {
		contentProfile.append("<Content>");
		contentProfile.append("<ak:record xmlns:ak=\"http://akpower.org\">");
		genearteProposalInfoContentProfile(proposalId, existingProposal, contentProfile);
		generateAuthorContentProfile(contentProfile, authorProfile);
		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX");
		contentProfile.append("<ak:currentdatetime>");
		contentProfile.append(dateFormat.format(new Date()));
		contentProfile.append("</ak:currentdatetime>");

		generateContentProfileForAllUsers(existingProposal, contentProfile, signatures, requiredSignatures);

		getExistingSignaturesForProposal(contentProfile, existingProposal, requiredSignatures, signedByCurrentUser);
		checkForSignedByAllUsers(contentProfile, requiredSignatures, signedByCurrentUser);
		contentProfile.append("</ak:proposal>");
		contentProfile.append("</ak:record>");
		contentProfile.append("</Content>");
	}

	/***
	 * Generates Proposal Content Profile
	 * 
	 * @param authorProfile
	 * @param proposalId
	 * @param existingProposal
	 * @param signedByCurrentUser
	 * @param contentProfile
	 * @param irbApprovalRequired
	 * @param requiredSignatures
	 * @return
	 */
	public List<SignatureUserInfo> generateProposalContentProfile(UserProfile authorProfile, String proposalId,
			Proposal existingProposal, boolean signedByCurrentUser, StringBuffer contentProfile,
			Boolean irbApprovalRequired, RequiredSignaturesInfo requiredSignatures) {
		List<SignatureUserInfo> signatures = new ArrayList<SignatureUserInfo>();
		if (!proposalId.equals("0")) {
			ObjectId id = new ObjectId(proposalId);
			signatures = findSignaturesExceptInvestigator(id, irbApprovalRequired);
			generateUsersInProposalContentProfile(authorProfile, proposalId, existingProposal, signedByCurrentUser,
					contentProfile, signatures, requiredSignatures);
		} else {
			generateDefaultProposalContentProfile(authorProfile, proposalId, existingProposal, signedByCurrentUser,
					contentProfile);
		}
		contentProfile.append(
				"<Attribute AttributeId=\"urn:oasis:names:tc:xacml:3.0:content-selector\" IncludeInResult=\"false\">");
		contentProfile.append(
				"<AttributeValue XPathCategory=\"urn:oasis:names:tc:xacml:3.0:attribute-category:resource\" DataType=\"urn:oasis:names:tc:xacml:3.0:data-type:xpathExpression\">//ak:record/ak:proposal</AttributeValue>");
		contentProfile.append("</Attribute>");
		return signatures;
	}

	/***
	 * Generates MDP Decision For available Actions For a User
	 * 
	 * @param attrMap
	 * @param actionMap
	 * @param contentProfile
	 * @return
	 */
//	public List<String> generateMDPDecision(HashMap<String, Multimap<String, String>> attrMap,
//			Multimap<String, String> actionMap, StringBuffer contentProfile) {
//		List<String> attributeValue = Arrays.asList("Save", "Submit", "Approve", "Disapprove", "Withdraw", "Archive",
//				"Delete");
//		for (String action : attributeValue) {
//			actionMap.put("proposal.action", action);
//			attrMap.put("Action", actionMap);
//		}
//		Set<AbstractResult> results = ac.getXACMLdecisionForMDPWithProfile(attrMap, contentProfile);
//		List<String> actions = new ArrayList<String>();
//		for (AbstractResult result : results) {
//			if (AbstractResult.DECISION_PERMIT == result.getDecision()) {
//				Set<Attributes> attributesSet = ((Result) result).getAttributes();
//				for (Attributes attributes : attributesSet) {
//					for (Attribute attribute : attributes.getAttributes()) {
//						actions.add(attribute.getValue().encode());
//					}
//				}
//			}
//		}
//		return actions;
//	}

	/***
	 * Generates Author Content Profile
	 * 
	 * @param contentProfile
	 * @param authorProfile
	 */
	public void generateAuthorContentProfile(StringBuffer contentProfile, UserProfile authorProfile) {
		contentProfile.append("<ak:authorprofile>");
		contentProfile.append("<ak:fullname>");
		contentProfile.append(authorProfile.getFullName());
		contentProfile.append("</ak:fullname>");
		contentProfile.append("<ak:userid>");
		contentProfile.append(authorProfile.getId().toString());
		contentProfile.append("</ak:userid>");
		contentProfile.append("</ak:authorprofile>");
	}

	/***
	 * Generates University Research Director Content Profile
	 * 
	 * @param contentProfile
	 * @param signatureInfo
	 */
	public void generateDirectorContentProfile(StringBuffer contentProfile, SignatureUserInfo signatureInfo) {
		contentProfile.append("<ak:director>");
		contentProfile.append("<ak:fullname>");
		contentProfile.append(signatureInfo.getFullName());
		contentProfile.append("</ak:fullname>");
		contentProfile.append("<ak:workemail>");
		contentProfile.append(signatureInfo.getEmail());
		contentProfile.append("</ak:workemail>");
		contentProfile.append("<ak:userid>");
		contentProfile.append(signatureInfo.getUserProfileId());
		contentProfile.append("</ak:userid>");
		contentProfile.append("</ak:director>");
	}

	/***
	 * Generates University Research Administrator Content Profile
	 * 
	 * @param contentProfile
	 * @param signatureInfo
	 */
	public void generateResearchAdminContentProfile(StringBuffer contentProfile, SignatureUserInfo signatureInfo) {
		contentProfile.append("<ak:administrator>");
		contentProfile.append("<ak:fullname>");
		contentProfile.append(signatureInfo.getFullName());
		contentProfile.append("</ak:fullname>");
		contentProfile.append("<ak:workemail>");
		contentProfile.append(signatureInfo.getEmail());
		contentProfile.append("</ak:workemail>");
		contentProfile.append("<ak:userid>");
		contentProfile.append(signatureInfo.getUserProfileId());
		contentProfile.append("</ak:userid>");
		contentProfile.append("</ak:administrator>");
	}

	/***
	 * Generates IRB Content Profile
	 * 
	 * @param contentProfile
	 * @param signatureInfo
	 */
	public void generateIRBContentProfile(StringBuffer contentProfile, SignatureUserInfo signatureInfo) {
		contentProfile.append("<ak:irb>");
		contentProfile.append("<ak:fullname>");
		contentProfile.append(signatureInfo.getFullName());
		contentProfile.append("</ak:fullname>");
		contentProfile.append("<ak:workemail>");
		contentProfile.append(signatureInfo.getEmail());
		contentProfile.append("</ak:workemail>");
		contentProfile.append("<ak:userid>");
		contentProfile.append(signatureInfo.getUserProfileId());
		contentProfile.append("</ak:userid>");
		contentProfile.append("</ak:irb>");
	}

	/***
	 * Generates Dean Content Profile
	 * 
	 * @param contentProfile
	 * @param signatureInfo
	 */
	public void generateDeanContentProfile(StringBuffer contentProfile, SignatureUserInfo signatureInfo) {
		contentProfile.append("<ak:dean>");
		contentProfile.append("<ak:fullname>");
		contentProfile.append(signatureInfo.getFullName());
		contentProfile.append("</ak:fullname>");
		contentProfile.append("<ak:workemail>");
		contentProfile.append(signatureInfo.getEmail());
		contentProfile.append("</ak:workemail>");
		contentProfile.append("<ak:userid>");
		contentProfile.append(signatureInfo.getUserProfileId());
		contentProfile.append("</ak:userid>");
		contentProfile.append("</ak:dean>");
	}

	/***
	 * Generates Business Manager Content Profile
	 * 
	 * @param contentProfile
	 * @param signatureInfo
	 */
	public void generateManagerContentProfile(StringBuffer contentProfile, SignatureUserInfo signatureInfo) {
		contentProfile.append("<ak:manager>");
		contentProfile.append("<ak:fullname>");
		contentProfile.append(signatureInfo.getFullName());
		contentProfile.append("</ak:fullname>");
		contentProfile.append("<ak:workemail>");
		contentProfile.append(signatureInfo.getEmail());
		contentProfile.append("</ak:workemail>");
		contentProfile.append("<ak:userid>");
		contentProfile.append(signatureInfo.getUserProfileId());
		contentProfile.append("</ak:userid>");
		contentProfile.append("</ak:manager>");
	}

	/***
	 * Generates Department Chair Content Profile
	 * 
	 * @param contentProfile
	 * @param signatureInfo
	 */
	public void generateChairContentProfile(StringBuffer contentProfile, SignatureUserInfo signatureInfo) {
		contentProfile.append("<ak:chair>");
		contentProfile.append("<ak:fullname>");
		contentProfile.append(signatureInfo.getFullName());
		contentProfile.append("</ak:fullname>");
		contentProfile.append("<ak:workemail>");
		contentProfile.append(signatureInfo.getEmail());
		contentProfile.append("</ak:workemail>");
		contentProfile.append("<ak:userid>");
		contentProfile.append(signatureInfo.getUserProfileId());
		contentProfile.append("</ak:userid>");
		contentProfile.append("</ak:chair>");
	}

	/***
	 * Generates Senior Content Profile
	 * 
	 * @param contentProfile
	 * @param senior
	 */
	public void generateSeniorContentProfile(StringBuffer contentProfile, InvestigatorRefAndPosition senior) {
		contentProfile.append("<ak:senior>");
		contentProfile.append("<ak:fullname>");
		contentProfile.append(senior.getUserRef().getFullName());
		contentProfile.append("</ak:fullname>");
		contentProfile.append("<ak:workemail>");
		contentProfile.append(senior.getUserRef().getWorkEmails().get(0));
		contentProfile.append("</ak:workemail>");
		contentProfile.append("<ak:userid>");
		contentProfile.append(senior.getUserProfileId());
		contentProfile.append("</ak:userid>");
		contentProfile.append("</ak:senior>");
	}

	/***
	 * Generates Co-PI Content Profile
	 * 
	 * @param contentProfile
	 * @param copi
	 */
	public void generateCoPIContentProfile(StringBuffer contentProfile, InvestigatorRefAndPosition copi) {
		contentProfile.append("<ak:copi>");
		contentProfile.append("<ak:fullname>");
		contentProfile.append(copi.getUserRef().getFullName());
		contentProfile.append("</ak:fullname>");
		contentProfile.append("<ak:workemail>");
		contentProfile.append(copi.getUserRef().getWorkEmails().get(0));
		contentProfile.append("</ak:workemail>");
		contentProfile.append("<ak:userid>");
		contentProfile.append(copi.getUserProfileId());
		contentProfile.append("</ak:userid>");
		contentProfile.append("</ak:copi>");
	}

	/***
	 * Generates PI Content Profile
	 * 
	 * @param contentProfile
	 * @param pi
	 */
	public void generatePIContentProfile(StringBuffer contentProfile, InvestigatorRefAndPosition pi) {
		contentProfile.append("<ak:pi>");
		contentProfile.append("<ak:fullname>");
		contentProfile.append(pi.getUserRef().getFullName());
		contentProfile.append("</ak:fullname>");
		contentProfile.append("<ak:workemail>");
		contentProfile.append(pi.getUserRef().getWorkEmails().get(0));
		contentProfile.append("</ak:workemail>");
		contentProfile.append("<ak:userid>");
		contentProfile.append(pi.getUserProfileId());
		contentProfile.append("</ak:userid>");
		contentProfile.append("</ak:pi>");
	}

	/***
	 * Generates Signature Content Profile of a Proposal
	 * 
	 * @param contentProfile
	 * @param signatureInfo
	 */
	public void generateSignatureContentProfile(StringBuffer contentProfile, SignatureUserInfo signatureInfo) {
		switch (signatureInfo.getPositionTitle()) {
		case "Department Chair":
			generateChairContentProfile(contentProfile, signatureInfo);
			break;
		case "Business Manager":
			generateManagerContentProfile(contentProfile, signatureInfo);
			break;
		case "Dean":
			generateDeanContentProfile(contentProfile, signatureInfo);
			break;
		case "IRB":
			generateIRBContentProfile(contentProfile, signatureInfo);
			break;
		case "University Research Administrator":
			generateResearchAdminContentProfile(contentProfile, signatureInfo);
			break;
		case "University Research Director":
			generateDirectorContentProfile(contentProfile, signatureInfo);
			break;
		default:
			break;
		}
	}

	/***
	 * Generates Investigator Content Profile
	 * 
	 * @param existingProposal
	 * @param contentProfile
	 */
	public void generateInvestigatorContentProfile(Proposal existingProposal, StringBuffer contentProfile) {
		generatePIContentProfile(contentProfile, existingProposal.getInvestigatorInfo().getPi());
		for (InvestigatorRefAndPosition copi : existingProposal.getInvestigatorInfo().getCo_pi()) {
			generateCoPIContentProfile(contentProfile, copi);
		}
		for (InvestigatorRefAndPosition senior : existingProposal.getInvestigatorInfo().getSeniorPersonnel()) {
			generateSeniorContentProfile(contentProfile, senior);
		}
	}

	/***
	 * Generates Proposal Info Content Profile
	 * 
	 * @param proposalId
	 * @param existingProposal
	 * @param contentProfile
	 */
	public void genearteProposalInfoContentProfile(String proposalId, Proposal existingProposal,
			StringBuffer contentProfile) {
		contentProfile.append("<ak:proposal>");
		contentProfile.append("<ak:proposalid>");
		contentProfile.append(proposalId);
		contentProfile.append("</ak:proposalid>");
		contentProfile.append("<ak:proposaltitle>");
		contentProfile.append(existingProposal.getProjectInfo().getProjectTitle());
		contentProfile.append("</ak:proposaltitle>");
		contentProfile.append("<ak:irbApprovalRequired>");
		contentProfile.append(existingProposal.isIrbApprovalRequired());
		contentProfile.append("</ak:irbApprovalRequired>");
		contentProfile.append("<ak:submittedbypi>");
		contentProfile.append(existingProposal.getSubmittedByPI().name());
		contentProfile.append("</ak:submittedbypi>");
		contentProfile.append("<ak:readyforsubmissionbypi>");
		contentProfile.append(existingProposal.isReadyForSubmissionByPI());
		contentProfile.append("</ak:readyforsubmissionbypi>");
		contentProfile.append("<ak:deletedbypi>");
		contentProfile.append(existingProposal.getDeletedByPI().name());
		contentProfile.append("</ak:deletedbypi>");
		contentProfile.append("<ak:approvedbydepartmentchair>");
		contentProfile.append(existingProposal.getChairApproval().name());
		contentProfile.append("</ak:approvedbydepartmentchair>");
		contentProfile.append("<ak:approvedbybusinessmanager>");
		contentProfile.append(existingProposal.getBusinessManagerApproval().name());
		contentProfile.append("</ak:approvedbybusinessmanager>");
		contentProfile.append("<ak:approvedbyirb>");
		contentProfile.append(existingProposal.getIrbApproval().name());
		contentProfile.append("</ak:approvedbyirb>");
		contentProfile.append("<ak:approvedbydean>");
		contentProfile.append(existingProposal.getDeanApproval().name());
		contentProfile.append("</ak:approvedbydean>");
		contentProfile.append("<ak:approvedbyuniversityresearchadministrator>");
		contentProfile.append(existingProposal.getResearchAdministratorApproval().name());
		contentProfile.append("</ak:approvedbyuniversityresearchadministrator>");
		contentProfile.append("<ak:withdrawnbyuniversityresearchadministrator>");
		contentProfile.append(existingProposal.getResearchAdministratorWithdraw().name());
		contentProfile.append("</ak:withdrawnbyuniversityresearchadministrator>");
		contentProfile.append("<ak:submittedbyuniversityresearchadministrator>");
		contentProfile.append(existingProposal.getResearchAdministratorSubmission().name());
		contentProfile.append("</ak:submittedbyuniversityresearchadministrator>");
		contentProfile.append("<ak:approvedbyuniversityresearchdirector>");
		contentProfile.append(existingProposal.getResearchDirectorApproval().name());
		contentProfile.append("</ak:approvedbyuniversityresearchdirector>");
		contentProfile.append("<ak:deletedbyuniversityresearchdirector>");
		contentProfile.append(existingProposal.getResearchDirectorDeletion().name());
		contentProfile.append("</ak:deletedbyuniversityresearchdirector>");
		contentProfile.append("<ak:archivedbyuniversityresearchdirector>");
		contentProfile.append(existingProposal.getResearchDirectorArchived().name());
		contentProfile.append("</ak:archivedbyuniversityresearchdirector>");
	}

	/***
	 * Generates Proposal Content Profile with Current Datetime
	 * 
	 * @param proposalId
	 * @param userInfo
	 * @param existingProposal
	 * @return
	 */
	public StringBuffer generateProposalContentProfile(String proposalId, GPMSCommonInfo userInfo,
			Proposal existingProposal) {
		StringBuffer contentProfile = new StringBuffer();
		contentProfile.append("<Content>");
		contentProfile.append("<ak:record xmlns:ak=\"http://akpower.org\">");
		genearteProposalInfoContentProfile(proposalId, existingProposal, contentProfile);
		contentProfile.append("<ak:authorprofile>");
		contentProfile.append("<ak:userid>");
		contentProfile.append(userInfo.getUserProfileID());
		contentProfile.append("</ak:userid>");
		contentProfile.append("</ak:authorprofile>");
		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX");
		contentProfile.append("<ak:currentdatetime>");
		contentProfile.append(dateFormat.format(new Date()));
		contentProfile.append("</ak:currentdatetime>");
		contentProfile.append("</ak:proposal>");
		contentProfile.append("</ak:record>");
		contentProfile.append("</Content>");
		contentProfile.append(
				"<Attribute AttributeId=\"urn:oasis:names:tc:xacml:3.0:content-selector\" IncludeInResult=\"false\">");
		contentProfile.append(
				"<AttributeValue XPathCategory=\"urn:oasis:names:tc:xacml:3.0:attribute-category:resource\" DataType=\"urn:oasis:names:tc:xacml:3.0:data-type:xpathExpression\">//ak:record/ak:proposal</AttributeValue>");
		contentProfile.append("</Attribute>");
		return contentProfile;
	}

	/***
	 * Generates Default Proposal Content Profile
	 * 
	 * @param authorProfile
	 * @param proposalId
	 * @param existingProposal
	 * @param signedByCurrentUser
	 * @param contentProfile
	 */
	public void generateDefaultProposalContentProfile(UserProfile authorProfile, String proposalId,
			Proposal existingProposal, boolean signedByCurrentUser, StringBuffer contentProfile) {
		contentProfile.append("<Content>");
		contentProfile.append("<ak:record xmlns:ak=\"http://akpower.org\">");
		contentProfile.append("<ak:proposal>");
		contentProfile.append("<ak:proposalid>");
		contentProfile.append(proposalId);
		contentProfile.append("</ak:proposalid>");
		contentProfile.append("<ak:proposaltitle>");
		contentProfile.append(existingProposal.getProjectInfo().getProjectTitle());
		contentProfile.append("</ak:proposaltitle>");
		contentProfile.append("<ak:irbApprovalRequired>");
		contentProfile.append(existingProposal.isIrbApprovalRequired());
		contentProfile.append("</ak:irbApprovalRequired>");
		generateAuthorContentProfile(contentProfile, authorProfile);
		generateInvestigatorContentProfile(existingProposal, contentProfile);
		contentProfile.append("<ak:signedByCurrentUser>");
		contentProfile.append(signedByCurrentUser);
		contentProfile.append("</ak:signedByCurrentUser>");
		contentProfile.append("</ak:proposal>");
		contentProfile.append("</ak:record>");
		contentProfile.append("</Content>");
	}

	/***
	 * Generates Content Profile for a Proposal
	 * 
	 * @param proposalId
	 * @param existingProposal
	 * @param signatures
	 * @param authorProfile
	 * @return
	 */
	public StringBuffer generateContentProfile(String proposalId, Proposal existingProposal,
			List<SignatureUserInfo> signatures, UserProfile authorProfile) {
		StringBuffer contentProfile = new StringBuffer();
		contentProfile.append("<Content>");
		contentProfile.append("<ak:record xmlns:ak=\"http://akpower.org\">");
		genearteProposalInfoContentProfile(proposalId, existingProposal, contentProfile);
		generateAuthorContentProfile(contentProfile, authorProfile);
		generateInvestigatorContentProfile(existingProposal, contentProfile);
		for (SignatureUserInfo signatureInfo : signatures) {
			generateSignatureContentProfile(contentProfile, signatureInfo);
		}
		contentProfile.append("</ak:proposal>");
		contentProfile.append("</ak:record>");
		contentProfile.append("</Content>");
		contentProfile.append(
				"<Attribute AttributeId=\"urn:oasis:names:tc:xacml:3.0:content-selector\" IncludeInResult=\"false\">");
		contentProfile.append(
				"<AttributeValue XPathCategory=\"urn:oasis:names:tc:xacml:3.0:attribute-category:resource\" DataType=\"urn:oasis:names:tc:xacml:3.0:data-type:xpathExpression\">//ak:record/ak:proposal</AttributeValue>");
		contentProfile.append("</Attribute>");
		return contentProfile;
	}

	/**
	 * Updates Proposal's Investigator List
	 * 
	 * @param existingProposal
	 * @param oldProposal
	 * @param addedInvestigators
	 * @param deletedInvestigators
	 */
	public void updateInvestigatorList(Proposal existingProposal, Proposal oldProposal,
			InvestigatorInfo addedInvestigators, InvestigatorInfo deletedInvestigators) {
		InvestigatorInfo existingInvestigators = new InvestigatorInfo();
		existingInvestigators = oldProposal.getInvestigatorInfo();
		for (InvestigatorRefAndPosition coPI : existingInvestigators.getCo_pi()) {
			if (!existingProposal.getInvestigatorInfo().getCo_pi().contains(coPI)) {
				if (!deletedInvestigators.getCo_pi().contains(coPI)) {
					deletedInvestigators.getCo_pi().add(coPI);
					existingProposal.getInvestigatorInfo().getCo_pi().remove(coPI);
				}
			} else {
				addedInvestigators.getCo_pi().remove(coPI);
			}
		}
		for (InvestigatorRefAndPosition senior : existingInvestigators.getSeniorPersonnel()) {
			if (!existingProposal.getInvestigatorInfo().getSeniorPersonnel().contains(senior)) {
				if (!deletedInvestigators.getSeniorPersonnel().contains(senior)) {
					deletedInvestigators.getSeniorPersonnel().add(senior);
					existingProposal.getInvestigatorInfo().getSeniorPersonnel().remove(senior);
				}
			} else {
				addedInvestigators.getSeniorPersonnel().remove(senior);
			}
		}
		// Remove Signatures FOR Deleted Investigators
		for (InvestigatorRefAndPosition coPI : deletedInvestigators.getCo_pi()) {
			for (SignatureInfo sign : oldProposal.getSignatureInfo()) {
				if (coPI.getUserProfileId().equalsIgnoreCase(sign.getUserProfileId())) {
					existingProposal.getSignatureInfo().remove(sign);
				}
			}
		}
	}

	// AuditLog Functions

	/***
	 * Finds All Logs in a AuditLog Grid for a Proposal
	 * 
	 * @param offset
	 * @param limit
	 * @param id
	 * @param auditLogInfo
	 * @return
	 * @throws ParseException
	 */
	public List<AuditLogInfo> findAllProposalAuditLogForGrid(int offset, int limit, ObjectId id,
			AuditLogCommonInfo auditLogInfo) throws ParseException {
		return getAuditListBasedOnPaging(offset, limit, getSortedAuditLogResults(auditLogInfo, id));
	}

	/***
	 * Gets Audit Logs list based On User provided Paging size
	 * 
	 * @param offset
	 * @param limit
	 * @param allAuditLogs
	 * @return
	 */
	private List<AuditLogInfo> getAuditListBasedOnPaging(int offset, int limit, List<AuditLogInfo> allAuditLogs) {
		int rowTotal = 0;
		rowTotal = allAuditLogs.size();
		if (rowTotal > 0) {
			for (AuditLogInfo t : allAuditLogs) {
				t.setRowTotal(rowTotal);
			}
		}
		if (rowTotal >= (offset + limit - 1)) {
			return allAuditLogs.subList(offset - 1, offset + limit - 1);
		} else {
			return allAuditLogs.subList(offset - 1, rowTotal);
		}
	}

	/***
	 * Gets Sorted Audit Logs List
	 * 
	 * @param auditLogInfo
	 * @param id
	 * @return
	 * @throws ParseException
	 */
	public List<AuditLogInfo> getSortedAuditLogResults(AuditLogCommonInfo auditLogInfo, ObjectId id)
			throws ParseException {
		Datastore ds = getDatastore();
		Query<Proposal> proposalQuery = ds.createQuery(Proposal.class);
		Proposal q = proposalQuery.field("_id").equal(id).get();
		List<AuditLogInfo> allAuditLogs = new ArrayList<AuditLogInfo>();
		if (q.getAuditLog() != null && q.getAuditLog().size() != 0) {
			for (AuditLog poposalAudit : q.getAuditLog()) {
				AuditLogInfo proposalAuditLog = new AuditLogInfo();
				boolean isActionMatch = isAuditLogActionFieldProvided(auditLogInfo.getAction(), poposalAudit);
				boolean isAuditedByMatch = isAuditLogAuditedByFieldProvided(auditLogInfo.getAuditedBy(), poposalAudit);
				boolean isActivityDateFromMatch = isAuditLogActivityDateFromProvided(auditLogInfo.getActivityOnFrom(),
						poposalAudit);
				boolean isActivityDateToMatch = isAuditLogActivityDateToProvided(auditLogInfo.getActivityOnTo(),
						poposalAudit);

				if (isActionMatch && isAuditedByMatch && isActivityDateFromMatch && isActivityDateToMatch) {
					proposalAuditLog.setUserName(poposalAudit.getUserProfile().getUserAccount().getUserName());
					proposalAuditLog.setUserFullName(poposalAudit.getUserProfile().getFullName());
					proposalAuditLog.setAction(poposalAudit.getAction());
					proposalAuditLog.setActivityDate(poposalAudit.getActivityDate());
					allAuditLogs.add(proposalAuditLog);
				}
			}
		}
		Collections.sort(allAuditLogs);
		return allAuditLogs;
	}

	/***
	 * Is Audit Log Activity Date To Provided
	 * 
	 * @param activityOnTo
	 * @param poposalAudit
	 * @return
	 * @throws ParseException
	 */
	private boolean isAuditLogActivityDateToProvided(String activityOnTo, AuditLog poposalAudit) throws ParseException {
		if (activityOnTo != null) {
			Date activityDateTo = formatter.parse(activityOnTo);
			if (poposalAudit.getActivityDate().compareTo(activityDateTo) > 0) {
				return false;
			} else if (poposalAudit.getActivityDate().compareTo(activityDateTo) < 0) {
				return true;
			} else if (poposalAudit.getActivityDate().compareTo(activityDateTo) == 0) {
				return true;
			}
		} else {
			return true;
		}
		return false;
	}

	/***
	 * Is Audit Log Activity Date From Provided
	 * 
	 * @param activityOnFrom
	 * @param poposalAudit
	 * @return
	 * @throws ParseException
	 */
	private boolean isAuditLogActivityDateFromProvided(String activityOnFrom, AuditLog poposalAudit)
			throws ParseException {
		if (activityOnFrom != null) {
			Date activityDateFrom = formatter.parse(activityOnFrom);
			if (poposalAudit.getActivityDate().compareTo(activityDateFrom) > 0) {
				return true;
			} else if (poposalAudit.getActivityDate().compareTo(activityDateFrom) < 0) {
				return false;
			} else if (poposalAudit.getActivityDate().compareTo(activityDateFrom) == 0) {
				return true;
			}
		} else {
			return true;
		}
		return false;
	}

	/***
	 * Is Audit Log Audited By Provided
	 * 
	 * @param auditedBy
	 * @param poposalAudit
	 * @return
	 */
	private boolean isAuditLogAuditedByFieldProvided(String auditedBy, AuditLog poposalAudit) {
		if (auditedBy != null) {
			if (poposalAudit.getUserProfile().getUserAccount().getUserName().toLowerCase()
					.contains(auditedBy.toLowerCase())) {
				return true;
			} else if (poposalAudit.getUserProfile().getFirstName().toLowerCase().contains(auditedBy.toLowerCase())) {
				return true;
			} else if (poposalAudit.getUserProfile().getMiddleName().toLowerCase().contains(auditedBy.toLowerCase())) {
				return true;
			} else if (poposalAudit.getUserProfile().getLastName().toLowerCase().contains(auditedBy.toLowerCase())) {
				return true;
			}
		} else {
			return true;
		}
		return false;
	}

	/***
	 * Is Audit Log Action Provided
	 * 
	 * @param action
	 * @param poposalAudit
	 * @return
	 */
	private boolean isAuditLogActionFieldProvided(String action, AuditLog poposalAudit) {
		if (action != null) {
			if (poposalAudit.getAction().toLowerCase().contains(action.toLowerCase())) {
				return true;
			}
		} else {
			return true;
		}
		return false;
	}
}
