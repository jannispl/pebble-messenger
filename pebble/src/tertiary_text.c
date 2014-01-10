#include <pebble.h>
#include "tertiary_text.h"

#define TOP 0
#define MID 1
#define BOT 2

Window *tertiary_window;

TextLayer *textLayer;
TextLayer *wordsYouWrite;

TextLayer *buttons1[3];
TextLayer *buttons2[3];
TextLayer *buttons3[3];
TextLayer** bbuttons[] = {buttons1, buttons2, buttons3};

bool menu = false;

// Here are the three cases, or sets
char caps[] =    "ABCDEFGHIJKLM NOPQRSTUVWXYZ";
char letters[] = "abcdefghijklm nopqrstuvwxyz";
char numsym[] = "1234567890!?-'\"$()&*+#:@/,.";

// the below three strings just have to be unique, abc - xyz will be overwritten with the long strings above
char* btext1[] = {"abc", "def", "ghi"};
char* btext2[] = {"jkl", "m n", "opq"};
char* btext3[] = {"rst", "uvw", "xyz"};
char** btexts[] = {btext1, btext2, btext3};

// These are the actual sets that are displayed on each button, also need to be unique
char set1[4] = "  a\0";
char set2[4] = "  b\0";
char set3[4] = "  c\0";
char* setlist[] = {set1, set2, set3};

char* cases[] = {"CAP", "low", "#@1"};

int cur_set = 1;
//bool blackout = false;

void drawSides();
void drawMenu();
void set_menu();

char* rotate_text[] = {caps, letters, numsym};
void next();

char* master = letters;

char text_buffer[60];
int pos = 0;
int top, end, size;

TertiaryDoneHandler done_handler;

// This function changes the next case/symbol set.
void change_set(int s, bool lock)
{
    int count = 0;
    master = rotate_text[s];
    for (int i=0; i<3; i++)
    {
        for (int j=0; j<3; j++)
        {
            for (int k=0; k<3; k++)
            {
                btexts[i][j][k] = master[count];
                count++;
            }
        }
    }
    
    menu = false;
    if (lock) cur_set = s;
    
    drawSides();
}

void next()
{
    top = 0;
    end = 26;
    size = 27;
}

// These are to prevent missed clicks on a hold
void up_long_release_handler(ClickRecognizerRef recognizer, void *context) {}
void select_long_release_handler(ClickRecognizerRef recognizer, void *context) {}
void down_long_release_handler(ClickRecognizerRef recognizer, void *context) {}

void clickButton(int b)
{
    //if (!blackout)
    {
        if (menu)
        {
            change_set(b, false);
            return;
        }
        
        if (size > 3)
        {
            size /= 3;
            if (b == TOP)
                end -= 2*size;
            else if (b == MID)
            {
                top += size;
                end -= size;
            }
            else if (b == BOT)
                top += 2*size;
        }
        else
        {
            text_buffer[pos++] = master[top+b];
            text_layer_set_text(wordsYouWrite, text_buffer);
            change_set(cur_set, false);
            next();
        }
		
        drawSides();
    }
    
}

// Modify these common button handlers
void up_single_click_handler(ClickRecognizerRef recognizer, void *context) {    
    clickButton(TOP);
	
}

void select_single_click_handler(ClickRecognizerRef recognizer, void *context) {
    clickButton(MID);
}

void down_single_click_handler(ClickRecognizerRef recognizer, void *context) {    
    clickButton(BOT);
}

bool common_long(int b)
{
    if (menu)
    {
        change_set(b, true);
        return true;
    }
    return false;
}

void up_long_click_handler(ClickRecognizerRef recognizer, void *context) {
    if (common_long(TOP)) return;
    
    set_menu();
    
}

void select_long_click_handler(ClickRecognizerRef recognizer, void *context) {
    if (common_long(MID)) return;
    
	window_stack_remove(tertiary_window, true);
	
	done_handler(text_buffer);
	
    /*blackout = !blackout;
	
    if (blackout)
        text_layer_set_background_color(&textLayer, GColorBlack);
    else
		text_layer_set_background_color(&textLayer, GColorClear);*/
}


void down_long_click_handler(ClickRecognizerRef recognizer, void *context) {    
    if (common_long(BOT)) return;
    
    // delete or cancel when back is held
    
    if (size==27 && pos>0 /*&& !blackout*/)
    {
        text_buffer[--pos] = 0;
        text_layer_set_text(wordsYouWrite, text_buffer);
    }
    else
    {
        next();
        drawSides();
    }
	
}

void set_menu()
{
    //if (!blackout)
    {
        menu = true;
        drawMenu();
    }
}

// This usually won't need to be modified

void click_config_provider(void *context) {
	window_single_repeating_click_subscribe(BUTTON_ID_SELECT, 100, select_single_click_handler);
	window_single_repeating_click_subscribe(BUTTON_ID_UP, 100, up_single_click_handler);
	window_single_repeating_click_subscribe(BUTTON_ID_DOWN, 100, down_single_click_handler);
	
	window_long_click_subscribe(BUTTON_ID_SELECT, 800, select_long_click_handler, select_long_release_handler);
	window_long_click_subscribe(BUTTON_ID_UP, 800, up_long_click_handler, up_long_release_handler);
	window_long_click_subscribe(BUTTON_ID_DOWN, 800, down_long_click_handler, down_long_release_handler);
	
	/*
	 config[BUTTON_ID_UP]->click.handler = (ClickHandler) up_single_click_handler;
	 config[BUTTON_ID_UP]->click.repeat_interval_ms = 100;
	 config[BUTTON_ID_UP]->long_click.handler = (ClickHandler) up_long_click_handler;
	 config[BUTTON_ID_UP]->long_click.release_handler = (ClickHandler) up_long_release_handler;
	 
	 config[BUTTON_ID_SELECT]->click.handler = (ClickHandler) select_single_click_handler;
	 config[BUTTON_ID_SELECT]->click.repeat_interval_ms = 100;
	 config[BUTTON_ID_SELECT]->long_click.handler = (ClickHandler) select_long_click_handler;
	 config[BUTTON_ID_SELECT]->long_click.release_handler = (ClickHandler) select_long_release_handler;
	 
	 config[BUTTON_ID_DOWN]->click.handler = (ClickHandler) down_single_click_handler;
	 config[BUTTON_ID_DOWN]->click.repeat_interval_ms = 100;
	 config[BUTTON_ID_DOWN]->long_click.handler = (ClickHandler) down_long_click_handler;
	 config[BUTTON_ID_DOWN]->long_click.release_handler = (ClickHandler) down_long_release_handler;
	 */
}

void drawMenu()
{
    for (int i=0; i<3; i++)
    {
        text_layer_set_text(bbuttons[i][i!=2], " ");
        text_layer_set_text(bbuttons[i][2], " ");
        
        text_layer_set_text(bbuttons[i][i==2], cases[i]);
        text_layer_set_font(bbuttons[i][0], fonts_get_system_font(FONT_KEY_GOTHIC_24_BOLD));
    }
}


// This method draws the characters on the right side near the buttons
void drawSides()
{
    if (size==27) // first click (full size)
    {
        // update all 9 labels to their proper values
        for (int h=0; h<3; h++)
        {
            for (int i=0; i<3; i++)
            {
                text_layer_set_text(bbuttons[h][i], btexts[h][i]);
                text_layer_set_background_color(bbuttons[h][i], GColorClear);
                text_layer_set_font(bbuttons[h][i], fonts_get_system_font(FONT_KEY_GOTHIC_18_BOLD));
            }
			
        }
    }
    else if (size==9)   // second click
    {
        
        for (int i=0; i<3; i++)
        {
            text_layer_set_text(bbuttons[i][i!=2], " ");
            text_layer_set_text(bbuttons[i][2], " ");
            
            text_layer_set_text(bbuttons[i][i==2], btexts[top/9][i]);
            text_layer_set_font(bbuttons[i][i==2], fonts_get_system_font(FONT_KEY_GOTHIC_24_BOLD));
        }
		
    } else if (size == 3)
    {
        for (int i=0; i<3; i++)
        {
            setlist[i][2] = master[top+i];
            text_layer_set_text(bbuttons[i][i==2], setlist[i]);
        }
    }
    
}

void initSidesAndText()
{
    wordsYouWrite = text_layer_create(GRect(10, 0, 100, 150));
    text_layer_set_background_color(wordsYouWrite, GColorClear);
    text_layer_set_font(wordsYouWrite, fonts_get_system_font(FONT_KEY_GOTHIC_28_BOLD));
    layer_add_child(window_get_root_layer(tertiary_window), text_layer_get_layer(wordsYouWrite));
	
	
    for (int i = 0; i<3; i++)
    {
        buttons1[i] = text_layer_create(GRect(115, 12*i, 100, 100));
        buttons2[i] = text_layer_create(GRect(115, 12*i+50, 100, 100));
        buttons3[i] = text_layer_create(GRect(115, 12*i+100, 100, 100));
    }
    
    for (int i=0; i<3; i++)
        for (int j=0; j<3; j++)
            layer_add_child(window_get_root_layer(tertiary_window), text_layer_get_layer(bbuttons[i][j]));
	
}

void tertiary_window_load(Window *me)
{
	textLayer = text_layer_create(layer_get_frame(window_get_root_layer(me)));
	//  text_layer_set_text(&textLayer, text_buffer);
    text_layer_set_background_color(textLayer, GColorClear);
	
	text_layer_set_font(textLayer, fonts_get_system_font(FONT_KEY_GOTHIC_28_BOLD));
	layer_add_child(window_get_root_layer(me), text_layer_get_layer(textLayer));
    
    initSidesAndText();
    drawSides();
}

Window *tertiary_open(TertiaryDoneHandler done_handler_)
{
	done_handler = done_handler_;
	memset(text_buffer, 0, sizeof(text_buffer));
	pos = 0;
	change_set(1, true);
	next();
	
	tertiary_window = window_create();
	
	window_set_window_handlers(tertiary_window, (WindowHandlers) {
		.load = tertiary_window_load
	});
	
	//resource_init_current_app(&FONT_DEMO_RESOURCES);
	    
	// Attach our desired button functionality
	window_set_click_config_provider(tertiary_window, (ClickConfigProvider) click_config_provider);
	
	window_stack_push(tertiary_window, true /* Animated */);
	
	return tertiary_window;
}

char *tertiary_get_buffer()
{
	return text_buffer;
}
