CONTIKI_PROJECT = mqtt-node
all: $(CONTIKI_PROJECT)

CONTIKI=../../..

CFLAGS += -DPROJECT_CONF_H=\"project-conf.h\"
PLATFORMS_EXCLUDE = sky z1

include $(CONTIKI)/Makefile.dir-variables
MODULES += $(CONTIKI_NG_APP_LAYER_DIR)/mqtt

-include $(CONTIKI)/Makefile.identify-target

MODULES_REL += arch/platform/$(TARGET)

include $(CONTIKI)/Makefile.include
