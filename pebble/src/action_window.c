//
//  action_window.c
//  pebble_whatsapp
//
//  Created by Jannis Pohl on 21.10.13.
//
//

#include "main.h"
#include "conversation.h"
#include "alert.h"
#include "tertiary_text.h"
#include "action_window.h"

Window *action_window;
MenuLayer *action_menu_layer;

quick_response_t quick_responses[MAX_QUICK_RESPONSES];
uint8_t num_quick_responses = 0;

ActionDoneHandler action_done_handler;

int16_t action_menu_get_header_height_callback(MenuLayer *me, uint16_t section_index, void *data) {
	// This is a define provided in pebble_os.h that you may use for the default height
	return MENU_CELL_BASIC_HEADER_HEIGHT;
}

uint16_t action_menu_get_num_sections_callback(MenuLayer *me, void *data)
{
	return 2;
}

uint16_t action_menu_get_num_rows_callback(MenuLayer *me, uint16_t section_index, void *data) {
	if (section_index == 0)
	{
		return 1;
	}
	
	if (num_quick_responses == 0)
	{
		return 1;
	}
	
	return num_quick_responses;
}

int16_t action_menu_get_cell_height_callback(MenuLayer *me, MenuIndex *cell_index, void *data)
{
	if (cell_index->section == 0)
	{
		return 52;
	}
	
	return 42;
}

// Here we draw what each header is
void action_menu_draw_header_callback(GContext* ctx, const Layer *cell_layer, uint16_t section_index, void *data) {
	// Draw title text in the section header
	if (section_index == 0)
	{
		menu_cell_basic_header_draw(ctx, cell_layer, "Actions");
	}
	else
	{
		menu_cell_basic_header_draw(ctx, cell_layer, "Quick responses");
	}
}

void action_menu_draw_row_callback(GContext* ctx, const Layer *cell_layer, MenuIndex *cell_index, void *data)
{
	uint16_t row = cell_index->row;
	
	if (cell_index->section == 0)
	{
		menu_cell_basic_draw(ctx, cell_layer, "Type...", "Hold SELECT to send", NULL);
	}
	else
	{
		if (num_quick_responses == 0)
		{
			menu_cell_basic_draw(ctx, cell_layer, "Loading...", " ", NULL);
		}
		else
		{
			menu_cell_basic_draw(ctx, cell_layer, quick_responses[row].response, " ", NULL);
		}
	}
}

void tertiary_done_handler(const char *text)
{
	if (text == NULL || text[0] == 0)
		return;
	
	action_done_handler(current_conversation, NULL, text);
}

void request_quick_responses()
{
	Tuplet value = TupletInteger(KEY_ACTION, (uint8_t) ACTION_REQUEST_QUICK_RESPONSES);
	
	DictionaryIterator *iter;
	app_message_outbox_begin(&iter);
	if (iter == NULL)
		return;
	
	dict_write_tuplet(iter, &value);
	dict_write_end(iter);
	
	// HACK
	app_comm_set_sniff_interval(SNIFF_INTERVAL_REDUCED);
	app_comm_set_sniff_interval(SNIFF_INTERVAL_NORMAL);
	
	app_message_outbox_send();
	
	num_quick_responses = 0;
	current_request_id = 0xFF;
}

void action_window_handle_quick_responses(DictionaryIterator *iter)
{
	if (num_quick_responses >= MAX_QUICK_RESPONSES)
		return;
	
	Tuple *request_id_tuple = dict_find(iter, KEY_REQUEST_ID);
	if (!request_id_tuple)
		return;
	uint8_t request_id = request_id_tuple->value->uint8;
	
	if (current_request_id == 0xFF)
		current_request_id = request_id;
	else
	{
		if (current_request_id != request_id)
			return;
	}
	
	Tuple *quick_response_id_tuple = dict_find(iter, KEY_QUICK_RESPONSE_ID);
	if (!quick_response_id_tuple)
		return;
	uint8_t quick_response_id = quick_response_id_tuple->value->uint8;
	
	if (quick_response_id == 0xFF)
	{
		show_alert("No quick responses defined :(");
		return;
	}
	
	Tuple *last_conv_tuple = dict_find(iter, KEY_LAST_CONVERSATION);
	if (last_conv_tuple)
	{
		//current_request_id = 0xFF;
	}
	
	Tuple *response_tuple = dict_find(iter, KEY_MESSAGE);
	if (!response_tuple)
		return;
	
	quick_responses[num_quick_responses].id = quick_response_id;
	memcpy(quick_responses[num_quick_responses].response, response_tuple->value->cstring, response_tuple->length);
	++num_quick_responses;
	
	menu_layer_reload_data(action_menu_layer);
}

void action_menu_select_callback(MenuLayer *me, MenuIndex *cell_index, void *data)
{
	if (current_conversation == NULL)
		return;
	
	if (cell_index->section == 0)
	{
		// Type
		tertiary_open(tertiary_done_handler);
	}
	else if (num_quick_responses != 0)
	{
		//send_message(current_conversation, quick_responses[cell_index->row].response);
		//send_quick_response(current_conversation, &quick_responses[cell_index->row]);
		action_done_handler(current_conversation, &quick_responses[cell_index->row], NULL);
	}
	
	window_stack_remove(action_window, true);
}

void action_window_load(Window *me)
{
	GRect bounds = layer_get_bounds(window_get_root_layer(me));
	action_menu_layer = menu_layer_create(bounds);
	
	menu_layer_set_callbacks(action_menu_layer, NULL, (MenuLayerCallbacks) {
		.get_num_sections = action_menu_get_num_sections_callback,
		.get_num_rows = action_menu_get_num_rows_callback,
		.get_header_height = action_menu_get_header_height_callback,
		.draw_header = action_menu_draw_header_callback,
		.draw_row = action_menu_draw_row_callback,
		.select_click = action_menu_select_callback,
		.get_cell_height = action_menu_get_cell_height_callback
	});
	
	// Bind the menu layer's click config provider to the window for interactivity
	menu_layer_set_click_config_onto_window(action_menu_layer, me);
	
	// Add it to the window for display
	layer_add_child(window_get_root_layer(me), menu_layer_get_layer(action_menu_layer));
}

void action_window_open(conversation_t *conv, ActionDoneHandler handler)
{
	action_done_handler = handler;
	current_conversation = conv;
	
	action_window = window_create();
	window_set_window_handlers(action_window, (WindowHandlers) {
		.load = action_window_load
	});
	
	window_stack_push(action_window, true);
	
	if (num_quick_responses == 0)
	{
		request_quick_responses();
	}
}
