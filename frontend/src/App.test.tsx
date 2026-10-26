import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";

import { App } from "./App";

jest.mock("./features/identification/Identification", () => ({
  Identification: () => <div>identificação ativa</div>,
}));

jest.mock("./features/registration/Registration", () => ({
  Registration: ({ token, onUnauthorized }: { token: string; onUnauthorized: () => void }) => (
    <div>cadastro com {token}<button onClick={onUnauthorized}>expirar</button></div>
  ),
}));

function response(body: unknown, ok = true): Response {
  return { ok, json: async () => body } as Response;
}

describe("App", () => {
  beforeEach(() => {
    history.pushState({}, "", "/");
    global.fetch = jest.fn();
  });

  test("abre somente o terminal na rota pública", () => {
    render(<App />);
    expect(screen.getByText("Terminal de acesso")).toBeInTheDocument();
    expect(screen.getByText("identificação ativa")).toBeInTheDocument();
    expect(screen.queryByText("Acesso administrativo")).not.toBeInTheDocument();
  });

  test("abre login administrativo na rota protegida", () => {
    history.pushState({}, "", "/admin");
    render(<App />);
    expect(screen.getByRole("heading", { name: "Acesso administrativo" })).toBeInTheDocument();
  });

  test("autentica ADMIN, guarda token e permite sair", async () => {
    history.pushState({}, "", "/admin");
    (global.fetch as jest.Mock).mockResolvedValue(response({ token: "jwt-valido", userType: "ADMIN" }));
    render(<App />);
    await userEvent.type(screen.getByLabelText("E-mail"), "admin@senai.br");
    await userEvent.type(screen.getByLabelText("Senha"), "admin12345");
    fireEvent.submit(screen.getByRole("button", { name: "Entrar" }).closest("form")!);
    expect(await screen.findByText("cadastro com jwt-valido")).toBeInTheDocument();
    expect(sessionStorage.getItem("adaptface.admin.token")).toBe("jwt-valido");
    await userEvent.click(screen.getByRole("button", { name: "Sair" }));
    expect(await screen.findByRole("heading", { name: "Acesso administrativo" })).toBeInTheDocument();
    expect(sessionStorage.getItem("adaptface.admin.token")).toBeNull();
  });

  test("recusa conta que não seja ADMIN", async () => {
    history.pushState({}, "", "/admin");
    (global.fetch as jest.Mock).mockResolvedValue(response({ token: "jwt", userType: "STUDENT" }));
    render(<App />);
    await userEvent.type(screen.getByLabelText("E-mail"), "aluno@senai.br");
    await userEvent.type(screen.getByLabelText("Senha"), "12345678");
    await userEvent.click(screen.getByRole("button", { name: "Entrar" }));
    expect(await screen.findByRole("alert")).toHaveTextContent("não possui acesso administrativo");
    expect(sessionStorage.getItem("adaptface.admin.token")).toBeNull();
  });

  test("exibe erro retornado pelo backend", async () => {
    history.pushState({}, "", "/admin");
    (global.fetch as jest.Mock).mockResolvedValue(response({ message: "Credenciais inválidas" }, false));
    render(<App />);
    await userEvent.type(screen.getByLabelText("E-mail"), "admin@senai.br");
    await userEvent.type(screen.getByLabelText("Senha"), "incorreta");
    await userEvent.click(screen.getByRole("button", { name: "Entrar" }));
    await waitFor(() => expect(screen.getByRole("alert")).toHaveTextContent("Credenciais inválidas"));
  });

  test("remove sessão quando o cadastro informa acesso expirado", async () => {
    history.pushState({}, "", "/admin");
    sessionStorage.setItem("adaptface.admin.token", "jwt-antigo");
    render(<App />);
    await userEvent.click(screen.getByRole("button", { name: "expirar" }));
    expect(await screen.findByRole("heading", { name: "Acesso administrativo" })).toBeInTheDocument();
  });
});
