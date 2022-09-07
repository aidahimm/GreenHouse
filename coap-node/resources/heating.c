#include <stdio.h>
#include <string.h>
#include "coap-engine.h"
#include "coap.h"
#include "contiki.h"
#include "os/dev/leds.h"

#include "sys/log.h"
#define LOG_MODULE "Heating Actuator"
#define LOG_LEVEL LOG_LEVEL_DBG

extern struct process coap_node;
process_event_t POST_EVENT;
bool heating_status = false;

static void res_get_handler(coap_message_t *request, coap_message_t *response, uint8_t *buffer, uint16_t preferred_size, int32_t *offset);

static void res_post_put_handler(coap_message_t *request, coap_message_t *response, uint8_t *buffer, uint16_t preferred_size, int32_t *offset);

static void res_event_handler(void);

EVENT_RESOURCE(heating, "title=\"Heating actuator ?POST/PUT status=on|off\";rt=\"Heating\"f",
	       res_get_handler,
               res_post_put_handler,
               res_post_put_handler,
               NULL,
               res_event_handler);

static void res_event_handler(void) {
	LOG_DBG("sending notification");
  	coap_notify_observers(&heating);
}

static void res_get_handler(coap_message_t *request, coap_message_t *response, uint8_t *buffer, uint16_t preferred_size, int32_t *offset){

	if(request!=NULL){
		LOG_DBG("Received GET\n");
	}

	LOG_DBG("Heating Status: %d\n", heating_status);
	
	unsigned int accept = -1;
	coap_get_header_accept(request, &accept);

	if(accept == -1 || accept == APPLICATION_JSON) {
	    coap_set_header_content_format(response, APPLICATION_JSON);
	    snprintf((char *)buffer, COAP_MAX_CHUNK_SIZE, "{\"Heating\":\"%d\"}", heating_status);
	    coap_set_payload(response, buffer, strlen((char *)buffer));

	} else {
	    coap_set_status_code(response, NOT_ACCEPTABLE_4_06);
	    const char *msg = "Supporting content-type application/json";
	    coap_set_payload(response, msg, strlen(msg));
	}
}

static void res_post_put_handler(coap_message_t *request, coap_message_t *response, uint8_t *buffer, uint16_t preferred_size, int32_t *offset){  
	
	if(request!=NULL){
		LOG_DBG("Received POST/PUT\n");
	}

	unsigned int accept = -1;
	coap_get_header_accept(request, &accept);

	if(accept == -1 || accept == APPLICATION_JSON) {

		size_t len = 0; 
		const char *heating_status_x = NULL;
		int success = 0;
	
		if((len = coap_get_post_variable(request, "mode", &heating_status_x))){

			if(strncmp(heating_status_x, "on", len)== 0){
				heating_status = true;
				LOG_DBG("Beginning Heating...\n");
				leds_on(LEDS_NUM_TO_MASK(LEDS_RED));
				heating_status = 1;
				success = 1;

			} else if(strncmp(heating_status_x, "off", len)== 0){
				heating_status = false;
				LOG_DBG("Stopping Heating...\n");
				leds_off(LEDS_NUM_TO_MASK(LEDS_RED));
				heating_status = 0;
				success = 1;
			}

			if (!success) coap_set_status_code(response, BAD_REQUEST_4_00);
			
		} else coap_set_status_code(response, BAD_REQUEST_4_00);
		
	} else {
		coap_set_status_code(response, NOT_ACCEPTABLE_4_06);
		const char *msg = "Supporting content-type application/json";
		coap_set_payload(response, msg, strlen(msg));
	}
  
}




