"use strict";
(() => {
 const reduce = matchMedia("(prefers-reduced-motion: reduce)").matches;
 const read = (key, fallback) => { try { return localStorage.getItem(key) ?? fallback; } catch (_) { return fallback; } };
 const write = (key, value) => { try { localStorage.setItem(key, value); } catch (_) {} };
 let enabled = read("pomora-sound", "on") === "on", audio = null, ready = false;
 const soundButtons = [...document.querySelectorAll(".sound-toggle")];
 const live = document.createElement("div"); live.className = "completion-toast"; live.setAttribute("role", "status");
 live.setAttribute("aria-live", "polite"); document.body.append(live);
 let toastTimeout;
 function toast(text) { live.textContent = text; live.classList.add("visible"); clearTimeout(toastTimeout); toastTimeout = setTimeout(() => live.classList.remove("visible"), 6500); }
 function soundLabels() {
  soundButtons.forEach(button => {
   button.textContent = !enabled ? "♪ Sonido apagado" : ready ? "♪ Sonido activo" : "♪ Activar sonido";
   button.setAttribute("aria-pressed", String(enabled && ready));
   button.title = "Alarma suave al finalizar concentración o descanso";
  });
 }
 async function unlock() {
  const Context = window.AudioContext || window.webkitAudioContext;
  if (!Context) return false;
  try { audio ||= new Context(); await audio.resume(); ready = audio.state === "running"; soundLabels(); return ready; }
  catch (_) { return false; }
 }
 function chime() {
  if (!enabled || !ready || audio?.state !== "running") return;
  const now = audio.currentTime;
  [523.25, 659.25, 783.99].forEach((frequency, i) => {
   const oscillator = audio.createOscillator(), volume = audio.createGain();
   oscillator.type = "sine"; oscillator.frequency.value = frequency;
   const start = now + i * .32;
   volume.gain.setValueAtTime(0, start); volume.gain.linearRampToValueAtTime(.055, start + .045);
   volume.gain.exponentialRampToValueAtTime(.001, start + 1.55);
   oscillator.connect(volume); volume.connect(audio.destination); oscillator.start(start); oscillator.stop(start + 1.65);
   oscillator.onended = () => { oscillator.disconnect(); volume.disconnect(); };
  });
 }
 // Los navegadores requieren una interacción antes de reproducir audio.
 const initialGesture = event => { if (enabled && !event.target.closest?.(".sound-toggle,.sound-preview")) unlock(); };
 document.addEventListener("pointerdown", initialGesture, {once:true});
 document.addEventListener("keydown", initialGesture, {once:true});
 soundButtons.forEach(button => button.addEventListener("click", async () => {
  if (enabled && ready) { enabled = false; }
  else { enabled = true; if (!await unlock()) toast("El navegador no permitió activar el audio."); }
  write("pomora-sound", enabled ? "on" : "off"); soundLabels();
 }));
 document.querySelectorAll(".sound-preview").forEach(button => button.addEventListener("click", async () => {
  enabled = true; write("pomora-sound", "on");
  if (await unlock()) { chime(); toast("Así sonará el final de cada bloque."); }
  else toast("El navegador no permitió activar el audio.");
 }));
 soundLabels();
 const played = new Set();
 window.PomoraUI = {
  blockEnded({key, type}) {
   if (played.has(key)) return; played.add(key);
   let previous; try { previous = JSON.parse(read("pomora-last-bells", "[]")); } catch (_) { previous = []; }
   if (!Array.isArray(previous)) previous = [];
   if (previous.includes(key)) return;
   write("pomora-last-bells", JSON.stringify([...previous.slice(-19), key]));
   const focus = type === "CONCENTRACION";
   toast(focus ? "Bloque de concentración terminado. Tómate un respiro." : "Descanso terminado. Es momento de volver al enfoque.");
   document.querySelectorAll(".timer-card").forEach(card => {
    card.classList.remove("block-finished"); void card.offsetWidth; card.classList.add("block-finished");
    setTimeout(() => card.classList.remove("block-finished"), 2400);
   });
   chime();
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
