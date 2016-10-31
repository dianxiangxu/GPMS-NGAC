package gpms.selenium;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({ BusinessManagerApprovalFail.class,
		BusinessManagerAttatchmentFail.class, ChairApprovalFail.class,
		ChairAttatchmentFail.class, CoPIaddsSeniorPersonal.class,
		CoPIattemptsProposalDeletion.class, CoPIattemptsToDeleteCoPI.class,
		CoPIproposalSubmissionFail.class, DeanApprovalFail.class,
		DeanDeletesAttatchmentFail.class, DeanUploadsAttatchmentFail.class,
		DelegationByChair.class, PIAddsCoPI.class, PIDeletesCoPI.class,
		PIDeletesFileAttatchment.class, PIFileAttatchmentUpload.class,
		ProposalAddFailure.class, ProposalAddSuccessByTenuredFaculty.class,
		ResearchAdminApprovalFail.class, ResearchAdminProposalWithdrawn.class,
		ResearchDirectorApprovalFail.class,
		ResearchDirectorProposalDeletionFail.class,
		SeniorPersonalProposalDeleteFail.class,
		SeniorPersonalProposalSaveFail.class,
		SuccessfullProposalSubmition_IRB.class,
		SuccessfullProposalSubmition_NoIRB.class,
		SuccessfullProposalSubmitionIRB_twoFaculty.class })
public class TestMaster {
	public TestMaster() {
	}
}
