package gpms.ngac.GraphTester.Mutations;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.opencsv.CSVWriter;

import gov.nist.csd.pm.exceptions.PMException;

public class MutationController {
	List<String> testMethods;
	int totalNumberOfMutants = 0;
	int totalNumberOfKilledMutants = 0;
	int totalNumberOfMutantsForTest = 0;
	int totalNumberOfKilledMutantsForTest = 0;
	List<String[]> data = new ArrayList<String[]>();
	String[] row;
	String CSVFilePath = "CSV/OverallMutationResults.csv";

	public static void main(String[] args) throws PMException, IOException {
		
		MutationController mc = new MutationController();
		File CSV = new File(mc.CSVFilePath);

		mc.createTestMethods();
		mc.createHeaderForCSV();
		for (String testMethod : mc.testMethods) {
			String[] row=new String[10];
			mc.totalNumberOfMutantsForTest = 0;
			mc.totalNumberOfKilledMutantsForTest = 0;
			double resultADDAR = mc.testADDAR(testMethod);
			double resultADDASSOC = mc.testADDASSOC(testMethod);
			double resultDEAS = mc.testDEAS(testMethod);
			double resultDISS = mc.testDISS(testMethod);
			double resultREMAR = mc.testREMAR(testMethod);
			double resultREMNODE = mc.testREMNODE(testMethod);
			double resultCHANGEAS = mc.testCHANGEAS(testMethod);
			double resultCHANGEASSOC = mc.testCHANGEASSOC(testMethod);

			row[0] = testMethod;
			row[1] = Double.toString(resultADDAR);
			row[2] = Double.toString(resultADDASSOC);
			row[3] = Double.toString(resultDEAS);
			row[4] = Double.toString(resultDISS);
			row[5] = Double.toString(resultREMAR);
			row[6] = Double.toString(resultREMNODE);
			row[7] = Double.toString(resultCHANGEAS);
			row[8] = Double.toString(resultCHANGEASSOC);
			row[9] = Double.toString((double)mc.totalNumberOfKilledMutantsForTest/(double)mc.totalNumberOfMutantsForTest*100);
			mc.data.add(row);
		}
		mc.saveCSV(mc.data, CSV);
	}

	private double testADDAR(String testMethod) {
		MutatorADDAR mutatorADDAR = new MutatorADDAR();
		try {
			mutatorADDAR.init(testMethod);
		} catch (PMException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		double mutationScore = mutatorADDAR.calculateMutationScore(mutatorADDAR.getNumberOfMutants(),
				mutatorADDAR.getNumberOfKilledMutants());
		//System.out.println("Number of mutations: " + mutatorADDAR.getNumberOfMutants());
		//System.out.println("Number of killed mutants: " + mutatorADDAR.getNumberOfKilledMutants());

		//System.out.println("Mutation Score: " + mutationScore + "%");
		//System.out.println();
		totalNumberOfMutantsForTest += mutatorADDAR.getNumberOfMutants();
		totalNumberOfKilledMutantsForTest += mutatorADDAR.getNumberOfKilledMutants();
		return mutationScore;
	}

	private double testADDASSOC(String testMethod) {
		MutatorADDASSOC mutatorADDASSOC = new MutatorADDASSOC();

		try {
			mutatorADDASSOC.init(testMethod);
		} catch (PMException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		double mutationScore = mutatorADDASSOC.calculateMutationScore(mutatorADDASSOC.getNumberOfMutants(),
				mutatorADDASSOC.getNumberOfKilledMutants());
		//System.out.println("Number of mutations: " + mutatorADDASSOC.getNumberOfMutants());
		//System.out.println("Number of killed mutants: " + mutatorADDASSOC.getNumberOfKilledMutants());

		//System.out.println("Mutation Score: " + mutationScore + "%");
		//System.out.println();
		totalNumberOfMutantsForTest += mutatorADDASSOC.getNumberOfMutants();
		totalNumberOfKilledMutantsForTest += mutatorADDASSOC.getNumberOfKilledMutants();
		return mutationScore;

	}

	private double testDEAS(String testMethod) {
		MutatorDEAS mutatorDEAS = new MutatorDEAS();

		try {
			mutatorDEAS.init(testMethod);
		} catch (PMException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		double mutationScore = mutatorDEAS.calculateMutationScore(mutatorDEAS.getNumberOfMutants(),
				mutatorDEAS.getNumberOfKilledMutants());
		//System.out.println("Number of mutations: " + mutatorDEAS.getNumberOfMutants());
		//System.out.println("Number of killed mutants: " + mutatorDEAS.getNumberOfKilledMutants());

		//System.out.println("Mutation Score: " + mutationScore + "%");
		//System.out.println();
		totalNumberOfMutantsForTest += mutatorDEAS.getNumberOfMutants();
		totalNumberOfKilledMutantsForTest += mutatorDEAS.getNumberOfKilledMutants();
		return mutationScore;

	}

	private double testDISS(String testMethod) {
		MutatorDISS mutatorDISS = new MutatorDISS();

		try {
			mutatorDISS.init(testMethod);
		} catch (PMException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		double mutationScore = mutatorDISS.calculateMutationScore(mutatorDISS.getNumberOfMutants(),
				mutatorDISS.getNumberOfKilledMutants());
		//System.out.println("Number of mutations: " + mutatorDISS.getNumberOfMutants());
		//System.out.println("Number of killed mutants: " + mutatorDISS.getNumberOfKilledMutants());

		//System.out.println("Mutation Score: " + mutationScore + "%");
		//System.out.println();
		totalNumberOfMutantsForTest += mutatorDISS.getNumberOfMutants();
		totalNumberOfKilledMutantsForTest += mutatorDISS.getNumberOfKilledMutants();
		return mutationScore;

	}

	private double testREMAR(String testMethod) {

		MutatorREMAR mutatorREMAR = new MutatorREMAR();

		try {
			mutatorREMAR.init(testMethod);
		} catch (PMException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		double mutationScore = mutatorREMAR.calculateMutationScore(mutatorREMAR.getNumberOfMutants(),
				mutatorREMAR.getNumberOfKilledMutants());
		//System.out.println("Number of mutations: " + mutatorREMAR.getNumberOfMutants());
		//System.out.println("Number of killed mutants: " + mutatorREMAR.getNumberOfKilledMutants());

		//System.out.println("Mutation Score: " + mutationScore + "%");
		//System.out.println();
		totalNumberOfMutantsForTest += mutatorREMAR.getNumberOfMutants();
		totalNumberOfKilledMutantsForTest += mutatorREMAR.getNumberOfKilledMutants();
		return mutationScore;

	}

	private double testREMNODE(String testMethod) {
		MutatorREMNODE mutatorREMNODE = new MutatorREMNODE();

		try {
			mutatorREMNODE.init(testMethod);
		} catch (PMException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		double mutationScore = mutatorREMNODE.calculateMutationScore(mutatorREMNODE.getNumberOfMutants(),
				mutatorREMNODE.getNumberOfKilledMutants());
		//System.out.println("Number of mutations: " + mutatorREMNODE.getNumberOfMutants());
		//System.out.println("Number of killed mutants: " + mutatorREMNODE.getNumberOfKilledMutants());

		//System.out.println("Mutation Score: " + mutationScore + "%");
		//System.out.println();
		totalNumberOfMutantsForTest += mutatorREMNODE.getNumberOfMutants();
		totalNumberOfKilledMutantsForTest += mutatorREMNODE.getNumberOfKilledMutants();
		return mutationScore;

	}
	private double testCHANGEAS(String testMethod) {
		MutatorCHANGEAS mutatorCHANGEAS = new MutatorCHANGEAS();

		try {
			mutatorCHANGEAS.init(testMethod);
		} catch (PMException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		double mutationScore = mutatorCHANGEAS.calculateMutationScore(mutatorCHANGEAS.getNumberOfMutants(),
				mutatorCHANGEAS.getNumberOfKilledMutants());
		//System.out.println("Number of mutations: " + mutatorCHANGEAS.getNumberOfMutants());
		//System.out.println("Number of killed mutants: " + mutatorCHANGEAS.getNumberOfKilledMutants());

		//System.out.println("Mutation Score: " + mutationScore + "%");
		//System.out.println();
		totalNumberOfMutantsForTest += mutatorCHANGEAS.getNumberOfMutants();
		totalNumberOfKilledMutantsForTest += mutatorCHANGEAS.getNumberOfKilledMutants();
		return mutationScore;

	}
	private double testCHANGEASSOC(String testMethod) {
		MutatorCHANGEASSOC mutatorCHANGEASSOC = new MutatorCHANGEASSOC();

		try {
			mutatorCHANGEASSOC.init(testMethod);
		} catch (PMException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		double mutationScore = mutatorCHANGEASSOC.calculateMutationScore(mutatorCHANGEASSOC.getNumberOfMutants(),
				mutatorCHANGEASSOC.getNumberOfKilledMutants());
		//System.out.println("Number of mutations: " + mutatorCHANGEASSOC.getNumberOfMutants());
		//System.out.println("Number of killed mutants: " + mutatorCHANGEASSOC.getNumberOfKilledMutants());

		//System.out.println("Mutation Score: " + mutationScore + "%");
		//System.out.println();
		totalNumberOfMutantsForTest += mutatorCHANGEASSOC.getNumberOfMutants();
		totalNumberOfKilledMutantsForTest += mutatorCHANGEASSOC.getNumberOfKilledMutants();
		return mutationScore;

	}
	private void createTestMethods() {
		testMethods = new ArrayList<String>();
		testMethods.add("RS");
		testMethods.add("R");
		testMethods.add("PNP");
		testMethods.add("PP");


	}

	private void createHeaderForCSV() {
		String[] header = new String[10];
		header[0] = "TestMethod";
		header[1] = "ADDAR";
		header[2] = "ADDASSOC";
		header[3] = "DEAS";
		header[4] = "DISS";
		header[5] = "REMAR";
		header[6] = "REMNODE";
		header[7] = "CHANGEAS";
		header[8] = "CHANGEASSOC";
		header[9] = "totalMutationScore";
		data.add(header);
	}
	
	public void saveCSV(List<String[]> data, File directoryForTestResults) throws PMException, IOException {

		if (directoryForTestResults.createNewFile()) {
			//System.out.println("File has been created.");
		} else {

			//System.out.println("File already exists.");
		}
		BufferedWriter writer = null;
		writer = new BufferedWriter(new FileWriter(directoryForTestResults));
		CSVWriter CSVwriter = new CSVWriter(writer);
		CSVwriter.writeAll(data);
		writer.flush();
		CSVwriter.close();

		if (writer != null)
			writer.close();

	}
}
