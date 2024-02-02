/*
 * Copyright 2021 Google LLC.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.cloud.verticals.foundations.dataharmonization.lsp;

import com.google.cloud.verticals.foundations.dataharmonization.lsp.exception.CloseConnectionException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.eclipse.lsp4j.jsonrpc.Launcher;
import org.eclipse.lsp4j.launch.LSPLauncher;
import org.eclipse.lsp4j.services.LanguageClient;

/**
 * Entry point for the server which acts as a wrapper for the {@link Launcher} utility class.
 * Responsible for initializing the server and creating connections to clients.
 */
public class ServerLauncher {

  private static final Logger logger = Logger.getLogger(ServerLauncher.class.getName());
  public ServerLauncher() {}

  public static void main(String[] args) throws ExecutionException, InterruptedException {
    try {
    startServer(System.in, System.out);
    } catch (CloseConnectionException e) {
      System.exit(e.getStatusCode());
    }
  }

  private static void startServer(InputStream in, OutputStream out)
      throws ExecutionException, InterruptedException {

    LSPServer wstlLanguageServer = new LSPServer();

    Launcher<LanguageClient> launcher =
        LSPLauncher.createServerLauncher(wstlLanguageServer, in, out);
    LanguageClient client = launcher.getRemoteProxy();
    logger.log(Level.INFO, client.toString());

    wstlLanguageServer.connect(client);
    Future<?> startListening = launcher.startListening();
    startListening.get();
  }
}
