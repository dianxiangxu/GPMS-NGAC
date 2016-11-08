package gpms.dao;

import gpms.DAL.MongoDBConnector;
import gpms.accesscontrol.BalanaConnector;
import gpms.model.AdditionalInfo;
import gpms.model.Appendix;
import gpms.model.ApprovalType;
import gpms.model.ArchiveType;
import gpms.model.AuditLog;
import gpms.model.AuditLogCommonInfo;
import gpms.model.AuditLogInfo;
import gpms.model.BaseInfo;
import gpms.model.BaseOptions;
import gpms.model.BasePIEligibilityOptions;
import gpms.model.CollaborationInfo;
import gpms.model.CollegeDepartmentInfo;
import gpms.model.ComplianceInfo;
import gpms.model.ConfidentialInfo;
import gpms.model.ConflictOfInterest;
import gpms.model.CostShareInfo;
import gpms.model.Delegation;
import gpms.model.DeleteType;
import gpms.model.EmailCommonInfo;
import gpms.model.FundingSource;
import gpms.model.GPMSCommonInfo;
import gpms.model.InvestigatorRefAndPosition;
import gpms.model.OSPSectionInfo;
import gpms.model.PositionDetails;
import gpms.model.ProjectInfo;
import gpms.model.ProjectLocation;
import gpms.model.ProjectPeriod;
import gpms.model.ProjectType;
import gpms.model.Proposal;
import gpms.model.ProposalCommonInfo;
import gpms.model.ProposalInfo;
import gpms.model.Recovery;
import gpms.model.SignatureByAllUsers;
import gpms.model.SignatureInfo;
import gpms.model.SignatureUserInfo;
import gpms.model.SponsorAndBudgetInfo;
import gpms.model.Status;
import gpms.model.SubmitType;
import gpms.model.TypeOfRequest;
import gpms.model.UniversityCommitments;
import gpms.model.UserProfile;
import gpms.model.WithdrawType;
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
import org.wso2.balana.ObligationResult;
import org.wso2.balana.ctx.AbstractResult;
import org.wso2.balana.ctx.Attribute;
import org.wso2.balana.ctx.AttributeAssignment;
import org.wso2.balana.ctx.xacml3.Result;
import org.wso2.balana.xacml3.Attributes;

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

public class ProposalDAO extends BasicDAO<Proposal, String> {
	private static final String DBNAME = "db_gpms";
	public static final String COLLECTION_NAME = "proposal";

	private static Morphia morphia;
	private static Datastore ds;
	private AuditLog audit = new AuditLog();
	private DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");

	private static Morphia getMorphia() throws UnknownHostException,
			MongoException {
		if (morphia == null) {
			morphia = new Morphia().map(Proposal.class);
		}
		return morphia;
	}

	@Override
	public Datastore getDatastore() {
		if (ds == null) {
			try {
				ds = getMorphia().createDatastore(MongoDBConnector.getMongo(),
						DBNAME);
			} catch (UnknownHostException | MongoException e) {
				e.printStackTrace();
			}
		}
		return ds;
	}

	public ProposalDAO(MongoClient mongo, Morphia morphia, String dbName) {
		super(mongo, morphia, dbName);
	}

	public Proposal findProposalByProposalID(ObjectId id)
			throws UnknownHostException {
		Datastore ds = getDatastore();
		return ds.createQuery(Proposal.class).field("_id").equal(id).get();
	}

	public void saveProposal(Proposal newProposal, UserProfile authorProfile) {
		Datastore ds = getDatastore();
		audit = new AuditLog(authorProfile, "Created proposal by "
				+ authorProfile.getUserAccount().getUserName(), new Date());
		newProposal.getAuditLog().add(audit);
		ds.save(newProposal);
	}

	public void updateProposal(Proposal existingProposal,
			UserProfile authorProfile) {
		Datastore ds = getDatastore();
		audit = new AuditLog(authorProfile, "Updated proposal by "
				+ authorProfile.getUserAccount().getUserName(), new Date());
		existingProposal.getAuditLog().add(audit);
		ds.save(existingProposal);
	}

	public boolean updateProposalStatus(Proposal existingProposal,
			UserProfile authorProfile) {
		Datastore ds = getDatastore();
		boolean isStatusUpdated = false;
		audit = new AuditLog(authorProfile, "Updated proposal by "
				+ authorProfile.getUserAccount().getUserName(), new Date());
		existingProposal.getAuditLog().add(audit);
		ds.save(existingProposal);
		isStatusUpdated = true;
		return isStatusUpdated;
	}

	public boolean deleteProposalByAdmin(Proposal proposal,
			UserProfile authorProfile) {
		Datastore ds = getDatastore();
		proposal.setResearchDirectorDeletion(DeleteType.DELETED);
		proposal.getProposalStatus().clear();
		proposal.getProposalStatus().add(Status.DELETEDBYADMIN);
		AuditLog entry = new AuditLog(authorProfile, "Deleted Proposal by "
				+ authorProfile.getUserAccount().getUserName(), new Date());
		proposal.getAuditLog().add(entry);
		ds.save(proposal);
		return true;
	}

	public boolean deleteProposal(Proposal proposal, String proposalRoles,
			String proposalUserTitle, UserProfile authorProfile) {
		Datastore ds = getDatastore();
		if (proposal.getSubmittedByPI() == SubmitType.NOTSUBMITTED
				&& proposal.getDeletedByPI() == DeleteType.NOTDELETED
				&& proposalRoles.equals("PI")
				&& !proposalUserTitle.equals("University Research Director")) {
			proposal.setDeletedByPI(DeleteType.DELETED);
			proposal.getProposalStatus().clear();
			proposal.getProposalStatus().add(Status.DELETEDBYPI);
			AuditLog entry = new AuditLog(authorProfile, "Deleted Proposal by "
					+ authorProfile.getUserAccount().getUserName(), new Date());
			proposal.getAuditLog().add(entry);
			ds.save(proposal);
			return true;
		} else if (proposal.getResearchDirectorDeletion() == DeleteType.NOTDELETED
				&& proposal.getResearchDirectorApproval() == ApprovalType.READYFORAPPROVAL
				&& !proposalRoles.equals("PI")
				&& proposalUserTitle.equals("University Research Director")) {
			proposal.setResearchDirectorDeletion(DeleteType.DELETED);
			proposal.setResearchDirectorApproval(ApprovalType.NOTREADYFORAPPROVAL);
			proposal.getProposalStatus().clear();
			proposal.getProposalStatus().add(Status.DELETEDBYRESEARCHDIRECTOR);
			AuditLog entry = new AuditLog(authorProfile, "Deleted Proposal by "
					+ authorProfile.getUserAccount().getUserName(), new Date());
			proposal.getAuditLog().add(entry);
			ds.save(proposal);
			return true;
		}
		return false;
	}

	/**
	 * Finds All Logs in a AuditLog Grid for a Proposal
	 * 
	 * @param offset
	 * @param limit
	 * @param id
	 * @param auditLogInfo
	 * @return
	 * @throws ParseException
	 */
	public List<AuditLogInfo> findAllForProposalAuditLogGrid(int offset,
			int limit, ObjectId id, AuditLogCommonInfo auditLogInfo)
			throws ParseException {

		return getAuditListBasedOnPageing(
				offset,
				limit,
				getAuditLogResults(auditLogInfo.getAction(),
						auditLogInfo.getAuditedBy(),
						auditLogInfo.getActivityOnFrom(),
						auditLogInfo.getActivityOnTo(), id));
	}

	/**
	 * @param offset
	 * @param limit
	 * @param allAuditLogs
	 * @return
	 */
	private List<AuditLogInfo> getAuditListBasedOnPageing(int offset,
			int limit, List<AuditLogInfo> allAuditLogs) {

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
	 * 
	 * @param action
	 * @param auditedBy
	 * @param activityOnFrom
	 * @param activityOnTo
	 * @param id
	 * @return
	 * @throws ParseException
	 */
	private List<AuditLogInfo> getAuditLogResults(String action,
			String auditedBy, String activityOnFrom, String activityOnTo,
			ObjectId id) throws ParseException {

		Query<Proposal> proposalQuery = getDatastore().createQuery(
				Proposal.class);
		Proposal q = proposalQuery.field("_id").equal(id).get();
		List<AuditLogInfo> allAuditLogs = new ArrayList<AuditLogInfo>();
		if (q.getAuditLog() != null && q.getAuditLog().size() != 0) {

			for (AuditLog poposalAudit : q.getAuditLog()) {

				AuditLogInfo proposalAuditLog = new AuditLogInfo();
				boolean isActionMatch = isAuditLogActionFieldSelected(action,
						poposalAudit);
				boolean isAuditedByMatch = isAuditLogAuditedByFieldSelected(
						auditedBy, poposalAudit);
				boolean isActivityDateFromMatch = isAuditLogActivityDateFromSelected(
						activityOnFrom, poposalAudit);
				boolean isActivityDateToMatch = isAuditLogActivityDateToSelected(
						activityOnTo, poposalAudit);

				if (isActionMatch && isAuditedByMatch
						&& isActivityDateFromMatch && isActivityDateToMatch) {
					proposalAuditLog.setUserName(poposalAudit.getUserProfile()
							.getUserAccount().getUserName());
					proposalAuditLog.setUserFullName(poposalAudit
							.getUserProfile().getFullName());
					proposalAuditLog.setAction(poposalAudit.getAction());
					proposalAuditLog.setActivityDate(poposalAudit
							.getActivityDate());
					allAuditLogs.add(proposalAuditLog);
				}
			}
		}
		Collections.sort(allAuditLogs);
		return allAuditLogs;
	}

	/**
	 * @param activityOnTo
	 * @param poposalAudit
	 * @param isActivityDateToMatch
	 * @return
	 * @throws ParseException
	 */
	private boolean isAuditLogActivityDateToSelected(String activityOnTo,
			AuditLog poposalAudit) throws ParseException {

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

	/**
	 * @param activityOnFrom
	 * @param poposalAudit
	 * @param isActivityDateFromMatch
	 * @return
	 * @throws ParseException
	 */
	private boolean isAuditLogActivityDateFromSelected(String activityOnFrom,
			AuditLog poposalAudit) throws ParseException {

		if (activityOnFrom != null) {
			Date activityDateFrom = formatter.parse(activityOnFrom);
			if (poposalAudit.getActivityDate().compareTo(activityDateFrom) > 0) {
				return true;
			} else if (poposalAudit.getActivityDate().compareTo(
					activityDateFrom) < 0) {
				return false;
			} else if (poposalAudit.getActivityDate().compareTo(
					activityDateFrom) == 0) {
				return true;
			}
		} else {
			return true;
		}
		return false;
	}

	/**
	 * @param auditedBy
	 * @param poposalAudit
	 * @param isAuditedByMatch
	 * @return
	 */
	private boolean isAuditLogAuditedByFieldSelected(String auditedBy,
			AuditLog poposalAudit) {

		if (auditedBy != null) {
			if (poposalAudit.getUserProfile().getUserAccount().getUserName()
					.toLowerCase().contains(auditedBy.toLowerCase())) {
				return true;
			} else if (poposalAudit.getUserProfile().getFirstName()
					.toLowerCase().contains(auditedBy.toLowerCase())) {
				return true;
			} else if (poposalAudit.getUserProfile().getMiddleName()
					.toLowerCase().contains(auditedBy.toLowerCase())) {
				return true;
			} else if (poposalAudit.getUserProfile().getLastName()
					.toLowerCase().contains(auditedBy.toLowerCase())) {
				return true;
			}
		} else {
			return true;
		}
		return false;
	}

	/**
	 * @param action
	 * @param poposalAudit
	 * @param isActionMatch
	 * @return
	 */
	private boolean isAuditLogActionFieldSelected(String action,
			AuditLog poposalAudit) {

		if (action != null) {
			if (poposalAudit.getAction().toLowerCase()
					.contains(action.toLowerCase())) {
				return true;
			}
		} else {
			return true;
		}
		return false;
	}

	/***
	 * Gets Audit Logs for User Export
	 * 
	 * @param id
	 * @param auditLogInfo
	 * @return
	 * @throws ParseException
	 */
	public List<AuditLogInfo> findAllUserProposalAuditLogs(ObjectId id,
			AuditLogCommonInfo auditLogInfo) throws ParseException {
		return getAuditLogResults(auditLogInfo.getAction(),
				auditLogInfo.getAuditedBy(), auditLogInfo.getActivityOnFrom(),
				auditLogInfo.getActivityOnTo(), id);
	}

	public Proposal findNextProposalWithSameProjectTitle(ObjectId id,
			String newProjectTitle) {

		Query<Proposal> proposalQuery = getDatastore().createQuery(
				Proposal.class);
		proposalQuery.and(proposalQuery.criteria("_id").notEqual(id),
				proposalQuery.criteria("project info.project title")
						.equalIgnoreCase(newProjectTitle));
		return proposalQuery.get();
	}

	public Proposal findAnyProposalWithSameProjectTitle(String newProjectTitle) {

		Query<Proposal> proposalQuery = getDatastore().createQuery(
				Proposal.class);
		proposalQuery.criteria("project info.project title").equalIgnoreCase(
				newProjectTitle);
		return proposalQuery.get();
	}

	/**
	 * 
	 * @param offset
	 * @param limit
	 * @param proposalInfo
	 * @return
	 * @throws ParseException
	 */
	public List<ProposalInfo> findAllForProposalGrid(int offset, int limit,
			ProposalCommonInfo proposalInfo) throws ParseException {

		Query<Proposal> queryPropsal = getMyProposalSearchSelectedFields(proposalInfo);
		List<Proposal> allCurrentLoginUserProposalsList = queryPropsal
				.offset(offset - 1).limit(limit)
				.order("-audit log.activity on").asList();
		return getMyProposalGridForTheCurrentUser(queryPropsal.asList().size(),
				allCurrentLoginUserProposalsList);

	}

	/**
	 * 
	 * @param proposalInfo
	 * @param ds
	 * @param proposalQuery
	 * @throws ParseException
	 * @Refactored
	 */
	private Query<Proposal> getMyProposalSearchSelectedFields(
			ProposalCommonInfo proposalInfo) throws ParseException {

		Datastore ds = getDatastore();
		Query<Proposal> proposalQuery = ds.createQuery(Proposal.class);
		if (proposalInfo.getProjectTitle() != null) {
			proposalQuery.field("project info.project title")
					.containsIgnoreCase(proposalInfo.getProjectTitle());
		}

		if (proposalInfo.getSubmittedOnFrom() != null
				&& !proposalInfo.getSubmittedOnFrom().isEmpty()) {
			Date receivedOnF = formatter.parse(proposalInfo
					.getSubmittedOnFrom());
			proposalQuery.field("date submitted").greaterThanOrEq(receivedOnF);
		}
		if (proposalInfo.getSubmittedOnTo() != null
				&& !proposalInfo.getSubmittedOnTo().isEmpty()) {
			Date receivedOnT = formatter.parse(proposalInfo.getSubmittedOnTo());
			proposalQuery.field("date submitted").lessThanOrEq(receivedOnT);
		}

		if (proposalInfo.getTotalCostsFrom() != null
				&& proposalInfo.getTotalCostsFrom() != 0.0) {
			proposalQuery.field("sponsor and budget info.total costs")
					.greaterThanOrEq(proposalInfo.getTotalCostsFrom());
		}
		if (proposalInfo.getTotalCostsTo() != null
				&& proposalInfo.getTotalCostsTo() != 0.0) {
			proposalQuery.field("sponsor and budget info.total costs")
					.lessThanOrEq(proposalInfo.getTotalCostsTo());
		}
		if (proposalInfo.getProposalStatus() != null) {
			proposalQuery.field("proposal status").contains(
					proposalInfo.getProposalStatus());
		}

		String usernameBy = proposalInfo.getUsernameBy();

		if (usernameBy != null) {
			Query<UserProfile> profileQuery = ds.createQuery(UserProfile.class);
			profileQuery.or(
					profileQuery.criteria("first name").containsIgnoreCase(
							usernameBy),
					profileQuery.criteria("middle name").containsIgnoreCase(
							usernameBy), profileQuery.criteria("last name")
							.containsIgnoreCase(usernameBy));
			proposalQuery.or(
					proposalQuery.criteria("investigator info.pi.user profile")
							.in(profileQuery.asKeyList()),
					proposalQuery.criteria(
							"investigator info.co_pi.user profile").in(
							profileQuery.asKeyList()),
					proposalQuery.criteria(
							"investigator info.senior personnel.user profile")
							.in(profileQuery.asKeyList()));
		}

		return proposalQuery;
	}

	/**
	 * @param proposalsGridInfoList
	 * @param rowTotal
	 * @param allCurrentLoginUserProposalsList
	 * @Refactored
	 */
	private List<ProposalInfo> getMyProposalGridForTheCurrentUser(int rowTotal,
			List<Proposal> allCurrentLoginUserProposalsList) {

		List<ProposalInfo> proposalsGridInfoList = new ArrayList<ProposalInfo>();

		for (Proposal userProposal : allCurrentLoginUserProposalsList) {
			ProposalInfo proposalGridInfo = new ProposalInfo();
			// Proposal
			proposalGridInfo.setRowTotal(rowTotal);
			proposalGridInfo.setId(userProposal.getId().toString());
			// ProjectInfo
			setProposalProjectInfo(userProposal, proposalGridInfo);

			// SponsorAndBudgetInfo
			setSponsorAndBudgetInfo(userProposal, proposalGridInfo);

			// Proposal Status
			setCurrentProposalStatusAttribute(userProposal, proposalGridInfo);

			// set audit log
			setLastProposalModificationAuditLog(userProposal, proposalGridInfo);

			setProposalPIInfo(userProposal, proposalGridInfo);

			setPropsalCoPIsInfo(userProposal, proposalGridInfo);

			setProposalSeniorsInfo(userProposal, proposalGridInfo);

			proposalsGridInfoList.add(proposalGridInfo);
		}
		return proposalsGridInfoList;
	}

	/**
	 * @param userProposal
	 * @param proposalGridInfo
	 * @Refactored
	 */
	private void setProposalSeniorsInfo(Proposal userProposal,
			ProposalInfo proposalGridInfo) {
		List<InvestigatorRefAndPosition> allSeniors = userProposal
				.getInvestigatorInfo().getSeniorPersonnel();
		for (InvestigatorRefAndPosition senior : allSeniors) {
			String seniorUser = senior.getUserProfileId();
			proposalGridInfo.getSeniorUsers().add(seniorUser);
			if (!proposalGridInfo.getAllUsers().contains(seniorUser)) {
				proposalGridInfo.getAllUsers().add(seniorUser);
			}
		}
	}

	/**
	 * @param userProposal
	 * @param proposalGridInfo
	 * @Refactored
	 */
	private void setPropsalCoPIsInfo(Proposal userProposal,
			ProposalInfo proposalGridInfo) {
		List<InvestigatorRefAndPosition> allCoPI = userProposal
				.getInvestigatorInfo().getCo_pi();
		for (InvestigatorRefAndPosition coPI : allCoPI) {
			String coPIUser = coPI.getUserProfileId();
			proposalGridInfo.getCopiUsers().add(coPIUser);
			if (!proposalGridInfo.getAllUsers().contains(coPIUser)) {
				proposalGridInfo.getAllUsers().add(coPIUser);
			}
		}
	}

	/**
	 * @param userProposal
	 * @param proposalGridInfo
	 * @Refactored
	 */
	private void setProposalPIInfo(Proposal userProposal,
			ProposalInfo proposalGridInfo) {
		String piUserId = userProposal.getInvestigatorInfo().getPi()
				.getUserProfileId();
		proposalGridInfo.setPiUser(piUserId);
		if (!proposalGridInfo.getAllUsers().contains(piUserId)) {
			proposalGridInfo.getAllUsers().add(piUserId);
		}
	}

	/**
	 * @param userProposal
	 * @param proposalGridInfo
	 * @Refactored
	 */
	private void setLastProposalModificationAuditLog(Proposal userProposal,
			ProposalInfo proposalGridInfo) {
		Date lastAudited = null;
		String lastAuditedBy = new String();
		String lastAuditAction = new String();
		int auditLogCount = userProposal.getAuditLog().size();
		if (userProposal.getAuditLog() != null && auditLogCount != 0) {
			AuditLog auditLog = userProposal.getAuditLog().get(
					auditLogCount - 1);
			lastAudited = auditLog.getActivityDate();
			lastAuditedBy = auditLog.getUserProfile().getFullName();
			lastAuditAction = auditLog.getAction();
		}
		proposalGridInfo.setLastAudited(lastAudited);
		proposalGridInfo.setLastAuditedBy(lastAuditedBy);
		proposalGridInfo.setLastAuditAction(lastAuditAction);
	}

	/**
	 * @param userProposal
	 * @param proposalGridInfo
	 * @Refactored
	 */
	private void setCurrentProposalStatusAttribute(Proposal userProposal,
			ProposalInfo proposalGridInfo) {

		for (Status status : userProposal.getProposalStatus()) {
			proposalGridInfo.getProposalStatus().add(status.toString());
		}

		// PI
		proposalGridInfo.setSubmittedByPI(userProposal.getSubmittedByPI());
		proposalGridInfo.setReadyForSubmissionByPI(userProposal
				.isReadyForSubmissionByPI());
		proposalGridInfo.setDeletedByPI(userProposal.getDeletedByPI());

		// Chair
		proposalGridInfo.setChairApproval(userProposal.getChairApproval());

		// Business Manager
		proposalGridInfo.setBusinessManagerApproval(userProposal
				.getBusinessManagerApproval());
		// IRB
		proposalGridInfo.setIrbApproval(userProposal.getIrbApproval());
		// Dean
		proposalGridInfo.setDeanApproval(userProposal.getDeanApproval());
		// University Research Administrator
		proposalGridInfo.setResearchAdministratorApproval(userProposal
				.getResearchAdministratorApproval());
		proposalGridInfo.setResearchAdministratorWithdraw(userProposal
				.getResearchAdministratorWithdraw());
		proposalGridInfo.setResearchAdministratorSubmission(userProposal
				.getResearchAdministratorSubmission());
		// University Research Director
		proposalGridInfo.setResearchDirectorApproval(userProposal
				.getResearchDirectorApproval());
		proposalGridInfo.setResearchDirectorDeletion(userProposal
				.getResearchDirectorDeletion());
		proposalGridInfo.setResearchDirectorArchived(userProposal
				.getResearchDirectorArchived());
		proposalGridInfo.setIrbApprovalRequired(userProposal
				.isIrbApprovalRequired());
		if (userProposal.getDeletedByPI().equals(DeleteType.DELETED)
				|| userProposal.getResearchDirectorDeletion().equals(
						DeleteType.DELETED)) {
			proposalGridInfo.setDeleted(true);
		}
	}

	/**
	 * @param userProposal
	 * @param proposalGridInfo
	 * @Refactored
	 */
	private void setSponsorAndBudgetInfo(Proposal userProposal,
			ProposalInfo proposalGridInfo) {

		proposalGridInfo.setGrantingAgencies(userProposal
				.getSponsorAndBudgetInfo().getGrantingAgency());
		proposalGridInfo.setDirectCosts(userProposal.getSponsorAndBudgetInfo()
				.getDirectCosts());
		proposalGridInfo.setFaCosts(userProposal.getSponsorAndBudgetInfo()
				.getFaCosts());
		proposalGridInfo.setTotalCosts(userProposal.getSponsorAndBudgetInfo()
				.getTotalCosts());
		proposalGridInfo.setFaRate(userProposal.getSponsorAndBudgetInfo()
				.getFaRate());
		proposalGridInfo.setDateCreated(userProposal.getDateCreated());
		proposalGridInfo.setDateSubmitted(userProposal.getDateSubmitted());
		proposalGridInfo.setDueDate(userProposal.getProjectInfo().getDueDate());
		proposalGridInfo.setProjectPeriodFrom(userProposal.getProjectInfo()
				.getProjectPeriod().getFrom());
		proposalGridInfo.setProjectPeriodTo(userProposal.getProjectInfo()
				.getProjectPeriod().getTo());
	}

	/**
	 * @param userProposal
	 * @param proposalGridInfo
	 * @Refactored
	 */
	private void setProposalProjectInfo(Proposal userProposal,
			ProposalInfo proposalGridInfo) {

		proposalGridInfo.setProjectTitle(userProposal.getProjectInfo()
				.getProjectTitle());
		ProjectType projectPropsalType = userProposal.getProjectInfo()
				.getProjectType();
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

		TypeOfRequest typeOfRequest = userProposal.getProjectInfo()
				.getTypeOfRequest();

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

	/**
	 * 
	 * @param proposalInfo
	 * @return
	 * @throws ParseException
	 * @Refactored
	 */
	public List<ProposalInfo> findAllProposals(ProposalCommonInfo proposalInfo)
			throws ParseException {

		Query<Proposal> queryPropsal = getMyProposalSearchSelectedFields(proposalInfo);
		int rowTotal = queryPropsal.asList().size();
		List<Proposal> allProposals = queryPropsal.order(
				"-audit log.activity on").asList();
		return getMyProposalGridForTheCurrentUser(rowTotal, allProposals);
	}

	// ////////////////////////////////////////////////////////////////////////////////////

	/**
	 * 
	 * @param offset
	 * @param limit
	 * @param proposalInfo
	 * @param userInfo
	 * @return
	 * @throws ParseException
	 */
	public List<ProposalInfo> findUserProposalGrid(int offset, int limit,
			ProposalCommonInfo proposalInfo, GPMSCommonInfo userInfo)
			throws ParseException {
		Datastore ds = getDatastore();
		String projectTitle = proposalInfo.getProjectTitle();
		String usernameBy = proposalInfo.getUsernameBy();
		Double totalCostsFrom = proposalInfo.getTotalCostsFrom();
		Double totalCostsTo = proposalInfo.getTotalCostsTo();
		String submittedOnFrom = proposalInfo.getSubmittedOnFrom();
		String submittedOnTo = proposalInfo.getSubmittedOnTo();
		String proposalStatus = proposalInfo.getProposalStatus();
		String userRole = proposalInfo.getUserRole();
		String userId = userInfo.getUserProfileID();
		String college = userInfo.getUserCollege();
		String department = userInfo.getUserDepartment();
		String positionType = userInfo.getUserPositionType();
		String positionTitle = userInfo.getUserPositionTitle();
		List<ProposalInfo> proposals = new ArrayList<ProposalInfo>();
		Query<Proposal> proposalQuery = ds.createQuery(Proposal.class);
		Query<UserProfile> profileQuery = ds.createQuery(UserProfile.class);
		if (projectTitle != null) {
			proposalQuery.field("project info.project title")
					.containsIgnoreCase(projectTitle);
		}
		if (submittedOnFrom != null && !submittedOnFrom.isEmpty()) {
			Date receivedOnF = formatter.parse(submittedOnFrom);
			proposalQuery.field("date submitted").greaterThanOrEq(receivedOnF);
		}
		if (submittedOnTo != null && !submittedOnTo.isEmpty()) {
			Date receivedOnT = formatter.parse(submittedOnTo);
			proposalQuery.field("date submitted").lessThanOrEq(receivedOnT);
		}

		if (totalCostsFrom != null && totalCostsFrom != 0.0) {
			proposalQuery.field("sponsor and budget info.total costs")
					.greaterThanOrEq(totalCostsFrom);
		}
		if (totalCostsTo != null && totalCostsTo != 0.0) {
			proposalQuery.field("sponsor and budget info.total costs")
					.lessThanOrEq(totalCostsTo);
		}
		if (proposalStatus != null) {
			proposalQuery.field("proposal status").contains(proposalStatus);
		}
		if (positionTitle.equals("IRB")) {
			proposalQuery.criteria("irb approval required").equal(true);
		}
		if (!positionTitle.equals("University Research Administrator")
				&& !positionTitle.equals("University Research Director")
				&& !positionTitle.equals("IRB")) {
			if (positionTitle.equals("Dean")
					|| positionTitle.equals("Associate Dean")) {
				proposalQuery.or(
						proposalQuery.criteria("investigator info.pi.college")
								.equal(college),
						proposalQuery.criteria(
								"investigator info.co_pi.college").equal(
								college),
						proposalQuery.criteria(
								"investigator info.senior personnel.college")
								.equal(college));
			} else if (positionTitle.equals("Business Manager")
					|| positionTitle
							.equals("Department Administrative Assistant")
					|| positionTitle.equals("Department Chair")
					|| positionTitle.equals("Associate Chair")) {
				proposalQuery
						.and(proposalQuery
								.or(proposalQuery.criteria(
										"investigator info.pi.college").equal(
										college),
										proposalQuery
												.criteria(
														"investigator info.co_pi.college")
												.equal(college),
										proposalQuery
												.criteria(
														"investigator info.senior personnel.college")
												.equal(college)),
								proposalQuery
										.or(proposalQuery
												.criteria(
														"investigator info.pi.department")
												.equal(department),
												proposalQuery
														.criteria(
																"investigator info.co_pi.department")
														.equal(department),
												proposalQuery
														.criteria(
																"investigator info.senior personnel.department")
														.equal(department)));
			} else {
				proposalQuery
						.or(proposalQuery.and(
								proposalQuery.criteria(
										"investigator info.pi.user profile id")
										.equal(userId),
								proposalQuery.criteria(
										"investigator info.pi.college").equal(
										college),
								proposalQuery.criteria(
										"investigator info.pi.department")
										.equal(department),
								proposalQuery.criteria(
										"investigator info.pi.position type")
										.equal(positionType),
								proposalQuery.criteria(
										"investigator info.pi.position title")
										.equal(positionTitle)),
								proposalQuery
										.and(proposalQuery
												.criteria(
														"investigator info.co_pi.user profile id")
												.equal(userId),
												proposalQuery
														.criteria(
																"investigator info.co_pi.college")
														.equal(college),
												proposalQuery
														.criteria(
																"investigator info.co_pi.department")
														.equal(department),
												proposalQuery
														.criteria(
																"investigator info.co_pi.position type")
														.equal(positionType),
												proposalQuery
														.criteria(
																"investigator info.co_pi.position title")
														.equal(positionTitle)),
								proposalQuery
										.and(proposalQuery
												.criteria(
														"investigator info.senior personnel.user profile id")
												.equal(userId),
												proposalQuery
														.criteria(
																"investigator info.senior personnel.college")
														.equal(college),
												proposalQuery
														.criteria(
																"investigator info.senior personnel.department")
														.equal(department),
												proposalQuery
														.criteria(
																"investigator info.senior personnel.position type")
														.equal(positionType),
												proposalQuery
														.criteria(
																"investigator info.senior personnel.position title")
														.equal(positionTitle)));
			}
		}
		if (usernameBy != null) {
			profileQuery.or(
					profileQuery.criteria("first name").containsIgnoreCase(
							usernameBy),
					profileQuery.criteria("middle name").containsIgnoreCase(
							usernameBy), profileQuery.criteria("last name")
							.containsIgnoreCase(usernameBy));
			if (userRole != null) {
				switch (userRole) {
				case "PI":
					proposalQuery.criteria("investigator info.pi.user profile")
							.in(profileQuery.asKeyList());
					break;
				case "Co-PI":
					proposalQuery.criteria(
							"investigator info.co_pi.user profile").in(
							profileQuery.asKeyList());
					break;
				case "Senior Personnel":
					proposalQuery.criteria(
							"investigator info.senior personnel.user profile")
							.in(profileQuery.asKeyList());
					break;
				default:
					break;
				}
			} else {
				proposalQuery
						.or(proposalQuery.criteria(
								"investigator info.pi.user profile").in(
								profileQuery.asKeyList()),
								proposalQuery.criteria(
										"investigator info.co_pi.user profile")
										.in(profileQuery.asKeyList()),
								proposalQuery
										.criteria(
												"investigator info.senior personnel.user profile")
										.in(profileQuery.asKeyList()));
			}
		} else if (usernameBy == null && userRole != null) {
			switch (userRole) {
			case "PI":
				proposalQuery.criteria("investigator info.pi.user profile id")
						.equal(userId);
				break;
			case "Co-PI":
				proposalQuery.criteria(
						"investigator info.co_pi.user profile id")
						.equal(userId);
				break;
			case "Senior Personnel":
				proposalQuery.criteria(
						"investigator info.senior personnel.user profile id")
						.equal(userId);
				break;
			default:
				break;
			}
		}
		int rowTotal = proposalQuery.asList().size();
		List<Proposal> allProposals = proposalQuery.offset(offset - 1)
				.limit(limit).order("-audit log.activity on").asList();
		for (Proposal userProposal : allProposals) {
			ProposalInfo proposal = new ProposalInfo();
			// Proposal
			proposal.setRowTotal(rowTotal);
			proposal.setId(userProposal.getId().toString());
			setProposalProjectInfo(userProposal, proposal);
			setSponsorAndBudgetInfo(userProposal, proposal);
			setCurrentProposalStatusAttribute(userProposal, proposal);
			setLastProposalModificationAuditLog(userProposal, proposal);
			String piUserId = userProposal.getInvestigatorInfo().getPi()
					.getUserProfileId();
			String piUserCollege = userProposal.getInvestigatorInfo().getPi()
					.getCollege();
			String piUserDepartment = userProposal.getInvestigatorInfo()
					.getPi().getDepartment();
			String piUserPositionType = userProposal.getInvestigatorInfo()
					.getPi().getPositionType();
			String piUserPositionTitle = userProposal.getInvestigatorInfo()
					.getPi().getPositionTitle();
			proposal.setPiUser(piUserId);
			if (!proposal.getAllUsers().contains(piUserId)) {
				proposal.getAllUsers().add(piUserId);
			}
			if (piUserId.equals(userId) && piUserCollege.equals(college)
					&& piUserDepartment.equals(department)
					&& piUserPositionType.equals(positionType)
					&& piUserPositionTitle.equals(positionTitle)) {
				proposal.getCurrentuserProposalRoles().add("PI");
			}
			List<InvestigatorRefAndPosition> allCoPI = userProposal
					.getInvestigatorInfo().getCo_pi();
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
				if (coPIUser.equals(userId) && coPIUserCollege.equals(college)
						&& coPIUserDepartment.equals(department)
						&& coPIUserPositionType.equals(positionType)
						&& coPIUserPositionTitle.equals(positionTitle)) {
					proposal.getCurrentuserProposalRoles().add("Co-PI");
				}
			}
			List<InvestigatorRefAndPosition> allSeniors = userProposal
					.getInvestigatorInfo().getSeniorPersonnel();
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
				if (seniorUser.equals(userId)
						&& seniorUserCollege.equals(college)
						&& seniorUserDepartment.equals(department)
						&& seniorUserPositionType.equals(positionType)
						&& seniorUserPositionTitle.equals(positionTitle)) {
					proposal.getCurrentuserProposalRoles().add(
							"Senior Personnel");
				}
			}
			proposals.add(proposal);
		}
		return proposals;
	}

	/**
	 * 
	 * @param proposalInfo
	 * @param userInfo
	 * @return
	 * @throws ParseException
	 */
	public List<ProposalInfo> findAllUserProposals(
			ProposalCommonInfo proposalInfo, GPMSCommonInfo userInfo)
			throws ParseException {

		Datastore ds = getDatastore();
		String projectTitle = proposalInfo.getProjectTitle();
		String usernameBy = proposalInfo.getUsernameBy();
		Double totalCostsFrom = proposalInfo.getTotalCostsFrom();
		Double totalCostsTo = proposalInfo.getTotalCostsTo();
		String submittedOnFrom = proposalInfo.getSubmittedOnFrom();
		String submittedOnTo = proposalInfo.getSubmittedOnTo();
		String proposalStatus = proposalInfo.getProposalStatus();
		String userRole = proposalInfo.getUserRole();
		String userId = userInfo.getUserProfileID();
		String college = userInfo.getUserCollege();
		String department = userInfo.getUserDepartment();
		String positionType = userInfo.getUserPositionType();
		String positionTitle = userInfo.getUserPositionTitle();
		List<ProposalInfo> proposals = new ArrayList<ProposalInfo>();
		Query<Proposal> proposalQuery = ds.createQuery(Proposal.class);
		Query<UserProfile> profileQuery = ds.createQuery(UserProfile.class);
		if (projectTitle != null) {
			proposalQuery.field("project info.project title")
					.containsIgnoreCase(projectTitle);
		}
		if (submittedOnFrom != null && !submittedOnFrom.isEmpty()) {
			Date receivedOnF = formatter.parse(submittedOnFrom);
			proposalQuery.field("date submitted").greaterThanOrEq(receivedOnF);
		}
		if (submittedOnTo != null && !submittedOnTo.isEmpty()) {
			Date receivedOnT = formatter.parse(submittedOnTo);
			proposalQuery.field("date submitted").lessThanOrEq(receivedOnT);
		}
		if (totalCostsFrom != null && totalCostsFrom != 0.0) {
			proposalQuery.field("sponsor and budget info.total costs")
					.greaterThanOrEq(totalCostsFrom);
		}
		if (totalCostsTo != null && totalCostsTo != 0.0) {
			proposalQuery.field("sponsor and budget info.total costs")
					.lessThanOrEq(totalCostsTo);
		}
		if (proposalStatus != null) {
			proposalQuery.field("proposal status").contains(proposalStatus);
		}
		if (!positionTitle.equals("IRB")
				&& !positionTitle.equals("University Research Administrator")
				&& !positionTitle.equals("University Research Director")) {
			if (positionTitle.equals("Dean")
					|| positionTitle.equals("Associate Dean")) {
				proposalQuery.or(
						proposalQuery.criteria("investigator info.pi.college")
								.equal(college),
						proposalQuery.criteria(
								"investigator info.co_pi.college").equal(
								college),
						proposalQuery.criteria(
								"investigator info.senior personnel.college")
								.equal(college));
			} else if (positionTitle.equals("Business Manager")
					|| positionTitle
							.equals("Department Administrative Assistant")
					|| positionTitle.equals("Department Chair")
					|| positionTitle.equals("Associate Chair")) {
				proposalQuery
						.and(proposalQuery
								.or(proposalQuery.criteria(
										"investigator info.pi.college").equal(
										college),
										proposalQuery
												.criteria(
														"investigator info.co_pi.college")
												.equal(college),
										proposalQuery
												.criteria(
														"investigator info.senior personnel.college")
												.equal(college)),
								proposalQuery
										.or(proposalQuery
												.criteria(
														"investigator info.pi.department")
												.equal(department),
												proposalQuery
														.criteria(
																"investigator info.co_pi.department")
														.equal(department),
												proposalQuery
														.criteria(
																"investigator info.senior personnel.department")
														.equal(department)));
			} else {
				proposalQuery
						.or(proposalQuery.and(
								proposalQuery.criteria(
										"investigator info.pi.user profile id")
										.equal(userId),
								proposalQuery.criteria(
										"investigator info.pi.college").equal(
										college),
								proposalQuery.criteria(
										"investigator info.pi.department")
										.equal(department),
								proposalQuery.criteria(
										"investigator info.pi.position type")
										.equal(positionType),
								proposalQuery.criteria(
										"investigator info.pi.position title")
										.equal(positionTitle)),
								proposalQuery
										.and(proposalQuery
												.criteria(
														"investigator info.co_pi.user profile id")
												.equal(userId),
												proposalQuery
														.criteria(
																"investigator info.co_pi.college")
														.equal(college),
												proposalQuery
														.criteria(
																"investigator info.co_pi.department")
														.equal(department),
												proposalQuery
														.criteria(
																"investigator info.co_pi.position type")
														.equal(positionType),
												proposalQuery
														.criteria(
																"investigator info.co_pi.position title")
														.equal(positionTitle)),
								proposalQuery
										.and(proposalQuery
												.criteria(
														"investigator info.senior personnel.user profile id")
												.equal(userId),
												proposalQuery
														.criteria(
																"investigator info.senior personnel.college")
														.equal(college),
												proposalQuery
														.criteria(
																"investigator info.senior personnel.department")
														.equal(department),
												proposalQuery
														.criteria(
																"investigator info.senior personnel.position type")
														.equal(positionType),
												proposalQuery
														.criteria(
																"investigator info.senior personnel.position title")
														.equal(positionTitle)));
			}
		}
		if (usernameBy != null) {
			profileQuery.or(
					profileQuery.criteria("first name").containsIgnoreCase(
							usernameBy),
					profileQuery.criteria("middle name").containsIgnoreCase(
							usernameBy), profileQuery.criteria("last name")
							.containsIgnoreCase(usernameBy));
			if (userRole != null) {
				switch (userRole) {
				case "PI":
					proposalQuery.criteria("investigator info.pi.user profile")
							.in(profileQuery.asKeyList());
					break;
				case "Co-PI":
					proposalQuery.criteria(
							"investigator info.co_pi.user profile").in(
							profileQuery.asKeyList());
					break;
				case "Senior Personnel":
					proposalQuery.criteria(
							"investigator info.senior personnel.user profile")
							.in(profileQuery.asKeyList());
					break;
				default:
					break;
				}
			} else {
				proposalQuery
						.or(proposalQuery.criteria(
								"investigator info.pi.user profile").in(
								profileQuery.asKeyList()),
								proposalQuery.criteria(
										"investigator info.co_pi.user profile")
										.in(profileQuery.asKeyList()),
								proposalQuery
										.criteria(
												"investigator info.senior personnel.user profile")
										.in(profileQuery.asKeyList()));
			}
		} else if (usernameBy == null && userRole != null) {
			switch (userRole) {
			case "PI":
				proposalQuery.criteria("investigator info.pi.user profile id")
						.equal(userId);
				break;
			case "Co-PI":
				proposalQuery.criteria(
						"investigator info.co_pi.user profile id")
						.equal(userId);
				break;

			case "Senior Personnel":
				proposalQuery.criteria(
						"investigator info.senior personnel.user profile id")
						.equal(userId);
				break;

			default:
				break;
			}
		}
		int rowTotal = proposalQuery.asList().size();
		List<Proposal> allProposals = proposalQuery.order(
				"-audit log.activity on").asList();

		for (Proposal userProposal : allProposals) {
			ProposalInfo proposal = new ProposalInfo();
			// Proposal
			proposal.setRowTotal(rowTotal);
			proposal.setId(userProposal.getId().toString());
			setProposalProjectInfo(userProposal, proposal);
			setSponsorAndBudgetInfo(userProposal, proposal);
			setCurrentProposalStatusAttribute(userProposal, proposal);
			setLastProposalModificationAuditLog(userProposal, proposal);
			String piUserId = userProposal.getInvestigatorInfo().getPi()
					.getUserProfileId().toString();
			proposal.setPiUser(piUserId);
			if (!proposal.getAllUsers().contains(piUserId)) {
				proposal.getAllUsers().add(piUserId);
			}
			if (piUserId.equals(userId)) {
				proposal.getCurrentuserProposalRoles().add("PI");
			}
			List<InvestigatorRefAndPosition> allCoPI = userProposal
					.getInvestigatorInfo().getCo_pi();
			for (InvestigatorRefAndPosition coPI : allCoPI) {
				String coPIUser = coPI.getUserProfileId().toString();
				proposal.getCopiUsers().add(coPIUser);
				if (!proposal.getAllUsers().contains(coPIUser)) {
					proposal.getAllUsers().add(coPIUser);
				}
				if (coPIUser.equals(userId)) {
					proposal.getCurrentuserProposalRoles().add("Co-PI");
				}
			}
			List<InvestigatorRefAndPosition> allSeniors = userProposal
					.getInvestigatorInfo().getSeniorPersonnel();
			for (InvestigatorRefAndPosition senior : allSeniors) {
				String seniorUser = senior.getUserProfileId().toString();
				proposal.getSeniorUsers().add(seniorUser);
				if (!proposal.getAllUsers().contains(seniorUser)) {
					proposal.getAllUsers().add(seniorUser);
				}
				if (seniorUser.equals(userId)) {
					proposal.getCurrentuserProposalRoles().add(
							"Senior Personnel");
				}
			}
			proposals.add(proposal);
		}
		return proposals;
	}

	public List<SignatureInfo> findAllSignatureForAProposal(ObjectId id,
			boolean irbApprovalRequired) throws ParseException {
		Datastore ds = getDatastore();
		List<SignatureInfo> signatures = new ArrayList<SignatureInfo>();
		List<CollegeDepartmentInfo> investigators = new ArrayList<CollegeDepartmentInfo>();
		Query<Proposal> q1 = ds
				.createQuery(Proposal.class)
				.field("_id")
				.equal(id)
				.retrievedFields(true, "_id", "investigator info",
						"signature info", "chair approval",
						"business manager approval", "dean approval");
		Proposal proposal = q1.get();
		// Adding PI
		SignatureInfo piSign = new SignatureInfo();
		InvestigatorRefAndPosition PI = proposal.getInvestigatorInfo().getPi();
		boolean piAlreadySigned = false;
		final List<SignatureInfo> proposalSignatures = proposal
				.getSignatureInfo();
		for (SignatureInfo signature : proposalSignatures) {
			if (PI.getUserProfileId().equals(signature.getUserProfileId())
					&& !PI.getUserRef().isDeleted()
					&& signature.getPositionTitle().equals("PI")) {
				piSign.setUserProfileId(signature.getUserProfileId());
				piSign.setFullName(signature.getFullName());
				piSign.setSignature(signature.getSignature());
				piSign.setSignedDate(signature.getSignedDate());
				piSign.setNote(signature.getNote());
				piSign.setPositionTitle(signature.getPositionTitle());
				piSign.setDelegated(signature.isDelegated());
				boolean piAlreadyExist = false;
				for (SignatureInfo sign : signatures) {
					if (sign.getUserProfileId().equalsIgnoreCase(
							piSign.getUserProfileId())) {
						piAlreadyExist = true;
						break;
					}
				}
				if (!piAlreadyExist) {
					signatures.add(piSign);
				}
				piAlreadySigned = true;
			}
		}
		if (!piAlreadySigned && !PI.getUserRef().isDeleted()) {
			piSign.setUserProfileId(PI.getUserProfileId().toString());
			piSign.setFullName(PI.getUserRef().getFullName());
			piSign.setSignature("");
			piSign.setNote("");
			piSign.setPositionTitle("PI");
			piSign.setDelegated(false);
			boolean piAlreadyExist = false;
			for (SignatureInfo sign : signatures) {
				if (sign.getUserProfileId().equalsIgnoreCase(
						piSign.getUserProfileId())) {
					piAlreadyExist = true;
					break;
				}
			}
			if (!piAlreadyExist) {
				signatures.add(piSign);
			}
		}
		CollegeDepartmentInfo investRef = new CollegeDepartmentInfo();
		if (!PI.getUserRef().isDeleted()) {
			investRef.setCollege(PI.getCollege());
			investRef.setDepartment(PI.getDepartment());
			investigators.add(investRef);
		}
		for (InvestigatorRefAndPosition coPIs : proposal.getInvestigatorInfo()
				.getCo_pi()) {
			// Adding Co-PIs
			SignatureInfo coPISign = new SignatureInfo();
			boolean coPIAlreadySigned = false;
			for (SignatureInfo signature : proposalSignatures) {
				if (coPIs.getUserProfileId().toString()
						.equals(signature.getUserProfileId())
						&& !coPIs.getUserRef().isDeleted()
						&& signature.getPositionTitle().equals("Co-PI")) {
					coPISign.setUserProfileId(signature.getUserProfileId());
					coPISign.setFullName(signature.getFullName());
					coPISign.setSignature(signature.getSignature());
					coPISign.setSignedDate(signature.getSignedDate());
					coPISign.setNote(signature.getNote());
					coPISign.setPositionTitle(signature.getPositionTitle());
					coPISign.setDelegated(signature.isDelegated());
					boolean coPIAlreadyExist = false;
					for (SignatureInfo sign : signatures) {
						if (sign.getUserProfileId().equalsIgnoreCase(
								coPISign.getUserProfileId())) {
							coPIAlreadyExist = true;
							break;
						}
					}
					if (!coPIAlreadyExist) {
						signatures.add(coPISign);
					}
					coPIAlreadySigned = true;
				}
			}
			if (!coPIAlreadySigned && !coPIs.getUserRef().isDeleted()) {
				coPISign.setUserProfileId(coPIs.getUserProfileId().toString());
				coPISign.setFullName(coPIs.getUserRef().getFullName());
				coPISign.setSignature("");
				coPISign.setNote("");
				coPISign.setPositionTitle("Co-PI");
				coPISign.setDelegated(false);
				boolean coPIAlreadyExist = false;
				for (SignatureInfo sign : signatures) {
					if (sign.getUserProfileId().equalsIgnoreCase(
							coPISign.getUserProfileId())) {
						coPIAlreadyExist = true;
						break;
					}
				}
				if (!coPIAlreadyExist) {
					signatures.add(coPISign);
				}
			}
			if (!coPIs.getUserRef().isDeleted()) {
				investRef = new CollegeDepartmentInfo();
				investRef.setCollege(coPIs.getCollege());
				investRef.setDepartment(coPIs.getDepartment());
				if (!investigators.contains(investRef)) {
					investigators.add(investRef);
				}
			}
		}
		List<String> positions = new ArrayList<String>();
		positions.add("Department Chair");
		// positions.add("Associate Chair");
		positions.add("Business Manager");
		// positions.add("Department Administrative Assistant");
		positions.add("Dean");
		// positions.add("Associate Dean");
		positions.add("University Research Administrator");
		positions.add("University Research Director");
		if (irbApprovalRequired) {
			positions.add("IRB");
		}
		Query<UserProfile> profileQuery = ds.createQuery(UserProfile.class)
				.retrievedFields(true, "_id", "first name", "middle name",
						"last name", "details");
		profileQuery.and(profileQuery.criteria("deleted").equal(false),
				profileQuery.criteria("details.position title").in(positions));
		List<UserProfile> userProfile = profileQuery.asList();
		for (UserProfile user : userProfile) {
			for (PositionDetails posDetails : user.getDetails()) {
				for (CollegeDepartmentInfo colDeptInfo : investigators) {
					if (posDetails.getCollege().equalsIgnoreCase(
							colDeptInfo.getCollege())
							&& posDetails.getDepartment().equalsIgnoreCase(
									colDeptInfo.getDepartment())
							&& posDetails.getPositionTitle().equalsIgnoreCase(
									"Department Chair")) {
						SignatureInfo signDeptChair = new SignatureInfo();
						boolean departmentChairAlreadySigned = false;
						for (SignatureInfo signature : proposalSignatures) {
							if (user.getId().toString()
									.equals(signature.getUserProfileId())
									&& signature.getPositionTitle().equals(
											"Department Chair")) {
								signDeptChair.setUserProfileId(signature
										.getUserProfileId());
								signDeptChair.setFullName(signature
										.getFullName());
								signDeptChair.setSignature(signature
										.getSignature());
								signDeptChair.setSignedDate(signature
										.getSignedDate());
								signDeptChair.setNote(signature.getNote());
								signDeptChair.setPositionTitle(signature
										.getPositionTitle());
								signDeptChair.setDelegated(signature
										.isDelegated());
								if (!signatures.contains(signDeptChair)) {
									signatures.add(signDeptChair);
								}
								departmentChairAlreadySigned = true;
							} else if (signature.getPositionTitle().equals(
									"Department Chair")
									&& proposal.getChairApproval() == ApprovalType.APPROVED) {
								signDeptChair.setUserProfileId(signature
										.getUserProfileId());
								signDeptChair.setFullName(signature
										.getFullName());
								signDeptChair.setSignature(signature
										.getSignature());
								signDeptChair.setSignedDate(signature
										.getSignedDate());
								signDeptChair.setNote(signature.getNote());
								signDeptChair.setPositionTitle(signature
										.getPositionTitle());
								signDeptChair.setDelegated(signature
										.isDelegated());
								if (!signatures.contains(signDeptChair)) {
									signatures.add(signDeptChair);
								}
								departmentChairAlreadySigned = true;
							}
						}
						if (!departmentChairAlreadySigned) {
							if (!isDelegator(user.getId().toString(),
									posDetails)
									&& proposal.getChairApproval() == ApprovalType.READYFORAPPROVAL) {
								signDeptChair.setUserProfileId(user.getId()
										.toString());
								signDeptChair.setFullName(user.getFullName());
								signDeptChair.setSignature("");
								signDeptChair.setNote("");
								signDeptChair
										.setPositionTitle("Department Chair");
								signDeptChair.setDelegated(false);
								if (!signatures.contains(signDeptChair)) {
									signatures.add(signDeptChair);
								}
							} else if (isDelegator(user.getId().toString(),
									posDetails)) {
								// here we used Transfer mode of Delegation
								List<SignatureInfo> delegatedChair = findDelegatedUsersForAUser(
										user.getId(), id.toString(),
										posDetails.getCollege(),
										posDetails.getDepartment(),
										posDetails.getPositionType(),
										"Department Chair");
								boolean delegatedDepartmentChairAlreadySigned = false;
								for (SignatureInfo delegateeInfo : delegatedChair) {
									delegatedDepartmentChairAlreadySigned = false;
									for (SignatureInfo signature : proposalSignatures) {
										if (delegateeInfo.getUserProfileId()
												.equals(signature
														.getUserProfileId())
												&& signature
														.getPositionTitle()
														.equals("Department Chair")) {
											delegateeInfo
													.setUserProfileId(signature
															.getUserProfileId());
											delegateeInfo.setFullName(signature
													.getFullName());
											delegateeInfo
													.setSignature(signature
															.getSignature());
											delegateeInfo
													.setSignedDate(signature
															.getSignedDate());
											delegateeInfo.setNote(signature
													.getNote());
											delegateeInfo
													.setPositionTitle(signature
															.getPositionTitle());
											delegateeInfo
													.setDelegated(signature
															.isDelegated());
											if (!signatures
													.contains(delegateeInfo)) {
												signatures.add(delegateeInfo);
											}
											delegatedDepartmentChairAlreadySigned = true;
										}
									}
									if (!delegatedDepartmentChairAlreadySigned) {
										if (!signatures.contains(delegateeInfo)) {
											signatures.add(delegateeInfo);
										}
									}
								}
							} else {
								signDeptChair.setUserProfileId(user.getId()
										.toString());
								signDeptChair.setFullName(user.getFullName());
								signDeptChair.setSignature("");
								signDeptChair.setNote("");
								signDeptChair
										.setPositionTitle("Department Chair");
								signDeptChair.setDelegated(false);
								if (!signatures.contains(signDeptChair)) {
									signatures.add(signDeptChair);
								}
							}
						}
					} else if (posDetails.getCollege().equalsIgnoreCase(
							colDeptInfo.getCollege())
							&& posDetails.getDepartment().equalsIgnoreCase(
									colDeptInfo.getDepartment())
							&& posDetails.getPositionTitle().equalsIgnoreCase(
									"Business Manager")) {
						SignatureInfo signBusinessMgr = new SignatureInfo();
						boolean businessManagerAlreadySigned = false;
						for (SignatureInfo signature : proposalSignatures) {
							if (user.getId().toString()
									.equals(signature.getUserProfileId())
									&& signature.getPositionTitle().equals(
											"Business Manager")) {
								signBusinessMgr.setUserProfileId(signature
										.getUserProfileId());
								signBusinessMgr.setFullName(signature
										.getFullName());
								signBusinessMgr.setSignature(signature
										.getSignature());
								signBusinessMgr.setSignedDate(signature
										.getSignedDate());
								signBusinessMgr.setNote(signature.getNote());
								signBusinessMgr.setPositionTitle(signature
										.getPositionTitle());
								signBusinessMgr.setDelegated(signature
										.isDelegated());
								if (!signatures.contains(signBusinessMgr)) {
									signatures.add(signBusinessMgr);
								}
								businessManagerAlreadySigned = true;
							} else if (signature.getPositionTitle().equals(
									"Business Manager")
									&& proposal.getBusinessManagerApproval() == ApprovalType.APPROVED) {
								signBusinessMgr.setUserProfileId(signature
										.getUserProfileId());
								signBusinessMgr.setFullName(signature
										.getFullName());
								signBusinessMgr.setSignature(signature
										.getSignature());
								signBusinessMgr.setSignedDate(signature
										.getSignedDate());
								signBusinessMgr.setNote(signature.getNote());
								signBusinessMgr.setPositionTitle(signature
										.getPositionTitle());
								signBusinessMgr.setDelegated(signature
										.isDelegated());
								if (!signatures.contains(signBusinessMgr)) {
									signatures.add(signBusinessMgr);
								}
								businessManagerAlreadySigned = true;
							}
						}
						if (!businessManagerAlreadySigned) {
							if (!isDelegator(user.getId().toString(),
									posDetails)
									&& proposal.getBusinessManagerApproval() == ApprovalType.READYFORAPPROVAL) {
								signBusinessMgr.setUserProfileId(user.getId()
										.toString());
								signBusinessMgr.setFullName(user.getFullName());
								signBusinessMgr.setSignature("");
								signBusinessMgr.setNote("");
								signBusinessMgr
										.setPositionTitle("Business Manager");
								signBusinessMgr.setDelegated(false);
								if (!signatures.contains(signBusinessMgr)) {
									signatures.add(signBusinessMgr);
								}
							} else if (isDelegator(user.getId().toString(),
									posDetails)) {
								List<SignatureInfo> delegatedBusinessManager = findDelegatedUsersForAUser(
										user.getId(), id.toString(),
										posDetails.getCollege(),
										posDetails.getDepartment(),
										posDetails.getPositionType(),
										"Business Manager");
								boolean delegatedBusinessManagerAlreadySigned = false;
								for (SignatureInfo delegateeInfo : delegatedBusinessManager) {
									for (SignatureInfo signature : proposalSignatures) {
										if (delegateeInfo.getUserProfileId()
												.equals(signature
														.getUserProfileId())
												&& signature
														.getPositionTitle()
														.equals("Business Manager")) {
											delegateeInfo
													.setUserProfileId(signature
															.getUserProfileId());
											delegateeInfo.setFullName(signature
													.getFullName());
											delegateeInfo
													.setSignature(signature
															.getSignature());
											delegateeInfo
													.setSignedDate(signature
															.getSignedDate());
											delegateeInfo.setNote(signature
													.getNote());
											delegateeInfo
													.setPositionTitle(signature
															.getPositionTitle());
											delegateeInfo
													.setDelegated(signature
															.isDelegated());
											if (!signatures
													.contains(delegateeInfo)) {
												signatures.add(delegateeInfo);
											}
											delegatedBusinessManagerAlreadySigned = true;
										}
									}
									if (!delegatedBusinessManagerAlreadySigned) {
										if (!signatures.contains(delegateeInfo)) {
											signatures.add(delegateeInfo);
										}
									}
								}
							} else {
								signBusinessMgr.setUserProfileId(user.getId()
										.toString());
								signBusinessMgr.setFullName(user.getFullName());
								signBusinessMgr.setSignature("");
								signBusinessMgr.setNote("");
								signBusinessMgr
										.setPositionTitle("Business Manager");
								signBusinessMgr.setDelegated(false);
								if (!signatures.contains(signBusinessMgr)) {
									signatures.add(signBusinessMgr);
								}
							}
						}
					} else if (posDetails.getCollege().equalsIgnoreCase(
							colDeptInfo.getCollege())
							&& posDetails.getDepartment().equalsIgnoreCase(
									colDeptInfo.getDepartment())
							&& posDetails.getPositionTitle().equalsIgnoreCase(
									"Dean")) {
						SignatureInfo signDean = new SignatureInfo();

						boolean deanAlreadySigned = false;
						for (SignatureInfo signature : proposalSignatures) {
							if (user.getId().toString()
									.equals(signature.getUserProfileId())
									&& signature.getPositionTitle().equals(
											"Dean")) {
								signDean.setUserProfileId(signature
										.getUserProfileId());
								signDean.setFullName(signature.getFullName());
								signDean.setSignature(signature.getSignature());
								signDean.setSignedDate(signature
										.getSignedDate());
								signDean.setNote(signature.getNote());
								signDean.setPositionTitle(signature
										.getPositionTitle());
								signDean.setDelegated(signature.isDelegated());
								if (!signatures.contains(signDean)) {
									signatures.add(signDean);
								}
								deanAlreadySigned = true;
							} else if (signature.getPositionTitle().equals(
									"Dean")
									&& proposal.getDeanApproval() == ApprovalType.APPROVED) {
								signDean.setUserProfileId(signature
										.getUserProfileId());
								signDean.setFullName(signature.getFullName());
								signDean.setSignature(signature.getSignature());
								signDean.setSignedDate(signature
										.getSignedDate());
								signDean.setNote(signature.getNote());
								signDean.setPositionTitle(signature
										.getPositionTitle());
								signDean.setDelegated(signature.isDelegated());
								if (!signatures.contains(signDean)) {
									signatures.add(signDean);
								}
								deanAlreadySigned = true;
							}
						}
						if (!deanAlreadySigned) {
							if (!isDelegator(user.getId().toString(),
									posDetails)
									&& proposal.getDeanApproval() == ApprovalType.READYFORAPPROVAL) {
								signDean.setUserProfileId(user.getId()
										.toString());
								signDean.setFullName(user.getFullName());
								signDean.setSignature("");
								signDean.setNote("");
								signDean.setPositionTitle("Dean");
								signDean.setDelegated(false);
								if (!signatures.contains(signDean)) {
									signatures.add(signDean);
								}
							} else if (isDelegator(user.getId().toString(),
									posDetails)) {
								List<SignatureInfo> delegatedDean = findDelegatedUsersForAUser(
										user.getId(), id.toString(),
										posDetails.getCollege(),
										posDetails.getDepartment(),
										posDetails.getPositionType(), "Dean");

								boolean delegatedDeanAlreadySigned = false;
								for (SignatureInfo delegateeInfo : delegatedDean) {
									for (SignatureInfo signature : proposalSignatures) {
										if (delegateeInfo.getUserProfileId()
												.equals(signature
														.getUserProfileId())
												&& signature.getPositionTitle()
														.equals("Dean")) {
											delegateeInfo
													.setUserProfileId(signature
															.getUserProfileId());
											delegateeInfo.setFullName(signature
													.getFullName());
											delegateeInfo
													.setSignature(signature
															.getSignature());
											delegateeInfo
													.setSignedDate(signature
															.getSignedDate());
											delegateeInfo.setNote(signature
													.getNote());
											delegateeInfo
													.setPositionTitle(signature
															.getPositionTitle());
											delegateeInfo
													.setDelegated(signature
															.isDelegated());
											if (!signatures
													.contains(delegateeInfo)) {
												signatures.add(delegateeInfo);
											}
											delegatedDeanAlreadySigned = true;
										}
									}
									if (!delegatedDeanAlreadySigned) {
										if (!signatures.contains(delegateeInfo)) {
											signatures.add(delegateeInfo);
										}
									}
								}
							} else {
								signDean.setUserProfileId(user.getId()
										.toString());
								signDean.setFullName(user.getFullName());
								signDean.setSignature("");
								signDean.setNote("");
								signDean.setPositionTitle("Dean");
								signDean.setDelegated(false);
								if (!signatures.contains(signDean)) {
									signatures.add(signDean);
								}
							}
						}
					}
				}
				if (posDetails.getPositionTitle().equalsIgnoreCase("IRB")
						&& irbApprovalRequired) {
					SignatureInfo signBusinessMgr = new SignatureInfo();

					boolean irbAlreadySigned = false;
					for (SignatureInfo signature : proposalSignatures) {
						if (user.getId().toString()
								.equals(signature.getUserProfileId())
								&& signature.getPositionTitle().equals("IRB")) {
							signBusinessMgr.setUserProfileId(signature
									.getUserProfileId());
							signBusinessMgr
									.setFullName(signature.getFullName());
							signBusinessMgr.setSignature(signature
									.getSignature());
							signBusinessMgr.setSignedDate(signature
									.getSignedDate());
							signBusinessMgr.setNote(signature.getNote());
							signBusinessMgr.setPositionTitle(signature
									.getPositionTitle());
							signBusinessMgr.setDelegated(signature
									.isDelegated());
							if (!signatures.contains(signBusinessMgr)) {
								signatures.add(signBusinessMgr);
							}
							irbAlreadySigned = true;
						}
					}
					if (!irbAlreadySigned) {
						signBusinessMgr.setUserProfileId(user.getId()
								.toString());
						signBusinessMgr.setFullName(user.getFullName());
						signBusinessMgr.setSignature("");
						signBusinessMgr.setNote("");
						signBusinessMgr.setPositionTitle("IRB");
						signBusinessMgr.setDelegated(false);
						if (!signatures.contains(signBusinessMgr)) {
							signatures.add(signBusinessMgr);
						}
					}
				} else if (posDetails.getPositionTitle().equalsIgnoreCase(
						"University Research Administrator")) {
					SignatureInfo signAdmin = new SignatureInfo();

					boolean adminAlreadySigned = false;
					for (SignatureInfo signature : proposalSignatures) {
						if (user.getId().toString()
								.equals(signature.getUserProfileId())
								&& signature.getPositionTitle().equals(
										"University Research Administrator")) {
							signAdmin.setUserProfileId(signature
									.getUserProfileId());
							signAdmin.setFullName(user.getFullName());
							signAdmin.setSignature(signature.getSignature());
							signAdmin.setSignedDate(signature.getSignedDate());
							signAdmin.setNote(signature.getNote());
							signAdmin
									.setPositionTitle("University Research Administrator");
							signAdmin.setDelegated(signature.isDelegated());
							if (!signatures.contains(signAdmin)) {
								signatures.add(signAdmin);
							}
							adminAlreadySigned = true;
						}
					}
					if (!adminAlreadySigned) {
						signAdmin.setUserProfileId(user.getId().toString());
						signAdmin.setFullName(user.getFullName());
						signAdmin.setSignature("");
						signAdmin.setNote("");
						signAdmin
								.setPositionTitle("University Research Administrator");
						signAdmin.setDelegated(false);
						if (!signatures.contains(signAdmin)) {
							signatures.add(signAdmin);
						}
					}
				} else if (posDetails.getPositionTitle().equalsIgnoreCase(
						"University Research Director")) {
					SignatureInfo signDirector = new SignatureInfo();

					boolean directorAlreadySigned = false;
					for (SignatureInfo signature : proposalSignatures) {
						if (user.getId().toString()
								.equals(signature.getUserProfileId())
								&& signature.getPositionTitle().equals(
										"University Research Director")) {
							signDirector.setUserProfileId(signature
									.getUserProfileId());
							signDirector.setFullName(signature.getFullName());
							signDirector.setSignature(signature.getSignature());
							signDirector.setSignedDate(signature
									.getSignedDate());
							signDirector.setNote(signature.getNote());
							signDirector
									.setPositionTitle("University Research Director");
							signDirector.setDelegated(signature.isDelegated());
							if (!signatures.contains(signDirector)) {
								signatures.add(signDirector);
							}
							directorAlreadySigned = true;
						}
					}
					if (!directorAlreadySigned) {
						signDirector.setUserProfileId(user.getId().toString());
						signDirector.setFullName(user.getFullName());
						signDirector.setSignature("");
						signDirector.setNote("");
						signDirector
								.setPositionTitle("University Research Director");
						signDirector.setDelegated(false);
						if (!signatures.contains(signDirector)) {
							signatures.add(signDirector);
						}
					}
				}
			}
		}
		return signatures;
	}

	private boolean isDelegator(String delegatorId, PositionDetails posDetails) {
		long delegationCount = ds.createQuery(Delegation.class)
				.field("revoked").equal(false).field("delegator user id")
				.equal(delegatorId).field("delegated college")
				.equal(posDetails.getCollege()).field("delegated department")
				.equal(posDetails.getDepartment())
				.field("delegated position type")
				.equal(posDetails.getPositionType())
				.field("delegated position title")
				.equal(posDetails.getPositionTitle()).countAll();
		if (delegationCount > 0) {
			return true;
		}
		return false;
	}

	public List<SignatureInfo> findDelegatedUsersForAUser(ObjectId userId,
			String proposalId, String positionCollege,
			String positionDeptartment, String positionType,
			String positionTitle) {
		Datastore ds = getDatastore();
		List<SignatureInfo> signatures = new ArrayList<SignatureInfo>();

		Query<Delegation> delegationQuery = ds.createQuery(Delegation.class);

		Query<UserProfile> profileQuery = ds.createQuery(UserProfile.class)
				.field("_id").equal(userId).field("deleted").equal(false)
				.retrievedFields(true, "_id");
		delegationQuery.or(
				delegationQuery.criteria("delegator user profile")
						.in(profileQuery.asKeyList()).criteria("revoked")
						.equal(false).criteria("delegatee college")
						.equal(positionCollege)
						.criteria("delegatee department")
						.equal(positionDeptartment)
						.criteria("delegatee position type")
						.equal(positionType)
						.criteria("delegated position title")
						.equal(positionTitle).criteria("proposal id").equal("")
						.criteria("from").lessThanOrEq(new Date())
						.criteria("to").greaterThanOrEq(new Date()),
				delegationQuery.criteria("delegator user profile")
						.in(profileQuery.asKeyList()).criteria("revoked")
						.equal(false).criteria("delegatee college")
						.equal(positionCollege)
						.criteria("delegatee department")
						.equal(positionDeptartment)
						.criteria("delegatee position type")
						.equal(positionType)
						.criteria("delegated position title")
						.equal(positionTitle).criteria("proposal id")
						.containsIgnoreCase(proposalId).criteria("from")
						.lessThanOrEq(new Date()).criteria("to")
						.greaterThanOrEq(new Date()));
		List<Delegation> delegates = delegationQuery.asList();
		for (Delegation delegation : delegates) {
			SignatureInfo signature = new SignatureInfo();
			signature.setUserProfileId(delegation.getDelegateeId());
			signature.setFullName(delegation.getDelegatee());
			signature.setPositionTitle(positionTitle);
			signature.setSignature("");
			signature.setNote("");
			signature.setDelegated(true);
			signature.setDelegatedAs(positionTitle);
			if (!signatures.contains(signature)) {
				signatures.add(signature);
			}
		}
		return signatures;
	}

	public List<SignatureUserInfo> findDelegatedUsersInfoForAUser(
			ObjectId userId, String proposalId, String positionCollege,
			String positionDeptartment, String positionType,
			String positionTitle) {
		Datastore ds = getDatastore();
		List<SignatureUserInfo> signatures = new ArrayList<SignatureUserInfo>();
		Query<Delegation> delegationQuery = ds.createQuery(Delegation.class);
		Query<UserProfile> profileQuery = ds.createQuery(UserProfile.class)
				.field("_id").equal(userId).retrievedFields(true, "_id");
		delegationQuery.or(
				delegationQuery
						.and(delegationQuery.criteria("delegator user profile")
								.in(profileQuery.asKeyList()),
								delegationQuery.criteria("revoked")
										.equal(false),
								delegationQuery.criteria("delegatee college")
										.equalIgnoreCase(positionCollege),
								delegationQuery
										.criteria("delegatee department")
										.equalIgnoreCase(positionDeptartment),
								delegationQuery.criteria(
										"delegatee position type")
										.equalIgnoreCase(positionType),
								delegationQuery.criteria(
										"delegated position title")
										.equalIgnoreCase(positionTitle),
								delegationQuery.criteria("proposal id").equal(
										"")).criteria("from")
						.lessThanOrEq(new Date()).criteria("to")
						.greaterThanOrEq(new Date()),
				delegationQuery
						.and(delegationQuery.criteria("delegator user profile")
								.in(profileQuery.asKeyList()),
								delegationQuery.criteria("revoked")
										.equal(false),
								delegationQuery.criteria("delegatee college")
										.equalIgnoreCase(positionCollege),
								delegationQuery
										.criteria("delegatee department")
										.equalIgnoreCase(positionDeptartment),
								delegationQuery.criteria(
										"delegatee position type")
										.equalIgnoreCase(positionType),
								delegationQuery.criteria(
										"delegated position title")
										.equalIgnoreCase(positionTitle),
								delegationQuery.criteria("proposal id")
										.containsIgnoreCase(proposalId))
						.criteria("from").lessThanOrEq(new Date())
						.criteria("to").greaterThanOrEq(new Date()));
		List<Delegation> delegates = delegationQuery.asList();
		for (Delegation delegation : delegates) {
			ObjectId delegateeUserProfileId = new ObjectId(
					delegation.getDelegateeId());
			UserProfile userQuery = ds
					.createQuery(UserProfile.class)
					.field("_id")
					.equal(delegateeUserProfileId)
					.retrievedFields(true, "_id", "first name", "middle name",
							"last name", "work email", "user id").get();
			SignatureUserInfo signature = new SignatureUserInfo();
			signature.setUserProfileId(userQuery.getId().toString());
			signature.setFullName(userQuery.getFullName());
			signature.setUserName(userQuery.getUserAccount().getUserName());
			signature.setEmail(userQuery.getWorkEmails().get(0));
			signature.setCollege(positionCollege);
			signature.setDepartment(positionDeptartment);
			signature.setPositionType(positionType);
			signature.setPositionTitle(positionTitle);
			signature.setDelegatedAs(positionTitle);
			signature.setSignature("");
			signature.setNote("");
			signature.setDelegated(true);
			if (!signatures.contains(signature)) {
				signatures.add(signature);
			}
		}
		return signatures;
	}

	public List<SignatureUserInfo> findSignaturesExceptInvestigator(
			ObjectId id, Boolean irbApprovalRequired) {
		Datastore ds = getDatastore();
		List<SignatureUserInfo> signatures = new ArrayList<SignatureUserInfo>();
		List<CollegeDepartmentInfo> investigators = new ArrayList<CollegeDepartmentInfo>();
		Query<Proposal> q1 = ds
				.createQuery(Proposal.class)
				.field("_id")
				.equal(id)
				.retrievedFields(true, "_id", "investigator info",
						"signature info");
		Proposal proposal = q1.get();
		// Adding PI
		InvestigatorRefAndPosition PI = proposal.getInvestigatorInfo().getPi();
		CollegeDepartmentInfo investRef = new CollegeDepartmentInfo();
		if (!PI.getUserRef().isDeleted()) {
			investRef.setCollege(PI.getCollege());
			investRef.setDepartment(PI.getDepartment());
			investigators.add(investRef);
		}
		for (InvestigatorRefAndPosition coPIs : proposal.getInvestigatorInfo()
				.getCo_pi()) {
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
		List<String> positions = new ArrayList<String>();
		positions.add("Department Chair");
		positions.add("Business Manager");
		positions.add("Dean");
		positions.add("University Research Administrator");
		positions.add("University Research Director");
		if (irbApprovalRequired) {
			positions.add("IRB");
		}
		Query<UserProfile> profileQuery = ds.createQuery(UserProfile.class)
				.retrievedFields(true, "_id", "first name", "middle name",
						"last name", "details", "work email");
		profileQuery.and(profileQuery.criteria("deleted").equal(false),
				profileQuery.criteria("details.position title").in(positions));
		List<UserProfile> userProfile = profileQuery.asList();
		for (UserProfile user : userProfile) {
			for (PositionDetails posDetails : user.getDetails()) {
				for (CollegeDepartmentInfo colDeptInfo : investigators) {
					if (posDetails.getCollege().equalsIgnoreCase(
							colDeptInfo.getCollege())
							&& posDetails.getDepartment().equalsIgnoreCase(
									colDeptInfo.getDepartment())
							&& posDetails.getPositionTitle().equalsIgnoreCase(
									"Department Chair")) {
						SignatureUserInfo signDeptChair = new SignatureUserInfo();
						if (!isDelegator(user.getId().toString(), posDetails)) {
							signDeptChair.setUserProfileId(user.getId()
									.toString());
							signDeptChair.setFullName(user.getFullName());
							signDeptChair.setUserName(user.getUserAccount()
									.getUserName());
							signDeptChair.setEmail(user.getWorkEmails().get(0));
							signDeptChair.setCollege(posDetails.getCollege());
							signDeptChair.setDepartment(posDetails
									.getDepartment());
							signDeptChair.setPositionType(posDetails
									.getPositionType());
							signDeptChair.setPositionTitle(posDetails
									.getPositionTitle());
							signDeptChair.setSignature("");
							signDeptChair.setNote("");
							signDeptChair.setPositionTitle("Department Chair");
							signDeptChair.setDelegated(false);
							if (!signatures.contains(signDeptChair)) {
								signatures.add(signDeptChair);
							}
						} else {
							List<SignatureUserInfo> delegatedChair = findDelegatedUsersInfoForAUser(
									user.getId(), id.toString(),
									posDetails.getCollege(),
									posDetails.getDepartment(),
									posDetails.getPositionType(),
									"Department Chair");
							for (SignatureUserInfo delegateeInfo : delegatedChair) {
								if (!signatures.contains(delegateeInfo)) {
									signatures.add(delegateeInfo);
								}
							}
						}
					} else if (posDetails.getCollege().equalsIgnoreCase(
							colDeptInfo.getCollege())
							&& posDetails.getDepartment().equalsIgnoreCase(
									colDeptInfo.getDepartment())
							&& posDetails.getPositionTitle().equalsIgnoreCase(
									"Business Manager")) {
						SignatureUserInfo signBusinessMgr = new SignatureUserInfo();
						if (!isDelegator(user.getId().toString(), posDetails)) {
							signBusinessMgr.setUserProfileId(user.getId()
									.toString());
							signBusinessMgr.setFullName(user.getFullName());
							signBusinessMgr.setUserName(user.getUserAccount()
									.getUserName());
							signBusinessMgr.setEmail(user.getWorkEmails()
									.get(0));
							signBusinessMgr.setCollege(posDetails.getCollege());
							signBusinessMgr.setDepartment(posDetails
									.getDepartment());
							signBusinessMgr.setPositionType(posDetails
									.getPositionType());
							signBusinessMgr.setPositionTitle(posDetails
									.getPositionTitle());
							signBusinessMgr.setSignature("");
							signBusinessMgr.setNote("");
							signBusinessMgr
									.setPositionTitle("Business Manager");
							signBusinessMgr.setDelegated(false);
							if (!signatures.contains(signBusinessMgr)) {
								signatures.add(signBusinessMgr);
							}
						} else {
							List<SignatureUserInfo> delegatedBusinessManager = findDelegatedUsersInfoForAUser(
									user.getId(), id.toString(),
									posDetails.getCollege(),
									posDetails.getDepartment(),
									posDetails.getPositionType(),
									"Business Manager");
							for (SignatureUserInfo delegateeInfo : delegatedBusinessManager) {
								if (!signatures.contains(delegateeInfo)) {
									signatures.add(delegateeInfo);
								}
							}
						}
					} else if (posDetails.getCollege().equalsIgnoreCase(
							colDeptInfo.getCollege())
							&& posDetails.getDepartment().equalsIgnoreCase(
									colDeptInfo.getDepartment())
							&& posDetails.getPositionTitle().equalsIgnoreCase(
									"Dean")) {
						SignatureUserInfo signDean = new SignatureUserInfo();
						if (!isDelegator(user.getId().toString(), posDetails)) {
							signDean.setUserProfileId(user.getId().toString());
							signDean.setFullName(user.getFullName());
							signDean.setUserName(user.getUserAccount()
									.getUserName());
							signDean.setEmail(user.getWorkEmails().get(0));
							signDean.setCollege(posDetails.getCollege());
							signDean.setDepartment(posDetails.getDepartment());
							signDean.setPositionType(posDetails
									.getPositionType());
							signDean.setPositionTitle(posDetails
									.getPositionTitle());
							signDean.setSignature("");
							signDean.setNote("");
							signDean.setPositionTitle("Dean");
							signDean.setDelegated(false);
							if (!signatures.contains(signDean)) {
								signatures.add(signDean);
							}
						} else {
							List<SignatureUserInfo> delegatedDean = findDelegatedUsersInfoForAUser(
									user.getId(), id.toString(),
									posDetails.getCollege(),
									posDetails.getDepartment(),
									posDetails.getPositionType(), "Dean");
							for (SignatureUserInfo delegateeInfo : delegatedDean) {
								if (!signatures.contains(delegateeInfo)) {
									signatures.add(delegateeInfo);
								}
							}
						}
					}
				}
				if (posDetails.getPositionTitle().equalsIgnoreCase("IRB")
						&& irbApprovalRequired) {
					SignatureUserInfo signBusinessMgr = new SignatureUserInfo();
					signBusinessMgr.setUserProfileId(user.getId().toString());
					signBusinessMgr.setFullName(user.getFullName());
					signBusinessMgr.setUserName(user.getUserAccount()
							.getUserName());
					signBusinessMgr.setEmail(user.getWorkEmails().get(0));
					signBusinessMgr.setPositionType(posDetails
							.getPositionType());
					signBusinessMgr.setPositionTitle(posDetails
							.getPositionTitle());
					signBusinessMgr.setSignature("");
					signBusinessMgr.setNote("");
					signBusinessMgr.setPositionTitle("IRB");
					signBusinessMgr.setDelegated(false);
					if (!signatures.contains(signBusinessMgr)) {
						signatures.add(signBusinessMgr);
					}
				} else if (posDetails.getPositionTitle().equalsIgnoreCase(
						"University Research Administrator")) {
					SignatureUserInfo signAdmin = new SignatureUserInfo();
					signAdmin.setUserProfileId(user.getId().toString());
					signAdmin.setFullName(user.getFullName());
					signAdmin.setUserName(user.getUserAccount().getUserName());
					signAdmin.setEmail(user.getWorkEmails().get(0));
					signAdmin.setPositionType(posDetails.getPositionType());
					signAdmin.setPositionTitle(posDetails.getPositionTitle());
					signAdmin.setSignature("");
					signAdmin.setNote("");
					signAdmin
							.setPositionTitle("University Research Administrator");
					signAdmin.setDelegated(false);
					if (!signatures.contains(signAdmin)) {
						signatures.add(signAdmin);
					}
				} else if (posDetails.getPositionTitle().equalsIgnoreCase(
						"University Research Director")) {
					SignatureUserInfo signDirector = new SignatureUserInfo();
					signDirector.setUserProfileId(user.getId().toString());
					signDirector.setFullName(user.getFullName());
					signDirector.setUserName(user.getUserAccount()
							.getUserName());
					signDirector.setEmail(user.getWorkEmails().get(0));
					signDirector.setPositionType(posDetails.getPositionType());
					signDirector
							.setPositionTitle(posDetails.getPositionTitle());
					signDirector.setSignature("");
					signDirector.setNote("");
					signDirector
							.setPositionTitle("University Research Director");
					signDirector.setDelegated(false);
					if (!signatures.contains(signDirector)) {
						signatures.add(signDirector);
					}
				}
			}
		}
		return signatures;
	}

	public List<SignatureUserInfo> findUsersExceptInvestigatorForAproposal(
			ObjectId id, Boolean needPI, Boolean needCoPI, Boolean needSenior,
			Boolean needChair, Boolean needManager, Boolean needDean,
			Boolean needIrb, Boolean needResearchadmin, Boolean needDirector) {
		Datastore ds = getDatastore();
		List<SignatureUserInfo> signatures = new ArrayList<SignatureUserInfo>();
		List<CollegeDepartmentInfo> investigators = new ArrayList<CollegeDepartmentInfo>();
		Query<Proposal> q1 = ds
				.createQuery(Proposal.class)
				.field("_id")
				.equal(id)
				.retrievedFields(true, "_id", "irb approval required",
						"investigator info", "signature info");
		Proposal proposal = q1.get();
		Boolean irbApprovalRequired = proposal.isIrbApprovalRequired();
		SignatureUserInfo signUser = new SignatureUserInfo();
		// Adding PI
		InvestigatorRefAndPosition PI = proposal.getInvestigatorInfo().getPi();
		CollegeDepartmentInfo investRef = new CollegeDepartmentInfo();
		if (!PI.getUserRef().isDeleted()) {
			investRef.setCollege(PI.getCollege());
			investRef.setDepartment(PI.getDepartment());
			investigators.add(investRef);
			if (needPI) {
				signUser = new SignatureUserInfo();
				UserProfile user = PI.getUserRef();
				signUser.setUserProfileId(user.getId().toString());
				signUser.setFullName(user.getFullName());
				signUser.setUserName(user.getUserAccount().getUserName());
				signUser.setEmail(user.getWorkEmails().get(0));
				signUser.setCollege(PI.getCollege());
				signUser.setDepartment(PI.getDepartment());
				signUser.setPositionType(PI.getPositionType());
				signUser.setPositionTitle(PI.getPositionTitle());
				signUser.setSignature("");
				signUser.setNote("");
				signUser.setDelegated(false);
				if (!signatures.contains(signUser)) {
					signatures.add(signUser);
				}
			}
		}
		for (InvestigatorRefAndPosition coPIs : proposal.getInvestigatorInfo()
				.getCo_pi()) {
			// Adding Co-PIs
			if (!coPIs.getUserRef().isDeleted()) {
				investRef = new CollegeDepartmentInfo();
				investRef.setCollege(coPIs.getCollege());
				investRef.setDepartment(coPIs.getDepartment());
				if (!investigators.contains(investRef)) {
					investigators.add(investRef);
				}
				if (needCoPI) {
					signUser = new SignatureUserInfo();
					UserProfile user = coPIs.getUserRef();
					signUser.setUserProfileId(user.getId().toString());
					signUser.setFullName(user.getFullName());
					signUser.setUserName(user.getUserAccount().getUserName());
					signUser.setEmail(user.getWorkEmails().get(0));
					signUser.setCollege(coPIs.getCollege());
					signUser.setDepartment(coPIs.getDepartment());
					signUser.setPositionType(coPIs.getPositionType());
					signUser.setPositionTitle(coPIs.getPositionTitle());
					signUser.setSignature("");
					signUser.setNote("");
					signUser.setDelegated(false);
					if (!signatures.contains(signUser)) {
						signatures.add(signUser);
					}
				}
			}
		}
		for (InvestigatorRefAndPosition seniors : proposal
				.getInvestigatorInfo().getSeniorPersonnel()) {
			// Adding Seniors
			if (!seniors.getUserRef().isDeleted()) {
				investRef = new CollegeDepartmentInfo();
				investRef.setCollege(seniors.getCollege());
				investRef.setDepartment(seniors.getDepartment());
				if (!investigators.contains(investRef)) {
					investigators.add(investRef);
				}
				if (needSenior) {
					signUser = new SignatureUserInfo();
					UserProfile user = seniors.getUserRef();
					signUser.setUserProfileId(user.getId().toString());
					signUser.setFullName(user.getFullName());
					signUser.setUserName(user.getUserAccount().getUserName());
					signUser.setEmail(user.getWorkEmails().get(0));
					signUser.setCollege(seniors.getCollege());
					signUser.setDepartment(seniors.getDepartment());
					signUser.setPositionType(seniors.getPositionType());
					signUser.setPositionTitle(seniors.getPositionTitle());
					signUser.setSignature("");
					signUser.setNote("");
					signUser.setDelegated(false);
					if (!signatures.contains(signUser)) {
						signatures.add(signUser);
					}
				}
			}
		}
		List<String> positions = new ArrayList<String>();
		positions.add("Department Chair");
		positions.add("Business Manager");
		positions.add("Dean");
		positions.add("University Research Administrator");
		positions.add("University Research Director");
		if (irbApprovalRequired) {
			positions.add("IRB");
		}
		Query<UserProfile> profileQuery = ds.createQuery(UserProfile.class)
				.retrievedFields(true, "_id", "first name", "middle name",
						"last name", "details", "work email");
		profileQuery.and(profileQuery.criteria("deleted").equal(false),
				profileQuery.criteria("details.position title").in(positions));
		List<UserProfile> userProfile = profileQuery.asList();
		for (UserProfile user : userProfile) {
			for (PositionDetails posDetails : user.getDetails()) {
				for (CollegeDepartmentInfo colDeptInfo : investigators) {
					if (posDetails.getCollege().equalsIgnoreCase(
							colDeptInfo.getCollege())
							&& posDetails.getDepartment().equalsIgnoreCase(
									colDeptInfo.getDepartment())
							&& posDetails.getPositionTitle().equalsIgnoreCase(
									"Department Chair") && needChair) {
						SignatureUserInfo signDeptChair = new SignatureUserInfo();

						signDeptChair.setUserProfileId(user.getId().toString());
						signDeptChair.setFullName(user.getFullName());
						signDeptChair.setUserName(user.getUserAccount()
								.getUserName());
						signDeptChair.setEmail(user.getWorkEmails().get(0));
						signDeptChair.setCollege(posDetails.getCollege());
						signDeptChair.setDepartment(posDetails.getDepartment());
						signDeptChair.setPositionType(posDetails
								.getPositionType());
						signDeptChair.setPositionTitle(posDetails
								.getPositionTitle());
						signDeptChair.setSignature("");
						signDeptChair.setNote("");
						signDeptChair.setDelegated(false);
						if (!signatures.contains(signDeptChair)) {
							signatures.add(signDeptChair);
						}
					} else if (posDetails.getCollege().equalsIgnoreCase(
							colDeptInfo.getCollege())
							&& posDetails.getDepartment().equalsIgnoreCase(
									colDeptInfo.getDepartment())
							&& posDetails.getPositionTitle().equalsIgnoreCase(
									"Business Manager") && needManager) {
						SignatureUserInfo signBusinessMgr = new SignatureUserInfo();
						signBusinessMgr.setUserProfileId(user.getId()
								.toString());
						signBusinessMgr.setFullName(user.getFullName());
						signBusinessMgr.setUserName(user.getUserAccount()
								.getUserName());
						signBusinessMgr.setEmail(user.getWorkEmails().get(0));
						signBusinessMgr.setCollege(posDetails.getCollege());
						signBusinessMgr.setDepartment(posDetails
								.getDepartment());
						signBusinessMgr.setPositionType(posDetails
								.getPositionType());
						signBusinessMgr.setPositionTitle(posDetails
								.getPositionTitle());
						signBusinessMgr.setSignature("");
						signBusinessMgr.setNote("");
						signBusinessMgr.setDelegated(false);
						if (!signatures.contains(signBusinessMgr)) {
							signatures.add(signBusinessMgr);
						}
					} else if (posDetails.getCollege().equalsIgnoreCase(
							colDeptInfo.getCollege())
							&& posDetails.getDepartment().equalsIgnoreCase(
									colDeptInfo.getDepartment())
							&& posDetails.getPositionTitle().equalsIgnoreCase(
									"Dean") && needDean) {
						SignatureUserInfo signDean = new SignatureUserInfo();
						signDean.setUserProfileId(user.getId().toString());
						signDean.setFullName(user.getFullName());
						signDean.setUserName(user.getUserAccount()
								.getUserName());
						signDean.setEmail(user.getWorkEmails().get(0));
						signDean.setCollege(posDetails.getCollege());
						signDean.setDepartment(posDetails.getDepartment());
						signDean.setPositionType(posDetails.getPositionType());
						signDean.setPositionTitle(posDetails.getPositionTitle());
						signDean.setSignature("");
						signDean.setNote("");
						signDean.setDelegated(false);
						if (!signatures.contains(signDean)) {
							signatures.add(signDean);
						}
					} else if (posDetails.getCollege().equalsIgnoreCase(
							colDeptInfo.getCollege())
							&& posDetails.getDepartment().equalsIgnoreCase(
									colDeptInfo.getDepartment())
							&& posDetails.getPositionTitle().equalsIgnoreCase(
									"IRB") && irbApprovalRequired && needIrb) {
						SignatureUserInfo signBusinessMgr = new SignatureUserInfo();
						signBusinessMgr.setUserProfileId(user.getId()
								.toString());
						signBusinessMgr.setFullName(user.getFullName());
						signBusinessMgr.setUserName(user.getUserAccount()
								.getUserName());
						signBusinessMgr.setEmail(user.getWorkEmails().get(0));
						signBusinessMgr.setCollege(posDetails.getCollege());
						signBusinessMgr.setDepartment(posDetails
								.getDepartment());
						signBusinessMgr.setPositionType(posDetails
								.getPositionType());
						signBusinessMgr.setPositionTitle(posDetails
								.getPositionTitle());
						signBusinessMgr.setSignature("");
						signBusinessMgr.setNote("");
						signBusinessMgr.setDelegated(false);
						if (!signatures.contains(signBusinessMgr)) {
							signatures.add(signBusinessMgr);
						}
					} else if (posDetails.getCollege().equalsIgnoreCase(
							colDeptInfo.getCollege())
							&& posDetails.getDepartment().equalsIgnoreCase(
									colDeptInfo.getDepartment())
							&& posDetails.getPositionTitle().equalsIgnoreCase(
									"University Research Administrator")
							&& needResearchadmin) {
						SignatureUserInfo signAdmin = new SignatureUserInfo();
						signAdmin.setUserProfileId(user.getId().toString());
						signAdmin.setFullName(user.getFullName());
						signAdmin.setUserName(user.getUserAccount()
								.getUserName());
						signAdmin.setEmail(user.getWorkEmails().get(0));
						signAdmin.setCollege(posDetails.getCollege());
						signAdmin.setDepartment(posDetails.getDepartment());
						signAdmin.setPositionType(posDetails.getPositionType());
						signAdmin.setPositionTitle(posDetails
								.getPositionTitle());
						signAdmin.setSignature("");
						signAdmin.setNote("");
						signAdmin.setDelegated(false);
						if (!signatures.contains(signAdmin)) {
							signatures.add(signAdmin);
						}
					} else if (posDetails.getCollege().equalsIgnoreCase(
							colDeptInfo.getCollege())
							&& posDetails.getDepartment().equalsIgnoreCase(
									colDeptInfo.getDepartment())
							&& posDetails.getPositionTitle().equalsIgnoreCase(
									"University Research Director")
							&& needDirector) {
						SignatureUserInfo signDirector = new SignatureUserInfo();
						signDirector.setUserProfileId(user.getId().toString());
						signDirector.setFullName(user.getFullName());
						signDirector.setUserName(user.getUserAccount()
								.getUserName());
						signDirector.setEmail(user.getWorkEmails().get(0));
						signDirector.setCollege(posDetails.getCollege());
						signDirector.setDepartment(posDetails.getDepartment());
						signDirector.setPositionType(posDetails
								.getPositionType());
						signDirector.setPositionTitle(posDetails
								.getPositionTitle());
						signDirector.setSignature("");
						signDirector.setNote("");
						signDirector.setDelegated(false);
						if (!signatures.contains(signDirector)) {
							signatures.add(signDirector);
						}
					}
				}
			}
		}
		return signatures;
	}

	public List<SignatureInfo> findUsersInProposal(ObjectId id,
			boolean irbApprovalRequired) {

		Datastore ds = getDatastore();
		List<SignatureInfo> signatures = new ArrayList<SignatureInfo>();
		List<CollegeDepartmentInfo> investigators = new ArrayList<CollegeDepartmentInfo>();
		Query<Proposal> q1 = ds
				.createQuery(Proposal.class)
				.field("_id")
				.equal(id)
				.retrievedFields(true, "_id", "irb approval required",
						"investigator info", "signature info");
		Proposal proposal = q1.get();
		// Adding PI
		SignatureInfo piSign = new SignatureInfo();
		InvestigatorRefAndPosition PI = proposal.getInvestigatorInfo().getPi();
		boolean piAlreadySigned = false;
		for (SignatureInfo signature : proposal.getSignatureInfo()) {
			if (PI.getUserProfileId().toString()
					.equals(signature.getUserProfileId())
					&& !PI.getUserRef().isDeleted()
					&& signature.getPositionTitle().equals("PI")) {
				piSign.setUserProfileId(signature.getUserProfileId());
				piSign.setFullName(signature.getFullName());
				piSign.setSignature(signature.getSignature());
				piSign.setSignedDate(signature.getSignedDate());
				piSign.setNote(signature.getNote());
				piSign.setPositionTitle(signature.getPositionTitle());
				piSign.setDelegated(signature.isDelegated());
				boolean piAlreadyExist = false;
				for (SignatureInfo sign : signatures) {
					if (sign.getUserProfileId().equalsIgnoreCase(
							piSign.getUserProfileId())) {
						piAlreadyExist = true;
						break;
					}
				}
				if (!piAlreadyExist) {
					signatures.add(piSign);
				}
				piAlreadySigned = true;
			}
		}
		if (!piAlreadySigned && !PI.getUserRef().isDeleted()) {
			piSign.setUserProfileId(PI.getUserProfileId().toString());
			piSign.setFullName(PI.getUserRef().getFullName());
			piSign.setSignature("");
			piSign.setNote("");
			piSign.setPositionTitle("PI");
			piSign.setDelegated(false);
			boolean piAlreadyExist = false;
			for (SignatureInfo sign : signatures) {
				if (sign.getUserProfileId().equalsIgnoreCase(
						piSign.getUserProfileId())) {
					piAlreadyExist = true;
					break;
				}
			}
			if (!piAlreadyExist) {
				signatures.add(piSign);
			}
		}
		CollegeDepartmentInfo investRef = new CollegeDepartmentInfo();
		if (!PI.getUserRef().isDeleted()) {
			investRef.setCollege(PI.getCollege());
			investRef.setDepartment(PI.getDepartment());
			investigators.add(investRef);
		}
		for (InvestigatorRefAndPosition coPIs : proposal.getInvestigatorInfo()
				.getCo_pi()) {
			// Adding Co-PIs
			SignatureInfo coPISign = new SignatureInfo();
			boolean coPIAlreadySigned = false;
			for (SignatureInfo signature : proposal.getSignatureInfo()) {
				if (coPIs.getUserProfileId().toString()
						.equals(signature.getUserProfileId())
						&& !coPIs.getUserRef().isDeleted()
						&& signature.getPositionTitle().equals("Co-PI")) {
					coPISign.setUserProfileId(signature.getUserProfileId());
					coPISign.setFullName(signature.getFullName());
					coPISign.setSignature(signature.getSignature());
					coPISign.setSignedDate(signature.getSignedDate());
					coPISign.setNote(signature.getNote());
					coPISign.setPositionTitle(signature.getPositionTitle());
					coPISign.setDelegated(signature.isDelegated());
					boolean coPIAlreadyExist = false;
					for (SignatureInfo sign : signatures) {
						if (sign.getUserProfileId().equalsIgnoreCase(
								coPISign.getUserProfileId())) {
							coPIAlreadyExist = true;
							break;
						}
					}
					if (!coPIAlreadyExist) {
						signatures.add(coPISign);
					}
					coPIAlreadySigned = true;
				}
			}
			if (!coPIAlreadySigned && !coPIs.getUserRef().isDeleted()) {
				coPISign.setUserProfileId(coPIs.getUserProfileId().toString());
				coPISign.setFullName(coPIs.getUserRef().getFullName());
				coPISign.setSignature("");
				coPISign.setNote("");
				coPISign.setPositionTitle("Co-PI");
				coPISign.setDelegated(false);
				boolean coPIAlreadyExist = false;
				for (SignatureInfo sign : signatures) {
					if (sign.getUserProfileId().equalsIgnoreCase(
							coPISign.getUserProfileId())) {
						coPIAlreadyExist = true;
						break;
					}
				}
				if (!coPIAlreadyExist) {
					signatures.add(coPISign);
				}
			}
			if (!coPIs.getUserRef().isDeleted()) {
				investRef = new CollegeDepartmentInfo();
				investRef.setCollege(coPIs.getCollege());
				investRef.setDepartment(coPIs.getDepartment());
				if (!investigators.contains(investRef)) {
					investigators.add(investRef);
				}
			}
		}
		for (InvestigatorRefAndPosition seniors : proposal
				.getInvestigatorInfo().getSeniorPersonnel()) {
			// Adding Seniors
			SignatureInfo seniorSign = new SignatureInfo();
			boolean seniorAlreadySigned = false;
			for (SignatureInfo signature : proposal.getSignatureInfo()) {
				if (seniors.getUserProfileId().toString()
						.equals(signature.getUserProfileId())
						&& !seniors.getUserRef().isDeleted()
						&& signature.getPositionTitle().equals(
								"Senior Personnel")) {
					seniorSign.setUserProfileId(signature.getUserProfileId());
					seniorSign.setFullName(signature.getFullName());
					seniorSign.setSignature(signature.getSignature());
					seniorSign.setSignedDate(signature.getSignedDate());
					seniorSign.setNote(signature.getNote());
					seniorSign.setPositionTitle(signature.getPositionTitle());
					seniorSign.setDelegated(signature.isDelegated());
					if (!signatures.contains(seniorSign)) {
						signatures.add(seniorSign);
					}
					boolean seniorAlreadyExist = false;
					for (SignatureInfo sign : signatures) {
						if (sign.getUserProfileId().equalsIgnoreCase(
								seniorSign.getUserProfileId())) {
							seniorAlreadyExist = true;
							break;
						}
					}
					if (!seniorAlreadyExist) {
						signatures.add(seniorSign);
					}
					seniorAlreadySigned = true;
				}
			}
			if (!seniorAlreadySigned && !seniors.getUserRef().isDeleted()) {
				seniorSign.setUserProfileId(seniors.getUserProfileId()
						.toString());
				seniorSign.setFullName(seniors.getUserRef().getFullName());
				seniorSign.setSignature("");
				seniorSign.setNote("");
				seniorSign.setPositionTitle("Senior Personnel");
				seniorSign.setDelegated(false);
				boolean seniorAlreadyExist = false;
				for (SignatureInfo sign : signatures) {
					if (sign.getUserProfileId().equalsIgnoreCase(
							seniorSign.getUserProfileId())) {
						seniorAlreadyExist = true;
						break;
					}
				}
				if (!seniorAlreadyExist) {
					signatures.add(seniorSign);
				}
			}
			if (!seniors.getUserRef().isDeleted()) {
				investRef = new CollegeDepartmentInfo();
				investRef.setCollege(seniors.getCollege());
				investRef.setDepartment(seniors.getDepartment());
				if (!investigators.contains(investRef)) {
					investigators.add(investRef);
				}
			}
		}
		List<String> positions = new ArrayList<String>();
		positions.add("Department Chair");
		positions.add("Business Manager");
		positions.add("Dean");
		positions.add("University Research Administrator");
		positions.add("University Research Director");
		if (irbApprovalRequired) {
			positions.add("IRB");
		}
		Query<UserProfile> profileQuery = ds.createQuery(UserProfile.class)
				.retrievedFields(true, "_id", "first name", "middle name",
						"last name", "details", "work email");
		profileQuery.and(profileQuery.criteria("deleted").equal(false),
				profileQuery.criteria("details.position title").in(positions));
		List<UserProfile> userProfile = profileQuery.asList();
		for (UserProfile user : userProfile) {
			for (PositionDetails posDetails : user.getDetails()) {
				for (CollegeDepartmentInfo colDeptInfo : investigators) {
					if (posDetails.getCollege().equalsIgnoreCase(
							colDeptInfo.getCollege())
							&& posDetails.getDepartment().equalsIgnoreCase(
									colDeptInfo.getDepartment())
							&& posDetails.getPositionTitle().equalsIgnoreCase(
									"Department Chair")) {
						SignatureInfo signDeptChair = new SignatureInfo();
						boolean departmentChairAlreadySigned = false;
						for (SignatureInfo signature : proposal
								.getSignatureInfo()) {
							if (user.getId().toString()
									.equals(signature.getUserProfileId())
									&& signature.getPositionTitle().equals(
											"Department Chair")) {
								signDeptChair.setUserProfileId(signature
										.getUserProfileId());
								signDeptChair.setFullName(signature
										.getFullName());
								signDeptChair.setSignature(signature
										.getSignature());
								signDeptChair.setSignedDate(signature
										.getSignedDate());
								signDeptChair.setNote(signature.getNote());
								signDeptChair.setPositionTitle(signature
										.getPositionTitle());
								signDeptChair.setDelegated(signature
										.isDelegated());
								if (!signatures.contains(signDeptChair)) {
									signatures.add(signDeptChair);
								}
								departmentChairAlreadySigned = true;
							}
						}
						if (!departmentChairAlreadySigned) {
							signDeptChair.setUserProfileId(user.getId()
									.toString());
							signDeptChair.setFullName(user.getFullName());
							signDeptChair.setSignature("");
							signDeptChair.setNote("");
							signDeptChair.setPositionTitle("Department Chair");
							signDeptChair.setDelegated(false);
							if (!signatures.contains(signDeptChair)) {
								signatures.add(signDeptChair);
							}
						}
					} else if (posDetails.getCollege().equalsIgnoreCase(
							colDeptInfo.getCollege())
							&& posDetails.getDepartment().equalsIgnoreCase(
									colDeptInfo.getDepartment())
							&& posDetails.getPositionTitle().equalsIgnoreCase(
									"Business Manager")) {
						SignatureInfo signBusinessMgr = new SignatureInfo();

						boolean businessManagerAlreadySigned = false;
						for (SignatureInfo signature : proposal
								.getSignatureInfo()) {
							if (user.getId().toString()
									.equals(signature.getUserProfileId())
									&& signature.getPositionTitle().equals(
											"Business Manager")) {
								signBusinessMgr.setUserProfileId(signature
										.getUserProfileId());
								signBusinessMgr.setFullName(signature
										.getFullName());
								signBusinessMgr.setSignature(signature
										.getSignature());
								signBusinessMgr.setSignedDate(signature
										.getSignedDate());
								signBusinessMgr.setNote(signature.getNote());
								signBusinessMgr.setPositionTitle(signature
										.getPositionTitle());
								signBusinessMgr.setDelegated(signature
										.isDelegated());
								if (!signatures.contains(signBusinessMgr)) {
									signatures.add(signBusinessMgr);
								}
								businessManagerAlreadySigned = true;
							}
						}
						if (!businessManagerAlreadySigned) {
							signBusinessMgr.setUserProfileId(user.getId()
									.toString());
							signBusinessMgr.setFullName(user.getFullName());
							signBusinessMgr.setSignature("");
							signBusinessMgr.setNote("");
							signBusinessMgr
									.setPositionTitle("Business Manager");
							signBusinessMgr.setDelegated(false);
							if (!signatures.contains(signBusinessMgr)) {
								signatures.add(signBusinessMgr);
							}
						}
					} else if (posDetails.getCollege().equalsIgnoreCase(
							colDeptInfo.getCollege())
							&& posDetails.getDepartment().equalsIgnoreCase(
									colDeptInfo.getDepartment())
							&& posDetails.getPositionTitle().equalsIgnoreCase(
									"Dean")) {
						SignatureInfo signDean = new SignatureInfo();

						boolean deanAlreadySigned = false;
						for (SignatureInfo signature : proposal
								.getSignatureInfo()) {
							if (user.getId().toString()
									.equals(signature.getUserProfileId())
									&& signature.getPositionTitle().equals(
											"Dean")) {
								signDean.setUserProfileId(signature
										.getUserProfileId());
								signDean.setFullName(signature.getFullName());
								signDean.setSignature(signature.getSignature());
								signDean.setSignedDate(signature
										.getSignedDate());
								signDean.setNote(signature.getNote());
								signDean.setPositionTitle(signature
										.getPositionTitle());
								signDean.setDelegated(signature.isDelegated());
								if (!signatures.contains(signDean)) {
									signatures.add(signDean);
								}
								deanAlreadySigned = true;
							}
						}
						if (!deanAlreadySigned) {
							signDean.setUserProfileId(user.getId().toString());
							signDean.setFullName(user.getFullName());
							signDean.setSignature("");
							signDean.setNote("");
							signDean.setPositionTitle("Dean");
							signDean.setDelegated(false);
							if (!signatures.contains(signDean)) {
								signatures.add(signDean);
							}
						}
					} else if (posDetails.getCollege().equalsIgnoreCase(
							colDeptInfo.getCollege())
							&& posDetails.getDepartment().equalsIgnoreCase(
									colDeptInfo.getDepartment())
							&& posDetails.getPositionTitle().equalsIgnoreCase(
									"IRB") && irbApprovalRequired) {
						SignatureInfo signBusinessMgr = new SignatureInfo();

						boolean irbAlreadySigned = false;
						for (SignatureInfo signature : proposal
								.getSignatureInfo()) {
							if (user.getId().toString()
									.equals(signature.getUserProfileId())
									&& signature.getPositionTitle().equals(
											"IRB")) {
								signBusinessMgr.setUserProfileId(signature
										.getUserProfileId());
								signBusinessMgr.setFullName(signature
										.getFullName());
								signBusinessMgr.setSignature(signature
										.getSignature());
								signBusinessMgr.setSignedDate(signature
										.getSignedDate());
								signBusinessMgr.setNote(signature.getNote());
								signBusinessMgr.setPositionTitle(signature
										.getPositionTitle());
								signBusinessMgr.setDelegated(signature
										.isDelegated());
								if (!signatures.contains(signBusinessMgr)) {
									signatures.add(signBusinessMgr);
								}
								irbAlreadySigned = true;
							}
						}
						if (!irbAlreadySigned) {
							signBusinessMgr.setUserProfileId(user.getId()
									.toString());
							signBusinessMgr.setFullName(user.getFullName());
							signBusinessMgr.setSignature("");
							signBusinessMgr.setNote("");
							signBusinessMgr.setPositionTitle("IRB");
							signBusinessMgr.setDelegated(false);
							if (!signatures.contains(signBusinessMgr)) {
								signatures.add(signBusinessMgr);
							}
						}
					} else if (posDetails.getCollege().equalsIgnoreCase(
							colDeptInfo.getCollege())
							&& posDetails.getDepartment().equalsIgnoreCase(
									colDeptInfo.getDepartment())
							&& posDetails.getPositionTitle().equalsIgnoreCase(
									"University Research Administrator")) {
						SignatureInfo signAdmin = new SignatureInfo();
						boolean adminAlreadySigned = false;
						for (SignatureInfo signature : proposal
								.getSignatureInfo()) {
							if (user.getId().toString()
									.equals(signature.getUserProfileId())
									&& signature
											.getPositionTitle()
											.equals("University Research Administrator")) {
								signAdmin.setUserProfileId(signature
										.getUserProfileId());
								signAdmin.setFullName(user.getFullName());
								signAdmin
										.setSignature(signature.getSignature());
								signAdmin.setSignedDate(signature
										.getSignedDate());
								signAdmin.setNote(signature.getNote());
								signAdmin
										.setPositionTitle("University Research Administrator");
								signAdmin.setDelegated(signature.isDelegated());
								if (!signatures.contains(signAdmin)) {
									signatures.add(signAdmin);
								}
								adminAlreadySigned = true;
							}
						}
						if (!adminAlreadySigned) {
							signAdmin.setUserProfileId(user.getId().toString());
							signAdmin.setFullName(user.getFullName());
							signAdmin.setSignature("");
							signAdmin.setNote("");
							signAdmin
									.setPositionTitle("University Research Administrator");
							signAdmin.setDelegated(false);
							if (!signatures.contains(signAdmin)) {
								signatures.add(signAdmin);
							}
						}
					} else if (posDetails.getCollege().equalsIgnoreCase(
							colDeptInfo.getCollege())
							&& posDetails.getDepartment().equalsIgnoreCase(
									colDeptInfo.getDepartment())
							&& posDetails.getPositionTitle().equalsIgnoreCase(
									"University Research Director")) {
						SignatureInfo signDirector = new SignatureInfo();
						boolean directorAlreadySigned = false;
						for (SignatureInfo signature : proposal
								.getSignatureInfo()) {
							if (user.getId().toString()
									.equals(signature.getUserProfileId())
									&& signature.getPositionTitle().equals(
											"University Research Director")) {
								signDirector.setUserProfileId(signature
										.getUserProfileId());
								signDirector.setFullName(signature
										.getFullName());
								signDirector.setSignature(signature
										.getSignature());
								signDirector.setSignedDate(signature
										.getSignedDate());
								signDirector.setNote(signature.getNote());
								signDirector
										.setPositionTitle("University Research Director");
								signDirector.setDelegated(signature
										.isDelegated());
								if (!signatures.contains(signDirector)) {
									signatures.add(signDirector);
								}
								directorAlreadySigned = true;
							}
						}
						if (!directorAlreadySigned) {
							signDirector.setUserProfileId(user.getId()
									.toString());
							signDirector.setFullName(user.getFullName());
							signDirector.setSignature("");
							signDirector.setNote("");
							signDirector
									.setPositionTitle("University Research Director");
							signDirector.setDelegated(false);
							if (!signatures.contains(signDirector)) {
								signatures.add(signDirector);
							}
						}
					}
				}
			}
		}
		return signatures;
	}

	public boolean validateNotEmptyValue(String value) {
		if (!value.equalsIgnoreCase("")) {
			return true;
		} else {
			return false;
		}
	}

	public boolean verifyValidFileExtension(String extension) {
		List<String> list = Arrays.asList("jpg", "png", "gif", "jpeg", "bmp",
				"png", "pdf", "doc", "docx", "xls", "xlsx", "txt");
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

	/**
	 * @param existingProposal
	 * @param proposalID
	 * @param newProjectInfo
	 * @param projectInfo
	 * @throws Exception
	 */
	public void getProposalTitle(Proposal existingProposal, String proposalID,
			ProjectInfo newProjectInfo, JsonNode projectInfo) throws Exception {
		if (projectInfo != null && projectInfo.has("ProjectTitle")) {
			final String proposalTitle = projectInfo.get("ProjectTitle")
					.textValue().trim().replaceAll("\\<[^>]*>", "");
			if (validateNotEmptyValue(proposalTitle)) {
				if (!proposalID.equals("0")) {
					if (!existingProposal.getProjectInfo().getProjectTitle()
							.equals(proposalTitle)) {
						existingProposal.getProjectInfo().setProjectTitle(
								proposalTitle);
					}
				} else {
					newProjectInfo.setProjectTitle(proposalTitle);
				}
			} else {
				throw new Exception("The Proposal Title can not be Empty");
			}
		}
	}

	/**
	 * @param existingProposal
	 * @param proposalID
	 * @param newProjectInfo
	 * @param projectInfo
	 */
	public void getProposalType(Proposal existingProposal, String proposalID,
			ProjectInfo newProjectInfo, JsonNode projectInfo) {
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
				if (!existingProposal.getProjectInfo().getProjectType()
						.equals(projectType)) {
					existingProposal.getProjectInfo().setProjectType(
							projectType);
				}
			} else {
				newProjectInfo.setProjectType(projectType);
			}
		}
	}

	/**
	 * @param existingProposal
	 * @param proposalID
	 * @param newProjectInfo
	 * @param projectInfo
	 */
	public void getTypeOfRequest(Proposal existingProposal, String proposalID,
			ProjectInfo newProjectInfo, JsonNode projectInfo) {
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
				if (!existingProposal.getProjectInfo().getTypeOfRequest()
						.equals(typeOfRequest)) {
					existingProposal.getProjectInfo().setTypeOfRequest(
							typeOfRequest);
				}
			} else {
				newProjectInfo.setTypeOfRequest(typeOfRequest);
			}
		}
	}

	/**
	 * @param existingProposal
	 * @param proposalID
	 * @param newProjectInfo
	 * @param projectInfo
	 */
	public void getProjectLocation(Proposal existingProposal,
			String proposalID, ProjectInfo newProjectInfo, JsonNode projectInfo) {
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
				if (!existingProposal.getProjectInfo().getProjectLocation()
						.equals(projectLocation)) {
					existingProposal.getProjectInfo().setProjectLocation(
							projectLocation);
				}
			} else {
				newProjectInfo.setProjectLocation(projectLocation);
			}
		}
	}

	/**
	 * @param existingProposal
	 * @param proposalID
	 * @param newProjectInfo
	 * @param projectInfo
	 * @throws ParseException
	 * @throws Exception
	 */
	public void getDueDate(Proposal existingProposal, String proposalID,
			ProjectInfo newProjectInfo, JsonNode projectInfo)
			throws ParseException, Exception {
		if (projectInfo != null && projectInfo.has("DueDate")) {
			Date dueDate = formatter.parse(projectInfo.get("DueDate")
					.textValue().trim().replaceAll("\\<[^>]*>", ""));
			if (validateNotEmptyValue(dueDate.toString())) {
				if (!proposalID.equals("0")) {
					if (!existingProposal.getProjectInfo().getDueDate()
							.equals(dueDate)) {
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

	/**
	 * @param existingProposal
	 * @param proposalID
	 * @param newProjectInfo
	 * @param projectInfo
	 * @throws ParseException
	 * @throws Exception
	 */
	public void getProjectPeriod(Proposal existingProposal, String proposalID,
			ProjectInfo newProjectInfo, JsonNode projectInfo)
			throws ParseException, Exception {
		ProjectPeriod projectPeriod = new ProjectPeriod();
		if (projectInfo != null && projectInfo.has("ProjectPeriodFrom")) {
			Date periodFrom = formatter.parse(projectInfo
					.get("ProjectPeriodFrom").textValue().trim()
					.replaceAll("\\<[^>]*>", ""));
			if (validateNotEmptyValue(periodFrom.toString())) {
				projectPeriod.setFrom(periodFrom);
			} else {
				throw new Exception("The Project Period From can not be Empty");
			}
		}
		if (projectInfo != null && projectInfo.has("ProjectPeriodTo")) {
			Date periodTo = formatter.parse(projectInfo.get("ProjectPeriodTo")
					.textValue().trim().replaceAll("\\<[^>]*>", ""));
			if (validateNotEmptyValue(periodTo.toString())) {
				projectPeriod.setTo(periodTo);
			} else {
				throw new Exception("The Project Period To can not be Empty");
			}
		}
		if (!proposalID.equals("0")) {
			if (!existingProposal.getProjectInfo().getProjectPeriod()
					.equals(projectPeriod)) {
				existingProposal.getProjectInfo().setProjectPeriod(
						projectPeriod);
			}
		} else {
			newProjectInfo.setProjectPeriod(projectPeriod);
		}
	}

	/**
	 * @param existingProposal
	 * @param proposalID
	 * @param proposalInfo
	 * @throws Exception
	 * @throws ParseException
	 */
	public void getProjectInfo(Proposal existingProposal, String proposalID,
			JsonNode proposalInfo) throws Exception, ParseException {
		ProjectInfo newProjectInfo = new ProjectInfo();
		if (proposalInfo != null && proposalInfo.has("ProjectInfo")) {
			JsonNode projectInfo = proposalInfo.get("ProjectInfo");
			getProposalTitle(existingProposal, proposalID, newProjectInfo,
					projectInfo);
			getProposalType(existingProposal, proposalID, newProjectInfo,
					projectInfo);
			getTypeOfRequest(existingProposal, proposalID, newProjectInfo,
					projectInfo);
			getProjectLocation(existingProposal, proposalID, newProjectInfo,
					projectInfo);
			getDueDate(existingProposal, proposalID, newProjectInfo,
					projectInfo);
			getProjectPeriod(existingProposal, proposalID, newProjectInfo,
					projectInfo);
		}
		// ProjectInfo
		if (proposalID.equals("0")) {
			existingProposal.setProjectInfo(newProjectInfo);
		}
	}

	/**
	 * @param existingProposal
	 * @param proposalID
	 * @param proposalInfo
	 * @throws Exception
	 */
	public void getSponsorAndBudgetInfo(Proposal existingProposal,
			String proposalID, JsonNode proposalInfo) throws Exception {
		SponsorAndBudgetInfo newSponsorAndBudgetInfo = new SponsorAndBudgetInfo();
		if (proposalInfo != null && proposalInfo.has("SponsorAndBudgetInfo")) {
			JsonNode sponsorAndBudgetInfo = proposalInfo
					.get("SponsorAndBudgetInfo");
			if (sponsorAndBudgetInfo != null
					&& sponsorAndBudgetInfo.has("GrantingAgency")) {
				for (String grantingAgency : sponsorAndBudgetInfo
						.get("GrantingAgency").textValue().trim()
						.replaceAll("\\<[^>]*>", "").split(", ")) {
					if (validateNotEmptyValue(grantingAgency)) {
						newSponsorAndBudgetInfo.getGrantingAgency().add(
								grantingAgency);
					} else {
						throw new Exception(
								"The Granting Agency can not be Empty");
					}
				}
			}
			if (sponsorAndBudgetInfo != null
					&& sponsorAndBudgetInfo.has("DirectCosts")) {
				final String directCost = sponsorAndBudgetInfo
						.get("DirectCosts").textValue().trim()
						.replaceAll("\\<[^>]*>", "");
				if (validateNotEmptyValue(directCost)) {
					newSponsorAndBudgetInfo.setDirectCosts(Double
							.parseDouble(directCost));
				} else {
					throw new Exception("The Direct Costs can not be Empty");
				}
			}
			if (sponsorAndBudgetInfo != null
					&& sponsorAndBudgetInfo.has("FACosts")) {
				final String FACosts = sponsorAndBudgetInfo.get("FACosts")
						.textValue().trim().replaceAll("\\<[^>]*>", "");
				if (validateNotEmptyValue(FACosts)) {
					newSponsorAndBudgetInfo.setFaCosts(Double
							.parseDouble(FACosts));
				} else {
					throw new Exception("The FA Costs can not be Empty");
				}
			}
			if (sponsorAndBudgetInfo != null
					&& sponsorAndBudgetInfo.has("TotalCosts")) {
				final String totalCosts = sponsorAndBudgetInfo
						.get("TotalCosts").textValue().trim()
						.replaceAll("\\<[^>]*>", "");
				if (validateNotEmptyValue(totalCosts)) {
					newSponsorAndBudgetInfo.setTotalCosts(Double
							.parseDouble(totalCosts));
				} else {
					throw new Exception("The Total Costs can not be Empty");
				}
			}
			if (sponsorAndBudgetInfo != null
					&& sponsorAndBudgetInfo.has("FARate")) {
				final String FARate = sponsorAndBudgetInfo.get("FARate")
						.textValue().trim().replaceAll("\\<[^>]*>", "");
				if (validateNotEmptyValue(FARate)) {
					newSponsorAndBudgetInfo.setFaRate(Double
							.parseDouble(FARate));
				} else {
					throw new Exception("The FA Rate can not be Empty");
				}
			}
		}

		// SponsorAndBudgetInfo
		if (!proposalID.equals("0")) {
			if (!existingProposal.getSponsorAndBudgetInfo().equals(
					newSponsorAndBudgetInfo)) {
				existingProposal
						.setSponsorAndBudgetInfo(newSponsorAndBudgetInfo);
			}
		} else {
			existingProposal.setSponsorAndBudgetInfo(newSponsorAndBudgetInfo);
		}
	}

	/**
	 * @param existingProposal
	 * @param proposalID
	 * @param proposalInfo
	 */
	public void getCostShareInfo(Proposal existingProposal, String proposalID,
			JsonNode proposalInfo) {
		CostShareInfo newCostShareInfo = new CostShareInfo();
		if (proposalInfo != null && proposalInfo.has("CostShareInfo")) {
			JsonNode costShareInfo = proposalInfo.get("CostShareInfo");
			if (costShareInfo != null
					&& costShareInfo.has("InstitutionalCommitted")) {
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

			if (costShareInfo != null
					&& costShareInfo.has("ThirdPartyCommitted")) {
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

	/**
	 * @param existingProposal
	 * @param proposalID
	 * @param proposalInfo
	 */
	public void getUniversityCommitments(Proposal existingProposal,
			String proposalID, JsonNode proposalInfo) {
		UniversityCommitments newUnivCommitments = new UniversityCommitments();
		if (proposalInfo != null && proposalInfo.has("UnivCommitments")) {
			JsonNode univCommitments = proposalInfo.get("UnivCommitments");
			if (univCommitments != null
					&& univCommitments.has("NewRenovatedFacilitiesRequired")) {
				switch (univCommitments.get("NewRenovatedFacilitiesRequired")
						.textValue()) {
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
			if (univCommitments != null
					&& univCommitments.has("RentalSpaceRequired")) {
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
			if (univCommitments != null
					&& univCommitments.has("InstitutionalCommitmentRequired")) {
				switch (univCommitments.get("InstitutionalCommitmentRequired")
						.textValue()) {
				case "1":
					newUnivCommitments.setInstitutionalCommitmentRequired(true);
					break;
				case "2":
					newUnivCommitments
							.setInstitutionalCommitmentRequired(false);
					break;
				default:
					break;
				}
			}
		}
		// UnivCommitments
		if (!proposalID.equals("0")) {
			if (!existingProposal.getUniversityCommitments().equals(
					newUnivCommitments)) {
				existingProposal.setUniversityCommitments(newUnivCommitments);
			}
		} else {
			existingProposal.setUniversityCommitments(newUnivCommitments);
		}
	}

	/**
	 * @param existingProposal
	 * @param proposalID
	 * @param proposalInfo
	 */
	public void getConflictOfInterest(Proposal existingProposal,
			String proposalID, JsonNode proposalInfo) {
		ConflictOfInterest newConflictOfInterest = new ConflictOfInterest();
		if (proposalInfo != null && proposalInfo.has("ConflicOfInterestInfo")) {
			JsonNode conflicOfInterestInfo = proposalInfo
					.get("ConflicOfInterestInfo");
			if (conflicOfInterestInfo != null
					&& conflicOfInterestInfo.has("FinancialCOI")) {
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
			if (conflicOfInterestInfo != null
					&& conflicOfInterestInfo.has("ConflictDisclosed")) {
				switch (conflicOfInterestInfo.get("ConflictDisclosed")
						.textValue()) {
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
			if (conflicOfInterestInfo != null
					&& conflicOfInterestInfo.has("DisclosureFormChange")) {
				switch (conflicOfInterestInfo.get("DisclosureFormChange")
						.textValue()) {
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
			if (!existingProposal.getConflicOfInterest().equals(
					newConflictOfInterest)) {
				existingProposal.setConflicOfInterest(newConflictOfInterest);
			}
		} else {
			existingProposal.setConflicOfInterest(newConflictOfInterest);
		}
	}

	/**
	 * @param existingProposal
	 * @param proposalID
	 * @param proposalInfo
	 */
	public void getAdditionalInfo(Proposal existingProposal, String proposalID,
			JsonNode proposalInfo) {
		AdditionalInfo newAdditionalInfo = new AdditionalInfo();
		if (proposalInfo != null && proposalInfo.has("AdditionalInfo")) {
			JsonNode additionalInfo = proposalInfo.get("AdditionalInfo");
			if (additionalInfo != null
					&& additionalInfo.has("AnticipatesForeignNationalsPayment")) {
				switch (additionalInfo
						.get("AnticipatesForeignNationalsPayment").textValue()) {
				case "1":
					newAdditionalInfo
							.setAnticipatesForeignNationalsPayment(true);
					break;
				case "2":
					newAdditionalInfo
							.setAnticipatesForeignNationalsPayment(false);
					break;
				default:
					break;
				}
			}
			if (additionalInfo != null
					&& additionalInfo.has("AnticipatesCourseReleaseTime")) {
				switch (additionalInfo.get("AnticipatesCourseReleaseTime")
						.textValue()) {
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
			if (additionalInfo != null
					&& additionalInfo
							.has("RelatedToCenterForAdvancedEnergyStudies")) {
				switch (additionalInfo.get(
						"RelatedToCenterForAdvancedEnergyStudies").textValue()) {
				case "1":
					newAdditionalInfo
							.setRelatedToCenterForAdvancedEnergyStudies(true);
					break;
				case "2":
					newAdditionalInfo
							.setRelatedToCenterForAdvancedEnergyStudies(false);
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

	/**
	 * @param existingProposal
	 * @param proposalID
	 * @param proposalInfo
	 * @throws Exception
	 */
	public void getCollaborationInfo(Proposal existingProposal,
			String proposalID, JsonNode proposalInfo) throws Exception {
		CollaborationInfo newCollaborationInfo = new CollaborationInfo();
		if (proposalInfo != null && proposalInfo.has("CollaborationInfo")) {
			JsonNode collaborationInfo = proposalInfo.get("CollaborationInfo");
			if (collaborationInfo != null
					&& collaborationInfo.has("InvolveNonFundedCollab")) {
				switch (collaborationInfo.get("InvolveNonFundedCollab")
						.textValue()) {
				case "1":
					newCollaborationInfo.setInvolveNonFundedCollab(true);
					if (collaborationInfo != null
							&& collaborationInfo.has("Collaborators")) {
						final String collabarationName = collaborationInfo
								.get("Collaborators").textValue().trim()
								.replaceAll("\\<[^>]*>", "");
						if (validateNotEmptyValue(collabarationName)) {
							newCollaborationInfo
									.setInvolvedCollaborators(collabarationName);
						} else {
							throw new Exception(
									"Collaborators can not be Empty");
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
			if (!existingProposal.getCollaborationInfo().equals(
					newCollaborationInfo)) {
				existingProposal.setCollaborationInfo(newCollaborationInfo);
			}
		} else {
			existingProposal.setCollaborationInfo(newCollaborationInfo);
		}
	}

	/**
	 * @param existingProposal
	 * @param proposalID
	 * @param proposalInfo
	 * @throws Exception
	 */
	public void getConfidentialInfo(Proposal existingProposal,
			String proposalID, JsonNode proposalInfo) throws Exception {
		ConfidentialInfo newConfidentialInfo = new ConfidentialInfo();
		if (proposalInfo != null && proposalInfo.has("ConfidentialInfo")) {
			JsonNode confidentialInfo = proposalInfo.get("ConfidentialInfo");
			if (confidentialInfo != null
					&& confidentialInfo.has("ContainConfidentialInformation")) {
				switch (confidentialInfo.get("ContainConfidentialInformation")
						.textValue()) {
				case "1":
					newConfidentialInfo.setContainConfidentialInformation(true);
					if (confidentialInfo != null
							&& confidentialInfo.has("OnPages")) {
						final String onPages = confidentialInfo.get("OnPages")
								.textValue().trim().replaceAll("\\<[^>]*>", "");
						if (validateNotEmptyValue(onPages)) {
							newConfidentialInfo.setOnPages(onPages);
						} else {
							throw new Exception("The Pages can not be Empty");
						}
					}
					if (confidentialInfo != null
							&& confidentialInfo.has("Patentable")) {
						newConfidentialInfo.setPatentable(confidentialInfo.get(
								"Patentable").booleanValue());
					}
					if (confidentialInfo != null
							&& confidentialInfo.has("Copyrightable")) {
						newConfidentialInfo.setCopyrightable(confidentialInfo
								.get("Copyrightable").booleanValue());
					}
					break;
				case "2":
					newConfidentialInfo
							.setContainConfidentialInformation(false);
					break;
				default:
					break;
				}
			}
			if (confidentialInfo != null
					&& confidentialInfo.has("InvolveIntellectualProperty")) {
				switch (confidentialInfo.get("InvolveIntellectualProperty")
						.textValue()) {
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
			if (!existingProposal.getConfidentialInfo().equals(
					newConfidentialInfo)) {
				existingProposal.setConfidentialInfo(newConfidentialInfo);
			}
		} else {
			existingProposal.setConfidentialInfo(newConfidentialInfo);
		}
	}

	/**
	 * @param existingProposal
	 * @param oSPSectionInfo
	 */
	public void getFundingSource(Proposal existingProposal,
			JsonNode oSPSectionInfo) {
		FundingSource newFundingSource = new FundingSource();
		if (oSPSectionInfo != null && oSPSectionInfo.has("Federal")) {
			newFundingSource.setFederal(oSPSectionInfo.get("Federal")
					.booleanValue());
		}
		if (oSPSectionInfo != null && oSPSectionInfo.has("FederalFlowThrough")) {
			newFundingSource.setFederalFlowThrough(oSPSectionInfo.get(
					"FederalFlowThrough").booleanValue());
		}
		if (oSPSectionInfo != null && oSPSectionInfo.has("StateOfIdahoEntity")) {
			newFundingSource.setStateOfIdahoEntity(oSPSectionInfo.get(
					"StateOfIdahoEntity").booleanValue());
		}
		if (oSPSectionInfo != null && oSPSectionInfo.has("PrivateForProfit")) {
			newFundingSource.setPrivateForProfit(oSPSectionInfo.get(
					"PrivateForProfit").booleanValue());
		}
		if (oSPSectionInfo != null
				&& oSPSectionInfo.has("NonProfitOrganization")) {
			newFundingSource.setNonProfitOrganization(oSPSectionInfo.get(
					"NonProfitOrganization").booleanValue());
		}
		if (oSPSectionInfo != null && oSPSectionInfo.has("NonIdahoStateEntity")) {
			newFundingSource.setNonIdahoStateEntity(oSPSectionInfo.get(
					"NonIdahoStateEntity").booleanValue());
		}
		if (oSPSectionInfo != null && oSPSectionInfo.has("CollegeOrUniversity")) {
			newFundingSource.setCollegeOrUniversity(oSPSectionInfo.get(
					"CollegeOrUniversity").booleanValue());
		}
		if (oSPSectionInfo != null && oSPSectionInfo.has("LocalEntity")) {
			newFundingSource.setLocalEntity(oSPSectionInfo.get("LocalEntity")
					.booleanValue());
		}
		if (oSPSectionInfo != null && oSPSectionInfo.has("NonIdahoLocalEntity")) {
			newFundingSource.setNonIdahoLocalEntity(oSPSectionInfo.get(
					"NonIdahoLocalEntity").booleanValue());
		}
		if (oSPSectionInfo != null && oSPSectionInfo.has("TirbalGovernment")) {
			newFundingSource.setTirbalGovernment(oSPSectionInfo.get(
					"TirbalGovernment").booleanValue());
		}
		if (oSPSectionInfo != null && oSPSectionInfo.has("Foreign")) {
			newFundingSource.setForeign(oSPSectionInfo.get("Foreign")
					.booleanValue());
		}
		// Funding Source
		if (!existingProposal.getOspSectionInfo().getFundingSource()
				.equals(newFundingSource)) {
			existingProposal.getOspSectionInfo().setFundingSource(
					newFundingSource);
		}
	}

	/**
	 * @param existingProposal
	 * @param oSPSectionInfo
	 * @throws Exception
	 */
	public void getListAgency(Proposal existingProposal, JsonNode oSPSectionInfo)
			throws Exception {
		// List Agency
		if (oSPSectionInfo != null && oSPSectionInfo.has("ListAgency")) {
			String agencies = oSPSectionInfo.get("ListAgency").textValue()
					.trim().replaceAll("\\<[^>]*>", "");
			if (validateNotEmptyValue(agencies)) {
				if (!existingProposal.getOspSectionInfo().getListAgency()
						.equals(agencies)) {
					existingProposal.getOspSectionInfo()
							.setListAgency(agencies);
				}
			} else {
				throw new Exception("The Agency List can not be Empty");
			}
		}
	}

	/**
	 * @param existingProposal
	 * @param oSPSectionInfo
	 */
	public void getRecoveryDetails(Proposal existingProposal,
			JsonNode oSPSectionInfo) {
		Recovery newRecovery = new Recovery();
		if (oSPSectionInfo != null && oSPSectionInfo.has("FullRecovery")) {
			newRecovery.setFullRecovery(oSPSectionInfo.get("FullRecovery")
					.booleanValue());
		}
		if (oSPSectionInfo != null
				&& oSPSectionInfo.has("NoRecoveryNormalSponsorPolicy")) {
			newRecovery.setNoRecoveryNormalSponsorPolicy(oSPSectionInfo.get(
					"NoRecoveryNormalSponsorPolicy").booleanValue());
		}
		if (oSPSectionInfo != null
				&& oSPSectionInfo.has("NoRecoveryInstitutionalWaiver")) {
			newRecovery.setNoRecoveryInstitutionalWaiver(oSPSectionInfo.get(
					"NoRecoveryInstitutionalWaiver").booleanValue());
		}
		if (oSPSectionInfo != null
				&& oSPSectionInfo.has("LimitedRecoveryNormalSponsorPolicy")) {
			newRecovery.setLimitedRecoveryNormalSponsorPolicy(oSPSectionInfo
					.get("LimitedRecoveryNormalSponsorPolicy").booleanValue());
		}
		if (oSPSectionInfo != null
				&& oSPSectionInfo.has("LimitedRecoveryInstitutionalWaiver")) {
			newRecovery.setLimitedRecoveryInstitutionalWaiver(oSPSectionInfo
					.get("LimitedRecoveryInstitutionalWaiver").booleanValue());
		}
		// Recovery
		if (!existingProposal.getOspSectionInfo().getRecovery()
				.equals(newRecovery)) {
			existingProposal.getOspSectionInfo().setRecovery(newRecovery);
		}
	}

	/**
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
			newBaseInfo.setNotApplicable(oSPSectionInfo.get("NotApplicable")
					.booleanValue());
		}
		// Base Info
		if (!existingProposal.getOspSectionInfo().getBaseInfo()
				.equals(newBaseInfo)) {
			existingProposal.getOspSectionInfo().setBaseInfo(newBaseInfo);
		}
	}

	/**
	 * @param existingProposal
	 * @param newOSPSectionInfo
	 * @param oSPSectionInfo
	 * @throws Exception
	 */
	public void getSalaryDetails(Proposal existingProposal,
			OSPSectionInfo newOSPSectionInfo, JsonNode oSPSectionInfo)
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
		if (existingProposal.getOspSectionInfo().isPiSalaryIncluded() != newOSPSectionInfo
				.isPiSalaryIncluded()) {
			existingProposal.getOspSectionInfo().setPiSalaryIncluded(
					newOSPSectionInfo.isPiSalaryIncluded());
		}
		if (oSPSectionInfo != null && oSPSectionInfo.has("PISalary")) {
			// PI Salary
			String PISalary = oSPSectionInfo.get("PISalary").textValue().trim()
					.replaceAll("\\<[^>]*>", "");
			if (validateNotEmptyValue(PISalary)) {
				if (existingProposal.getOspSectionInfo().getPiSalary() != Double
						.parseDouble(PISalary)) {
					existingProposal.getOspSectionInfo().setPiSalary(
							Double.parseDouble(PISalary));
				}
			} else {
				throw new Exception("The PI Salary can not be Empty");
			}
		}
		if (oSPSectionInfo != null && oSPSectionInfo.has("PIFringe")) {
			// PI Fringe
			String PiFringe = oSPSectionInfo.get("PIFringe").textValue().trim()
					.replaceAll("\\<[^>]*>", "");
			if (validateNotEmptyValue(PiFringe)) {
				if (existingProposal.getOspSectionInfo().getPiFringe() != Double
						.parseDouble(PiFringe)) {
					existingProposal.getOspSectionInfo().setPiFringe(
							Double.parseDouble(PiFringe));
				}
			} else {
				throw new Exception("The PI Fringe can not be Empty");
			}
		}
		if (oSPSectionInfo != null && oSPSectionInfo.has("DepartmentId")) {
			// Department Id
			String departmentId = oSPSectionInfo.get("DepartmentId")
					.textValue().trim().replaceAll("\\<[^>]*>", "");
			if (validateNotEmptyValue(departmentId)) {
				if (!existingProposal.getOspSectionInfo().getDepartmentId()
						.equals(departmentId)) {
					existingProposal.getOspSectionInfo().setDepartmentId(
							departmentId);
				}
			} else {
				throw new Exception("The Department Id can not be Empty");
			}
		}
	}

	/**
	 * @param existingProposal
	 * @param oSPSectionInfo
	 * @throws Exception
	 */
	public void getProgramDetails(Proposal existingProposal,
			JsonNode oSPSectionInfo) throws Exception {
		// CFDA No
		if (oSPSectionInfo != null && oSPSectionInfo.has("CFDANo")) {
			String CFDANo = oSPSectionInfo.get("CFDANo").textValue().trim()
					.replaceAll("\\<[^>]*>", "");
			if (validateNotEmptyValue(CFDANo)) {
				if (!existingProposal.getOspSectionInfo().getCfdaNo()
						.equals(CFDANo)) {
					existingProposal.getOspSectionInfo().setCfdaNo(CFDANo);
				}
			} else {
				throw new Exception("The CFDA No can not be Empty");
			}
		}

		// Program No
		if (oSPSectionInfo != null && oSPSectionInfo.has("ProgramNo")) {
			String programNo = oSPSectionInfo.get("ProgramNo").textValue()
					.trim().replaceAll("\\<[^>]*>", "");
			if (validateNotEmptyValue(programNo)) {
				if (!existingProposal.getOspSectionInfo().getProgramNo()
						.equals(programNo)) {
					existingProposal.getOspSectionInfo()
							.setProgramNo(programNo);
				}
			} else {
				throw new Exception("The Program No can not be Empty");
			}
		}

		// Program Title
		if (oSPSectionInfo != null && oSPSectionInfo.has("ProgramTitle")) {
			String programTitle = oSPSectionInfo.get("ProgramTitle")
					.textValue().trim().replaceAll("\\<[^>]*>", "");
			if (validateNotEmptyValue(programTitle)) {
				if (!existingProposal.getOspSectionInfo().getProgramTitle()
						.equals(programTitle)) {
					existingProposal.getOspSectionInfo().setProgramTitle(
							programTitle);
				}
			} else {
				throw new Exception("The Program Title can not be Empty");
			}
		}
	}

	/**
	 * @param existingProposal
	 * @param oSPSectionInfo
	 */
	public void getInstitutionalCostDetails(Proposal existingProposal,
			JsonNode oSPSectionInfo) {
		BaseOptions newBaseOptions = new BaseOptions();
		if (oSPSectionInfo != null
				&& oSPSectionInfo.has("InstitutionalCostDocumented")) {
			switch (oSPSectionInfo.get("InstitutionalCostDocumented")
					.textValue()) {
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
		if (!existingProposal.getOspSectionInfo()
				.getInstitutionalCostDocumented().equals(newBaseOptions)) {
			existingProposal.getOspSectionInfo()
					.setInstitutionalCostDocumented(newBaseOptions);
		}
		newBaseOptions = new BaseOptions();
		if (oSPSectionInfo != null
				&& oSPSectionInfo.has("ThirdPartyCostDocumented")) {
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
		if (!existingProposal.getOspSectionInfo().getThirdPartyCostDocumented()
				.equals(newBaseOptions)) {
			existingProposal.getOspSectionInfo().setThirdPartyCostDocumented(
					newBaseOptions);
		}
	}

	/**
	 * @param existingProposal
	 * @param newOSPSectionInfo
	 * @param oSPSectionInfo
	 * @throws Exception
	 */
	public void getSubRecipientsDetails(Proposal existingProposal,
			OSPSectionInfo newOSPSectionInfo, JsonNode oSPSectionInfo)
			throws Exception {
		if (oSPSectionInfo != null
				&& oSPSectionInfo.has("IsAnticipatedSubRecipients")) {
			switch (oSPSectionInfo.get("IsAnticipatedSubRecipients")
					.textValue()) {
			case "1":
				newOSPSectionInfo.setAnticipatedSubRecipients(true);
				if (oSPSectionInfo != null
						&& oSPSectionInfo.has("AnticipatedSubRecipientsNames")) {
					final String anticipatedSubRecipients = oSPSectionInfo
							.get("AnticipatedSubRecipientsNames").textValue()
							.trim().replaceAll("\\<[^>]*>", "");
					if (validateNotEmptyValue(anticipatedSubRecipients)) {
						newOSPSectionInfo
								.setAnticipatedSubRecipientsNames(anticipatedSubRecipients);
					} else {
						throw new Exception(
								"The Anticipated SubRecipients Names can not be Empty");
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
			existingProposal.getOspSectionInfo().setAnticipatedSubRecipients(
					newOSPSectionInfo.isAnticipatedSubRecipients());
		}
		// Anticipated SubRecipients Names
		if (existingProposal.getOspSectionInfo()
				.getAnticipatedSubRecipientsNames() != null) {
			if (!existingProposal
					.getOspSectionInfo()
					.getAnticipatedSubRecipientsNames()
					.equals(newOSPSectionInfo
							.getAnticipatedSubRecipientsNames())) {
				existingProposal.getOspSectionInfo()
						.setAnticipatedSubRecipientsNames(
								newOSPSectionInfo
										.getAnticipatedSubRecipientsNames());
			}
		} else {
			existingProposal.getOspSectionInfo()
					.setAnticipatedSubRecipientsNames(
							newOSPSectionInfo
									.getAnticipatedSubRecipientsNames());
		}
	}

	/**
	 * @param existingProposal
	 * @param oSPSectionInfo
	 */
	public void getBasePIEligibilityOptions(Proposal existingProposal,
			JsonNode oSPSectionInfo) {
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
		if (!existingProposal.getOspSectionInfo().getPiEligibilityWaiver()
				.equals(newBasePIEligibilityOptions)) {
			existingProposal.getOspSectionInfo().setPiEligibilityWaiver(
					newBasePIEligibilityOptions);
		}
	}

	/**
	 * @param existingProposal
	 * @param oSPSectionInfo
	 */
	public void getConflictOfInterestForms(Proposal existingProposal,
			JsonNode oSPSectionInfo) {
		BaseOptions newBaseOptions = new BaseOptions();
		if (oSPSectionInfo != null
				&& oSPSectionInfo.has("ConflictOfInterestForms")) {
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
		if (!existingProposal.getOspSectionInfo().getConflictOfInterestForms()
				.equals(newBaseOptions)) {
			existingProposal.getOspSectionInfo().setConflictOfInterestForms(
					newBaseOptions);
		}
	}

	/**
	 * @param existingProposal
	 * @param oSPSectionInfo
	 */
	public void getExcludedPartyListChecked(Proposal existingProposal,
			JsonNode oSPSectionInfo) {
		BaseOptions newBaseOptions = new BaseOptions();
		if (oSPSectionInfo != null
				&& oSPSectionInfo.has("ExcludedPartyListChecked")) {
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
		if (!existingProposal.getOspSectionInfo().getExcludedPartyListChecked()
				.equals(newBaseOptions)) {
			existingProposal.getOspSectionInfo().setExcludedPartyListChecked(
					newBaseOptions);
		}
	}

	/**
	 * @param existingProposal
	 * @param proposalID
	 * @param signByAllUsersInfo
	 * @param authorUserName
	 * @param notificationMessage
	 * @param currentProposalRoles
	 * @return
	 */
	public String updateForProposalSave(Proposal existingProposal,
			String proposalID, SignatureByAllUsers signByAllUsersInfo,
			String authorUserName, String notificationMessage,
			List<String> currentProposalRoles) {
		// Change status to ready to submitted by PI
		if (proposalID.equals("0")) {
			notificationMessage = "Saved by " + authorUserName + ".";
			if (existingProposal.getInvestigatorInfo().getCo_pi().size() == 0) {
				existingProposal.setReadyForSubmissionByPI(true);
				existingProposal.getProposalStatus().clear();
				existingProposal.getProposalStatus().add(
						Status.READYFORSUBMITBYPI);
			}
		} else if (!proposalID.equals("0") && currentProposalRoles != null) {
			if ((currentProposalRoles.contains("PI") || (currentProposalRoles
					.contains("Co-PI") && !existingProposal
					.isReadyForSubmissionByPI()))
					&& existingProposal.getSubmittedByPI() == SubmitType.NOTSUBMITTED) {

				if (signByAllUsersInfo.isSignedByPI()
						&& signByAllUsersInfo.isSignedByAllCoPIs()) {
					existingProposal.setReadyForSubmissionByPI(true);

					existingProposal.getProposalStatus().clear();
					existingProposal.getProposalStatus().add(
							Status.READYFORSUBMITBYPI);
				} else {
					existingProposal.setReadyForSubmissionByPI(false);

					existingProposal.getProposalStatus().clear();
					existingProposal.getProposalStatus().add(
							Status.NOTSUBMITTEDBYPI);
				}

				notificationMessage = "Updated by " + authorUserName + ".";
			}
		}
		return notificationMessage;
	}

	/**
	 * @param existingProposal
	 * @param proposalInfo
	 * @throws Exception
	 */
	public void getOSPSectionInfo(Proposal existingProposal,
			JsonNode proposalInfo) throws Exception {
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
			getSalaryDetails(existingProposal, newOSPSectionInfo,
					oSPSectionInfo);
			getInstitutionalCostDetails(existingProposal, oSPSectionInfo);
			getSubRecipientsDetails(existingProposal, newOSPSectionInfo,
					oSPSectionInfo);
			getBasePIEligibilityOptions(existingProposal, oSPSectionInfo);
			getConflictOfInterestForms(existingProposal, oSPSectionInfo);
			getExcludedPartyListChecked(existingProposal, oSPSectionInfo);
		}
	}

	public EmailCommonInfo saveProposalWithObligations(
			List<ObligationResult> obligations) throws JsonProcessingException {
		EmailCommonInfo emailDetails = new EmailCommonInfo();
		getObligationsDetails(obligations, emailDetails);
		return emailDetails;
	}

	public EmailCommonInfo saveProposalWithoutObligations(String message,
			String proposalId, Proposal existingProposal, Proposal oldProposal,
			UserProfile authorProfile, Boolean irbApprovalRequired)
			throws IOException {
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
					emailDetails
							.setEmailSubject("The proposal has been created by: "
									+ authorUserName);
					emailDetails
							.setEmailBody("Hello User,<br/><br/>The proposal has been created by Admin.<br/><br/>Thank you, <br/> GPMS Team");
				} else if (!proposalId.equals("0")) {
					emailDetails
							.setEmailSubject("The proposal has been updated by: "
									+ authorUserName);
					emailDetails
							.setEmailBody("Hello User,<br/><br/>The proposal has been updated by Admin.<br/><br/>Thank you, <br/> GPMS Team");
				}
				break;
			default:
				break;
			}
		}
		emailDetails.setPiEmail(existingProposal.getInvestigatorInfo().getPi()
				.getUserRef().getWorkEmails().get(0));
		for (InvestigatorRefAndPosition copis : existingProposal
				.getInvestigatorInfo().getCo_pi()) {
			emailDetails.getEmaillist().add(
					copis.getUserRef().getWorkEmails().get(0));
		}
		for (InvestigatorRefAndPosition seniors : existingProposal
				.getInvestigatorInfo().getSeniorPersonnel()) {
			emailDetails.getEmaillist().add(
					seniors.getUserRef().getWorkEmails().get(0));
		}
		return emailDetails;
	}

	/**
	 * @param proposalId
	 * @param existingProposal
	 * @param oldProposal
	 * @param proposalInfo
	 * @return
	 * @throws IOException
	 * @throws JsonParseException
	 * @throws JsonMappingException
	 */
	public void getAppendixDetails(String proposalId,
			Proposal existingProposal, Proposal oldProposal,
			JsonNode proposalInfo) throws IOException, JsonParseException,
			JsonMappingException {
		ObjectMapper mapper = new ObjectMapper();
		// Appendix Info
		if (proposalInfo != null && proposalInfo.has("AppendixInfo")) {
			List<Appendix> appendixInfo = Arrays.asList(mapper.readValue(
					proposalInfo.get("AppendixInfo").toString(),
					Appendix[].class));
			if (appendixInfo.size() != 0) {
				String UPLOAD_PATH = new String();
				try {
					UPLOAD_PATH = this.getClass().getResource("/uploads")
							.toURI().getPath();
				} catch (URISyntaxException e) {
					e.printStackTrace();
				}
				List<String> existingFiles = new ArrayList<String>();
				if (!proposalId.equals("0")) {
					boolean alreadyExist = false;
					for (Appendix appendix : oldProposal.getAppendices()) {
						for (Appendix appendixObj : appendixInfo) {
							if (appendix.getFilename().equalsIgnoreCase(
									appendixObj.getFilename())
									&& appendix
											.getTitle()
											.equalsIgnoreCase(
													appendixObj
															.getTitle()
															.trim()
															.replaceAll(
																	"\\<[^>]*>",
																	""))) {
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
									Response.status(403)
											.entity(extension
													+ " is not allowed. Allowed extensions: jpg,png,gif,jpeg,bmp,png,pdf,doc,docx,xls,xlsx,txt")
											.build();
								}
							}
							long fileSize = file.length();
							if (verifyValidFileSize(fileSize)) {
								uploadFile.setFilesize(fileSize);
							} else {
								Response.status(403)
										.entity("The uploaded file is larger than 5MB")
										.build();
							}
							uploadFile.setFilepath("/uploads/" + fileName);
							String fileTitle = uploadFile.getTitle().trim()
									.replaceAll("\\<[^>]*>", "");
							if (validateNotEmptyValue(fileTitle)) {
								uploadFile.setTitle(fileTitle);
							} else {
								Response.status(403)
										.entity("The Uploaded File's Title can not be Empty")
										.build();
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
								Response.status(403)
										.entity(extension
												+ " is not allowed. Allowed extensions: jpg,png,gif,jpeg,bmp,png,pdf,doc,docx,xls,xlsx,txt")
										.build();
							}
						}
						long fileSize = file.length();
						if (verifyValidFileSize(fileSize)) {
							uploadFile.setFilesize(fileSize);
						} else {
							Response.status(403)
									.entity("The uploaded file is larger than 5MB")
									.build();
						}
						uploadFile.setFilesize(fileSize);
						uploadFile.setFilepath("/uploads/" + fileName);
						String fileTitle = uploadFile.getTitle().trim()
								.replaceAll("\\<[^>]*>", "");
						if (validateNotEmptyValue(fileTitle)) {
							uploadFile.setTitle(fileTitle);
						} else {
							Response.status(403)
									.entity("The Uploaded File's Title can not be Empty")
									.build();
						}
						existingProposal.getAppendices().add(uploadFile);
					}
				}
			} else {
				existingProposal.getAppendices().clear();
			}
		}
	}

	/**
	 * @param userInfo
	 * @param proposalId
	 * @param existingProposal
	 * @param signedByCurrentUser
	 * @param proposalInfo
	 * @return
	 * @throws ParseException
	 */
	public boolean getSignatureDetails(GPMSCommonInfo userInfo,
			String proposalId, Proposal existingProposal,
			boolean signedByCurrentUser, JsonNode proposalInfo)
			throws ParseException {
		if (proposalInfo != null && proposalInfo.has("SignatureInfo")) {
			String[] rows = proposalInfo.get("SignatureInfo").textValue()
					.split("#!#");
			List<SignatureInfo> newSignatureInfo = new ArrayList<SignatureInfo>();
			List<SignatureInfo> allSignatureInfo = new ArrayList<SignatureInfo>();
			List<SignatureInfo> removeSignatureInfo = new ArrayList<SignatureInfo>();
			DateFormat format = new SimpleDateFormat("yyyy/MM/dd hh:mm:ss a");
			for (String col : rows) {
				String[] cols = col.split("!#!");
				SignatureInfo signatureInfo = new SignatureInfo();
				signatureInfo.setUserProfileId(cols[0]);
				final String signatureText = cols[1]
						.replaceAll("\\<[^>]*>", "");
				if (validateNotEmptyValue(signatureText)) {
					signatureInfo.setSignature(signatureText);
				} else {
					Response.status(403)
							.entity("The Signature can not be Empty").build();
				}
				final String signedDate = cols[2].trim().replaceAll(
						"\\<[^>]*>", "");
				if (validateNotEmptyValue(signedDate)) {
					signatureInfo.setSignedDate(format.parse(signedDate));
				} else {
					Response.status(403)
							.entity("The Signed Date can not be Empty").build();
				}
				final String noteText = cols[3].replaceAll("\\<[^>]*>", "");
				if (validateNotEmptyValue(noteText)) {
					signatureInfo.setNote(noteText);
				} else {
					Response.status(403).entity("The Note can not be Empty")
							.build();
				}
				signatureInfo.setFullName(cols[4]);
				signatureInfo.setPositionTitle(cols[5]);
				signatureInfo.setDelegated(Boolean.parseBoolean(cols[6]));
				allSignatureInfo.add(signatureInfo);
				if (!proposalId.equals("0")) {
					boolean alreadyExist = false;
					for (SignatureInfo sign : existingProposal
							.getSignatureInfo()) {
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
				if (!existingProposal.getSignatureInfo().equals(
						allSignatureInfo)) {
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
					signedByCurrentUser = true;
					break;
				}
			}
		}
		return signedByCurrentUser;
	}

	/**
	 * @param newComplianceInfo
	 * @param irbApprovalRequired
	 * @param complianceInfo
	 * @return
	 */
	public Boolean getInvolveUseOfHumanSubjectsInfo(
			ComplianceInfo newComplianceInfo, Boolean irbApprovalRequired,
			JsonNode complianceInfo) {
		if (complianceInfo != null
				&& complianceInfo.has("InvolveUseOfHumanSubjects")) {
			switch (complianceInfo.get("InvolveUseOfHumanSubjects").textValue()) {
			case "1":
				newComplianceInfo.setInvolveUseOfHumanSubjects(true);
				irbApprovalRequired = true;
				if (complianceInfo != null && complianceInfo.has("IRBPending")) {
					switch (complianceInfo.get("IRBPending").textValue()) {
					case "1":
						newComplianceInfo.setIrbPending(false);
						if (complianceInfo != null && complianceInfo.has("IRB")) {
							final String IRBNo = complianceInfo.get("IRB")
									.textValue().trim()
									.replaceAll("\\<[^>]*>", "");
							if (validateNotEmptyValue(IRBNo)) {
								newComplianceInfo.setIrb(IRBNo);
							} else {
								Response.status(403)
										.entity("The IRB # can not be Empty")
										.build();
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

	/**
	 * @param newComplianceInfo
	 * @param irbApprovalRequired
	 * @param complianceInfo
	 * @return
	 */
	public Boolean getInvolveUseOfVertebrateAnimalsInfo(
			ComplianceInfo newComplianceInfo, Boolean irbApprovalRequired,
			JsonNode complianceInfo) {
		if (complianceInfo != null
				&& complianceInfo.has("InvolveUseOfVertebrateAnimals")) {
			switch (complianceInfo.get("InvolveUseOfVertebrateAnimals")
					.textValue()) {
			case "1":
				newComplianceInfo.setInvolveUseOfVertebrateAnimals(true);
				irbApprovalRequired = true;
				if (complianceInfo != null
						&& complianceInfo.has("IACUCPending")) {
					switch (complianceInfo.get("IACUCPending").textValue()) {
					case "1":
						newComplianceInfo.setIacucPending(false);
						if (complianceInfo != null
								&& complianceInfo.has("IACUC")) {
							final String IACUCNo = complianceInfo.get("IACUC")
									.textValue().trim()
									.replaceAll("\\<[^>]*>", "");
							if (validateNotEmptyValue(IACUCNo)) {
								newComplianceInfo.setIacuc(IACUCNo);
							} else {
								Response.status(403)
										.entity("The IACUC # can not be Empty")
										.build();
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

	/**
	 * @param newComplianceInfo
	 * @param irbApprovalRequired
	 * @param complianceInfo
	 * @return
	 */
	public Boolean getInvolveBiosafetyConcernsInfo(
			ComplianceInfo newComplianceInfo, Boolean irbApprovalRequired,
			JsonNode complianceInfo) {
		if (complianceInfo != null
				&& complianceInfo.has("InvolveBiosafetyConcerns")) {
			switch (complianceInfo.get("InvolveBiosafetyConcerns").textValue()) {
			case "1":
				newComplianceInfo.setInvolveBiosafetyConcerns(true);
				irbApprovalRequired = true;
				if (complianceInfo != null && complianceInfo.has("IBCPending")) {
					switch (complianceInfo.get("IBCPending").textValue()) {
					case "1":
						newComplianceInfo.setIbcPending(false);
						if (complianceInfo != null && complianceInfo.has("IBC")) {
							final String IBCNo = complianceInfo.get("IBC")
									.textValue().trim()
									.replaceAll("\\<[^>]*>", "");

							if (validateNotEmptyValue(IBCNo)) {
								newComplianceInfo.setIbc(IBCNo);
							} else {
								Response.status(403)
										.entity("The IBC # can not be Empty")
										.build();
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

	/**
	 * @param newComplianceInfo
	 * @param irbApprovalRequired
	 * @param complianceInfo
	 * @return
	 */
	public Boolean getInvolveEnvironmentalHealthAndSafetyConcernsInfo(
			ComplianceInfo newComplianceInfo, Boolean irbApprovalRequired,
			JsonNode complianceInfo) {
		if (complianceInfo != null
				&& complianceInfo
						.has("InvolveEnvironmentalHealthAndSafetyConcerns")) {
			switch (complianceInfo.get(
					"InvolveEnvironmentalHealthAndSafetyConcerns").textValue()) {
			case "1":
				newComplianceInfo
						.setInvolveEnvironmentalHealthAndSafetyConcerns(true);
				irbApprovalRequired = true;
				break;
			case "2":
				newComplianceInfo
						.setInvolveEnvironmentalHealthAndSafetyConcerns(false);
				break;
			default:
				break;
			}
		}
		return irbApprovalRequired;
	}

	/**
	 * @param proposalId
	 * @param existingProposal
	 * @param proposalInfo
	 * @return
	 */
	public Boolean getComplianceDetails(String proposalId,
			Proposal existingProposal, JsonNode proposalInfo) {
		ComplianceInfo newComplianceInfo = new ComplianceInfo();
		Boolean irbApprovalRequired = false;
		if (proposalInfo != null && proposalInfo.has("ComplianceInfo")) {
			JsonNode complianceInfo = proposalInfo.get("ComplianceInfo");
			irbApprovalRequired = getInvolveUseOfHumanSubjectsInfo(
					newComplianceInfo, irbApprovalRequired, complianceInfo);
			irbApprovalRequired = getInvolveUseOfVertebrateAnimalsInfo(
					newComplianceInfo, irbApprovalRequired, complianceInfo);
			irbApprovalRequired = getInvolveBiosafetyConcernsInfo(
					newComplianceInfo, irbApprovalRequired, complianceInfo);
			irbApprovalRequired = getInvolveEnvironmentalHealthAndSafetyConcernsInfo(
					newComplianceInfo, irbApprovalRequired, complianceInfo);
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

	/**
	 * @param contentProfile
	 * @param requiredPISign
	 * @param requiredCoPISigns
	 * @param requiredChairSigns
	 * @param requiredBusinessManagerSigns
	 * @param requiredDeanSigns
	 * @param requiredIRBSigns
	 * @param requiredResearchAdminSigns
	 * @param requiredResearchDirectorSigns
	 * @param signedByCurrentUser
	 * @param signByAllUsersInfo
	 * @param existingPISign
	 * @param existingCoPISigns
	 * @param existingChairSigns
	 * @param existingBusinessManagerSigns
	 * @param existingDeanSigns
	 * @param existingIRBSigns
	 * @param existingResearchAdminSigns
	 * @param existingResearchDirectorSigns
	 */
	public void checkForSignedByAllUsers(StringBuffer contentProfile,
			List<String> requiredPISign, List<String> requiredCoPISigns,
			List<String> requiredChairSigns,
			List<String> requiredBusinessManagerSigns,
			List<String> requiredDeanSigns, List<String> requiredIRBSigns,
			List<String> requiredResearchAdminSigns,
			List<String> requiredResearchDirectorSigns,
			boolean signedByCurrentUser,
			SignatureByAllUsers signByAllUsersInfo,
			List<String> existingPISign, List<String> existingCoPISigns,
			List<String> existingChairSigns,
			List<String> existingBusinessManagerSigns,
			List<String> existingDeanSigns, List<String> existingIRBSigns,
			List<String> existingResearchAdminSigns,
			List<String> existingResearchDirectorSigns) {
		boolean signedByPI = false;
		boolean signedByAllCoPIs = false;
		boolean signedByAllChairs = false;
		boolean signedByAllBusinessManagers = false;
		boolean signedByAllDeans = false;
		boolean signedByAllIRBs = false;
		boolean signedByAllResearchAdmins = false;
		boolean signedByAllResearchDirectors = false;
		signedByPI = existingPISign.containsAll(requiredPISign);
		signedByAllCoPIs = existingCoPISigns.containsAll(requiredCoPISigns);
		signedByAllChairs = existingChairSigns.containsAll(requiredChairSigns);
		signedByAllBusinessManagers = existingBusinessManagerSigns
				.containsAll(requiredBusinessManagerSigns);
		signedByAllDeans = existingDeanSigns.containsAll(requiredDeanSigns);
		signedByAllIRBs = existingIRBSigns.containsAll(requiredIRBSigns);
		signedByAllResearchAdmins = existingResearchAdminSigns
				.containsAll(requiredResearchAdminSigns);
		signedByAllResearchDirectors = existingResearchDirectorSigns
				.containsAll(requiredResearchDirectorSigns);
		contentProfile.append("<ak:signedByCurrentUser>");
		contentProfile.append(signedByCurrentUser);
		contentProfile.append("</ak:signedByCurrentUser>");
		signByAllUsersInfo.setSignedByPI(signedByPI);
		contentProfile.append("<ak:signedByPI>");
		contentProfile.append(signedByPI);
		contentProfile.append("</ak:signedByPI>");
		signByAllUsersInfo.setSignedByAllCoPIs(signedByAllCoPIs);
		contentProfile.append("<ak:signedByAllCoPIs>");
		contentProfile.append(signedByAllCoPIs);
		contentProfile.append("</ak:signedByAllCoPIs>");
		signByAllUsersInfo.setSignedByAllChairs(signedByAllChairs);
		contentProfile.append("<ak:signedByAllChairs>");
		contentProfile.append(signedByAllChairs);
		contentProfile.append("</ak:signedByAllChairs>");
		signByAllUsersInfo
				.setSignedByAllBusinessManagers(signedByAllBusinessManagers);
		contentProfile.append("<ak:signedByAllBusinessManagers>");
		contentProfile.append(signedByAllBusinessManagers);
		contentProfile.append("</ak:signedByAllBusinessManagers>");
		signByAllUsersInfo.setSignedByAllDeans(signedByAllDeans);
		contentProfile.append("<ak:signedByAllDeans>");
		contentProfile.append(signedByAllDeans);
		contentProfile.append("</ak:signedByAllDeans>");
		signByAllUsersInfo.setSignedByAllIRBs(signedByAllIRBs);
		contentProfile.append("<ak:signedByAllIRBs>");
		contentProfile.append(signedByAllIRBs);
		contentProfile.append("</ak:signedByAllIRBs>");
		signByAllUsersInfo
				.setSignedByAllResearchAdmins(signedByAllResearchAdmins);
		contentProfile.append("<ak:signedByAllResearchAdmins>");
		contentProfile.append(signedByAllResearchAdmins);
		contentProfile.append("</ak:signedByAllResearchAdmins>");
		signByAllUsersInfo
				.setSignedByAllResearchDirectors(signedByAllResearchDirectors);
		contentProfile.append("<ak:signedByAllResearchDirectors>");
		contentProfile.append(signedByAllResearchDirectors);
		contentProfile.append("</ak:signedByAllResearchDirectors>");
	}

	/***
	 * 
	 * @param contentProfile
	 * @param existingProposal
	 * @param requiredPISign
	 * @param requiredCoPISigns
	 * @param requiredChairSigns
	 * @param requiredBusinessManagerSigns
	 * @param requiredDeanSigns
	 * @param requiredIRBSigns
	 * @param requiredResearchAdminSigns
	 * @param requiredResearchDirectorSigns
	 * @param signedByCurrentUser
	 * @param signByAllUsersInfo
	 */
	public void getExistingSignaturesForProposal(StringBuffer contentProfile,
			Proposal existingProposal, List<String> requiredPISign,
			List<String> requiredCoPISigns, List<String> requiredChairSigns,
			List<String> requiredBusinessManagerSigns,
			List<String> requiredDeanSigns, List<String> requiredIRBSigns,
			List<String> requiredResearchAdminSigns,
			List<String> requiredResearchDirectorSigns,
			boolean signedByCurrentUser, SignatureByAllUsers signByAllUsersInfo) {
		List<String> existingPISign = new ArrayList<String>();
		List<String> existingCoPISigns = new ArrayList<String>();
		List<String> existingChairSigns = new ArrayList<String>();
		List<String> existingBusinessManagerSigns = new ArrayList<String>();
		List<String> existingDeanSigns = new ArrayList<String>();
		List<String> existingIRBSigns = new ArrayList<String>();
		List<String> existingResearchAdminSigns = new ArrayList<String>();
		List<String> existingResearchDirectorSigns = new ArrayList<String>();
		for (SignatureInfo sign : existingProposal.getSignatureInfo()) {
			if (sign.getPositionTitle().equals("PI")) {
				existingPISign.add(sign.getUserProfileId());
			} else if (sign.getPositionTitle().equals("Co-PI")) {
				existingCoPISigns.add(sign.getUserProfileId());
			} else if (sign.getPositionTitle().equals("Department Chair")) {
				existingChairSigns.add(sign.getUserProfileId());
			} else if (sign.getPositionTitle().equals("Business Manager")) {
				existingBusinessManagerSigns.add(sign.getUserProfileId());
			} else if (sign.getPositionTitle().equals("Dean")) {
				existingDeanSigns.add(sign.getUserProfileId());
			} else if (sign.getPositionTitle().equals("IRB")) {
				existingIRBSigns.add(sign.getUserProfileId());
			} else if (sign.getPositionTitle().equals(
					"University Research Administrator")) {
				existingResearchAdminSigns.add(sign.getUserProfileId());
			} else if (sign.getPositionTitle().equals(
					"University Research Director")) {
				existingResearchDirectorSigns.add(sign.getUserProfileId());
			}
		}

		checkForSignedByAllUsers(contentProfile, requiredPISign,
				requiredCoPISigns, requiredChairSigns,
				requiredBusinessManagerSigns, requiredDeanSigns,
				requiredIRBSigns, requiredResearchAdminSigns,
				requiredResearchDirectorSigns, signedByCurrentUser,
				signByAllUsersInfo, existingPISign, existingCoPISigns,
				existingChairSigns, existingBusinessManagerSigns,
				existingDeanSigns, existingIRBSigns,
				existingResearchAdminSigns, existingResearchDirectorSigns);
	}

	/**
	 * @param existingProposal
	 * @param contentProfile
	 * @param signatures
	 * @param requiredPISign
	 * @param requiredCoPISigns
	 * @param requiredChairSigns
	 * @param requiredBusinessManagerSigns
	 * @param requiredDeanSigns
	 * @param requiredIRBSigns
	 * @param requiredResearchAdminSigns
	 * @param requiredResearchDirectorSigns
	 */
	public void generateContentProfileForAllUsers(Proposal existingProposal,
			StringBuffer contentProfile, List<SignatureUserInfo> signatures,
			List<String> requiredPISign, List<String> requiredCoPISigns,
			List<String> requiredChairSigns,
			List<String> requiredBusinessManagerSigns,
			List<String> requiredDeanSigns, List<String> requiredIRBSigns,
			List<String> requiredResearchAdminSigns,
			List<String> requiredResearchDirectorSigns) {
		if (!existingProposal.getInvestigatorInfo().getPi().getUserRef()
				.isDeleted()) {
			generatePIContentProfile(existingProposal, contentProfile);
			requiredPISign.add(existingProposal.getInvestigatorInfo().getPi()
					.getUserProfileId());
		}
		for (InvestigatorRefAndPosition copis : existingProposal
				.getInvestigatorInfo().getCo_pi()) {
			if (!copis.getUserRef().isDeleted()) {
				generateCoPIContentProfile(contentProfile, copis);
				requiredCoPISigns.add(copis.getUserProfileId());
			}
		}
		for (InvestigatorRefAndPosition seniors : existingProposal
				.getInvestigatorInfo().getSeniorPersonnel()) {
			if (!seniors.getUserRef().isDeleted()) {
				generateSeniorContentProfile(contentProfile, seniors);
			}
		}
		for (SignatureUserInfo signatureInfo : signatures) {
			switch (signatureInfo.getPositionTitle()) {
			case "Department Chair":
				generateChairContentProfile(contentProfile, signatureInfo);
				requiredChairSigns.add(signatureInfo.getUserProfileId());
				break;
			case "Business Manager":
				generateManagerContentProfile(contentProfile, signatureInfo);
				requiredBusinessManagerSigns.add(signatureInfo
						.getUserProfileId());
				break;
			case "Dean":
				generateDeanContentProfile(contentProfile, signatureInfo);
				requiredDeanSigns.add(signatureInfo.getUserProfileId());
				break;
			case "IRB":
				generateIRBContentProfile(contentProfile, signatureInfo);
				requiredIRBSigns.add(signatureInfo.getUserProfileId());
				break;
			case "University Research Administrator":
				generateResearchAdminContentProfile(contentProfile,
						signatureInfo);
				requiredResearchAdminSigns
						.add(signatureInfo.getUserProfileId());
				break;
			case "University Research Director":
				generateDirectorContentProfile(contentProfile, signatureInfo);
				requiredResearchDirectorSigns.add(signatureInfo
						.getUserProfileId());
				break;
			default:
				break;
			}
		}
	}

	/**
	 * @param authorProfile
	 * @param authorFullName
	 * @param proposalId
	 * @param existingProposal
	 * @param signedByCurrentUser
	 * @param contentProfile
	 * @param signatures
	 * @param signByAllUsersInfo
	 */
	public void generateProposalContentProfile(UserProfile authorProfile,
			String authorFullName, String proposalId,
			Proposal existingProposal, boolean signedByCurrentUser,
			StringBuffer contentProfile, List<SignatureUserInfo> signatures,
			SignatureByAllUsers signByAllUsersInfo) {
		contentProfile.append("<Content>");
		contentProfile.append("<ak:record xmlns:ak=\"http://akpower.org\">");
		genearteProposalInfoContentProfile(proposalId, existingProposal,
				contentProfile);
		generateAuthorContentProfile(contentProfile, authorProfile,
				authorFullName);
		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX");
		contentProfile.append("<ak:currentdatetime>");
		contentProfile.append(dateFormat.format(new Date()));
		contentProfile.append("</ak:currentdatetime>");
		List<String> requiredPISign = new ArrayList<String>();
		List<String> requiredCoPISigns = new ArrayList<String>();
		List<String> requiredChairSigns = new ArrayList<String>();
		List<String> requiredBusinessManagerSigns = new ArrayList<String>();
		List<String> requiredDeanSigns = new ArrayList<String>();
		List<String> requiredIRBSigns = new ArrayList<String>();
		List<String> requiredResearchAdminSigns = new ArrayList<String>();
		List<String> requiredResearchDirectorSigns = new ArrayList<String>();
		generateContentProfileForAllUsers(existingProposal, contentProfile,
				signatures, requiredPISign, requiredCoPISigns,
				requiredChairSigns, requiredBusinessManagerSigns,
				requiredDeanSigns, requiredIRBSigns,
				requiredResearchAdminSigns, requiredResearchDirectorSigns);
		getExistingSignaturesForProposal(contentProfile, existingProposal,
				requiredPISign, requiredCoPISigns, requiredChairSigns,
				requiredBusinessManagerSigns, requiredDeanSigns,
				requiredIRBSigns, requiredResearchAdminSigns,
				requiredResearchDirectorSigns, signedByCurrentUser,
				signByAllUsersInfo);
		contentProfile.append("</ak:proposal>");
		contentProfile.append("</ak:record>");
		contentProfile.append("</Content>");
	}

	/**
	 * @param authorProfile
	 * @param authorFullName
	 * @param proposalId
	 * @param existingProposal
	 * @param signedByCurrentUser
	 * @param contentProfile
	 * @param irbApprovalRequired
	 * @param signatures
	 * @param signByAllUsersInfo
	 * @return
	 */
	public List<SignatureUserInfo> generateProposalContentProfile(
			UserProfile authorProfile, String authorFullName,
			String proposalId, Proposal existingProposal,
			boolean signedByCurrentUser, StringBuffer contentProfile,
			Boolean irbApprovalRequired, List<SignatureUserInfo> signatures,
			SignatureByAllUsers signByAllUsersInfo) {
		if (!proposalId.equals("0")) {
			ObjectId id = new ObjectId(proposalId);
			signatures = findSignaturesExceptInvestigator(id,
					irbApprovalRequired);
			generateProposalContentProfile(authorProfile, authorFullName,
					proposalId, existingProposal, signedByCurrentUser,
					contentProfile, signatures, signByAllUsersInfo);
		} else {
			generateDefaultProposalContentProfile(authorProfile,
					authorFullName, proposalId, existingProposal,
					signedByCurrentUser, contentProfile);
		}
		contentProfile
				.append("<Attribute AttributeId=\"urn:oasis:names:tc:xacml:3.0:content-selector\" IncludeInResult=\"false\">");
		contentProfile
				.append("<AttributeValue XPathCategory=\"urn:oasis:names:tc:xacml:3.0:attribute-category:resource\" DataType=\"urn:oasis:names:tc:xacml:3.0:data-type:xpathExpression\">//ak:record/ak:proposal</AttributeValue>");
		contentProfile.append("</Attribute>");
		return signatures;
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
	public boolean updateWithdrawnStatus(String proposalId,
			String proposalUserTitle, Proposal existingProposal,
			UserProfile authorProfile, String authorUserName,
			EmailCommonInfo emailDetails) throws JsonProcessingException {
		boolean isStatusUpdated = false;
		if (!proposalId.equals("0")) {
			if (existingProposal.getResearchAdministratorWithdraw() == WithdrawType.NOTWITHDRAWN
					&& existingProposal.getResearchAdministratorApproval() == ApprovalType.READYFORAPPROVAL
					&& proposalUserTitle
							.equals("University Research Administrator")) {
				existingProposal
						.setResearchAdministratorWithdraw(WithdrawType.WITHDRAWN);
				existingProposal
						.setResearchAdministratorApproval(ApprovalType.NOTREADYFORAPPROVAL);
				existingProposal.getProposalStatus().clear();
				existingProposal.getProposalStatus().add(
						Status.WITHDRAWBYRESEARCHADMIN);
				isStatusUpdated = updateProposalStatus(existingProposal,
						authorProfile);
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
	public boolean updateArchivedStatus(String proposalId,
			String proposalUserTitle, Proposal existingProposal,
			UserProfile authorProfile, String authorUserName,
			EmailCommonInfo emailDetails) throws JsonProcessingException {
		boolean isStatusUpdated = false;
		if (!proposalId.equals("0")) {
			if (existingProposal.getResearchDirectorArchived() == ArchiveType.NOTARCHIVED
					&& existingProposal.getResearchAdministratorSubmission() == SubmitType.SUBMITTED
					&& proposalUserTitle.equals("University Research Director")) {
				existingProposal
						.setResearchDirectorArchived(ArchiveType.ARCHIVED);
				existingProposal
						.setResearchDirectorApproval(ApprovalType.NOTREADYFORAPPROVAL);
				existingProposal.getProposalStatus().clear();
				existingProposal.getProposalStatus().add(
						Status.ARCHIVEDBYRESEARCHDIRECTOR);
				isStatusUpdated = updateProposalStatus(existingProposal,
						authorProfile);
			}
		}
		return isStatusUpdated;
	}

	/**
	 * Gets Obligations Details with all Email Information
	 * 
	 * @param obligations
	 * @param emailDetails
	 * @return
	 */
	public Response getObligationsDetails(List<ObligationResult> obligations,
			EmailCommonInfo emailDetails) {
		if (obligations.size() > 0) {
			List<ObligationResult> preObligations = new ArrayList<ObligationResult>();
			List<ObligationResult> postObligations = new ArrayList<ObligationResult>();
			List<ObligationResult> ongoingObligations = new ArrayList<ObligationResult>();
			for (ObligationResult obligation : obligations) {
				categorizeObligationTypes(preObligations, postObligations,
						ongoingObligations, obligation);
			}
			// Performs Preobligations
			Boolean preCondition = true;
			String alertMessage = new String();
			if (preObligations.size() != 0) {
				preCondition = false;
				System.out
						.println("\n======================== Printing Obligations ====================");
				for (ObligationResult obligation : preObligations) {
					if (obligation instanceof org.wso2.balana.xacml3.Obligation) {
						List<AttributeAssignment> assignments = ((org.wso2.balana.xacml3.Obligation) obligation)
								.getAssignments();
						String obligationType = "preobligation";
						for (AttributeAssignment assignment : assignments) {
							switch (assignment.getAttributeId().toString()) {
							case "signedByCurrentUser":
								preCondition = Boolean.parseBoolean(assignment
										.getContent());
								break;
							case "alertMessage":
								alertMessage = assignment.getContent();
								break;
							default:
								break;
							}
						}
						System.out.println(obligationType + " is RUNNING");
						if (!preCondition) {
							break;
						}
					}
				}
			}

			if (preCondition) {
				// Performs Postobligations
				for (ObligationResult obligation : postObligations) {
					if (obligation instanceof org.wso2.balana.xacml3.Obligation) {
						List<AttributeAssignment> assignments = ((org.wso2.balana.xacml3.Obligation) obligation)
								.getAssignments();
						String obligationType = "postobligation";
						for (AttributeAssignment assignment : assignments) {
							switch (assignment.getAttributeId().toString()) {
							case "authorName":
								emailDetails.setAuthorName(assignment
										.getContent());
								break;
							case "emailSubject":
								emailDetails.setEmailSubject(assignment
										.getContent());
								break;
							case "emailBody":
								emailDetails.setEmailBody(assignment
										.getContent());
								break;
							case "piEmail":
								emailDetails
										.setPiEmail(assignment.getContent());
								break;
							case "copisEmail":
							case "seniorsEmail":
							case "chairsEmail":
							case "managersEmail":
							case "deansEmail":
							case "irbsEmail":
							case "administratorsEmail":
							case "directorsEmail":
								if (!assignment.getContent().equals("")) {
									emailDetails.getEmaillist().add(
											assignment.getContent());
								}
								break;
							default:
								break;
							}
						}
						System.out.println(obligationType + " is RUNNING");
					}
				}
			} else {
				return Response.status(403).type(MediaType.APPLICATION_JSON)
						.entity(alertMessage).build();
			}
		}
		return Response
				.status(403)
				.entity("{\"error\": \"Error while geting Obligations Information!\", \"status\": \"FAIL\"}")
				.build();
	}

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
	public String updateProposalStatusWithObligations(String proposalId,
			String buttonType, String proposalUserTitle,
			Proposal existingProposal, UserProfile authorProfile,
			String authorUserName, List<ObligationResult> obligations)
			throws JsonProcessingException {
		boolean isStatusUpdated = false;
		String changeDone = new String();
		EmailCommonInfo emailDetails = new EmailCommonInfo();
		getObligationsDetails(obligations, emailDetails);
		if (buttonType != null && buttonType != "") {
			switch (buttonType) {
			case "Withdraw":
				isStatusUpdated = updateWithdrawnStatus(proposalId,
						proposalUserTitle, existingProposal, authorProfile,
						authorUserName, emailDetails);
				changeDone = "Withdrawn";
				break;
			case "Archive":
				isStatusUpdated = updateArchivedStatus(proposalId,
						proposalUserTitle, existingProposal, authorProfile,
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
				EmailUtil emailUtil = new EmailUtil();
				emailUtil.sendMailMultipleUsersWithoutAuth(piEmail, emaillist,
						emailSubject + authorName, emailBody);
			}
		}
		return changeDone;
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
	public String exportToExcelFile(List<ProposalInfo> proposals,
			List<AuditLogInfo> proposalAuditLogs) throws URISyntaxException,
			JsonProcessingException {
		ObjectMapper mapper = new ObjectMapper();
		String filename = new String();
		Xcelite xcelite = new Xcelite();
		if (proposals != null) {
			XceliteSheet sheet = xcelite.createSheet("Proposals");
			SheetWriter<ProposalInfo> writer = sheet
					.getBeanWriter(ProposalInfo.class);
			writer.write(proposals);
		} else {
			XceliteSheet sheet = xcelite.createSheet("AuditLogs");
			SheetWriter<AuditLogInfo> writer = sheet
					.getBeanWriter(AuditLogInfo.class);
			writer.write(proposalAuditLogs);
		}
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy_MM_dd");
		Date date = new Date();
		String fileName = String.format(
				"%s.%s",
				RandomStringUtils.randomAlphanumeric(8) + "_"
						+ dateFormat.format(date), "xlsx");
		String downloadLocation = this.getClass().getResource("/uploads")
				.toURI().getPath();
		xcelite.write(new File(downloadLocation + fileName));
		filename = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(
				fileName);
		return filename;
	}

	/**
	 * @param attrMap
	 * @param actionMap
	 * @param contentProfile
	 * @return
	 */
	public List<String> generateMDPDecision(
			HashMap<String, Multimap<String, String>> attrMap,
			Multimap<String, String> actionMap, StringBuffer contentProfile) {
		BalanaConnector ac = new BalanaConnector();
		List<String> attributeValue = Arrays.asList("Save", "Submit",
				"Approve", "Disapprove", "Withdraw", "Archive", "Delete");
		for (String action : attributeValue) {
			actionMap.put("proposal.action", action);
			attrMap.put("Action", actionMap);
		}
		Set<AbstractResult> results = ac.getXACMLdecisionForMDPWithProfile(
				attrMap, contentProfile);
		List<String> actions = new ArrayList<String>();
		for (AbstractResult result : results) {
			if (AbstractResult.DECISION_PERMIT == result.getDecision()) {
				Set<Attributes> attributesSet = ((Result) result)
						.getAttributes();
				for (Attributes attributes : attributesSet) {
					for (Attribute attribute : attributes.getAttributes()) {
						actions.add(attribute.getValue().encode());
					}
				}
			}
		}
		return actions;
	}

	/**
	 * Generates Author Content Profile
	 * 
	 * @param contentProfile
	 * @param authorProfile
	 * @param authorFullName
	 */
	public void generateAuthorContentProfile(StringBuffer contentProfile,
			UserProfile authorProfile, String authorFullName) {
		contentProfile.append("<ak:authorprofile>");
		contentProfile.append("<ak:fullname>");
		contentProfile.append(authorFullName);
		contentProfile.append("</ak:fullname>");
		contentProfile.append("<ak:userid>");
		contentProfile.append(authorProfile.getId().toString());
		contentProfile.append("</ak:userid>");
		contentProfile.append("</ak:authorprofile>");
	}

	/**
	 * @param contentProfile
	 * @param signatureInfo
	 */
	public void generateDirectorContentProfile(StringBuffer contentProfile,
			SignatureUserInfo signatureInfo) {
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

	/**
	 * @param contentProfile
	 * @param signatureInfo
	 */
	public void generateResearchAdminContentProfile(
			StringBuffer contentProfile, SignatureUserInfo signatureInfo) {
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

	/**
	 * @param contentProfile
	 * @param signatureInfo
	 */
	public void generateIRBContentProfile(StringBuffer contentProfile,
			SignatureUserInfo signatureInfo) {
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

	/**
	 * @param contentProfile
	 * @param signatureInfo
	 */
	public void generateDeanContentProfile(StringBuffer contentProfile,
			SignatureUserInfo signatureInfo) {
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

	/**
	 * @param contentProfile
	 * @param signatureInfo
	 */
	public void generateManagerContentProfile(StringBuffer contentProfile,
			SignatureUserInfo signatureInfo) {
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

	/**
	 * @param contentProfile
	 * @param signatureInfo
	 */
	public void generateChairContentProfile(StringBuffer contentProfile,
			SignatureUserInfo signatureInfo) {
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

	/**
	 * @param contentProfile
	 * @param seniors
	 */
	public void generateSeniorContentProfile(StringBuffer contentProfile,
			InvestigatorRefAndPosition seniors) {
		contentProfile.append("<ak:senior>");
		contentProfile.append("<ak:fullname>");
		contentProfile.append(seniors.getUserRef().getFullName());
		contentProfile.append("</ak:fullname>");
		contentProfile.append("<ak:workemail>");
		contentProfile.append(seniors.getUserRef().getWorkEmails().get(0));
		contentProfile.append("</ak:workemail>");
		contentProfile.append("<ak:userid>");
		contentProfile.append(seniors.getUserProfileId());
		contentProfile.append("</ak:userid>");
		contentProfile.append("</ak:senior>");
	}

	/**
	 * @param contentProfile
	 * @param copis
	 */
	public void generateCoPIContentProfile(StringBuffer contentProfile,
			InvestigatorRefAndPosition copis) {
		contentProfile.append("<ak:copi>");
		contentProfile.append("<ak:fullname>");
		contentProfile.append(copis.getUserRef().getFullName());
		contentProfile.append("</ak:fullname>");
		contentProfile.append("<ak:workemail>");
		contentProfile.append(copis.getUserRef().getWorkEmails().get(0));
		contentProfile.append("</ak:workemail>");
		contentProfile.append("<ak:userid>");
		contentProfile.append(copis.getUserProfileId());
		contentProfile.append("</ak:userid>");
		contentProfile.append("</ak:copi>");
	}

	/**
	 * @param existingProposal
	 * @param contentProfile
	 */
	public void generatePIContentProfile(Proposal existingProposal,
			StringBuffer contentProfile) {
		contentProfile.append("<ak:pi>");
		contentProfile.append("<ak:fullname>");
		contentProfile.append(existingProposal.getInvestigatorInfo().getPi()
				.getUserRef().getFullName());
		contentProfile.append("</ak:fullname>");
		contentProfile.append("<ak:workemail>");
		contentProfile.append(existingProposal.getInvestigatorInfo().getPi()
				.getUserRef().getWorkEmails().get(0));
		contentProfile.append("</ak:workemail>");
		contentProfile.append("<ak:userid>");
		contentProfile.append(existingProposal.getInvestigatorInfo().getPi()
				.getUserProfileId());
		contentProfile.append("</ak:userid>");
		contentProfile.append("</ak:pi>");
	}

	/**
	 * Generates Signature Content Profile of a Proposal
	 * 
	 * @param contentProfile
	 * @param signatureInfo
	 */
	public void generateSignatureContentProfile(StringBuffer contentProfile,
			SignatureUserInfo signatureInfo) {
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

	/**
	 * Generates Investigator Content Profile
	 * 
	 * @param existingProposal
	 * @param contentProfile
	 */
	public void generateInvestigatorContentProfile(Proposal existingProposal,
			StringBuffer contentProfile) {
		generatePIContentProfile(existingProposal, contentProfile);
		for (InvestigatorRefAndPosition copis : existingProposal
				.getInvestigatorInfo().getCo_pi()) {
			generateCoPIContentProfile(contentProfile, copis);
		}
		for (InvestigatorRefAndPosition seniors : existingProposal
				.getInvestigatorInfo().getSeniorPersonnel()) {
			generateSeniorContentProfile(contentProfile, seniors);
		}
	}

	/**
	 * Geneartes Proposal Info Content Profile
	 * 
	 * @param proposalId
	 * @param existingProposal
	 * @param contentProfile
	 */
	public void genearteProposalInfoContentProfile(String proposalId,
			Proposal existingProposal, StringBuffer contentProfile) {
		contentProfile.append("<ak:proposal>");
		contentProfile.append("<ak:proposalid>");
		contentProfile.append(proposalId);
		contentProfile.append("</ak:proposalid>");
		contentProfile.append("<ak:proposaltitle>");
		contentProfile.append(existingProposal.getProjectInfo()
				.getProjectTitle());
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
		contentProfile.append(existingProposal.getBusinessManagerApproval()
				.name());
		contentProfile.append("</ak:approvedbybusinessmanager>");
		contentProfile.append("<ak:approvedbyirb>");
		contentProfile.append(existingProposal.getIrbApproval().name());
		contentProfile.append("</ak:approvedbyirb>");
		contentProfile.append("<ak:approvedbydean>");
		contentProfile.append(existingProposal.getDeanApproval().name());
		contentProfile.append("</ak:approvedbydean>");
		contentProfile.append("<ak:approvedbyuniversityresearchadministrator>");
		contentProfile.append(existingProposal
				.getResearchAdministratorApproval().name());
		contentProfile
				.append("</ak:approvedbyuniversityresearchadministrator>");
		contentProfile
				.append("<ak:withdrawnbyuniversityresearchadministrator>");
		contentProfile.append(existingProposal
				.getResearchAdministratorWithdraw().name());
		contentProfile
				.append("</ak:withdrawnbyuniversityresearchadministrator>");
		contentProfile
				.append("<ak:submittedbyuniversityresearchadministrator>");
		contentProfile.append(existingProposal
				.getResearchAdministratorSubmission().name());
		contentProfile
				.append("</ak:submittedbyuniversityresearchadministrator>");
		contentProfile.append("<ak:approvedbyuniversityresearchdirector>");
		contentProfile.append(existingProposal.getResearchDirectorApproval()
				.name());
		contentProfile.append("</ak:approvedbyuniversityresearchdirector>");
		contentProfile.append("<ak:deletedbyuniversityresearchdirector>");
		contentProfile.append(existingProposal.getResearchDirectorDeletion()
				.name());
		contentProfile.append("</ak:deletedbyuniversityresearchdirector>");
		contentProfile.append("<ak:archivedbyuniversityresearchdirector>");
		contentProfile.append(existingProposal.getResearchDirectorArchived()
				.name());
		contentProfile.append("</ak:archivedbyuniversityresearchdirector>");
	}

	/**
	 * Generates Proposal Content Profile with Current Datetime
	 * 
	 * @param proposalId
	 * @param userInfo
	 * @param existingProposal
	 * @return
	 */
	public StringBuffer generateProposalContentProfile(String proposalId,
			GPMSCommonInfo userInfo, Proposal existingProposal) {
		StringBuffer contentProfile = new StringBuffer();
		contentProfile.append("<Content>");
		contentProfile.append("<ak:record xmlns:ak=\"http://akpower.org\">");
		genearteProposalInfoContentProfile(proposalId, existingProposal,
				contentProfile);
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
		contentProfile
				.append("<Attribute AttributeId=\"urn:oasis:names:tc:xacml:3.0:content-selector\" IncludeInResult=\"false\">");
		contentProfile
				.append("<AttributeValue XPathCategory=\"urn:oasis:names:tc:xacml:3.0:attribute-category:resource\" DataType=\"urn:oasis:names:tc:xacml:3.0:data-type:xpathExpression\">//ak:record/ak:proposal</AttributeValue>");
		contentProfile.append("</Attribute>");
		return contentProfile;
	}

	/**
	 * @param authorProfile
	 * @param authorFullName
	 * @param proposalId
	 * @param existingProposal
	 * @param signedByCurrentUser
	 * @param contentProfile
	 */
	public void generateDefaultProposalContentProfile(
			UserProfile authorProfile, String authorFullName,
			String proposalId, Proposal existingProposal,
			boolean signedByCurrentUser, StringBuffer contentProfile) {
		contentProfile.append("<Content>");
		contentProfile.append("<ak:record xmlns:ak=\"http://akpower.org\">");
		contentProfile.append("<ak:proposal>");
		contentProfile.append("<ak:proposalid>");
		contentProfile.append(proposalId);
		contentProfile.append("</ak:proposalid>");
		contentProfile.append("<ak:proposaltitle>");
		contentProfile.append(existingProposal.getProjectInfo()
				.getProjectTitle());
		contentProfile.append("</ak:proposaltitle>");
		contentProfile.append("<ak:irbApprovalRequired>");
		contentProfile.append(existingProposal.isIrbApprovalRequired());
		contentProfile.append("</ak:irbApprovalRequired>");
		generateAuthorContentProfile(contentProfile, authorProfile,
				authorFullName);
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
	public StringBuffer generateContentProfile(String proposalId,
			Proposal existingProposal, List<SignatureUserInfo> signatures,
			UserProfile authorProfile) {
		StringBuffer contentProfile = new StringBuffer();
		String authorFullName = authorProfile.getFullName();
		contentProfile.append("<Content>");
		contentProfile.append("<ak:record xmlns:ak=\"http://akpower.org\">");
		genearteProposalInfoContentProfile(proposalId, existingProposal,
				contentProfile);
		generateAuthorContentProfile(contentProfile, authorProfile,
				authorFullName);
		generateInvestigatorContentProfile(existingProposal, contentProfile);
		for (SignatureUserInfo signatureInfo : signatures) {
			generateSignatureContentProfile(contentProfile, signatureInfo);
		}
		contentProfile.append("</ak:proposal>");
		contentProfile.append("</ak:record>");
		contentProfile.append("</Content>");
		contentProfile
				.append("<Attribute AttributeId=\"urn:oasis:names:tc:xacml:3.0:content-selector\" IncludeInResult=\"false\">");
		contentProfile
				.append("<AttributeValue XPathCategory=\"urn:oasis:names:tc:xacml:3.0:attribute-category:resource\" DataType=\"urn:oasis:names:tc:xacml:3.0:data-type:xpathExpression\">//ak:record/ak:proposal</AttributeValue>");
		contentProfile.append("</Attribute>");
		return contentProfile;
	}

	/**
	 * Generates Attributes based on policy info
	 * 
	 * @param policyInfo
	 * @return
	 */
	public HashMap<String, Multimap<String, String>> generateAttributes(
			JsonNode policyInfo) {
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

	/**
	 * Categorizes different Obligation Types
	 * 
	 * @param preObligations
	 * @param postObligations
	 * @param ongoingObligations
	 * @param obligation
	 */
	public void categorizeObligationTypes(
			List<ObligationResult> preObligations,
			List<ObligationResult> postObligations,
			List<ObligationResult> ongoingObligations,
			ObligationResult obligation) {
		if (obligation instanceof org.wso2.balana.xacml3.Obligation) {
			List<AttributeAssignment> assignments = ((org.wso2.balana.xacml3.Obligation) obligation)
					.getAssignments();
			String obligationType = "postobligation";
			for (AttributeAssignment assignment : assignments) {
				if (assignment.getAttributeId().toString()
						.equalsIgnoreCase("obligationType")) {
					obligationType = assignment.getContent();
					break;
				}
			}
			if (obligationType.equals("preobligation")) {
				preObligations.add(obligation);
				System.out.println(obligationType + " is FOUND");
			} else if (obligationType.equals("postobligation")) {
				postObligations.add(obligation);
				System.out.println(obligationType + " is FOUND");
			} else {
				ongoingObligations.add(obligation);
				System.out.println(obligationType + " is FOUND");
			}
		}
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
	public boolean deleteProposalWithObligations(String proposalRoles,
			String proposalUserTitle, Proposal existingProposal,
			UserProfile authorProfile, String authorUserName,
			List<ObligationResult> obligations) throws JsonProcessingException {
		EmailCommonInfo emailDetails = new EmailCommonInfo();
		getObligationsDetails(obligations, emailDetails);
		boolean isDeleted = deleteProposal(existingProposal, proposalRoles,
				proposalUserTitle, authorProfile);
		if (isDeleted) {
			String emailSubject = emailDetails.getEmailSubject();
			String emailBody = emailDetails.getEmailBody();
			String authorName = emailDetails.getAuthorName();
			String piEmail = emailDetails.getPiEmail();
			List<String> emaillist = emailDetails.getEmaillist();
			if (!emailSubject.equals("")) {
				EmailUtil emailUtil = new EmailUtil();
				emailUtil.sendMailMultipleUsersWithoutAuth(piEmail, emaillist,
						emailSubject + authorName, emailBody);
			}
		}
		return isDeleted;
	}
}
