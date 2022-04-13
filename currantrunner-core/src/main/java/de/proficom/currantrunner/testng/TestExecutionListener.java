package de.proficom.currantrunner.testng;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.testng.IMethodInstance;
import org.testng.IMethodInterceptor;
import org.testng.ITestContext;

import de.proficom.currantrunner.core.PrioritizationData;

/**
 * This class is used to alter the list of test methods that TestNG is about to
 * run.
 * 
 * Here we do the machine learning-based prioritization of test cases.
 */
public class TestExecutionListener implements IMethodInterceptor {

	/**
	 * Prioritize the test cases with the machine learning model.
	 * 
	 * @return the test cases prioritized after their probability of failure.
	 */
	public List<IMethodInstance> intercept(List<IMethodInstance> methods, ITestContext context) {
		// Avoid empty lists
		if (methods == null || methods.isEmpty()) {
			return methods;
		}

		// maps the test case names to the test cases that are currently under test
		HashMap<String, IMethodInstance> foundTestsDict = new HashMap<String, IMethodInstance>();
		List<String> allMethodNames = new ArrayList<>();
		for (IMethodInstance test : methods) {
			foundTestsDict.put(test.getMethod().getQualifiedName(), test);
			allMethodNames.add(test.getMethod().getQualifiedName());
		}
		
		// Prioritize by CurrantRunner
		List<PrioritizationData> prioritizedTests = CurrantRunnerTestNG.getCurrantRunner().prioritize(allMethodNames);

		// Convert test cases name to it's related instances
		List<IMethodInstance> orderedTestcases = new ArrayList<>();
		for (PrioritizationData method : prioritizedTests) {
			orderedTestcases.add(foundTestsDict.get(method.getTestcaseName()));
		}		
		
		// Print ordered list
		System.out.println("[CurrantRunner] Execution order of prioritized test cases:");
		int i = 1;
		for (PrioritizationData test : prioritizedTests) {
			System.out.format("[CurrantRunner]   %5s. ", i);
			System.out.println(test.getTestcaseName() + " (" + test.formatProbabilityPercent() + ")");
			i++;
		}
		System.out.println();

		return orderedTestcases;
	}

}
