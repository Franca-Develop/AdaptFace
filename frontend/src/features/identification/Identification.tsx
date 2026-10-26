import { useCallback, useEffect, useRef, useState } from "react";

type Stage = "permission" | "ready" | "looking" | "scanning" | "granted" | "denied" | "spoof" | "error";
type VisionResult = { status: "looking" | "multiple" | "guidance" | "scanning" | "granted" | "denied" | "spoof" | "error"; name?: string; message: string; detail?: string; progress?: number };

const copy: Record<Stage, { title: string; description: string }> = {
  permission: { title: "Preparando a câmera", description: "Confirme a permissão para iniciar." },
  ready: { title: "Câmera ativada", description: "Posicione seu rosto dentro do guia." },
  looking: { title: "Procurando rosto", description: "Olhe para a câmera e mantenha o rosto visível." },
  scanning: { title: "Verificando identidade", description: "Continue visível enquanto lemos seu rosto." },
  granted: { title: "Identificação concluída", description: "Acesso liberado. Você pode passar." },
  denied: { title: "Rosto não reconhecido", description: "Não encontramos um cadastro correspondente." },
  spoof: { title: "Prova de vida recusada", description: "Fotos, vídeos e telas não são aceitos." },
  error: { title: "Verificação indisponível", description: "Confira os serviços e tente novamente." },
};

export function Identification() {
  const videoRef = useRef<HTMLVideoElement>(null);
  const streamRef = useRef<MediaStream | null>(null);
  const runningRef = useRef(false);
  const finalRef = useRef(false);
  const tipUntilRef = useRef(0);
  const sessionRef = useRef(crypto.randomUUID());
  const [stage, setStage] = useState<Stage>("permission");
  const [message, setMessage] = useState(copy.permission.description);
  const [name, setName] = useState("");
  const [progress, setProgress] = useState(0);

  const openCamera = useCallback(async () => {
    try {
      finalRef.current = false;
      sessionRef.current = crypto.randomUUID();
      setName("");
      setProgress(0);
      const stream = await navigator.mediaDevices.getUserMedia({ video: { facingMode: "user", width: 1280, height: 720 }, audio: false });
      streamRef.current?.getTracks().forEach((track) => track.stop());
      streamRef.current = stream;
      if (videoRef.current) {
        videoRef.current.srcObject = stream;
        await videoRef.current.play();
      }
      setStage("ready");
      setMessage(copy.ready.description);
    } catch {
      setStage("error");
      setMessage("Permita o acesso à câmera no navegador e tente novamente.");
    }
  }, []);

  const analyze = useCallback(async () => {
    const video = videoRef.current;
    if (!video || video.readyState < 2 || runningRef.current || finalRef.current) return;
    runningRef.current = true;
    try {
      const canvas = document.createElement("canvas");
      const scale = Math.min(1, 720 / video.videoWidth);
      canvas.width = Math.round(video.videoWidth * scale);
      canvas.height = Math.round(video.videoHeight * scale);
      const context = canvas.getContext("2d");
      context?.translate(canvas.width, 0);
      context?.scale(-1, 1);
      context?.drawImage(video, 0, 0, canvas.width, canvas.height);
      const response = await fetch("/vision/analyze", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ image: canvas.toDataURL("image/jpeg", 0.82), sessionId: sessionRef.current }),
      });
      const result = await response.json() as VisionResult;
      if (!response.ok || result.status === "error") throw new Error(result.message);
      if (result.status === "granted") {
        finalRef.current = true;
        setName(result.name ?? "Usuário identificado");
        setStage("granted");
        setMessage(result.message);
      } else if (result.status === "denied" || result.status === "spoof") {
        finalRef.current = true;
        setStage(result.status);
        setMessage(result.detail ?? result.message);
      } else {
        const isTip = result.status === "guidance" || result.status === "multiple" || result.status === "looking";
        if (isTip) {
          tipUntilRef.current = Date.now() + 3000;
          setStage("looking");
          setMessage(result.detail ?? result.message);
        } else {
          setStage("scanning");
          setProgress(result.progress ?? 0);
          if (Date.now() >= tipUntilRef.current) setMessage("Lendo seu rosto...");
        }
      }
    } catch (error) {
      setStage("error");
      setMessage(error instanceof Error ? error.message : copy.error.description);
    } finally {
      runningRef.current = false;
    }
  }, []);

  useEffect(() => {
    void openCamera();
    return () => streamRef.current?.getTracks().forEach((track) => track.stop());
  }, [openCamera]);

  useEffect(() => {
    if (!streamRef.current || ["permission", "granted", "denied", "spoof", "error"].includes(stage)) return;
    const timer = window.setInterval(() => void analyze(), 900);
    return () => window.clearInterval(timer);
  }, [analyze, stage]);

  function restart() {
    finalRef.current = false;
    sessionRef.current = crypto.randomUUID();
    setName("");
    setProgress(0);
    setStage("ready");
    setMessage(copy.ready.description);
  }

  const current = copy[stage];

  return (
    <section className={`identification page-grid stage-${stage}`} aria-live="polite">
      <div className="intro-copy">
        <span className="eyebrow">CONTROLE DE ACESSO</span>
        <h1>{current.title}</h1>
        <p>{name || current.description}</p>
        <div className="status-line"><span className={`status-dot ${stage}`} />{message}</div>
        {stage === "scanning" && <progress className="challenge-progress" aria-label="Progresso da leitura facial" value={progress} max={100} />}
      </div>
      <div className="camera-panel">
        <div className="camera-frame">
          <video ref={videoRef} autoPlay muted playsInline aria-label="Visualização da câmera" />
          <div className="face-guide" aria-hidden="true"><span /><span /><span /><span /></div>
          {stage === "permission" && <div className="camera-empty"><CameraIcon /><strong>Solicitando acesso à câmera</strong><span>Sua imagem é processada localmente.</span></div>}
          {(stage === "granted" || stage === "denied" || stage === "spoof") && <div className={`result-overlay ${stage}`}><ResultIcon success={stage === "granted"} /><strong>{stage === "granted" ? name : stage === "spoof" ? "Apresentação bloqueada" : "Não identificado"}</strong><span>{message}</span></div>}
        </div>
        {stage === "error" && <button className="primary-button" type="button" onClick={openCamera}>Tentar novamente <ArrowIcon /></button>}
        {(stage === "granted" || stage === "denied" || stage === "spoof") && <button className="primary-button" type="button" onClick={restart}>Tentar novamente <ArrowIcon /></button>}
        <p className="privacy-note"><ShieldIcon /> Verificação local e protegida</p>
      </div>
    </section>
  );
}

function CameraIcon() { return <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M4 7h3l1.5-2h7L17 7h3v11H4z" /><circle cx="12" cy="12.5" r="3.5" /></svg>; }
function ArrowIcon() { return <svg viewBox="0 0 24 24" aria-hidden="true"><path d="m9 5 7 7-7 7M4 12h12" /></svg>; }
function ShieldIcon() { return <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M12 3 5 6v5c0 4.7 2.9 8.2 7 10 4.1-1.8 7-5.3 7-10V6z" /><path d="m9 12 2 2 4-4" /></svg>; }
function ResultIcon({ success }: { success: boolean }) { return <svg viewBox="0 0 24 24" aria-hidden="true"><circle cx="12" cy="12" r="10" /><path d={success ? "m7.5 12 3 3 6-7" : "m8 8 8 8M16 8l-8 8"} /></svg>; }
