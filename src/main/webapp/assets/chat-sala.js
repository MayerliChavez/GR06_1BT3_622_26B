"use strict";

(() => {
const panel = document.getElementById("chatSala");
const ciclo = document.getElementById("cicloSala");
if (!panel || !ciclo) return;

const form = document.getElementById("formChatSala");
const input = document.getElementById("textoChatSala");
const boton = document.getElementById("enviarChatSala");
const lista = document.getElementById("mensajesChatSala");
const estado = document.getElementById("estadoChatSala");
const error = document.getElementById("errorChatSala");

const nodos = new Map();

let conectado = false;
let permitido = false;
let pendiente = false;
let espera = null;

function controles() {
input.disabled = !conectado || !permitido;
input.readOnly = pendiente;
boton.disabled = !conectado || !permitido || pendiente;
}

function terminarEnvio() {
pendiente = false;
window.clearTimeout(espera);
controles();
}

function mostrarError(mensaje) {
error.textContent = mensaje;
terminarEnvio();
}

function actualizarHistorial(mensajes) {
if (!Array.isArray(mensajes)) return;

const abajo =
lista.scrollHeight
- lista.scrollTop
- lista.clientHeight < 60;

const vigentes = new Set(
mensajes.map(m => String(m.id))
);

for (const [id, nodo] of nodos) {
if (!vigentes.has(id)) {
nodo.remove();
nodos.delete(id);
}
}

for (const m of mensajes) {
const id = String(m.id);
if (nodos.has(id)) continue;

lista.querySelector(".chat-empty")?.remove();

const item = document.createElement("div");

item.className = "chat-message"
+ (m.autorId === panel.dataset.user
? " own"
: "");

const cabecera = document.createElement("div");
const autor = document.createElement("strong");
const hora = document.createElement("time");
const texto = document.createElement("p");

autor.textContent = m.autor;
hora.dateTime = m.fecha;

hora.textContent =
new Date(m.fecha).toLocaleTimeString([], {
hour: "2-digit",
minute: "2-digit"
});

texto.textContent = m.texto;

cabecera.append(autor, hora);
item.append(cabecera, texto);
lista.append(item);

nodos.set(id, item);
}

if (!mensajes.length
&& !lista.querySelector(".chat-empty")) {
const vacio = document.createElement("p");
vacio.className = "chat-empty";
vacio.textContent = "Todavía no hay mensajes.";
lista.append(vacio);
}

if (abajo) {
lista.scrollTop = lista.scrollHeight;
}
}

ciclo.addEventListener("sala:estado", evento => {
const datos = evento.detail;

conectado = true;

permitido = !datos.iniciado
|| datos.bloque === "DESCANSO_CORTO"
|| datos.bloque === "DESCANSO_LARGO";

const aviso = permitido
? "En línea · Solo tu sala"
: "El chat se habilitará en el próximo descanso.";

if (estado.textContent !== aviso) {
estado.textContent = aviso;
}

actualizarHistorial(datos.mensajes);

for (const respuesta of datos.eventosChat || []) {
if (respuesta.tipo === "guardado" && pendiente) {
input.value = "";
error.textContent = "";
terminarEnvio();

} else if (respuesta.tipo === "error") {
mostrarError(respuesta.mensaje);
}
}

controles();
});

ciclo.addEventListener("sala:desconexion", evento => {
conectado = false;

estado.textContent = evento.detail.permanente
? "Chat no disponible. Recarga la página."
: "Sin conexión. Recuperando el historial…";

if (pendiente) {
mostrarError(
"La conexión se interrumpió. "
+ "Revisa el historial antes de reenviar el mensaje."
);
}

controles();
});

ciclo.addEventListener("sala:errorChat", evento => {
mostrarError(evento.detail);
});

form.addEventListener("submit", evento => {
evento.preventDefault();

if (!conectado || !permitido || pendiente) return;

if (!input.value.trim()) {
mostrarError("Escribe un mensaje antes de enviarlo.");
return;
}
if (input.value.length > 2000) {
mostrarError("El mensaje no puede superar los 2000 caracteres.");
return;
}

error.textContent = "";
pendiente = true;
controles();

espera = window.setTimeout(() => {
error.textContent =
"No llegó la confirmación. "
+ "Recuperando la conexión; revisa el historial "
+ "antes de reenviar.";

ciclo.dispatchEvent(
new CustomEvent("sala:reconectar")
);
}, 15000);

ciclo.dispatchEvent(
new CustomEvent("sala:enviarChat", {
detail: input.value
})
);
});

input.addEventListener("keydown", evento => {
if (evento.key === "Enter"
&& !evento.shiftKey
&& !evento.isComposing) {
evento.preventDefault();
form.requestSubmit();
}
});

window.addEventListener("pagehide", () => {
window.clearTimeout(espera);
});
})();
