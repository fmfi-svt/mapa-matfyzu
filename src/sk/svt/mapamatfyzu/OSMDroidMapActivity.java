package sk.svt.mapamatfyzu;

import java.util.ArrayList;

import android.os.Bundle;

import org.osmdroid.DefaultResourceProxyImpl;
import org.osmdroid.ResourceProxy;
import org.osmdroid.events.MapListener;
import org.osmdroid.events.ScrollEvent;
import org.osmdroid.events.ZoomEvent;
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase;
import org.osmdroid.tileprovider.tilesource.XYTileSource;
import org.osmdroid.util.BoundingBoxE6;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.overlay.ItemizedIconOverlay;
import org.osmdroid.views.overlay.ItemizedOverlay;
import org.osmdroid.views.overlay.OverlayItem;

import org.osmdroid.views.MapController;
import org.osmdroid.views.MapView;
import org.osmdroid.views.MapView.Projection;

import android.app.Activity;
import android.content.Context;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.Window;
import android.widget.TextView;
import android.widget.Toast;

public class OSMDroidMapActivity extends Activity {

	BoundedMapView mapView;
	MapController mapController;
	GeoPoint BorderLeftTop = null;
	GeoPoint BorderRightBottom = null;
	ArrayList<OverlayItem> markers;
	ItemizedOverlay<OverlayItem> myOverlay;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		super.onCreate(savedInstanceState);
				
        setContentView(R.layout.activity_osmdroid_map); 
        
        this.mapView = (BoundedMapView) findViewById(R.id.osmmapview);
        
        this.mapController = mapView.getController();
        
        mapView.setBuiltInZoomControls(true);
        
        mapView.getController().setZoom(16);
        
        mapView.getController().setCenter(new GeoPoint(48.151836,17.071214)); // Right upon FMFI UK
                
        mapView.setUseDataConnection(true); //Setting to false will make the device load from external storage
        
        
        // Attempt to add some overlay.. but somehow it doesn't work yet
        markers = new ArrayList<OverlayItem>();
        
        OverlayItem marker = new OverlayItem("Kancelaria", "Majakova kancelaria", new GeoPoint(48.151123,17.069084));
        marker.setMarker(this.getResources().getDrawable(R.drawable.marker_default));
                
        markers.add(marker);
        this.myOverlay = new ItemizedIconOverlay<OverlayItem>(this.getApplicationContext(), new ArrayList<OverlayItem>(), new ItemizedIconOverlay.OnItemGestureListener<OverlayItem>() {

			public boolean onItemLongPress(int arg0, OverlayItem arg1) {
				Toast.makeText(
                        OSMDroidMapActivity.this,
                        "Item '" + arg1.mTitle, Toast.LENGTH_LONG).show();
				return false;
			}

			public boolean onItemSingleTapUp(int arg0, OverlayItem arg1) {
				Toast.makeText(
                        OSMDroidMapActivity.this, 
                        "Item '" + arg1.mTitle ,Toast.LENGTH_LONG).show();
				return false;
			}

        });
        mapView.getOverlays().add(this.myOverlay);        
        mapView.invalidate();
        // End of attempt

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
			
        	mapView.setScrollableAreaLimit(new BoundingBoxE6(BorderLeftTop.getLatitudeE6(),BorderRightBottom.getLongitudeE6(),BorderRightBottom.getLatitudeE6(),BorderLeftTop.getLongitudeE6()));
        
		}
		
		return super.dispatchTouchEvent(ev);
		
	}
	
}
