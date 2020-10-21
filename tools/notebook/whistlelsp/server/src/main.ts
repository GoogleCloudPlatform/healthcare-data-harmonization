import {Server} from './server';
// TODO(b/169868513): Keep the function.txt updated with the Whistle built-in
// functions.
/**
 * The input functions file is default to server/function.txt. To use another
 * functions file, put the file in server/ and put the file name as the first
 * parameter of the Server(), or direct to the file location directly by using
 * Server('<file name>', '<relative path from the compiled server.js file at
 * out/server/src/server.js to the directory containing the functions file>').
 */
const server = new Server();
server.run();
