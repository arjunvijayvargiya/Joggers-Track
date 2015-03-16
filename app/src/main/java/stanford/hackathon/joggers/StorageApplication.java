package stanford.hackathon.joggers;

import android.app.Application;

/***
 * This class gives us a singleton instance of Storage Service to be used throughout
 * our application.
 *
 */
public class StorageApplication extends Application {

	private StorageService mStorageService;
	
	public StorageApplication() {}
	
	public StorageService getStorageService() {
		if (mStorageService == null) {
			mStorageService = new StorageService(this);
		}
		return mStorageService;
	}
		
}
