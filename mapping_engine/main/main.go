// Copyright 2019 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

// A driver to run the mapping library.
package main

import (
	"context"
	"encoding/json"
	"flag"
	"io/ioutil"
	"log"
	"os"
	"path/filepath"
	"strings"

	"github.com/GoogleCloudPlatform/healthcare-data-harmonization/mapping_engine/transform" /* copybara-comment: transform */
	"github.com/GoogleCloudPlatform/healthcare-data-harmonization/mapping_engine/util/jsonutil" /* copybara-comment: jsonutil */
	"github.com/GoogleCloudPlatform/healthcare-data-harmonization/mapping_language/transpiler" /* copybara-comment: transpiler */
	"github.com/golang/protobuf/proto" /* copybara-comment: proto */

	dhpb "github.com/GoogleCloudPlatform/healthcare-data-harmonization/mapping_engine/proto" /* copybara-comment: data_harmonization_go_proto */
	hpb "github.com/GoogleCloudPlatform/healthcare-data-harmonization/mapping_engine/proto" /* copybara-comment: harmonization_go_proto */
	libpb "github.com/GoogleCloudPlatform/healthcare-data-harmonization/mapping_engine/proto" /* copybara-comment: library_go_proto */
	fileutil "github.com/GoogleCloudPlatform/healthcare-data-harmonization/mapping_engine/util/ioutil" /* copybara-comment: ioutil */

)

const fileWritePerm = 0666

type stringSlice []string

// String joins the slice into a semicolon-separated string.
func (s *stringSlice) String() string {
	if s == nil {
		return ""
	}
	return strings.Join(*s, ";")
}

// Set splits the given semicolon-separated string into this stringSlice.
func (s *stringSlice) Set(v string) error {
	*s = strings.Split(v, ";")
	return nil
}

var (
	inputFile           = flag.String("input_file_spec", "", "Input data file or directory (JSON).")
	outputDir           = flag.String("output_dir", "", "Path to the directory where the output will be written to. Leave empty to print to stdout.")
	mappingFile         = flag.String("mapping_file_spec", "", "Mapping file (DHML file).")
	harmonizeCodeConfig = flag.String("harmonize_code_spec", "", "Code harmonization config (text proto)")
	harmonizeUnitConfig = flag.String("harmonize_unit_spec", "", "Unit harmonization config (text proto)")
	libConfigDir        = flag.String("lib_dir_spec", "", "Path to the directory where the libraries are.")

	verbose = flag.Bool("verbose", false, "Enables outputting full trace of operations at the end.")

)

const (
	dhmlExtension      = ".dhml"
	textProtoExtension = ".textproto"
	jsonExtension      = ".json"
	inputExtension     = ".input"
	outputExtension    = ".output.json"
)

func outputFileName(outputPath, inputFilePath string) string {
	f := filepath.Base(inputFilePath)
	f = strings.TrimSuffix(f, jsonExtension)
	f = strings.TrimSuffix(f, inputExtension)
	return filepath.Join(outputPath, f+outputExtension)
}

func readLibConfigs(path string) []*libpb.LibraryConfig {
	if path == "" {
		return []*libpb.LibraryConfig{}
	}
	fs := fileutil.MustReadDir(path, "library dir")

	var libs []*libpb.LibraryConfig
	for _, f := range fs {

		var lbc *libpb.LibraryConfig
		if strings.HasSuffix(f, dhmlExtension) {

			b := fileutil.MustRead(f, "library file")

			c, err := transpiler.Transpile(string(b))
			if err != nil {
				log.Fatalf("Failed to transpile library file %v: %v", f, err)
			}
			lbc = &libpb.LibraryConfig{Projector: c.GetProjector()}
		}

		if strings.HasSuffix(f, textProtoExtension) {
			ls := fileutil.MustRead(f, "library file")
			lbc = &libpb.LibraryConfig{}
			if err := proto.UnmarshalText(string(ls), lbc); err != nil {
				log.Fatalf("Failed to parse library %q: %v", ls, err)
			}
		}

		if lbc == nil {
			log.Printf("Unsupported library file: %v. library files must be either .dhml or .textproto files", f)
		}

		libs = append(libs, lbc)
	}
	return libs
}

func readInputs(path string) []string {
	fi, err := os.Stat(*inputFile)
	if err != nil {
		log.Fatalf("Failed to read input spec: %v", err)
	}

	var fs []string
	switch fm := fi.Mode(); {
	case fm.IsDir():
		fs = append(fs, fileutil.MustReadDir(*inputFile, "input dir")...)
	case fm.IsRegular():
		fs = append(fs, *inputFile)
	}
	return fs
}

func main() {
	flag.Parse()

	ch := &hpb.CodeHarmonizationConfig{}
	if *harmonizeCodeConfig != "" {
		c := fileutil.MustRead(*harmonizeCodeConfig, "code harmonization config")
		if err := proto.UnmarshalText(string(c), ch); err != nil {
			log.Fatalf("Failed to parse code harmonization config: %v", err)
		}
	}

	uh := &hpb.UnitHarmonizationConfig{}
	if *harmonizeUnitConfig != "" {
		u := fileutil.MustRead(*harmonizeUnitConfig, "unit harmonization config")
		if err := proto.UnmarshalText(string(u), uh); err != nil {
			log.Fatalf("Failed to read unit harmonization config: %v", err)
		}
	}

	dhConfig := &dhpb.DataHarmonizationConfig{
		StructureMappingConfig: &hpb.StructureMappingConfig{
			Mapping: &hpb.StructureMappingConfig_MappingLanguageString{
				MappingLanguageString: string(fileutil.MustRead(*mappingFile, "mapping")),
			},
		},
		HarmonizationConfig:     ch,
		UnitHarmonizationConfig: uh,
		LibraryConfig:           readLibConfigs(*libConfigDir),
	}

	var tr *transform.Transformer
	var err error

	if tr, err = transform.NewTransformer(context.Background(), dhConfig); err != nil {
		log.Fatalf("Failed to load mapping config: %v", err)
	}

	tconfig := transform.TransformationConfigs{
		LogTrace: *verbose,
	}

	for _, f := range readInputs(*inputFile) {
		i := fileutil.MustRead(f, "input")

		ji := &jsonutil.JSONContainer{}
		if err := ji.UnmarshalJSON(i); err != nil {
			log.Fatalf("Failed to parse input JSON in file %v: %v", f, err)
		}

		res, err := tr.Transform(ji, tconfig)
		if err != nil {
			log.Fatalf("Mapping failed for input file %v: %v", f, err)
		}

		bres, err := json.MarshalIndent(res, "", "  ")
		if err != nil {
			log.Fatalf("Failed to serialize output: %v", err)
		}

		op := outputFileName(*outputDir, f)
		if *outputDir == "" {
			log.Printf("File %q\n\n%s\n", op, string(bres))
		} else {
			if err := ioutil.WriteFile(op, bres, fileWritePerm); err != nil {
				log.Fatalf("Could not write output file %q: %v", op, err)
			}
		}
	}
}
