const fs = require("node:fs");
const path = require("node:path");

const tempDirectory = path.resolve(__dirname, "..", ".test-tmp");
fs.mkdirSync(tempDirectory, { recursive: true });
process.env.TEMP = tempDirectory;
process.env.TMP = tempDirectory;
process.env.TMPDIR = tempDirectory;

require("jest-cli/bin/jest");
