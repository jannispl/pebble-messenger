package com.mavedev.PebbleMessenger;

import java.lang.reflect.Method;
import java.util.ArrayList;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import com.mavedev.PebbleMessenger.R;

public class QuickResponsesManager extends ListActivity
{
	private ArrayAdapter<String> mAdapter;
	private ArrayList<String> mResponses = new ArrayList<String>();

	public static final String PREF_FILE_NAME = "PebbleWhatsAppPref";

	private static final int MAX_QUICK_RESPONSES = 10;

	@Override
	protected void onCreate(final Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		try
		{
			Method m = Activity.class.getMethod("getActionBar", new Class[] {});
			Object o = m.invoke(this);

			Class<?> actionBarClass = Class.forName("android.app.ActionBar");
			Method m2 = actionBarClass.getMethod("setDisplayHomeAsUpEnabled",
					new Class[] { boolean.class });
			m2.invoke(o, true);
		}
		catch (Exception e)
		{
		}

		ListView listView = (ListView) findViewById(android.R.id.list);

		registerForContextMenu(listView);

		SharedPreferences pref = getSharedPreferences(PREF_FILE_NAME, MODE_PRIVATE);
		int numQuickResponses = pref.getInt("numQuickResponses", 0);
		for (int i = 0; i < numQuickResponses; ++i)
		{
			mResponses.add(pref.getString("quickResponse" + i, "N/A"));
		}

		// Define a new Adapter
		// First parameter - Context
		// Second parameter - Layout for the row
		// Third - the Array of data

		mAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, mResponses);

		// Assign adapter to List
		setListAdapter(mAdapter);
	}

	@Override
	protected void onPause()
	{
		super.onPause();
	}

	@Override
	protected void onResume()
	{
		super.onResume();
	}

	@Override
	protected void onListItemClick(ListView l, View v, final int position, long id)
	{
		super.onListItemClick(l, v, position, id);

		openMessageEditor((int) id);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		// Inflate the menu items for use in the action bar
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.options_menu, menu);
		return super.onCreateOptionsMenu(menu);
	}

	private void addQuickResponse(String message)
	{
		SharedPreferences pref = getSharedPreferences(PREF_FILE_NAME, MODE_PRIVATE);

		int numQuickResponses = pref.getInt("numQuickResponses", 0);

		pref.edit().putInt("numQuickResponses", numQuickResponses + 1)
				.putString("quickResponse" + numQuickResponses, message).commit();

		mResponses.add(numQuickResponses, message);
		mAdapter.notifyDataSetChanged();
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		// Handle presses on the action bar items
		switch (item.getItemId())
		{
		case R.id.add_quick_response:
			if (mResponses.size() >= MAX_QUICK_RESPONSES)
			{
				Toast.makeText(
						this,
						"You can only have a maximum of " + MAX_QUICK_RESPONSES
								+ " quick responses", Toast.LENGTH_LONG).show();
				return true;
			}

			AlertDialog.Builder alert = new AlertDialog.Builder(this);

			alert.setTitle(R.string.add_quick_response);
			alert.setMessage(R.string.add_quick_response_help);

			// Set an EditText view to get user input
			final EditText input = new EditText(this);
			alert.setView(input);

			alert.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener()
			{
				public void onClick(DialogInterface dialog, int whichButton)
				{
					String value = input.getText().toString().trim();

					if (value.length() >= 100)
					{
						Toast.makeText(QuickResponsesManager.this,
								"Message is too long (max 100 characters)", Toast.LENGTH_LONG)
								.show();
					}
					else
					{
						addQuickResponse(value);
					}
				}
			});

			alert.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener()
			{
				public void onClick(DialogInterface dialog, int whichButton)
				{
					// Canceled.
				}
			});

			alert.show();
			return true;
		}

		return super.onOptionsItemSelected(item);
	}

	private void openMessageEditor(final int id)
	{
		final SharedPreferences pref = getSharedPreferences(PREF_FILE_NAME, MODE_PRIVATE);
		String message = pref.getString("quickResponse" + id, "N/A");

		AlertDialog.Builder alert = new AlertDialog.Builder(this);

		alert.setTitle(R.string.edit_quick_response);

		// Set an EditText view to get user input
		final EditText input = new EditText(this);
		input.setText(message);
		alert.setView(input);

		alert.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener()
		{
			public void onClick(DialogInterface dialog, int whichButton)
			{
				String value = input.getText().toString().trim();

				if (value.length() >= 100)
				{
					Toast.makeText(QuickResponsesManager.this,
							"Message is too long (max 100 characters)", Toast.LENGTH_LONG).show();
				}
				else
				{
					mResponses.set(id, value);
					mAdapter.notifyDataSetChanged();
					pref.edit().putString("quickResponse" + id, value).commit();
				}
			}
		});

		alert.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener()
		{
			public void onClick(DialogInterface dialog, int whichButton)
			{
				// Canceled.
			}
		});

		alert.show();
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo)
	{
		menu.add(Menu.NONE, 0, Menu.NONE, R.string.edit);
		menu.add(Menu.NONE, 1, Menu.NONE, R.string.delete);
	}

	@Override
	public boolean onContextItemSelected(MenuItem item)
	{
		AdapterView.AdapterContextMenuInfo info;
		try
		{
			info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
		}
		catch (ClassCastException e)
		{
			Log.e("MainActivity", "bad menuInfo", e);
			return false;
		}
		long id = getListAdapter().getItemId(info.position);

		int actionId = item.getItemId();
		switch (actionId)
		{
		case 0: // edit
			openMessageEditor((int) id);
			break;

		case 1: // delete
			SharedPreferences pref = getSharedPreferences(PREF_FILE_NAME, MODE_PRIVATE);

			int numQuickResponses = pref.getInt("numQuickResponses", 0);

			SharedPreferences.Editor edit = pref.edit().putInt("numQuickResponses",
					numQuickResponses - 1);

			for (int i = (int) id; i < numQuickResponses; ++i)
			{
				if (pref.contains("quickResponse" + (i + 1)))
				{
					edit.putString("quickResponse" + i,
							pref.getString("quickResponse" + (i + 1), "N/A"));
				}
				else
				{
					edit.remove("quickResponse" + i);
				}
			}
			edit.commit();

			mResponses.remove((int) id);
			mAdapter.notifyDataSetChanged();

			return true;
		}

		return false;
	}
}
