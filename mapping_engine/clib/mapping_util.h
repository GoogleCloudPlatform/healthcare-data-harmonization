#ifndef THIRD_PARTY_CLOUD_HEALTHCARE_DATA_HARMONIZATION_MAPPING_ENGINE_CLIB_MAPPING_UTIL_H_
#define THIRD_PARTY_CLOUD_HEALTHCARE_DATA_HARMONIZATION_MAPPING_ENGINE_CLIB_MAPPING_UTIL_H_

#include <jni.h>

// Returns a pointer to an array of bytes representing the string in modified
// UTF-8 encoding. If isCopy is not NULL, then *isCopy is set to JNI_TRUE if a
// copy is made; or it is set to JNI_FALSE if no copy is made.
const char *GetStringUTFChars(JNIEnv *env, jstring str, jboolean *isCopy);

// Constructs a new java.lang.String object from an array of characters in
// modified UTF-8 encoding.
const jstring NewStringUTF(JNIEnv *env, const char *utf);

// Throws a new RuntimeException with msg as the error message.
void ThrowNewRuntimeException(JNIEnv *env, const char *msg);

#endif  // THIRD_PARTY_CLOUD_HEALTHCARE_DATA_HARMONIZATION_MAPPING_ENGINE_CLIB_MAPPING_UTIL_H_
