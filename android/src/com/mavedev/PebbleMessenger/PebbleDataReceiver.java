package com.mavedev.PebbleMessenger;

import static com.getpebble.android.kit.Constants.APP_UUID;
import static com.getpebble.android.kit.Constants.MSG_DATA;
import static com.getpebble.android.kit.Constants.TRANSACTION_ID;

import java.util.UUID;

import org.json.JSONException;

import com.getpebble.android.kit.Constants;
import com.getpebble.android.kit.PebbleKit;
import com.getpebble.android.kit.util.PebbleDictionary;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;

public class PebbleDataReceiver extends BroadcastReceiver
{
	@SuppressLint("Wakelock")
	@Override
	public void onReceive(Context context, Intent intent)
	{
		PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
		WakeLock wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
				PebbleDataReceiver.class.getSimpleName());
		wakeLock.acquire(10 * 1000);

		if (intent.getAction().equals(Constants.INTENT_APP_RECEIVE))
		{
			final UUID receivedUuid = (UUID) intent.getSerializableExtra(APP_UUID);

			// Pebble-enabled apps are expected to be good citizens and only
			// inspect broadcasts containing their UUID
			if (!PebbleProtocol.WHATSAPP_UUID.equals(receivedUuid))
			{
				wakeLock.release();
				return;
			}

			final int transactionId = intent.getIntExtra(TRANSACTION_ID, -1);
			final String jsonData = intent.getStringExtra(MSG_DATA);
			if (jsonData == null || jsonData.length() == 0)
			{
				wakeLock.release();
				return;
			}

			try
			{
				final PebbleDictionary data = PebbleDictionary.fromJson(jsonData);
				receiveData(context, transactionId, data);
			}
			catch (JSONException e)
			{
				e.printStackTrace();
				wakeLock.release();
				return;
			}
		}
		else if (intent.getAction().equals(Constants.INTENT_APP_RECEIVE_ACK))
		{
			Intent i = new Intent(context, MessengerService.class);
			i.putExtra("acknack", 1);
			context.startService(i);
		}
		else if (intent.getAction().equals(Constants.INTENT_APP_RECEIVE_NACK))
		{
			Intent i = new Intent(context, MessengerService.class);
			i.putExtra("acknack", 2);
			context.startService(i);
		}

		wakeLock.release();
	}

	private void receiveData(Context context, int transactionId, PebbleDictionary data)
	{
		if (!data.contains(PebbleProtocol.KEY_ACTION))
			return;

		PebbleKit.sendAckToPebble(context, transactionId);

		final int cmd = data.getUnsignedInteger(PebbleProtocol.KEY_ACTION).intValue();

		Intent i = new Intent(context, MessengerService.class);

		i.putExtra("action", cmd);
		i.putExtra("transactionId", transactionId);

		switch (cmd)
		{
		case PebbleProtocol.ACTION_WHATSAPP_REQUEST_CONVERSATIONS:
		case PebbleProtocol.ACTION_SMS_REQUEST_CONVERSATIONS:
			if (data.contains(PebbleProtocol.KEY_VERSION_CODE))
			{
				i.putExtra("versionCode", data.getUnsignedInteger(PebbleProtocol.KEY_VERSION_CODE)
						.intValue());
			}
			break;

		case PebbleProtocol.ACTION_WHATSAPP_SEND_MESSAGE:
		case PebbleProtocol.ACTION_SMS_SEND_MESSAGE:
			i.putExtra("number", data.getString(PebbleProtocol.KEY_NUMBER));
			i.putExtra("message", data.getString(PebbleProtocol.KEY_MESSAGE));
			break;
			
		case PebbleProtocol.ACTION_REQUEST_QUICK_RESPONSES:
			break;
			
		case PebbleProtocol.ACTION_WHATSAPP_SEND_QUICK_RESPONSE:
		case PebbleProtocol.ACTION_SMS_SEND_QUICK_RESPONSE:
			i.putExtra("number", data.getString(PebbleProtocol.KEY_NUMBER));
			i.putExtra("responseId", data.getUnsignedInteger(PebbleProtocol.KEY_QUICK_RESPONSE_ID)
					.intValue());
			break;
		}

		context.startService(i);
	}
}
