#include <jni.h>
#include "mapping_util.h"

static const char kJavaRunTimeException[] = "java/lang/RuntimeException";

const char* GetStringUTFChars(JNIEnv *env, jstring str, jboolean *isCopy) {
    return (*env)->GetStringUTFChars(env, str, isCopy);
}

const jstring NewStringUTF(JNIEnv *env, const char *utf) {
    return (*env)->NewStringUTF(env, utf);
}

void ThrowNewRuntimeException(JNIEnv *env, const char *msg) {
    jclass cls = (*env)->FindClass(env, kJavaRunTimeException);
    if (cls != NULL) {
        (*env)->ThrowNew(env, cls, msg);
    }
    (*env)->DeleteLocalRef(env, cls);
}
