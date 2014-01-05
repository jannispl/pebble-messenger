package com.mavedev.PebbleMessenger;

import java.util.List;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import com.getpebble.android.kit.PebbleKit;
import com.getpebble.android.kit.util.PebbleDictionary;
import com.mavedev.PebbleMessenger.R;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;

public class MessengerService extends Service
{
	public static class MessageManager implements Runnable
	{
		public Handler messageHandler;
		private final BlockingQueue<PebbleDictionary> messageQueue = new LinkedBlockingQueue<PebbleDictionary>();
		private Boolean isMessagePending = Boolean.valueOf(false);
		private Handler remoteHandler;

		@Override
		public void run()
		{
			Looper.prepare();
			messageHandler = new Handler()
			{
				@Override
				public void handleMessage(Message msg)
				{
					Log.w(this.getClass().getSimpleName(),
							"Please post() your blocking runnables to Mr Manager, "
									+ "don't use sendMessage()");
				}

			};
			remoteHandler.dispatchMessage(new Message());
			Looper.loop();
		}

		private void consumeAsync(final Context cx)
		{
			messageHandler.post(new Runnable()
			{
				@Override
				public void run()
				{
					synchronized (isMessagePending)
					{
						if (isMessagePending.booleanValue())
						{
							return;
						}

						synchronized (messageQueue)
						{
							if (messageQueue.size() == 0)
							{
								return;
							}
							PebbleKit.sendDataToPebble(cx, PebbleProtocol.WHATSAPP_UUID,
									messageQueue.peek());
						}

						isMessagePending = Boolean.valueOf(true);
					}
				}
			});
		}

		public void notifyAckReceivedAsync(final Context cx)
		{
			messageHandler.post(new Runnable()
			{
				@Override
				public void run()
				{
					synchronized (isMessagePending)
					{
						isMessagePending = Boolean.valueOf(false);
					}

					try
					{
						messageQueue.remove();
					}
					catch (NoSuchElementException ex)
					{
						Log.e("MessageManager", "NoSuchElementException!");
						ex.printStackTrace();
					}
				}
			});
			consumeAsync(cx);
		}

		public void notifyNackReceivedAsync(final Context cx)
		{
			messageHandler.post(new Runnable()
			{
				@Override
				public void run()
				{
					synchronized (isMessagePending)
					{
						isMessagePending = Boolean.valueOf(false);
					}
				}
			});
			consumeAsync(cx);
		}

		public boolean offer(final Context cx, final PebbleDictionary data)
		{
			final boolean success = messageQueue.offer(data);

			if (success)
			{
				consumeAsync(cx);
			}

			return success;
		}
	}

	private static final MessageManager messageManager = new MessageManager();
	
	private Random rand = new Random();

	@Override
	public IBinder onBind(Intent arg0)
	{
		return null;
	}

	@Override
	public void onCreate()
	{
		super.onCreate();

	}

	@Override
	public int onStartCommand(final Intent intent, int flags, int startId)
	{
		super.onStartCommand(intent, flags, startId);

		messageManager.remoteHandler = new Handler()
		{
			public void handleMessage(Message msg)
			{
				if (intent != null)
				{
					Bundle extras = intent.getExtras();

					if (extras.containsKey("acknack"))
					{
						int acknack = extras.getInt("acknack");
						if (acknack == 1) // ack
						{
							messageManager.notifyAckReceivedAsync(MessengerService.this);
						}
						else if (acknack == 2) // nack
						{
							messageManager.notifyNackReceivedAsync(MessengerService.this);
						}
					}
					else
					{
						int action = extras.getInt("action");
						// int transactionId = extras.getInt("transactionId");

						switch (action)
						{
						case PebbleProtocol.ACTION_WHATSAPP_REQUEST_CONVERSATIONS:
						case PebbleProtocol.ACTION_SMS_REQUEST_CONVERSATIONS:
							if (extras.containsKey("versionCode"))
							{
								SharedPreferences pref = getSharedPreferences(
										UpdateHelper.UPDATE_PREFERENCES, MODE_PRIVATE);

								pref.edit().putInt("watchappVersion", extras.getInt("versionCode"))
										.commit();

								UpdateHelper.maybeAutoUpdate(MessengerService.this);
							}

							if (action == PebbleProtocol.ACTION_WHATSAPP_REQUEST_CONVERSATIONS)
							{
								handleRequestWhatsAppConversations();
							}
							else
							{
								handleRequestSMSConversations();
							}
							break;
						case PebbleProtocol.ACTION_WHATSAPP_SEND_MESSAGE:
							handleSendMessage(extras.getString("number"),
									extras.getString("message"));
							break;
						case PebbleProtocol.ACTION_REQUEST_QUICK_RESPONSES:
							handleRequestQuickResponses();
							break;
						case PebbleProtocol.ACTION_WHATSAPP_SEND_QUICK_RESPONSE:
							handleSendQuickResponse(extras.getString("number"), extras.getInt("responseId"));
							break;
						case PebbleProtocol.ACTION_SMS_SEND_MESSAGE:
							handleSendSMSMessage(extras.getString("number"),
									extras.getString("message"));
							break;
						case PebbleProtocol.ACTION_SMS_SEND_QUICK_RESPONSE:
							handleSendSMSQuickResponse(extras.getString("number"), extras.getInt("responseId"));
							break;
						}
					}
				}
			}
		};

		new Thread(messageManager).start();

		return START_STICKY;
	}

	private void handleRequestWhatsAppConversations()
	{
		Log.d("MessengerService", "handleRequestConversations");

		List<WhatsAppUtils.Conversation> conversations = WhatsAppUtils
				.getConversations(this);
		int totalConversations = conversations.size() > 10 ? 10 : conversations.size();

		byte requestId = (byte) rand.nextInt(0xFF);
		
		int i = 0;
		for (WhatsAppUtils.Conversation conv : conversations)
		{
			if (i >= totalConversations)
				break;
			
			String number = conv.jid.substring(0, conv.jid.indexOf('@'));
			String name = conv.fullName;
			
			// <TEMP - DISABLE GROUPS>
			if (number.indexOf('-') != -1)
			{
				continue;
			}
			// </TEMP - DISABLE GROUPS>

			PebbleDictionary data = new PebbleDictionary();
			data.addUint8(PebbleProtocol.KEY_ACTION,
					(byte) PebbleProtocol.ACTION_WHATSAPP_REQUEST_CONVERSATIONS);
			data.addUint8(PebbleProtocol.KEY_REQUEST_ID, requestId);
			if (i == totalConversations - 1)
			{
				data.addUint8(PebbleProtocol.KEY_LAST_CONVERSATION, (byte) 1);
			}
			data.addString(PebbleProtocol.KEY_NUMBER, number);
			data.addString(PebbleProtocol.KEY_NAME, name);
			messageManager.offer(this, data);
			
			++i;
		}
	}

	private void handleRequestSMSConversations()
	{
		Log.d("MessengerService", "handleRequestSMSConversations");

		List<SMSUtils.Conversation> conversations = SMSUtils
				.getConversations(this);
		int totalConversations = conversations.size() > 10 ? 10 : conversations.size();

		byte requestId = (byte) rand.nextInt(0xFF);
		
		int i = 0;
		for (SMSUtils.Conversation conv : conversations)
		{
			if (i >= totalConversations)
				break;
			
			String number = conv.number;
			String name = conv.fullName;

			PebbleDictionary data = new PebbleDictionary();
			data.addUint8(PebbleProtocol.KEY_ACTION,
					(byte) PebbleProtocol.ACTION_SMS_REQUEST_CONVERSATIONS);
			data.addUint8(PebbleProtocol.KEY_REQUEST_ID, requestId);
			if (i == totalConversations - 1)
			{
				data.addUint8(PebbleProtocol.KEY_LAST_CONVERSATION, (byte) 1);
			}
			data.addString(PebbleProtocol.KEY_NUMBER, number);
			data.addString(PebbleProtocol.KEY_NAME, name);
			messageManager.offer(this, data);
			
			++i;
		}
	}
	
	private void sendMessageInternal(String number, String message)
	{
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		if (prefs.getBoolean("appendSignature", true))
		{
			message += "\n\n"
					+ prefs.getString("signature", getResources()
							.getString(R.string.message_append));
		}

		WhatsAppUtils.sendMessage(this, number, message);
	}

	private void handleSendMessage(String number, String message)
	{
		Log.d("MessengerService", "number: '" + number + "', message: '" + message + "'");
		sendMessageInternal(number, message);
	}

	private void handleRequestQuickResponses()
	{
		Log.i("MessengerService", "handleRequestQuickResponses");

		byte requestId = (byte) rand.nextInt(0xFF);
		
		SharedPreferences pref = getSharedPreferences(QuickResponsesManager.PREF_FILE_NAME,
				MODE_PRIVATE);
		int numQuickResponses = pref.getInt("numQuickResponses", 0);
		if (numQuickResponses == 0)
		{
			PebbleDictionary data = new PebbleDictionary();
			data.addUint8(PebbleProtocol.KEY_ACTION,
					(byte) PebbleProtocol.ACTION_REQUEST_QUICK_RESPONSES);
			data.addUint8(PebbleProtocol.KEY_REQUEST_ID, requestId);
			data.addUint8(PebbleProtocol.KEY_QUICK_RESPONSE_ID, (byte) 0xFF);
			data.addUint8(PebbleProtocol.KEY_LAST_CONVERSATION, (byte) 1);
			messageManager.offer(this, data);
		}
		else
		{
			for (int i = 0; i < numQuickResponses; ++i)
			{
				String response = pref.getString("quickResponse" + i, "N/A");

				PebbleDictionary data = new PebbleDictionary();
				data.addUint8(PebbleProtocol.KEY_ACTION,
						(byte) PebbleProtocol.ACTION_REQUEST_QUICK_RESPONSES);
				data.addUint8(PebbleProtocol.KEY_REQUEST_ID, requestId);
				data.addUint8(PebbleProtocol.KEY_QUICK_RESPONSE_ID, (byte) i);
				data.addString(PebbleProtocol.KEY_MESSAGE, response);
				if (i == numQuickResponses - 1)
					data.addUint8(PebbleProtocol.KEY_LAST_CONVERSATION, (byte) 1);
				messageManager.offer(this, data);
			}
		}
	}

	private void handleSendQuickResponse(String number, int responseId)
	{
		SharedPreferences pref = getSharedPreferences(QuickResponsesManager.PREF_FILE_NAME,
				MODE_PRIVATE);
		if (pref.contains("quickResponse" + responseId))
		{
			String message = pref.getString("quickResponse" + responseId, "");
			sendMessageInternal(number, message);
		}
		else
		{
			Log.e("MessengerService", "received invalid response id " + responseId);
		}
	}

	private void sendSMSMessageInternal(String number, String message)
	{
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		if (prefs.getBoolean("appendSignature", true))
		{
			message += "\n\n"
					+ prefs.getString("signature", getResources()
							.getString(R.string.message_append));
		}

		SMSUtils.sendMessage(this, number, message);
	}
	private void handleSendSMSMessage(String number, String message)
	{
		Log.d("MessengerService", "SMS number: '" + number + "', message: '" + message + "'");
		sendSMSMessageInternal(number, message);
	}
	
	private void handleSendSMSQuickResponse(String number, int responseId)
	{
		Log.d("MessengerService", "SMS number: '" + number + "', response id: '" + responseId + "'");
		SharedPreferences pref = getSharedPreferences(QuickResponsesManager.PREF_FILE_NAME,
				MODE_PRIVATE);
		if (pref.contains("quickResponse" + responseId))
		{
			String message = pref.getString("quickResponse" + responseId, "");
			sendSMSMessageInternal(number, message);
		}
		else
		{
			Log.e("MessengerService", "received invalid response id " + responseId);
		}
	}
}
