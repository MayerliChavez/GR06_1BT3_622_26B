"use strict";
(() => {
 const card = document.getElementById("sharedCard"); if (!card) return;
 let id = card.dataset.session, waiting = card.dataset.waiting === "true", busy = false;
 let remaining = Number(card.dataset.remaining), target = Number(card.dataset.target), sampled = performance.now(), connected = true;
 const message = document.getElementById("sharedConnection");
 function render() {
  if (!id) return;
  const current = Math.max(0, remaining - (connected ? performance.now() - sampled : 0));
  const seconds = Math.ceil(current / 1000), clock = document.getElementById("sharedClock");
  if (clock) clock.textContent = `${String(Math.floor(seconds / 60)).padStart(2,"0")}:${String(seconds % 60).padStart(2,"0")}`;
  const ring = document.getElementById("sharedRing");
  if (ring) ring.style.strokeDashoffset = String(860.8 * (1 - current / target));
 }
 async function sync() {
  if (busy || (!id && !waiting)) return;
  busy = true;
  try {
   const response = await fetch(card.dataset.url, {method:"POST",body:new URLSearchParams({accion:"actualizar",csrf:card.dataset.csrf,formato:"json"}),signal:AbortSignal.timeout(8000)});
   if (!response.ok) throw new Error("No se pudo sincronizar la sesión.");
   const next = await response.json();
   if (next.compartidaId !== id || next.esperando !== waiting) { location.reload(); return; }
   remaining = next.restanteMillis; target = next.objetivoMillis; sampled = performance.now(); connected = true;
   if (id) {
    document.getElementById("sharedOrder").textContent = `BLOQUE ${next.orden}`;
    document.getElementById("sharedType").textContent = next.tipo === "CONCENTRACION" ? "CONCENTRACIÓN COMPARTIDA" : "DESCANSO COMPARTIDO";
    document.getElementById("partnerStatus").textContent = next.presente ? "Conectado al mismo temporizador" : next.motivo === "DESCONEXION" ? "El compañero se desconectó. Puedes continuar." : "El compañero salió. Puedes continuar.";
   }
   message.textContent = ""; render();
  } catch (_) {
   if (connected) remaining = Math.max(0,remaining - (id ? performance.now() - sampled : 0));
   connected = false; message.textContent = "Sin conexión con el servidor. Verificando el estudio compartido…";
  } finally { busy = false; }
 }
 setInterval(render,200); setInterval(sync,3000); sync();
 document.addEventListener("visibilitychange", () => { if (!document.hidden) sync(); });
 window.addEventListener("online",sync);
})();
