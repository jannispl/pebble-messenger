package com.mavedev.PebbleMessenger;

import java.util.UUID;

public class PebbleProtocol
{
	public static final UUID WHATSAPP_UUID = UUID
			.fromString("73f27a7a-cbb3-4d31-b460-7d37fd099b35");

	public static final int KEY_ACTION = 0x01;
	//public static final int KEY_NUM_CONVERSATIONS = 0x02;
	public static final int KEY_NUMBER = 0x03;
	public static final int KEY_MESSAGE = 0x04;
	public static final int KEY_LAST_CONVERSATION = 0x05;
	public static final int KEY_QUICK_RESPONSE_ID = 0x06;
	public static final int KEY_VERSION_CODE = 0x07;
	public static final int KEY_NAME = 0x08;
	public static final int KEY_REQUEST_ID = 0x09;
	public static final int KEY_SUCCESS = 0x0A;

	//public static final int KEY_CONVERSATION = 0x7F;

	public static final int ACTION_WHATSAPP_REQUEST_CONVERSATIONS = 0x01;
	public static final int ACTION_WHATSAPP_SEND_MESSAGE = 0x02;
	public static final int ACTION_REQUEST_QUICK_RESPONSES = 0x03;
	public static final int ACTION_WHATSAPP_SEND_QUICK_RESPONSE = 0x04;
	public static final int ACTION_SMS_SEND_MESSAGE = 0x05;
	public static final int ACTION_SMS_SEND_QUICK_RESPONSE = 0x06;
	public static final int ACTION_SMS_REQUEST_CONVERSATIONS = 0x07;
	public static final int ACTION_SENT_CONFIRMATION = 0x08;
}
