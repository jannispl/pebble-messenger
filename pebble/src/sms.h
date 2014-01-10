//
//  sms.h
//  pebble_whatsapp
//
//  Created by Jannis Pohl on 21.10.13.
//
//

#ifndef pebble_whatsapp_sms_h
#define pebble_whatsapp_sms_h

void sms_open();

void sms_out_sent_handler(DictionaryIterator *sent, void *context);
void sms_out_fail_handler(DictionaryIterator *failed, AppMessageResult reason, void *context);
//void sms_in_received_handler(DictionaryIterator *iter, void *context);
void sms_in_drop_handler(void *context, AppMessageResult reason);

void sms_handle_conversations(DictionaryIterator *iter);

#endif
