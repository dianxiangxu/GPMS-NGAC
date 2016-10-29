package gpms.selenium;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({ DelegationByChair.class,
		SuccessfullProposalSubmition_IRB.class,
		SuccessfullProposalSubmition_NoIRB.class,
		SuccessfullProposalSubmitionIRB_twoFaculty.class,
		ProposalAddFailure.class, ProposalAddSuccessByTenuredFaculty.class,
		PIAddsCoPI.class, PIDeletesCoPI.class, PIFileAttatchmentUpload.class,
		PIDeletesFileAttatchment.class, CoPIaddsSeniorPersonal.class,
		CoPIattemptsToDeleteCoPI.class, CoPIattemptsProposalDeletion.class,
		CoPIproposalSubmissionFail.class, SeniorPersonalProposalSaveFail.class,
		SeniorPersonalProposalDeleteFail.class, ChairAttatchmentFail.class,
		ChairApprovalFail.class, BusinessManagerAttatchmentFail.class,
		BusinessManagerApprovalFail.class, DeanUploadsAttatchmentFail.class,
		DeanDeletesAttatchmentFail.class, DeanApprovalFail.class,
		ResearchAdminApprovalFail.class, ResearchAdminProposalWithdrawn.class,
		ResearchDirectorProposalDeletionFail.class,
		ResearchDirectorApprovalFail.class })
public class TestMaster {
	public TestMaster() {
	}
}
