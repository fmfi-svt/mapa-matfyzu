package sk.svt.mapamatfyzu;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.InputMismatchException;
import java.util.Scanner;

import android.os.Bundle;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.Window;

import org.osmdroid.util.GeoPoint;

public class MainActivity extends Activity {
	
	ArrayList<ArrayList<String>> teacherNames;
	ArrayList<ArrayList<Double>> teacherPositions;
	ArrayList<Integer> teacherFloors;
	private boolean createNewFile;
	Scanner in;
	PrintStream out;

	private ArrayList<Object> parseInput(String s) {
		ArrayList<Object> res = new ArrayList<Object>();
		int index = 1;
		if (s == null) {
			return null;
		}
		
		boolean fl = false;
		boolean in = false;
		
		while (index < s.length()) {
			if (s.charAt(index-1) == '#') {
				fl = true;
			} else {
				fl = false;
			}
			if (s.charAt(index-1) == '$') {
				in = true;
			} else {
				in = false;
			}
			
			String tmp = new String();
			while ((index < s.length()) && (s.charAt(index) != ' ') && (s.charAt(index) != '#') && (s.charAt(index) != '$')) {
				tmp += s.charAt(index);
				index++;
			}
			if (fl) {
				Double ftmp = new Double(Double.parseDouble(tmp));
				res.add(ftmp);
			} else if (in) {
				Integer itmp = new Integer(Integer.parseInt(tmp));
				res.add(itmp);
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
        
		teacherNames = new ArrayList<ArrayList<String>>();
		teacherPositions = new ArrayList<ArrayList<Double>>();
		teacherFloors = new ArrayList<Integer>();
        setContentView(R.layout.activity_main);
        createNewFile = false;
        
        try {
			FileInputStream fis = openFileInput(filename);
			in = new Scanner(fis);
			Log.d("File load","File: "+filename+" found");
		} catch (FileNotFoundException e1) {
			// TODO Auto-generated catch block
			Log.d("File load","Resource file not found - creating new");
			createNewFile = true;
		}
		try {
			// Build the teacher names and positions from resource file
			if (createNewFile) {
				in = new Scanner(this.getAssets().open(filename));
				FileOutputStream fos = openFileOutput(filename, Context.MODE_PRIVATE);
				out = new PrintStream(fos);
			}
			String line = "";
			int count = 0;
			
			// Skip comments
			while (in.hasNextLine() && (!line.equals("#begin"))) {
				if (line == null) {
					throw new InputMismatchException("No beginning found in the resource file " + filename + " !");
				}
				line = in.nextLine();
				if (createNewFile) out.println(line);
			}
			if (in.hasNextLine()) {
				line = in.nextLine();
				if (createNewFile) out.println(line);
			}
			
			// Parse the input in the file into ArrayLists
			while (in.hasNextLine() && (!line.equals("#end"))) {
				ArrayList<Object> tmp = new ArrayList<Object>();
				tmp = parseInput(line);
				ArrayList<String> tmpnames = new ArrayList<String>();
				ArrayList<Double> tmppositions = new ArrayList<Double>();
				for (int i = 0; i < tmp.size(); i++) {
					if (tmp.get(i).getClass() == Integer.class) {
						Log.d("Loading teachers","Integer found");
						teacherFloors.add((Integer) tmp.get(i));
					} else if (tmp.get(i).getClass() == Double.class) {
						Log.d("Loading teachers","Double found");
						tmppositions.add((Double) tmp.get(i));
					} else {
						tmpnames.add(tmp.get(i).toString());
						Log.d("Loading teachers","String found");
					}
				}
				
				teacherNames.add(tmpnames);
				teacherPositions.add(tmppositions);
				count++;
				line = in.nextLine();
				if (createNewFile) out.println(line);
			}
			
			// There is no #end marker found in the file, it is probably broken
			if (!line.equals("#end")) {
				throw new InputMismatchException("No end found in the resource file " + filename + " !");
			}
			if (createNewFile) {
				while (in.hasNextLine()) {
					out.println(line);
				}
			}
			in.close();
			if (createNewFile) out.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return;
		}
		
	//	filename = getFilesDir().getPath().toString()+"/names.txt";
		
		for (int i = 0; i < teacherFloors.size(); i++) {
			Log.d("Loading teachers", ""+teacherFloors.get(i));
		}
		
		Intent intent = new Intent(this, OSMDroidMapActivity.class);
		for (int i = 0; i < teacherNames.size(); i++) {
			intent.putExtra("filename", filename);
    		intent.putExtra("teacherNames", teacherNames);
    		intent.putExtra("teacherPositions", teacherPositions);
    		intent.putExtra("teacherFloors", teacherFloors);
    	}
		startActivity(intent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }
    
    public void onClick(View view) {
    	// If user cicks on a button, we need to send names and positions to the map activity
    /*	if (view.getId() == R.id.button_osm_map) {
    		Intent intent = new Intent(this, OSMDroidMapActivity.class);
    		for (int i = 0; i < teacherNames.size(); i++) {
	    		intent.putExtra("teacherNames", teacherNames);
	    		intent.putExtra("teacherPositions", teacherPositions);
	    	}
    		startActivity(intent);
    	}
    	
    	if (view.getId() == R.id.button_edit) {
    		Intent intent = new Intent(this, OSMDroidMapActivity.class);
    		// Send extra data, indicating that the user wants to edit data
    		intent.putExtra("edit", 1);
    		for (int i = 0; i < teacherNames.size(); i++) {
    			intent.putExtra("teacherNames", teacherNames);
    			intent.putExtra("teacherPositions", teacherPositions);
    		}
    		startActivity(intent);
    	}
    	*/
    }
}