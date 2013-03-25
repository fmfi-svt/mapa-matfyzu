package sk.svt.mapamatfyzu;

import org.osmdroid.tileprovider.MapTile;
import org.osmdroid.tileprovider.MapTileRequestState;
import org.osmdroid.tileprovider.constants.OpenStreetMapTileProviderConstants;
import org.osmdroid.tileprovider.modules.MapTileModuleProviderBase;
import org.osmdroid.tileprovider.tilesource.BitmapTileSourceBase.LowMemoryException;
import org.osmdroid.tileprovider.tilesource.ITileSource;

import android.graphics.drawable.Drawable;

public class MapTileFileAssetsProvider extends MapTileModuleProviderBase {

	protected ITileSource mTileSource;

	public MapTileFileAssetsProvider(final ITileSource pTileSource) {
		super(OpenStreetMapTileProviderConstants.NUMBER_OF_TILE_FILESYSTEM_THREADS, OpenStreetMapTileProviderConstants.TILE_FILESYSTEM_MAXIMUM_QUEUE_SIZE);

		mTileSource = pTileSource;
	}

	@Override
	public boolean getUsesDataConnection() {
		return false;
	}

	@Override
	protected String getName() {
		return "Assets Folder Provider";
	}

	@Override
	protected String getThreadGroupName() {
		return "assetsfolder";
	}

	@Override
	protected Runnable getTileLoader() {
		return new TileLoader();
	}

	@Override
	public int getMinimumZoomLevel() {
		return mTileSource != null ? mTileSource.getMinimumZoomLevel() : MAXIMUM_ZOOMLEVEL;
	}

	@Override
	public int getMaximumZoomLevel() {
		return mTileSource != null ? mTileSource.getMaximumZoomLevel() : MINIMUM_ZOOMLEVEL;
	}

	@Override
	public void setTileSource(final ITileSource pTileSource) {
		mTileSource = pTileSource;
	}

	private class TileLoader extends MapTileModuleProviderBase.TileLoader {

		@Override
		public Drawable loadTile(final MapTileRequestState pState) throws CantContinueException {

			if (mTileSource == null) {
				return null;
			}

			final MapTile pTile = pState.getMapTile();
			String path = mTileSource.getTileRelativeFilenameString(pTile);
			
			Drawable drawable;
			try {
				drawable = mTileSource.getDrawable(path);
			} catch (final LowMemoryException e) {
				// low memory so empty the queue
				throw new CantContinueException(e);
			}
			
			return drawable;
		}
	}
}