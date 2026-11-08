// Ejecuta el controlador real del chat con eventos DOM y transporte controlados.
const assert = require('node:assert/strict');
const fs = require('node:fs');
const vm = require('node:vm');
const path = require('node:path');
const nodes = new Map();
function element(id) {
    if (!nodes.has(id)) nodes.set(id, {
        dataset: {}, value: '', textContent: '', handlers: {},
        addEventListener(name, handler) { this.handlers[name] = handler; },
        dispatchEvent(event) { this.handlers[event.type]?.(event); },
        querySelector() { return {}; }
    });
    return nodes.get(id);
}
const sent = [];
const cycle = element('cicloSala');
cycle.addEventListener('sala:enviarChat', event => sent.push(event.detail));
vm.runInNewContext(fs.readFileSync(path.join(__dirname, '../src/main/webapp/assets/chat-sala.js'), 'utf8'), {
    document: { getElementById: element },
    window: { addEventListener() {}, clearTimeout() {}, setTimeout() { return 1; } },
    CustomEvent: class { constructor(type, options) { this.type = type; this.detail = options.detail; } }
});
const input = element('textoChatSala');
const button = element('enviarChatSala');
const error = element('errorChatSala');
const submit = text => {
    input.value = text;
    element('formChatSala').handlers.submit({ preventDefault() {} });
};
const state = block => cycle.dispatchEvent({ type: 'sala:estado', detail: { iniciado: true, bloque: block, mensajes: [] } });
state('DESCANSO_CORTO');
for (const text of ['', '   ', '\u00a0\t\n']) {
    submit(text);
    assert.equal(error.textContent, 'Escribe un mensaje antes de enviarlo.');
}
submit('A'.repeat(2001));
assert.equal(error.textContent, 'El mensaje no puede superar los 2000 caracteres.');
assert.equal(input.value.length, 2001);
assert.equal(sent.length, 0);
submit('A'.repeat(2000));
assert.equal(sent[0], 'A'.repeat(2000));
cycle.dispatchEvent({ type: 'sala:estado', detail: {
    iniciado: true, bloque: 'DESCANSO_CORTO', mensajes: [], eventosChat: [{ tipo: 'guardado' }]
} });
assert.equal(input.value, '');
state('CONCENTRACION');
assert.equal(input.disabled, true);
assert.equal(button.disabled, true);
assert.equal(element('estadoChatSala').textContent, 'El chat se habilitará en el próximo descanso.');
submit('No publicar');
assert.equal(sent.length, 1);
state('DESCANSO_LARGO');
assert.equal(input.disabled, false);
assert.equal(button.disabled, false);
submit('Hola');
assert.equal(sent[1], 'Hola');
console.log('OK: vacío, espacios, 2001, 2000, confirmación y bloqueo/reactivación por fase.');
