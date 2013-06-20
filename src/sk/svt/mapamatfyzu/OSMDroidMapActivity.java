package sk.svt.mapamatfyzu;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
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
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class OSMDroidMapActivity extends Activity {

	BoundedMapView mapView;
	MapController mapController;
	GeoPoint BorderLeftTop = new GeoPoint(48.153060, 17.066788);
	GeoPoint BorderRightBottom = new GeoPoint(48.149180, 17.072130);
	ArrayList<ArrayList< GeoPoint > > floors;
	ArrayList<String> floorsNumbers;
	ArrayList<OverlayItem> markers;
	ItemizedOverlay<OverlayItem> myOverlay;
	ArrayList<ArrayList<String> > teacherNames;
	ArrayList<String> teacherNamesPlain;
	ArrayList<GeoPoint> teacherPositions;	
	SearchTree tree;
	ListView list;
	ListView floorList;
	EditText edit;
	ArrayList<String> selection; 
	OnItemGestureListener<OverlayItem> gestureListener;
	DefaultResourceProxyImpl defaultResourceProxyImpl;
//	Button hidePanelButton;
//	Button showPanelButton;
	Button floorButton;
	String lastSearch;
	int currentFloor;
	boolean consumeNextClick;
	final ArrayList<Integer> indexes = new ArrayList<Integer>();
	InputMethodManager imm;
	
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
	/*
	 * Consider only best matches	
	 */
		
		for (int i = 0; i < forsort.size(); i++) {
			if (forsort.get(i).getKey() >= strings.size()) {
				res.add(forsort.get(i).getValue());
			}
		}
		
/*
 *  Another algorithm, considers even non-perfect matches		
 *  
		Pair indexes[] = new Pair[forsort.size()];
		
		for (int i = 0; i < forsort.size(); i++) {
			indexes[i] = forsort.get(i);
		}
		
		
		Arrays.sort(indexes);
		
		for (int i = forsort.size()-1; i >= 0; i--) {
			res.add(indexes[i].getValue());
		}
*/
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
    	floorButton.setVisibility(floorButton.VISIBLE);
    	floorButton.setClickable(true);
    	floorButton.setEnabled(true);
    	list.setClickable(true);
    	list.setEnabled(true);
    	list.setVisibility(list.VISIBLE);
    	edit.setClickable(true);
    	edit.setEnabled(true);
    	edit.setVisibility(edit.VISIBLE);
    	edit.setText(lastSearch);
    	edit.addTextChangedListener(new TextWatcher() {

    		// Searched string has changed -> update the list
			public void afterTextChanged(Editable s) {
				selection.clear();
				indexes.clear();
				indexes.addAll(getPossibleSearches(s.toString()));
				ArrayList<OverlayItem> selectedPositions = new ArrayList<OverlayItem>();
				
				for (int i = 0; i < indexes.size(); i++) {
					selection.add(teacherNamesPlain.get(indexes.get(i)));
					selectedPositions.add(markers.get(indexes.get(i)));
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
				edit.setText(teacherNamesPlain.get(indexes.get(arg2)));
    			imm.hideSoftInputFromWindow(edit.getWindowToken(), 0);
				list.setVisibility(list.GONE);
				list.setEnabled(false);
				list.setActivated(false);				
				
			}
    		
    	});
    	mapView.invalidate();
	}
	
	private void hideSearchPanel() {
    	list.setClickable(false);
    	list.setEnabled(false);
    	list.setVisibility(list.GONE);    
    	edit.setClickable(false);
    	edit.setEnabled(false);
    	edit.setVisibility(edit.GONE);
    	TextView text = (TextView) findViewById(R.id.textView1);
    	text.setVisibility(text.VISIBLE);
    	
    /*	hidePanelButton.setVisibility(hidePanelButton.GONE);
    	hidePanelButton.setClickable(false);
    	hidePanelButton.setEnabled(false);
    	showPanelButton.setEnabled(true);
    	showPanelButton.setClickable(true);
    	showPanelButton.setVisibility(showPanelButton.VISIBLE);
   */
    	floorButton.setVisibility(floorButton.VISIBLE);
    	floorButton.setClickable(true);
    	floorButton.setEnabled(true);
    	mapView.invalidate();
	}
	
	synchronized private void setConsumeNextClick(boolean b) {
		consumeNextClick = b;
	}
	
	synchronized private boolean getConsumeNextClick() {
		return consumeNextClick;
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
		
		int counter = 0;
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
    //	hidePanelButton = (Button) findViewById(R.id.button_hide);
    //	showPanelButton = (Button) findViewById(R.id.button_show);
        
    	consumeNextClick = false;
    	
        this.mapView = (BoundedMapView) findViewById(R.id.osmmapview);
        	
        this.mapController = mapView.getController();
        
        mapView.setBuiltInZoomControls(true);
        mapView.setMultiTouchControls(true);
        
        mapView.getController().setZoom(16);
        mapView.setOnClickListener(new OnClickListener() {

			public void onClick(View v) {
    			imm.hideSoftInputFromWindow(edit.getWindowToken(), 0);
				list.setVisibility(list.GONE);
				list.setEnabled(false);
				list.setActivated(false);
			}
        	
        });
        
        edit.setOnClickListener(new OnClickListener() {
        	
        	public void onClick(View v) {
        		imm.showSoftInput(edit, 0);
				list.setVisibility(list.VISIBLE);
				list.setEnabled(true);
				list.setActivated(true);
				floorList.setClickable(false);
				floorList.setActivated(false);
				floorList.setVisibility(floorList.GONE);
        	}
        	
        });
        
        mapView.getController().setCenter(new GeoPoint(48.151836,17.071214)); // Right upon FMFI UK
        
    	lastSearch = "";
    	
    /*	hidePanelButton.setVisibility(hidePanelButton.GONE);
    	hidePanelButton.setActivated(false);
    	hidePanelButton.setEnabled(false);
    	showPanelButton.setVisibility(showPanelButton.GONE);
    	showPanelButton.setClickable(false);
    	showPanelButton.setEnabled(false);
    */	
    	showSearchPanel();
    	
    	/*
    	hidePanelButton.setOnClickListener(new OnClickListener() {
    		
    		public void onClick(View v) {
    			hideSearchPanel();
    		}
    		
    	});
    	
    	showPanelButton.setOnClickListener(new OnClickListener() {
    		
    		public void onClick(View v) {
    			showSearchPanel();
    		}
    	});
    	*/
    	floorButton.setOnClickListener(new OnClickListener(){
    		
    		public void onClick(View v) {
    			imm.hideSoftInputFromWindow(edit.getWindowToken(), 0);
				list.setVisibility(list.GONE);
				list.setEnabled(false);
				list.setActivated(false);    			
				if (floorList.isClickable()) {
    				floorList.setClickable(false);
    				floorList.setActivated(false);
    				floorList.setVisibility(floorList.GONE);
    			} else {
    				floorList.setClickable(true);
    				floorList.setActivated(true);
    				floorList.setVisibility(floorList.VISIBLE);
    			}
    		}
    		
    	});
    	
    	floorList = (ListView) findViewById(R.id.listView2);
		floorList.setClickable(false);
		floorList.setActivated(false);
		floorList.setVisibility(floorList.GONE);
		
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
				floorList.setClickable(false);
				floorList.setActivated(false);
				floorList.setVisibility(floorList.GONE);
			}
    		
    	});
        list.setAlpha((float) 1);
        
        selection = new ArrayList<String>(); // Results matching the searched string
        
        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, selection);
        list.setAdapter(arrayAdapter);
        
        // User wanted to search -> display search tools
        /*
        
        if (extras.containsKey("search")) {
        	showSearchPanel();
        } else {
        	hideSearchPanel();
        }
        
        */
        
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
        	oitem.setMarker(this.getResources().getDrawable(R.drawable.blue_dot5));
        	markers.add(oitem);
        }
        
        gestureListener = new ItemizedIconOverlay.OnItemGestureListener<OverlayItem>() {

			public boolean onItemLongPress(int arg0, OverlayItem arg1) {
				Toast.makeText(
                        OSMDroidMapActivity.this,
                        arg1.mDescription, Toast.LENGTH_SHORT).show();
				return false;
			}

			public boolean onItemSingleTapUp(int arg0, OverlayItem arg1) {
				Toast.makeText(
                        OSMDroidMapActivity.this, 
                        arg1.mTitle ,Toast.LENGTH_SHORT).show();
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
