package stanford.hackathon.joggers;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Bundle;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class BlobDetailsActivity extends Activity {
	private final String TAG = "BlobDetailsActivity";
	private String mContainerName;
	private String mBlobName;
	private int mBlobPosition;
	private StorageService mStorageService;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_blob_details);

		// Show the Up button in the action bar.
		setupActionBar();
		//Get access to the storage service
		StorageApplication myApp = (StorageApplication) getApplication();
		mStorageService = myApp.getStorageService();
		//Get data from the intent that launched this activity
		Intent launchIntent = getIntent();
		mContainerName = launchIntent.getStringExtra("ContainerName");
		mBlobName = launchIntent.getStringExtra("BlobName");
		mBlobPosition = launchIntent.getIntExtra("BlobPosition", -1);		

		/*Button btnLoadWithSas = (Button) findViewById(R.id.btnLoadWithSas);
		btnLoadWithSas.setOnClickListener(new OnClickListener() {			
			@Override
			public void onClick(View v) {
				//Start the process of getting a SAS to read the blob
				//and then display it
				mStorageService.getBlobSas(mContainerName, mBlobName);
			}
		});*/

		Set<Entry<String, JsonElement>> set = mStorageService.getLoadedBlobObjects()[mBlobPosition].getAsJsonObject().entrySet();
		String url = "";
		String contentType = "";
		
		//Loop through each entry in the blob and handle it depending on it's key
		for (Entry<String, JsonElement> entry : set) {
			String key = entry.getKey();			
			if (key.equals("name")) {
				TextView lblBlobNameValue = (TextView) findViewById(R.id.lblBlobNameValue);
				lblBlobNameValue.setText(entry.getValue().getAsString());
			} else if (key.equals("url")) {
				url = entry.getValue().getAsString();
				TextView lblUrlValue = (TextView) findViewById(R.id.lblUrlValue);
				lblUrlValue.setText(url);
			} else if (key.equals("properties")) {
				//Pull the content-type out of the properties element
				JsonElement properties = entry.getValue();				
				contentType = properties.getAsJsonObject().getAsJsonPrimitive("Content-Type").getAsString();
				TextView lblContentTypeValue = (TextView) findViewById(R.id.lblContentTypeValue);
				lblContentTypeValue.setText(contentType);
			}					
		}
	}
    public void downloadSong(View v) {
        mStorageService.getBlobSas(mContainerName, mBlobName);
    }
	//This class specifically handles fetching a Song from a URL and setting
	//the image view source on the screen
	private class ImageFetcherTask extends AsyncTask<Void, Integer, Boolean> {
	    private String mUrl;
        private final ProgressDialog dialog = new ProgressDialog(BlobDetailsActivity.this);
        @Override
        protected void onPreExecute(){
            dialog.setMessage("Downloading the song. Please wait.");
            dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            dialog.setCancelable(true);
            dialog.setCanceledOnTouchOutside(false);
            dialog.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });
            dialog.show();
        }
	    public ImageFetcherTask(String url) {
	        mUrl = url;
	    }
String PATH="";
	    @Override
	    protected Boolean doInBackground(Void... params) {
	        try {
                try {
                    System.out.println(mUrl);
                    System.out.println("Folders:"+mContainerName);
                    System.out.println("Files:"+mBlobName);
                    File cacheDir = new File(android.os.Environment.getExternalStorageDirectory(), "Joggers/"+mContainerName);
                    if (!cacheDir.exists())
                        cacheDir.mkdirs();
                     PATH=cacheDir.getAbsolutePath();
                    File f = new File(cacheDir,mBlobName + ".mp3");
                    URL url = new URL(mUrl);
                    InputStream input = new BufferedInputStream(url.openStream());
                    OutputStream output = new FileOutputStream(f);

                    byte data[] = new byte[1024];
                    long total = 0;
                    int count = 0;
                    while ((count = input.read(data)) != -1) {
                        total++;
                        Log.e("while", "#" + count + "A" + total);
                        output.write(data, 0, count);
                    }
                    output.flush();
                    output.close();
                    input.close();
                }
                catch(Exception e) {
                    e.printStackTrace();
                }
	        } catch (Exception e) {
	        	Log.e(TAG, e.getMessage());
	        	return false;
	        }
	        return true;
	    }
	    /***
	     * If the image was loaded successfully, set the image view
	     */
	    @Override
	    protected void onPostExecute(Boolean loaded) {
            dialog.dismiss();
            try{
                MediaPlayer mMediaPlayer = new MediaPlayer();
                mMediaPlayer.setDataSource(PATH+"/"+mBlobName+".mp3");
                mMediaPlayer.prepare();
                mMediaPlayer.start();
            }
            catch(Exception e){System.out.println("Srujan"+e.toString());}
            /*AlertDialog.Builder builder = new AlertDialog.Builder(getApplicationContext());
            LayoutInflater inflater = ((Activity) getApplicationContext()).getLayoutInflater();
            View dialogView = inflater.inflate(R.layout.dialog_new_blob, null);
            final TextView txtBlobName = (TextView) dialogView.findViewById(R.id.txtBlobName);
            txtBlobName.setText("Do you want to play "+mBlobName+"?");
            builder.setView(dialogView)
                    .setPositiveButton("Play", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int id) {
                            try {
                                MediaPlayer mMediaPlayer = new MediaPlayer();
                                mMediaPlayer.setDataSource(fUrl
                                );
                                mMediaPlayer.prepare();
                                mMediaPlayer.start();
                                dialog.dismiss();
                            }catch(Exception e){
                                Toast.makeText(getApplicationContext(),e.toString(),Toast.LENGTH_LONG).show();
                            }
                        }
                    })
                    .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.cancel();
                        }
                    });
            //Show the dialog
            builder.show();*/
            Toast.makeText(getApplicationContext(), "Song downloaded at "+PATH, Toast.LENGTH_LONG).show();

        }
	}

	/**
	 * Set up the {@link android.app.ActionBar}.
	 */
	private void setupActionBar() {
		getActionBar().setDisplayHomeAsUpEnabled(true);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.blob_details, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			// This ID represents the Home or Up button. In the case of this
			// activity, the Up button is shown. Use NavUtils to allow users
			// to navigate up one level in the application structure. For
			// more details, see the Navigation pattern on Android Design:
			//
			// http://developer.android.com/design/patterns/navigation.html#up-vs-back
			//
			//NavUtils.navigateUpFromSameTask(this);
			onBackPressed();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	/***
	 * Handle registering for the broadcast action
	 */
	@Override
	protected void onResume() {
		IntentFilter filter = new IntentFilter();
		filter.addAction("blob.loaded");
		registerReceiver(receiver, filter);
		super.onResume();
	}
	
	/***
	 * Handle unregistering for broadcast action
	 */
	@Override
	protected void onPause() {
		unregisterReceiver(receiver);
		super.onPause();
	}
	
	/***
	 * This broadcast receiver handles things after the blob's SAS URL is fetched
	 */
	private BroadcastReceiver receiver = new BroadcastReceiver() {
		public void onReceive(Context context, android.content.Intent intent) {
			//Load the image using the SAS URL
			JsonObject blob = mStorageService.getLoadedBlob();
			String sasUrl = blob.getAsJsonPrimitive("sasUrl").toString();
			sasUrl = sasUrl.replace("\"", "");
			(new ImageFetcherTask(sasUrl)).execute();
		}
	};
}