import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";

import { json, Registration } from "./Registration";

function response(body: string, ok = true, status = ok ? 200 : 400): Response {
  return { ok, status, text: async () => body } as Response;
}

async function fillRequiredFields() {
  await userEvent.type(screen.getByLabelText("Nome completo"), "Maria Silva");
  await userEvent.type(screen.getByLabelText("CPF"), "52998224725");
  await userEvent.type(screen.getByLabelText("E-mail"), "maria@senai.br");
  await userEvent.type(screen.getByLabelText("Senha inicial"), "senha12345");
  await userEvent.type(screen.getByLabelText("ID da instituição"), "1");
  await userEvent.type(screen.getByLabelText("RM"), "4321");
  await userEvent.type(screen.getByLabelText("Curso"), "ADS");
  await userEvent.click(screen.getByText(/Autorizo o cadastro/));
}

describe("json", () => {
  test("retorna JSON válido em resposta de sucesso", async () => {
    await expect(json<{ id: number }>(response('{"id":7}'))).resolves.toEqual({ id: 7 });
  });

  test("prioriza mensagem e erros do backend", async () => {
    await expect(json(response('{"message":"CPF já cadastrado"}', false))).rejects.toThrow("CPF já cadastrado");
    await expect(json(response('{"errors":{"cpf":"CPF inválido"}}', false))).rejects.toThrow("CPF inválido");
  });

  test("trata resposta textual ou JSON inválido", async () => {
    await expect(json(response("falha do proxy", false))).rejects.toThrow("falha do proxy");
    await expect(json(response("conteúdo inesperado"))).rejects.toThrow("resposta inválida");
  });
});

describe("Registration", () => {
  beforeEach(() => {
    global.fetch = jest.fn();
  });

  test("exige foto antes de acessar os serviços", () => {
    render(<Registration token="jwt" onUnauthorized={jest.fn()} />);
    fireEvent.submit(screen.getByRole("button", { name: /Validar e cadastrar/ }).closest("form")!);
    expect(screen.getByRole("status")).toHaveTextContent("Adicione uma foto frontal");
    expect(global.fetch).not.toHaveBeenCalled();
  });

  test("alterna campos de aluno para funcionário", async () => {
    render(<Registration token="jwt" onUnauthorized={jest.fn()} />);
    expect(screen.getByLabelText("RM")).toBeRequired();
    await userEvent.selectOptions(screen.getByLabelText("Perfil"), "EMPLOYEE");
    expect(screen.queryByLabelText("RM")).not.toBeInTheDocument();
    expect(screen.getByLabelText("Departamento")).toBeRequired();
  });

  test("gera embedding, cadastra aluno e limpa o formulário", async () => {
    const embedding = Array.from({ length: 512 }, (_, index) => index / 512);
    (global.fetch as jest.Mock)
      .mockResolvedValueOnce(response(JSON.stringify({ embedding, faceModel: "DEEPFACE_ARCFACE_OPENCV_V1" })))
      .mockResolvedValueOnce(response(JSON.stringify({ name: "Maria Silva", userId: 8, enrollmentId: 10 })));
    render(<Registration token="jwt-admin" onUnauthorized={jest.fn()} />);
    await fillRequiredFields();
    const photo = new File(["imagem"], "maria.jpg", { type: "image/jpeg" });
    await userEvent.upload(screen.getByLabelText(/Selecionar foto frontal/), photo);
    await userEvent.click(screen.getByRole("button", { name: /Validar e cadastrar/ }));

    expect(await screen.findByRole("status")).toHaveTextContent("Maria Silva cadastrado com sucesso");
    expect(global.fetch).toHaveBeenCalledTimes(2);
    const registrationCall = (global.fetch as jest.Mock).mock.calls[1];
    const options = registrationCall[1] as RequestInit;
    const payload = JSON.parse(String(options.body));
    expect(registrationCall[0]).toBe("/api/registrations");
    expect((options.headers as Record<string, string>).Authorization).toBe("Bearer jwt-admin");
    expect(payload.user).toMatchObject({ userType: "STUDENT", registrationNumber: 4321, course: "ADS", institutionId: 1 });
    expect(payload.faceEmbedding).toHaveLength(512);
    expect(screen.getByLabelText("Nome completo")).toHaveValue("");
  });

  test("recusa embedding facial com tamanho inválido", async () => {
    (global.fetch as jest.Mock).mockResolvedValue(response(JSON.stringify({ embedding: [1, 2], faceModel: "ArcFace" })));
    render(<Registration token="jwt" onUnauthorized={jest.fn()} />);
    await fillRequiredFields();
    await userEvent.upload(screen.getByLabelText(/Selecionar foto frontal/), new File(["x"], "foto.jpg", { type: "image/jpeg" }));
    await userEvent.click(screen.getByRole("button", { name: /Validar e cadastrar/ }));
    expect(await screen.findByRole("status")).toHaveTextContent("não gerou uma identificação facial válida");
    expect(global.fetch).toHaveBeenCalledTimes(1);
  });

  test("encerra sessão ao receber 401", async () => {
    const embedding = Array(512).fill(0.1);
    (global.fetch as jest.Mock)
      .mockResolvedValueOnce(response(JSON.stringify({ embedding, faceModel: "ArcFace" })))
      .mockResolvedValueOnce(response("{}", false, 401));
    const onUnauthorized = jest.fn();
    render(<Registration token="jwt" onUnauthorized={onUnauthorized} />);
    await fillRequiredFields();
    await userEvent.upload(screen.getByLabelText(/Selecionar foto frontal/), new File(["x"], "foto.jpg", { type: "image/jpeg" }));
    await userEvent.click(screen.getByRole("button", { name: /Validar e cadastrar/ }));
    await waitFor(() => expect(onUnauthorized).toHaveBeenCalledTimes(1));
    expect(screen.getByRole("status")).toHaveTextContent("sessão administrativa expirou");
  });
});
