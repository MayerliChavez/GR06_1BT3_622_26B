"use strict";
(() => {
 const date = document.getElementById("fechaHoy");
 if (date) date.textContent = new Intl.DateTimeFormat("es-CO", {day:"numeric",month:"long",year:"numeric",timeZone:"America/Bogota"}).format(new Date());
 const card = document.getElementById("timerCard");
 if (!card) return;
 const form = document.getElementById("timerForm"), action = document.getElementById("actionInput");
 const button = document.getElementById("mainAction"), clock = document.getElementById("clock");
 const message = document.getElementById("connectionMessage");
 let state = {sesionId:card.dataset.session,estado:card.dataset.state,tipo:card.dataset.type,
  orden:Number(card.dataset.order),restanteMillis:Number(card.dataset.remaining),objetivoMillis:Number(card.dataset.target)};
 let sampled = performance.now(), busy = false, syncing = false, online = true, version = 0;

 function render() {
  const elapsed = state.estado === "EN_EJECUCION" && online ? performance.now() - sampled : 0;
  const remaining = Math.max(0, state.restanteMillis - elapsed);
  const seconds = Math.ceil(remaining / 1000);
  clock.textContent = `${String(Math.floor(seconds / 60)).padStart(2,"0")}:${String(seconds % 60).padStart(2,"0")}`;
  document.title = `${clock.textContent} · Pomora`;
  document.getElementById("ringProgress").style.strokeDashoffset = String(860.8 * (1 - remaining / state.objetivoMillis));
  if (state.estado === "EN_EJECUCION" && remaining === 0 && online && !busy) mutate("completar");
 }

 function apply(next) {
  if (next.sesionId === state.sesionId && next.orden === state.orden && next.estado === "COMPLETADO" && state.estado !== "COMPLETADO") {
   window.PomoraUI?.blockEnded({key:`${state.sesionId}:${state.orden}`, type:state.tipo});
  }
  const changed = next.estado !== state.estado || next.sesionId !== state.sesionId || next.orden !== state.orden;
  // Una acción realizada en otra pestaña debe actualizar también los formularios de cierre.
  if (next.sesionId !== state.sesionId) { window.location.reload(); return; }
  state = next;
  sampled = performance.now();
  online = true;
  card.dataset.state = state.estado;
  card.dataset.type = state.tipo;
  const paused = state.estado === "PAUSADO", running = state.estado === "EN_EJECUCION", completed = state.estado === "COMPLETADO";
  action.value = paused ? "reanudar" : running ? "pausar" : completed ? "siguiente" : "iniciar";
  document.getElementById("mainActionLabel").textContent = paused ? "Reanudar temporizador" : running ? "Pausar temporizador" : completed ? "Iniciar siguiente bloque" : "Iniciar temporizador";
  document.getElementById("mainActionIcon").textContent = running ? "Ⅱ" : "▶";
  document.getElementById("statusLabel").textContent = paused ? "En pausa" : running ? "En ejecución" : completed ? "Bloque completado" : "Listo para empezar";
  document.getElementById("blockLabel").textContent = `BLOQUE ${state.orden}`;
  document.getElementById("clockType").textContent = state.tipo === "CONCENTRACION" ? "TIEMPO DE ENFOQUE" : "TIEMPO DE DESCANSO";
  document.getElementById("clockCaption").textContent = paused ? "Tu tiempo está a salvo." : completed ? "Bien hecho. Tómate un respiro." : state.tipo === "CONCENTRACION" ? "Dedica este momento a una sola tarea." : "Descansa antes de volver a concentrarte.";
  document.getElementById("timerHint").textContent = completed ? "Continúa con el siguiente bloque o finaliza tu sesión." : paused ? "Continúa cuando estés listo." : "Puedes hacer una pausa cuando lo necesites.";
  document.getElementById("phaseFocus").classList.toggle("active", state.tipo === "CONCENTRACION");
  document.getElementById("phaseShort").classList.toggle("active", state.tipo === "DESCANSO_CORTO");
  document.getElementById("phaseLong").classList.toggle("active", state.tipo === "DESCANSO_LARGO");
  if (changed) document.getElementById("statusAnnouncement").textContent = document.getElementById("statusLabel").textContent;
  message.textContent = "";
  render();
 }

 async function sync() {
  if (syncing || busy) return;
  syncing = true;
  const requestedVersion = version;
  try {
   const response = await fetch(`${card.dataset.url}?formato=json`, {cache:"no-store",signal:AbortSignal.timeout(8000)});
   if (!response.ok) throw new Error("No se pudo consultar el temporizador.");
   const next = await response.json();
   if (requestedVersion === version) apply(next);
  } catch (_) {
   if (requestedVersion !== version) return;
   if (online) {
    state.restanteMillis = Math.max(0,state.restanteMillis - (state.estado === "EN_EJECUCION" ? performance.now() - sampled : 0));
    online = false;
   }
   message.textContent = "Sin conexión con el servidor. Reconectando para verificar el tiempo…";
  } finally { syncing = false; }
 }

 async function mutate(command) {
  if (busy) return;
  busy = true; button.disabled = true;
  version++;
  try {
   const data = new URLSearchParams({csrf:card.dataset.csrf,sesionId:state.sesionId,accion:command,formato:"json"});
   const response = await fetch(card.dataset.url, {method:"POST",body:data,signal:AbortSignal.timeout(8000)});
   if (response.status === 403) { message.textContent = "Recarga la página para continuar con tu sesión."; return; }
   if (!response.ok && response.status !== 409) throw new Error("No se pudo guardar la acción.");
   apply(await response.json());
   if (response.status === 409) message.textContent = "El estado cambió. El temporizador ya está actualizado.";
  } catch (_) { message.textContent = "No se pudo confirmar la acción. Verificando la sesión…"; }
  finally { busy = false; button.disabled = false; }
 }

 // Los formularios funcionan también sin JavaScript. JS solo sincroniza y presenta la cuenta regresiva.
 form.addEventListener("submit", event => { event.preventDefault(); mutate(action.value); });
 document.addEventListener("visibilitychange", () => { if (!document.hidden) sync(); });
 window.addEventListener("online", sync);
 apply(state);
 setInterval(render, 200);
 setInterval(sync, 3000);
})();
