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
	
	private String deleteRedundant(String s) {
		int pos = s.length()-1;
		StringBuilder sb = new StringBuilder();
		sb.append(s);
		while (pos >= 0) {
			if ((sb.charAt(pos) < 'a') || (sb.charAt(pos) > 'z')) {
				sb.deleteCharAt(pos);
			}
			pos--;
		}
		return sb.toString();
	}
	
	public void addRecord(String record, int index) {
		int pos = 0;
		record = removeAccents(record);
		record = record.toLowerCase();
		record = deleteRedundant(record);
		Node actual = root;		
		
		while (pos < record.length()) {
			actual = actual.getChild(record.charAt(pos) - 'a');
			pos++;
		}
		actual.addIndex(index);
	}
	
	public ArrayList<Integer> searchName(String name, int mistakes) {
		
		name = removeAccents(name);
		name = name.toLowerCase();
		name = deleteRedundant(name);
		
		return search(name, mistakes, this.root);
	}
	
	public ArrayList<Integer> suggestiveSearch(String name) {
		
		name = removeAccents(name);
		name = name.toLowerCase();
		name = deleteRedundant(name);
	
		return sugSearch(name, this.root);
		
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
		
		public boolean hasChild(int n) {
			if (this.nodes.get(n) == null) {
				return false;
			}
			return true;
		}
		
		public Node getChild(int n) {
			if (this.nodes.get(n) == null) {
				Node c = new Node(n, new ArrayList<Integer>());
				this.nodes.set(n, c);
				return c;
			} else {
				return this.nodes.get(n);
			}
		}
		
		public void addIndex(int index) {
			indexes.add(index);
		}
		
		public ArrayList<Integer> getIndexes() {
			return indexes;
		}
		
	}
	
}