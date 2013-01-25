package sk.svt.mapamatfyzu;

import org.osmdroid.ResourceProxy;
import org.osmdroid.events.ZoomEvent;
import org.osmdroid.tileprovider.MapTileProviderBase;
import org.osmdroid.views.MapView;

import android.content.Context;
import android.os.Handler;
import android.util.AttributeSet;

public class MyMapView extends MapView {
	
	public MyMapView(Context context, int tileSizePixels) {
		super(context, tileSizePixels);
		// TODO Auto-generated constructor stub
	}

	public MyMapView(Context context, AttributeSet attrs) {
		super(context, attrs);
		// TODO Auto-generated constructor stub
	}

	public MyMapView(Context context, int tileSizePixels,
			ResourceProxy resourceProxy) {
		super(context, tileSizePixels, resourceProxy);
		// TODO Auto-generated constructor stub
	}

	public MyMapView(Context context, int tileSizePixels,
			ResourceProxy resourceProxy, MapTileProviderBase aTileProvider) {
		super(context, tileSizePixels, resourceProxy, aTileProvider);
		// TODO Auto-generated constructor stub
	}

	public MyMapView(Context context, int tileSizePixels,
			ResourceProxy resourceProxy, MapTileProviderBase aTileProvider,
			Handler tileRequestCompleteHandler) {
		super(context, tileSizePixels, resourceProxy, aTileProvider,
				tileRequestCompleteHandler);
		// TODO Auto-generated constructor stub
	}

	@Override
	public int getMaxZoomLevel() {
		return 18;
	}
	
	@Override
	public int getMinZoomLevel() {
		return 14;
	}
	
	
}
