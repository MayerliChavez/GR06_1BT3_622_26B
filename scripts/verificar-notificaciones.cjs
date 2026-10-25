const {chromium}=require('playwright');
const assert=require('node:assert/strict');
const base=process.env.BASE_URL||'http://127.0.0.1:18083';
const rest='¡El descanso ha comenzado! Disfruta tu tiempo', focus='Es hora de concentrarse. ¡Mucho éxito!';
(async()=>{
 const browser=await chromium.launch({channel:'msedge',headless:true});
 const errors=[];
 try{
  async function login(mail,mobile=false){
   const context=await browser.newContext({viewport:mobile?{width:390,height:844}:{width:1440,height:1080},reducedMotion:'reduce'});
   const html=await(await context.request.get(base+'/ingresar')).text();
   const csrf=/name="csrf" value="([^"]+)"/.exec(html)[1];
   await context.request.post(base+'/ingresar',{form:{csrf,correo:mail,contrasena:'PruebaAvisos123!'}});
      const page=await context.newPage();
   await page.addInitScript(()=>{
    window.audioEvidence=[];
    const Audio=window.AudioContext||window.webkitAudioContext;
    if(!Audio)return;
    const create=Audio.prototype.createOscillator;
    Audio.prototype.createOscillator=function(){
     const node=create.call(this),start=node.start;
     node.start=function(...args){window.audioEvidence.push({frequency:this.frequency.value,type:this.type});return start.apply(this,args);};
     return node;
    };
   });
   page.on('pageerror',e=>errors.push(e.message));
   return{context,page};
  }
  const [a,b,individual]=await Promise.all([
   login('ana-avisos@example.test'),login('luis-avisos@example.test',true),login('individual-avisos@example.test')]);
  await Promise.all([a.page.goto(base+'/compartido'),b.page.goto(base+'/compartido'),individual.page.goto(base+'/pomodoro')]);
  for(const user of [a,b,individual]){
   await user.page.evaluate(()=>{
    window.notices=[];
    const el=document.querySelector('.completion-toast');
    new MutationObserver(()=>{
     if(el.classList.contains('visible') && el.textContent && window.notices.at(-1)!==el.querySelector(".completion-message")?.textContent)window.notices.push(el.querySelector(".completion-message")?.textContent);
    }).observe(el,{attributes:true,childList:true,characterData:true,subtree:true});
   });
  }
  const token=await individual.page.locator('input[name=csrf]').first().inputValue();
  await individual.context.request.post(base+'/pomodoro',{form:{csrf:token,accion:'guardarPreferencias',concentracion:'1',descansoCorto:'1',descansoLargo:'1',bloques:'1'}});
  await individual.page.reload();
  await individual.page.evaluate(()=>{
   window.notices=[];
   const el=document.querySelector('.completion-toast');
   new MutationObserver(()=>{if(el.classList.contains('visible')&&el.textContent&&window.notices.at(-1)!==el.querySelector(".completion-message")?.textContent)window.notices.push(el.querySelector(".completion-message")?.textContent);}).observe(el,{attributes:true,childList:true,characterData:true,subtree:true});
  });
  await individual.page.locator('#mainAction').click();
  await individual.page.waitForFunction(()=>document.querySelector('#timerCard').dataset.state==='EN_EJECUCION');
  console.log('OK: dos participantes compartidos y una sesión individual reales iniciados');
  async function verify(user,label,shared){
   const page=user.page, input=shared?'#chatText':'input[name=concentracion]';
   if(await page.locator('.sound-toggle').getAttribute('aria-pressed')!=='true') await page.locator('.sound-toggle').click();
   await page.locator(input).fill(shared?'Sigo estudiando sin interrupciones':'2');
   await page.locator(input).focus();
   await page.waitForFunction(text=>document.querySelector('.completion-toast.visible .completion-message')?.textContent===text,rest,{timeout:110000});
   const rect=await page.locator('.completion-toast').boundingBox(),size=page.viewportSize();
      const center=await page.evaluate(()=>{const r=document.querySelector('.workspace').getBoundingClientRect();return(Math.max(0,r.left)+Math.min(innerWidth,r.right))/2;});
   assert.ok(Math.abs(rect.x+rect.width/2-center)<3);
   assert.ok(Math.abs(rect.y+rect.height/2-size.height/2)<3);
   assert.equal(await page.locator('.completion-toast button').count(),0);
   const text=await page.locator('.completion-duration').textContent();
   assert.ok(text.includes(shared?'25 segundos':'1 minuto'));
   assert.equal(await page.locator('.completion-toast').evaluate(el=>getComputedStyle(el).pointerEvents),'none');
   assert.equal(await page.locator(input).evaluate(el=>el===document.activeElement),true);
   assert.equal(await page.locator(input).inputValue(),shared?'Sigo estudiando sin interrupciones':'2');
   const clock=shared?'#sharedClock':'#clock',before=await page.locator(clock).textContent();
   await page.waitForFunction(({selector,before})=>document.querySelector(selector).textContent!==before,{selector:clock,before},{timeout:4000});
   await page.screenshot({path:'docs/tdd-notificaciones/'+label+'-descanso.png',fullPage:true});
   await page.waitForFunction(()=>!document.querySelector('.completion-toast').classList.contains('visible'),null,{timeout:16000});
   assert.equal(await page.locator('.completion-toast').textContent(),'');
   console.log('OK '+label+': 3.2 descanso, 3.3 foco y temporizador continúan, 3.4 ocultación a 12.5 segundos, tarjeta centrada, duración real y sin botones');
   await page.waitForFunction(text=>document.querySelector('.completion-toast.visible .completion-message')?.textContent===text,focus,{timeout:80000});
   await page.screenshot({path:'docs/tdd-notificaciones/'+label+'-concentracion.png',fullPage:true});
   const notices=await page.evaluate(()=>window.notices);
   assert.equal(notices.filter(t=>t===rest).length,1);
   const n=await page.evaluate(()=>window.notices.length);
   await page.waitForFunction(()=>!document.querySelector('.completion-toast').classList.contains('visible'),null,{timeout:16000});
   assert.equal(await page.evaluate(()=>window.notices.length),n);
   assert.ok(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth));
   console.log('OK '+label+': 3.1 concentración y sin duplicados durante sincronización');
  }
  await Promise.all([verify(a,'compartido-ana',true),verify(b,'compartido-luis-movil',true),verify(individual,'individual',false)]);
  // Se comprueba el tercer perfil (descanso corto) como componente UI; la integración Java cubre su transición real.
  await individual.page.evaluate(message=>window.PomoraUI.blockStarted({notification:{
   id:'verificacion-sonido-corto',mensaje:message,posicion:'CENTRO_APLICACION',duracionMillis:12500,
   tipoBloque:'DESCANSO_CORTO',duracionBloqueMillis:300000
  }}),rest);
  await individual.page.waitForFunction(()=>window.audioEvidence.some(n=>n.type==='triangle'));
  const audio=await individual.page.evaluate(()=>window.audioEvidence);
  assert.ok(audio.some(n=>Math.abs(n.frequency-987.77)<.1&&n.type==='sine'));
  assert.ok(audio.some(n=>Math.abs(n.frequency-349.23)<.1&&n.type==='sine'));
  assert.ok(audio.some(n=>Math.abs(n.frequency-783.99)<.1&&n.type==='triangle'));
  assert.ok((await individual.page.locator('.completion-duration').textContent()).includes('5 minutos'));
  await individual.page.screenshot({path:'docs/tdd-notificaciones/tarjeta-descanso-corto.png',fullPage:false});
  console.log('OK: tres perfiles de sonido distintos programados en Web Audio; descanso corto muestra 5 minutos');
  // El mismo identificador sigue consumido después de recargar.
  await individual.page.reload();
  await individual.page.waitForFunction(()=>document.querySelector('#timerCard').dataset.state==='EN_EJECUCION');
  assert.equal(await individual.page.locator('.completion-toast.visible').count(),0);
  const id=await individual.page.locator('#timerCard').getAttribute('data-session');
  await individual.context.request.post(base+'/pomodoro',{form:{csrf:token,accion:'finalizar',sesionId:id,formato:'json'}});
  const closed=await(await individual.context.request.get(base+'/pomodoro?formato=json')).json();
  assert.equal(closed.notificacion,null);
  assert.deepEqual(errors,[]);
  console.log('OK: sin aviso de sesión cerrada, recarga sin repetir y sin errores JavaScript');
 }finally{await browser.close();}
})().catch(e=>{console.error(e);process.exitCode=1;});

