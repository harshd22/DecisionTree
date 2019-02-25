//took some help from a friend
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class DecisionTree {

	int numCategories;
	int numAtts;
	List<String> categoryNames;
	List<String> attNames;
	List<String> attNamesCopy;
	List<Instance> allInstances;
	private Node root;
	int correct = 0;
	private int totalLiveInstances;
	private int totalDieInstances;
	private int correctLiveInstances;
	private int correctDieInstances;

	DecisionTree(String trainingFile, String testFile) {
		attNamesCopy = new ArrayList<>();
		readDataFile(trainingFile);
		for (String attribute : attNames) {
			attNamesCopy.add(attribute);
		}
		root = buildTree(allInstances, attNamesCopy);

		testTree(testFile);
		System.out.println("live : " + correctLiveInstances + " correct out of " + totalLiveInstances);	
		System.out.println("die : " + correctDieInstances + " correct out of " + totalDieInstances + "\n");
		double Accuracy = (double)(correctDieInstances+correctLiveInstances)*100/(double)(totalDieInstances+totalLiveInstances);
		System.out.println("Accuracy : " + Accuracy + "% \n" );
		root.report("\t");

	}

	private void testTree(String testFile) {
		totalLiveInstances = 0;
		totalDieInstances = 0;
		correctLiveInstances = 0;
		correctDieInstances = 0;
		List<Instance> testInstances;
		try {
			Scanner din = new Scanner(new File(testFile));
			din.nextLine();
			din.nextLine();
			testInstances = readInstances(din);

			for (Instance instance : testInstances) {
				findTestInstancesCategory(root, instance);

			}

			for (Instance instance : testInstances) {
				if (instance.getCategory() == 0) {
					totalLiveInstances++;
				} else {
					totalDieInstances++;
				}

				if (instance.getCategory() == 0 && instance.isCorrect) {
					correctLiveInstances++;

				} else if (instance.getCategory() == 1 && instance.isCorrect) {

					correctDieInstances++;
				}
			}
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	private void findTestInstancesCategory(Node node, Instance instance) {

		if (!(node instanceof leafNode)) {
			if (instance.getAtt(node.getAttribute())) {
				findTestInstancesCategory(node.left, instance);
			} else {
				findTestInstancesCategory(node.right, instance);
			}
		}

		if (node instanceof leafNode && instance.getCategory() == ((leafNode) node).getCategory()) {
			instance.setCorrect(true);
			correct++;
		}

	}

	private Node buildTree(List<Instance> instances, List<String> attributes) {

		if (instances.isEmpty()) {
			int category1 = 0;
			int category2 = 0;
			for (Instance instance : allInstances) {
				if (instance.getCategory() == 0) {
					category1++;
				} else
					category2++;
			}

			if (category1 >= category2) {
				return new leafNode(0, (double) category1 * 100 / allInstances.size());
			} else
				return new leafNode(1, (double) category1 * 100 / allInstances.size());

		} else if (isPure(instances)) {
			return new leafNode(instances.get(0).getCategory(), 100);
		} else if (attributes.isEmpty()) {

			int category1 = 0;
			int category2 = 0;
			for (Instance instance : instances) {
				if (instance.getCategory() == 0) {
					category1++;
				} else
					category2++;
			}

			if (category1 > category2) {
				return new leafNode(0, (double) category1 * 100 / (double) instances.size());
			} else {
				return new leafNode(1, (double) category2 * 100 / (double) instances.size());
			}

		} else {

			int bestAttribute = 0;
			double impurity = Double.MAX_VALUE;
			List<Instance> bestTrueInst = new ArrayList<>();
			List<Instance> bestFalseInst = new ArrayList<>();
			List<Instance> trueInst;
			List<Instance> falseInst;
			int attrIndex = 0;
			for (String attribute : attributes) {

				trueInst = new ArrayList<>();
				falseInst = new ArrayList<>();
				for (Instance instance : instances) {
					if (instance.getAtt(attrIndex)) {
						trueInst.add(instance);
					} else {
						falseInst.add(instance);
					}
				}

				double truePurity = checkImpurity(trueInst);
				double falsePurity = checkImpurity(falseInst);
				double newImpurity = ((double) trueInst.size() / instances.size()) * truePurity
						+ ((double) falseInst.size() / instances.size()) * falsePurity;

				if (newImpurity < impurity) {
					impurity = newImpurity;
					bestAttribute = attrIndex;
					bestFalseInst = falseInst;
					bestTrueInst = trueInst;
				}

				attrIndex++;
			}

			attributes.remove(bestAttribute);
			Node left = buildTree(bestTrueInst, attributes);
			Node right = buildTree(bestFalseInst, attributes);

			return new Node(left, right, bestAttribute);

		}

	}

	private boolean isPure(List<Instance> instances) {

		for (Instance instance : instances) {
			if (instances.get(0).getCategory() != instance.getCategory()) {
				return false;
			}
		}

		return true;
	}

	private double checkImpurity(List<Instance> Inst) {
		int category0 = 0;
		int category1 = 0;

		for (Instance instance : Inst) {
			if (instance.getCategory() == 0) {
				category0++;
			} else {
				category1++;
			}
		}

		return (double) category1 / (double) Inst.size() * (double) category0 / (double) Inst.size();
	}

	private void readDataFile(String fname) {
		/*
		 * format of names file: names of categories, separated by spaces names of
		 * attributes category followed by true's and false's for each instance
		 */
		System.out.println("Reading data from file " + fname);
		try {
			Scanner din = new Scanner(new File(fname));

			categoryNames = new ArrayList<String>();
			for (Scanner s = new Scanner(din.nextLine()); s.hasNext();)
				categoryNames.add(s.next());
			numCategories = categoryNames.size();
			System.out.println(numCategories + " categories");

			attNames = new ArrayList<String>();
			for (Scanner s = new Scanner(din.nextLine()); s.hasNext();)
				attNames.add(s.next());
			numAtts = attNames.size();
			System.out.println(numAtts + " attributes");

			allInstances = readInstances(din);
			din.close();
		} catch (IOException e) {
			throw new RuntimeException("Data File caused IO exception");
		}
	}

	private List<Instance> readInstances(Scanner din) {
		/* instance = classname and space separated attribute values */
		List<Instance> instances = new ArrayList<Instance>();
		String ln;
		while (din.hasNext()) {
			Scanner line = new Scanner(din.nextLine());
			instances.add(new Instance(categoryNames.indexOf(line.next()), line));
		}
		System.out.println("Read " + instances.size() + " instances");
		return instances;
	}

	private class Instance {

		private int category;
		private List<Boolean> vals;
		private boolean isCorrect;

		public Instance(int cat, Scanner s) {
			category = cat;
			vals = new ArrayList<Boolean>();
			while (s.hasNextBoolean())
				vals.add(s.nextBoolean());
		}

		public boolean getAtt(int index) {
			return vals.get(index);
		}

		public int getCategory() {
			return category;
		}

		public String toString() {
			StringBuilder ans = new StringBuilder(categoryNames.get(category));
			ans.append(" ");
			for (Boolean val : vals)
				ans.append(val ? "true  " : "false ");
			return ans.toString();
		}

		public boolean isCorrect() {
			return isCorrect;
		}

		public void setCorrect(boolean isCorrect) {
			this.isCorrect = isCorrect;
		}

	}

	public class Node {

		private int attribute;

		private Node left;
		private Node right;

		public Node() {

		}

		public Node(Node left, Node right, int attribute) {
			this.left = left;
			this.right = right;
			this.attribute = attribute;
		}

		public void report(String indent) {
			System.out.format("%s%s = True:\n", indent, attNames.get(attribute));
			left.report(indent + "   ");
			System.out.format("%s%s = False:\n", indent, attNames.get(attribute));
			right.report(indent + "   ");
		}

		public int getAttribute() {
			return attribute;
		}

		public Node getLeft() {
			return left;
		}

		public Node getRight() {
			return right;
		}

	}

	public class leafNode extends Node {

		private int category;
		private double probab;

		public leafNode(int category, double probab) {
			super();
			this.category = category;
			this.probab = probab;
		}

		public int getCategory() {
			return category;
		}

		@Override
		public void report(String indent) {
			System.out.format("%sClass %s, prob=%4.2f\n", indent, categoryNames.get(category), probab);
		}

	}

	public static void main(String args[]) {
		String training = args[0];
		String test = args[1];
		new DecisionTree(training , test);

	}

}
