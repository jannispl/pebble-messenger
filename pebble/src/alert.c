#include <pebble.h>
#include "alert.h"

Window *alert_window;
TextLayer *alert_text;
const char *alert_str;

void alert_window_load(Window *me)
{
	alert_text = text_layer_create(GRect(2, 0, 140, 150));
	text_layer_set_text_alignment(alert_text, GTextAlignmentCenter);
	text_layer_set_font(alert_text, fonts_get_system_font(FONT_KEY_GOTHIC_24_BOLD));
	text_layer_set_text(alert_text, alert_str);
	layer_add_child(window_get_root_layer(me), text_layer_get_layer(alert_text));
}

void alert_click_handler(ClickRecognizerRef recognizer, void *context)
{
	window_stack_remove(alert_window, true);
}

void alert_click_config_provider(void *context)
{
	window_single_click_subscribe(BUTTON_ID_SELECT, alert_click_handler);
}

void show_alert(const char *text)
{
	alert_str = text;
	alert_window = window_create();
	window_set_window_handlers(alert_window, (WindowHandlers) {
		.load = alert_window_load
	});
	window_set_click_config_provider(alert_window, (ClickConfigProvider) alert_click_config_provider);
	
	window_stack_push(alert_window, true);
	
	vibes_short_pulse();
}
