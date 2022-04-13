package de.proficom.currantrunner.handler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Class to calculate uniqueness values for test case names.
 * 
 * This uniqueness is calculated by word tokens. The tokens are generated either
 * from camelCase notation or separated by '_'.
 */
public class UniquenessCalculation {

	// Sets to store synonyms of a word
	Set<String> testFilter = new HashSet<String>();
	Set<String> equalFilter = new HashSet<String>();
	Set<String> errorFilter = new HashSet<String>();

	/**
	 * Private constructor, because it is intended to only call the main method
	 * "calculateUniqueness" in a static way. Nevertheless this constructor is used
	 * to initialize the word filter.
	 */
	private UniquenessCalculation() {
		// synonyms of the words to achieve a "soft cosine similarity"
		// this is used to equalize names like "testFunction" and "testsFunction"
		testFilter.add("tests");
		testFilter.add("testing");
		testFilter.add("tester");
		testFilter.add("assert");
		testFilter.add("asserting");
		testFilter.add("check");
		testFilter.add("assess");
		testFilter.add("checking");

		equalFilter.add("equally");
		equalFilter.add("same");
		equalFilter.add("equivalent");
		equalFilter.add("equals");

		errorFilter.add("fail");
		errorFilter.add("failure");
		errorFilter.add("exception");
		errorFilter.add("invalid");
		errorFilter.add("fault");
		errorFilter.add("faulty");
		errorFilter.add("flaw");
		errorFilter.add("mistake");
		errorFilter.add("wrong");
		errorFilter.add("incorrect");
		errorFilter.add("miss");
	}

	/**
	 * Calculate how "unique" a test case name is in contrast to all other test case
	 * names. The uniqueness-number is the cosine similarity. This metric calculates
	 * the cosine of the angle between word embeddings (vectors) in a
	 * multidimensional space spanned over all available words.
	 * 
	 * The uniqueness is calculated for all test case names and the sum is mapped to
	 * them. The greater the value, the more similar is one test case name to all
	 * others. Small values can be interpreted as a small similarity and therefore
	 * this words are more unique.
	 * 
	 * @param testcaseNames the names of which the uniqueness should be calculated.
	 * @return test case name mapped to their uniqueness
	 */
	public static HashMap<String, Double> calculateUniqueness(Set<String> testcaseNames) {
		UniquenessCalculation uniqueness = new UniquenessCalculation();
		// camel case tokenization: testCaseName -> [test, case, name]
		HashMap<String, List<String>> splittedTestNames = uniqueness.tokenizeCamelCase(testcaseNames);
		// the word are embedded in vectors
		HashMap<String, Integer[]> wordToVectors = uniqueness.vectorize(splittedTestNames);

		/*
		 * Iterate over all vectors and calculate the cosine similarity between these.
		 * Sum up the values and later store the sum mapped to the test case name.
		 */
		HashMap<String, Double> resultSimilarity = new HashMap<String, Double>();
		for (Map.Entry<String, Integer[]> vector1 : wordToVectors.entrySet()) {
			double sumOfSimilarity = 0;
			for (Map.Entry<String, Integer[]> vector2 : wordToVectors.entrySet()) {
				// Similarity to the identical vector is not interesting
				if (vector1.getKey().equals(vector2.getKey())) {
					continue;
				}
				// Based on cosine similarity we calculate uniqueness value
				double value = uniqueness.cosineSimilarityCalc(vector1.getValue(), vector2.getValue());

				// Cosine Similarity is defined as: 0 = not similar, 1 = identical
				// BUT: We wan't uniqueness. There we invert the value
				value = 1.0 - value;

				// Sum all the value
				sumOfSimilarity += value;
			}

			// calculate mean value (Note: -1 is needed as we don't compute similarity to
			// ourselves.
			double meanSimilarity = sumOfSimilarity;
			if (wordToVectors.size() > 2) {
				meanSimilarity /= (double) (wordToVectors.size()) - 1.0;
			}
			resultSimilarity.put(vector1.getKey(), meanSimilarity);
		}
		return resultSimilarity;
	}

	/**
	 * Camel case tokenization of the given words. For example thisTestCaseName is
	 * split into [this, test, case, name]. The tokens are converted into lowercase
	 * and also filtered. For example {testing, check ... } -> test Words are also
	 * tokenized by underscore and numbers. For example: test_case_number12Package
	 * is split into [test, case, number, 12, package]
	 * 
	 * @param words the words to tokenize
	 * @return the tokenized words mapped to the untokenized original word.
	 */
	private HashMap<String, List<String>> tokenizeCamelCase(Set<String> words) {
		// stores the token to one word
		HashMap<String, List<String>> wordToSplit = new HashMap<String, List<String>>();
		for (String word : words) {
			// regular expression to split function names into tokens by:
			// * camel case notation
			// * numbers
			// * underscore
			// * dots
			String[] wordSplit = word.split("(?<!(^|[A-Z0-9]))(?=[A-Z0-9])"
					+ "|(?<!(^|[^A-Z]))(?=[0-9])|"
					+ "(?<!(^|[^0-9]))(?=[A-Za-z])"
					+ "|(?<!^)(?=[A-Z][a-z])"
					+ "|_" + "|\\.");
			
			// will store the lower case token
			List<String> lowercaseToken = new ArrayList<String>();
			for (String wordSpl : wordSplit) {
				String wordSplLow = wordSpl.toLowerCase();
				// convert synonyms to one unified name and store them in the lower case token
				// list
				if (testFilter.contains(wordSplLow) || wordSplLow.contains("test")) {
					lowercaseToken.add("test");
					continue;
				}
				if (equalFilter.contains(wordSplLow)) {
					lowercaseToken.add("equal");
					continue;
				}
				if (errorFilter.contains(wordSplLow)) {
					lowercaseToken.add("error");
					continue;
				}
				lowercaseToken.add(wordSplLow);
			}
			// remove empty tokens
			Predicate<String> filterEmpty = str -> (str.length() <= 0);
			lowercaseToken.removeIf(filterEmpty);

			// the tokenized word is mapped to their tokens
			// System.out.println("Tokens for Word '" + word + "' = " + String.join(" / ", lowercaseToken));
			wordToSplit.put(word, lowercaseToken);
		}
		return wordToSplit;
	}

	/**
	 * Calculate the cosine similarity between two vectors in a multidimensional
	 * space. The cosine is applied to the ankle between the two vectors.
	 * 
	 * @param v1 vector 1
	 * @param v2 vector 2
	 * @return the cosine similarity
	 */
	private double cosineSimilarityCalc(Integer[] v1, Integer[] v2) {
		// Check if vectors are equal size and contain at minimum one element
		if (v1.length != v2.length || v1.length < 1 || v2.length < 1) {
			return 0;
		}

		double upperSum = 0; // sum of multiplied components of the vectors v1 and v2
		double v1SqtSum = 0; // square-rooted sum of squared components of the vector v1
		double v2SqtSum = 0; // square-rooted sum of squared components of the vector v2

		/*
		 * Go over all components of the vector. Because v1.length = v2.length it
		 * sufficient to iterate over the length of one vector. The rest follows the
		 * formula of the cosine similarity.
		 */
		for (int i = 0; i < v1.length; i++) {
			upperSum += v1[i] * v2[i];
			v1SqtSum += Math.pow(v1[i], 2);
			v2SqtSum += Math.pow(v2[i], 2);
		}
		v1SqtSum = Math.sqrt(v1SqtSum);
		v2SqtSum = Math.sqrt(v2SqtSum);

		// Calculate COS(angle) value, avoid division by 0
		double denominator = v1SqtSum * v2SqtSum;
		double cosinus;
		if (denominator == 0) {
			cosinus = 0.0;
		} else {
			cosinus = upperSum / denominator;
		}
		return cosinus;
	}

	/**
	 * The word with its tokens is converted into a vector. The components of the
	 * vectors are either 0 or 1 (or the number of occurrences). 0 = a word does not
	 * contain the token 1 = a word contains the token 1x 2 = a word contains the
	 * token 2x ...
	 * 
	 * Example: word1 = [hello, word, is, an, example] word2 = [this, is, an, test]
	 * allTokens = [hello, word, is, an, example, this, test] vector of word1 = [ 1
	 * , 1 , 1 , 1, 1 , 0 , 0 ] vector of word2 = [ 0 , 0 , 1 , 1, 0 , 1 , 1 ]
	 * 
	 * @param splittedWords words mapped to their token
	 * @return word mapped to the corresponding vector
	 */
	private HashMap<String, Integer[]> vectorize(HashMap<String, List<String>> splittedWords) {
		// allTokens contains all tokens of all words
		// first use a set to store all words to avoid duplicates
		HashSet<String> tokenSet = new HashSet<String>();
		for (List<String> tokens : splittedWords.values()) {
			tokenSet.addAll(tokens);
		}

		// after that store in an ArrayList to have an order for the vector
		ArrayList<String> allTokens = new ArrayList<String>();
		allTokens.addAll(tokenSet);
		// System.out.println("allTokens = " + String.join(", ", allTokens));

		/*
		 * Iterate all words and assign the number of occurrences to each vector element
		 */
		HashMap<String, Integer[]> vectors = new HashMap<String, Integer[]>();
		int vectorLength = allTokens.size();
		for (Map.Entry<String, List<String>> entry : splittedWords.entrySet()) {
			String testName = entry.getKey();
			Integer[] vector = new Integer[vectorLength];

			// Iterate all words
			int idx = 0;
			for (String token : allTokens) {
				int noOfOccurs = Collections.frequency(entry.getValue(), token);
				vector[idx] = noOfOccurs;
				idx++;
			}
			vectors.put(testName, vector);
		}
		// for (Map.Entry<String, Integer[]> entry : vectors.entrySet()) {
		// System.out.println("testName = " + entry.getKey() + ", vector = " +
		// Arrays.toString(entry.getValue()));
		// }
		return vectors;
	}

}
