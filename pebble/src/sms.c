#include <pebble.h>
#include "tertiary_text.h"
#include "alert.h"
#include "main.h"
#include "conversation.h"
#include "action_window.h"

Window *sms_window;
MenuLayer *sms_conv_menu_layer;

bool loading = false;
bool load_fail = false;

uint16_t sms_menu_get_num_sections_callback(MenuLayer *me, void *data)
{
	return 1;
}

uint16_t sms_menu_get_num_rows_callback(MenuLayer *me, uint16_t section_index, void *data)
{
	if (loading && num_conversations == 0)
	{
		return 1;
	}
	if (load_fail && num_conversations == 0)
	{
		return 1;
	}
	
	return num_conversations;
}

int16_t sms_menu_get_header_height_callback(MenuLayer *me, uint16_t section_index, void *data) {
	// This is a define provided in pebble_os.h that you may use for the default height
	return MENU_CELL_BASIC_HEADER_HEIGHT;
}

// Here we draw what each header is
void sms_menu_draw_header_callback(GContext* ctx, const Layer *cell_layer, uint16_t section_index, void *data) {
	// Draw title text in the section header
	char temp[32];
	if (!loading)
	{
		snprintf(temp, sizeof(temp), "Conversations (%d)", num_conversations);
		menu_cell_basic_header_draw(ctx, cell_layer, temp);
	}
	else
	{
		if (num_conversations == 0)
		{
			menu_cell_basic_header_draw(ctx, cell_layer, "Conversations");
		}
		else
		{
			snprintf(temp, sizeof(temp), "Conversations (%d) [...]", num_conversations);
			menu_cell_basic_header_draw(ctx, cell_layer, temp);
		}
	}
}

int16_t sms_menu_get_cell_height_callback(MenuLayer *me, MenuIndex *cell_index, void *data)
{
	return 25;
}

void sms_menu_draw_row_callback(GContext* ctx, const Layer *cell_layer, MenuIndex *cell_index, void *data)
{
	if (loading && num_conversations == 0)
	{
		menu_cell_basic_draw(ctx, cell_layer, "Loading...", " ", NULL);
		return;
	}
	if (load_fail && num_conversations == 0)
	{
		menu_cell_basic_draw(ctx, cell_layer, "Failed to retrieve", " ", NULL);
		return;
	}
	
	uint16_t row = cell_index->row;
	if (row >= num_conversations)
		return;
	
	menu_cell_basic_draw(ctx, cell_layer, conversations[row].full_name, " ", NULL);
}

void sms_send_message(conversation_t *conv, const char *message)
{
	DictionaryIterator *iter;
	app_message_outbox_begin(&iter);
	if (iter == NULL)
		return;
	
	dict_write_uint8(iter, KEY_ACTION, (uint8_t) ACTION_SMS_SEND_MESSAGE);
	dict_write_cstring(iter, KEY_NUMBER, conv->number);
	dict_write_cstring(iter, KEY_MESSAGE, message);
	dict_write_end(iter);
	
	// HACK
	//app_comm_set_sniff_interval(SNIFF_INTERVAL_REDUCED);
	//app_comm_set_sniff_interval(SNIFF_INTERVAL_NORMAL);
	
	app_message_outbox_send();
}

void sms_send_quick_response(conversation_t *conv, quick_response_t *response)
{
	DictionaryIterator *iter;
	app_message_outbox_begin(&iter);
	if (iter == NULL)
		return;
	
	dict_write_uint8(iter, KEY_ACTION, (uint8_t) ACTION_SMS_SEND_QUICK_RESPONSE);
	dict_write_cstring(iter, KEY_NUMBER, conv->number);
	dict_write_uint8(iter, KEY_QUICK_RESPONSE_ID, response->id);
	dict_write_end(iter);
	
	// HACK
	//app_comm_set_sniff_interval(SNIFF_INTERVAL_REDUCED);
	//app_comm_set_sniff_interval(SNIFF_INTERVAL_NORMAL);
	
	app_message_outbox_send();
}

void sms_action_done_callback(conversation_t *conv, quick_response_t *response, const char *message)
{
	if (response != NULL)
		sms_send_quick_response(conv, response);
	else if (message != NULL)
		sms_send_message(conv, message);
}

void sms_menu_select_callback(MenuLayer *me, MenuIndex *cell_index, void *data)
{
	if (loading)
		return;
	
	action_window_open(&conversations[cell_index->row], sms_action_done_callback);
}

void sms_request_conversations()
{
	DictionaryIterator *iter;
	app_message_outbox_begin(&iter);
	if (iter == NULL)
		return;
	
	dict_write_uint8(iter, KEY_ACTION, (uint8_t) ACTION_SMS_REQUEST_CONVERSATIONS);
	dict_write_uint16(iter, KEY_VERSION_CODE, (uint16_t)(VERSION_CODE));
	dict_write_end(iter);
	
	// HACK
	//app_comm_set_sniff_interval(SNIFF_INTERVAL_REDUCED);
	//app_comm_set_sniff_interval(SNIFF_INTERVAL_NORMAL);
	
	app_message_outbox_send();
	
	loading = true;
	num_conversations = 0;
	current_request_id = 0xFF;
}

void sms_handle_conversations(DictionaryIterator *iter)
{
	if (num_conversations >= MAX_CONVERSATIONS)
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
	
	load_fail = false;
	
	Tuple *number_tuple = dict_find(iter, KEY_NUMBER);
	if (!number_tuple)
		return;
	
	Tuple *name_tuple = dict_find(iter, KEY_NAME);
	
	Tuple *last_conv_tuple = dict_find(iter, KEY_LAST_CONVERSATION);
	if (last_conv_tuple)
	{
		loading = false;
		//current_request_id = 0xFF;
	}
	
	memcpy(conversations[num_conversations].number, number_tuple->value->cstring, number_tuple->length);
	conversations[num_conversations].number[number_tuple->length] = 0;
	
	memcpy(conversations[num_conversations].full_name, name_tuple->value->cstring, name_tuple->length);
	conversations[num_conversations].full_name[name_tuple->length] = 0;
	
	++num_conversations;
	
	menu_layer_reload_data(sms_conv_menu_layer);
}

void sms_out_sent_handler(DictionaryIterator *sent, void *context)
{
}

void sms_out_fail_handler(DictionaryIterator *failed, AppMessageResult reason, void *context)
{
	if (loading)
	{
		loading = false;
		load_fail = true;
		
		menu_layer_reload_data(sms_conv_menu_layer);
	}
	else
	{
		//show_alert("Failed to send stuff to phone");
	}
}

/*void sms_in_received_handler(DictionaryIterator *iter, void *context)
{
	// HACK
	app_comm_set_sniff_interval(SNIFF_INTERVAL_REDUCED);
	app_comm_set_sniff_interval(SNIFF_INTERVAL_NORMAL);
	
	Tuple *action_tuple = dict_find(iter, KEY_ACTION);
	if (!action_tuple)
		return;
	
	uint8_t action = action_tuple->value->uint8;
	switch (action) {
		case ACTION_SMS_REQUEST_CONVERSATIONS:
			sms_handle_conversations(iter);
			break;
			
		case ACTION_REQUEST_QUICK_RESPONSES:
			action_window_handle_quick_responses(iter);
			break;
	}
}*/

void sms_in_drop_handler(void *context, AppMessageResult reason)
{
	//show_alert("Failed to receive stuff from phone");
}

void sms_window_load(Window *me)
{
	GRect bounds = layer_get_bounds(window_get_root_layer(me));
	
	sms_conv_menu_layer = menu_layer_create(bounds);
	menu_layer_set_callbacks(sms_conv_menu_layer, NULL, (MenuLayerCallbacks) {
		.get_num_sections = sms_menu_get_num_sections_callback,
		.get_num_rows = sms_menu_get_num_rows_callback,
		.get_header_height = sms_menu_get_header_height_callback,
		.draw_header = sms_menu_draw_header_callback,
		.draw_row = sms_menu_draw_row_callback,
		.select_click = sms_menu_select_callback,
		.get_cell_height = sms_menu_get_cell_height_callback
	});
	
	// Bind the menu layer's click config provider to the window for interactivity
	menu_layer_set_click_config_onto_window(sms_conv_menu_layer, me);
	
	// Add it to the window for display
	layer_add_child(window_get_root_layer(me), menu_layer_get_layer(sms_conv_menu_layer));
}

void sms_window_unload(Window *me)
{
	menu_layer_destroy(sms_conv_menu_layer);
}

void sms_open()
{
	sms_window = window_create();
	
	window_set_window_handlers(sms_window, (WindowHandlers) {
		.load = sms_window_load,
		.unload = sms_window_unload
	});
	
	window_stack_push(sms_window, true /* Animated */);
	
	sms_request_conversations();
}
