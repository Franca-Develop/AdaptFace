import { FormEvent, useState } from "react";

type ApiError = { message?: string; errors?: Record<string, string> };
type EmbeddingResponse = { embedding: number[]; faceModel: string; message?: string };
type RegistrationResponse = { name: string; userId: number; enrollmentId: number };

async function json<T>(response: Response): Promise<T> {
  const text = await response.text();
  let data: T & ApiError;
  try {
    data = JSON.parse(text) as T & ApiError;
  } catch {
    throw new Error(response.ok ? "O serviço retornou uma resposta inválida." : text || "Não foi possível acessar o serviço.");
  }
  if (!response.ok) throw new Error(data.message || Object.values(data.errors ?? {})[0] || "Não foi possível concluir a operação.");
  return data;
}

type RegistrationProps = { token: string; onUnauthorized: () => void };

export function Registration({ token, onUnauthorized }: RegistrationProps) {
  const [type, setType] = useState("STUDENT");
  const [photo, setPhoto] = useState<File | null>(null);
  const [status, setStatus] = useState<"idle" | "sending" | "success" | "error">("idle");
  const [notice, setNotice] = useState("");

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const formElement = event.currentTarget;
    if (!photo) {
      setStatus("error");
      setNotice("Adicione uma foto frontal para continuar.");
      return;
    }
    setStatus("sending");
    setNotice("Preparando o cadastro…");
    const form = new FormData(formElement);
    try {
      setNotice("Analisando a foto com DeepFace…");
      const image = await new Promise<string>((resolve, reject) => {
        const reader = new FileReader();
        reader.onload = () => resolve(String(reader.result));
        reader.onerror = () => reject(new Error("Não foi possível ler a foto."));
        reader.readAsDataURL(photo);
      });
      const facial = await json<EmbeddingResponse>(await fetch("/vision/embedding", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ image }),
      }));
      if (!Array.isArray(facial.embedding) || facial.embedding.length !== 512) {
        throw new Error(facial.message || "A foto não gerou uma identificação facial válida.");
      }

      setNotice("Salvando o cadastro…");
      const student = type === "STUDENT";
      const payload = {
        user: {
          name: form.get("name"),
          cpf: form.get("cpf"),
          email: form.get("email"),
          password: form.get("password"),
          phone: form.get("phone") || null,
          userType: type,
          registrationNumber: student ? Number(form.get("registrationNumber")) : null,
          institutionId: Number(form.get("institutionId")),
          period: student ? form.get("period") || null : null,
          course: student ? form.get("course") : null,
          department: student ? null : form.get("department"),
        },
        faceEmbedding: facial.embedding,
        faceModel: facial.faceModel,
        rfidTag: form.get("rfidTag") || null,
      };
      const registrationResponse = await fetch("/api/registrations", {
        method: "POST",
        headers: { "Content-Type": "application/json", Authorization: `Bearer ${token}` },
        body: JSON.stringify(payload),
      });
      if (registrationResponse.status === 401 || registrationResponse.status === 403) {
        onUnauthorized();
        throw new Error("Sua sessão administrativa expirou. Entre novamente.");
      }
      const created = await json<RegistrationResponse>(registrationResponse);
      setStatus("success");
      setNotice(`${created.name} cadastrado com sucesso. Matrícula facial ${created.enrollmentId}.`);
      formElement.reset();
      setPhoto(null);
    } catch (error) {
      setStatus("error");
      setNotice(error instanceof Error ? error.message : "Não foi possível concluir o cadastro.");
    }
  }

  return (
    <section className="registration">
      <div className="section-heading"><span className="eyebrow">NOVO CADASTRO</span><h1>Dados de acesso</h1><p>Preencha as informações para liberar o reconhecimento facial.</p></div>
      <form className="registration-form" onSubmit={submit}>
        <div className="form-section"><div className="section-number">01</div><div className="fields"><h2>Informações pessoais</h2><div className="field-grid">
          <label className="wide">Nome completo<input name="name" autoComplete="name" required /></label>
          <label>CPF<input name="cpf" inputMode="numeric" pattern="\d{11}" maxLength={11} required /></label>
          <label>E-mail<input name="email" type="email" required /></label>
          <label>Senha inicial<input name="password" type="password" minLength={8} maxLength={72} required /></label>
          <label>Telefone<input name="phone" /></label>
          <label>ID da instituição<input name="institutionId" type="number" min="1" required /></label>
          <label>Perfil<select name="userType" value={type} onChange={(event) => setType(event.target.value)}><option value="STUDENT">Aluno</option><option value="EMPLOYEE">Funcionário</option></select></label>
          <label>RFID opcional<input name="rfidTag" /></label>
          {type === "STUDENT" ? <><label>RM<input name="registrationNumber" type="number" min="1" required /></label><label>Curso<input name="course" required /></label><label>Período<input name="period" /></label></> : <label className="wide">Departamento<input name="department" required /></label>}
        </div></div></div>
        <div className="form-section"><div className="section-number">02</div><div className="fields"><h2>Identificação facial</h2><label className="upload-area"><input type="file" accept="image/jpeg,image/png,image/webp" onChange={(event) => setPhoto(event.target.files?.[0] ?? null)} /><span className="upload-symbol">+</span><strong>{photo?.name ?? "Selecionar foto frontal"}</strong><span>Rosto centralizado, visível e bem iluminado.</span></label><label className="consent"><input type="checkbox" required /> Autorizo o cadastro da representação facial para controle de acesso.</label></div></div>
        {notice && <p className={`form-notice ${status}`} role="status">{notice}</p>}
        <button className="primary-button submit-button" type="submit" disabled={status === "sending"}>{status === "sending" ? "Processando…" : "Validar e cadastrar"} <span aria-hidden="true">→</span></button>
      </form>
    </section>
  );
}
