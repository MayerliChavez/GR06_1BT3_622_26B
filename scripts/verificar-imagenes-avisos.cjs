const {chromium}=require('playwright');
const assert=require('node:assert/strict');
const base='http://127.0.0.1:18084';
(async()=>{
 const browser=await chromium.launch({channel:'msedge',headless:true});
 try{
  const context=await browser.newContext({viewport:{width:1440,height:1080},reducedMotion:'reduce'});
  const html=await(await context.request.get(base+'/registro')).text();
  const csrf=/name="csrf" value="([^"]+)"/.exec(html)[1];
  const correo='ilustraciones-'+crypto.randomUUID()+'@example.test',contrasena=crypto.randomUUID()+'Aa1!';
  await context.request.post(base+'/registro',{form:{csrf,nombre:'Vista de avisos',correo,contrasena}});
  await context.request.post(base+'/ingresar',{form:{csrf,correo,contrasena}});
  const page=await context.newPage(),errors=[];page.on('pageerror',e=>errors.push(e.message));
  await page.addInitScript(()=>localStorage.setItem('pomora-sound','off'));
  await page.goto(base+'/pomodoro');
  await page.locator('#mainAction').click();
  await page.waitForSelector('.completion-toast.visible[data-type=CONCENTRACION]');
  async function verify(type,file,label){
   await page.waitForFunction(()=>{const image=document.querySelector('.completion-art');return image?.complete&&image.naturalWidth>0;});
   const image=page.locator('.completion-art'),card=page.locator('.completion-card');
   assert.ok((await image.getAttribute('src')).endsWith(file));
   const artRect=await image.boundingBox(),cardRect=await card.boundingBox();
   assert.ok(artRect.y<cardRect.y&&artRect.y+artRect.height<=cardRect.y+15);
   const overlay=await page.locator('.completion-backdrop').evaluate(el=>{
    const r=el.getBoundingClientRect(),s=getComputedStyle(el);
    return{x:r.x,y:r.y,w:r.width,h:r.height,opacity:s.opacity,color:s.backgroundColor,pointer:s.pointerEvents};
   });
   assert.equal(overlay.x,0);assert.equal(overlay.y,0);
   assert.equal(overlay.w,page.viewportSize().width);assert.equal(overlay.h,page.viewportSize().height);
   assert.equal(overlay.opacity,'1');assert.equal(overlay.pointer,'none');
   assert.ok(overlay.color.includes('0.48'));
   assert.equal(await card.locator('button').count(),0);
   await page.screenshot({path:'docs/tdd-notificaciones/ilustracion-'+label+'.png',fullPage:false});
   console.log('OK: '+type+' imagen correcta encima de tarjeta, aplicación oscurecida y sin botones');
  }
  await verify('CONCENTRACION','concentracion.png','concentracion');
  await page.locator('input[name=concentracion]').fill('2');
  await page.locator('input[name=concentracion]').focus();
  const before=await page.locator('#clock').textContent();
  await page.waitForFunction(text=>document.querySelector('#clock').textContent!==text,before,{timeout:4000});
  assert.equal(await page.locator('input[name=concentracion]').evaluate(el=>el===document.activeElement),true);
  await page.waitForFunction(()=>!document.querySelector('.completion-toast').classList.contains('visible'),null,{timeout:16000});
  assert.equal(await page.locator('.completion-backdrop').evaluate(el=>getComputedStyle(el).opacity),'0');
  for(const [type,file,label] of [
   ['DESCANSO_CORTO','descanso-corto.png','descanso-corto'],
   ['DESCANSO_LARGO','descanso-largo.png','descanso-largo']
  ]){
   await page.evaluate(type=>window.PomoraUI.blockStarted({notification:{id:crypto.randomUUID(),tipoBloque:type,
    mensaje:'¡El descanso ha comenzado! Disfruta tu tiempo',posicion:'CENTRO_APLICACION',duracionMillis:12500,duracionBloqueMillis:type==='DESCANSO_CORTO'?300000:900000}}),type);
   await verify(type,file,label);
   await page.waitForFunction(()=>!document.querySelector('.completion-toast').classList.contains('visible'),null,{timeout:16000});
   assert.equal(await page.locator('.completion-backdrop').evaluate(el=>getComputedStyle(el).opacity),'0');
  }
  await page.setViewportSize({width:390,height:844});
  await page.evaluate(()=>window.PomoraUI.blockStarted({notification:{id:crypto.randomUUID(),tipoBloque:'DESCANSO_CORTO',
   mensaje:'¡El descanso ha comenzado! Disfruta tu tiempo',posicion:'CENTRO_APLICACION',duracionMillis:12500,duracionBloqueMillis:300000}}));
  await verify('DESCANSO_CORTO','descanso-corto.png','movil');
  assert.ok(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth));
  assert.deepEqual(errors,[]);
  console.log('OK: fondo desaparece automáticamente, escritura y reloj continúan, móvil sin desbordamiento y sin errores JS');
 }finally{await browser.close();}
})().catch(e=>{console.error(e);process.exitCode=1;});

