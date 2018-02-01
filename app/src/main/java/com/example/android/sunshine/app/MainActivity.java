package com.example.android.sunshine.app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

public class MainActivity extends AppCompatActivity
{
	private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
	private final String LOG_TAG = this.getClass().getSimpleName();
	private View m_snackbarView;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		Log.d(LOG_TAG, "onCreate");

		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		m_snackbarView = findViewById(R.id.snackbarPosition);

		if (savedInstanceState == null)
		{
			getSupportFragmentManager().beginTransaction()
				.add(R.id.container, new ForecastFragment())
				.commit();
		}
	}

	@Override
	protected void onStart()
	{
		super.onStart();
		Log.d(LOG_TAG, "onStart");
	}

	@Override
	protected void onResume()
	{
		super.onResume();
		Log.d(LOG_TAG, "onResume");
	}

	@Override
	protected void onPause()
	{
		super.onPause();
		Log.d(LOG_TAG, "onPause");
	}

	@Override
	protected void onStop()
	{
		super.onStop();
		Log.d(LOG_TAG, "onStop");
	}

	@Override
	protected void onDestroy()
	{
		super.onDestroy();
		Log.d(LOG_TAG, "onDestroy");
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		int id = item.getItemId();
		if (id == R.id.action_settings)
		{
			startActivity(new Intent(this, SettingsActivity.class));
			return true;
		}
		else if (id == R.id.action_map)
		{
			openPreferredLocationInMap();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	private void openPreferredLocationInMap()
	{
		SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
		String location = sharedPrefs.getString(getString(R.string.pref_location_key), getString(R.string.pref_location_default));

		Uri geoLocation = Uri.parse("geo:0,0?").buildUpon()
			.appendQueryParameter("q", location).build();

		Intent viewIntent = new Intent(Intent.ACTION_VIEW);
		viewIntent.setData(geoLocation);

		if (viewIntent.resolveActivity(getPackageManager()) != null)
		{ startActivity(viewIntent); }
		else
		{ Log.d(LOG_TAG, "Couldn't call " + location + ", no map found!"); }
	}

	@Override
	public void onRequestPermissionsResult(
		int requestCode,
		@NonNull String[] permissions,
		@NonNull int[] grantResults)
	{
		if (requestCode == LOCATION_PERMISSION_REQUEST_CODE)
		{
			if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
			{
				Snackbar.make(m_snackbarView,
					"Location permission successfully granted.", Snackbar.LENGTH_SHORT).show();
			}
			else
			{
				Snackbar.make(m_snackbarView,
					"Location permission denied.", Snackbar.LENGTH_LONG).show();
			}
		}
	}
}
