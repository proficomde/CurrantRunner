package de.proficom.currantrunner.core;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

import de.proficom.currantrunner.metrics.MetricsBase;
import weka.classifiers.Classifier;
import weka.classifiers.trees.HoeffdingTree;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instances;

/**
 * Class to hold and train the machine learning model (k-nearest neighbor
 * Classifier) for prioritizing test cases depending on their metric values.
 */
public class MLModel {
	private final String CLASSIFIER_RESULT_PASS = "pass";
	private final String CLASSIFIER_RESULT_FAIL = "fail";
	
	/**
	 * the machine learning model (HoeffdingTree) classifier
	 */
	HoeffdingTree HTClassifier;

	/**
	 * Simple constructor to access the machine learning model that is saved in the
	 * database.
	 */
	public MLModel(DatabaseConnector db) {
		byte[] modelBytes = db.getModel();
		// if classifier stays uninitialized, a new model is build later.
		if (modelBytes == null) {
			return;
		}

		try {
			// Get the runtime object from the byte representation.
			ByteArrayInputStream modelStream = new ByteArrayInputStream(modelBytes);
			ObjectInputStream ois = new ObjectInputStream(modelStream);
			Classifier cls = (Classifier) ois.readObject();
			HTClassifier = (HoeffdingTree) cls;
			ois.close();
			modelStream.close();
		} catch (Exception e) {
			System.out.println("There is no model in the database yet.");
		}
	}

	/**
	 * Train the model with the given set of test data.
	 * 
	 * @param testcases the training set a.k.a. samples
	 * @param db        storage for trained model
	 */
	public void train(List<TestCase> testcases, DatabaseConnector db) {
		/*
		 * Checks if everything necessary is available. If there is not model yet, a new
		 * one is build.
		 */
		if (testcases == null || testcases.size() == 0) {
			return;
		}
		if (HTClassifier == null) {
			buildModel(testcases, db);
			return;
		}

		// create the needed data structure for the model
		Instances inst = createARFFData(testcases);
		inst.setClassIndex(inst.numAttributes() - 1);

		try {
			// train the classifier for each Instance in instances
			inst.forEach(i -> {
				try {
					HTClassifier.updateClassifier(i);
				} catch (Exception e) {
					System.out.println("Error when updating classifier:\n" + e.getMessage() + "\n------------");
				}
			});

			// Serialize the model/ Get the byte representation and save it in the database.
			ByteArrayOutputStream modelStream = new ByteArrayOutputStream();
			ObjectOutputStream oos = new ObjectOutputStream(modelStream);
			oos.writeObject(HTClassifier);
			db.insertOrUpdateModel(modelStream.toByteArray());
			oos.flush();
			oos.close();
			modelStream.flush();
			modelStream.close();
		} catch (Exception e) {
			System.err.println(e.getMessage());
		}
	}
	
	/**
	 * Calculate for every testcase it's likelyhood to fail in next test run
	 * 
	 * @param testcases	List of all testcases
	 * @return			Map from testcases to it's probability to have FAILED
	 */
	public HashMap<TestCase, Double> getFailureProbability(List<TestCase> testcases) {
		HashMap<TestCase, Double> failureProbability = new HashMap<TestCase, Double>();
		
		// Initial fill: Assume every test will fail
		for (TestCase tc : testcases) {
			failureProbability.put(tc, 1.0);
		}
		
		// Check if we have already a ML model and at least one testcase
		if (HTClassifier == null) {
			System.err.println("There is no model yet...");
		} else if (testcases.size() <= 0) {
			System.err.println("There is no testcases to prioritize...");
		} else {
			// create the needed data format for the ml model
			Instances instances = createARFFData(testcases);
			instances.setClassIndex(instances.numAttributes() - 1);

			//  Loop over all test cases and calculate the probability of failure.
			for (int i = 0; i < instances.numInstances(); i++) {
				try {
					// Returns a tuple that describes the probability of failure [0] and the
					// probability of a test success [1].
					double[] dist = HTClassifier.distributionForInstance(instances.get(i));
					
					// we are interested in FAILED probability
					double tcFailureProbability = dist[0];
					
					// Store the value
					failureProbability.put(testcases.get(i), tcFailureProbability);
				} catch (Exception e) {
					System.err.println(e.getMessage());
				}
			}
		}
		return failureProbability;
	}
	
	/**
	 * Returns the prioritized test cases descending in priority. If the model does
	 * not exist yet, the given test cases are returned without a specific order. If
	 * the model throws an exception during classification, the original given test
	 * case prioritization is returned.
	 * 
	 * @param testcases				set of test cases that need to be ordered
	 * @param failureProbability	maps testcase to it's likelihood to FAIL
	 * @return						prioritized test cases
	 */
	public ArrayList<TestCase> prioritize(List<TestCase> testcases, HashMap<TestCase, Double> failureProbability) {
		/*
		 * if there is no model or there is an error with the given testcases list, the
		 * input is returned.
		 */
		if (HTClassifier == null || testcases.size() == 0) {
			System.err.println("There is no model yet. Will use original order...");
			return new ArrayList<TestCase>(testcases);
		}

		// This comparator is used to sort the the probability of failure.
		Comparator<TestCase> byProbability = (TestCase tc1, TestCase tc2) -> {
			double probTc1 = 1.0;
			double probTc2 = 1.0;

			// Get the probabilities
			if (failureProbability.containsKey(tc1)) {
				probTc1 = failureProbability.get(tc1);
			}
			if (failureProbability.containsKey(tc2)) {
				probTc2 = failureProbability.get(tc2);
			}
			// Sort INCREASING (most likely error at first)
			if (probTc1 < probTc2) {
				return +1;
			} else if (probTc1 == probTc2) {
				return 0;
			}
			return -1;
		};

		// Sort test cases by probability of failure
		ArrayList<TestCase> orderedTestCases = new ArrayList<TestCase>(testcases);
		orderedTestCases.sort(byProbability);

		return orderedTestCases;
	}

	/**
	 * Method, that creates the model for given test cases and then stores the byte
	 * representation in the database.
	 * 
	 * @param testcases training data for the model.
	 */
	private void buildModel(List<TestCase> testcases, DatabaseConnector db) {
		if (testcases == null | testcases.size() < 1) {
			return;
		}
		// create Hoeffding tree classifier
		HTClassifier = new HoeffdingTree();

		// create the needed data format for the model
		Instances inst = createARFFData(testcases);
		inst.setClassIndex(inst.numAttributes() - 1);

		try {
			 // increases accuracy and training performance
			HTClassifier.setBatchSize("5");

			// after model parameterization, the model is build with the training data
			HTClassifier.buildClassifier(inst); 

			// Serialize the model/ Get the byte representation and save it in the database.
			ByteArrayOutputStream modelStream = new ByteArrayOutputStream();
			ObjectOutputStream oos = new ObjectOutputStream(modelStream);
			oos.writeObject(HTClassifier);
			db.insertOrUpdateModel(modelStream.toByteArray());
			oos.flush();
			oos.close();
			modelStream.flush();
			modelStream.close();
		} catch (Exception e) {
			System.err.println(e.getMessage());
		}
	}

	/**
	 * ARFF is the standard data format used for training and testing weka machine
	 * learning models. This method takes a list of mapped {@link TestCase} with
	 * their label as input data and converts this into the ARFF data format. The
	 * "Instances" class is used for holding this data structure.
	 * 
	 * @param rawData Mapped list with tests and their label ["fail", "pass"].
	 * @return The converted input mapping as ARFF data structure held by an
	 *         Instances object.
	 */
	private Instances createARFFData(List<TestCase> rawData) {
		ArrayList<Attribute> attributes = new ArrayList<Attribute>(); // list of all Attributes or test metrics
																		// respectively
		/*
		 * The possible labels are stored in a list because the list is needed for
		 * initializing a "nominal attribute" in the ARFF data structure.
		 */
		ArrayList<String> results = new ArrayList<String>();
		results.add(CLASSIFIER_RESULT_FAIL);
		results.add(CLASSIFIER_RESULT_PASS);

		/*
		 * The attributes are filled with created Attribute objects. Per default they
		 * are numeric. If additionally a list is given it is declared as nominal
		 * attribute (see "Result").
		 */
		if (rawData.size() > 0) {
			List<MetricsBase<?>> allMetrics = rawData.get(0).getAllMetrics();
			for (MetricsBase<?> curMetric : allMetrics) {
				if (curMetric.isMLContained()) {
					attributes.add(new Attribute(curMetric.getMLAttributeName()));
				}
			}
		}
		// Finally add classifier result
		attributes.add(new Attribute("Result", results));

		/*
		 * A new Instances method is created with all defined attributes. Later it will
		 * be filled with data
		 */
		Instances data = new Instances("Metrics", attributes, 0);

		int numberOfAttributes = data.numAttributes();

		/*
		 * For all input tests the values are extracted and put into a new instance of a
		 * values array. At the end the array is added to data.
		 */
		for (TestCase test : rawData) {
			// Array that stores all test values and later will be added to data.
			double[] values = new double[numberOfAttributes];

			int idxAttribute = 0;
			for (MetricsBase<?> curMetric : test.getAllMetrics()) {
				if (curMetric.isMLContained()) {
					values[idxAttribute] = curMetric.getMLValue();
					idxAttribute++;
				}
			}

			// Last value = Current result of test case
			// Gets label for the test and returns the index, because values needs numeric
			// values. {0: fail, 1: pass}
			values[idxAttribute] = results.indexOf(lastResultValue(test));
			data.add(new DenseInstance(1.0, values));
		}
		return data;
	}

	/**
	 * Get the result of last test run execution as String representation.
	 * If a test is not executed it is treated as FAIL.
	 * 
	 * @param tc	Test case to be analyzed
	 * @return		"pass" or "fail"
	 */
	private String lastResultValue(TestCase tc) {
		TestCase.Results result = tc.getLastResult();
		if (result == TestCase.Results.PASSED) {
			return CLASSIFIER_RESULT_PASS;
		}
		return CLASSIFIER_RESULT_FAIL;
	}

}
