#include <sys/inotify.h>
#include <sys/stat.h>
#include <sys/types.h>
#include <sys/socket.h>
#include <sys/un.h>
#include <sys/time.h>
#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#include <fcntl.h>
#include <dirent.h>
#include <unistd.h>
#include <pthread.h>
#include <errno.h>
#include <stddef.h>
#include <sys/file.h>
#include "android/log.h"

#pragma clang diagnostic ignored "-Wmissing-noreturn"

#define BUFFER_LENGTH 4608
#define SLEEP_INTERVEL 10   // every x seconds to check if process is running
#define PROC_NOT_FOUND 0
#define BUFFER_SIZE 3 // 缓冲区数量
#define TAG "Supervisor"

static char CommandTmp[BUFFER_LENGTH];
static int paramNum = 6;
static int paramPackageNameIndex = 1;
static int paramServiceNameIndex = 2;
static int paramProcessNameIndex = 3;
static int paramAppDirIndex = 4;
static int paramUserSerialIndex = 5;

struct prodcons {
    // 缓冲区相关数据结构
    int buffer[BUFFER_SIZE]; /* 实际数据存放的数组*/
    pthread_mutex_t lock; /* 互斥体lock 用于对缓冲区的互斥操作 */
    int readpos, writepos; /* 读写指针*/
    pthread_cond_t notempty; /* 缓冲区非空的条件变量 */
    pthread_cond_t notfull; /* 缓冲区未满的条件变量 */
};
struct prodcons buffer;

/* 初始化缓冲区结构 */
void init(struct prodcons *b) {
    pthread_mutex_init(&b->lock, NULL);
    pthread_cond_init(&b->notempty, NULL);
    pthread_cond_init(&b->notfull, NULL);
    b->readpos = 0;
    b->writepos = 0;
}
/* 将产品放入缓冲区,这里是存入一个整数*/
void put(struct prodcons *b, int data) {
    pthread_mutex_lock(&b->lock);
    /* 等待缓冲区未满*/
    if ((b->writepos + 1) % BUFFER_SIZE == b->readpos) {
        pthread_cond_wait(&b->notfull, &b->lock);
    }
    /* 写数据,并移动指针 */
    b->buffer[b->writepos] = data;
    b->writepos++;
    if (b->writepos >= BUFFER_SIZE)
        b->writepos = 0;
    /* 设置缓冲区非空的条件变量*/
    pthread_cond_signal(&b->notempty);
    pthread_mutex_unlock(&b->lock);
}
/* 从缓冲区中取出整数*/
int get(struct prodcons *b) {
    int data;
    pthread_mutex_lock(&b->lock);
    /* 等待缓冲区非空*/
    if (b->writepos == b->readpos) {
        pthread_cond_wait(&b->notempty, &b->lock);
    }
    /* 读数据,移动读指针*/
    data = b->buffer[b->readpos];
    b->readpos++;
    if (b->readpos >= BUFFER_SIZE)
        b->readpos = 0;
    /* 设置缓冲区未满的条件变量*/
    pthread_cond_signal(&b->notfull);
    pthread_mutex_unlock(&b->lock);
    return data;
}


void android_log_write_debug(const char *tag, const char *msg) {
    __android_log_write(ANDROID_LOG_DEBUG, tag, msg);
}

void android_log_write_error(const char *tag, const char *msg) {
    __android_log_write(ANDROID_LOG_ERROR, tag, msg);
}

int strbcmp(const char *haystack, const char *needle) {
    int length;
    char *sub;
    if (haystack && needle && strlen(haystack) >= (length = strlen(needle)) && (sub = strstr(haystack, needle))
        && strlen(sub) == length)
        return 0;
    return 1;
}

pid_t proc_find(const char* name, const pid_t curpid) {
    DIR* dir;
    struct dirent* ent;
    char buf[512];

    long pid;
    char pname[100] = { 0, };
    char state;
    FILE *fp = NULL;

    if (!(dir = opendir("/proc"))) {
        android_log_write_error(TAG, "can't open /proc");
        return -1;
    }
    android_log_write_debug(TAG, name);
    while ((ent = readdir(dir)) != NULL) {
        long lpid = atol(ent->d_name);
        if (lpid < 0 || (((pid_t) lpid) == curpid)) {
            sprintf(CommandTmp, "continue curpid : %d", curpid);
            android_log_write_debug(TAG, CommandTmp);
            continue;
        }
        snprintf(buf, sizeof(buf), "/proc/%ld/stat", lpid);
        fp = fopen(buf, "r");

        if (fp) {
            if ((fscanf(fp, "%ld (%[^)]) %c", &pid, pname, &state)) != 3) {
                android_log_write_debug(TAG, "fscanf failed \n");
                fclose(fp);
                closedir(dir);
                return -1;
            }
            if (strlen(pname) >= 15 && !strbcmp(name, pname)) {
                fclose(fp);
                closedir(dir);
                return (pid_t) lpid;
            }
            fclose(fp);
        }
    }

    closedir(dir);
    return -1;
}

void start_supervisor(void* args) {
    char* appDir = ((void**)args)[0];
    char* processName = ((void**)args)[1];

    while (1) {
        sleep(SLEEP_INTERVEL);
        android_log_write_debug(TAG, "start NotifyService");

        FILE *p_appDir = fopen(appDir, "r");
        if (p_appDir == NULL) {
            exit(1);
            break;
        }
        fclose(p_appDir);

        pid_t pid = proc_find(processName, getpid());
        if (pid != -1) {
            android_log_write_debug(TAG, "proc_find");
            continue;
        }
        // send proc not find msg
        put(&buffer, PROC_NOT_FOUND);
    }
}

int check_lock(char* appFileDir, char* appLockFile) {
    android_log_write_debug(TAG, "check_lock start");
    android_log_write_debug(TAG, appFileDir);
    android_log_write_debug(TAG, appLockFile);

    // 若监听文件所在文件夹不存在，创建
    FILE *p_filesDir = fopen(appFileDir, "r");
    if (p_filesDir == NULL) {
        int filesDirRet = mkdir(appFileDir, S_IRWXU | S_IRWXG | S_IXOTH);
        if (filesDirRet == -1) {
            p_filesDir = fopen(appFileDir, "r");
            if (p_filesDir == NULL) {
                android_log_write_error(TAG, "mkdir failed !!!");
                return -1;
            }
        }
    }
    if (p_filesDir != NULL) {
        fclose(p_filesDir);
    }
    android_log_write_debug(TAG, "mkdir success");

    int lockFileDescriptor = open(appLockFile, O_CREAT | O_RDONLY, S_IRUSR | S_IWUSR);
    int lockRet = flock(lockFileDescriptor, LOCK_EX | LOCK_NB);
    if (lockRet == -1) {
        android_log_write_debug(TAG, "Supervisor by another process");
        close(lockFileDescriptor);
    }
    return lockRet;
}


int main(int argc, char **argv) {
    if (argc != (paramNum - 1) && argc != paramNum) {
        return 0;
    }

    strcpy(argv[0], "ss_supervisor");
    char* APP_DIR = argv[paramAppDirIndex];
    const char* packageNameChars = argv[paramPackageNameIndex];
    const char* serviceNameChars = argv[paramServiceNameIndex];
    char* PROCESS_NAME = argv[paramProcessNameIndex];

    char APP_FILES_DIR[strlen(APP_DIR) + 7];
    sprintf(APP_FILES_DIR, "%s/files", APP_DIR);
    char APP_LOCK_FILE[strlen(APP_FILES_DIR) + 11];
    sprintf(APP_LOCK_FILE, "%s/lockFile", APP_FILES_DIR);

    android_log_write_debug(TAG, APP_DIR);
    android_log_write_debug(TAG, APP_FILES_DIR);
    android_log_write_debug(TAG, PROCESS_NAME);

    // 创建锁文件，通过检测加锁状态来保证只有一个监听进程
    int lock_ret = check_lock(APP_FILES_DIR, APP_LOCK_FILE);
    if(lock_ret == -1){
        exit(1);
    }

    init(&buffer);

    pthread_t supervisor_thread;
    void* supervisorThreadArg[3] = {APP_DIR, PROCESS_NAME};
    int ret_supervisor_thread = pthread_create(&supervisor_thread, NULL, (void *) &start_supervisor, supervisorThreadArg);

    if (ret_supervisor_thread != 0) {
        android_log_write_debug(TAG, "Add Supervisor Fail");
    } else {
        android_log_write_debug(TAG, "Add Supervisor Success");
    }


    while(1) {
        int msg_type = get(&buffer);
        // wait for msg
        if (msg_type == PROC_NOT_FOUND) {
            android_log_write_debug(TAG, "PROC_NOT_FOUND");
            android_log_write_debug(TAG, packageNameChars);
            android_log_write_debug(TAG, serviceNameChars);
            if (argc == (paramNum - 1)) {
                // 执行命令am startservice -n $(packageName)/$(serviceName)
                sprintf(CommandTmp, "am startservice -n %s/%s", packageNameChars, serviceNameChars);
            } else if (argc == paramNum) {
                // 执行命令am startservice --user userSerial -n $(packageName)/$(serviceName)
                const char* userSerialChars = argv[paramUserSerialIndex];
                sprintf(CommandTmp, "am startservice --user %s -n %s/%s", userSerialChars, packageNameChars, serviceNameChars);
            }
            android_log_write_debug(TAG, CommandTmp);
            int result = system(CommandTmp);
            if (result == -1) {
                android_log_write_debug(TAG, "exec command failed");
            }
        }
    }
    return 0;
}
