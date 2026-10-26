import { cpfDigits, formatCpf, isValidCpf } from "./cpf";

describe("CPF", () => {
  test.each([
    ["52998224725", "529.982.247-25"],
    ["529.982.247-25", "529.982.247-25"],
    ["123", "123"],
    ["1234567", "123.456.7"],
  ])("formata %s", (input, expected) => expect(formatCpf(input)).toBe(expected));

  test("remove pontuação e limita a onze números", () => {
    expect(cpfDigits("529.982.247-25 texto 99")).toBe("52998224725");
  });

  test.each(["52998224725", "168.995.350-09", "11144477735"])("aceita CPF válido %s", (cpf) => {
    expect(isValidCpf(cpf)).toBe(true);
  });

  test.each(["", "123", "52998224724", "00000000000", "111.111.111-11"])("recusa CPF inválido %s", (cpf) => {
    expect(isValidCpf(cpf)).toBe(false);
  });
});
