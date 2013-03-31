package sk.svt.mapamatfyzu;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.InputMismatchException;
import java.util.Scanner;

import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.view.Menu;
import android.view.View;
import android.view.Window;

import org.osmdroid.util.GeoPoint;

public class MainActivity extends Activity {
	
	SearchTree tree;
	ArrayList<ArrayList<String>> teacherNames;
	ArrayList<ArrayList<Double>> teacherPositions;

	private ArrayList<Object> parseInput(String s) {
		ArrayList<Object> res = new ArrayList<Object>();
		int index = 1;
		if (s == null) {
			return null;
		}
		
		boolean fl = false;
		
		while (index < s.length()) {
			if (s.charAt(index-1) == '#') {
				fl = true;
			} else {
				fl = false;
			}
			
			String tmp = new String();
			while ((index < s.length()) && ((s.charAt(index) != ' ') && (s.charAt(index) != '#'))) {
				tmp += s.charAt(index);
				index++;
			}
			if (fl) {
				Double ftmp = new Double(Double.parseDouble(tmp));
				res.add(ftmp);
			} else {
				res.add(tmp);	
			}
			index++;
		}
		return res;
		
	}	
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	requestWindowFeature(Window.FEATURE_NO_TITLE);
        super.onCreate(savedInstanceState);
        
        String filename = "names.txt";
        
        tree = new SearchTree();
		teacherNames = new ArrayList<ArrayList<String>>();
		teacherPositions = new ArrayList<ArrayList<Double>>();
        
		try {
			Scanner in = new Scanner(this.getAssets().open(filename));
			String line = "";
			int count = 0;
			while (in.hasNextLine() && (!line.equals("#begin"))) {
				if (line == null) {
					throw new InputMismatchException("No beginning found in the resource file " + filename + " !");
				}
				line = in.nextLine();
			}
			if (in.hasNextLine()) {
				line = in.nextLine();
			}
			while (in.hasNextLine() && (!line.equals("#end"))) {
				ArrayList<Object> tmp = new ArrayList<Object>();
				tmp = parseInput(line);
				ArrayList<String> tmpnames = new ArrayList<String>();
				ArrayList<Double> tmppositions = new ArrayList<Double>();
				for (int i = 0; i < tmp.size(); i++) {
					if (tmp.get(i).getClass() != Double.class) {
						tree.addRecord(tmp.get(i).toString(), count);
						tmpnames.add(tmp.get(i).toString());
					} else {
						tmppositions.add((Double) tmp.get(i));
					}
				}
				teacherNames.add(tmpnames);
				teacherPositions.add(tmppositions);
				count++;
				line = in.nextLine();
			}
			if (!line.equals("#end")) {
				throw new InputMismatchException("No end found in the resource file " + filename + " !");
			}
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return;
		}
        
        setContentView(R.layout.activity_main);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }
    
    public void onClick(View view) {
    	if (view.getId() == R.id.button_osm_map) {
    		Intent intent = new Intent(this, OSMDroidMapActivity.class);
    		intent.putExtra("test", 8);
    		for (int i = 0; i < teacherNames.size(); i++) {
	    		intent.putExtra("teacherNames", teacherNames);
	    		intent.putExtra("teacherPositions", teacherPositions);
	    	}
    		startActivity(intent);
    	}
    }
}