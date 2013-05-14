package tartar.common;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.regex.Pattern;

import tartar.common.data_models.TreeNode;

/**
 * TARTAR is Copyright 2004-2005 - University of Karlsruhe (UKA) & Jozef Stefan Institute (JSI).
 * 
 * @Author: Aleksander Pivk Redistribution and use in source and binary forms, with or without modification, are
 *          permitted provided that the following conditions are met: 1. Redistributions of source code must retain the
 *          above copyright notice, this list of conditions and the following disclaimer. 2. Redistributions in binary
 *          form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the
 *          documentation and/or other materials provided with the distribution. THIS SOFTWARE IS PROVIDED BY UKA & JSI
 *          "AS IS" AND ANY EXPRESSED OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 *          MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL UKA or JSI NOR ITS
 *          EMPLOYEES BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 *          (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
 *          OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 *          LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE,
 *          EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

/**
 * <p>
 * Title:
 * </p>
 * <p>
 * Description: A synactic category (i.e. 12345 = LargeNumber) is assigned to particular token
 * </p>
 * <p>
 * Copyright: Copyright (c) 2003
 * </p>
 * <p>
 * Company: IJS& AIFB
 * </p>
 * 
 * @author Aleksander Pivk
 * @version 1.0
 */
@SuppressWarnings({ "unchecked", "static-access" })
public class SyntacticCategorization
{
	protected transient Pattern				pattern;
	public static final int					cNoSyntacticGroups	= 14;
	public static final String				ALL_PUNCTUATIONS	= "!\"#$%&'()*+,-./:;?@[\\]^_`{|}~";
	protected final static String[]			SYNTACTIC_GROUPS	= new String[] { "TOKEN", "PUNCTUATION", "HTML",
			"ALPHANUMERIC", "ALPHA", "NUMBER", "ALL_UPPERCASE", "ALL_LOWERCASE", "FIRST_UPPERCASE", "MIXEDCASE",
			"SMALL_NUMBER", "MEDIUM_NUMBER", "LARGE/FLOAT_NUMBER", "CURRENCY" };
	protected Vector<String>				vStrings;
	protected static int[]					counter;
	protected CategoryHierarchyTree[]		categTree;
	protected Hashtable<String, int[]>		htTokens;

	public static final ArrayList<String>	listALPHA			= new ArrayList<String>();
	public static final ArrayList<String>	listNUMERIC			= new ArrayList<String>();

	public static final int					TOKEN				= 0;

	public static final int					PUNCT				= 1;
	public static final int					HTML				= 2;
	public static final int					ALPHA_NUM			= 3;

	public static final int					ALPHA				= 4;
	public static final int					NUMBER				= 5;

	public static final int					ALL_UPPER			= 6;
	public static final int					ALL_LOWER			= 7;
	public static final int					FIRST_UPPER			= 8;
	public static final int					MIXED_CASE			= 9;

	public static final int					SMALL_NUM			= 10;
	public static final int					MEDIUM_NUM			= 11;
	public static final int					LARGE_NUM			= 12;
	public static final int					CURRENCY			= 13;

	private static final Pattern			PUNCT_RE			= Pattern.compile("\\p{Punct}+");							// ==
																															// //
																															// "!"#$%&'()*+,-./:;?@[\]^_`{|}~"
	private static final Pattern			ALPHA_NUM_RE		= Pattern.compile("\\p{Alnum}+");

	private static final Pattern			ALPHA_RE			= Pattern.compile("\\p{Alpha}+");
	private static final Pattern			ALL_UPPER_RE		= Pattern.compile("\\p{Upper}+");
	private static final Pattern			ALL_LOWER_RE		= Pattern.compile("\\p{Lower}+");
	private static final Pattern			FIRST_UPPER_RE		= Pattern.compile("\\p{Upper}\\p{Lower}+");

	private static final Pattern			NUMBER_RE			= Pattern.compile("\\d+");
	private static final Pattern			SMALL_NUM_RE		= Pattern.compile("\\d{1,2}");
	private static final Pattern			MEDIUM_NUM_RE		= Pattern.compile("\\d{3,4}");
	private static final Pattern			LARGE_NAMBER_RE1	= Pattern.compile("\\d{5,}");
	private static final Pattern			LARGE_NAMBER_RE2	= Pattern.compile("([0-9]+[.,:/-\\\\]?)+[0-9]+");
	private static final Pattern			CURRENCY_RE1		= Pattern.compile("\\p{Sc}");
	private static final Pattern			CURRENCY_RE2		= Pattern
																		.compile("\\p{Sc}?[0-9]+([.,]?[0-9]*)*\\p{Sc}?");

	public SyntacticCategorization()
	{
		vStrings = new Vector<String>();
		htTokens = new Hashtable<String, int[]>();
		counter = new int[cNoSyntacticGroups];
		categTree = new CategoryHierarchyTree[cNoSyntacticGroups];
		initializeCounter();
		setCategoryHierarchyTree();
		listALPHA.add(String.valueOf(ALPHA));
		listALPHA.add(String.valueOf(ALL_UPPER));
		listALPHA.add(String.valueOf(ALL_LOWER));
		listALPHA.add(String.valueOf(FIRST_UPPER));
		listALPHA.add(String.valueOf(MIXED_CASE));
		listALPHA.trimToSize();
		listNUMERIC.add(String.valueOf(ALPHA_NUM));
		listNUMERIC.add(String.valueOf(NUMBER));
		listNUMERIC.add(String.valueOf(SMALL_NUM));
		listNUMERIC.add(String.valueOf(MEDIUM_NUM));
		listNUMERIC.add(String.valueOf(LARGE_NUM));
		listNUMERIC.add(String.valueOf(CURRENCY));
		listNUMERIC.trimToSize();
	}

	public SyntacticCategorization(String string)
	{
		this(new String[] { string });
	}

	public SyntacticCategorization(String[] stringArr)
	{
		this();
		addStrings(stringArr);
	}

	// add String which consists of at least one token
	public void addString(String string)
	{
		addStrings(new String[] { string });
	}

	// add more Strings where each consists of at least one token
	public void addStrings(String[] stringArr)
	{
		for (int i = 0; i < stringArr.length; i++)
		{
			vStrings.add(stringArr[i]);
			StringTokenizer tokens = new StringTokenizer(stringArr[i]);
			while (tokens.hasMoreTokens())
			{
				String t = tokens.nextToken();
				htTokens.put(t, getCategorizationTreePathOfToken(getMostSpecificSyntacticCategory(t)));
			}
		}
	}

	/**
	 * Construction of a category hierarchy tree...
	 * 
	 * @return - stored in a categTree[]
	 */
	private void setCategoryHierarchyTree()
	{
		CategoryHierarchyTree nodeTOKEN = new CategoryHierarchyTree(TOKEN);
		CategoryHierarchyTree nodePUNCT = new CategoryHierarchyTree(PUNCT);
		CategoryHierarchyTree nodeALPHA_NUM = new CategoryHierarchyTree(ALPHA_NUM);
		CategoryHierarchyTree nodeHTML = new CategoryHierarchyTree(HTML);
		CategoryHierarchyTree nodeALPHA = new CategoryHierarchyTree(ALPHA);
		CategoryHierarchyTree nodeNUMBER = new CategoryHierarchyTree(NUMBER);
		CategoryHierarchyTree nodeALL_UPPER = new CategoryHierarchyTree(ALL_UPPER);
		CategoryHierarchyTree nodeALL_LOWER = new CategoryHierarchyTree(ALL_LOWER);
		CategoryHierarchyTree nodeFIRST_UPPER = new CategoryHierarchyTree(FIRST_UPPER);
		CategoryHierarchyTree nodeMIXED_CASE = new CategoryHierarchyTree(MIXED_CASE);
		CategoryHierarchyTree nodeSMALL_NUM = new CategoryHierarchyTree(SMALL_NUM);
		CategoryHierarchyTree nodeMEDIUM_NUM = new CategoryHierarchyTree(MEDIUM_NUM);
		CategoryHierarchyTree nodeLARGE_NUM = new CategoryHierarchyTree(LARGE_NUM);
		CategoryHierarchyTree nodeCURRENCY = new CategoryHierarchyTree(CURRENCY);

		nodeTOKEN.linkNode(null, null, nodePUNCT);

		nodePUNCT.linkNode(nodeTOKEN, nodeALPHA_NUM, null);
		nodeALPHA_NUM.linkNode(nodeTOKEN, nodeHTML, nodeALPHA);
		nodeHTML.linkNode(nodeTOKEN, null, null);

		nodeALPHA.linkNode(nodeALPHA_NUM, nodeNUMBER, nodeALL_UPPER);
		nodeNUMBER.linkNode(nodeALPHA_NUM, null, nodeSMALL_NUM);

		nodeALL_UPPER.linkNode(nodeALPHA, nodeALL_LOWER, null);
		nodeALL_LOWER.linkNode(nodeALPHA, nodeFIRST_UPPER, null);
		nodeFIRST_UPPER.linkNode(nodeALPHA, nodeMIXED_CASE, null);
		nodeMIXED_CASE.linkNode(nodeALPHA, null, null);

		nodeSMALL_NUM.linkNode(nodeNUMBER, nodeMEDIUM_NUM, null);
		nodeMEDIUM_NUM.linkNode(nodeNUMBER, nodeLARGE_NUM, null);
		nodeLARGE_NUM.linkNode(nodeNUMBER, nodeCURRENCY, null);
		nodeCURRENCY.linkNode(nodeNUMBER, null, null);

		categTree[TOKEN] = nodeTOKEN;
		categTree[PUNCT] = nodePUNCT;
		categTree[ALPHA_NUM] = nodeALPHA_NUM;
		categTree[HTML] = nodeHTML;
		categTree[ALPHA] = nodeALPHA;
		categTree[NUMBER] = nodeNUMBER;
		categTree[ALL_UPPER] = nodeALL_UPPER;
		categTree[ALL_LOWER] = nodeALL_LOWER;
		categTree[FIRST_UPPER] = nodeFIRST_UPPER;
		categTree[MIXED_CASE] = nodeMIXED_CASE;
		categTree[SMALL_NUM] = nodeSMALL_NUM;
		categTree[MEDIUM_NUM] = nodeMEDIUM_NUM;
		categTree[LARGE_NUM] = nodeLARGE_NUM;
		categTree[CURRENCY] = nodeCURRENCY;
	}

	public int isCategory1ParentOfCategory2(int category1, int category2)
	{
		int x = 0;
		if (category1 >= 0 && category1 < cNoSyntacticGroups && category2 >= 0 && category2 < cNoSyntacticGroups)
		{
			TreeNode parent = categTree[category1];
			TreeNode tmp = categTree[category2];
			while (tmp.getParent() != null)
			{
				tmp = tmp.getParent();
				x++;
				if (tmp.equals(parent))
					return x;
			}
		}
		return Integer.MIN_VALUE;
	}

	public boolean isCategory1SiblingOfCategory2(int category1, int category2)
	{
		if (category1 >= 0 && category1 < cNoSyntacticGroups && category2 >= 0 && category2 < cNoSyntacticGroups)
		{
			TreeNode sibling = categTree[category1];
			TreeNode tmp = categTree[category2];
			if (tmp.getParent() != null)
				tmp = tmp.getParent();
			else
				return false;
			if (tmp.getChild() != null)
				tmp = tmp.getChild();
			else
				return false;
			if (tmp.equals(sibling))
				return true;
			while (tmp.getSibling() != null)
			{
				tmp = tmp.getSibling();
				if (tmp.equals(sibling))
					return true;
			}
		}
		return false;
	}

	public int isCategory1ChildOfCategory2(int category1, int category2)
	{
		return isCategory1ParentOfCategory2(category2, category1);
	}

	/**
	 * @param token least string token that has been priorly added
	 * @return list of token types (from most specific to most general)
	 */
	public int[] getCategorizationTreePathOfKnownToken(String token)
	{
		if (token != null)
		{
			if (htTokens.containsKey(token))
				return (int[]) htTokens.get(token);
		}
		return null;
	}

	public int[] getCategorizationTreePathOfUnknownToken(String token)
	{
		if (token != null)
			return getCategorizationTreePathOfToken(getMostSpecificSyntacticCategory(token));
		return null;
	}

	/**
	 * @param tokenType least general syntactical category of a token
	 * @return list of token types (from most specific to most general)
	 */
	public int[] getCategorizationTreePathOfToken(int tokenType)
	{
		if (tokenType >= 0 && tokenType < cNoSyntacticGroups)
		{
			int[] cat = new int[10];
			cat[0] = tokenType;
			int count = 1;
			CategoryHierarchyTree node = categTree[tokenType];
			// while (node.getParent()!=null &&
			// ((CategoryHierarchyTree)node.getParent()).categoryType!=TOKEN) {
			while (node.getParent() != null)
			{
				node = (CategoryHierarchyTree) node.getParent();
				cat[count++] = node.categoryType;
			}
			int[] catReturn = new int[count];
			for (int i = 0; i < count; i++)
				catReturn[i] = cat[i];
			/*
			 * for (int i=count-1; i>=0; i--) catReturn[count-i-1]=cat[i];
			 */return catReturn;
		}
		return null;
	}

	public static int getMostSpecificSyntacticCategory(String token)
	{
		int x = TOKEN;
		if (ALPHA_NUM_RE.matcher(token).matches())
		{
			x = ALPHA_NUM;
			if (ALPHA_RE.matcher(token).matches())
			{
				x = ALPHA;
				if (FIRST_UPPER_RE.matcher(token).matches())
					x = FIRST_UPPER;
				else if (ALL_LOWER_RE.matcher(token).matches())
					x = ALL_LOWER;
				else if (ALL_UPPER_RE.matcher(token).matches())
					x = ALL_UPPER;
				else
					x = MIXED_CASE;
			} else if (NUMBER_RE.matcher(token).matches())
			{
				x = NUMBER;
				if (SMALL_NUM_RE.matcher(token).matches())
					x = SMALL_NUM;
				else if (MEDIUM_NUM_RE.matcher(token).matches())
					x = MEDIUM_NUM;
				else if (LARGE_NAMBER_RE1.matcher(token).matches())
					x = LARGE_NUM;
			}
		} else if (LARGE_NAMBER_RE2.matcher(token).matches())
			x = LARGE_NUM;
		else if (CURRENCY_RE2.matcher(token).matches())
			x = CURRENCY;
		else if (CURRENCY_RE1.matcher(token).matches())
			x = CURRENCY;
		else if (PUNCT_RE.matcher(token).matches())
			x = PUNCT;
		counter[x]++;
		return x;
	}

	public int[] getCategorizationCounter()
	{
		return counter;
	}

	public int countTokens()
	{
		int x = 0;
		for (int i = 0; i < cNoSyntacticGroups; i++)
			x += counter[i];
		return x;
	}

	public static String getNameOfSyntacticCategory(int x)
	{
		return SYNTACTIC_GROUPS[x];
	}

	public String[] getArrayOfStringsWithTokens()
	{
		String[] s = new String[vStrings.size()];
		System.arraycopy(vStrings, 0, s, 0, vStrings.size());
		return s;
	}

	private void initializeCounter()
	{
		htTokens = new Hashtable<String, int[]>();
		for (int i = 0; i < cNoSyntacticGroups; i++)
			counter[i] = 0;
	}

	public static final int	MAX_DIFF	= 10;

	public int getMostSpecificCommonCategory(int a, int b)
	{
		if (a >= 0 && a < cNoSyntacticGroups && b >= 0 && b < cNoSyntacticGroups)
		{
			int[] aList = getCategorizationTreePathOfToken(a);
			int[] bList = getCategorizationTreePathOfToken(b);
			for (int i = 0; i < aList.length; i++)
				for (int j = 0; j < bList.length; j++)
					if (aList[i] == bList[j])
						return aList[i];
		}
		return TOKEN; // should never happen, for TOKEN is always a common parent
	}

	public int getMostSpecificCommonCategory(int[] a)
	{
		int max = Integer.MAX_VALUE;
		for (int i = 0; i < a.length; i++)
		{
			for (int j = i; j < a.length; j++)
			{
				int tmp = getMostSpecificCommonCategory(a[i], a[j]);
				max = (max < tmp) ? max : tmp;
			}
		}
		return max;
	}

	public int getSyntacticDistanceOfTwoCategories(int a, int b)
	{
		if (a >= 0 && a < cNoSyntacticGroups && b >= 0 && b < cNoSyntacticGroups)
		{
			int[] aList = getCategorizationTreePathOfToken(a);
			int[] bList = getCategorizationTreePathOfToken(b);
			for (int i = 0; i < aList.length; i++)
				for (int j = 0; j < bList.length; j++)
					if (aList[i] == bList[j])
						return i + j;
		}
		return MAX_DIFF; // should never happen, for ROOT is always a common parent
	}

	/**
	 * Each string is split into tokens and then categorized.
	 * 
	 * @param listTokens (either as LIST of String[] or String (not yet tokenized))
	 * @return listToken is modified if input as String,
	 * @return list of corresponding SYNTACTIC CATEGORIES
	 */
	public static ArrayList<int[]> categorizeTokens(ArrayList<Object> listTokens)
	{
		ArrayList<int[]> listAllStringsCategories = new ArrayList<int[]>();
		ArrayList<Integer> listOneStringCategories = new ArrayList<Integer>();
		ArrayList<String> listOneStringTokens = new ArrayList<String>();
		ArrayList<Object> listCopyInputTokens = (ArrayList<Object>) listTokens.clone();
		SyntacticCategorization sc = new SyntacticCategorization();

		listTokens.clear();

		StringTokenizer st;
		for (int i = 0; i < listCopyInputTokens.size(); i++)
		{

			if (listCopyInputTokens.get(i) instanceof String)
			{
				String text = (String) listCopyInputTokens.get(i);
				st = new StringTokenizer(text);
				while (st.hasMoreTokens())
				{
					String s = st.nextToken();
					int cat = sc.getMostSpecificSyntacticCategory(s);
					if (cat != TOKEN)
					{
						listOneStringTokens.add(s);
						listOneStringCategories.add(new Integer(cat));
					} else
					{
						// System.out.println("Here 1");
						// cat==TOKEN
						splitTokenCategorizedAsTOKEN_TYPE(s, listOneStringCategories, listOneStringTokens);
					}
				}
			} else if (listCopyInputTokens.get(i) instanceof String[])
			{
				String[] tokens = (String[]) listCopyInputTokens.get(i);
				for (int j = 0; j < tokens.length; j++)
				{
					int cat = sc.getMostSpecificSyntacticCategory(tokens[j]);
					if (cat != sc.TOKEN)
					{
						listOneStringTokens.add(tokens[j]);
						listOneStringCategories.add(new Integer(cat));
					} else
					{
						// System.out.println("Here 2");
						// cat==TOKEN
						splitTokenCategorizedAsTOKEN_TYPE(tokens[j], listOneStringCategories, null);
					}
				}
			}

			String[] tokens = new String[listOneStringTokens.size()];
			for (int k = 0; k < tokens.length; k++)
				tokens[k] = (String) listOneStringTokens.get(k);
			listTokens.add(tokens);
			listOneStringTokens.clear();

			int[] categories = new int[listOneStringCategories.size()];
			for (int k = 0; k < categories.length; k++)
				categories[k] = ((Integer) listOneStringCategories.get(k)).intValue();
			listAllStringsCategories.add(categories);
			listOneStringCategories.clear();
		}
		return listAllStringsCategories;
	}

	private static void splitTokenCategorizedAsTOKEN_TYPE(String token, ArrayList<Integer> listOneStringCategories,
			ArrayList<String> listOneStringTokens)
	{
		// SyntacticCategorization sc = new SyntacticCategorization();
		java.util.regex.Pattern PUNCT_RE = java.util.regex.Pattern.compile("\\p{Punct}+");
		StringBuffer sb = new StringBuffer();
		ArrayList<String> list = new ArrayList<String>();
		list.add(token);
		java.util.regex.Matcher matcher;
		StringTokenizer st1;
		while (list.size() > 0)
		{
			// System.out.println("Before remove: " + list);
			String ss = (String) list.get(0);
			matcher = PUNCT_RE.matcher(ss);

			while (matcher.find())
			{
				// System.out.println("within the while loop 1");
				matcher.appendReplacement(sb, " " + matcher.group() + " ");
			}
			matcher.appendTail(sb);
			list.remove(ss);
			// System.out.println("After remove: " + ss + "\t" + list);
			st1 = new StringTokenizer(sb.toString());
			while (st1.hasMoreTokens())
			{
				// System.out.println("within the while loop 2");
				String s1 = st1.nextToken();
				int cat1 = getMostSpecificSyntacticCategory(s1);
				if (cat1 != TOKEN)
				{
					if (listOneStringTokens != null)
						listOneStringTokens.add(s1);
					listOneStringCategories.add(new Integer(cat1));
				} else
					list.add(s1);
			}
		}
	}

	public static String getTokenRepresentedAccordingToCategory(String token, int category)
	{
		if (category == ALL_UPPER)
			return token.toUpperCase();
		else if (category == FIRST_UPPER)
			return token.substring(0, 1).toUpperCase() + token.substring(1).toLowerCase();
		else
			return token.toLowerCase();
	}

	public static void main(String[] args)
	{
		args = new String[2];
		args[0] = "research";
		args[1] = "director";
		if (args.length < 1)
		{
			System.err.println("usage: SyntacticClassModule token(s)");
			return;
		}
		// SyntacticCategorization sc = new SyntacticCategorization();
		// int x;
		// StringBuffer sb = new StringBuffer();
		// System.out.println("-------------------------------------------------------------------");
		// for (int i = 0; i < args.length; i++)
		// {
		// System.out
		// .println((i + 1) + ".Token \"" + args[i] + "\" belongs to Syntactic Class: ["
		// + (x = sc.getMostSpecificSyntacticCategory(args[i])) + "-"
		// + sc.getNameOfSyntacticCategory(x) + "]");
		// sb.append(args[i] + " ");
		// }

		// System.out.println("-------------------------------------------------------------------");

		// sc = new SyntacticCategorization(sb.toString());
		// for (int i = 0; i < args.length; i++)
		// {
		// int[] catList = sc.getCategorizationTreePathOfKnownToken(args[i]);
		// System.out.println((i + 1) + ". " + "token: \"" + args[i] + "\"");
		// for (int j = 0; j < catList.length; j++)
		// System.out.println("   -> " + sc.getNameOfSyntacticCategory(catList[j]) + " [" + catList[j] + "]");
		// }

		// System.out.println("-------------------------------------------------------------------");
		// int[] counter = sc.getCategorizationCounter();
		// for (int i = 0; i < sc.cNoSyntacticGroups; i++)
		// {
		// if (counter[i] > 0)
		// System.out.println("Syntactic Group [" + i + "-" + sc.getNameOfSyntacticCategory(i) + "] has occured '"
		// + counter[i] + "' times!");
		// }
		// System.out.println("Number of all tokens = " + sc.countTokens());
		// System.out.println("-------------------------------------------------------------------");
	}

}

class CategoryHierarchyTree extends TreeNode
{
	int	categoryType;

	public CategoryHierarchyTree()
	{
		this(-1);
	}

	public CategoryHierarchyTree(int categoryType)
	{
		this(null, null, null, categoryType);
	}

	public CategoryHierarchyTree(TreeNode parent, TreeNode sibling, TreeNode child, int categoryType)
	{
		super(parent, sibling, child);
		this.categoryType = categoryType;
	}

}
