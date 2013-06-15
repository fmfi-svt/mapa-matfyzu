package sk.svt.mapamatfyzu;

import android.annotation.SuppressLint;
import java.text.Normalizer;
import java.text.Normalizer.Form;
import java.util.ArrayList;

public class SearchTree {
	
	Node root;
	
	public SearchTree() {
		root = new Node(-1, new ArrayList<Integer>());
	}
	
	// Remove diacritics
	@SuppressLint("NewApi")
	public static String removeAccents(String text) {
	    return text == null ? null
	        : Normalizer.normalize(text, Form.NFD)
	            .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
	}
	
	private String normalize(String s) {
		int pos = s.length()-1;
		StringBuilder sb = new StringBuilder();
		s = removeAccents(s);
		s = s.toLowerCase();
		sb.append(s);
		while (pos >= 0) {
			if ((sb.charAt(pos) < 'a') || (sb.charAt(pos) > 'z')) {
				sb.deleteCharAt(pos);
			}
			pos--;
		}
		return sb.toString();
	}
	
	/*
	 * Adds a search record into a tree (appends index of the record onto the right node)
	 */
	public void addRecord(String record, int index) {
		int pos = 0;
		record = normalize(record);
		Node current = root;		
		
		while (pos < record.length()) {
			current = current.getChild(record.charAt(pos) - 'a');
			pos++;
		}
		current.addIndex(index);
	}
	
	public ArrayList<Integer> searchName(String name, int mistakes) {
		
		name = normalize(name);
		
		return search(name, mistakes, this.root);
	}

	private ArrayList<Integer> getIndexesFromSubtree(Node sub) {
		ArrayList<Integer> res = new ArrayList<Integer>();
		res.addAll(sub.getIndexes());
		for (int i = 0; i < 26; i++) {
			if (sub.hasChild(i)) {
				res.addAll(getIndexesFromSubtree(sub.getChild(i)));
			}
		}
		return res;
	}
	
	/*
	 * Suggestive search, returns list of indexes matching the string
	 * 
	 * Algorithm gets to the node representing the end of the string
	 * and returns all indexes in each node from its subtree
	 */
	public ArrayList<Integer> suggestiveSearch(String name) {
		
		name = normalize(name);
	
		int index = 0;
		
		Node currentNode = this.root;
		
		while (index < name.length()) {
			if (currentNode.hasChild(name.charAt(index) - 'a')) {
				currentNode = currentNode.getChild(name.charAt(index) - 'a');
				index++;
			} else {
				return new ArrayList<Integer>();
			}
		}
		
		return getIndexesFromSubtree(currentNode);
		
	}
	private ArrayList<Integer> sugSearch(String name, Node actual) {
		
		if (name.length() > 0) {
			StringBuilder sb = new StringBuilder();
			sb.append(name);
			sb.deleteCharAt(0);
			int first = name.charAt(0) - 'a';
			if (actual.hasChild(first)) {
				return sugSearch(sb.toString(), actual.getChild(first));
			} else {
				return new ArrayList<Integer>();
			}
		} else {
			ArrayList<Integer> ret = actual.getIndexes();
			for (int i = 0; i < 26; i++) {
				if (actual.hasChild(i)) {
					ret.addAll(sugSearch(name, actual.getChild(i)));
				}
			}
			return ret;
		}
		
	}
	
	private ArrayList<Integer> search(String name, int mistakes, Node actual) {
		
		ArrayList<Integer> ret = new ArrayList<Integer>();
		
		if (name.length() == 0) {
			return actual.getIndexes();
		}
		
		StringBuilder sb = new StringBuilder();
		sb.append(name);
		sb.deleteCharAt(0);
		String nametail = sb.toString();
		int first = name.charAt(0) - 'a';
		
		if (actual.hasChild(first)) {
			ret = search(nametail, mistakes, actual.getChild(first));
		}
		
		if (mistakes > 0) {
			for (int i = 0; i < 26; i++) {
				if (i != first) {
					if (actual.hasChild(i)) {
						ret.addAll(search(nametail, mistakes-1, actual.getChild(i)));
					}
				}
			}
		}
		
		return ret;
	}
	
	/*
	 * Every node represents one letter. String is represented by chain of Nodes, where every
	 * successor is child of the predecessor.
	 */
	private class Node {
		ArrayList<Node> nodes;
		ArrayList<Integer> indexes;
		int value;
		
		public Node(int _value, ArrayList<Integer> index) {
			value = _value;
			indexes = index;
			nodes = new ArrayList<Node>();
			for (int i = 0; i < 26; i++) {
				nodes.add(null);
			}
		}
		
		/*
		 * Returns true if this node has a child number n, false otherwise
		 */
		public boolean hasChild(int n) {
			if ((n > 26) || (n < 0)) {
				return false;
			}
			if (this.nodes.get(n) == null) {
				return false;
			}
			return true;
		}
		
		/*
		 * Returns a child with desired number, creates new if necessary.
		 * Returns null if desired number is out of range.
		 */
		public Node getChild(int n) {
			if ((n > 26) || (n < 0)) {
				return null;
			}
			if (this.nodes.get(n) == null) {
				Node child = new Node(n, new ArrayList<Integer>());
				this.nodes.set(n, child);
				return child;
			} else {
				return this.nodes.get(n);
			}
		}
		
		/*
		 * Adds a single index into the list of indexes on this node
		 */
		public void addIndex(int index) {
			indexes.add(index);
		}
		
		/*
		 * Returns indexes on this node
		 */
		public ArrayList<Integer> getIndexes() {
			return indexes;
		}
		
	}
	
}