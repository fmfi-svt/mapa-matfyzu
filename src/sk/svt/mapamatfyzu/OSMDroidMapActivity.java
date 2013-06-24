package sk.svt.mapamatfyzu;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.sql.Date;
import java.util.ArrayList;
import java.util.Arrays;

import android.os.Bundle;

import org.osmdroid.DefaultResourceProxyImpl;
import org.osmdroid.ResourceProxy;
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
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
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
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;
import android.widget.TextView;
import android.widget.Toast;

public class OSMDroidMapActivity extends Activity implements NoticeDialogFragment.NoticeDialogListener {

	FragmentManager fragmentManager;
	BoundedMapView mapView;
	MapController mapController;
	GeoPoint BorderLeftTop = new GeoPoint(48.153060, 17.066788);
	GeoPoint BorderRightBottom = new GeoPoint(48.149180, 17.072130);
	ArrayList<ArrayList< GeoPoint > > floors;
	ArrayList<String> floorsNumbers;
	ArrayList<OverlayItem> markers;
	Drawable marker;
	ItemizedOverlay<OverlayItem> myOverlay;
	ArrayList<ArrayList<String> > teacherNames;
	ArrayList<String> teacherNamesPlain;
	ArrayList<GeoPoint> teacherPositions;	
	ArrayList<Integer> alive = new ArrayList<Integer>();
	ArrayList<OverlayItem> selectedPositions = new ArrayList<OverlayItem>();
	OverlayItem movedItem = null;
	boolean movedItemPinned = true;
	ImageView movedImage;
	OverlayItem editedItem = null;
	long pressTime = 0;
	SearchTree tree;
	ListView list;
	ListView floorList;
	EditText edit;
	ArrayList<String> selection; 
	OnItemGestureListener<OverlayItem> gestureListener;
	DefaultResourceProxyImpl defaultResourceProxyImpl;
	Button floorButton;
	String lastSearch;
	int currentFloor;
	boolean consumeNextClick;
	final ArrayList<Integer> indexes = new ArrayList<Integer>();
	InputMethodManager imm;
	boolean editMode;
	PopupWindow popup;
	
	/*
	 * Parses the words divided by whitespaces in string into array of these words
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
	
	/*
	 * Returns a list of teacher IDs, that match the searched string
	 */
	private ArrayList<Integer> getPossibleSearches(String search) {
		
		ArrayList<String> strings = parseString(search);
		ArrayList<Integer> tmpindexes = new ArrayList<Integer>();
		
		for (int i = 0; i < strings.size(); i++) {
			tmpindexes.addAll(tree.suggestiveSearch(strings.get(i)));
		}
		
		ArrayList<Pair> forsort = new ArrayList<Pair>();
		ArrayList<Integer> res = new ArrayList<Integer>();
		
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
	
	private void showSearchPanel() {
		TextView text = (TextView) findViewById(R.id.textView1);
    	text.setVisibility(text.GONE);
    	
    /*	showPanelButton.setVisibility(showPanelButton.GONE);
    	showPanelButton.setClickable(false);
    	showPanelButton.setEnabled(false);
    	hidePanelButton.setVisibility(hidePanelButton.VISIBLE);
    	hidePanelButton.setClickable(true);
    	hidePanelButton.setEnabled(true);
    */
    	showView(floorButton);
    	showView(list);
    	showView(edit);
    	edit.setText(lastSearch);
    	edit.addTextChangedListener(new TextWatcher() {

    		// Searched string has changed -> update the list
			public void afterTextChanged(Editable s) {
				selection.clear();
				indexes.clear();
				indexes.addAll(getPossibleSearches(s.toString()));
				selectedPositions.clear();
				
				for (int i = 0; i < indexes.size(); i++) {
					if (alive.get(indexes.get(i)) == 1) {
						selection.add(teacherNamesPlain.get(indexes.get(i)));
						selectedPositions.add(markers.get(indexes.get(i)));	
					}
				}
				if (myOverlay != null) {
					mapView.getOverlays().remove(myOverlay);
				}
				myOverlay = new ItemizedIconOverlay<OverlayItem>(selectedPositions, gestureListener, defaultResourceProxyImpl);
				mapView.getOverlays().add(myOverlay);
				mapView.invalidate();
				lastSearch = s.toString();
				
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
				for (int i = 0; i < indexes.size(); i++) {
					if (alive.get(indexes.get(i)) == 1) {
						counter++;
						if (counter == arg2+1) {
							arg2 = i;
							break;
						}
					}
				}
				GeoPoint Middle = teacherPositions.get(indexes.get(arg2));
				for (int i = 0; i < floors.size(); i++) {
					GeoPoint tmpLT = floors.get(i).get(0);
					GeoPoint tmpRB = floors.get(i).get(1);
					if ((Middle.getLatitudeE6() < tmpLT.getLatitudeE6()) && (Middle.getLongitudeE6() > tmpLT.getLongitudeE6()) &&
							(tmpRB.getLatitudeE6() < Middle.getLatitudeE6()) && (tmpRB.getLongitudeE6() > Middle.getLongitudeE6())) {
						mapView.setScrollableAreaLimit(new BoundingBoxE6(tmpLT.getLatitudeE6(), tmpRB.getLongitudeE6(),
						tmpRB.getLatitudeE6(), tmpLT.getLongitudeE6()));
						break;
					}
				}
				mapController.setCenter(teacherPositions.get(indexes.get(arg2)));
				StringBuilder tmpname = new StringBuilder(teacherNamesPlain.get(indexes.get(arg2)));
				tmpname.delete(tmpname.lastIndexOf(" "), tmpname.length());
				edit.setText(tmpname.toString());
    			imm.hideSoftInputFromWindow(edit.getWindowToken(), 0);
    			hideView(list);
			}
    		
    	});
    	mapView.invalidate();
	}
	
	private void hideSearchPanel() {
		hideView(list);
		hideView(edit);
		TextView text = (TextView) findViewById(R.id.textView1);
    	text.setVisibility(text.VISIBLE);
    	
    /*	hidePanelButton.setVisibility(hidePanelButton.GONE);
    	hidePanelButton.setClickable(false);
    	hidePanelButton.setEnabled(false);
    	showPanelButton.setEnabled(true);
    	showPanelButton.setClickable(true);
    	showPanelButton.setVisibility(showPanelButton.VISIBLE);
   */
    	showView(floorButton);
    	mapView.invalidate();
	}
	
	private void showView(View v) {
		v.setVisibility(v.VISIBLE);
		v.setClickable(true);
		v.setActivated(true);
		v.setEnabled(true);
	}
	private void hideView(View v) {
		v.setVisibility(v.GONE);
		v.setClickable(false);
		v.setActivated(false);
		v.setEnabled(false);
	}

	public void onDialogPositiveClick(NoticeDialogFragment dialog) {
		// TODO Auto-generated method stub
		EditText name = (EditText) dialog.getDialog().findViewById(R.id.edit_name);
		EditText room = (EditText) dialog.getDialog().findViewById(R.id.edit_room);
		Log.d("Positive Click", "Name: "+ name.getText().toString() + ", Room: " + room.getText().toString());
		int index = markers.indexOf(editedItem);
		int index2 = selectedPositions.indexOf(editedItem);
		String newName = name.getText().toString();
		String newRoom = room.getText().toString();		
		OverlayItem tmp = new OverlayItem(newName, newRoom, editedItem.getPoint());
		tmp.setMarker(marker);
		tmp.setMarkerHotspot(HotspotPlace.CENTER);
		markers.set(index, tmp);
		for (int i = 0; i < teacherNames.get(index).size()-1; i++) {
			tree.removeRecord(teacherNames.get(index).get(i), index);
		}
		ArrayList<String> newNameList = parseString(newName + " " + newRoom);
		for (int i = 0; i < newNameList.size()-1; i++) {
			tree.addRecord(newNameList.get(i), index);
		}
		teacherNames.set(index, newNameList);
		teacherNamesPlain.set(index, newName + " (" + newRoom + ")");
		//selection.set(index2, newName + " (" + newRoom + ")");
		Log.d("Positive Click","Index2 = " + index2);
		list.invalidate();
		selectedPositions.set(index2, tmp);
		mapView.getOverlays().remove(myOverlay);
		myOverlay = new ItemizedIconOverlay<OverlayItem>(selectedPositions, gestureListener, defaultResourceProxyImpl);
		mapView.getOverlays().add(myOverlay);
		editedItem = null;		
	}

	public void onDialogNegativeClick(NoticeDialogFragment dialog) {
		// TODO Auto-generated method stub
		EditText name = (EditText) dialog.getDialog().findViewById(R.id.edit_name);
		EditText room = (EditText) dialog.getDialog().findViewById(R.id.edit_room);
		Log.d("Negative Click", "Name: "+ name.getText().toString() + ", Room: " + room.getText().toString());
		editedItem = null;
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		Intent intent = this.getIntent();
		Bundle extras = intent.getExtras();
		super.onCreate(savedInstanceState);
		
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
		
		floorsNumbers = new ArrayList<String>( Arrays.asList("Prízemie","1.","2."));
		currentFloor = 0;
		
		marker = this.getResources().getDrawable(R.drawable.blue_dot5);
		teacherNames = new ArrayList<ArrayList<String>>();
		teacherPositions = new ArrayList<GeoPoint>();
		teacherNamesPlain = new ArrayList<String>();
		ArrayList<ArrayList<Double>> teacherPositionsTmp = new ArrayList<ArrayList<Double>>();
		teacherNames = (ArrayList<ArrayList<String>>) extras.get("teacherNames");
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
		
		// Rebuild the SearchTree
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
		
    //	hidePanelButton = (Button) findViewById(R.id.button_hide);
    //	showPanelButton = (Button) findViewById(R.id.button_show);

        editMode = false;
        
        if (extras.containsKey("edit")) {
        // User wants to edit data
        	editMode = true;
        }
    	
        this.mapView = (BoundedMapView) findViewById(R.id.osmmapview);
        	
        this.mapController = mapView.getController();
        
    //    mapView.setBuiltInZoomControls(true);
        mapView.setMultiTouchControls(true);
        
        mapView.getController().setZoom(16);
/*        mapView.setOnClickListener(new OnClickListener() {

			public void onClick(View v) {
    			
			}
        	
        });
        */
        mapView.setOnTouchListener(new OnTouchListener() {
			
			public boolean onTouch(View v, MotionEvent event) {
				// TODO Auto-generated method stub
				boolean result = false;
				if (event.getAction() == MotionEvent.ACTION_DOWN) {
					imm.hideSoftInputFromWindow(edit.getWindowToken(), 0);
					hideView(list);	
					result = true;
				}
				
				Log.d("Touch Event", "Any");
				
				long pressTimeTreshold = 700;
				
				int x = (int)event.getX();
				int y = (int)event.getY();
				
				if (y < 0) y = 0;
				if (x < 0) x = 0;
				
				if (editMode) {
					if (event.getAction() == MotionEvent.ACTION_DOWN) {
						for (OverlayItem item : selectedPositions) {
							Point p = new Point(0,0);
							mapView.getProjection().toPixels(item.getPoint(), p);
							Point p2 = new Point(0,0);							
							mapView.getProjection().toPixels(mapView.getProjection().fromPixels(0, 0), p2);
							Rect bounds = marker.getBounds();
							
							Log.d("Touch Event - Down", "Point X:" + (p.x - p2.x) + ", Y:" + (p.y - p2.y));
							Log.d("Touch Event - Down", "Position X:" + x + ", Y:" + y);
							Log.d("Touch Event - Down", "Marker Bounds: L:" + bounds.left + ", R:" + bounds.right + ", B:" + bounds.bottom + ", T:" + bounds.top); 
							
							if (bounds.contains(p2.x + x - p.x, p2.y + y - p.y)) {
								Log.d("Touch Event - Down", "Item hit !");
								movedItem = item;
								movedItemPinned = true;
								pressTime = System.currentTimeMillis();
								
								break;
							}
							
						}
						result = true;
					}
					if (event.getAction() == MotionEvent.ACTION_MOVE) {
						if (movedItem != null) {
							Log.d("Touch Event - Move","Press time = " + (System.currentTimeMillis() - pressTime));
							if (System.currentTimeMillis() - pressTime < pressTimeTreshold) {
								Point p = new Point(0,0);
								mapView.getProjection().toPixels(movedItem.getPoint(), p);
								Point p2 = new Point(0,0);
								mapView.getProjection().toPixels(mapView.getProjection().fromPixels(0,0), p2);
								
								if (!marker.getBounds().contains(p2.x + x - p.x , p2.y + y - p.y)) {
									movedItem = null;
								}
							} else {
								if (movedItemPinned) {
									Log.d("Touch Event - Move", "Unpinning");
									selectedPositions.remove(movedItem);
									mapView.getOverlays().remove(myOverlay);
									myOverlay = new ItemizedIconOverlay<OverlayItem>(selectedPositions, gestureListener, defaultResourceProxyImpl);
									mapView.getOverlays().add(myOverlay);
									movedImage.setVisibility(movedImage.VISIBLE);
									
									mapView.invalidate();
									movedItemPinned = false;
								}
								
								RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) movedImage.getLayoutParams();
								Rect bounds = marker.getBounds();
								lp.setMargins(x + bounds.left, y + bounds.top, 0, 0);
								movedImage.setLayoutParams(lp);
							}
						}
						result = true;
					}
					if (event.getAction() == MotionEvent.ACTION_UP) {
						if ((movedItem != null) && (System.currentTimeMillis() - pressTime < pressTimeTreshold)) {
							Log.d("Touch Event - Up", "Showing Dialog");
							editedItem = movedItem;
							NoticeDialogFragment d = new NoticeDialogFragment();
							d.show(getFragmentManager(), "MarkerEdit");
							getFragmentManager().executePendingTransactions();
							if (d.getDialog() == null) {
								Log.d("Tap","Dialog == null");
							} else {
								EditText n = (EditText) d.getDialog().findViewById(R.id.edit_name);
								EditText r = (EditText) d.getDialog().findViewById(R.id.edit_room);
								n.setText(editedItem.getTitle());
								r.setText(editedItem.getSnippet());
							}
							movedItem = null;
						}
						if ((movedItem != null) && (!movedItemPinned)){
							Log.d("Touch Event - Up","Pinning back");
							GeoPoint g = (GeoPoint) mapView.getProjection().fromPixels(x, y);
							OverlayItem oi = new OverlayItem( movedItem.getTitle(), movedItem.getSnippet(), g);
							oi.setMarker(marker);
							oi.setMarkerHotspot(HotspotPlace.CENTER);
							selectedPositions.add(oi);
							int index = markers.indexOf(movedItem);
							markers.set(index, oi);
							teacherPositions.set(index, g);
							movedImage.setVisibility(movedImage.GONE);
							mapView.getOverlays().remove(myOverlay);
							myOverlay = new ItemizedIconOverlay<OverlayItem>(selectedPositions, gestureListener, defaultResourceProxyImpl);
							mapView.getOverlays().add(myOverlay);
							mapView.invalidate();
							movedItem = null;
						}
						result = true;
					}
				}
				
				return result;
			}
			
		});
        
        edit.setOnClickListener(new OnClickListener() {
        	
        	public void onClick(View v) {
        		imm.showSoftInput(edit, 0);
        		showView(list);
        		hideView(floorList);
        		Log.d("Edit", "Click");
        	}
        	
        });
        
        mapView.getController().setCenter(new GeoPoint(48.151836,17.071214)); // Right upon FMFI UK
        
    	lastSearch = "";
    	
    	showSearchPanel();
    	
    	floorButton.setOnClickListener(new OnClickListener(){
    		
    		public void onClick(View v) {
    			imm.hideSoftInputFromWindow(edit.getWindowToken(), 0);
				hideView(list);    			
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
				// TODO Auto-generated method stub
				mapView.setScrollableAreaLimit(new BoundingBoxE6(floors.get(arg2).get(0).getLatitudeE6(), floors.get(arg2).get(1).getLongitudeE6(),
						floors.get(arg2).get(1).getLatitudeE6(), floors.get(arg2).get(0).getLongitudeE6()));

				GeoPoint Middle = (GeoPoint) mapView.getProjection().fromPixels(mapView.getWidth()/2, mapView.getHeight()/2);
				mapController.setCenter(new GeoPoint(floors.get(arg2).get(0).getLatitudeE6() + Middle.getLatitudeE6() - floors.get(currentFloor).get(0).getLatitudeE6(),
						floors.get(arg2).get(0).getLongitudeE6() + Middle.getLongitudeE6() - floors.get(currentFloor).get(0).getLongitudeE6()));
				mapView.invalidate();
				currentFloor = arg2;
				hideView(floorList);
			}
    		
    	});
        list.setAlpha((float) 1);
        
        selection = new ArrayList<String>(); // Results matching the searched string
        
        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, selection);
        list.setAdapter(arrayAdapter);
        
        // This section makes the application load maps from assets folder
        final AssetsTileSource ASSETS_TILE_SOURCE = new AssetsTileSource(this.getAssets(), "Map",  ResourceProxy.string.offline_mode, 14, 18, 256, ".png"); // This will load from assets/Map/14/xxxx/yyyyy.png

        MapTileModuleProviderBase moduleProvider = new MapTileFileAssetsProvider(ASSETS_TILE_SOURCE);
        SimpleRegisterReceiver simpleReceiver = new SimpleRegisterReceiver(this);
        MapTileProviderArray tileProviderArray = new MapTileProviderArray(ASSETS_TILE_SOURCE, simpleReceiver, new MapTileModuleProviderBase[] { moduleProvider });

        TilesOverlay tilesOverlay = new TilesOverlay(tileProviderArray, getApplicationContext());
		tilesOverlay.setLoadingBackgroundColor(Color.TRANSPARENT);
        mapView.getOverlays().add(tilesOverlay);
        
        // Simple overlay
        markers = new ArrayList<OverlayItem>();
        defaultResourceProxyImpl = new DefaultResourceProxyImpl(this);
        
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
        }
      /*  
        popup = new PopupWindow(this);
        RelativeLayout rl = new RelativeLayout(this);
        RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        TextView tv = new TextView(this);
        tv.setText("Sample text on PopupView ... ");
        rl.addView(tv, lp);
        RelativeLayout.LayoutParams lp2 = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        
        Button b = new Button(this);
        rl.addView(b,lp2);
        popup.setContentView(rl);
        */
        gestureListener = new ItemizedIconOverlay.OnItemGestureListener<OverlayItem>() {

			public boolean onItemLongPress(int arg0, OverlayItem arg1) {
				if (editMode) {
					/*int tmpindex = markers.indexOf(arg1);
					alive.set(tmpindex, 0);
					mapView.getOverlays().remove(myOverlay);
					selectedPositions.remove(arg1);
					myOverlay = new ItemizedIconOverlay<OverlayItem>(selectedPositions, gestureListener, defaultResourceProxyImpl);
					mapView.getOverlays().add(myOverlay);
					selection.clear();
					for (int i = 0; i < indexes.size(); i++) {
						if (alive.get(indexes.get(i)) == 1) {
							selection.add(teacherNamesPlain.get(indexes.get(i)));
						}
					}
					list.refreshDrawableState();
					mapView.invalidate();*/
				} else {
				Toast.makeText(
                        OSMDroidMapActivity.this,
                        arg1.mDescription, Toast.LENGTH_SHORT).show();
				}
				return false;
			}

			public boolean onItemSingleTapUp(int arg0, OverlayItem arg1) {
				if (editMode) {
					
				} else {
					Toast.makeText(
	                        OSMDroidMapActivity.this, 
	                        arg1.mTitle ,Toast.LENGTH_SHORT).show();
				}
				return false;
			}

        };
    	mapView.setScrollableAreaLimit(new BoundingBoxE6(BorderLeftTop.getLatitudeE6(),BorderRightBottom.getLongitudeE6(),BorderRightBottom.getLatitudeE6(),BorderLeftTop.getLongitudeE6()));
    	mapView.invalidate();
    	/*
    	TextView text = (TextView) findViewById(R.id.textView1);
    	text.setText(""+markers.get(0).getPoint().getLatitudeE6()/1E6+"\n " + markers.get(0).getPoint().getLongitudeE6()/1E6);
    	*/
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_osmdroid_map, menu);
		return true;
	}
	
	public GeoPoint isOutOfBounds(Projection proj) {
		
		GeoPoint TopLeft = (GeoPoint) proj.fromPixels(0, 0);
		GeoPoint BottomRight = (GeoPoint) proj.fromPixels(mapView.getWidth(), mapView.getHeight());
		
		GeoPoint Middle = (GeoPoint) proj.fromPixels(mapView.getWidth()/2, mapView.getHeight()/2);
	
		// DEBUG	
		TextView text = (TextView) findViewById(R.id.textView1);
		text.setText("Longitude: " + TopLeft.getLongitudeE6()/1E6 + ", Latitude: " + TopLeft.getLatitudeE6()/1E6 + 
				 "\nLeftTop: " + BorderLeftTop.getLongitudeE6()/1E6 + ", " + BorderLeftTop.getLatitudeE6()/1E6);
			
		boolean problems = false;				
		
		// Set appropriate longitude
		if (Math.abs(BottomRight.getLongitudeE6() - TopLeft.getLongitudeE6()) >= Math.abs(BorderRightBottom.getLongitudeE6() - BorderLeftTop.getLongitudeE6())) {
			problems = true;
			Middle.setLongitudeE6((BorderLeftTop.getLongitudeE6() + BorderRightBottom.getLongitudeE6())/2);
		} else {
			
			if (TopLeft.getLongitudeE6() < BorderLeftTop.getLongitudeE6()) {
				problems = true;
				Middle.setLongitudeE6(BorderLeftTop.getLongitudeE6() + Middle.getLongitudeE6() - TopLeft.getLongitudeE6());
				BottomRight.setLongitudeE6(BorderLeftTop.getLongitudeE6() + BottomRight.getLongitudeE6() - TopLeft.getLongitudeE6());
				TopLeft.setLongitudeE6(BorderLeftTop.getLongitudeE6());
			}
			if (BottomRight.getLongitudeE6() > BorderRightBottom.getLongitudeE6()) {
				problems = true;
				Middle.setLongitudeE6(Middle.getLongitudeE6() - BottomRight.getLongitudeE6() + BorderRightBottom.getLongitudeE6());
				TopLeft.setLongitudeE6(TopLeft.getLongitudeE6() - BottomRight.getLongitudeE6() + BorderRightBottom.getLongitudeE6());
				BottomRight.setLongitudeE6(BorderRightBottom.getLongitudeE6());
			}
			
		}
		
		// Set appropriate latitude
		if (Math.abs(BottomRight.getLatitudeE6() - TopLeft.getLatitudeE6()) >= Math.abs(BorderRightBottom.getLatitudeE6() - BorderLeftTop.getLatitudeE6())) {
			problems = true;
			Middle.setLatitudeE6((BorderLeftTop.getLatitudeE6() + BorderRightBottom.getLatitudeE6())/2);
		} else {
			
			if (TopLeft.getLatitudeE6() > BorderLeftTop.getLatitudeE6()) {
				problems = true;
				Middle.setLatitudeE6(Middle.getLatitudeE6() - TopLeft.getLatitudeE6() + BorderLeftTop.getLatitudeE6());
				BottomRight.setLatitudeE6(BottomRight.getLatitudeE6() - TopLeft.getLatitudeE6() + BorderLeftTop.getLatitudeE6());
				TopLeft.setLatitudeE6(BorderLeftTop.getLatitudeE6());
			}
			if (BottomRight.getLatitudeE6() < BorderRightBottom.getLatitudeE6()) {
				problems = true;
				Middle.setLatitudeE6(Middle.getLatitudeE6() + BorderRightBottom.getLatitudeE6() - BottomRight.getLatitudeE6());
				TopLeft.setLatitudeE6(TopLeft.getLatitudeE6() + BorderRightBottom.getLatitudeE6() - BottomRight.getLatitudeE6());
				BottomRight.setLatitudeE6(BorderRightBottom.getLatitudeE6());
			}
			
		}
		
		if (problems) {
			return Middle;
		} else {
			return null;
		}		

	}
	
	@Override
	public boolean dispatchTouchEvent(MotionEvent ev) {		
			
		if (BorderLeftTop == null) {
	        BorderLeftTop = (GeoPoint) mapView.getProjection().fromPixels(0, 0);
	        BorderRightBottom = (GeoPoint) mapView.getProjection().fromPixels(mapView.getWidth(), mapView.getHeight());
	        TextView t = (TextView) findViewById(R.id.textView1);
	        t.setText("Left top: " + BorderLeftTop.getLongitudeE6()/1E6 + ", " + BorderLeftTop.getLatitudeE6()/1E6 + "\n" +
	        "Right Bottom" + BorderRightBottom.getLongitudeE6()/1E6 + ", " + BorderRightBottom.getLatitudeE6()/1E6);
			
		} else {
			TextView t = (TextView) findViewById(R.id.textView1);
			GeoPoint LT = (GeoPoint) mapView.getProjection().fromPixels(0, 0);
			GeoPoint RB = (GeoPoint) mapView.getProjection().fromPixels(mapView.getWidth(), mapView.getHeight());
			
		//	t.setText("X: " + ev.getX() + ", Y: " + ev.getY() + "\nW: " + mapView.getWidth() + ", H: " + mapView.getHeight());
			
			t.setText("Longitude: " + ((ev.getX()/mapView.getWidth())*(RB.getLongitudeE6()-LT.getLongitudeE6())+LT.getLongitudeE6())/1E6 + "\n" +
					  "Latitude: " + ((1-(ev.getY()-mapView.getY())/mapView.getHeight())*(LT.getLatitudeE6()-RB.getLatitudeE6())+RB.getLatitudeE6())/1E6 + "\n" +
					"Zoom: " + mapView.getZoomLevel());
		}
		//mapView.invalidate();
		
		return super.dispatchTouchEvent(ev);
		
	}
	
}