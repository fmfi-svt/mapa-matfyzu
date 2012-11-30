package sk.svt.mapamatfyzu;

import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.view.Menu;
import android.view.View;

public class MainActivity extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }
    
    public void onClick(View view) {
    	if (view.getId() == R.id.button_google_map) {
    		Intent intent = new Intent(this, MFMap.class);
    		startActivity(intent);
    	}
    	if (view.getId() == R.id.button_osm_map) {
    		Intent intent = new Intent(this, OSMDroidMapActivity.class);
    		startActivity(intent);
    	}
    }
}
