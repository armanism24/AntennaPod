package de.podfetcher.asynctask;

import de.podfetcher.PodcastApp;
import de.podfetcher.R;
import de.podfetcher.feed.FeedImage;
import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.support.v4.util.LruCache;
import android.util.Log;
import android.widget.ImageView;

/** Caches and loads FeedImage bitmaps in the background */
public class FeedImageLoader {
	private static final String TAG = "FeedImageLoader";
	private static FeedImageLoader singleton;

	/**
	 * Stores references to loaded bitmaps. Bitmaps can be accessed by the id of
	 * the FeedImage the bitmap belongs to.
	 */

	final int memClass = ((ActivityManager) PodcastApp.getInstance()
			.getSystemService(Context.ACTIVITY_SERVICE)).getMemoryClass();

	// Use 1/8th of the available memory for this memory cache.
	final int cacheSize = 1024 * 1024 * memClass / 8;
	private LruCache<Long, Bitmap> imageCache;

	private FeedImageLoader() {
		Log.d(TAG, "Creating cache with size " + cacheSize);
		imageCache = new LruCache<Long, Bitmap>(cacheSize) {

			@SuppressLint("NewApi")
			@Override
			protected int sizeOf(Long key, Bitmap value) {
				if (Integer.valueOf(android.os.Build.VERSION.SDK_INT) >= 12)
					return value.getByteCount();
				else
					return (value.getRowBytes() * value.getHeight());

			}

		};
	}

	public static FeedImageLoader getInstance() {
		if (singleton == null) {
			singleton = new FeedImageLoader();
		}
		return singleton;
	}

	public void loadBitmap(FeedImage image, ImageView target) {
		if (image != null) {
			Bitmap bitmap = getBitmapFromCache(image.getId());
			if (bitmap != null) {
				target.setImageBitmap(bitmap);
			} else {
				target.setImageResource(R.drawable.default_cover);
				BitmapWorkerTask worker = new BitmapWorkerTask(target);
				worker.execute(image);
			}
		} else {
			target.setImageResource(R.drawable.default_cover);
		}
	}

	public void addBitmapToCache(long id, Bitmap bitmap) {
		imageCache.put(id, bitmap);
	}

	public void wipeImageCache() {
		imageCache.evictAll();
	}
	
	public boolean isInCache(FeedImage image) {
		return imageCache.get(image.getId()) != null;
	}

	public Bitmap getBitmapFromCache(long id) {
		return imageCache.get(id);
	}

	class BitmapWorkerTask extends AsyncTask<FeedImage, Void, Void> {
		private static final String TAG = "BitmapWorkerTask";
		private ImageView target;
		private Bitmap bitmap;

		public BitmapWorkerTask(ImageView target) {
			super();
			this.target = target;
		}

		@Override
		protected void onPostExecute(Void result) {
			super.onPostExecute(result);
			target.setImageBitmap(bitmap);
		}

		@Override
		protected Void doInBackground(FeedImage... params) {
			if (params[0].getFile_url() != null) {
				bitmap = BitmapFactory.decodeFile(params[0].getFile_url());
				addBitmapToCache(params[0].getId(), bitmap);
				Log.d(TAG, "Finished loading bitmaps");
			} else {
				Log.e(TAG, "FeedImage has no file url. Using default image");
				bitmap = BitmapFactory.decodeResource(target.getResources(),
						R.drawable.default_cover);
			}
			return null;
		}
	}

}
