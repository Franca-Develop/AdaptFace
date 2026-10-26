import { act, render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";

import { Identification } from "./Identification";

const stop = jest.fn();
const stream = { getTracks: () => [{ stop }] } as unknown as MediaStream;

function response(body: unknown, ok = true): Response {
  return { ok, json: async () => body } as Response;
}

async function tick(milliseconds = 900) {
  await act(async () => {
    jest.advanceTimersByTime(milliseconds);
    await Promise.resolve();
    await Promise.resolve();
  });
}

describe("Identification", () => {
  beforeEach(() => {
    jest.useFakeTimers();
    stop.mockClear();
    Object.defineProperty(navigator, "mediaDevices", {
      configurable: true,
      value: { getUserMedia: jest.fn().mockResolvedValue(stream) },
    });
    global.fetch = jest.fn();
  });

  afterEach(() => jest.useRealTimers());

  test("abre a câmera automaticamente e encerra a mídia ao desmontar", async () => {
    const view = render(<Identification />);
    expect(await screen.findByRole("heading", { name: "Câmera ativada" })).toBeInTheDocument();
    expect(navigator.mediaDevices.getUserMedia).toHaveBeenCalledWith({
      video: { facingMode: "user", width: 1280, height: 720 }, audio: false,
    });
    view.unmount();
    expect(stop).toHaveBeenCalled();
  });

  test("orienta quando a permissão da câmera falha e permite tentar novamente", async () => {
    (navigator.mediaDevices.getUserMedia as jest.Mock).mockRejectedValue(new Error("negado"));
    render(<Identification />);
    expect(await screen.findByRole("heading", { name: "Verificação indisponível" })).toBeInTheDocument();
    expect(screen.getByText(/Permita o acesso à câmera/)).toBeInTheDocument();
    await userEvent.setup({ advanceTimers: jest.advanceTimersByTime }).click(screen.getByRole("button", { name: /Tentar novamente/ }));
    expect(navigator.mediaDevices.getUserMedia).toHaveBeenCalledTimes(2);
  });

  test("mostra progresso e conclui uma identificação autorizada", async () => {
    (global.fetch as jest.Mock)
      .mockResolvedValueOnce(response({ status: "scanning", message: "Lendo", progress: 50 }))
      .mockResolvedValueOnce(response({ status: "granted", name: "João Martins", message: "Acesso liberado" }));
    render(<Identification />);
    await screen.findByRole("heading", { name: "Câmera ativada" });
    await tick();
    expect(screen.getByRole("progressbar")).toHaveAttribute("value", "50");
    expect(screen.getByText("Lendo seu rosto...")).toBeInTheDocument();
    await tick();
    expect(await screen.findByRole("heading", { name: "Identificação concluída" })).toBeInTheDocument();
    expect(screen.getAllByText("João Martins").length).toBeGreaterThan(0);
    expect(global.fetch).toHaveBeenCalledTimes(2);
    const payload = JSON.parse((global.fetch as jest.Mock).mock.calls[0][1].body);
    expect(payload.sessionId).toBe("123e4567-e89b-12d3-a456-426614174000");
    expect(payload.image).toMatch(/^data:image\/jpeg/);
  });

  test("mantém uma orientação por três segundos enquanto continua analisando", async () => {
    (global.fetch as jest.Mock)
      .mockResolvedValueOnce(response({ status: "guidance", message: "Aproxime-se", detail: "Mantenha o rosto no guia" }))
      .mockResolvedValue(response({ status: "scanning", message: "Lendo", progress: 75 }));
    render(<Identification />);
    await screen.findByRole("heading", { name: "Câmera ativada" });
    await tick();
    expect(screen.getByText("Mantenha o rosto no guia")).toBeInTheDocument();
    await tick(1800);
    expect(screen.getByText("Mantenha o rosto no guia")).toBeInTheDocument();
    await tick(1800);
    expect(screen.getByText("Lendo seu rosto...")).toBeInTheDocument();
  });

  test.each([
    ["denied", "Rosto não reconhecido", "Não identificado"],
    ["spoof", "Prova de vida recusada", "Apresentação bloqueada"],
  ])("encerra análise para retorno %s", async (status, heading, overlay) => {
    (global.fetch as jest.Mock).mockResolvedValue(response({ status, message: heading, detail: "Resultado final" }));
    render(<Identification />);
    await screen.findByRole("heading", { name: "Câmera ativada" });
    await tick();
    expect(await screen.findByRole("heading", { name: heading })).toBeInTheDocument();
    expect(screen.getByText(overlay)).toBeInTheDocument();
    await tick(1800);
    expect(global.fetch).toHaveBeenCalledTimes(1);
  });

  test("exibe falha HTTP e reinicia a câmera", async () => {
    (global.fetch as jest.Mock).mockResolvedValue(response({ status: "error", message: "Visão indisponível" }, false));
    render(<Identification />);
    await screen.findByRole("heading", { name: "Câmera ativada" });
    await tick();
    expect(await screen.findByText("Visão indisponível")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /Tentar novamente/ })).toBeInTheDocument();
  });
});
