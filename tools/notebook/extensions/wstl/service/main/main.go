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

// A gRPC server that transform json to json using the Whistle Mapping Language.
package main

import (
	"flag"
	"fmt"
	"log"
	"net"

	"github.com/GoogleCloudPlatform/healthcare-data-harmonization/tools/notebook/extensions/wstl/service/wstlserver" /* copybara-comment: wstlserver */
	"google.golang.org/grpc" /* copybara-comment: grpc */

	wsgrpc "github.com/GoogleCloudPlatform/healthcare-data-harmonization/tools/notebook/extensions/wstl/proto" /* copybara-comment: wstlservice_proto_grpc */
)

var (
	host = flag.String("host", "localhost", "Whistler service host")
	port = flag.Int("port", 50051, "Whistle service port")
)

func main() {

	flag.Parse()
	hostPort := fmt.Sprintf("%s:%d", *host, *port)
	listener, err := net.Listen("tcp", hostPort)
	if err != nil {
		log.Fatalf("Server failed to listen on %s due to error: %v", hostPort, err)
	}
	log.Printf("Server is now listening on: %s", hostPort)

	server := grpc.NewServer()
	// TODO : unify how the server is registered internally and externally.
	wsgrpc.RegisterWhistleServiceServer(server, wstlserver.NewWstlServiceServer())

	if err := server.Serve(listener); err != nil {
		log.Fatalf("failed to serve: %v", err)
	}
}
