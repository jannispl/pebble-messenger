#include "main.h"
#include "whatsapp.h"
#include "sms.h"
#include "conversation.h"
#include "action_window.h"

Window *main_window;
MenuLayer *feature_menu_layer;

#define NUM_FEATURES 2
char *features[NUM_FEATURES] = {
	"WhatsApp",
	"SMS"
};

int current_mode = -1;

uint8_t current_request_id = 0xFF;

uint16_t menu_get_num_sections_callback(MenuLayer *me, void *data)
{
	return 1;
}

uint16_t menu_get_num_rows_callback(MenuLayer *me, uint16_t section_index, void *data)
{
	return NUM_FEATURES;
}

int16_t menu_get_header_height_callback(MenuLayer *me, uint16_t section_index, void *data) {
	// This is a define provided in pebble_os.h that you may use for the default height
	return MENU_CELL_BASIC_HEADER_HEIGHT;
}

// Here we draw what each header is
void menu_draw_header_callback(GContext* ctx, const Layer *cell_layer, uint16_t section_index, void *data) {
	menu_cell_basic_header_draw(ctx, cell_layer, "Features");
}

void menu_draw_row_callback(GContext* ctx, const Layer *cell_layer, MenuIndex *cell_index, void *data)
{
	menu_cell_title_draw(ctx, cell_layer, features[cell_index->row]);
}

void menu_select_callback(MenuLayer *menu_layer, MenuIndex *cell_index, void *data)
{
	switch (cell_index->row)
	{
		case 0:
			current_mode = 0;
			whatsapp_open();
			break;
			
		case 1:
			current_mode = 1;
			sms_open();
			break;
	}
}

void out_sent_handler(DictionaryIterator *sent, void *context)
{
	switch (current_mode)
	{
		case 0:
			whatsapp_out_sent_handler(sent, context);
			break;
			
		case 1:
			sms_out_sent_handler(sent, context);
			break;
	}
}

void out_fail_handler(DictionaryIterator *failed, AppMessageResult reason, void *context)
{
	switch (current_mode)
	{
		case 0:
			whatsapp_out_fail_handler(failed, reason, context);
			break;
			
		case 1:
			sms_out_fail_handler(failed, reason, context);
			break;
	}
}

void in_received_handler(DictionaryIterator *iter, void *context)
{
	// HACK
	//app_comm_set_sniff_interval(SNIFF_INTERVAL_REDUCED);
	//app_comm_set_sniff_interval(SNIFF_INTERVAL_NORMAL);
	
	Tuple *action_tuple = dict_find(iter, KEY_ACTION);
	if (!action_tuple)
		return;
	
	uint8_t action = action_tuple->value->uint8;
	switch (action) {
		case ACTION_WHATSAPP_REQUEST_CONVERSATIONS:
			whatsapp_handle_conversations(iter);
			break;
			
		case ACTION_SMS_REQUEST_CONVERSATIONS:
			sms_handle_conversations(iter);
			break;
			
		case ACTION_REQUEST_QUICK_RESPONSES:
			action_window_handle_quick_responses(iter);
			break;
	}
}

void in_drop_handler(AppMessageResult reason, void *context)
{
	switch (current_mode)
	{
		case 0:
			whatsapp_in_drop_handler(context, reason);
			break;
			
		case 1:
			sms_in_drop_handler(context, reason);
			break;
	}
}

void window_load(Window *me)
{
	GRect bounds = layer_get_bounds(window_get_root_layer(me));
	
	feature_menu_layer = menu_layer_create(bounds);
	menu_layer_set_callbacks(feature_menu_layer, NULL, (MenuLayerCallbacks) {
		.get_num_sections = menu_get_num_sections_callback,
		.get_num_rows = menu_get_num_rows_callback,
		.get_header_height = menu_get_header_height_callback,
		.draw_header = menu_draw_header_callback,
		.draw_row = menu_draw_row_callback,
		.select_click = menu_select_callback
	});
	
	// Bind the menu layer's click config provider to the window for interactivity
	menu_layer_set_click_config_onto_window(feature_menu_layer, me);
	
	// Add it to the window for display
	layer_add_child(window_get_root_layer(me), menu_layer_get_layer(feature_menu_layer));
}

void handle_init()
{
	app_message_open(124, 124);
	
	app_message_register_inbox_received(in_received_handler);
	app_message_register_inbox_dropped(in_drop_handler);
	app_message_register_outbox_failed(out_fail_handler);
	app_message_register_outbox_sent(out_sent_handler);
	
	main_window = window_create();
	
	window_set_window_handlers(main_window, (WindowHandlers) {
		.load = window_load
	});
	
	window_stack_push(main_window, true /* Animated */);
}

void handle_deinit()
{
	window_destroy(main_window);
}

int main()
{
	handle_init();
	app_event_loop();
	handle_deinit();
	
	/*PebbleAppHandlers handlers =
	{
		.init_handler = &handle_init,
		.messaging_info =
		{
			.buffer_sizes =
			{
				.inbound = 124,
				.outbound = 124
			},
			.default_callbacks.callbacks =
			{
				.out_sent = out_sent_handler,
				.out_failed = out_fail_handler,
				.in_received = in_received_handler,
				.in_dropped = in_drop_handler
			}
		}
	};
	app_event_loop(params, &handlers);*/
}
