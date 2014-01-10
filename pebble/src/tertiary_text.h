//
//  tertiary_text.h
//  pebble_whatsapp
//
//  Created by Jannis Pohl on 13.10.13.
//
//

#ifndef pebble_whatsapp_tertiary_text_h
#define pebble_whatsapp_tertiary_text_h

typedef void (*TertiaryDoneHandler)(const char *buffer);

Window *tertiary_open(TertiaryDoneHandler doneHandler);

#endif
