const {writeFile, existsSync, mkdirSync, readFileSync} = require('fs');
const {join} = require('path');
const PromptSync = require('prompt-sync');

const pathServer = join(__dirname, 'bin', 'wstl-language-server');

const wstlSpec = {
  // Note that this is not the version of the language server. Please keep the
  // version to be 2 for the spec to work.
  version: 2,
  argv: [pathServer, '--stdio'],
  languages: ['wstl'],
  display_name: 'wstl-language-server',
  mime_types: ['text/x-wstl']
};

const dirJupyter = join('/', 'usr', 'local', 'etc', 'jupyter');
if (!existsSync(dirJupyter)) {
  try {
    mkdirSync(dirJupyter);
  } catch (err) {
    if (err.code === 'ENOENT') {
      throw {
        name: 'UnableToCreateDirectoryError',
        message: `Unable to create the directory ${
            dirJupyter}. Please enable the write permission on ${
            dirJupyter} or create ${
            dirJupyter} manually and run 'node config-wstl-spec' to generate the spec file.`
      };
    } else {
      throw err;
    }
  }
}

const filePath = join(dirJupyter, 'jupyter_notebook_config.json');
let writeBack = {};
if (!existsSync(filePath)) {
  writeBack = {
    LanguageServerManager:
        {autodetect: true, language_servers: {'wstl-language-server': wstlSpec}}
  };
} else {
  let conFile = '';
  try {
    conFile = readFileSync(filePath, 'utf-8');
  } catch (err) {
    if (err.code === 'ENOENT') {
      throw {
        name: 'FileReadError',
        message: `Unable to read the file ${filePath}.`
      };
    } else {
      throw err;
    }
  }
  const configs = JSON.parse(conFile);
  writeBack = mergerSpec(configs, wstlSpec);
}

writeFile(filePath, JSON.stringify(writeBack, null, 2) + '\n', function(err) {
  if (err) throw err;
  console.log(`Spec file is ready at ${filePath}`);
});

/**
 * mergeSpec merges the newSpec into orig and returns the result.
 * @param {Object!} orig The JSON object to merge the newSpec into.
 * @param {Object!} newSpec The JSON object to merge into orig.
 * @returns {Object!} The merged result as a JSON object.
 */
function mergerSpec(orig, newSpec) {
  if (orig.LanguageServerManager) {
    if (orig.LanguageServerManager.language_servers) {
      let servers = orig.LanguageServerManager.language_servers;
      if (Object.keys(servers).includes('wstl_language_server') |
          Object.keys(servers).includes('wstl-language-server')) {
        const prompt = PromptSync();
        let ans = prompt(`wstl language server spec already exists in ${
            filePath}. Do you want to overwrite it?[y/n]`);
        while (!['yes', 'y', 'no', 'n'].includes(ans)) {
          ans = prompt('please make a valid selection [y/n]');
        }
        if (ans === 'y' | ans === 'yes') {
          servers['wstl-language-server'] = newSpec;
          if (Object.keys(servers).includes('wstl_language_server')) {
            delete servers['wstl_language_server'];
          }
        }
      } else {
        servers['wstl-language-server'] = newSpec;
      }
    } else {
      orig.LanguageServerManager.language_servers = {
        'wstl-language-server': newSpec
      };
    }
  } else {
    orig.LanguageServerManager = {
      autodetect: true,
      language_servers: {'wstl-language-server': newSpec}
    };
  }
  return orig;
}
