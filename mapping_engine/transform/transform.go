// Copyright 2020 Google LLC.
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

// Package transform contains methods to transform json trees as specified by the config.
package transform

import (
	"context"
	"encoding/json"
	"fmt"
	"log"

	"github.com/golang/protobuf/proto" /* copybara-comment: proto */

	"github.com/GoogleCloudPlatform/healthcare-data-harmonization/mapping_engine/auth" /* copybara-comment: auth */
	"github.com/GoogleCloudPlatform/healthcare-data-harmonization/mapping_engine/builtins" /* copybara-comment: builtins */
	"github.com/GoogleCloudPlatform/healthcare-data-harmonization/mapping_engine/cloudfunction" /* copybara-comment: cloudfunction */
	"github.com/GoogleCloudPlatform/healthcare-data-harmonization/mapping_engine/errors" /* copybara-comment: errors */
	"github.com/GoogleCloudPlatform/healthcare-data-harmonization/mapping_engine/fetch" /* copybara-comment: fetch */
	"github.com/GoogleCloudPlatform/healthcare-data-harmonization/mapping_engine/harmonization/harmonizecode" /* copybara-comment: harmonizecode */
	"github.com/GoogleCloudPlatform/healthcare-data-harmonization/mapping_engine/harmonization/harmonizeunit" /* copybara-comment: harmonizeunit */
	"github.com/GoogleCloudPlatform/healthcare-data-harmonization/mapping_engine/mapping" /* copybara-comment: mapping */
	"github.com/GoogleCloudPlatform/healthcare-data-harmonization/mapping_engine/postprocess" /* copybara-comment: postprocess */
	"github.com/GoogleCloudPlatform/healthcare-data-harmonization/mapping_engine/projector" /* copybara-comment: projector */
	"github.com/GoogleCloudPlatform/healthcare-data-harmonization/mapping_engine/types" /* copybara-comment: types */
	"github.com/GoogleCloudPlatform/healthcare-data-harmonization/mapping_engine/util/gcsutil" /* copybara-comment: gcsutil */
	"github.com/GoogleCloudPlatform/healthcare-data-harmonization/mapping_engine/util/jsonutil" /* copybara-comment: jsonutil */
	"github.com/GoogleCloudPlatform/healthcare-data-harmonization/mapping_language/transpiler" /* copybara-comment: transpiler */

	dhpb "github.com/GoogleCloudPlatform/healthcare-data-harmonization/mapping_engine/proto" /* copybara-comment: data_harmonization_go_proto */
	hapb "github.com/GoogleCloudPlatform/healthcare-data-harmonization/mapping_engine/proto" /* copybara-comment: harmonization_go_proto */
	httppb "github.com/GoogleCloudPlatform/healthcare-data-harmonization/mapping_engine/proto" /* copybara-comment: http_go_proto */
	libpb "github.com/GoogleCloudPlatform/healthcare-data-harmonization/mapping_engine/proto" /* copybara-comment: library_go_proto */
	mappb "github.com/GoogleCloudPlatform/healthcare-data-harmonization/mapping_engine/proto" /* copybara-comment: mapping_go_proto */

)

// Transformer contains projectors initialized for a specific config, and receiver methods to
// perform transformations.
type Transformer struct {
	Registry                *types.Registry
	dataHarmonizationConfig *dhpb.DataHarmonizationConfig
	mappingConfig           *mappb.MappingConfig
	parallel                bool
}

// TransformationConfigs contains metadata used during transformation.
type TransformationConfigs struct {
	LogTrace     bool
	SkipBundling bool
}

// Options for initializing Data Harmonization transform library
type Options struct {
	// CloudFunctions enables support for cloud functions within the transform library.
	CloudFunctions bool

	// FetchConfigs enables support for fetch configurations wtihin the transform library.
	FetchConfigs bool

	// Parallel enables support for parallelization within the transform library.
	Parallel bool

	// GCSClient is a client for downloading from GCS. If unset, the default third party GCS client will be used.
	GCSClient gcsutil.StorageClient
}

// Option is a setter function for Options.
type Option func(*Options)

// CloudFunctions initializes the CloudFunctions transform option.
func CloudFunctions(enabledCloudFunctions bool) Option {
	return func(args *Options) {
		args.CloudFunctions = enabledCloudFunctions
	}
}

// FetchConfigs initializes the FetchConfigs transform option.
func FetchConfigs(enableFetchConfig bool) Option {
	return func(args *Options) {
		args.FetchConfigs = enableFetchConfig
	}
}

// Parallel initializes the Parallel transform option.
func Parallel(enableParallel bool) Option {
	return func(args *Options) {
		args.Parallel = enableParallel
	}
}

// GCSClient sets the GCSClient in the transform option.
func GCSClient(c gcsutil.StorageClient) Option {
	return func(args *Options) {
		args.GCSClient = c
	}
}

// NewTransformer creates and initializes a transformer.
func NewTransformer(ctx context.Context, config *dhpb.DataHarmonizationConfig, setters ...Option) (*Transformer, error) {
	t := &Transformer{
		Registry:                types.NewRegistry(),
		dataHarmonizationConfig: config,
	}

	if err := builtins.RegisterAll(t.Registry); err != nil {
		return nil, err
	}

	options := &Options{}
	for _, setter := range setters {
		setter(options)
	}

	t.parallel = options.Parallel

	gcsutil.InitializeClient(options.GCSClient)

	if hc := config.GetHarmonizationConfig(); hc != nil {
		if err := harmonizecode.LoadCodeHarmonizationProjectors(t.Registry, hc); err != nil {
			return nil, err
		}
	}

	if uc := config.GetUnitHarmonizationConfig(); uc != nil {
		if err := harmonizeunit.LoadUnitHarmonizationProjectors(t.Registry, uc); err != nil {
			return nil, err
		}
	}

	mpc, err := t.LoadMappingConfig(config)
	if err != nil {
		return nil, err
	}
	t.mappingConfig = mpc

	if err := t.LoadProjectors(mpc.GetProjector()); err != nil {
		return nil, err
	}

	// Load the library configurations.
	for _, lc := range config.GetLibraryConfig() {
		if err := t.LoadProjectors(lc.Projector); err != nil {
			return nil, err
		}

		if options.CloudFunctions {
			if err := cloudfunction.LoadCloudFunctionProjectors(t.Registry, lc.CloudFunction); err != nil {
				return nil, err
			}
		} else {
			if len(lc.CloudFunction) != 0 {
				return nil, &invalidCloudFunctionProjectorError{lc: lc}
			}
		}

		if err := auth.LoadServerConfigs(lc.Servers); err != nil {
			return nil, err
		}

		if options.FetchConfigs {
			if err := fetch.LoadFetchProjectors(context.Background(), t.Registry, lc.HttpQuery); err != nil {
				return nil, err
			}
		} else {
			if len(lc.HttpQuery) != 0 {
				return nil, &invalidFetchProjectorError{lc: lc}
			}
		}

		for _, lib := range lc.GetUserLibraries() {
			mpc, err := loadMappingConfig(lib.GetPath(), lib.GetType())
			if err != nil {
				return nil, err
			}
			// Custom library configs should only have projectors defined, all else is ignored.
			if err := t.LoadProjectors(mpc.GetProjector()); err != nil {
				return nil, err
			}
		}
	}

	return t, nil
}

type invalidCloudFunctionProjectorError struct {
	lc *libpb.LibraryConfig
}

type invalidFetchProjectorError struct {
	lc *libpb.LibraryConfig
}

func (e invalidCloudFunctionProjectorError) Error() string {
	var names []string
	for _, cf := range e.lc.CloudFunction {
		names = append(names, cf.Name)
	}
	return fmt.Sprintf("attempting to use disabled Cloud Function projectors feature with projectors: %v", names)
}

func (e invalidFetchProjectorError) Error() string {
	var names []string
	for _, cf := range e.lc.HttpQuery {
		names = append(names, cf.Name)
	}
	return fmt.Sprintf("attempting to use disabled Fetch projectors feature with projectors: %v", names)
}

// Project is a convenience function to call a single projector out of context.
func (t *Transformer) Project(projector string, args ...jsonutil.JSONMetaNode) (res jsonutil.JSONToken, err error) {
	pctx := types.NewContext(t.Registry)

	defer errors.RecoverWithTrace(pctx.Trace, "Project", func(e error) {
		err = e
	})

	proj, err := t.Registry.FindProjector(projector)
	if err != nil {
		return nil, err
	}

	res, err = proj(args, pctx)
	return
}

// LoadMappingConfig loads the mapping config inline or from a GCS path.
func (t *Transformer) LoadMappingConfig(config *dhpb.DataHarmonizationConfig) (*mappb.MappingConfig, error) {
	mpc := &mappb.MappingConfig{}
	if config.GetDeprecateStructureMappingConfig() != nil && config.GetStructureMappingConfig() != nil {
		return nil, fmt.Errorf("only one of DeprecateStructureMappingConfig or StructureMappingConfig can be filled")
	}
	if config.GetDeprecateStructureMappingConfig() != nil {
		return config.GetDeprecateStructureMappingConfig(), nil
	}
	if sm := config.GetStructureMappingConfig(); sm != nil {
		switch mapping := config.GetStructureMappingConfig().Mapping.(type) {
		case *hapb.StructureMappingConfig_MappingConfig:
			return mapping.MappingConfig, nil
		case *hapb.StructureMappingConfig_MappingPathConfig:
			return loadMappingConfig(mapping.MappingPathConfig.MappingConfigPath, mapping.MappingPathConfig.MappingType)
		case *hapb.StructureMappingConfig_MappingLanguageString:
			return transpiler.Transpile(mapping.MappingLanguageString)
		default:
			return nil, fmt.Errorf("unsupported structure mapping config type: %v", mapping)
		}
	}
	return mpc, nil
}

// LoadProjectors registers all given projectors.
func (t *Transformer) LoadProjectors(projectors []*mappb.ProjectorDefinition) error {
	for _, pd := range projectors {
		p := projector.FromDef(pd, t.parallel)

		if err := t.Registry.RegisterProjector(pd.Name, p); err != nil {
			return fmt.Errorf("error registering projector %s: %v", pd.Name, err)
		}
	}
	return nil
}

// Transform converts the json tree using the specified config.
func (t *Transformer) Transform(in *jsonutil.JSONContainer, tconfig TransformationConfigs) (res jsonutil.JSONToken, err error) {
	pctx := types.NewContext(t.Registry)
	defer errors.RecoverWithTrace(pctx.Trace, "Transform", func(e error) {
		err = e
	})

	pctx.Variables.Push()

	inn, err := jsonutil.TokenToNode(*in)
	if err != nil {
		return nil, fmt.Errorf("input was invalid: %v", err)
	}
	args := []jsonutil.JSONMetaNode{inn}

	if err := mapping.ProcessMappings(t.mappingConfig.RootMapping, "root", args, &pctx.Output, pctx, t.parallel); err != nil {
		return nil, pctx.Trace.AsError(err)
	}

	result, err := postprocess.Process(pctx, t.mappingConfig, tconfig.SkipBundling, t.parallel)
	if err != nil {
		return nil, pctx.Trace.AsError(err)
	}

	if tconfig.LogTrace {
		log.Printf(pctx.Trace.String())
	}

	return result, nil
}

// JSONtoJSON converts the byte array (JSON format) using the specified config.
// TODO: Refactor to use json.RawMessage instead of []byte.
func (t *Transformer) JSONtoJSON(in []byte, tconfig TransformationConfigs) ([]byte, error) {
	ji := &jsonutil.JSONContainer{}
	if err := ji.UnmarshalJSON(in); err != nil {
		return nil, err
	}

	res, err := t.Transform(ji, tconfig)
	if err != nil {
		return nil, err
	}
	return json.Marshal(res)
}

// RegisterProjector adds the given Projector to this transformer's registry.
func (t *Transformer) RegisterProjector(name string, proj types.Projector) error {
	return t.Registry.RegisterProjector(name, proj)
}

// HasPostProcessProjector returns true iff a post process projector is set.
func (t *Transformer) HasPostProcessProjector() bool {
	return t.mappingConfig.GetPostProcessProjectorDefinition() != nil || t.mappingConfig.GetPostProcessProjectorName() != ""
}

// loadMappingConfig loads a mapping config from GCS.
func loadMappingConfig(loc *httppb.Location, typ hapb.MappingType) (*mappb.MappingConfig, error) {
	mpc := &mappb.MappingConfig{}
	switch l := loc.Location.(type) {
	case *httppb.Location_GcsLocation:
		data, err := gcsutil.ReadFromGcs(context.Background(), l.GcsLocation)
		if err != nil {
			return nil, fmt.Errorf("failed to read MappingConfig from GCS, %v", err)
		}
		switch typ {
		case hapb.MappingType_RAW_PROTO:
			if err := proto.UnmarshalText(string(data), mpc); err != nil {
				return nil, err
			}
		case hapb.MappingType_MAPPING_LANGUAGE:
			lmpc, err := transpiler.Transpile(string(data))
			if err != nil {
				return nil, err
			}
			mpc = lmpc
		default:
			return nil, fmt.Errorf("invalid mapping config type %v", typ)
		}
		return mpc, nil
	case *httppb.Location_UrlPath:
		return nil, fmt.Errorf("loading from remote path %s is unsupported", l.UrlPath)
	case *httppb.Location_LocalPath:
		return nil, fmt.Errorf("loading from local path %s is unsupported", l.LocalPath)
	default:
		return nil, fmt.Errorf("location type %s is unsupported", l)
	}
}
