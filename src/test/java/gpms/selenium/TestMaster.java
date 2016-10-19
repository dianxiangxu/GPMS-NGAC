package gpms.selenium;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({ BusinessManagerAttatchmentFail.class,
		BusinessManagerApprovalFail.class, ChairApprovalFail.class,
		ChairAttatchmentFail.class, CoPIaddsSeniorPersonal.class,
		CoPIattemptsProposalDeletion.class, CoPIattemptsToDeleteCoPI.class,
		CoPIproposalSubmissionFail.class, DeanApprovalFail.class,
		DeanAttatchmentFail.class, DeanDeletesAttatchmentFail.class,
		DeanFileUploadFail.class, DelegationByChair.class, PIaddsCoPI.class,
		PIaddsFileAttatchment.class, PIdeletesAttatchment.class,
		PIdeletesCoPI.class, PIFileAttatchmentUpload.class,
		PIuploadsFileAttatchment.class, ProposalAddFailure.class,
		ResearchAdminApprovalFail.class, ResearchAdminProposalWithdrawn.class,
		ResearchDirectorApprovalFail.class,
		ResearchDirectorProposalDeletionFail.class,
		SeniorPersonalProposalDeleteFail.class,
		SeniorPersonalProposalSaveFail.class,
		SuccessfullProposalSubmition_IRB.class,
		SuccessfullProposalSubmition_NoIRB.class,
		SuccessfullProposalSubmitionIRB_twoFaculty.class,
		TenuredChemFacultyProposal.class })
public class TestMaster {
	public TestMaster() {
	}
}
