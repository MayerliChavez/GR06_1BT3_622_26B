const { chromium } = require('playwright');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const base = process.env.BASE_URL || 'http://127.0.0.1:18081';
const results = [];
function ok(label) { results.push(label); console.log('OK: ' + label); }
(async () => {
 const browser = await chromium.launch({channel: process.env.BROWSER_CHANNEL || 'msedge', headless:true});
 const users = [];
 try {
  for (const name of ['Ana Chat','Luis Chat','Carla Chat','Diego Chat']) {
   const context = await browser.newContext({viewport:{width:1440,height:1080}});
   const registration = await context.request.get(base + '/registro');
   const html = await registration.text();
   const csrf = /name="csrf" value="([^"]+)"/.exec(html)[1];
   const email = 'chat-' + crypto.randomUUID() + '@example.test', password = crypto.randomUUID()+'aA1!';
   const registered = await context.request.post(base+'/registro',{form:{csrf,nombre:name,correo:email,contrasena:password}});
   assert.ok((await registered.text()).includes('Cuenta creada'));
   await context.request.post(base+'/ingresar',{form:{csrf,correo:email,contrasena:password}});
   const shared = await context.request.get(base+'/compartido');
   const token = /data-csrf="([^"]+)"/.exec(await shared.text())[1];
   const page = await context.newPage();
   await page.addInitScript(() => {
    window.__frames = [];
    const Original = window.WebSocket;
    window.WebSocket = class extends Original {
     constructor(...args) {
      super(...args); window.__chatSocket = this;
      this.addEventListener('message', event => window.__frames.push(JSON.parse(event.data)));
     }
    };
   });
   users.push({name,context,page,token});
  }
  async function action(user, accion) {
   const response = await user.context.request.post(base+'/compartido',{form:{csrf:user.token,accion,formato:'json'}});
   assert.equal(response.status(),200); return response.json();
  }
  await action(users[0],'buscar');
  const ab = await action(users[1],'buscar');
  await action(users[2],'buscar');
  const cd = await action(users[3],'buscar');
  assert.notEqual(ab.compartidaId, cd.compartidaId);
  for (const user of users) {
   await user.page.goto(base+'/compartido');
   await user.page.waitForFunction(() => document.querySelector('.chat-status')?.textContent.startsWith('En línea'), {timeout:15000});
  }
  ok('Cuatro estudiantes conectados por WebSocket en dos sesiones diferentes');
  const [a,b,c,d] = users.map(u=>u.page);
  let navigationsB = 0; b.on('framenavigated', frame=> {if(frame === b.mainFrame()) navigationsB++;});
  const text = 'Tengo una duda: ¿cómo resolvemos el ejercicio?';
  await a.locator('#chatText').fill(text);
  await a.locator('.chat-form button').click();
  await a.waitForFunction(t => [...document.querySelectorAll('.chat-message p')].some(p=>p.textContent===t), text);
  await b.waitForFunction(t => [...document.querySelectorAll('.chat-message p')].some(p=>p.textContent===t), text);
  assert.equal(await a.locator('.chat-message strong').last().textContent(),'Ana Chat');
  assert.equal(await b.locator('.chat-message strong').last().textContent(),'Ana Chat');
  await a.waitForFunction(()=>document.querySelector('#chatText').value==='');
  assert.equal(navigationsB,0);
  ok('1.1 y 1.2: mensaje con autor en ambos chats, caja limpia y receptor sin recarga');
  for (const value of ['', '   ']) {
   await a.locator('#chatText').fill(value);
   await a.locator('.chat-form button').click();
   assert.ok((await a.locator('.chat-error').textContent()).includes('contenido'));
  }
  for (const value of ['', '   ', '\u00a0\u2003']) {
   const before = await a.evaluate(()=>window.__frames.length);
   await a.evaluate(value=>window.__chatSocket.send('M'+value), value);
   await a.waitForFunction(n=>window.__frames.slice(n).some(f=>f.tipo==='error'),before);
  }
  assert.equal(await a.locator('.chat-message').count(),1);
  assert.equal(await b.locator('.chat-message').count(),1);
  ok('1.3: vacio y espacios rechazados por interfaz y servidor, sin mensajes en ninguno de los chats');
  const second = 'Mensaje exclusivo de Carla y Diego';
  await c.locator('#chatText').fill(second); await c.locator('.chat-form button').click();
  await d.waitForFunction(t=>document.querySelector('.chat-log').textContent.includes(t),second);
  for (const page of [a,b]) {
   const n = await page.evaluate(()=>window.__frames.length);
   await page.evaluate(()=>window.__chatSocket.send('P'));
   await page.waitForFunction(n=>window.__frames.slice(n).some(f=>f.tipo==='pong'),n);
   assert.equal(await page.locator('.chat-message').count(),1);
   assert.ok(!(await page.locator('.chat-log').textContent()).includes(second));
  }
  assert.equal(await c.locator('.chat-message').count(),1);
  assert.equal(await d.locator('.chat-message').count(),1);
  ok('1.4: aislamiento bidireccional entre las dos parejas');
  const attack = '<img src=x onerror="window.chatXss=true"> "texto"\nsegunda línea';
  await b.locator('#chatText').fill(attack); await b.locator('.chat-form button').click();
  await a.waitForFunction(t=>[...document.querySelectorAll('.chat-message p')].some(p=>p.textContent===t),attack);
  assert.equal(await a.locator('.chat-log img').count(),0);
  assert.equal(await a.evaluate(()=>window.chatXss),undefined);
  ok('Texto con HTML, comillas, acentos y saltos de linea mostrado como texto seguro');
  await a.reload(); await a.waitForFunction(()=>document.querySelectorAll('.chat-message').length===2);
  assert.ok((await a.locator('.chat-log').textContent()).includes(text));
  ok('Mensajes registrados en base de datos y recuperados al recargar');
  const unauthorized = await b.evaluate(async ({room,csrf}) => {
   const url = new URL('/chat/'+room,location.href); url.protocol='ws:'; url.searchParams.set('csrf',csrf);
   return await new Promise((resolve,reject)=>{
    const ws = new WebSocket(url); const timeout=setTimeout(()=>reject(new Error('sin cierre')),5000);
    ws.onclose=e=>{clearTimeout(timeout); resolve(e.code);};
   });
  },{room:cd.compartidaId,csrf:users[1].token});
  assert.equal(unauthorized,1008);
  ok('Acceso directo a una sesion ajena rechazado');
  const badCsrf = await a.evaluate(async room => {
   return await new Promise((resolve,reject)=>{
    const ws = new WebSocket('ws://'+location.host+'/chat/'+room+'?csrf=incorrecto');
    const timeout=setTimeout(()=>reject(new Error('sin cierre')),5000);
    ws.onclose=e=>{clearTimeout(timeout); resolve(e.code);};
   });
  },ab.compartidaId);
  assert.equal(badCsrf,1008);
  ok('Conexion sin token CSRF valido rechazada');
  // Se mantiene una conexion valida del otro participante para probar revocacion.
  await b.reload(); await b.waitForFunction(()=>document.querySelector('.chat-status')?.textContent.startsWith('En línea'));
  await action(users[1],'salir');
  await b.evaluate(()=>window.__chatSocket.send('MNo debe registrarse'));
  await b.waitForFunction(()=>window.__chatSocket.readyState===3);
  await a.reload(); await a.waitForFunction(()=>document.querySelectorAll('.chat-message').length===2);
  ok('Un participante que salio no puede seguir publicando con un WebSocket previo');
  const out = path.join('docs','tdd-chat');
  fs.mkdirSync(out,{recursive:true});
  await c.emulateMedia({reducedMotion:"reduce"});
  await c.screenshot({path:path.join(out,'chat-desktop.png'),fullPage:true,animations:"disabled"});
  await c.setViewportSize({width:390,height:844});
  await c.screenshot({path:path.join(out,'chat-mobile.png'),fullPage:true,animations:"disabled"});
  assert.equal(await c.evaluate(()=>document.documentElement.scrollWidth > innerWidth),false);
  ok('Interfaz adaptable a movil sin desbordamiento horizontal');
  fs.writeFileSync(path.join(out,'aceptacion.txt'),results.join('\n')+'\n');
 } finally {
  await Promise.all(users.map(u=>u.context.close()));
  await browser.close();
 }
})().catch(error=>{console.error(error);process.exitCode=1;});
