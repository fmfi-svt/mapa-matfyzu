package sk.svt.mapamatfyzu;

import android.app.Activity;
import android.os.Bundle;
import com.google.android.maps.MapActivity;

public class MFMap extends MapActivity {

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    setContentView(R.layout.map);
	    // TODO Auto-generated method stub
	}

	@Override
	protected boolean isRouteDisplayed() {
		return false;
	}

}