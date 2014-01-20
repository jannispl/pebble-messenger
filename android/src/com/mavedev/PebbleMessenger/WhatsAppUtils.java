package com.mavedev.PebbleMessenger;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.stericson.RootTools.RootTools;
import com.stericson.RootTools.execution.Command;
import com.stericson.RootTools.execution.Shell;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.provider.BaseColumns;
import android.provider.ContactsContract;
import android.util.Log;
import android.widget.Toast;

public class WhatsAppUtils
{
	public static class Conversation
	{
		public String jid;
		public String fullName;
	}

	public static final String DB_LOCATION = "/data/data/com.whatsapp/databases/msgstore.db";

	private static long start_timestamp = System.currentTimeMillis() / 1000L;
	private static int msg_counter = 0;

	public static String getContactDisplayNameByNumber(Context cx, String number)
	{
		Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
				Uri.encode(number));
		String name = number;

		ContentResolver contentResolver = cx.getContentResolver();
		Cursor contactLookup = contentResolver.query(uri, new String[] { BaseColumns._ID,
				ContactsContract.PhoneLookup.DISPLAY_NAME }, null, null, null);

		try
		{
			if (contactLookup != null && contactLookup.getCount() > 0)
			{
				contactLookup.moveToNext();
				name = contactLookup.getString(contactLookup
						.getColumnIndex(ContactsContract.Data.DISPLAY_NAME));
				// String contactId =
				// contactLookup.getString(contactLookup.getColumnIndex(BaseColumns._ID));
			}
		}
		finally
		{
			if (contactLookup != null)
			{
				contactLookup.close();
			}
		}

		return name;
	}
	
	private static String checkRootStuff()
	{
		if (!RootTools.isAccessGiven())
		{
			return "No root access";
		}
		
		if (!RootTools.isBusyboxAvailable())
		{
			return "No busybox";
		}
		
		return null;
	}
	
	private static boolean copyDatabase(String from, String to, int ownerUid)
	{
		try
		{
			Shell shell = RootTools.getShell(true);
			
			Command cmd = new Command(0, "busybox cp -f \"" + from + "\" \"" + to + "\"", "busybox chown " + ownerUid + "." + ownerUid + " \"" + to + "\"")
			{
				@Override
				public void commandCompleted(int id, int exitCode)
				{
					Log.d("WhatsAppUtils", "commandCompleted: id " + id + ", exit code " + exitCode);
				}

				@Override
				public void commandOutput(int id, String line)
				{
					Log.d("WhatsAppUtils", "commandOutput: id " + id + ", output '" + line + "'");
				}

				@Override
				public void commandTerminated(int id, String reason)
				{
					Log.d("WhatsAppUtils", "commandTerminated: id " + id + ", reason '" + reason + "'");
				}
			};
			shell.add(cmd);
			synchronized (cmd)
			{
				cmd.wait();
			}
			
			int exitCode = cmd.getExitCode();
			if (exitCode != 0)
			{
				Log.d("WhatsAppUtils", "copyDatabase exit code " + exitCode);
				return false;
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
			return false;
		}

		/*File destFile = new File(to);
		if (!destFile.exists())
		{
			return false;
		}*/
		
		return true;
	}

	public static List<Conversation> getConversations(Context cx)
	{
		RootTools.debugMode = true;
		
		ArrayList<Conversation> al = new ArrayList<Conversation>();

		String err = checkRootStuff();
		if (err != null)
		{
			Toast.makeText(cx, "getConversations failed - " + err, Toast.LENGTH_LONG).show();
			return al;
		}
		
		File cacheDir = cx.getCacheDir();
		cacheDir.mkdirs();
		String destPath = cacheDir.getPath();
		destPath = destPath.substring(0, destPath.lastIndexOf("/")) + "/whatsapp.db";
		int myUid = android.os.Process.myUid();;
		
		if (!copyDatabase(DB_LOCATION, destPath, myUid))
		{
			Toast.makeText(cx, "getConversations failed - unable to copy database", Toast.LENGTH_LONG).show();
			return al;
		}
		
		SQLiteDatabase db;
		try
		{
			db = SQLiteDatabase.openDatabase(destPath, null, SQLiteDatabase.OPEN_READONLY);
		}
		catch (SQLiteException e)
		{
			e.printStackTrace();
			
			Toast.makeText(cx, "getConversation failed - " + e.toString(), Toast.LENGTH_LONG).show();
			return al;
		}

		Cursor cur = db.query("chat_list", new String[] { "key_remote_jid" }, null, null, null,
				null, "message_table_id DESC");
		if (cur != null)
		{
			if (cur.moveToFirst())
			{
				int remoteJidIdx = cur.getColumnIndex("key_remote_jid");

				do
				{
					Conversation conv = new Conversation();
					conv.jid = cur.getString(remoteJidIdx);

					int idx = conv.jid.indexOf('@');
					String number = "+" + conv.jid.substring(0, idx);
					if (number.indexOf('-') != -1)
					{
						conv.fullName = conv.jid;

						// probably a group conversation
						if (conv.jid.substring(idx + 1).equals("g.us"))
						{
							// yep, is a group conversation. get the name
							Cursor cur2 = db.query("messages", new String[] { "data" }, "key_remote_jid = ? AND status = 6 AND media_size = 1", new String[] { conv.jid }, null, null, "timestamp DESC");
							if (cur2 != null)
							{
								int dataColumn = cur2.getColumnIndex("data");
								if (cur2.moveToFirst())
								{
									conv.fullName = cur2.getString(dataColumn);
								}
								cur2.close();
							}
						}
					}
					else
					{
						conv.fullName = getContactDisplayNameByNumber(cx, number);
					}

					al.add(conv);
				}
				while (cur.moveToNext());
			}

			cur.close();
		}

		db.close();

		return al;
	}

	public static boolean sendMessage(Context cx, String receiverNumber, String message)
	{
		RootTools.debugMode = true;
		
		String err = checkRootStuff();
		if (err != null)
		{
			Toast.makeText(cx, "sendMessage failed - " + err, Toast.LENGTH_LONG).show();
			return false;
		}
		
		++msg_counter;
		String key_id = "" + start_timestamp + "-" + msg_counter;
		String server = receiverNumber.indexOf('-') != -1 ? "g.us" : "s.whatsapp.net";
		
		ContentValues cv = new ContentValues();
		cv.put("key_remote_jid", receiverNumber + "@" + server);
		cv.put("key_from_me", 1);
		cv.put("key_id", key_id);
		cv.put("status", 0);
		cv.put("needs_push", 0);
		cv.put("data", message);
		cv.put("timestamp", System.currentTimeMillis());
		cv.putNull("media_url");
		cv.putNull("media_mime_type");
		cv.put("media_wa_type", 0);
		cv.put("media_size", 0);
		cv.putNull("media_name");
		cv.put("latitude", 0.0);
		cv.put("longitude", 0.0);
		cv.putNull("thumb_image");
		cv.putNull("remote_resource");
		cv.put("received_timestamp", System.currentTimeMillis());
		cv.put("send_timestamp", -1);
		cv.put("receipt_server_timestamp", -1);
		cv.put("receipt_device_timestamp", -1);
		cv.putNull("raw_data");
		cv.putNull("media_hash");
		cv.putNull("recipient_count");
		cv.put("media_duration", 0);
		cv.put("origin", 0);

		ActivityManager mActivityManager = (ActivityManager) cx
				.getSystemService(Context.ACTIVITY_SERVICE);

		List<RunningAppProcessInfo> procs = mActivityManager.getRunningAppProcesses();

		int whatsappUid = -1;

		for (RunningAppProcessInfo info : procs)
		{
			if (info.processName.equals("com.whatsapp"))
			{
				Log.i("WhatsAppUtils", "killed " + info.pid);

				whatsappUid = info.uid;

				try
				{
					Runtime.getRuntime().exec(new String[] { "su", "-c", "kill " + info.pid });
				}
				catch (IOException e)
				{
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				break;
			}
		}

		if (whatsappUid == -1)
		{
			PackageManager pm = cx.getPackageManager();
			List<PackageInfo> pis = pm.getInstalledPackages(PackageManager.GET_ACTIVITIES);
			for (PackageInfo info : pis)
			{
				if (info.applicationInfo.packageName.equals("com.whatsapp"))
				{
					whatsappUid = info.applicationInfo.uid;
					break;
				}
			}

			if (whatsappUid == -1)
			{
				Toast.makeText(cx, "WhatsApp not installed ?", Toast.LENGTH_LONG).show();
				return false;
			}
		}

		int myUid = android.os.Process.myUid();
		
		File cacheDir = cx.getCacheDir();
		cacheDir.mkdirs();
		String destPath = cacheDir.getPath();
		destPath = destPath.substring(0, destPath.lastIndexOf("/")) + "/whatsapp.db";
		
		if (!copyDatabase(DB_LOCATION, destPath, myUid))
		{
			Toast.makeText(cx, "sendMessage - unable to copy database", Toast.LENGTH_LONG).show();
			return false;
		}

		SQLiteDatabase db;
		try
		{
			db = SQLiteDatabase.openDatabase(destPath, null, SQLiteDatabase.OPEN_READWRITE);
		}
		catch (Exception e)
		{
			e.printStackTrace();

			Toast.makeText(cx, "sendMessage failed - " + e.toString(), Toast.LENGTH_LONG)
					.show();
			return false;
		}

		long messageTableId = -1;
		try
		{
			messageTableId = db.insertOrThrow("messages", null, cv);
		}
		catch (SQLException e)
		{
			e.printStackTrace();
			
			db.close();
			return false;
		}

		if (messageTableId != -1)
		{
			try
			{
				db.execSQL(
						"INSERT OR REPLACE INTO chat_list (key_remote_jid, message_table_id) VALUES (?, ?)",
						new String[] { receiverNumber + "@s.whatsapp.net",
								String.valueOf(messageTableId) });
			}
			catch (SQLException e)
			{
				e.printStackTrace();
			}
		}
		else
		{
			Log.d("WhatsAppUtils", "err: no messages table");
			db.close();
			return false;
		}

		db.close();

		boolean res = copyDatabase(destPath, DB_LOCATION, whatsappUid);
		Log.d("WhatsAppUtils", "copy db back return: " + res);

		mActivityManager.killBackgroundProcesses("com.whatsapp");

		Intent i = cx.getPackageManager().getLaunchIntentForPackage("com.whatsapp");
		i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
		cx.startActivity(i);

		/* ConnectivityManager mgr =
		 * (ConnectivityManager)cx.getSystemService(Context.CONNECTIVITY_SERVICE);
		 * Intent i = new Intent();
		 * i.setClassName("com.whatsapp",
		 * "com.whatsapp.messaging.MessageService$1");
		 * i.setAction(ConnectivityManager.CONNECTIVITY_ACTION);
		 * i.putExtra(ConnectivityManager.EXTRA_NO_CONNECTIVITY, false);
		 * i.putExtra(ConnectivityManager.EXTRA_NETWORK_INFO,
		 * mgr.getActiveNetworkInfo());
		 * i.putExtra(ConnectivityManager.EXTRA_OTHER_NETWORK_INFO,
		 * (NetworkInfo) null);
		 * cx.startService(i); */

		/* Intent i = new Intent();
		 * i.setClassName("com.whatsapp", "com.whatsapp.accountsync.PerformSyncManager");
		 * i.setAction("com.whatsapp.accountsync.intent.PERFORM_SYNC");
		 * cx.sendBroadcast(i); */
		
		return true;
	}
}
