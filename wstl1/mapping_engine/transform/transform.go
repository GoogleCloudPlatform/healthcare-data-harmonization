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
	"io/ioutil"

	"google.golang.org/protobuf/encoding/prototext" /* copybara-comment: prototext */

	"github.com/GoogleCloudPlatform/healthcare-data-harmonization/mapping_engine/auth" /* copybara-comment: auth */
	"github.com/GoogleCloudPlatform/healthcare-data-harmonization/mapping_engine/cloudfunction" /* copybara-comment: cloudfunction */
	"github.com/GoogleCloudPlatform/healthcare-data-harmonization/mapping_engine/errors" /* copybara-comment: errors */
	"github.com/GoogleCloudPlatform/healthcare-data-harmonization/mapping_engine/fetch" /* copybara-comment: fetch */
	"github.com/GoogleCloudPlatform/healthcare-data-harmonization/mapping_engine/harmonization/harmonizecode" /* copybara-comment: harmonizecode */
	"github.com/GoogleCloudPlatform/healthcare-data-harmonization/mapping_engine/harmonization/harmonizeunit" /* copybara-comment: harmonizeunit */
	"github.com/GoogleCloudPlatform/healthcare-data-harmonization/mapping_engine/mapping" /* copybara-comment: mapping */
	"github.com/GoogleCloudPlatform/healthcare-data-harmonization/mapping_engine/postprocess" /* copybara-comment: postprocess */
	"github.com/GoogleCloudPlatform/healthcare-data-harmonization/mapping_engine/projector" /* copybara-comment: projector */
	"github.com/GoogleCloudPlatform/healthcare-data-harmonization/mapping_engine/types/register_all" /* copybara-comment: registerall */
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

// Transformer defines an interface to perform transformations.
type Transformer interface {
	// Transform transforms given JSONToken (parsed JSON) into a target JSONToken using the
	// config.
	Transform(jsonutil.JSONToken) (jsonutil.JSONToken, error)
	// JSONtoJSON transforms given raw JSON into a target raw JSON using the config.
	JSONtoJSON(json.RawMessage) (json.RawMessage, error)

	// ParseJSON parses given raw JSON into a JSONToken.
	ParseJSON(json.RawMessage) (jsonutil.JSONToken, error)

	// LoadProjectors registers all given projectors in the config.
	LoadProjectors([]*mappb.ProjectorDefinition) error

	// Registry returns the registry used in Transformer.
	Registry() *types.Registry

	// HasPostProcessProjector returns true iff a post process projector is set.
	HasPostProcessProjector() bool
}

// DefaultTransformer contains projectors initialized for a specific config, and receiver methods
// to perform transformations.
type DefaultTransformer struct {
	registry                *types.Registry
	dataHarmonizationConfig *dhpb.DataHarmonizationConfig
	mappingConfig           *mappb.MappingConfig
	transformationConfig    TransformationConfig
}

// TransformationConfig contains metadata used during transformation.
type TransformationConfig struct {
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

// NewTransformer creates and initializes a transformer, and returns a new DefaultTransformer by
// default.
func NewTransformer(ctx context.Context, config *dhpb.DataHarmonizationConfig, tconfig TransformationConfig, setters ...Option) (Transformer, error) {
	return NewDefaultTransformer(ctx, config, tconfig, setters...)
}

// NewDefaultTransformer creates and initializes a default transformer.
func NewDefaultTransformer(ctx context.Context, config *dhpb.DataHarmonizationConfig, tconfig TransformationConfig, setters ...Option) (*DefaultTransformer, error) {
	t := &DefaultTransformer{
		registry:                types.NewRegistry(),
		dataHarmonizationConfig: config,
		transformationConfig:    tconfig,
	}

	if err := registerall.RegisterAll(t.registry); err != nil {
		return nil, err
	}

	options := &Options{}
	for _, setter := range setters {
		setter(options)
	}

	gcsutil.InitializeClient(options.GCSClient)

	if hc := config.GetHarmonizationConfig(); hc != nil {
		if err := harmonizecode.LoadCodeHarmonizationProjectors(t.registry, hc); err != nil {
			return nil, err
		}
	}

	if uc := config.GetUnitHarmonizationConfig(); uc != nil {
		if err := harmonizeunit.LoadUnitHarmonizationProjectors(t.registry, uc); err != nil {
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
			if err := cloudfunction.LoadCloudFunctionProjectors(t.registry, lc.CloudFunction); err != nil {
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
			if err := fetch.LoadFetchProjectors(context.Background(), t.registry, lc.HttpQuery); err != nil {
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
func (t *DefaultTransformer) Project(projector string, args ...jsonutil.JSONMetaNode) (res jsonutil.JSONToken, err error) {
	pctx := types.NewContext(t.registry)

	defer errors.Recover("Project", func(e error) {
		err = e
	})

	proj, err := t.registry.FindProjector(projector)
	if err != nil {
		return nil, err
	}

	res, err = proj(args, pctx)
	return
}

// LoadMappingConfig loads the mapping config inline or from a GCS path.
func (t *DefaultTransformer) LoadMappingConfig(config *dhpb.DataHarmonizationConfig) (*mappb.MappingConfig, error) {
	mpc := &mappb.MappingConfig{}
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
func (t *DefaultTransformer) LoadProjectors(projectors []*mappb.ProjectorDefinition) error {
	for _, pd := range projectors {
		p := projector.FromDef(pd, mapping.NewWhistler())

		if err := t.registry.RegisterProjector(pd.Name, p); err != nil {
			return fmt.Errorf("error registering projector %s: %v", pd.Name, err)
		}
	}
	return nil
}

// Transform converts the json tree using the specified config.
func (t *DefaultTransformer) Transform(in jsonutil.JSONToken) (res jsonutil.JSONToken, err error) {
	pctx := types.NewContext(t.registry)
	defer errors.Recover("Transform", func(e error) {
		err = e
	})

	pctx.Variables.Push()

	inn, err := jsonutil.TokenToNode(in)
	if err != nil {
		return nil, fmt.Errorf("input was invalid: %v", err)
	}
	args := []jsonutil.JSONMetaNode{inn}

	e := mapping.NewWhistler()
	if err := e.ProcessMappings(t.mappingConfig.RootMapping, "root", args, pctx.Output, pctx); err != nil {
		return nil, err
	}

	result, err := postprocess.Process(pctx, t.mappingConfig, t.transformationConfig.SkipBundling, e)
	if err != nil {
		return nil, err
	}

	return result, nil
}

// JSONtoJSON converts the byte array (JSON format) using the specified config.
func (t *DefaultTransformer) JSONtoJSON(in json.RawMessage) (json.RawMessage, error) {
	ji, err := t.ParseJSON(in)
	if err != nil {
		return nil, err
	}

	res, err := t.Transform(ji)
	if err != nil {
		return nil, err
	}
	return json.Marshal(res)
}

// ParseJSON parses the given JSON into a JSONToken.
func (t *DefaultTransformer) ParseJSON(in json.RawMessage) (jsonutil.JSONToken, error) {
	mc, err := jsonutil.UnmarshalJSON(in)
	if err != nil {
		return nil, err
	}
	return mc, nil
}

// Registry returns the registry in DefaultTransformer.
func (t *DefaultTransformer) Registry() *types.Registry {
	return t.registry
}

// RegisterProjector adds the given Projector to this transformer's registry.
func (t *DefaultTransformer) RegisterProjector(name string, proj types.Projector) error {
	return t.registry.RegisterProjector(name, proj)
}

// HasPostProcessProjector returns true iff a post process projector is set.
func (t *DefaultTransformer) HasPostProcessProjector() bool {
	return t.mappingConfig.GetPostProcessProjectorDefinition() != nil || t.mappingConfig.GetPostProcessProjectorName() != ""
}

// loadMappingConfig loads a mapping config from GCS.
func loadMappingConfig(loc *httppb.Location, typ hapb.MappingType) (*mappb.MappingConfig, error) {
	var data []byte
	switch l := loc.Location.(type) {
	case *httppb.Location_GcsLocation:
		d, err := gcsutil.ReadFromGcs(context.Background(), l.GcsLocation)
		if err != nil {
			return nil, fmt.Errorf("failed to read mapping config from GCS, %v", err)
		}
		data = d
	case *httppb.Location_LocalPath:
		d, err := ioutil.ReadFile(l.LocalPath)
		if err != nil {
			return nil, fmt.Errorf("failed to read library file with error %v", err)
		}
		data = d
	case *httppb.Location_UrlPath:
		return nil, fmt.Errorf("loading mappings from remote path %s is unsupported", l.UrlPath)
	default:
		return nil, fmt.Errorf("location type %T is unsupported", l)
	}

	mpc := &mappb.MappingConfig{}
	switch typ {
	case hapb.MappingType_RAW_PROTO:
		if err := prototext.Unmarshal(data, mpc); err != nil {
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
}
