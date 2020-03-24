/*
 * Copyright 2019 Google LLC.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Package main exposes APIs from the mapping library to other languages with CGO.
package main

//#cgo CFLAGS: -I/usr/lib/jvm/java-8-openjdk-amd64/include/
//#cgo CFLAGS: -I/usr/lib/jvm/java-8-openjdk-amd64/include/linux/
//#cgo LDFLAGS: mapping_util.o
/*
#include "../clib/mapping_util.h"
#include <jni.h>
#include <stdlib.h>
*/
import "C"

import (
	"context"
	"encoding/json"
	"fmt"
	"unsafe"

	"github.com/GoogleCloudPlatform/healthcare-data-harmonization/mapping_engine/util/jsonutil" /* copybara-comment: jsonutil */
	"github.com/GoogleCloudPlatform/healthcare-data-harmonization/mapping_language/transpiler" /* copybara-comment: transpiler */
	"github.com/golang/protobuf/proto" /* copybara-comment: proto */

	dhpb "github.com/GoogleCloudPlatform/healthcare-data-harmonization/mapping_engine/proto" /* copybara-comment: data_harmonization_go_proto */
	hpb "github.com/GoogleCloudPlatform/healthcare-data-harmonization/mapping_engine/proto" /* copybara-comment: harmonization_go_proto */
	whistler "github.com/GoogleCloudPlatform/healthcare-data-harmonization/mapping_engine/transform" /* copybara-comment: transform */
)

var transformer *whistler.Transformer

//export Java_com_google_cloud_healthcare_etl_util_library_TransformWrapper_transform
func Java_com_google_cloud_healthcare_etl_util_library_TransformWrapper_transform(env *C.JNIEnv, cls C.jclass, cInput C.jstring) C.jstring {
	input := jStringGoString(env, cInput)
	i := []byte(input)
	ji := &jsonutil.JSONContainer{}
	if err := ji.UnmarshalJSON(i); err != nil {
		throwRuntimeException(env, fmt.Sprintf("unable to unmarshal JSON: %v", err))
		return emptyJString(env)
	}
	if transformer == nil {
		throwRuntimeException(env, fmt.Sprint("transformer has not been initialized yet"))
		return emptyJString(env)
	}
	// TODO: Should we expose the transformation configs to the plugin?
	res, err := transformer.Transform(ji, whistler.TransformationConfigs{})
	if err != nil {
		throwRuntimeException(env, fmt.Sprintf("unable to transform: %v", err))
		return emptyJString(env)
	}
	bres, err := json.Marshal(res)
	if err != nil {
		throwRuntimeException(env, fmt.Sprintf("unable to marshal result: %v", err))
		return emptyJString(env)
	}
	return goStringJString(env, string(bres))
}

//export Java_com_google_cloud_healthcare_etl_util_library_TransformWrapper_initializeWhistler
func Java_com_google_cloud_healthcare_etl_util_library_TransformWrapper_initializeWhistler(env *C.JNIEnv, cls C.jclass, cConfig C.jstring) {
	config := jStringGoString(env, cConfig)
	dhc := &dhpb.DataHarmonizationConfig{}
	if err := proto.UnmarshalText(config, dhc); err != nil {
		throwRuntimeException(env, fmt.Sprintf("unable to unmarshal data harmonization config: %v", err))
		return
	}
	if err := initialize(dhc); err != nil {
		throwRuntimeException(env, fmt.Sprintf("unable to initialize config: %v", err))
		return
	}
}

//export Java_com_google_cloud_healthcare_etl_util_library_TransformWrapper_initializeWhistle
func Java_com_google_cloud_healthcare_etl_util_library_TransformWrapper_initializeWhistle(env *C.JNIEnv, cls C.jclass, cConfig C.jstring) {
	config := jStringGoString(env, cConfig)
	mc, err := transpiler.Transpile(config)
	if err != nil {
		throwRuntimeException(env, fmt.Sprintf("unable to transpile Whistle mapping: %v", err))
		return
	}
	if err := initialize(&dhpb.DataHarmonizationConfig{
		StructureMappingConfig: &hpb.StructureMappingConfig{
			Mapping: &hpb.StructureMappingConfig_MappingConfig{
				MappingConfig: mc,
			},
		},
	}); err != nil {
		throwRuntimeException(env, fmt.Sprintf("unable to initialize Whistle mapping: %v", err))
		return
	}
}

func initialize(dhc *dhpb.DataHarmonizationConfig) error {
	var err error
	transformer, err = whistler.NewTransformer(context.Background(), dhc)
	if err != nil {
		return fmt.Errorf("unable to initialize data harmonization config: %v", err)
	}
	return nil
}

// Returns an empty C.jstring, used as return type for error cases.
func emptyJString(env *C.JNIEnv) C.jstring {
	return goStringJString(env, "")
}

// Throws a RuntimeException in the Java world with a provided messsage.
func throwRuntimeException(env *C.JNIEnv, msg string) {
	cMsg := C.CString(msg)
	C.ThrowNewRuntimeException(env, cMsg)
	C.free(unsafe.Pointer(cMsg))
}

// jStringGoString converts Java strings to Go strings.
func jStringGoString(env *C.JNIEnv, s C.jstring) string {
	return C.GoString(C.GetStringUTFChars(env, s, (*C.jboolean)(nil)))
}

// goStringJString converts Go strings to Java strings.
func goStringJString(env *C.JNIEnv, s string) C.jstring {
	cs := C.CString(s)
	js := C.NewStringUTF(env, cs)
	C.free(unsafe.Pointer(cs))
	return js
}

// jBooleanGoBool converts Java booleans to Go booleans.
func jBooleanGoBool(b C.jboolean) bool {
	return b == C.JNI_TRUE
}

func main() {
	// No op.
}
