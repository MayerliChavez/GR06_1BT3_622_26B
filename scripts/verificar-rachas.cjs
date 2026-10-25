const {chromium} = require('playwright');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const base = process.env.BASE_URL || 'http://127.0.0.1:18082';
(async()=>{
const browser=await chromium.launch({channel:'msedge',headless:true});
try{
 const context=await browser.newContext({viewport:{width:1440,height:1080}});
 const html=await (await context.request.get(base+'/registro')).text();
 const csrf=/name="csrf" value="([^"]+)"/.exec(html)[1];
 const correo='racha-'+crypto.randomUUID()+'@example.test', contrasena=crypto.randomUUID()+'Aa1!';
 await context.request.post(base+'/registro',{form:{csrf,nombre:'Prueba Racha',correo,contrasena}});
 await context.request.post(base+'/ingresar',{form:{csrf,correo,contrasena}});
 const page=await context.newPage(); const errors=[];
 page.on('pageerror',e=>errors.push(e.message));
 await page.goto(base+'/pomodoro');
 assert.equal(await page.locator('#rachaDias').textContent(),'0');
 const token=await page.locator('input[name=csrf]').first().inputValue();
 const act=async(accion,extra={})=>{
  const response=await context.request.post(base+'/pomodoro',{form:{csrf:token,accion,formato:'json',...extra}});
  assert.equal(response.status(),200); return response.json();
 };
 await context.request.post(base+'/pomodoro',{form:{csrf:token,accion:'guardarPreferencias',concentracion:'1',descansoCorto:'1',descansoLargo:'1',bloques:'4'}});
 let state=await act('iniciar');
 await act('finalizar',{sesionId:state.sesionId});
 assert.equal((await (await context.request.get(base+'/pomodoro?formato=json')).json()).rachaDias,0);
 console.log('OK: finalizar antes de tiempo no suma');
 state=await act('iniciar');
 await page.reload();
 console.log('Esperando un Pomodoro real de 60 segundos...');
 await page.waitForFunction(()=>document.querySelector('#timerCard').dataset.type==='DESCANSO_CORTO',null,{timeout:75000});
 await page.waitForFunction(()=>document.querySelector('#rachaDias').textContent==='1');
 assert.equal((await (await context.request.get(base+'/racha?formato=json')).json()).rachaDias,1);
 state=await act('finalizar',{sesionId:state.sesionId});
 assert.equal(state.rachaDias,1);
 await page.waitForFunction(()=>document.querySelector('#rachaDias').textContent==='1');
 console.log('OK: completar Pomodoro y finalizar actualiza contador sin recarga');
 for(const route of ['/historial','/compartido','/pomodoro']){
  await page.goto(base+route);
  assert.equal(await page.locator('#rachaDias').textContent(),'1');
 }
  await page.waitForSelector('.calendar-day.studied');
 const data=await (await context.request.get(base+'/racha?formato=json')).json();
 assert.ok(data.fechas.includes(data.hoy));
 assert.equal(await page.locator('.calendar-day.is-today.studied').count(),1);
 const originalMonth=await page.locator('#calendarMonth').textContent();
 await page.locator('#calendarPrevious').click();
 assert.notEqual(await page.locator('#calendarMonth').textContent(),originalMonth);
 await page.locator('#calendarToday').click();
 assert.equal(await page.locator('#calendarMonth').textContent(),originalMonth);
 await page.locator('.calendar-day.is-today').click();
 assert.ok((await page.locator('#calendarDetail').textContent()).includes('completaste concentración'));
 console.log('OK: fecha real marcada, navegación entre meses y detalle del día');
 await page.waitForFunction(()=>getComputedStyle(document.querySelector('.timer-card')).opacity === '1');
 await page.screenshot({path:'docs/tdd-rachas/racha-desktop.png',fullPage:true});
 await page.setViewportSize({width:390,height:844});
 await page.screenshot({path:'docs/tdd-rachas/racha-movil.png',fullPage:true});
 assert.ok(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth));

 const anonymous=await browser.newContext();
 assert.equal((await anonymous.request.get(base+'/racha?formato=json')).status(),401);
 await anonymous.close();
 const peer=await browser.newContext();
 const regHtml=await (await peer.request.get(base+'/registro')).text();
 const regToken=/name="csrf" value="([^"]+)"/.exec(regHtml)[1];
 const peerMail='cal-'+crypto.randomUUID()+'@example.test', peerPassword=crypto.randomUUID()+'Aa1!';
 await peer.request.post(base+'/registro',{form:{csrf:regToken,nombre:'Compañero Calendario',correo:peerMail,contrasena:peerPassword}});
 await peer.request.post(base+'/ingresar',{form:{csrf:regToken,correo:peerMail,contrasena:peerPassword}});
 const peerHtml=await (await peer.request.get(base+'/compartido')).text();
 const peerToken=/data-csrf="([^"]+)"/.exec(peerHtml)[1];
 await context.request.post(base+'/compartido',{form:{csrf:token,accion:'buscar',formato:'json'}});
 await peer.request.post(base+'/compartido',{form:{csrf:peerToken,accion:'buscar',formato:'json'}});
 await page.goto(base+'/compartido');
 assert.equal(await page.locator('#studyCalendar').count(),0);
 await page.waitForFunction(()=>document.querySelector('.chat-status')?.textContent.startsWith('En línea'));
 assert.equal(await page.locator('#sharedCard').getAttribute('data-session')===' ',false);
 await page.waitForFunction(()=>getComputedStyle(document.querySelector('.timer-card')).opacity === '1');
 await page.screenshot({path:'docs/tdd-rachas/chat-sin-calendario.png',fullPage:true});
 await context.request.post(base+'/compartido',{form:{csrf:token,accion:'salir',formato:'json'}});
 await page.reload();
 assert.equal(await page.locator('#studyCalendar').count(),1);
 console.log('OK: calendario oculto durante sesión compartida activa, vuelve al salir; endpoint protegido');


 assert.deepEqual(errors,[]);
 console.log('OK: contador persistente en temporizador, historial y compartido; móvil sin desbordamiento; sin errores JS');
}finally{await browser.close();}
})().catch(e=>{console.error(e);process.exitCode=1});

