package gpms.ngac.GraphTester;

import java.util.ArrayList;

public class AllPairsTest {
	
	
	  static void test1(){
		  int[] choices = {10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10};
		  boolean noShuffle = false;
		  int maxGoes = 100;
		  long seed = 42;
		  AllPairs.indefiniteGenerate(choices, seed, maxGoes, !noShuffle, null, false);
	  }

	  static void test2(){
		  int[] choices = {10, 10, 10, 10, 10, 10, 10, 10, 10, 10};
		  boolean noShuffle = true;
		  int maxGoes = 100;
		  long seed = 42;
		  AllPairs.indefiniteGenerate(choices, seed, maxGoes, !noShuffle, null, false);
	  }

	  static void test102(){
		  int[] choices = {10, 10, 10, 10, 10, 10, 10, 10, 10, 10};
		  boolean noShuffle = true;
		  int maxGoes = 100;
		  long seed = 42;
		  printPairs(AllPairs.generatePairs(choices, seed, maxGoes, !noShuffle, null, false));
		  
	  }

	  static void test103(){
		  int[] choices = {10, 10, 10, 1};
		  boolean noShuffle = true;
		  int maxGoes = 100;
		  long seed = 42;
		  printPairs(AllPairs.generatePairs(choices, seed, maxGoes, !noShuffle, null, false));
		  
	  }
	  static void test104(){
		  int[] choices = {12, 1, 2};
		  boolean noShuffle = true;
		  int maxGoes = 100;
		  long seed = 42;
		  printPairs(AllPairs.generatePairs(choices, seed, maxGoes, !noShuffle, null, false));
		  
	  }
	  static void printPairs(ArrayList<int[]> pairs){
		  System.out.println("#pairs: "+pairs.size());
		  for (int[] pair: pairs){
			  System.out.print(pairs.indexOf(pair)+1+".");
			  for (int i=0; i<pair.length; i++)
				  System.out.print(" "+pair[i]);
			  System.out.println();
		  }
	  }
	  
	  public static void main(String[] s) {
//		  test1();
//		  test2();
		  test104();
		//  test103();
	  }
}
