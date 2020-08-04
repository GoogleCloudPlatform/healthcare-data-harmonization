module.exports = {
  preset: 'ts-jest/presets/js-with-babel',
  moduleFileExtensions: ['ts', 'tsx', 'js', 'jsx', 'json', 'node'],
  moduleNameMapper: {
    '\\.(css|less|sass|scss)$': 'identity-obj-proxy',
    '\\.(gif|ttf|eot|svg)$': '@jupyterlab/testutils/lib/jest-file-mock.js'
  },
  setupFiles: ['@jupyterlab/testutils/lib/jest-shim.js'],
  setupFilesAfterEnv: ['<rootDir>src/setupTests.ts'],
  collectCoverageFrom: ['src/**/*{ts,tsx}'],
  transformIgnorePatterns: ['/node_modules/(?!(@jupyterlab/.*)/)'],
  globals: {'ts-jest': {tsConfig: 'tsconfig.json'}},
  snapshotSerializers: ['enzyme-to-json/serializer'],
};
