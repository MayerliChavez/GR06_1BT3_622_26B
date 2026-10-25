"use strict";
(() => {
 const reduce = matchMedia("(prefers-reduced-motion: reduce)").matches;
 const read = (key, fallback) => { try { return localStorage.getItem(key) ?? fallback; } catch (_) { return fallback; } };
 const write = (key, value) => { try { localStorage.setItem(key, value); } catch (_) {} };
 let enabled = read("pomora-sound", "on") === "on", audio = null, ready = false;
 const soundButtons = [...document.querySelectorAll(".sound-toggle")];
  const artBase = new URL("notificaciones/", document.currentScript.src);
 const noticeArtwork = {
  CONCENTRACION: new URL("concentracion.png", artBase).href,
  DESCANSO_CORTO: new URL("descanso-corto.png", artBase).href,
  DESCANSO_LARGO: new URL("descanso-largo.png", artBase).href
 };
 if (document.querySelector(".timer-card")) Object.values(noticeArtwork).forEach(src => { const image = new Image(); image.src = src; });
 const backdrop = document.createElement("div"); backdrop.className = "completion-backdrop"; backdrop.setAttribute("aria-hidden", "true"); document.body.append(backdrop);
 const live = document.createElement("div"); live.className = "completion-toast"; live.setAttribute("role", "status");
 live.setAttribute("aria-live", "polite"); document.body.append(live);
 let toastTimeout;
 const pendingBells = new Map();
 let flushing = false;
 function formatBlockTime(millis) {
  const seconds = Math.max(1, Math.round(millis / 1000)), minutes = Math.floor(seconds / 60), remainder = seconds % 60;
  const parts = [];
  if (minutes) parts.push(`${minutes} ${minutes === 1 ? "minuto" : "minutos"}`);
  if (remainder) parts.push(`${remainder} ${remainder === 1 ? "segundo" : "segundos"}`);
  return parts.join(" y ");
 }
 function positionNotice() {
  const workspace = document.querySelector(".workspace");
  const rect = workspace?.getBoundingClientRect();
  live.style.left = rect ? `${(Math.max(0, rect.left) + Math.min(innerWidth, rect.right)) / 2}px` : "50%";
 }
 function toast(text, duration = 12500, notification = null) {
  live.replaceChildren();
  const type = notification?.tipoBloque;
  live.dataset.type = type || "INFO";
  const heading = document.createElement("h2"); heading.className = "completion-title";
  heading.textContent = type === "CONCENTRACION" ? "¡Tiempo de concentrarse!" : type === "DESCANSO_LARGO" ? "¡Tiempo de descanso largo!" : type === "DESCANSO_CORTO" ? "¡Tiempo de descanso!" : "Pomora";
  const body = document.createElement("div"); body.className = "completion-body";
  const message = document.createElement("p"); message.className = "completion-message"; message.textContent = text;
  body.append(message);
  if (notification && Number.isFinite(notification.duracionBloqueMillis) && notification.duracionBloqueMillis > 0) {
   const detail = document.createElement("p"); detail.className = "completion-duration";
   const blockTime = formatBlockTime(notification.duracionBloqueMillis);
   detail.textContent = type === "CONCENTRACION" ? `Dedica ${blockTime} a tu estudio. Tú puedes.` : `Buen trabajo. Tómate ${blockTime} para recargar energías.`;
   body.append(detail);
  }
  const progress = document.createElement("span"); progress.className = "completion-progress"; progress.setAttribute("aria-hidden","true"); progress.style.animationDuration = `${duration}ms`;
  const card = document.createElement("div"); card.className = "completion-card"; card.append(heading, body, progress);
  if (noticeArtwork[type]) {
   const image = document.createElement("img"); image.className = "completion-art"; image.src = noticeArtwork[type];
   image.alt = ""; image.setAttribute("aria-hidden", "true"); image.width = 195; image.height = 195; image.decoding = "async";
   live.append(image);
  }
  live.append(card); positionNotice(); live.classList.add("visible"); backdrop.classList.toggle("visible", Boolean(notification));
  clearTimeout(toastTimeout);
  toastTimeout = setTimeout(() => { live.classList.remove("visible"); backdrop.classList.remove("visible"); live.replaceChildren(); }, duration);
 }
 window.addEventListener("resize", positionNotice);
 function soundLabels() {
  soundButtons.forEach(button => {
   button.textContent = !enabled ? "♪ Sonido apagado" : ready ? "♪ Sonido activo" : "♪ Activar sonido";
   button.setAttribute("aria-pressed", String(enabled && ready));
   button.title = "Tonos diferentes para concentración, descanso corto y descanso largo";
  });
 }
 async function unlock() {
  const Context = window.AudioContext || window.webkitAudioContext;
  if (!Context) return false;
  try { audio ||= new Context(); await audio.resume(); ready = audio.state === "running"; soundLabels(); if (ready) flushBells(); return ready; }
  catch (_) { return false; }
 }
 async function chime(type) {
  if (!enabled || !audio) return false;
  if (audio.state === "suspended") {
   try { await audio.resume(); } catch (_) { return false; }
  }
  ready = audio.state === "running"; soundLabels();
  if (!ready) return false;
  const tones = {
   CONCENTRACION: {notes:[659.25,783.99,987.77],wave:"sine",gap:.23,length:.65},
   DESCANSO_CORTO: {notes:[783.99,659.25],wave:"triangle",gap:.34,length:.9},
   DESCANSO_LARGO: {notes:[523.25,440,349.23],wave:"sine",gap:.48,length:1.3}
  };
  const profile = tones[type] || tones.CONCENTRACION;
  const now = audio.currentTime;
  profile.notes.forEach((frequency, i) => {
   const oscillator = audio.createOscillator(), volume = audio.createGain();
   oscillator.type = profile.wave; oscillator.frequency.value = frequency;
   const start = now + i * profile.gap;
   volume.gain.setValueAtTime(0, start); volume.gain.linearRampToValueAtTime(.055, start + .045);
   volume.gain.exponentialRampToValueAtTime(.001, start + profile.length);
   oscillator.connect(volume); volume.connect(audio.destination); oscillator.start(start); oscillator.stop(start + profile.length + .1);
   oscillator.onended = () => { oscillator.disconnect(); volume.disconnect(); };
  });
  return true;
 }
 async function flushBells() {
  if (flushing || !enabled) return;
  flushing = true;
  try {
   for (const [key, pending] of pendingBells) {
    if (Date.now() - pending.when > 20000) { pendingBells.delete(key); continue; }
    let previous; try { previous = JSON.parse(read("pomora-last-bells", "[]")); } catch (_) { previous = []; }
    if (!Array.isArray(previous)) previous = [];
    if (previous.includes(key)) { pendingBells.delete(key); continue; }
    // Registrar el aviso solo cuando el navegador pudo programar el sonido.
    if (!await chime(pending.type)) break;
    write("pomora-last-bells", JSON.stringify([...previous.slice(-19), key]));
    pendingBells.delete(key);
   }
  } finally { flushing = false; }
 }
 // Los navegadores requieren una interacción antes de reproducir audio.
 const initialGesture = event => { if (enabled && !event.target.closest?.(".sound-toggle")) unlock(); };
 document.addEventListener("pointerdown", initialGesture);
 document.addEventListener("keydown", initialGesture);
 soundButtons.forEach(button => button.addEventListener("click", async () => {
  if (enabled && ready) { enabled = false; }
  else { enabled = true; if (!await unlock()) toast("El navegador no permitió activar el audio."); }
  write("pomora-sound", enabled ? "on" : "off"); soundLabels();
 }));
 soundLabels();
 let savedNotices;
 try { savedNotices = JSON.parse(sessionStorage.getItem("pomora-notices") || "[]"); } catch (_) { savedNotices = []; }
 const played = new Set(Array.isArray(savedNotices) ? savedNotices : []);
 window.PomoraUI = {
  blockStarted({notification}) {
   if (!notification || typeof notification.id !== "string" || typeof notification.mensaje !== "string"
       || notification.posicion !== "CENTRO_APLICACION" || played.has(notification.id)) return;
   const key = notification.id;
   played.add(key);
   if (played.size > 100) played.delete(played.values().next().value);
   try { sessionStorage.setItem("pomora-notices", JSON.stringify([...played])); } catch (_) {}
   const duration = Number.isFinite(notification.duracionMillis) ? Math.min(15000, Math.max(1000, notification.duracionMillis)) : 12500;
   toast(notification.mensaje, duration, notification);

   document.querySelectorAll(".timer-card").forEach(card => {
    card.classList.remove("block-finished"); void card.offsetWidth; card.classList.add("block-finished");
    setTimeout(() => card.classList.remove("block-finished"), 2400);
   });
   pendingBells.set(key, {when:Date.now(),type:notification.tipoBloque});
   flushBells();
  }
 };
 const revealTargets = document.querySelectorAll(".page-heading,.timer-card,.rhythm-card,.progress-card,.stat,.history-card,.shared-notice-details>div,.bottom-note,.page-footer,.access-card");
 if (!reduce && "IntersectionObserver" in window) {
  const observer = new IntersectionObserver(entries => entries.forEach(entry => {
   if (entry.isIntersecting) { entry.target.classList.add("is-visible"); observer.unobserve(entry.target); }
  }), {threshold:.06});
  revealTargets.forEach((element, i) => { element.classList.add("reveal-ready"); element.style.setProperty("--enter-delay", `${Math.min(i % 4, 3) * 60}ms`); observer.observe(element); });
 }
 document.querySelectorAll(".profile-menu").forEach(menu => {
  document.addEventListener("click", event => { if (!menu.contains(event.target)) menu.open = false; });
  menu.addEventListener("keydown", event => { if (event.key === "Escape") { menu.open = false; menu.querySelector("summary").focus(); } });
 });
 document.querySelectorAll(".profile-name").forEach(name => {
  const avatar = name.closest(".profile-menu").querySelector(".avatar"); avatar.textContent = name.textContent.trim().slice(0, 1).toLocaleUpperCase("es");
 });
 document.querySelectorAll(".clock-wrap").forEach(clock => {
  const dots = document.createElement("div"); dots.className = "clock-sparks"; dots.setAttribute("aria-hidden", "true");
  dots.innerHTML = "<span></span><span></span><span></span>"; clock.append(dots);
 });
})();
