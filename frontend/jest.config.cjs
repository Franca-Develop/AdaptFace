module.exports = {
  preset: "ts-jest/presets/default-esm",
  testEnvironment: "jsdom",
  cacheDirectory: "<rootDir>/.jest-cache",
  coverageDirectory: "<rootDir>/coverage",
  extensionsToTreatAsEsm: [".ts", ".tsx"],
  setupFilesAfterEnv: ["<rootDir>/src/test/setup.ts"],
  testMatch: ["<rootDir>/src/**/*.test.ts?(x)"],
  transform: {
    "^.+\\.tsx?$": ["ts-jest", { useESM: true, tsconfig: "<rootDir>/tsconfig.test.json" }],
  },
  collectCoverageFrom: [
    "src/**/*.{ts,tsx}",
    "!src/main.tsx",
    "!src/**/*.d.ts",
  ],
  coverageThreshold: {
    global: { branches: 70, functions: 75, lines: 80, statements: 80 },
  },
};
