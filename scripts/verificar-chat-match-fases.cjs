const assert = require('node:assert/strict');
const fs = require('node:fs');
const vm = require('node:vm');
const path = require('node:path');
function node() {
    return {dataset: {}, value: '', handlers: {}, textContent: '',
        addEventListener(type, handler) { this.handlers[type] = handler; },
        append() {}, replaceChildren() {}, focus() {}, querySelector() { return null; }};
}
const panel = node(), form = node(), input = node(), button = node();
const list = node(), status = node(), error = node(), card = node();
card.dataset.type = 'CONCENTRACION';
panel.dataset = {url: '/chat/prueba', csrf: 'token', user: 'ana'};
panel.querySelector = selector => ({form, textarea: input, '[role=log]': list, '.chat-status': status, '.chat-error': error})[selector];
form.querySelector = () => button;
let socket;
const sent = [];
class Socket {
    static OPEN = 1;
    constructor() { socket = this; this.readyState = 1; }
    send(text) { sent.push(text); }
}
vm.runInNewContext(fs.readFileSync(path.join(__dirname, '../src/main/webapp/assets/chat.js'), 'utf8'), {
    document: {getElementById: id => id === 'studyChat' ? panel : card, createElement: node},
    location: {href: 'http://localhost/compartido', protocol: 'http:'}, URL, WebSocket: Socket,
    window: {addEventListener() {}}, setTimeout() {}, clearTimeout() {}, setInterval() {}, clearInterval() {}
});
socket.onmessage({data: JSON.stringify({tipo: 'historial', mensajes: []})});
assert.equal(input.disabled, true);
assert.equal(button.disabled, true);
assert.equal(status.textContent, 'El chat se habilitará en el próximo descanso.');
const submit = () => form.handlers.submit({preventDefault() {}});
const phase = (tipo, conectado = true) => card.handlers['compartido:estado']({detail: {tipo, conectado}});
input.value = 'Mi borrador';
submit();
assert.equal(sent.length, 0);
phase('DESCANSO_CORTO');
assert.equal(input.disabled, false);
assert.equal(button.disabled, false);
assert.equal(input.value, 'Mi borrador');
submit();
assert.deepEqual(sent, ['MMi borrador']);
phase('CONCENTRACION');
// Una respuesta tardía no debe habilitar el chat en concentración.
socket.onmessage({data: JSON.stringify({tipo: 'error', mensaje: 'El chat se habilitará en el próximo descanso.'})});
assert.equal(input.disabled, true);
assert.equal(button.disabled, true);
phase('DESCANSO_LARGO');
assert.equal(input.disabled, false);
assert.equal(button.disabled, false);
phase('DESCANSO_LARGO', false);
assert.equal(input.disabled, true);
phase('DESCANSO_LARGO');
assert.equal(input.disabled, false);
socket.onclose({code: 1006});
assert.equal(input.disabled, true);
assert.equal(input.value, 'Mi borrador');
console.log('OK: concentración, descansos corto/largo, borrador, respuesta tardía y desconexión.');

async function sincronizacionReal() {
    const elements = new Map(), intervals = [], events = [];
    const get = id => {
        if (!elements.has(id)) elements.set(id, {...node(), style: {}});
        return elements.get(id);
    };
    const timer = get('sharedCard');
    timer.dataset = {session: 'match', waiting: 'false', type: 'CONCENTRACION', remaining: '10000', target: '10000', order: '1'};
    timer.dispatchEvent = event => events.push(event.detail);
    let tipo = 'CONCENTRACION', failed = false;
    vm.runInNewContext(fs.readFileSync(path.join(__dirname, '../src/main/webapp/assets/compartido.js'), 'utf8'), {
        document: {getElementById: get, dispatchEvent() {}, addEventListener() {}},
        performance: {now: () => 0}, window: {addEventListener() {}},
        location: {reload() { throw Error('No debe recargar'); }},
        URLSearchParams, AbortSignal, Event,
        CustomEvent: class { constructor(type, options) { this.type = type; this.detail = options.detail; } },
        setInterval: (fn, ms) => intervals.push({fn, ms}),
        fetch: async () => {
            if (failed) throw Error('Sin conexión');
            return {ok: true, json: async () => ({compartidaId: 'match', esperando: false, tipo,
                orden: 1, restanteMillis: 10000, objetivoMillis: 10000, presente: true})};
        }
    });
    await new Promise(resolve => setImmediate(resolve));
    assert.equal(events.at(-1).tipo, 'CONCENTRACION');
    const sync = intervals.find(item => item.ms === 3000).fn;
    for (tipo of ['DESCANSO_CORTO', 'DESCANSO_LARGO', 'CONCENTRACION']) {
        await sync();
        assert.equal(events.at(-1).tipo, tipo);
        assert.equal(events.at(-1).conectado, true);
    }
    failed = true;
    await sync();
    assert.equal(events.at(-1).conectado, false);
    console.log('OK: el temporizador real comunica las fases y la pérdida de sincronización al chat.');
}
sincronizacionReal().catch(error => { console.error(error); process.exitCode = 1; });
