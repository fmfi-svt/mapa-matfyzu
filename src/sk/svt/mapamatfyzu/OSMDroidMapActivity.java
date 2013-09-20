package sk.svt.mapamatfyzu;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.PrintStream;
import java.sql.Date;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import org.osmdroid.DefaultResourceProxyImpl;
import org.osmdroid.ResourceProxy;
import org.osmdroid.events.MapAdapter;
import org.osmdroid.events.MapListener;
import org.osmdroid.events.ScrollEvent;
import org.osmdroid.events.ZoomEvent;
import org.osmdroid.tileprovider.MapTileProviderArray;
import org.osmdroid.tileprovider.modules.MapTileModuleProviderBase;
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase;
import org.osmdroid.tileprovider.tilesource.XYTileSource;
import org.osmdroid.tileprovider.util.SimpleRegisterReceiver;
import org.osmdroid.util.BoundingBoxE6;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.overlay.ItemizedIconOverlay;
import org.osmdroid.views.overlay.ItemizedIconOverlay.OnItemGestureListener;
import org.osmdroid.views.overlay.ItemizedOverlay;
import org.osmdroid.views.overlay.OverlayItem;
import org.osmdroid.views.overlay.TilesOverlay;
import org.osmdroid.views.overlay.OverlayItem.HotspotPlace;

import org.osmdroid.views.MapController;
import org.osmdroid.views.MapView;
import org.osmdroid.views.MapView.Projection;

import android.app.Activity;
import android.app.DialogFragment;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.support.v4.app.*;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.View.OnTouchListener;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;
import android.widget.TextView;
import android.widget.Toast;

public class OSMDroidMapActivity extends Activity implements NoticeDialogFragment.NoticeDialogListener {

	FragmentManager fragmentManager;
	BoundedMapView mapView;
	MapController mapController;
	GeoPoint BorderLeftTop = new GeoPoint(48.152975, 17.068688888);
	GeoPoint BorderRightBottom = new GeoPoint(48.15184444, 17.07144166);
	ArrayList<ArrayList< GeoPoint > > floors; // List of upper left and lower right corners for floors
	ArrayList<String> floorsNumbers; // How the floors are visually represented
	ArrayList<OverlayItem> markers; // All markers on the map
	ArrayList<OverlayItem> markersInvisible; // All markers on the map with invisible picture
	Drawable marker; // Appearance of the marker used
	Drawable markerInvisible; // Invisible marker .. obviously
	ItemizedOverlay<OverlayItem> myOverlay; // Overlay used for current marker selection
	ItemizedOverlay<OverlayItem> allMarkersOverlay; // Overlay used for displaying all the markers (invisible)
	ArrayList<ArrayList<String> > teacherNames; // Every element is arraylist of names of a teacher
	ArrayList<String> teacherNamesPlain; // Every element is a string, full name of a teacher
	ArrayList<GeoPoint> teacherPositions; // Positions of the teachers
	ArrayList<Integer> teacherFloors; // Floor on which the desired teacher is
	ArrayList<Integer> alive = new ArrayList<Integer>(); // Indicates if the marker is alive
	ArrayList<OverlayItem> selectedPositions = new ArrayList<OverlayItem>(); // Positions selected for current floor
	ArrayList<OverlayItem> globalSelectedPositions = new ArrayList<OverlayItem>(); // All positions selected in search
	ArrayList<Integer> globalSelectedIndexes = new ArrayList<Integer>(); // All indexes of positions selected
	OverlayItem movedItem = null; // Indicates if item is still moved
	boolean movedItemPinned = true; // Indicates if the item, that is going to be moved, is still pinned
	boolean canBeMoved;
	ImageView movedImage; // Image that is displayed while the marker is moved
	OverlayItem editedItem = null; // Indicates if item is still edited
	long pressTime = 0; // ... does this need explanation ?
	SearchTree tree; // The tree, where all the information for search are stored
	ListView list; // List for search bar, where matching results are displayed
	ListView floorList; // List, where the floors are displayed
	ArrayList<TilesOverlay> tilesOverlays;
	EditText edit; // Search bar
	ArrayList<String> selection; // Array for "list"
	ArrayAdapter<String> selectionArrayAdapter; // Adapter for the selection array..
	OnItemGestureListener<OverlayItem> gestureListener; // Listener for taps on markers
	DefaultResourceProxyImpl defaultResourceProxyImpl; // No idea, but it's necessary
	Button floorButton; // Button to display floors
	String lastSearch; // A string that was searched the last time
	int currentFloor; // Floor that is currently displayed
	final ArrayList<Integer> indexes = new ArrayList<Integer>(); // List of indexes for results displayed for searched string
	InputMethodManager imm; // Handling the show/hide of keyboard
	boolean editMode; // Indicates, if user wanted to edit data
	boolean newItem;
	boolean timerRunning = false; // Only one timer can be running at time
	boolean dialogShown = false; // Only one dialog can be shown at a time
	boolean dialogStarted = false; // Controlls if the dialog was actually shown to the user
	int maxListSize = 3;
	ImageView editButton; // Button for switching to edit mode
	ImageView saveButton; // Button for saving edited data
	Timer timer;
	String filename; // Name of the file, where all the data is stored (can be relative path)
	int x,y; // last position of pointer
	int lastPointerCount = 1; // Number of pointers on the last touch event
	ArrayList<OverlayItem> newItems = new ArrayList<OverlayItem>(); // Recently added items (before changing the search bar)
	ArrayList<RadioButton> radioButtons = new ArrayList<RadioButton>();
	long lowestDist;
	OverlayItem lowestDistItem;
	long distTreshold = 30;
	
	private Handler handler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			if (newItem) {
				if (!dialogShown) {
					Toast.makeText(OSMDroidMapActivity.this, "Nová značka", Toast.LENGTH_SHORT).show();
					dialogShown = true;
					editedItem = movedItem;
					// Show the custom dialog for editing markers
					NoticeDialogFragment d = new NoticeDialogFragment();
					d.show(getFragmentManager(), "MarkerEdit");
					getFragmentManager().executePendingTransactions();
					addRadioButtonsListener(d);
					if (d.getDialog() == null) {
						Log.d("Tap","Dialog == null");
					} else {
						// Set the texts in edit bars corresponding to the former name and room
						EditText n = (EditText) d.getDialog().findViewById(R.id.edit_name);
						EditText r = (EditText) d.getDialog().findViewById(R.id.edit_room);
						radioButtons.set(0, (RadioButton) d.getDialog().findViewById(R.id.radioFloorButton0));
						radioButtons.set(1, (RadioButton) d.getDialog().findViewById(R.id.radioFloorButton1));
						radioButtons.set(2, (RadioButton) d.getDialog().findViewById(R.id.radioFloorButton2));
						
					//	EditText f = (EditText) d.getDialog().findViewById(R.id.edit_floor);
						n.setText("");
						r.setText("");
						for (int i = 0; i < radioButtons.size(); i++) {
							if (currentFloor == i) {
								radioButtons.get(currentFloor).setChecked(true);
							} else {
								radioButtons.get(i).setChecked(false);
							}
						}
						
						
					//	f.setText(teacherFloors.get(markers.indexOf(editedItem)).toString());
					}
					// Item is not moved, it's only being edited
					movedItem = null;
				}
			} else {
				Toast.makeText(OSMDroidMapActivity.this, "Premiestniť učiteľa " + movedItem.mTitle, Toast.LENGTH_SHORT).show();
				selectedPositions.remove(movedItem);
				mapView.getOverlays().remove(myOverlay);
				myOverlay = new ItemizedIconOverlay<OverlayItem>(selectedPositions, gestureListener, defaultResourceProxyImpl);
				mapView.getOverlays().add(myOverlay);
			 	RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) movedImage.getLayoutParams();
				Rect bounds = marker.getBounds();
				lp.setMargins(x + bounds.left, y + bounds.top, 0, 0);
				movedImage.setLayoutParams(lp);
				showView(movedImage);
				mapView.invalidate();
			}
			timerRunning = false;
		}
	};
	
	/**
	 * Parses the words divided by whitespaces in string into array of these words
	 * 
	 * @param s string to be parsed
	 * @return list of words in the string
	 */
	private ArrayList<String> parseString(String s) {
		int index = 0;
		ArrayList<String> res = new ArrayList<String>();
		while (index < s.length()) {
			
			// Ignore ALL the whitespaces !
			while ((index < s.length()) && (s.charAt(index) == ' ')) {
				index++;
			}
			String tmp = new String();
			while ((index < s.length()) && (s.charAt(index) != ' ')) {
				tmp += s.charAt(index);
				index++;
			}
			if (tmp.length() != 0) {
				res.add(tmp);				
			}
		}
		
		return res;
	}
	
	private class Pair implements Comparable {
		int key;
		int value;
		
		public Pair(int _key, int _value) {
			key = _key;
			value = _value;
		}
		
		public int compareTo(Object another) {
			// TODO Auto-generated method stub
			Pair tmp = (Pair) another;
			return this.key - tmp.getKey();
		}
		
		public int getKey() {
			return key;
		}
		
		public int getValue() {
			return value;
		}
		
	}
	
	/**
	 * Parses the string into words and searches for these words. Only best results
	 * (all the words are part of the full name) are considered.
	 * 
	 * @param search searched string
	 * @return a list of indexes into the array teacherNames that match the searched string
	 */
	private ArrayList<Integer> getPossibleSearches(String search) {
		
		ArrayList<String> strings = parseString(search);
		ArrayList<Integer> tmpindexes = new ArrayList<Integer>();
		
		// Get all the indexes for every word into one array
		for (int i = 0; i < strings.size(); i++) {
			tmpindexes.addAll(tree.suggestiveSearch(strings.get(i)));
		}
		
		ArrayList<Pair> forsort = new ArrayList<Pair>(); // for sorting
		ArrayList<Integer> res = new ArrayList<Integer>(); // result
		
		// Count how many of each index is there in the array
		while (tmpindexes.size() > 0) {
			int count = 0;
			int value = tmpindexes.get(0);
			for (int i = tmpindexes.size()-1; i >= 0; i--) {
				if (tmpindexes.get(i) == value) {
					count++;
					tmpindexes.remove(i);
				}
			}
			forsort.add(new Pair(count, value));
		}
	
	 // Consider only best matches 
		
		for (int i = 0; i < forsort.size(); i++) {
			if (forsort.get(i).getKey() >= strings.size()) {
				res.add(forsort.get(i).getValue());
			}
		}
		
		return res;
	}
	
	/**
	 * Shows all the search tools and sets listeners
	 */
	private void showSearchPanel() {
		TextView text = (TextView) findViewById(R.id.textView1);
    	text.setVisibility(text.GONE);
    	showView(floorButton);
    	showView(edit);
    	showView(list);
    	edit.setText(lastSearch);
    	edit.addTextChangedListener(new TextWatcher() {

    		// Searched string has changed -> update the list
			public void afterTextChanged(Editable s) {
				newItems.clear();
				selection.clear(); // Clear results written on the list
				indexes.clear(); // Clear corresponding indexes to general array of names
				selectedPositions.clear();
				globalSelectedPositions.clear();
				if (s.toString().isEmpty()) {
					// Search bar is empty, add all the indexes !
					for (int i = 0; i < markers.size(); i++) {
						if (alive.get(i) == 1) {
							if (editMode) {
								globalSelectedPositions.add(markers.get(i));
								globalSelectedIndexes.add(i);
								if (teacherFloors.get(i) == currentFloor) selectedPositions.add(markers.get(i));
							} else {
								globalSelectedPositions.add(markersInvisible.get(i));
								globalSelectedIndexes.add(i);
								if (teacherFloors.get(i) == currentFloor) selectedPositions.add(markersInvisible.get(i));
							}
						}
					}
					hideView(list);
				} else {
					indexes.addAll(getPossibleSearches(s.toString())); // Search for results and add them to array
					showView(list);	

					for (int i = 0; i < indexes.size(); i++) {
						if (selection.size() < maxListSize) {
							if (alive.get(indexes.get(i)) == 1) { // Maybe the marker was already removed when editing data
								globalSelectedPositions.add(markers.get(indexes.get(i)));
								globalSelectedIndexes.add(indexes.get(i));
								selection.add(teacherNamesPlain.get(indexes.get(i)));
								if (teacherFloors.get(indexes.get(i)) == currentFloor) {
									selectedPositions.add(markers.get(indexes.get(i)));	
								}
							}
						} else {
							break;
						}
					}
					
				}
				// Update overlay if exists, if not, create new
				if (myOverlay != null) {
					mapView.getOverlays().remove(myOverlay);
				}
				myOverlay = new ItemizedIconOverlay<OverlayItem>(selectedPositions, gestureListener, defaultResourceProxyImpl);
				mapView.getOverlays().add(myOverlay);
				mapView.invalidate();
				lastSearch = s.toString();
				selectionArrayAdapter.notifyDataSetChanged();
				list.refreshDrawableState();
			}

			public void beforeTextChanged(CharSequence s, int start,
					int count, int after) {
				// TODO Auto-generated method stub
				
			}

			public void onTextChanged(CharSequence s, int start,
					int before, int count) {
				// TODO Auto-generated method stub
				
			}
    		
    	});
    	list.setOnItemClickListener(new android.widget.AdapterView.OnItemClickListener() {

			public void onItemClick(AdapterView<?> arg0, View arg1,
					int arg2, long arg3) {
				// TODO Auto-generated method stub
				int counter = 0;
				// Dead markers are skipped, find first arg2 alive ones
				for (int i = 0; i < indexes.size(); i++) {
					if (alive.get(indexes.get(i)) == 1) {
						counter++;
						if (counter == arg2+1) {
							arg2 = i;
							break;
						}
					}
				}
				
				// Find the corresponding floor and move to position
				GeoPoint Middle = teacherPositions.get(indexes.get(arg2));
				/*
				for (int i = 0; i < floors.size(); i++) {
					GeoPoint tmpLT = floors.get(i).get(0);index
					GeoPoint tmpRB = floors.get(i).get(1);
					if ((Middle.getLatitudeE6() < tmpLT.getLatitudeE6()) && (Middle.getLongitudeE6() > tmpLT.getLongitudeE6()) &&
							(tmpRB.getLatitudeE6() < Middle.getLatitudeE6()) && (tmpRB.getLongitudeE6() > Middle.getLongitudeE6())) {
						mapView.setScrollableAreaLimit(new BoundingBoxE6(tmpLT.getLatitudeE6(), tmpRB.getLongitudeE6(),
						tmpRB.getLatitudeE6(), tmpLT.getLongitudeE6()));
						break;
					}
				}
				*/
				changeFloorTo(teacherFloors.get(indexes.get(arg2)));
				mapController.setCenter(teacherPositions.get(indexes.get(arg2)));
				// Set text in the search bar as full name without the room
				StringBuilder tmpname = new StringBuilder(teacherNamesPlain.get(indexes.get(arg2)));
				tmpname.delete(tmpname.lastIndexOf(" "), tmpname.length());
				edit.setText(tmpname.toString());
    			imm.hideSoftInputFromWindow(edit.getWindowToken(), 0);
    			hideView(list);
			}
    		
    	});
    	mapView.invalidate();
	}
	
	/**
	 * Hides all the search tools
	 */
	private void hideSearchPanel() {
		hideView(list);
		hideView(edit);
		TextView text = (TextView) findViewById(R.id.textView1);
    	text.setVisibility(text.VISIBLE);
    	showView(floorButton);
    	mapView.invalidate();
	}
	
	/**
	 * Shows the desired View (sets visible, clickable, enabled, activated)
	 * @param v the view to show
	 */
	private void showView(View v) {
		v.setVisibility(v.VISIBLE);
		v.setClickable(true);
		v.setActivated(true);
		v.setEnabled(true);
	}
	
	/**
	 * Hides the desired View (sets gone, not clickable nor enabled nor activated)
	 * @param v the view to hide
	 */
	private void hideView(View v) {
		v.setVisibility(v.GONE);
		v.setClickable(false);
		v.setActivated(false);
		v.setEnabled(false);
	}

	private boolean isBlank(String s) {
		if (s.isEmpty()) {
			return true;
		}
		for (int i = 0; i < s.length(); i++) {
			if (s.charAt(i) != ' ') {
				return false;
			}
		}
		return true;
	}
	
	/**
	 * Handles a positive click on the dialog window for editing marker
	 */
	public void onDialogPositiveClick(NoticeDialogFragment dialog) {
		// TODO Auto-generated method stub
		dialogShown = false;
		dialogStarted = false;
		EditText name = (EditText) dialog.getDialog().findViewById(R.id.edit_name);
		EditText room = (EditText) dialog.getDialog().findViewById(R.id.edit_room);
		radioButtons.set(0, (RadioButton) dialog.getDialog().findViewById(R.id.radioFloorButton0));
		radioButtons.set(1, (RadioButton) dialog.getDialog().findViewById(R.id.radioFloorButton1));
		radioButtons.set(2, (RadioButton) dialog.getDialog().findViewById(R.id.radioFloorButton2));
		Log.d("Positive Click", "Name: "+ name.getText().toString() + ", Room: " + room.getText().toString());
		String newName = name.getText().toString();
		String newRoom = room.getText().toString();
		int newFloor = 0;
		for (int i = 0; i < radioButtons.size(); i++) {
			if (radioButtons.get(i).isChecked()) {
				newFloor = i;
				break;
			}
		}
		Log.d("Positive Click","New Floor: " + newFloor);
		OverlayItem tmp = new OverlayItem(newName, newRoom, editedItem.getPoint());
		tmp.setMarker(marker);
		tmp.setMarkerHotspot(HotspotPlace.CENTER);
		ArrayList<String> newNameList = parseString(newName + " " + newRoom);
		if (isBlank(newRoom)) {
			newNameList.add("");
		}
		if (newItem) {
			// Item is not edited, it's a new item
			Log.d("Positive Click","Item is NEW");
			markers.add(tmp);
			for (int i = 0; i < newNameList.size()-1; i++) {
				tree.addRecord(newNameList.get(i), markers.size()-1);
			}
			teacherNames.add(newNameList);
			teacherNamesPlain.add(newName + " (" + newRoom + ")");
			teacherPositions.add(editedItem.getPoint());
			alive.add(1);
			if (currentFloor == newFloor) {
				// The item shall be displayed no matter what until the search bar changes
				selectedPositions.add(tmp);
				globalSelectedPositions.add(tmp);
				globalSelectedIndexes.add(markers.size()-1);
			}
			newItems.add(tmp);
			teacherFloors.add(newFloor);
			Log.d("Positive click - New Item","Adding a teacher: " + teacherNamesPlain.get(teacherNamesPlain.size()-1) + " on the floor nr.: " + newFloor);
			newItem = false;
		} else {
			// Item was edited, it's not new
			// Find, where the marker is in general array and selection array
			int index = markers.indexOf(editedItem);
			int index2 = globalSelectedPositions.indexOf(editedItem);
			int index3 = selectedPositions.indexOf(editedItem);
			int indexNew = newItems.indexOf(editedItem);
			globalSelectedPositions.set(index2, tmp);
			// Create new OverlayItem with desired preferences
			markers.set(index, tmp);
			// Update the search tree
			for (int i = 0; i < teacherNames.get(index).size()-1; i++) {
				tree.removeRecord(teacherNames.get(index).get(i), index);
			}
			for (int i = 0; i < newNameList.size()-1; i++) {
				tree.addRecord(newNameList.get(i), index);
			}
			// Update all arrays regarding names/rooms
			teacherNames.set(index, newNameList);
			teacherNamesPlain.set(index, newName + " (" + newRoom + ")");
			if (!edit.getText().toString().isEmpty()) {
				if (indexNew == -1) {
					// It's not a new item, it's been searched for
					selection.set(index2, newName + " (" + newRoom + ")");
				} else {
					// Update the new item
					newItems.set(indexNew, tmp);
				}
			}
			Log.d("Positive Click - Edited Item","Index2 = " + index2);
			if (newFloor != teacherFloors.get(index)) {
				
				// Teacher's floor has changed
				
				teacherFloors.set(index, newFloor);
				Log.d("Positive Click - Edited Item","Teacher's new floor is: " + newFloor);
					
				// Single edit mode --> show the marker on the new floor
				
				changeFloorTo(newFloor);
			} else {
				Log.d("Positive Click - Edited Item","Floor hasn't changed");
				if (index3 != -1) {
					selectedPositions.set(index3, tmp);
				}
			}
			selectionArrayAdapter.notifyDataSetChanged();
			list.invalidate();
		}
		// Update overlay
		mapView.getOverlays().remove(myOverlay);
		myOverlay = new ItemizedIconOverlay<OverlayItem>(selectedPositions, gestureListener, defaultResourceProxyImpl);
		mapView.getOverlays().add(myOverlay);
		mapView.invalidate();
		editedItem = null;		
	}

	/**
	 * Handles a negative click on the dialog window for editing markers,
	 * only closes the edit window.
	 */
	public void onDialogNegativeClick(NoticeDialogFragment dialog) {
		// TODO Auto-generated method stub
		dialogShown = false;
		dialogStarted = false;
		if (!newItem) {
			
			// Deleting the item
			
			if (editedItem == null) Log.d("Negative Click","editedItem == null");
			int index = markers.indexOf(editedItem); // Global index of that item
			alive.set(index, 0);
			int index2 = globalSelectedPositions.indexOf(editedItem); // Index into selection in search list
			int indexNew = newItems.indexOf(editedItem);
			globalSelectedPositions.remove(editedItem);
			globalSelectedIndexes.remove(index2);
			selectedPositions.remove(editedItem);
			if (!edit.getText().toString().isEmpty()) {
				if (indexNew == -1) {
					// It's not a new item, it's a result of the search
					selection.remove(index2);
				} else {
					newItems.remove(indexNew);
				}
			}
			selectionArrayAdapter.notifyDataSetChanged();
			mapView.getOverlays().remove(myOverlay);
			myOverlay = new ItemizedIconOverlay<OverlayItem>(selectedPositions, gestureListener, defaultResourceProxyImpl);
			mapView.getOverlays().add(myOverlay);
			mapView.invalidate();
			editedItem = null;
		}
		
		EditText name = (EditText) dialog.getDialog().findViewById(R.id.edit_name);
		EditText room = (EditText) dialog.getDialog().findViewById(R.id.edit_room);
		Log.d("Negative Click", "Name: "+ name.getText().toString() + ", Room: " + room.getText().toString());
		editedItem = null;
	}
	
	private void saveDataToFile(String file) {
		try {
			FileOutputStream fos = openFileOutput(filename, Context.MODE_PRIVATE);
			PrintStream out = new PrintStream(fos);
			out.println("#begin");
			
			
			for (int i = 0; i < teacherNames.size(); i++) {
				String tmp = "";
				if (alive.get(i) == 1) {
					for (int j = 0; j < teacherNames.get(i).size(); j++) {
						tmp += " "+teacherNames.get(i).get(j);
					}
					tmp += "#";
					tmp += Double.toString((teacherPositions.get(i).getLatitudeE6()/1E6))+"#";
					tmp += Double.toString((teacherPositions.get(i).getLongitudeE6()/1E6))+"$";
					tmp += Integer.toString((teacherFloors.get(i)));
					Log.d("Writing to file",tmp);
					out.println(tmp);
				}
			}
			out.println("#end");
			out.close();
			
			markersInvisible.clear();
			
			for (int i = 0; i < markers.size(); i++) {
				OverlayItem oitem = new OverlayItem(markers.get(i).mTitle, markers.get(i).mDescription, markers.get(i).mGeoPoint);
				oitem.setMarkerHotspot(HotspotPlace.CENTER);
				oitem.setMarker(markerInvisible);
				markersInvisible.add(oitem);
			}
			
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			Log.d("Saving Error","Could not create file");
			e.printStackTrace();
		}
	}

	protected void changeFloorTo(int fl) {
		
		// Changes tileset to desired floor tileset... (floors are numbered from 0 up)
		
		if (currentFloor != fl) {
			Log.d("Floor Click", "Floor number " + fl);
			mapView.getOverlays().remove(tilesOverlays.get(currentFloor));
			mapView.getOverlays().add(tilesOverlays.get(fl));
			mapView.getOverlays().remove(myOverlay);
			currentFloor = fl;
			mapView.invalidate();
			mapView.postInvalidate();
			int sels = edit.getSelectionStart();
			int sele = edit.getSelectionEnd();
			edit.setText(edit.getText().toString());
			imm.hideSoftInputFromWindow(edit.getWindowToken(), 0);
			edit.setSelection(sels, sele);
			hideView(list);    
			/*
			selectedPositions.clear();
			
			for (int i = 0; i < globalSelectedPositions.size(); i++) {
				if (teacherFloors.get(globalSelectedIndexes.get(i)) == currentFloor) {
					selectedPositions.add(globalSelectedPositions.get(i));
				}
			}
			myOverlay = new ItemizedIconOverlay<OverlayItem>(selectedPositions, gestureListener, defaultResourceProxyImpl);
			mapView.getOverlays().add(myOverlay);
			*/
			mapView.invalidate(); 
		}
		
	}
	
	protected void addRadioButtonsListener(NoticeDialogFragment d) {
		
		radioButtons.set(0, (RadioButton) d.getDialog().findViewById(R.id.radioFloorButton0));
		radioButtons.set(1, (RadioButton) d.getDialog().findViewById(R.id.radioFloorButton1));
		radioButtons.set(2, (RadioButton) d.getDialog().findViewById(R.id.radioFloorButton2));
	
		OnCheckedChangeListener listener = new OnCheckedChangeListener() {

			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				if (!isChecked) {
					Log.d("Checked Changed","Unchecking");
				} else {
					Log.d("Checked Changed","Unchecking old, checking new");
					for (int i = 0; i < radioButtons.size(); i++) {
						radioButtons.get(i).setChecked(false);
					}
					buttonView.setChecked(true);					
				}
			}
		};
		
		for (int i = 0; i < radioButtons.size(); i++) {
			radioButtons.get(i).setOnCheckedChangeListener(listener);
		}
		
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		Intent intent = this.getIntent();
		Bundle extras = intent.getExtras();
		super.onCreate(savedInstanceState);
		filename = (String) extras.get("filename"); // Name of the file, where all the data is stored
		
		// Set borders of the floors
		ArrayList<GeoPoint> tmpgp = new ArrayList<GeoPoint>();
		floors = new ArrayList< ArrayList < GeoPoint> >();
		tmpgp.add(BorderLeftTop);
		tmpgp.add(BorderRightBottom);
		floors.add(tmpgp);
		tmpgp = new ArrayList<GeoPoint>();
		tmpgp.add(new GeoPoint(48.3,17.2));	
		tmpgp.add(new GeoPoint(48.292,17.207));
		floors.add(tmpgp);
		tmpgp = new ArrayList<GeoPoint>();
		tmpgp.add(new GeoPoint(48.2,17.1));
		tmpgp.add(new GeoPoint(48.192, 17.107));
		floors.add(tmpgp);
		
		for (int i = 0; i < 3; i++) {
			radioButtons.add(null);
		}
		
		floorsNumbers = new ArrayList<String>( Arrays.asList("Prízemie","1.","2."));
		currentFloor = 0;
		
		marker = this.getResources().getDrawable(R.drawable.blue_dot4); // Set appearance of the markers
		markerInvisible = this.getResources().getDrawable(R.drawable.marker_invisible); // Set the marker for overview to be invisible
		
		// Rebuild the names and positions arrays from given extras
		teacherNames = new ArrayList<ArrayList<String>>();
		teacherPositions = new ArrayList<GeoPoint>();
		teacherFloors = new ArrayList<Integer>();
		teacherNamesPlain = new ArrayList<String>();
		ArrayList<ArrayList<Double>> teacherPositionsTmp = new ArrayList<ArrayList<Double>>();
		teacherNames = (ArrayList<ArrayList<String>>) extras.get("teacherNames");
		teacherFloors = (ArrayList<Integer>) extras.get("teacherFloors");
		teacherPositionsTmp = (ArrayList<ArrayList<Double>>) extras.get("teacherPositions");
		for (int i = 0; i < teacherPositionsTmp.size(); i++) {
			teacherPositions.add(new GeoPoint(teacherPositionsTmp.get(i).get(0), teacherPositionsTmp.get(i).get(1)));
			String s = new String();
			for (int j = 0; j < teacherNames.get(i).size()-1; j++) {
				s += teacherNames.get(i).get(j)+ " ";
			}
			s += "(" + teacherNames.get(i).get(teacherNames.get(i).size()-1) + ")";
			teacherNamesPlain.add(s);
			alive.add(1);
		}
		
		// Build the SearchTree
		tree = new SearchTree();
		
		for (int i = 0; i < teacherNames.size(); i++) {
			// Last string is room, we don't want it here.
			for (int j = 0; j < teacherNames.get(i).size()-1; j++) {
				tree.addRecord(teacherNames.get(i).get(j), i);
			}
		}
				
        setContentView(R.layout.activity_osmdroid_map); 
        list = (ListView) findViewById(R.id.listView1); // List, where the results are displayed
    	edit = (EditText) findViewById(R.id.editText1); // Search bar
    	floorButton = (Button) findViewById(R.id.button_floor); // Button for selecting floors
		imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
		movedImage = (ImageView) findViewById(R.id.imageView1);
		movedImage.setVisibility(movedImage.GONE);
		editButton = (ImageView) findViewById(R.id.edit_data_button);
		saveButton = (ImageView) findViewById(R.id.save_data_button);
		
		Log.d("Init", "teacherFloors size is " + teacherFloors.size());
		
		// We start from explore mode
        editMode = false;
        showView(editButton);
		hideView(saveButton);
		
        this.mapView = (BoundedMapView) findViewById(R.id.osmmapview);
        this.mapController = mapView.getController();
        
      //  mapView.setBuiltInZoomControls(true);  // Touch events don't work when zoom controls are enabled
        mapView.setMultiTouchControls(true); 
        
        mapView.getController().setZoom(18);
        
        editButton.setOnClickListener(new OnClickListener() {

			public void onClick(View v) {
				// TODO Auto-generated method stub
				editMode = true;
				int sels = edit.getSelectionStart();
				int sele = edit.getSelectionEnd();
				edit.setText(edit.getText().toString());
				edit.setSelection(sels, sele);
				imm.hideSoftInputFromWindow(edit.getWindowToken(), 0);
				hideView(list);
				hideView(editButton);
				showView(saveButton);
			}
        	
        });
        
        saveButton.setOnClickListener(new OnClickListener() {
        	
        	public void onClick(View v) {
        		// TODO Auto-generated method stub
        		editMode = false;
        		hideView(saveButton);
        		saveDataToFile(filename);
				int sels = edit.getSelectionStart();
				int sele = edit.getSelectionEnd();
				edit.setText(edit.getText().toString());
				edit.setSelection(sels, sele);
				imm.hideSoftInputFromWindow(edit.getWindowToken(), 0);
				hideView(list);
        		showView(editButton);
        	}
        });

        mapView.setOnTouchListener(new OnTouchListener() {
			
			public boolean onTouch(View v, MotionEvent event) {
				// TODO Auto-generated method stub
				boolean result = false;
				
				// Hide the keyboard
				if (event.getAction() == MotionEvent.ACTION_DOWN) {
					imm.hideSoftInputFromWindow(edit.getWindowToken(), 0);
					hideView(list);	
					result = true;
				}
				lastPointerCount = event.getPointerCount();
				if (event.getPointerCount() == 1) {
					Log.d("Touch Event", "Any");
					
					// Treshold in milliseconds between click and long click
					long pressTimeTreshold = 700;
					
					x = (int)event.getX();
					y = (int)event.getY();
					
					// If they are out of range, set them to the closest approximation
					if (y < 0) y = 0;
					if (x < 0) x = 0;
					
					if (editMode) {
						if (event.getAction() == MotionEvent.ACTION_DOWN) {
							if (!dialogShown) {
								// Find out which item was clicked on
								lowestDist = 10000;
								lowestDistItem = null;
								for (OverlayItem item : selectedPositions) {
									Point p = new Point(0,0);
									mapView.getProjection().toPixels(item.getPoint(), p);
									Point p2 = new Point(0,0);							
									mapView.getProjection().toPixels(mapView.getProjection().fromPixels(0, 0), p2);
									Rect bounds = marker.getBounds();
									
									Log.d("Touch Event - Down", "Point X:" + (p.x - p2.x) + ", Y:" + (p.y - p2.y));
									Log.d("Touch Event - Down", "Position X:" + x + ", Y:" + y);
									Log.d("Touch Event - Down", "Marker Bounds: L:" + bounds.left + ", R:" + bounds.right + ", B:" + bounds.bottom + ", T:" + bounds.top); 
									
									long dist = sqr((p.x - p2.x) - x) + sqr((p.y - p2.y)- y);
									
									if ((dist < lowestDist) || (lowestDistItem == null)) {
										lowestDist = dist;
										lowestDistItem = item;										
									}
									/*
									if (bounds.contains(p2.x + x - p.x, p2.y + y - p.y)) {
										// Item found !
										Log.d("Touch Event - Down", "Item hit !");
										movedItem = item;
										// Found, but it's still pinned !
										movedItemPinned = true;
										// Start the timer
										pressTime = System.currentTimeMillis();
										canBeMoved = true;
										newItem = false;
										
										// We don't need to keep on searching, the item was found..
										break;
										
									}
									*/
									
								}
								
								if (lowestDistItem != null) { // It actually had searched through some items
									if (lowestDist < sqr(distTreshold)) {
										// Item found !
										Log.d("Touch Event - Down", "Item hit !");
										movedItem = lowestDistItem;
										// Found, but it's still pinned !
										movedItemPinned = true;
										// Start the timer
										pressTime = System.currentTimeMillis();
										canBeMoved = true;
										newItem = false;
											
									}
								}
								
								if (movedItem == null) {
									movedItem = new OverlayItem("","",(GeoPoint) mapView.getProjection().fromPixels(x, y));
									movedItemPinned = true;
									newItem = true;
									canBeMoved = true;							
								}
								// Start timer for long click
								if (timerRunning) {
									canBeMoved = false;
								} else {
									timerRunning = true;
									timer = new Timer();
									timer.schedule(new TimerTask() {
			
										@Override
										public void run() {
											
											// THIS AREA HAS TO BE FIXED !! 
											
											// DOESN'T WORK PROPERLY WHEN ZOOMING BY MULTITOUCHING
											
											
											if (lastPointerCount == 1) {
												Log.d("Timer time elapsed", "canBeMoved == " + canBeMoved);
												if (!dialogShown) {
													if (canBeMoved) {
														// Item was pinned, so unpin it first
														movedItemPinned = false;	
														Log.d("Touch Event - Move", "Unpinning");
														// That means to remove item from overlays and add an image or marker for faster dragging
														handler.sendEmptyMessage(0);
													} else {
														movedItem = null;
														timerRunning = false;
													}
												}
											}
										}
										
									}, pressTimeTreshold);
								}
								result = true;
							}
						}
						if (event.getAction() == MotionEvent.ACTION_MOVE) {
							if (movedItem != null) {
								// There is some item, that was previously clicked on
								Log.d("Touch Event - Move","Press time = " + (System.currentTimeMillis() - pressTime));
								if (movedItemPinned) {
									// That item is clicked for less than the threshold time
									
									// Check, if the pointer is still on the marker
									Point p = new Point(0,0);
									mapView.getProjection().toPixels(movedItem.getPoint(), p);
									Point p2 = new Point(0,0);
									mapView.getProjection().toPixels(mapView.getProjection().fromPixels(0,0), p2);
									
									if (!marker.getBounds().contains(p2.x + x - p.x , p2.y + y - p.y)) {
										// Pointer is not on the marker anymore, user has probably lost interest in this marker
										canBeMoved = false;
									}
								} else {
									// Item is already unpinned, we just need to move it
									RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) movedImage.getLayoutParams();
									Rect bounds = marker.getBounds();
									lp.setMargins(x + bounds.left, y + bounds.top, 0, 0);
									movedImage.setLayoutParams(lp);
								}
							}
							result = true;
						}
						if (event.getAction() == MotionEvent.ACTION_UP) {
							if (newItem) {
								canBeMoved = false;
								timer.cancel();
								timerRunning = false;
							}
							if (((movedItem != null) && (movedItemPinned)) && (!newItem) && (!dialogShown)) {
								// Item was only clicked, not moved
								canBeMoved = false;
								// Show the custom dialog for editing markers
								dialogShown = true;
								timer.cancel();
								timerRunning = false;
								// Therefore, it's going to be edited
								editedItem = movedItem;
								NoticeDialogFragment d = new NoticeDialogFragment();
								d.show(getFragmentManager(), "MarkerEdit");
								getFragmentManager().executePendingTransactions();
								Log.d("Touch Event - Up", "Showing Dialog");
								dialogShown = true;
								
								addRadioButtonsListener(d);
								if (d.getDialog() == null) {
									Log.d("Tap","Dialog == null");
								} else {
									// Set the texts in edit bars corresponding to the former name and room
									EditText n = (EditText) d.getDialog().findViewById(R.id.edit_name);
									EditText r = (EditText) d.getDialog().findViewById(R.id.edit_room);
									radioButtons.set(0, (RadioButton) d.getDialog().findViewById(R.id.radioFloorButton0));
									radioButtons.set(1, (RadioButton) d.getDialog().findViewById(R.id.radioFloorButton1));
									radioButtons.set(2, (RadioButton) d.getDialog().findViewById(R.id.radioFloorButton2));
									n.setText(editedItem.getTitle());
									r.setText(editedItem.getSnippet());
									radioButtons.get(teacherFloors.get(markers.indexOf(editedItem))).setChecked(true);
								//	f.setText(teacherFloors.get(markers.indexOf(editedItem)).toString());
								}
								// Item is not moved, it's only being edited
								movedItem = null;
							}
							if ((movedItem != null) && (!movedItemPinned)){
								// Item was actually moved from it's former position (at least it was unpinned), so pin it back
								Log.d("Touch Event - Up","Pinning back");
								GeoPoint g = (GeoPoint) mapView.getProjection().fromPixels(x, y);
								// Create new OverlayItem with the desired preferences
								OverlayItem oi = new OverlayItem( movedItem.getTitle(), movedItem.getSnippet(), g);
								oi.setMarker(marker);
								oi.setMarkerHotspot(HotspotPlace.CENTER);
								// Update all the arrays regarding this marker
								selectedPositions.add(oi);
								int index = markers.indexOf(movedItem);
								int index2 = globalSelectedPositions.indexOf(movedItem);
								globalSelectedPositions.set(index2, oi);
								markers.set(index, oi);
								teacherPositions.set(index, g);
								// Hide the image
								hideView(movedImage);
								// Update overlay
								mapView.getOverlays().remove(myOverlay);
								myOverlay = new ItemizedIconOverlay<OverlayItem>(selectedPositions, gestureListener, defaultResourceProxyImpl);
								mapView.getOverlays().add(myOverlay);
								mapView.invalidate();
								// Item is not moved anymore
								movedItem = null;
							}
							result = true;
						}
					} else {
					
						if (event.getAction() == MotionEvent.ACTION_DOWN) {
							if (!dialogShown) {
								// Find out which item was clicked on
								lowestDist = 10000;
								lowestDistItem = null;
								int counter = 0;
								for (OverlayItem item : selectedPositions) {
									Point p = new Point(0,0);
									mapView.getProjection().toPixels(item.getPoint(), p);
									Point p2 = new Point(0,0);							
									mapView.getProjection().toPixels(mapView.getProjection().fromPixels(0, 0), p2);
									Rect bounds = marker.getBounds();
									
									Log.d("Touch Event - Down", "Point X:" + (p.x - p2.x) + ", Y:" + (p.y - p2.y));
									Log.d("Touch Event - Down", "Position X:" + x + ", Y:" + y);
									Log.d("Touch Event - Down", "Marker Bounds: L:" + bounds.left + ", R:" + bounds.right + ", B:" + bounds.bottom + ", T:" + bounds.top); 
									
									long dist = sqr((p.x - p2.x) - x) + sqr((p.y - p2.y)- y);
									
									if ((dist < sqr(distTreshold)/2) && (counter < 5)) {
										counter++; // So that it wouldn't show like..all of them when they are in overview
										Toast.makeText(OSMDroidMapActivity.this, item.mTitle + " ("+item.mDescription+") ", Toast.LENGTH_SHORT).show();
									}
									
									
									if ((dist < lowestDist) || (lowestDistItem == null)) {
										lowestDist = dist;
										lowestDistItem = item;										
									}
									
								}
								
								if (lowestDistItem != null) { // It actually had searched through some items
									if ((lowestDist < sqr(distTreshold)) && (lowestDist >= sqr(distTreshold)/2)) {
										// Displaying a single item
										Toast.makeText(OSMDroidMapActivity.this, lowestDistItem.mTitle + " (" + lowestDistItem.mDescription + ") ", Toast.LENGTH_SHORT).show();
									}
								}
							}
						}
					}
				}
					
					
				return result;
			}
			
		});
        
        edit.setOnClickListener(new OnClickListener() {
        	
        	public void onClick(View v) {
        		// If edit is clicked, the keyboard and search list need to be displayed and floor list hidden
        		imm.showSoftInput(edit, 0);
        		if (!edit.getText().toString().isEmpty()) {
        			showView(list);	
        		}
        		hideView(floorList);
        		Log.d("Edit", "Click");
        	}
        	
        });
        
        mapView.getController().setCenter(new GeoPoint(48.15240972,17.070065274)); // Right upon FMFI UK
    	lastSearch = "";
    	
    	showSearchPanel();
    	
    	floorButton.setOnClickListener(new OnClickListener(){
    		
    		public void onClick(View v) {
    			// We don't need keyboard nor search list now
    			imm.hideSoftInputFromWindow(edit.getWindowToken(), 0);
				hideView(list);    			
				// Toggle floor list visibility
				if (floorList.isClickable()) {
					hideView(floorList);
    			} else {
    				showView(floorList);
    			}
    		}
    		
    	});
    	
    	floorList = (ListView) findViewById(R.id.listView2);
    	hideView(floorList);
		floorList.setAlpha((float) 1);
		floorList.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, floorsNumbers));
		floorList.setOnItemClickListener(new android.widget.AdapterView.OnItemClickListener() {

			public void onItemClick(AdapterView<?> arg0, View arg1,
					int arg2, long arg3) {
				changeFloorTo(arg2);
				hideView(floorList);				
			}
    		
    	});
        list.setAlpha((float) 1);
        
        mapView.setMapListener(new MapAdapter() {
        	
        	public boolean onZoom(ZoomEvent event) {
        		mapView.postInvalidate();
				return false;
        	}
        	
        });
        
        selection = new ArrayList<String>(); // Results matching the searched string
        
        selectionArrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, selection);
        list.setAdapter(selectionArrayAdapter);
        
        tilesOverlays = new ArrayList<TilesOverlay>();
        
        // This section makes the application load maps from assets folder
        final AssetsTileSource ASSETS_TILE_SOURCE1 = new AssetsTileSource(this.getAssets(), "floor1",  ResourceProxy.string.offline_mode, mapView.getMinZoomLevel(), mapView.getMaxZoomLevel(), 256, ".jpg"); // This will load from assets/Map/14/xxxx/yyyyy.png

        MapTileModuleProviderBase moduleProvider1 = new MapTileFileAssetsProvider(ASSETS_TILE_SOURCE1);
        SimpleRegisterReceiver simpleReceiver1 = new SimpleRegisterReceiver(this);
        MapTileProviderArray tileProviderArray1 = new MapTileProviderArray(ASSETS_TILE_SOURCE1, simpleReceiver1, new MapTileModuleProviderBase[] { moduleProvider1 });

        tilesOverlays.add(new TilesOverlay(tileProviderArray1, getApplicationContext()));
		tilesOverlays.get(0).setLoadingBackgroundColor(Color.WHITE);
		tilesOverlays.get(0).setLoadingLineColor(Color.WHITE);
		

        final AssetsTileSource ASSETS_TILE_SOURCE2 = new AssetsTileSource(this.getAssets(), "floor2",  ResourceProxy.string.offline_mode, mapView.getMinZoomLevel(), mapView.getMaxZoomLevel(), 256, ".jpg"); // This will load from assets/Map/24/xxxx/yyyyy.png

        MapTileModuleProviderBase moduleProvider2 = new MapTileFileAssetsProvider(ASSETS_TILE_SOURCE2);
        SimpleRegisterReceiver simpleReceiver2 = new SimpleRegisterReceiver(this);
        MapTileProviderArray tileProviderArray2 = new MapTileProviderArray(ASSETS_TILE_SOURCE2, simpleReceiver2, new MapTileModuleProviderBase[] { moduleProvider2 });

        tilesOverlays.add(new TilesOverlay(tileProviderArray2, getApplicationContext()));
		tilesOverlays.get(1).setLoadingBackgroundColor(Color.WHITE);
		tilesOverlays.get(1).setLoadingLineColor(Color.WHITE);
		

        final AssetsTileSource ASSETS_TILE_SOURCE3 = new AssetsTileSource(this.getAssets(), "floor3",  ResourceProxy.string.offline_mode, mapView.getMinZoomLevel(), mapView.getMaxZoomLevel(), 256, ".jpg"); // This will load from assets/Map/34/xxxx/yyyyy.png

        MapTileModuleProviderBase moduleProvider3 = new MapTileFileAssetsProvider(ASSETS_TILE_SOURCE3);
        SimpleRegisterReceiver simpleReceiver3 = new SimpleRegisterReceiver(this);
        MapTileProviderArray tileProviderArray3 = new MapTileProviderArray(ASSETS_TILE_SOURCE3, simpleReceiver3, new MapTileModuleProviderBase[] { moduleProvider3 });

        tilesOverlays.add(new TilesOverlay(tileProviderArray3, getApplicationContext()));
		tilesOverlays.get(2).setLoadingBackgroundColor(Color.WHITE);
		tilesOverlays.get(2).setLoadingLineColor(Color.WHITE);
		
		
        mapView.getOverlays().add(tilesOverlays.get(currentFloor));
        
        // Simple overlay
        markers = new ArrayList<OverlayItem>();
        markersInvisible = new ArrayList<OverlayItem>();
        defaultResourceProxyImpl = new DefaultResourceProxyImpl(this);

        // Build the ArrayList of all the markers
        for (int i = 0; i < teacherNames.size(); i++) {
        	String tmp = "";
        	// Everything, except the last String is name (the last is name of the office)
        	for (int j = 0; j < teacherNames.get(i).size()-1; j++) {
        		tmp+= teacherNames.get(i).get(j)+" ";
        	}
        	OverlayItem oitem = new OverlayItem(tmp, teacherNames.get(i).get(teacherNames.get(i).size()-1),  teacherPositions.get(i));
        	oitem.setMarkerHotspot(HotspotPlace.CENTER);
        	oitem.setMarker(marker);
        	markers.add(oitem);
        	
        	OverlayItem oitem2 = new OverlayItem(tmp, teacherNames.get(i).get(teacherNames.get(i).size()-1), teacherPositions.get(i));
        	oitem2.setMarkerHotspot(HotspotPlace.CENTER);
        	oitem2.setMarker(markerInvisible);
        	markersInvisible.add(oitem2);
        }
        
        gestureListener = new ItemizedIconOverlay.OnItemGestureListener<OverlayItem>() {

			public boolean onItemLongPress(int arg0, OverlayItem arg1) {
				return false;
			}

			public boolean onItemSingleTapUp(int arg0, OverlayItem arg1) {
				/*if (!editMode) {
					// Show name + room if the edit mode is not activated
					Toast.makeText(
	                        OSMDroidMapActivity.this, 
	                        arg1.mTitle + " (" + arg1.mDescription + ")",Toast.LENGTH_SHORT).show();
				}*/
				return false;
			}

        };
        
		for (int i = 0; i < markers.size(); i++) {
			if (teacherFloors.get(i) == currentFloor) {
				selectedPositions.add(markersInvisible.get(i));
			}
			globalSelectedPositions.add(markersInvisible.get(i));
			globalSelectedIndexes.add(i);
		}
		myOverlay = new ItemizedIconOverlay<OverlayItem>(selectedPositions, gestureListener, defaultResourceProxyImpl);
		mapView.getOverlays().add(myOverlay);
		hideView(list);

        
		mapView.setUseDataConnection(false);
		
        // Set the borders of this view
    	mapView.setScrollableAreaLimit(new BoundingBoxE6(BorderLeftTop.getLatitudeE6(),BorderRightBottom.getLongitudeE6(),BorderRightBottom.getLatitudeE6(),BorderLeftTop.getLongitudeE6()));
    	mapView.invalidate();
    	mapView.postInvalidate();
	}

	protected long sqr(long i) {
		// TODO Auto-generated method stub
		return i*i;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_osmdroid_map, menu);
		return true;
	}
	
	@Override
	public boolean dispatchTouchEvent(MotionEvent ev) {		
		
		if ((dialogShown) && (ev.getAction() == MotionEvent.ACTION_DOWN) && (dialogStarted)) {
			dialogShown = false;
			dialogStarted = false;
			Log.d("Map Touch Event","Hiding dialog");
		}
		
		return super.dispatchTouchEvent(ev);
		
	}

	
	// FAILING HERE, NEEDS FIX !
	
	public void onDialogStart(NoticeDialogFragment dialog) {
		Log.d("Dialog Event","On Start");
		dialogStarted = true;
	}
	
}