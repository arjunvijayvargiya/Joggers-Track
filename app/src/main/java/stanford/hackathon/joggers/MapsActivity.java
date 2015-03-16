package stanford.hackathon.joggers;

import android.annotation.TargetApi;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Build;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.microsoft.windowsazure.mobileservices.MobileServiceClient;
import com.microsoft.windowsazure.mobileservices.MobileServiceTable;
import com.microsoft.windowsazure.mobileservices.ServiceFilterResponse;
import com.microsoft.windowsazure.mobileservices.TableOperationCallback;
import com.microsoft.windowsazure.mobileservices.TableQueryCallback;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import static com.microsoft.windowsazure.mobileservices.MobileServiceQueryOperations.val;

public class MapsActivity extends FragmentActivity {

    private GoogleMap mMap; // Might be null if Google Play services APK is not available.\
    private List<Double> lat=new ArrayList<>();
    private List<Double> lon=new ArrayList<>();
    private List<String> title=new ArrayList<>();
    private Location getLocation()
    {
        AppLocationService appLocationService = new AppLocationService(MapsActivity.this);
        Location location = appLocationService.getLocation(LocationManager.NETWORK_PROVIDER);
        if(location==null)location=appLocationService.getLocation(LocationManager.GPS_PROVIDER);
        if(location==null)location=appLocationService.getLocation(LocationManager.PASSIVE_PROVIDER);
        if (location == null) {
            location.setLatitude(17.23);
            location.setLongitude(78.35);
        }
        return location;
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        setupActionBar();
        mMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map))
                .getMap();
        try{
            mMap.setMyLocationEnabled(true);
        Location loc=getLocation();
        LatLng curr=new LatLng(loc.getLatitude(),loc.getLongitude());
        CameraUpdate center = CameraUpdateFactory.newLatLng(curr);
        CameraUpdate zoom = CameraUpdateFactory.zoomTo(18);
        mMap.moveCamera(center);
        mMap.animateCamera(zoom);
        }catch(Exception e){System.out.println("Location"+e.toString());}
        try {List<Item> result=SplashActivity.markerList;
            for (Item item : result) {
                            final LatLng mark = new LatLng(item.lat,item.lon);
                            mMap.addMarker(new MarkerOptions().position(mark)
                                    .title(item.title+" | "+item.playlist)
                                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.marker)))
                            .showInfoWindow();
                            lat.add(item.lat);
                            lon.add(item.lon);
                            title.add(item.title);
         }
            mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
                @Override
                public boolean onMarkerClick(Marker marker) {
                    String title=marker.getTitle();
                    String playlist=title.substring(title.indexOf('|')+2);
                    title=title.substring(0,title.indexOf('|')-1);
                    new SongFetcherTask(title,playlist).execute();
                    return false;
                }
            });

        } catch (Exception e) {
            System.out.println("There was an error creating the Mobile Service. Verify the URL");
        }

        new MarkersDownloadTask().execute();
    }
    private MobileServiceTable<Item> mToDoTable;
    private MobileServiceTable mTableContainers;
    private MobileServiceClient mClient;
    class MarkersDownloadTask extends AsyncTask<Void, Void, Boolean> {
        @Override
        protected void onPreExecute() {
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            try {
                // Create the Mobile Service Client instance, using the provided
                // Mobile Service URL and key
                mClient = new MobileServiceClient("https://joggers.azure-mobile.net/","xNuXCWcFCMzhHBjAgncgVRppUyVONF71",getApplicationContext());
                // Get the Mobile Service Table instance to use
                mToDoTable = mClient.getTable(Item.class);
                mToDoTable.where().execute(new TableQueryCallback<Item>() {
                    public void onCompleted(List<Item> result, int count, Exception exception, ServiceFilterResponse response) {
                        if (exception == null) {
                            SplashActivity.markerList = result;
                        } else {
                            System.out.println("Error" + exception);
                        }
                    }
                });
                Thread.sleep(500);
            } catch (Exception ex) {
                Log.e("Maps", ex.getMessage());
            }
            return false;
        }
        @Override
        protected void onPostExecute(Boolean uploaded) {
            if(mMap!=null)mMap.clear();
            try {List<Item> result=SplashActivity.markerList;
                for (Item item : result) {
                    final LatLng mark = new LatLng(item.lat,item.lon);
                    mMap.addMarker(new MarkerOptions().position(mark)
                            .title(item.title+" | "+item.playlist)
                            .icon(BitmapDescriptorFactory.fromResource(R.drawable.marker)))
                            .showInfoWindow();
                    lat.add(item.lat);
                    lon.add(item.lon);
                    title.add(item.title);
                }
                mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
                    @Override
                    public boolean onMarkerClick(Marker marker) {
                        String title=marker.getTitle();
                        String playlist=title.substring(title.indexOf('|')+2);
                        title=title.substring(0,title.indexOf('|')-1);
                        new SongFetcherTask(title,playlist).execute();
                        return false;
                    }
                });

            } catch (Exception e) {
                System.out.println("There was an error creating the Mobile Service. Verify the URL");
            }

        }
    }

    private class SongFetcherTask extends AsyncTask<Void, Integer, Boolean> {
        private String mContainerName,mBlobName;
        private final ProgressDialog dialog = new ProgressDialog(MapsActivity.this);
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
        public SongFetcherTask(String title,String folder) {
            mContainerName = folder;mBlobName=title;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            try {
                try {
                    File cacheDir = new File(android.os.Environment.getExternalStorageDirectory(), "Joggers/"+mContainerName);
                    if (!cacheDir.exists())
                        cacheDir.mkdirs();

                    File f = new File(cacheDir,mBlobName + ".mp3");
                    URL url = new URL("https://joggers.blob.core.windows.net/"+mContainerName+"/"+mBlobName);

                    InputStream input = new BufferedInputStream(url.openStream());
                    OutputStream output = new FileOutputStream(f);

                    byte data[] = new byte[1024];
                    long total = 0;
                    int count = 0;
                    while ((count = input.read(data)) != -1) {
                        if(dialog.isShowing()){
                        total++;
                        Log.e("while", "#" + count + "A" + total);
                        output.write(data, 0, count);}
                        else
                        {output.flush();
                            output.close();
                            input.close();return false;}
                    }
                    output.flush();
                    output.close();
                    input.close();
                }
                catch(Exception e) {
                    e.printStackTrace();
                }
            } catch (Exception e) {
                Log.e("MapsActivity", e.getMessage());
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
            if(loaded)Toast.makeText(getApplicationContext(),"Song downloaded at Joggers/"+mContainerName+"/"+mBlobName,Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        setUpMapIfNeeded();
    }

    /**
     * Sets up the map if it is possible to do so (i.e., the Google Play services APK is correctly
     * installed) and the map has not already been instantiated.. This will ensure that we only ever
     *
     * <p/>
     * If it isn't installed {@link SupportMapFragment} (and
     * {@link com.google.android.gms.maps.MapView MapView}) will show a prompt for the user to
     * install/update the Google Play services APK on their device.
     * <p/>
     * A user can return to this FragmentActivity after following the prompt and correctly
     * installing/updating/enabling the Google Play services. Since the FragmentActivity may not
     * have been completely destroyed during this process (it is likely that it would only be
     * stopped or paused), {@link #onCreate(Bundle)} may not be called again so we should call this
     * method in {@link #onResume()} to guarantee that it will be called.
     */
    private void setUpMapIfNeeded() {
        // Do a null check to confirm that we have not already instantiated the map.
        if (mMap == null) {
            // Try to obtain the map from the SupportMapFragment.
           mMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map))
                    .getMap();
        }
    }

    /**
     * This is where we can add markers or lines, add listeners or move the camera. In this case, we
     * just add a marker near Africa.
     * <p/>
     * This should only be called once and when we are sure that {@link #mMap} is not null.
     */
   /* private void setUpMap() {
        mMap.addMarker(new MarkerOptions().position(new LatLng(0, 0)).title("Marker"));
    }*/
    /**
     * Set up the {@link android.app.ActionBar}, if the API is available.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private void setupActionBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            getActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.maps, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(this);
        }
                return true;
    }
}
