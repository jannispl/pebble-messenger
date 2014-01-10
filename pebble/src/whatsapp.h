//
//  whatsapp.h
//  pebble_whatsapp
//
//  Created by Jannis Pohl on 21.10.13.
//
//

#ifndef pebble_whatsapp_whatsapp_h
#define pebble_whatsapp_whatsapp_h

void whatsapp_open();

void whatsapp_out_sent_handler(DictionaryIterator *sent, void *context);
void whatsapp_out_fail_handler(DictionaryIterator *failed, AppMessageResult reason, void *context);
//void whatsapp_in_received_handler(DictionaryIterator *iter, void *context);
void whatsapp_in_drop_handler(void *context, AppMessageResult reason);

void whatsapp_handle_conversations(DictionaryIterator *iter);

#endif
