#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include "contiki.h"
#include "coap-engine.h"
#include "coap-blocking-api.h"
#include "os/dev/leds.h"
/* Log configuration */
#include "sys/log.h"
#define LOG_MODULE "App"
#define LOG_LEVEL LOG_LEVEL_DBG
extern process_event_t POST_EVENT;

//RESOURCE DEFINITION 
//extern coap_resource_t  temperature;
//extern coap_resource_t  humidity;
//extern coap_resource_t  heating;
extern coap_resource_t  irrigation;

#define SERVER_EP "coap://[fd00::1]:5683"
char *registration_service = "/register";

bool registered = false; 
static struct etimer timer;
#define TEMP_THRESHOLD 20
#define HUM_THRESHOLD 40
extern bool is_cold;
extern bool is_dry;
extern bool irrigation_status;
//extern bool heating_status;
extern int humidity_value;
//extern int temperature_value;
PROCESS(coap_node, "Coap Node");
AUTOSTART_PROCESSES(&coap_node);

void client_chunk_handler(coap_message_t *response) {
  
	const uint8_t *chunk;

	if(response == NULL) {
		puts("Request timed out");
	return;
	}

	if(!registered)
	registered = true;

	int len = coap_get_payload(response, &chunk);
	printf("|%.*s", len, (char *)chunk);
}

/*void temperature_change () {
	temperature_value = temperature_value +5;
}*/

void humidity_change () {
	humidity_value = humidity_value +5;
}

PROCESS_THREAD(coap_node, ev, data){

  static coap_endpoint_t coap_server;
  static coap_message_t request[1];    

  PROCESS_BEGIN();

  LOG_INFO("Starting coap node\n");
  
  //activate the resources
  //coap_activate_resource(&temperature, "temperature-sensor");
  //coap_activate_resource(&heating, "heating-actuator");
  coap_activate_resource(&irrigation, "irrigation-actuator"); 
  //coap_activate_resource(&humidity, "humidity-sensor"); 

  coap_endpoint_parse(SERVER_EP, strlen(SERVER_EP), &coap_server);

  LOG_INFO("Registering resources...\n");
  coap_init_message(request, COAP_TYPE_CON, COAP_GET, 0);
  coap_set_header_uri_path(request, registration_service);

  while(!registered){
    LOG_DBG("Retrying registration...\n");
    COAP_BLOCKING_REQUEST(&coap_server, request, client_chunk_handler);
  }

  LOG_DBG("Registered!\n");

  etimer_set(&timer, 30*CLOCK_SECOND);
  
  while(true) {
	PROCESS_WAIT_EVENT();
	if(ev == PROCESS_EVENT_TIMER){ 

/*		humidity_value = rand()% 100;
		LOG_DBG("humidity: %d\n", humidity_value);
		if(humidity_value < HUM_THRESHOLD){
			is_dry = true;
			irrigation_status= true;
			leds_on(LEDS_NUM_TO_MASK(LEDS_GREEN));
			humidity_change();
		}else{
			is_dry = false;
			leds_off(LEDS_NUM_TO_MASK(LEDS_GREEN));
		}
		
		temperature_value = rand()% 100;
		LOG_DBG("temperature: %d\n", temperature_value);
		if(temperature_value < TEMP_THRESHOLD){
			is_cold = true;
			heating_status = true;
			leds_on(LEDS_NUM_TO_MASK(LEDS_RED));
			temperature_change();
		}else{
			is_cold = false;
			leds_off(LEDS_NUM_TO_MASK(LEDS_RED));
		} */

		//humidity.trigger();
		//temperature.trigger();
		irrigation.trigger();
		///heating.trigger();
		etimer_reset(&timer);
	}
  }

  PROCESS_END();
}










