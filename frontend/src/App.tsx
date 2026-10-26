import { FormEvent, useState } from "react";
import { Identification } from "./features/identification/Identification";
import { Registration } from "./features/registration/Registration";

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
      if (data.userType !== "ADMIN") throw new Error("Esta conta não possui acesso administrativo.");
      sessionStorage.setItem(ADMIN_TOKEN_KEY, data.token);
      setToken(data.token);
      setStatus("idle");
    } catch (error) {
      setStatus("error");
      setMessage(error instanceof Error ? error.message : "Não foi possível entrar.");
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
          <form className="login-card" onSubmit={login}>
            <span className="eyebrow">ÁREA RESTRITA</span>
            <h1>Acesso administrativo</h1>
            <p>Entre com uma conta autorizada para gerenciar os cadastros.</p>
            <label>E-mail<input name="email" type="email" autoComplete="username" required autoFocus /></label>
            <label>Senha<input name="password" type="password" autoComplete="current-password" required /></label>
            {message && <p className="login-error" role="alert">{message}</p>}
            <button className="primary-button" type="submit" disabled={status === "sending"}>{status === "sending" ? "Entrando…" : "Entrar"}</button>
          </form>
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
