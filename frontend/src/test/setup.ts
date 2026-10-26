import "@testing-library/jest-dom";
import { cleanup } from "@testing-library/react";

Object.defineProperty(globalThis, "crypto", {
  configurable: true,
  value: { randomUUID: jest.fn(() => "123e4567-e89b-12d3-a456-426614174000") },
});

Object.defineProperty(HTMLMediaElement.prototype, "play", {
  configurable: true,
  value: jest.fn().mockResolvedValue(undefined),
});

Object.defineProperties(HTMLVideoElement.prototype, {
  readyState: { configurable: true, get: () => 4 },
  videoWidth: { configurable: true, get: () => 1280 },
  videoHeight: { configurable: true, get: () => 720 },
});

const canvasContext = {
  translate: jest.fn(),
  scale: jest.fn(),
  drawImage: jest.fn(),
};

Object.defineProperty(HTMLCanvasElement.prototype, "getContext", {
  configurable: true,
  value: jest.fn(() => canvasContext),
});

Object.defineProperty(HTMLCanvasElement.prototype, "toDataURL", {
  configurable: true,
  value: jest.fn(() => "data:image/jpeg;base64,dGVzdGU="),
});

afterEach(() => {
  cleanup();
  jest.restoreAllMocks();
  sessionStorage.clear();
});
