//
//  action_window.h
//  pebble_whatsapp
//
//  Created by Jannis Pohl on 21.10.13.
//
//

#ifndef pebble_whatsapp_action_window_h
#define pebble_whatsapp_action_window_h

#define MAX_QUICK_RESPONSES 10
typedef struct {
	uint8_t id;
	char response[128];
} quick_response_t;
extern quick_response_t quick_responses[MAX_QUICK_RESPONSES];
extern uint8_t num_quick_responses;

typedef void (*ActionDoneHandler)(conversation_t *conv, quick_response_t *response, const char *message);

void action_window_open(conversation_t *conv, ActionDoneHandler);

void action_window_handle_quick_responses(DictionaryIterator *iter);

#endif
