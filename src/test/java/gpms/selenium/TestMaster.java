package gpms.selenium;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({ BusinessManagerAttatchmentFail.class,
		BusinessManagerApprovalFail.class, ChairApprovalFail.class,
		ChairAttatchmentFail.class, CoPIaddsSeniorPersonal.class,
		CoPIattemptsProposalDeletion.class, CoPIattemptsToDeleteCoPI.class,
		CoPIproposalSubmissionFail.class, DeanApprovalFail.class,
		DeanUploadsAttatchmentFail.class, DeanDeletesAttatchmentFail.class,
		DeanUploadsAttatchmentFail.class, DelegationByChair.class,
		PIAddsCoPI.class, PIUploadsFileAttatchment.class,
		PIDeletesFileAttatchment.class, PIDeletesCoPI.class,
		PIFileAttatchmentUpload.class, PIUploadsFileAttatchment.class,
		ProposalAddFailure.class, ResearchAdminApprovalFail.class,
		ResearchAdminProposalWithdrawn.class,
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
