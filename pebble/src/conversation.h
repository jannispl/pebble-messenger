//
//  conversation.h
//  pebble_whatsapp
//
//  Created by Jannis Pohl on 21.10.13.
//
//

#ifndef pebble_whatsapp_conversation_h
#define pebble_whatsapp_conversation_h

#define MAX_CONVERSATIONS 10
typedef struct {
	char full_name[64];
	char number[16];
} conversation_t;

extern conversation_t conversations[MAX_CONVERSATIONS];
extern int16_t num_conversations;
extern conversation_t *current_conversation;

#endif
