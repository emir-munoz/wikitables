package tartar.algorithm_DataProg;

// KRISTINE LERMAN - DATAPROG algorithm implementation
// JAIR 2003 article

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.ListIterator;

import tartar.common.SyntacticCategorization;
import tartar.common.data_models.TreeNode;

@SuppressWarnings({ "unchecked", "static-access" })
public class DataProg
{
	public static final double				ALPHA	= 0.06;
	ArrayList<TreeNode>						listTree;
	ArrayList<Object>						listTokenStrings;		// list holds TEXT (String[]) of tokens
	ArrayList<int[]>						listTokenCategories;	// list holds CATEGORIES (int[]) of tokens
	Hashtable<Object, Object>				htProbabilities;		// each token has its probability calculated out of
																	// examples
	DataProgNode							rootNode;
	private static SyntacticCategorization	sc;

	public DataProg()
	{
		sc = new SyntacticCategorization();
		// listTokenStrings = (ArrayList<Object>) tokens.clone();
		// listTokenCategories = sc.categorizeTokens(listTokenStrings);
		// htProbabilities = calculateTokenProbabilities(listTokenStrings);
		// listTree = new ArrayList<DataProgNode>();
		// initRoodNode();
	}

	public void clear()
	{
		listTokenStrings = new ArrayList<Object>();
		listTokenCategories = new ArrayList<int[]>();
		htProbabilities = new Hashtable<Object, Object>();
		listTree = new ArrayList<TreeNode>();
	}

	public void setTokens(final ArrayList<String> tokens)
	{
		clear();

		listTokenStrings = (ArrayList<Object>) tokens.clone();
		listTokenCategories = sc.categorizeTokens(listTokenStrings);
		htProbabilities = calculateTokenProbabilities(listTokenStrings);
		listTree = new ArrayList<TreeNode>();
		initRoodNode();
	}

	private void initRoodNode()
	{
		rootNode = new DataProgNode();
		listTree.add(rootNode);
		rootNode.setTokenObject(new Integer(SyntacticCategorization.TOKEN));
		rootNode.setTokenPosition(-1);
		rootNode.setPattern(new ArrayList<Object>());
		// rootNode covers all examples/instances

		ArrayList<Integer> list = new ArrayList<Integer>();
		for (int i = 0; i < listTokenStrings.size(); i++)
			list.add(new Integer(i));
		rootNode.setExamples(list);
	}

	private void calculateSyntacticCategoryProbabilities(Hashtable<Object, Integer> htTokens, String token)
	{
		int[] tokenCategories = sc.getCategorizationTreePathOfUnknownToken(token);
		for (int i = 0; i < tokenCategories.length; i++)
		{
			String tokenCatName = sc.getNameOfSyntacticCategory(tokenCategories[i]);
			int x;
			if (htTokens.containsKey(tokenCatName))
				x = ((Integer) htTokens.get(tokenCatName)).intValue() + 1;
			else
				x = 1;
			htTokens.put(tokenCatName, new Integer(x));
		}
	}

	public Hashtable<Object, Object> calculateTokenProbabilities(ArrayList<Object> listTokens)
	{
		Hashtable<Object, Object> htProb = new Hashtable<Object, Object>();
		Hashtable<Object, Integer> htCatProb = new Hashtable<Object, Integer>();
		int count = 0;
		for (int i = 0; i < listTokens.size(); i++)
		{
			String[] tokens = (String[]) listTokens.get(i);
			count += tokens.length;
			for (int j = 0; j < tokens.length; j++)
			{
				int x;
				if (htProb.containsKey(tokens[j].toLowerCase().trim()))
					x = ((Integer) htProb.get(tokens[j].toLowerCase().trim())).intValue() + 1;
				else
					x = 1;
				htProb.put(tokens[j].toLowerCase().trim(), new Integer(x));
				calculateSyntacticCategoryProbabilities(htCatProb, tokens[j]);
			}
		}
		// double sum=0;
		// double checkSum=0;
		Enumeration<Object> enum1 = htProb.keys();
		while (enum1.hasMoreElements())
		{
			Object key = enum1.nextElement();
			double f = ((Integer) htProb.get(key)).doubleValue() / count;
			// checkSum+=((Integer)htProb.get(key)).doubleValue();
			// sum+=f;
			htProb.put(key, new Double(f));
		}

		enum1 = htCatProb.keys();
		while (enum1.hasMoreElements())
		{
			Object key = enum1.nextElement();
			double f = ((Integer) htCatProb.get(key)).doubleValue() / count;
			htProb.put(key, new Double(f));
		}

		return htProb;
	}

	private ArrayList<Integer> extractExamplesThatFollowToken(ArrayList<Integer> examples, Object tokenObject,
			int tokenPos)
	{
		String tokenString = null;
		int tokenCategory = -Integer.MIN_VALUE;
		ArrayList<Integer> listNewExamples = new ArrayList<Integer>();

		// is tokenObject a Token(String) or Category(Integer)
		if (tokenObject instanceof String)
			tokenString = (String) tokenObject;
		else if (tokenObject instanceof Integer)
			tokenCategory = ((Integer) tokenObject).intValue();

		int noExamples = examples.size();
		for (int i = 0; i < noExamples; i++)
		{ // walk over all the examples
			int iCurrentExample = ((Integer) examples.get(i)).intValue();
			String[] arrExampleToken = (String[]) listTokenStrings.get(iCurrentExample);
			int[] arrExampleCateg = (int[]) listTokenCategories.get(iCurrentExample);

			if (tokenPos < arrExampleToken.length)
			{ // current string[] has enough elements
				if (tokenString != null)
				{
					String compareToken = arrExampleToken[tokenPos];
					if (compareToken.toLowerCase().equals(tokenString.toLowerCase()))
						listNewExamples.add(new Integer(iCurrentExample));
				} else if (tokenCategory != Integer.MIN_VALUE)
				{
					int exampleCategory = arrExampleCateg[tokenPos];
					int synDistance = sc.getSyntacticDistanceOfTwoCategories(exampleCategory, tokenCategory);
					// int commonCategory = sc.getMostSpecificCommonCategory(exampleCategory,
					// tokenCategory);
					// if ((synDistance<=2 && commonCategory>=3) || (synDistance<=1 &&
					// commonCategory>0 && commonCategory<4))
					// distance of Nodes <=2 && common category >=4
					// distance of Nodes <=1 && commonCategory >0 && common category <4
					if (synDistance == 0 || sc.isCategory1ChildOfCategory2(exampleCategory, tokenCategory) >= 0)
						listNewExamples.add(new Integer(iCurrentExample));
				}
			}
		}
		return listNewExamples;
	}

	// create a CHILD, check SIGNIFICANCE and IF OK -> CONNECT it to the parent
	private void addNewChild(DataProgNode parent, Object tokenObj, String htProbabilities_Key)
	{
		DataProgNode child = new DataProgNode();
		child.setTokenObject(tokenObj);
		child.setExamples(extractExamplesThatFollowToken(parent.getExamples(), tokenObj, parent.getTokenPosition() + 1));
		child.setTokenPosition(parent.getTokenPosition() + 1);
		ArrayList<Object> pattern = (ArrayList<Object>) parent.getPattern().clone();
		pattern.add(tokenObj);
		child.setPattern(pattern);
		if (StatisticalSignificanceClass.isSignificant(parent.getNumberOfExamples(), child.getNumberOfExamples(),
				((Double) htProbabilities.get(htProbabilities_Key)).doubleValue(), ALPHA))
		{
			// if (StatisticalSignificanceClass.significant(parent.getNumberOfExamples(),
			// child.getNumberOfExamples(),
			// ((Double)htProbabilities.get(htProbabilities_Key)).doubleValue(), ALPHA)) {
			listTree.add(child);
			TreeNode.setAsChildOrChildSiblingNode(parent, child);
		}

	}

	protected void createChildrenNodes(DataProgNode parent)
	{
		// ArrayList listNewExamples = new ArrayList();
		ArrayList<Integer> examples = parent.getExamples();
		HashSet<String> hsAddedTokenObjects = new HashSet<String>();
		int noExamples = parent.getNumberOfExamples();

		int iCurrentTokenPos = parent.getTokenPosition() + 1;
		for (int i = 0; i < noExamples; i++)
		{ // walk over all the examples still in the game at this node
			int iCurrentExample = ((Integer) examples.get(i)).intValue();
			String[] arrExampleToken = (String[]) listTokenStrings.get(iCurrentExample);
			int[] arrExampleCateg = (int[]) listTokenCategories.get(iCurrentExample);

			if (iCurrentTokenPos < arrExampleToken.length)
			{ // current string[] has enough elements
				// first TOKEN is added,
				String tokenObj = arrExampleToken[iCurrentTokenPos];
				if (!hsAddedTokenObjects.contains(tokenObj.toLowerCase()))
				{
					addNewChild(parent, tokenObj, tokenObj.toLowerCase());
					hsAddedTokenObjects.add(tokenObj.toLowerCase());

					// then also ALL IT'S CATEGORIES are added
					int[] cat = sc.getCategorizationTreePathOfToken(arrExampleCateg[iCurrentTokenPos]);
					for (int k = 0; k < cat.length - 1; k++)
					{
						Integer tokenObjInt = new Integer(cat[k]);
						String key = sc.getNameOfSyntacticCategory(cat[k]);
						if (!hsAddedTokenObjects.contains(key))
						{
							addNewChild(parent, tokenObjInt, key);
							hsAddedTokenObjects.add(key);
						}
					}
				}
			}
		}
	}

	/**
	 * Test which of the two PATTERNS is MORE/LESS GENERAL
	 * 
	 * @param pattern1
	 * @param pattern2
	 * @return 0 - neither pattern is more/less general
	 * @return 1 - pattern1 is MORE GENERAL than pattern2
	 * @return -1 - pattern1 is LESS GENERAL than pattern2 (MORE SPECIFIC)
	 */
	private int isPattern1MoreGeneralThanPattern2(Object tokenObj1, Object tokenObj2)
	{
		if ((tokenObj1 instanceof String) && (tokenObj2 instanceof String))
			return 0;
		else if ((tokenObj1 instanceof String) && (tokenObj2 instanceof Integer))
			return -1;
		else if ((tokenObj1 instanceof Integer) && (tokenObj2 instanceof String))
			return 1;
		else
		{ // both tokenObj are INTEGER -> represent category
			int value1 = ((Integer) tokenObj1).intValue();
			int value2 = ((Integer) tokenObj2).intValue();
			if (sc.isCategory1SiblingOfCategory2(value1, value2))
				return 0;
			else if (sc.isCategory1ParentOfCategory2(value1, value2) > 0)
				return 1;
			else if (sc.isCategory1ChildOfCategory2(value1, value2) > 0)
				return -1;
		}
		return 0;
	}

	private String getKeyValueOfTokenObject(Object tokenObject)
	{
		@SuppressWarnings("unused")
		String s = "";
		if (tokenObject instanceof Integer)
			return s = sc.getNameOfSyntacticCategory(((Integer) tokenObject).intValue());
		else
			return s = ((String) tokenObject).toLowerCase();
	}

	protected static double getCoverageOfPattern(String pattern)
	{
		java.util.regex.Pattern PUNCT_RE = java.util.regex.Pattern.compile("<(\\d+)/(\\d+)>");
		java.util.regex.Matcher m = PUNCT_RE.matcher(pattern);
		double d = 0;
		while (m.find())
			d = Double.parseDouble(m.group(1)) / Double.parseDouble(m.group(2));
		return d;
	}

	protected void pruningNodes(DataProgNode firstChild)
	{
		for (DataProgNode iNode = firstChild; iNode != null; iNode = (DataProgNode) iNode.getSibling())
		{
			if (iNode.getSibling() != null)
			{
				for (DataProgNode jNode = (DataProgNode) iNode.getSibling(); jNode != null; jNode = (DataProgNode) jNode
						.getSibling())
				{
					int iGeneral = isPattern1MoreGeneralThanPattern2(iNode.getTokenObject(), jNode.getTokenObject());

					DataProgNode generalNode = null, specificNode = null;
					if (iGeneral > 0)
					{ // t is MORE GENERAL than nodeSibling
						generalNode = iNode;
						specificNode = jNode;
					} else if (iGeneral < 0)
					{ // t is LESS GENERAL (MORE SPECIALIZED) than nodeSibling
						generalNode = jNode;
						specificNode = iNode;
					}

					if (generalNode != null && specificNode != null)
					{
						int N = generalNode.getNumberOfExamples() - specificNode.getNumberOfExamples();
						String key = getKeyValueOfTokenObject(generalNode.getTokenObject());
						// String generalPattern = convertPatternToString(generalNode);
						// String specificPattern = convertPatternToString(specificNode);
						if (!StatisticalSignificanceClass.isSignificant(
								((DataProgNode) generalNode.getParent()).getNumberOfExamples(), N,
								((Double) htProbabilities.get(key)).doubleValue(), ALPHA))
						{
							float f = (float) N / ((DataProgNode) generalNode.getParent()).getNumberOfExamples();
							if (f <= 0.1)
							{// || ((generalNode.getTokenObject() instanceof Integer) &&
								// (specificNode.getTokenObject() instanceof Integer))) {
								// 1. ocena 10% - dolocena po obcutku
								// 2. if ((N==0) || ((generalNode.getTokenObject() instanceof
								// Integer) && (specificNode.getTokenObject() instanceof Integer)))
								// {
								// 3. if (N==0) {
								listTree.remove(generalNode);
								TreeNode.deleteNode(generalNode);
								if (generalNode.equals(iNode))
									break;
							}
						} else
						{
							listTree.remove(specificNode);
							TreeNode.deleteNode(specificNode);
							if (specificNode.equals(iNode))
								break;
						}
					}
				}
			}
		}
	}

	private String convertPatternToString(DataProgNode node)
	{
		StringBuffer sb = new StringBuffer();
		ListIterator<Object> listIter = node.pattern.listIterator();
		while (listIter.hasNext())
		{
			Object tokenObj = listIter.next();
			if (tokenObj instanceof Integer)
				sb.append(sc.getNameOfSyntacticCategory(((Integer) tokenObj).intValue()) + " ");
			else
				sb.append((String) tokenObj + " ");
		}
		if (sb.length() > 0)
			sb.append("<" + node.getNumberOfExamples() + "/" + listTokenStrings.size() + ">");
		return sb.toString().trim();
	}

	protected ArrayList<String> extractPatternsFromTree(DataProgNode parent)
	{
		ArrayList<String> significantPatterns = new ArrayList<String>();
		ArrayList<TreeNode> listTreeNodes = new ArrayList<TreeNode>();
		listTreeNodes.add(parent);

		for (int k = 0; k < listTreeNodes.size(); k++)
		{
			DataProgNode parentNode = (DataProgNode) listTreeNodes.get(k); // Q
			if (parentNode.getChild() == null && !significantPatterns.contains(convertPatternToString(parentNode)))
				significantPatterns.add(convertPatternToString(parentNode));
			else
			{
				int min = listTreeNodes.size();
				TreeNode.getSiblingNodesOnly(parentNode.getChild(), listTreeNodes);
				int max = listTreeNodes.size();
				ArrayList<TreeNode> childrenNodes = new ArrayList<TreeNode>();
				for (int i = min; i < max; i++)
				{ // for every child C of Q
					childrenNodes.clear();
					DataProgNode tmpParentNode = (DataProgNode) listTreeNodes.get(i); // C
					int iSi = 0, iC = tmpParentNode.getNumberOfExamples();
					if (tmpParentNode.getChild() != null)
					{
						TreeNode.getSiblingNodesOnly(tmpParentNode.getChild(), childrenNodes);
						for (int j = 0; j < childrenNodes.size(); j++)
							// for every child S of C
							iSi += ((DataProgNode) childrenNodes.get(j)).getNumberOfExamples();
					}
					int N = iC - iSi;
					String key = getKeyValueOfTokenObject(tmpParentNode.getTokenObject());
					if (StatisticalSignificanceClass.isSignificant(parentNode.getNumberOfExamples(), N,
							((Double) htProbabilities.get(key)).doubleValue(), ALPHA))
						// if
						// (StatisticalSignificanceClass.significant(parentNode.getNumberOfExamples(),
						// N, ((Double)htProbabilities.get(key)).doubleValue(), ALPHA))
						significantPatterns.add(convertPatternToString(tmpParentNode));
				}
			}
		}
		return significantPatterns;
	}

	public ArrayList<String> getSignificantPatterns()
	{
		return getSignificantPatterns(0.);
	}

	public ArrayList<String> getSignificantPatterns(double treshold)
	{
		// MAIN LOOP
		for (int i = 0; i < listTree.size(); i++)
		{
			DataProgNode node = (DataProgNode) listTree.get(i);
			createChildrenNodes(node);
			if (node.getChild() != null)
				pruningNodes((DataProgNode) node.getChild());
		}

		// SIGNIFICANT PATTERNS
		ArrayList<String> list = extractPatternsFromTree(rootNode);
		java.util.Collections.sort(list, new NodePatternComparator());

		if (treshold > 0.)
		{
			for (int i = 0; i < list.size(); i++)
			{
				String s = (String) list.get(i);
				if (getCoverageOfPattern(s) < treshold)
				{
					list.add(i, "-------------- BELOW TRESHOLD [" + treshold + "] -----------------");
					break;
				}
			}
			// CONSTRUCT NEW LIST WITH ONLY PATTERNS ABOVE TRESHOLD
			/*
			 * ArrayList newlist = new ArrayList(); for (int i=0; i<list.size(); i++) { String s = (String)list.get(i);
			 * if (getCoverageOfPattern(s)>=treshold) newlist.add(s); } list = newlist;
			 */
		}
		return list;
	}

	public static void main(String[] args)
	{
		ArrayList<String> listText = new ArrayList<String>();

		// listText.add("Los Angeles");
		// listText.add("Los Angeles");
		// listText.add("Pasadena");
		// listText.add("Marina Del Ray");
		// listText.add("Marina Del Ray");
		// listText.add("Marina Del Ray");
		// listText.add("Venice");
		// listText.add("New York");
		// listText.add("Goleta");
		// listText.add("Brooklyn");
		// listText.add("New York");
		// listText.add("New York");
		// listText.add("New York");
		// listText.add("New York");
		// listText.add("Buffallo");
		// listText.add("Los Angeles");
		// listText.add("Pasadena");
		// listText.add("West Hollywood");
		// listText.add("Los Angeles");
		// listText.add("Los Angeles");
		// listText.add("Venice");
		// listText.add("Marina Del Ray");
		// listText.add("Marina Del Ray");
		// listText.add("New York");
		// listText.add("New York");
		// listText.add("Los Angeles");
		// listText.add("Culver City");
		// listText.add("Marina Del Ray");
		// listText.add("Culver City");
		// listText.add("Marina Del Ray");
		// listText.add("West Hollywood");

		// listText.add("(315) 111 - 2222");
		// listText.add("(314) 211 - 3222");
		// listText.add("(314) 311 - 4222");
		// listText.add("(310) 411 - 5222");
		// listText.add("(310) 511 - 6222");
		// listText.add("(311) 611 - 7222");
		// listText.add("(311) 711 - 8222");
		// listText.add("(312) 811 - 9222");
		// listText.add("(312) 111 - 2322");
		// listText.add("(312) 211 - 2422");
		// listText.add("(312) 211 - 2522");

		// listText.add("Tokyo 4-Day");

		// listText.add("Tokyo Atami 1-Day");
		// listText.add("Tokyo Xtami 1-Day");
		// listText.add("Tokyo Ctami 1-Day");
		// listText.add("Tokyo Rrami 1-Day");
		// listText.add("Tokyo Tami 1-Day");
		// listText.add("Tokyo Ytami 1-Day");
		// listText.add("Tokyo Utami 7-Day");
		// listText.add("Tokyo Itami 8-Day");
		// listText.add("Tokyo Btami 2-Day");
		// listText.add("Tokyo Rtami 4-Day");
		// listText.add("Tokyo-Kyoto 5-Day");
		// listText.add("Tokyo Banana 5-Day");
		// listText.add("Golden Route 5-Day");
		// listText.add("Golden Route 6-Day");
		// listText.add("Golden Route 7-Day");
		// listText.add("Japan Alps 7-Day");
		// listText.add("Japanese Pottery 10-Day");
		// listText.add("World Heritage 7-Day");

		listText.add("5 Dudley Avenue");
		listText.add("5711 West Century Boulevard");
		listText.add("10835 Venice Blvd");
		listText.add("11614 Santa Monica Blvd.");
		listText.add("1544 S.La Cienega");
		listText.add("6751 Santa Monica Blvd.");
		listText.add("7912 Beverly Blvd.");
		listText.add("224 Lincoln Blvd");
		listText.add("2628 Wilshire Blvd (@ 26th St)");
		listText.add("510 Santa Monica Blvd (betw 5th&6th)");
		listText.add("3110 Main Street");
		listText.add("1315 3rd St Promenade Ste H");
		listText.add("1622 Ocean Park Blvd");
		listText.add("13723 Fiji Way @ Fisherman's Village");
		listText.add("215 Broadway");
		listText.add("10916 W Pico Blvd (1/2 Blk W Of Westwood Blvd)");
		listText.add("11829 Wilshire Blvd");
		listText.add("119 Broadway @ Ocean");
		listText.add("119 Culver Boulevard");
		listText.add("101 Broadway");
		listText.add("1928 Lincoln Boulevard");
		listText.add("12217 Santa Monica Blvd. #201");
		listText.add("3105 Washington Boulevard");
		listText.add("10032 Venice Blvd");
		listText.add("401 Santa Monica Pier");
		listText.add("3110 Santa Monica Boulevard");
		listText.add("1621 Wilshire Blvd");
		listText.add("1451 3rd Street Promenade");
		listText.add("1401 Ocean Avenue");
		listText.add("1413 5th St");

		// INITIALIZATION
		DataProg dataProg = new DataProg();

		// for (int i = 0; i < 5; i++)
		// {
		ArrayList<String> listAux = (ArrayList<String>) listText.clone();
		// System.out.println("=========================");
		dataProg.setTokens(listAux);
		// SIGNIFICANT PATTERNS
		listAux = dataProg.getSignificantPatterns(0.5);

		// ListIterator<String> listIter = listAux.listIterator();
		// while (listIter.hasNext())
		// System.out.println("Significant pattern: " + (String) listIter.next());
		// System.out.println();
		// }

	}
}

class DataProgNode extends TreeNode
{
	protected int					iTokenPos;		// POSITION to which token we refer in this node
	protected Object				tokenObject;	// TOKEN which has been used in this node
	protected ArrayList<Integer>	examples;		// all the examples that this node COVERS
	protected ArrayList<Object>		pattern;		// NODE PATTERN

	public DataProgNode()
	{
		this(null, null, null);
	}

	public DataProgNode(TreeNode parent, TreeNode sibling, TreeNode child)
	{
		super(parent, sibling, child);
	}

	public void setTokenPosition(int pos)
	{
		this.iTokenPos = pos;
	}

	public int getTokenPosition()
	{
		return iTokenPos;
	}

	public int getNumberOfExamples()
	{
		return examples.size();
	}

	public void setExamples(ArrayList<Integer> exampleSet)
	{
		this.examples = exampleSet;
	}

	public ArrayList<Integer> getExamples()
	{
		return examples;
	}

	public void setPattern(ArrayList<Object> pattern)
	{
		this.pattern = pattern;
	}

	public ArrayList<Object> getPattern()
	{
		return pattern;
	}

	public void setTokenObject(Object tokenObject)
	{
		this.tokenObject = tokenObject;
	}

	public Object getTokenObject()
	{
		return tokenObject;
	}

}

@SuppressWarnings("rawtypes")
class DataProgNodeComparator implements java.util.Comparator
{
	public int compare(Object o1, Object o2)
	{
		DataProgNode node1 = (DataProgNode) o1;
		DataProgNode node2 = (DataProgNode) o2;
		int d1 = node1.getNumberOfExamples();
		int d2 = node2.getNumberOfExamples();
		if (d1 < d2)
		{
			return 1;
		} else if (d2 < d1)
		{
			return -1;
		}
		return 0;
	}
}

@SuppressWarnings("rawtypes")
class NodePatternComparator implements java.util.Comparator
{
	public int compare(Object o1, Object o2)
	{
		double d1 = DataProg.getCoverageOfPattern((String) o1);
		double d2 = DataProg.getCoverageOfPattern((String) o2);
		if (d1 < d2)
		{
			return 1;
		} else if (d2 < d1)
		{
			return -1;
		}
		return 0;
	}

}
