"use strict";
(() => {
 const panel = document.getElementById("studyChat");
 if (!panel) return;
 const form = panel.querySelector("form"), input = panel.querySelector("textarea");
 const list = panel.querySelector("[role=log]"), status = panel.querySelector(".chat-status");
 const error = panel.querySelector(".chat-error"), button = form.querySelector("button");
 let ws, retry, pending = false, stopped = false;
 const ids = new Set();
 function ready(value) { button.disabled = !value || pending; }
 function append(m) {
  if (ids.has(m.id)) return;
  ids.add(m.id);
  const nearBottom = list.scrollHeight - list.scrollTop - list.clientHeight < 60;
  const item = document.createElement("div");
  item.className = "chat-message" + (m.autorId === panel.dataset.user ? " own" : "");
  const head = document.createElement("div"), author = document.createElement("strong");
  const time = document.createElement("time"), body = document.createElement("p");
  author.textContent = m.autor;
  time.dateTime = m.fecha;
  time.textContent = new Date(m.fecha).toLocaleTimeString([], {hour:"2-digit",minute:"2-digit"});
  body.textContent = m.texto;
  head.append(author,time); item.append(head,body);
  list.querySelector(".chat-empty")?.remove();
  list.append(item);
  if (nearBottom || m.autorId === panel.dataset.user) list.scrollTop = list.scrollHeight;
 }
 function connect() {
  if (stopped) return;
  status.textContent = "Conectando…"; ready(false);
  const url = new URL(panel.dataset.url, location.href);
  url.protocol = location.protocol === "https:" ? "wss:" : "ws:";
  url.searchParams.set("csrf", panel.dataset.csrf);
  ws = new WebSocket(url);
  ws.onmessage = event => {
   const data = JSON.parse(event.data);
   if (data.tipo === "historial") {
    list.replaceChildren(); ids.clear();
    data.mensajes.forEach(append);
    if (!data.mensajes.length) {
     const empty = document.createElement("p");
     empty.className = "chat-empty"; empty.textContent = "Empieza la conversación con tu compañero.";
     list.append(empty);
    }
    status.textContent = "En línea · Solo tu sesión"; ready(true);
   } else if (data.tipo === "mensaje") {
    append(data.mensaje);
    if (data.mensaje.autorId === panel.dataset.user && pending) {
     input.value = ""; pending = false; input.readOnly = false; ready(true); input.focus();
    }
   } else if (data.tipo === "error") {
    error.textContent = data.mensaje; pending = false; input.readOnly = false; ready(true);
   }
  };
  ws.onclose = event => {
   ready(false); input.readOnly = false;
   if (pending) error.textContent = "Se perdió la conexión. El historial se recuperará al reconectar; revisa si tu mensaje se guardó antes de reenviarlo.";
   pending = false;
   if (event.code === 1008) {
    stopped = true; status.textContent = "Chat no disponible. Vuelve a ingresar a tu sesión.";
   } else {
    status.textContent = "Sin conexión. Reconectando…";
    retry = setTimeout(connect, 2500);
   }
  };
  ws.onerror = () => { status.textContent = "No se pudo conectar al chat."; };
 }
 form.addEventListener("submit", event => {
  event.preventDefault(); error.textContent = "";
  if (!input.value.trim()) { error.textContent = "Escribe un mensaje con contenido."; input.focus(); return; }
  if (!ws || ws.readyState !== WebSocket.OPEN || pending) return;
  pending = true; input.readOnly = true; ready(false);
  ws.send("M" + input.value);
 });
 input.addEventListener("keydown", event => {
  if (event.key === "Enter" && !event.shiftKey && !event.isComposing) { event.preventDefault(); form.requestSubmit(); }
 });
 const heartbeat = setInterval(() => {
  if (ws?.readyState === WebSocket.OPEN) ws.send("P");
 }, 30000);
 window.addEventListener("pagehide", () => { stopped = true; clearTimeout(retry); clearInterval(heartbeat); ws?.close(); });
 connect();
})();
