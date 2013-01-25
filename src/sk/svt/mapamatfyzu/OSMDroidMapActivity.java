package sk.svt.mapamatfyzu;

import android.os.Bundle;

import org.osmdroid.ResourceProxy;
import org.osmdroid.events.MapListener;
import org.osmdroid.events.ScrollEvent;
import org.osmdroid.events.ZoomEvent;
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase;
import org.osmdroid.tileprovider.tilesource.XYTileSource;
import org.osmdroid.util.GeoPoint;

import org.osmdroid.views.MapController;
import org.osmdroid.views.MapView;
import org.osmdroid.views.MapView.Projection;

import android.app.Activity;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.Window;
import android.widget.TextView;

public class OSMDroidMapActivity extends Activity {

	MyMapView mapView;
	MapController mapController;
	GeoPoint BorderLeftTop = null;
	GeoPoint BorderRightBottom = null;
		
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		super.onCreate(savedInstanceState);
		
		
    /*  mapView = new MyMapView(this, 256); 
        mapView.setClickable(true);
        mapView.setEnabled(true);
        mapView.setBuiltInZoomControls(true);
        mapView.setMultiTouchControls(true);
        mapView.setActivated(true);
        setContentView(mapView); 
    */
        setContentView(R.layout.activity_osmdroid_map); 
        
        this.mapView = (MyMapView) findViewById(R.id.osmmapview);
        
        this.mapController = mapView.getController();
        
        mapView.setBuiltInZoomControls(true);
        
        mapView.getController().setZoom(16);
        
        mapView.getController().setCenter(new GeoPoint(48.151836,17.071214)); // Right upon FMFI UK
        
        mapView.setUseDataConnection(true); //Setting to false will make the device load from external storage
        
        mapView.setMapListener(new MapListener() {

			public boolean onScroll(ScrollEvent arg0) {				
				return false;
			}

			public boolean onZoom(ZoomEvent arg0) {
				Projection proj = mapView.getProjection();
				GeoPoint Middle = isOutOfBounds(proj);
				if (Middle != null) {
					mapController.setCenter(Middle);
				}
				return false;
			}
        	
        });

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
		if (Math.abs(BottomRight.getLongitudeE6() - TopLeft.getLongitudeE6()) > Math.abs(BorderRightBottom.getLongitudeE6() - BorderLeftTop.getLongitudeE6())) {
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
		if (Math.abs(BottomRight.getLatitudeE6() - TopLeft.getLatitudeE6()) > Math.abs(BorderRightBottom.getLatitudeE6() - BorderLeftTop.getLatitudeE6())) {
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
			
			Projection proj = mapView.getProjection();	
			
			if (ev.getAction() == MotionEvent.ACTION_UP){

				GeoPoint Middle = isOutOfBounds(proj);
				if (Middle != null) {
					mapController.setCenter(Middle);
				}
			
			}
			
		}
		
		return super.dispatchTouchEvent(ev);
		
	}
	
}
