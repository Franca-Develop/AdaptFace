import { FormEvent, useEffect, useState } from "react";
import { Identification } from "./features/identification/Identification";
import { Registration } from "./features/registration/Registration";
import { cpfDigits, formatCpf, isValidCpf } from "./utils/cpf";

type LoginResponse = { token: string; userType: "ADMIN" | "EMPLOYEE" | "STUDENT" };
type ApiError = { message?: string };

const ADMIN_TOKEN_KEY = "adaptface.admin.token";

function Brand() {
  return (
    <a className="brand" href="/" aria-label="Abrir reconhecimento facial">
      <span className="brand-name">SENAI</span>
      <span className="brand-separator" aria-hidden="true" />
      <span className="brand-message">EDUCAÇÃO<br />QUE TRANSFORMA</span>
    </a>
  );
}

function KioskApp() {
  return (
    <main className="app-shell kiosk-shell">
      <header className="topbar"><Brand /><span className="mode-label">Terminal de acesso</span></header>
      <Identification />
    </main>
  );
}

function AdminApp() {
  const [token, setToken] = useState(() => sessionStorage.getItem(ADMIN_TOKEN_KEY) ?? "");
  const [status, setStatus] = useState<"idle" | "sending" | "error">("idle");
  const [message, setMessage] = useState("");
  const [requiresSetup, setRequiresSetup] = useState(false);
  const [setupMode, setSetupMode] = useState(false);
  const [setupCpf, setSetupCpf] = useState("");

  useEffect(() => {
    if (token) return;
    fetch("/api/auth/setup-status")
      .then(async (response) => response.ok ? response.json() as Promise<{ requiresSetup: boolean }> : null)
      .then((data) => setRequiresSetup(data?.requiresSetup === true))
      .catch(() => setRequiresSetup(false));
  }, [token]);

  function authorizeAdmin(data: LoginResponse) {
    if (data.userType !== "ADMIN") throw new Error("Esta conta não possui acesso administrativo.");
    sessionStorage.setItem(ADMIN_TOKEN_KEY, data.token);
    setToken(data.token);
    setStatus("idle");
  }

  async function login(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setStatus("sending");
    setMessage("");
    const form = new FormData(event.currentTarget);
    try {
      const response = await fetch("/api/auth/login", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ email: form.get("email"), password: form.get("password") }),
      });
      const data = await response.json() as LoginResponse & ApiError;
      if (!response.ok) throw new Error(data.message || "E-mail ou senha inválidos.");
      authorizeAdmin(data);
    } catch (error) {
      setStatus("error");
      setMessage(error instanceof Error ? error.message : "Não foi possível entrar.");
    }
  }

  async function bootstrap(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!isValidCpf(setupCpf)) {
      setStatus("error");
      setMessage("Informe um CPF válido no formato 000.000.000-00.");
      return;
    }
    setStatus("sending");
    setMessage("");
    const form = new FormData(event.currentTarget);
    try {
      const response = await fetch("/api/auth/bootstrap", {
        method: "POST",
        headers: { "Content-Type": "application/json", "X-Bootstrap-Key": String(form.get("bootstrapKey")) },
        body: JSON.stringify({
          institutionName: form.get("institutionName"), name: form.get("name"),
          cpf: cpfDigits(setupCpf), email: form.get("email"), password: form.get("password"),
          department: form.get("department"), gender: "UNDEFINED",
        }),
      });
      const data = await response.json() as LoginResponse & ApiError;
      if (!response.ok) throw new Error(data.message || "Não foi possível criar o administrador.");
      authorizeAdmin(data);
      setRequiresSetup(false);
    } catch (error) {
      setStatus("error");
      setMessage(error instanceof Error ? error.message : "Não foi possível criar o administrador.");
    }
  }

  function logout() {
    sessionStorage.removeItem(ADMIN_TOKEN_KEY);
    setToken("");
  }

  if (!token) {
    return (
      <main className="app-shell admin-shell">
        <header className="topbar"><Brand /><a className="quiet-link" href="/">Voltar ao terminal</a></header>
        <section className="login-page">
          {setupMode ? <form className="login-card setup-card" onSubmit={bootstrap}>
            <span className="eyebrow">CONFIGURAÇÃO INICIAL</span>
            <h1>Primeiro administrador</h1>
            <p>Disponível somente enquanto o sistema não possui uma conta ADMIN.</p>
            <label>Instituição<input name="institutionName" required autoFocus /></label>
            <label>Nome completo<input name="name" autoComplete="name" required /></label>
            <label>CPF<input name="cpf" inputMode="numeric" placeholder="000.000.000-00" value={setupCpf} onChange={(event) => setSetupCpf(formatCpf(event.target.value))} maxLength={14} required /></label>
            <label>E-mail administrativo<input name="email" type="email" autoComplete="username" required /></label>
            <label>Senha<input name="password" type="password" minLength={8} maxLength={72} autoComplete="new-password" required /></label>
            <label>Departamento<input name="department" required /></label>
            <label>Chave de configuração<input name="bootstrapKey" type="password" minLength={32} autoComplete="off" required /></label>
            {message && <p className="login-error" role="alert">{message}</p>}
            <button className="primary-button" type="submit" disabled={status === "sending"}>{status === "sending" ? "Criando…" : "Criar administrador"}</button>
            <button className="secondary-button" type="button" onClick={() => { setSetupMode(false); setMessage(""); }}>Voltar ao login</button>
          </form> : <form className="login-card" onSubmit={login}>
            <span className="eyebrow">ÁREA RESTRITA</span>
            <h1>Acesso administrativo</h1>
            <p>Entre com uma conta autorizada para gerenciar os cadastros.</p>
            <label>E-mail<input name="email" type="email" autoComplete="username" required autoFocus /></label>
            <label>Senha<input name="password" type="password" autoComplete="current-password" required /></label>
            {message && <p className="login-error" role="alert">{message}</p>}
            <button className="primary-button" type="submit" disabled={status === "sending"}>{status === "sending" ? "Entrando…" : "Entrar"}</button>
            {requiresSetup && <button className="secondary-button" type="button" onClick={() => { setSetupMode(true); setMessage(""); }}>Configurar primeiro administrador</button>}
          </form>}
        </section>
      </main>
    );
  }

  return (
    <main className="app-shell admin-shell">
      <header className="topbar">
        <Brand />
        <nav className="navigation" aria-label="Área administrativa">
          <span className="mode-label">Administração</span>
          <button type="button" onClick={logout}>Sair</button>
        </nav>
      </header>
      <Registration token={token} onUnauthorized={logout} />
    </main>
  );
}

export function App() {
  return window.location.pathname.startsWith("/admin") ? <AdminApp /> : <KioskApp />;
}
