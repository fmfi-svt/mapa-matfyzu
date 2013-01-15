package sk.svt.mapamatfyzu;

import android.os.Bundle;
import org.osmdroid.util.GeoPoint;

import org.osmdroid.views.MapView;
import android.app.Activity;
import android.view.Menu;

public class OSMDroidMapActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
        MapView mapView = new MapView(this, 256); //constructor

        mapView.setClickable(true);

        mapView.setBuiltInZoomControls(true);

        setContentView(mapView); 

        mapView.getController().setZoom(15); 

        mapView.getController().setCenter(new GeoPoint(48.155,17.134)); //Somewhere in Bratislava

        mapView.setUseDataConnection(true); //Setting to false will make the device load from external storage

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_osmdroid_map, menu);
		return true;
	}

}
