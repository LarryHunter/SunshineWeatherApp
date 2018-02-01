package com.example.android.sunshine.app;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.text.format.Time;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.concurrent.TimeUnit;

public class ForecastFragment extends Fragment
{
	private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
	private static final String API_APP_ID = "dbf3860a9560b934c5f8f3acb79b633f";
	private ArrayAdapter<String> m_forecastAdapter;

	public ForecastFragment()
	{
	}

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		setHasOptionsMenu(true);
		super.onCreate(savedInstanceState);
	}

	@Override
	public void onStart()
	{
		super.onStart();
		upateWeather();
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater)
	{
		inflater.inflate(R.menu.forecastfragment, menu);
		super.onCreateOptionsMenu(menu, inflater);
	}
/*
	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		int id = item.getItemId();
		if (id == R.id.action_refresh)
		{
			upateWeather();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
*/
	private void upateWeather()
	{
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
		String location = prefs.getString(getString(R.string.pref_location_key), getString(R.string.pref_location_default));
		FetchWeatherTask weatherTask = new FetchWeatherTask();
		weatherTask.execute(location);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		m_forecastAdapter = new ArrayAdapter<>(
			getActivity(),
			R.layout.list_item_forecast,
			R.id.list_item_forecast_textview,
			new ArrayList<String>());

		View rootView = inflater.inflate(R.layout.fragment_main, container, false);

		// Get a reference of the ListView and attache the adapter to it
		final ListView forecastListView = (ListView) rootView.findViewById(R.id.listview_forecast);
		forecastListView.setAdapter(m_forecastAdapter);
		forecastListView.setOnItemClickListener(new AdapterView.OnItemClickListener()
		{
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id)
			{
				String forcast = m_forecastAdapter.getItem(position).toString();
				Intent detailIntent = new Intent(getContext(), DetailActivity.class);
				detailIntent.putExtra("ForecastDetail", forcast);
				startActivity(detailIntent);
			}
		});

		checkLocationPermission();
		return rootView;
	}

	private void checkLocationPermission()
	{
		// Check if the permission has already been granted
		if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_DENIED)
		{
			AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
			builder
				.setTitle(R.string.permission_required_title)
				.setMessage(R.string.location_permission_reasoning_text)
				.setCancelable(false)
				.setNegativeButton(R.string.button_text_ok, new DialogInterface.OnClickListener()
				{
					@Override
					public void onClick(DialogInterface dialog, int which)
					{
						showPermissionRequest();
					}

					private void showPermissionRequest()
					{
						String[] permission = new String[]{Manifest.permission.ACCESS_FINE_LOCATION};
						ActivityCompat.requestPermissions(getActivity(), permission, LOCATION_PERMISSION_REQUEST_CODE);
					}
				})
				.show();
		}
	}

	// AsyncTask<Params, Progress, Result>.
	//    Params – the type (Object/primitive) you pass to the AsyncTask from .execute()
	//    Progress – the type that gets passed to onProgressUpdate()
	//    Result – the type returns from doInBackground()
	// Any of them can be String, Integer, Void, etc.
	public class FetchWeatherTask extends AsyncTask<String, Void, String[]>
	{
		private final String LOG_TAG = FetchWeatherTask.class.getSimpleName();

		@Override
		protected void onPostExecute(String[] strings)
		{
			if (strings != null)
			{
				m_forecastAdapter.clear();
				m_forecastAdapter.addAll(strings);
			}
		}

		@Override
		protected String[] doInBackground(String... params)
		{
			// If there's no zip code, there's nothing to look up. Verify size of params
			if (params.length == 0)
			{
				return null;
			}

			// These two need to be declared outside the try/catch
			// so that they can be closed in the finally block.
			HttpURLConnection urlConnection = null;
			BufferedReader reader = null;

			// Will contain the raw JSON response as a string.
			String forecastJsonStr = null;

			String format = "json";
			String units = "metric";
			int numDays = 7;

			try
			{
				// Construct the URL for the OpenWeatherMap query
				// Possible parameters are available at OWM's forecast API page, at
				// http://openweathermap.org/API#forecast
				final String FORECAST_BASE_URL = "http://api.openweathermap.org/data/2.5/forecast/daily?";
				final String QUERY_PARAM_CITY = "q";
				final String QUERY_PARAM_ZIP = "zip";
				final String FORMAT_PARAM = "mode";
				final String UNITS_PARAM = "units";
				final String DAYS_PARAM = "cnt";
				final String APP_ID_PARAM = "appid";

				// Create the request to OpenWeatherMap, and open the connection
				Uri builtUri = Uri.parse(FORECAST_BASE_URL).buildUpon()
					.appendQueryParameter(QUERY_PARAM_ZIP, params[0])
					.appendQueryParameter(FORMAT_PARAM, format)
					.appendQueryParameter(UNITS_PARAM, units)
					.appendQueryParameter(DAYS_PARAM, Integer.toString(numDays))
					.appendQueryParameter(APP_ID_PARAM, API_APP_ID).build();

				//Log.d(LOG_TAG, "The Uri Builder output is: " + builtUri.toString());

				URL url = new URL(builtUri.toString());
				urlConnection = (HttpURLConnection) url.openConnection();
				urlConnection.setRequestMethod("GET");
				urlConnection.connect();

				// Read the input stream into a String
				InputStream inputStream = urlConnection.getInputStream();
				StringBuffer buffer = new StringBuffer();
				if (inputStream == null)
				{
					// Nothing to do.
					return null;
				}
				reader = new BufferedReader(new InputStreamReader(inputStream));

				String line;
				while ((line = reader.readLine()) != null)
				{
					// Since it's JSON, adding a newline isn't necessary (it won't affect parsing)
					// But it does make debugging a *lot* easier if you print out the completed
					// buffer for debugging.
					buffer.append(line + "\n");
				}

				if (buffer.length() == 0)
				{
					// Stream was empty.  No point in parsing.
					return null;
				}
				forecastJsonStr = buffer.toString();

				//Log.d(LOG_TAG, "Forecast JSON String: " + forecastJsonStr);
			}
			catch (IOException e)
			{
				Log.e(LOG_TAG, "Error ", e);
				// If the code didn't successfully get the weather data,
				// there's no point in attempting to parse it.
				return null;
			}
			finally
			{
				if (urlConnection != null)
				{
					urlConnection.disconnect();
				}
				if (reader != null)
				{
					try
					{
						reader.close();
					}
					catch (final IOException e)
					{
						Log.e(LOG_TAG, "Error closing stream", e);
					}
				}
			}

			String[] weatherDataArray = new String[numDays];
			try
			{
				weatherDataArray = getWeatherDataFromJson(forecastJsonStr, numDays);
			}
			catch (JSONException e)
			{
				Log.e(LOG_TAG, e.getMessage(), e);
				e.printStackTrace();
			}
			return weatherDataArray;
		}

		/**
		 * The date/time conversion code is going to be moved outside the asynctask later,
		 * so for convenience we're breaking it out into its own method now.
		 */
		private String getReadableDateString(long time)
		{
			// Because the API returns a unix timestamp (measured in seconds),
			// it must be converted to milliseconds in order to be converted to valid date.
			SimpleDateFormat shortenedDateFormat = new SimpleDateFormat("EEEE MMM dd");

			Calendar calendar = Calendar.getInstance();
			long timeDiff = calendar.getTimeInMillis() - time;
			if (timeDiff > 0 && timeDiff <= TimeUnit.HOURS.toMillis(24))
			{
				return getString(R.string.today_text_value);
			}
			else
			{
				return shortenedDateFormat.format(time);
			}
		}

		/**
		 * Prepare the weather high/lows for presentation.
		 */
		private String formatHighLows(double high, double low)
		{
			// For presentation, assume the user doesn't care about tenths of a degree.
			long roundedHigh = Math.round(high);
			long roundedLow = Math.round(low);

			//final String highArrow = getString(R.string.unicode_high_arrow);
			//final String lowArrow = getString(R.string.unicode_low_arrow);
			//String highLowStr = highArrow + roundedHigh + " / " + lowArrow + roundedLow;

			final String highStr = getString(R.string.high_string);
			final String lowStr = getString(R.string.low_string);
			String highLowStr = highStr + " " + roundedHigh + "° and " + lowStr + " " + roundedLow + "°";
			return highLowStr;
		}

		/**
		 * Take the String representing the complete forecast in JSON Format and
		 * pull out the data we need to construct the Strings needed for the wireframes.
		 * <p>
		 * Fortunately parsing is easy:  constructor takes the JSON string and converts it
		 * into an Object hierarchy for us.
		 */
		private String[] getWeatherDataFromJson(String forecastJsonStr, int numDays) throws JSONException
		{
			// These are the names of the JSON objects that need to be extracted.
			final String OWM_LIST = "list";
			final String OWM_WEATHER = "weather";
			final String OWM_TEMPERATURE = "temp";
			final String OWM_MAX = "max";
			final String OWM_MIN = "min";
			final String OWM_DESCRIPTION = "main";

			JSONObject forecastJson = new JSONObject(forecastJsonStr);
			JSONArray weatherArray = forecastJson.getJSONArray(OWM_LIST);

			// OWM returns daily forecasts based upon the local time of the city that is being
			// asked for, which means that we need to know the GMT offset to translate this data
			// properly.

			// Since this data is also sent in-order and the first day is always the
			// current day, we're going to take advantage of that to get a nice
			// normalized UTC date for all of our weather.

			Time dayTime = new Time();
			dayTime.setToNow();

			// we start at the day returned by local time. Otherwise this is a mess.
			int julianStartDay = Time.getJulianDay(System.currentTimeMillis(), dayTime.gmtoff);

			// now we work exclusively in UTC
			dayTime = new Time();

			String[] resultStrs = new String[numDays];
			for (int i = 0; i < weatherArray.length(); i++)
			{
				// For now, using the format "Day, description, hi/low"
				String day;
				String description;
				String highAndLow;

				// Get the JSON object representing the day
				JSONObject dayForecast = weatherArray.getJSONObject(i);

				// The date/time is returned as a long.  We need to convert that
				// into something human-readable, since most people won't read "1400356800" as
				// "this saturday".
				long dateTime;

				// Cheating to convert this to UTC time, which is what we want anyhow
				dateTime = dayTime.setJulianDay(julianStartDay + i);
				day = getReadableDateString(dateTime);

				// description is in a child array called "weather", which is 1 element long.
				JSONObject weatherObject = dayForecast.getJSONArray(OWM_WEATHER).getJSONObject(0);
				description = weatherObject.getString(OWM_DESCRIPTION);

				// Temperatures are in a child object called "temp".  Try not to name variables
				// "temp" when working with temperature.  It confuses everybody.
				JSONObject temperatureObject = dayForecast.getJSONObject(OWM_TEMPERATURE);
				double high = temperatureObject.getDouble(OWM_MAX);
				double low = temperatureObject.getDouble(OWM_MIN);

				SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
				String units = prefs.getString(getString(R.string.pref_units_key), getString(R.string.pref_units_default));

				if (units.equalsIgnoreCase(getString(R.string.pref_units_default)))
				{
					high = high * 1.8 + 32;
					low = low * 1.8 + 32;
					highAndLow = formatHighLows(high, low);
				}
				else
				{
					highAndLow = formatHighLows(high, low);
				}
				resultStrs[i] = day + ":  " + description + " with " + highAndLow;
			}
			return resultStrs;
		}
	}
}
