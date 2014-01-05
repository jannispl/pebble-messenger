package com.mavedev.PebbleMessenger;

import java.util.ArrayList;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.telephony.SmsManager;

public class SMSUtils
{
	public static class Conversation
	{
		public String number;
		public String fullName;
	}

	public static List<Conversation> getConversations(Context cx)
	{
		ArrayList<Conversation> al = new ArrayList<Conversation>();

		Cursor c = cx.getContentResolver().query(Uri.parse("content://sms/conversations/"),
				new String[] { "thread_id" }, null, null, "date DESC");

		// int msgCountCol = c.getColumnIndexOrThrow("msg_count");
		int threadIdCol = c.getColumnIndexOrThrow("thread_id");

		c.moveToFirst();
		do
		{
			// int msgCount = c.getInt(msgCountCol);
			int threadId = c.getInt(threadIdCol);

			Cursor mycursor = cx.getContentResolver().query(Uri.parse("content://sms/inbox"),
					new String[] { "address" }, "thread_id=?",
					new String[] { String.valueOf(threadId) }, "date DESC");
			if (mycursor.moveToFirst())
			{
				String number = mycursor.getString(mycursor.getColumnIndexOrThrow("address"));

				Conversation conv = new Conversation();
				conv.number = number;
				conv.fullName = WhatsAppUtils.getContactDisplayNameByNumber(cx, number);

				al.add(conv);
			}
			mycursor.close();
		}
		while (c.moveToNext());
		c.close();

		return al;
	}

	public static void sendMessage(Context cx, String number, String message)
	{
		SmsManager mgr = SmsManager.getDefault();
		mgr.sendTextMessage(number, null, message, null, null);
		
		ContentValues values = new ContentValues();
		values.put("address", number);
		values.put("body", message);
		values.put("date", System.currentTimeMillis());
		cx.getContentResolver().insert(Uri.parse("content://sms/sent"), values);
	}
}
