package com.mavedev.PebbleMessenger;

import java.lang.reflect.Method;

import org.json.JSONException;
import org.json.JSONObject;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;

import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;

public class UpdateHelper
{
	private static final String VERSION_INFO_URL = "http://dl.dropboxusercontent.com/u/69214204/PebbleWhatsApp/version.txt";
	private static final String WATCHAPP_DOWNLOAD_URL = "http://dl.dropboxusercontent.com/u/69214204/PebbleWhatsApp/latest.pbw";
	private static final String ANDROID_APP_DOWNLOAD_URL = "http://dl.dropboxusercontent.com/u/69214204/PebbleWhatsApp/latest.apk";

	public static final String UPDATE_PREFERENCES = "PebbleWhatsAppUpdate";

	public static void offerWatchappUpdate(final Context cx, final int versionCode,
			final String versionName, String notice)
	{
		AlertDialog.Builder alert = new AlertDialog.Builder(cx);

		alert.setTitle("Update");
		alert.setMessage("Watchapp update\nDo you want to update your watchapp to version " + versionName + "?" + (notice != null ? "\n\n" + notice : ""));

		alert.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener()
		{
			public void onClick(DialogInterface dialog, int whichButton)
			{
				doUpdate(cx, versionCode);
			}
		});

		alert.setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener()
		{
			public void onClick(DialogInterface dialog, int whichButton)
			{
			}
		});

		alert.show();
	}

	public static void offerAndroidUpdate(final Context cx, final int versionCode,
			final String versionName)
	{
		AlertDialog.Builder alert = new AlertDialog.Builder(cx);

		alert.setTitle("Update");
		alert.setMessage("Android app update\nDo you want to update your Android app to version " + versionName + "?");

		alert.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener()
		{
			public void onClick(DialogInterface dialog, int whichButton)
			{
				Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(ANDROID_APP_DOWNLOAD_URL));
				intent.addCategory(Intent.CATEGORY_BROWSABLE);
				cx.startActivity(intent);
			}
		});

		alert.setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener()
		{
			public void onClick(DialogInterface dialog, int whichButton)
			{
			}
		});

		alert.show();
	}

	private static void doUpdate(Context cx, int versionCode)
	{
		Intent i = new Intent(Intent.ACTION_VIEW);
		i.addCategory(Intent.CATEGORY_BROWSABLE);
		i.setClassName("com.getpebble.android", "com.getpebble.android.ui.UpdateActivity");
		i.setData(Uri.parse(WATCHAPP_DOWNLOAD_URL));
		try
		{
			cx.startActivity(i);

			if (versionCode > 0)
			{
				SharedPreferences pref = cx.getSharedPreferences(UPDATE_PREFERENCES,
						Context.MODE_PRIVATE);
				pref.edit().putInt("watchappVersion", versionCode).commit();
			}
		}
		catch (ActivityNotFoundException e)
		{
			e.printStackTrace();
		}
	}
	
	@SuppressWarnings("deprecation")
	private static void notifyAndroidUpdate(Context cx, int versionCode, String version)
	{
		// prepare intent which is triggered if the
		// notification is selected

		Intent intent = new Intent(cx, MainActivity.class);
		intent.putExtra("update", "android");
		intent.putExtra("versionCode", versionCode);
		intent.putExtra("versionName", version);
		PendingIntent pIntent = PendingIntent.getActivity(cx, 0, intent, 0);

		// build notification
		// the addAction re-use the same intent to keep the example short
		Notification.Builder nb = new Notification.Builder(cx).setContentTitle("Pebble WhatsApp")
				.setContentText("New Android app update available: " + version)
				.setSmallIcon(android.R.drawable.stat_notify_sync_noanim).setContentIntent(pIntent)
				.setAutoCancel(true);

		Notification n;
		try
		{
			Method m = Notification.Builder.class.getMethod("build", new Class[] {});
			n = (Notification) m.invoke(nb);
		}
		catch (Exception e)
		{
			n = nb.getNotification();
		}

		NotificationManager notificationManager = (NotificationManager) cx
				.getSystemService(Context.NOTIFICATION_SERVICE);
		notificationManager.notify(0, n);
	}

	@SuppressWarnings("deprecation")
	private static void notifyWatchappUpdate(Context cx, int versionCode, String version, String notice)
	{
		// prepare intent which is triggered if the
		// notification is selected

		Intent intent = new Intent(cx, MainActivity.class);
		intent.putExtra("update", "watchapp");
		intent.putExtra("versionCode", versionCode);
		intent.putExtra("versionName", version);
		if (notice != null)
			intent.putExtra("notice", notice);
		PendingIntent pIntent = PendingIntent.getActivity(cx, 0, intent, 0);

		// build notification
		// the addAction re-use the same intent to keep the example short
		Notification.Builder nb = new Notification.Builder(cx).setContentTitle("Pebble WhatsApp")
				.setContentText("New watchapp update available: " + version)
				.setSmallIcon(android.R.drawable.stat_notify_sync_noanim).setContentIntent(pIntent)
				.setAutoCancel(true);

		Notification n;
		try
		{
			Method m = Notification.Builder.class.getMethod("build", new Class[] {});
			n = (Notification) m.invoke(nb);
		}
		catch (Exception e)
		{
			n = nb.getNotification();
		}

		NotificationManager notificationManager = (NotificationManager) cx
				.getSystemService(Context.NOTIFICATION_SERVICE);
		notificationManager.notify(0, n);
	}

	private static void notifyAlreadyLatest(Context cx)
	{
		AlertDialog.Builder alert = new AlertDialog.Builder(cx);

		alert.setTitle("Update");
		alert.setMessage("You're using the latest version of the Android app and the watchapp :)");

		alert.setNeutralButton(android.R.string.ok, new DialogInterface.OnClickListener()
		{
			public void onClick(DialogInterface dialog, int whichButton)
			{
			}
		});

		alert.show();
	}

	public static void checkForUpdate(final Context cx, final boolean automatic)
	{
		SharedPreferences pref = cx.getSharedPreferences(UPDATE_PREFERENCES, Context.MODE_PRIVATE);
		if (!automatic && !pref.contains("watchappVersion"))
		{
			AlertDialog.Builder alert = new AlertDialog.Builder(cx);

			alert.setTitle("Update");
			alert.setMessage("I don't know what version your watch is using :(\nInstall latest watchapp anyway?");

			alert.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener()
			{
				public void onClick(DialogInterface dialog, int whichButton)
				{
					doUpdate(cx, -1);
				}
			});

			alert.setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener()
			{
				public void onClick(DialogInterface dialog, int whichButton)
				{
				}
			});

			alert.show();
			return;
		}
		final int installedVersion = pref.getInt("watchappVersion", 0);
		
		final ProgressDialog pd;
		if (!automatic)
		{
			pd = new ProgressDialog(cx);
			pd.setTitle("Checking...");
			pd.setMessage("Checking for updates...");
			pd.setCancelable(false);
			pd.setIndeterminate(true);
			pd.show();
		}
		else
			pd = null;

		AsyncHttpClient client = new AsyncHttpClient();
		client.get(VERSION_INFO_URL, new JsonHttpResponseHandler()
		{
			@Override
			public void onSuccess(JSONObject response)
			{
				if (pd != null)
					pd.dismiss();
				
				try
				{
					JSONObject watchapp = response.getJSONObject("watchapp");
					JSONObject androidapp = response.getJSONObject("android");
					
					int currentAndroidVersion = androidapp.getInt("currentVersion");
					String androidVersionName = androidapp.getString("versionName");

					PackageManager pm = cx.getPackageManager();
					PackageInfo info = pm.getPackageInfo(cx.getPackageName(), 0);
					if (currentAndroidVersion > info.versionCode)
					{
						if (automatic)
						{
							notifyAndroidUpdate(cx, currentAndroidVersion, androidVersionName);
						}
						else
						{
							offerAndroidUpdate(cx, currentAndroidVersion, androidVersionName);
						}
						return;
					}
					
					int currentVersion = Integer.parseInt(watchapp.getString("currentVersion"), 16);
					String versionName = watchapp.getString("versionName");
					String notice = watchapp.has("notice") ? watchapp.getString("notice") : null;

					if (currentVersion > installedVersion)
					{
						if (automatic)
						{
							notifyWatchappUpdate(cx, currentVersion, versionName, notice);
						}
						else
						{
							offerWatchappUpdate(cx, currentVersion, versionName, notice);
						}
					}
					else if (!automatic)
					{
						notifyAlreadyLatest(cx);
					}
				}
				catch (JSONException e)
				{
					e.printStackTrace();
				}
				catch (NameNotFoundException e)
				{
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		});

		pref.edit().putLong("lastUpdateCheck", System.currentTimeMillis()).commit();
	}

	public static void maybeAutoUpdate(Context cx)
	{
		SharedPreferences pref = cx.getSharedPreferences(UPDATE_PREFERENCES, Context.MODE_PRIVATE);
		long lastUpdateCheck = pref.getLong("lastUpdateCheck", 0);
		if (System.currentTimeMillis() - lastUpdateCheck > 24 * 60 * 60 * 1000)
		{
			// last update check was more than 1 day ago

			checkForUpdate(cx, true);
		}
	}
}
