package tartar.common.data_models;

/**
 * <p>
 * Title:
 * </p>
 * <p>
 * Description:
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

public abstract class TreeNode
{
	protected TreeNode	parent;
	protected TreeNode	sibling;	// next sibling
	protected TreeNode	child;		// first child

	public TreeNode(TreeNode parent, TreeNode sibling, TreeNode child)
	{
		this.parent = parent;
		this.sibling = sibling;
		this.child = child;
	}

	/**
	 * Link element to related nodes.
	 */
	public void linkNode(TreeNode parent, TreeNode sibling, TreeNode child)
	{
		this.parent = parent;
		this.sibling = sibling;
		this.child = child;
	}

	/**
	 * Get element's parent.
	 * 
	 * @return element that contains this element, or null if at top-level.
	 */
	public TreeNode getParent()
	{
		return parent;
	}

	/**
	 * Get element's next sibling.
	 * 
	 * @return element that follows this element, or null if at end of parent's children.
	 */
	public TreeNode getSibling()
	{
		return sibling;
	}

	/**
	 * Get element's first child.
	 * 
	 * @return first element contained by this element, or null if no children.
	 */
	public TreeNode getChild()
	{
		return child;
	}

	/**
	 * Return next element in an inorder walk of the tree, assuming this element and its children have been visited.
	 * 
	 * @return next element
	 */
	public TreeNode getNext()
	{
		if (sibling != null)
			return sibling;
		else if (parent != null)
			return parent.getNext();
		else
			return null;
	}

	public static void getLeafNodesOnly(TreeNode treeNode, java.util.ArrayList<TreeNode> list)
	{
		for (TreeNode t = treeNode; t != null; t = t.getSibling())
		{
			if (t.getChild() == null)
				list.add(t);
			else
				getLeafNodesOnly(t.getChild(), list);
		}
	}

	public static void getSiblingNodesOnly(TreeNode firstChild, java.util.ArrayList<TreeNode> list)
	{
		if (firstChild != null)
		{
			list.add(firstChild);
			while (firstChild.getSibling() != null)
			{
				firstChild = firstChild.getSibling();
				list.add(firstChild);
			}
		}
	}

	public static void levelizeTree(TreeNode treeNode, java.util.Hashtable<TreeNode, Integer> hsLevels, int level)
	{
		for (TreeNode t = treeNode; t != null; t = t.getSibling())
		{
			hsLevels.put(t, new Integer(level));
			if (t.getChild() != null)
				levelizeTree(t.getChild(), hsLevels, level + 1);
		}
	}

	public static void setAsChildOrChildSiblingNode(TreeNode parent, TreeNode node)
	{
		if (parent.getChild() == null)
		{
			parent.linkNode(parent.getParent(), parent.getSibling(), node);
		} else
		{
			TreeNode sibling = parent.getChild();
			if (sibling.getSibling() == null)
				sibling.linkNode(sibling.getParent(), node, sibling.getChild());
			else
			{
				while (sibling.getSibling() != null)
					sibling = sibling.getSibling();
				sibling.linkNode(sibling.getParent(), node, sibling.getChild());
			}
		}
		node.linkNode(parent, node.getSibling(), node.getChild());
	}

	public static void deleteNode(TreeNode node)
	{
		TreeNode successor = null;
		if (node.getSibling() != null)
			successor = node.getSibling();
		TreeNode ancestor = node.getParent();
		if (ancestor.getChild().equals(node)) // PARENT
			ancestor.linkNode(ancestor.getParent(), ancestor.getSibling(), successor);
		else
		{
			ancestor = ancestor.getChild(); // SIBLING
			while (ancestor.getSibling() != null)
			{ // SIBLING
				if (ancestor.getSibling().equals(node))
				{
					ancestor.linkNode(ancestor.getParent(), successor, ancestor.getChild());
					break;
				}
				ancestor = ancestor.getSibling();
			}
		}
	}

	public static boolean isNode1SiblingOfNode2(TreeNode node1, TreeNode node2)
	{
		if (node1 != null && node2 != null)
		{
			TreeNode tmp = node2;
			if (tmp.getParent() != null)
				tmp = tmp.getParent();
			else
				return false;
			if (tmp.getChild() != null)
				tmp = tmp.getChild();
			else
				return false;
			if (tmp.equals(node1))
				return true;
			while ((tmp = tmp.getSibling()) != null)
			{
				if (tmp.equals(node1))
					return true;
			}
		}
		return false;
	}

}
