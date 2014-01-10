//
//  conversation.c
//  pebble_whatsapp
//
//  Created by Jannis Pohl on 21.10.13.
//
//

#include "main.h"
#include "conversation.h"

conversation_t conversations[MAX_CONVERSATIONS];
int16_t num_conversations = 0;

conversation_t *current_conversation = NULL;

