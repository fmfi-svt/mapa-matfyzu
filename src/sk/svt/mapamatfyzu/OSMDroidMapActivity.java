package sk.svt.mapamatfyzu;

import android.os.Bundle;
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
		
		
        mapView = new MyMapView(this, 256); // Sposob pouzitia konstruktora, no ignoruje sa layout

        mapView.setClickable(true);

        mapView.setBuiltInZoomControls(true);

        setContentView(mapView); 
		
      /*  setContentView(R.layout.activity_osmdroid_map); // Sposob pouzitia findViewById(), layout sa zachovava
        
        this.mapView = (MyMapView) findViewById(R.id.osmmapview);
      */
        this.mapController = mapView.getController();
        
        mapView.setBuiltInZoomControls(true);
        
        mapView.getController().setZoom(16);
        
        mapView.getController().setCenter(new GeoPoint(48.151836,17.071214)); // Right upon FMFI UK

        
        mapView.setUseDataConnection(true); //Setting to false will make the device load from external storage

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
	//	text.setText("2. Screen X: " + String.format("%.2f", ev.getX()) + ", Y: " + String.format("%.2f", ev.getY()) + "\n" + "Longitude: " + TopLeft.getLongitudeE6()/1E6 + ", Latitude: " + TopLeft.getLatitudeE6()/1E6 + 
	//			 "\nLeftTop: " + BorderLeftTop.getLongitudeE6()/1E6 + ", " + BorderLeftTop.getLatitudeE6()/1E6);
			
		boolean problems = false;				
		
		if (TopLeft.getLongitudeE6() < BorderLeftTop.getLongitudeE6()) {
			problems = true;
			Middle.setLongitudeE6(BorderLeftTop.getLongitudeE6() + Middle.getLongitudeE6() - TopLeft.getLongitudeE6());
			BottomRight.setLongitudeE6(BorderLeftTop.getLongitudeE6() + BottomRight.getLongitudeE6() - TopLeft.getLongitudeE6());
			TopLeft.setLongitudeE6(BorderLeftTop.getLongitudeE6());
		}
		if (TopLeft.getLatitudeE6() > BorderLeftTop.getLatitudeE6()) {
			problems = true;
			Middle.setLatitudeE6(Middle.getLatitudeE6() - TopLeft.getLatitudeE6() + BorderLeftTop.getLatitudeE6());
			BottomRight.setLatitudeE6(BottomRight.getLatitudeE6() - TopLeft.getLatitudeE6() + BorderLeftTop.getLatitudeE6());
			TopLeft.setLatitudeE6(BorderLeftTop.getLatitudeE6());
		}
		if (BottomRight.getLongitudeE6() > BorderRightBottom.getLongitudeE6()) {
			problems = true;
			Middle.setLongitudeE6(Middle.getLongitudeE6() - BottomRight.getLongitudeE6() + BorderRightBottom.getLongitudeE6());
			TopLeft.setLongitudeE6(TopLeft.getLongitudeE6() - BottomRight.getLongitudeE6() + BorderRightBottom.getLongitudeE6());
			BottomRight.setLongitudeE6(BorderRightBottom.getLongitudeE6());
		}
		if (BottomRight.getLatitudeE6() < BorderRightBottom.getLatitudeE6()) {
			problems = true;
			Middle.setLatitudeE6(Middle.getLatitudeE6() + BorderRightBottom.getLatitudeE6() - BottomRight.getLatitudeE6());
			TopLeft.setLatitudeE6(TopLeft.getLatitudeE6() + BorderRightBottom.getLatitudeE6() - BottomRight.getLatitudeE6());
			BottomRight.setLatitudeE6(BorderRightBottom.getLatitudeE6());
		}
		
		if (problems) {
			return Middle;
		} else {
			return null;
		}
		
		

	}
	
	
	//Zakomentovanim tejto funkcie a definovanim mapView cez konstruktor sa mozes hybat po mape s obmedzenym zoomom
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
		//	TextView text = (TextView) findViewById(R.id.textView1);	
		//	text.setText("Screen X: " + String.format("%.2f", ev.getX()) + ", Y: " + String.format("%.2f", ev.getY()) + "\n" + "Longitude: " + Middle.getLongitudeE6()/1E6 + ", Latitude: " + Middle.getLatitudeE6()/1E6);
			
			
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
