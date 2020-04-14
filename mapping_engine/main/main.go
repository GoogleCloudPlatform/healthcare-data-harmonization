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
	"github.com/golang/protobuf/proto" /* copybara-comment: proto */

	dhpb "github.com/GoogleCloudPlatform/healthcare-data-harmonization/mapping_engine/proto" /* copybara-comment: data_harmonization_go_proto */
	hpb "github.com/GoogleCloudPlatform/healthcare-data-harmonization/mapping_engine/proto" /* copybara-comment: harmonization_go_proto */
	httppb "github.com/GoogleCloudPlatform/healthcare-data-harmonization/mapping_engine/proto" /* copybara-comment: http_go_proto */
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
	inputFile         = flag.String("input_file_spec", "", "Input data file or glob pattern (JSON).")
	outputDir         = flag.String("output_dir", "", "Path to the directory where the output will be written to. Leave empty to print to stdout.")
	mappingFile       = flag.String("mapping_file_spec", "", "Mapping file (DHML file).")
	harmonizeCodeDir  = flag.String("harmonize_code_dir_spec", "", "Path to the directory where the FHIR ConceptMaps that should be used for harmozing codes are.")
	harmonizeUnitFile = flag.String("harmonize_unit_spec", "", "Unit harmonization file (textproto)")
	libDir            = flag.String("lib_dir_spec", "", "Path to the directory where the libraries are.")
	dhConfigFile      = flag.String("data_harmonization_config_file_spec", "", "Data Harmonization config (textproto). If this flag is specified, other configs cannot be specified.")

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

func libConfigs(path string) []*libpb.LibraryConfig {
	if path == "" {
		return nil
	}
	fs := fileutil.MustReadDir(path, "library dir")

	var libs []*libpb.LibraryConfig
	for _, f := range fs {

		var lbc *libpb.LibraryConfig
		lbc = &libpb.LibraryConfig{UserLibraries: []*libpb.UserLibrary{
			&libpb.UserLibrary{
				Type: hpb.MappingType_MAPPING_LANGUAGE,
				Path: &httppb.Location{Location: &httppb.Location_LocalPath{LocalPath: f}},
			}}}

		libs = append(libs, lbc)
	}
	return libs
}

func codeHarmonizationConfig(path string) *hpb.CodeHarmonizationConfig {
	if path == "" {
		return nil
	}
	fs := fileutil.MustReadDir(path, "code harmonization dir")

	var locs []*httppb.Location
	for _, f := range fs {
		locs = append(locs, &httppb.Location{Location: &httppb.Location_LocalPath{LocalPath: f}})
	}
	return &hpb.CodeHarmonizationConfig{CodeLookup: locs}
}

func unitHarmonizationConfig(path string) *hpb.UnitHarmonizationConfig {
	if path == "" {
		return nil
	}
	return &hpb.UnitHarmonizationConfig{
		UnitConversion: &httppb.Location{
			Location: &httppb.Location_LocalPath{LocalPath: path},
		}}
}

func readInputs(pattern string) []string {
	fs := fileutil.MustReadGlob(pattern, "input_dir")

	var ret []string
	for _, f := range fs {
		fi, err := os.Stat(f)
		if err != nil {
			log.Fatalf("Failed to read input spec: %v", err)
		}
		if fi.IsDir() {
			continue
		}
		ret = append(ret, f)
	}
	return ret
}

func main() {
	flag.Parse()

	dhConfig := &dhpb.DataHarmonizationConfig{}

	if *dhConfigFile != "" {
		if *mappingFile != "" || *harmonizeCodeDir != "" || *harmonizeUnitFile != "" || *libDir != "" {
			log.Fatal("data_harmonization_config_file_spec flag should not be set along with other configuration flags " +
				"(mapping_file_spec, harmonize_code_dir_spec, harmonize_unit_spec, lib_dir_spec).")
		}
		n := fileutil.MustRead(*dhConfigFile, "data harmonization config")
		if err := proto.UnmarshalText(string(n), dhConfig); err != nil {
			log.Fatalf("Failed to parse data harmonization config")
		}
	} else {
		dhConfig = &dhpb.DataHarmonizationConfig{
			StructureMappingConfig: &hpb.StructureMappingConfig{
				Mapping: &hpb.StructureMappingConfig_MappingLanguageString{
					MappingLanguageString: string(fileutil.MustRead(*mappingFile, "mapping")),
				},
			},
			HarmonizationConfig:     codeHarmonizationConfig(*harmonizeCodeDir),
			UnitHarmonizationConfig: unitHarmonizationConfig(*harmonizeUnitFile),
			LibraryConfig:           libConfigs(*libDir),
		}
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
