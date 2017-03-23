/*
 * Copyright 2017 The MITRE Corporation
 * Approved for Public Release; Distribution Unlimited. Case Number 17-0137.
 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.

 * NOTICE
 * This software was produced for the U.S. Government under Basic Contract
 * No. W15P7T-13-C-A802, and is subject to the Rights in Noncommercial
 * Computer Software and Noncommercial Computer Software Documentation
 * Clause 252.227-7014 (FEB 2012)
 */

// This class gathers the device's current location and sends it to a remote server.

package com.example.uploaddataapp;

import android.Manifest;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings;
import android.util.Log;

import java.util.Timer;
import java.util.TimerTask;

import pub.devrel.easypermissions.EasyPermissions;

public class LocationService extends Service {

	private LocationManager lm;
	public Location loc;
	public long last_update;
	public long started;

	// Define a listener that responds to location updates
	LocationListener locationListener = new LocationListener() {
		public void onLocationChanged(Location location) {
			// Called when a new location is found by the network location provider.
			Log.d("LocationService", "Loc change");
			if (makeUseOfNewLocation(location)) {
				//	  sendMyLocation(location);
				last_update = System.currentTimeMillis();
				//lm.removeUpdates(locationListener);
			}

		}

		public void onStatusChanged(String provider, int status, Bundle extras) {
		}

		public void onProviderEnabled(String provider) {
		}

		public void onProviderDisabled(String provider) {
		}
	};

	private static final int TWO_MINUTES = 1000 * 60 * 2;

	private boolean makeUseOfNewLocation(Location newloc) {
		if (loc == null) {
			loc = newloc;
			return true;
		}
		if (newloc == null) {
			return false;
		}

		long timeDelta = newloc.getTime() - loc.getTime();
		boolean isMuchNewer = timeDelta > TWO_MINUTES;
		boolean isMuchOlder = timeDelta < -TWO_MINUTES;
		boolean isNewer = timeDelta > 0;

		if (isMuchNewer) {
			loc = newloc;
			return true;
		}
		if (isMuchOlder) {
			return false;
		}
		int accuracyDelta = (int) (newloc.getAccuracy() - loc.getAccuracy());
		boolean isLessAccurate = accuracyDelta > 0;
		boolean isMoreAccurate = accuracyDelta < 0;
		boolean isMuchLessAccurate = accuracyDelta > 200;

		if (isMoreAccurate) {
			loc = newloc;
			return true;
		} else if (isNewer && !isLessAccurate) {
			loc = newloc;
			return true;
		} else if (isNewer && !isMuchLessAccurate && (loc.getProvider().equals(newloc.getProvider()))) {
			loc = newloc;
			return true;
		}
		return false;

	}


	private static Timer t = new Timer();

	@Override
	public void onCreate() {
		// TODO Auto-generated method stub
		Log.d("LocationService", "Started up");
		try {
			lm = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
			lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);
			lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
			started = System.currentTimeMillis();

			t.schedule(new mainTask(), 5000, 5000);
		} catch (IllegalArgumentException e) {
			Log.d("LocationService", "Illegal argument exception"); // Probably means no GPS available
		} catch (SecurityException e) {
			Log.d("LocationService", "Security exception");
		}
	}

	public void sendMyLocation(Location l) {
		double lat = l.getLatitude();
		double lon = l.getLongitude();
		double alt = l.getAltitude();
		long time = l.getTime();
		float acc = l.getAccuracy();

		String myLoc = lat + " " + lon + " " + alt + " " + time + " " + acc;
		SendIntentService.sendStuff("GPS_" + Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID),
				myLoc);
	}


	private class mainTask extends TimerTask {
		public void run() {
			// If location info is available, send it and stop this task & service.
			// If not available yet, don't do anything yet, wait until timer calls this again.
			String perms[] = { Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION };
			if (loc != null) {
				if (EasyPermissions.hasPermissions(LocationService.this, perms)) {
					lm.removeUpdates(locationListener);
					sendMyLocation(loc);
				} else {
					Log.d("LocationService", "Does not hold permission ACCESS_COARSE_LOCATION or ACCESS_FINE_LOCATION");
				}
        		cancel();
        		stopSelf();
        	}

        }
    }    


	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return null;
	}
}
