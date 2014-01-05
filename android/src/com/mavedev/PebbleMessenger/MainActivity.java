package com.mavedev.PebbleMessenger;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.mavedev.PebbleMessenger.R;
import com.stericson.RootTools.RootTools;

public class MainActivity extends ListActivity
{
	private ArrayAdapter<String> mAdapter;

	@Override
	protected void onCreate(final Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		String[] options = new String[] { getResources().getString(R.string.edit_quick_responses),
				getResources().getString(R.string.check_for_updates),
				getResources().getString(R.string.settings),
				getResources().getString(R.string.about) };

		mAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, options);

		// Assign adapter to List
		setListAdapter(mAdapter);

		Intent intent = getIntent();
		if (intent != null && intent.hasExtra("update"))
		{
			int versionCode = intent.getIntExtra("versionCode", 0);
			String versionName = intent.getStringExtra("versionName");
			
			String updateWhat = intent.getStringExtra("update");
			if (updateWhat.equalsIgnoreCase("android"))
			{
				UpdateHelper.offerAndroidUpdate(this, versionCode, versionName);
			}
			else if (updateWhat.equalsIgnoreCase("watchapp"))
			{
				String notice = intent.hasExtra("notice") ? intent.getStringExtra("notice") : null;
				UpdateHelper.offerWatchappUpdate(this, versionCode, versionName, notice);
			}
		}
		
		if (!RootTools.isBusyboxAvailable())
		{
			AlertDialog.Builder alert = new AlertDialog.Builder(this);

			alert.setTitle("BusyBox");
			alert.setMessage("BusyBox was not found on your system. You will be forwarded to Google Play.");

			alert.setNeutralButton(android.R.string.ok, new DialogInterface.OnClickListener()
			{
				public void onClick(DialogInterface dialog, int whichButton)
				{
					RootTools.offerBusyBox(MainActivity.this);
				}
			});

			alert.show();
		}
	}

	@Override
	protected void onListItemClick(ListView l, View v, final int position, long id)
	{
		super.onListItemClick(l, v, position, id);

		Intent intent;

		switch ((int) id)
		{
		case 0: // edit quick responses
			intent = new Intent(this, QuickResponsesManager.class);
			startActivity(intent);
			break;

		case 1: // check for watchapp update
			UpdateHelper.checkForUpdate(this, false);
			break;

		case 2:
			intent = new Intent(this, SettingsActivity.class);
			startActivity(intent);
			break;

		case 3: // about
			intent = new Intent(this, AboutActivity.class);
			startActivity(intent);
			break;
		}
	}
}
