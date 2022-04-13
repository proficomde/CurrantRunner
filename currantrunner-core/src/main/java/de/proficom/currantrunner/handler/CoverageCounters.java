package de.proficom.currantrunner.handler;

import org.jacoco.core.analysis.CoverageBuilder;
import org.jacoco.core.analysis.IClassCoverage;

/**
 * Class to collect all data from JaCoCo coverage report
 */
public class CoverageCounters {
	private int coveredInstructions;
	private int coveredBranches;
	private int coveredComplexity;

	/**
	 * Constructor to reset all counters
	 */
	public CoverageCounters() {
		this.coveredInstructions = 0;
		this.coveredBranches = 0;
		this.coveredComplexity = 0;
	}

	/**
	 * Get instruction coverage
	 * 
	 * @return number of covered instructions during test run
	 */
	public int getCoveredInstructions() {
		return this.coveredInstructions;
	}

	/**
	 * Get branch coverage
	 * 
	 * @return number of covered branches during test run
	 */
	public int getCoveredBranches() {
		return this.coveredBranches;
	}

	/**
	 * Get instruction complexity
	 * 
	 * @return complexity covered during test run
	 */
	public int getCoveredComplexity() {
		return this.coveredComplexity;
	}

	/**
	 * Update all internal counters by JaCoCo coverage builder We calculate the sum
	 * of all classes available in coverage report. Don't forget to reset these
	 * counters before test starts!
	 * 
	 * @param coverageBuilder Coverage of last test run
	 */
	public void updateCounters(CoverageBuilder coverageBuilder) {
		for (final IClassCoverage cc : coverageBuilder.getClasses()) {		
			this.coveredInstructions += cc.getInstructionCounter().getCoveredCount();
			this.coveredBranches += cc.getBranchCounter().getCoveredCount();
			this.coveredComplexity += cc.getComplexityCounter().getCoveredCount();
		}
	}
}
