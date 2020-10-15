/*
 * Copyright (c) 2020 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */
package org.dpppt.android.calibration;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import org.dpppt.android.calibration.controls.ControlsFragment;
import org.dpppt.android.calibration.handshakes.HandshakesFragment;
import org.dpppt.android.calibration.logs.LogsFragment;
import org.dpppt.android.calibration.parameters.ParametersFragment;
import org.dpppt.android.sdk.DP3T;

public class MainActivity extends AppCompatActivity {

	private ActivityResultLauncher<String[]> requestPermissionLauncher =
			registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), isGranted -> {
				Boolean locationGranted = isGranted.get(Manifest.permission.ACCESS_FINE_LOCATION);

				if (locationGranted == null || !locationGranted) {
					new AlertDialog.Builder(this)
						.setTitle("Missing required permission")
						.setMessage("This application requires access to the device's location" +
									" and will now close.")
						.setIcon(R.drawable.ic_error)
						.setCancelable(false)
						.setPositiveButton("OK", (view, arg) -> {
							finishAndRemoveTask();
						}).create().show();
				}
			});

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) !=
				PackageManager.PERMISSION_GRANTED) {
			requestPermissionLauncher.launch(new String[]{
					Manifest.permission.ACCESS_FINE_LOCATION,
					Manifest.permission.ACCESS_BACKGROUND_LOCATION,
			});
		}

		setupNavigationView();

		if (savedInstanceState == null) {
			getSupportFragmentManager().beginTransaction()
					.add(R.id.main_fragment_container, ControlsFragment.newInstance())
					.commit();
		}
	}

	private void setupNavigationView() {
		BottomNavigationView navigationView = findViewById(R.id.main_navigation_view);
		navigationView.inflateMenu(R.menu.menu_navigation_main);

		navigationView.setOnNavigationItemSelectedListener(item -> {
			switch (item.getItemId()) {
				case R.id.action_controls:
					getSupportFragmentManager().beginTransaction()
							.replace(R.id.main_fragment_container, ControlsFragment.newInstance())
							.commit();
					break;
				case R.id.action_parameters:
					getSupportFragmentManager().beginTransaction()
							.replace(R.id.main_fragment_container, ParametersFragment.newInstance())
							.commit();
					break;
				case R.id.action_handshakes:
					getSupportFragmentManager().beginTransaction()
							.replace(R.id.main_fragment_container, HandshakesFragment.newInstance())
							.commit();
					break;
				case R.id.action_logs:
					getSupportFragmentManager().beginTransaction()
							.replace(R.id.main_fragment_container, LogsFragment.newInstance())
							.commit();
					break;
			}
			return true;
		});
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
		boolean handled = DP3T.onActivityResult(this, requestCode, resultCode, data);

		if (!handled) {
			super.onActivityResult(requestCode, resultCode, data);
		}
	}

}
