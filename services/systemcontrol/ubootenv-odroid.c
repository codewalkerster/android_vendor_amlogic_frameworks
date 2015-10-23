#define LOG_TAG "SystemControl"

#include <stdio.h>
#include <sys/types.h>
#include <fcntl.h>

#include <cutils/properties.h>
#include <cutils/threads.h>

#include "common.h"

static mutex_t env_lock = MUTEX_INITIALIZER;

static char *prefix = "ubootenv.var";
static int bootenv_init_done = 0;

void bootenv_print(void)
{
}

static int is_bootenv_varible(const char *key)
{
	if (!key || !(*key))
		return 0;

	return (strncmp(key, prefix, strlen(prefix)) == 0)
		&& (strlen(key) > strlen(prefix));
}

int bootenv_init(void)
{
	char cmdline[4096];
	int fd;
	char *str1, *str2, *token, *subtoken;
	char *saveptr1, *saveptr2;

	fd = open("/proc/cmdline", O_RDONLY);
	read(fd, cmdline, sizeof(cmdline));
	close(fd);

	for (str1 = cmdline; ; str1 = NULL) {
		token = strtok_r(str1, " ", &saveptr1);
		if (token == NULL)
			break;

		for (str2 = token; ; str2 = NULL) {
			subtoken = strtok_r(str2, "=", &saveptr2);
			if (subtoken == NULL)
				break;
			if (token != subtoken) {
				char key[80];
				sprintf(key, "%s.%s", prefix, token);
				SYS_LOGI("[ubootenv] key=%s, value=%s\n",
						key, subtoken);
				property_set(key, subtoken);
			}
		}
	}

	bootenv_init_done = 1;

	return 0;
}

int bootenv_reinit(void)
{
	bootenv_init();
	return 0;
}

int bootenv_update(const char *key, const char *value)
{
	if (!bootenv_init_done) {
		SYS_LOGE("[ubootenv] bootenv do not init\n");
		return -1;
	}

	/*
	 * TODO: What does ODROID do if update is necessary?
	 */
#if 0
	SYS_LOGI("[ubootenv] update_bootenv_varible key [%s]: value [%s] \n",
			key, value);
#endif

	return 0;
}

const char *bootenv_get(const char *key)
{
	if (!is_bootenv_varible(key)) {
		//should assert here.
		SYS_LOGE("[ubootenv] %s is not a ubootenv varible.\n", key);
		return NULL;
	}

	char value[PROP_VALUE_MAX];
	property_get(key, value, "");

	if (strlen(value) == 0)
		return NULL;

	return value;
}
