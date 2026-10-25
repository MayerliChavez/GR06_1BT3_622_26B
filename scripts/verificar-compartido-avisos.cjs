const {chromium}=require('playwright'),assert=require('node:assert/strict');
const base='http://127.0.0.1:18083';
(async()=>{
 const browser=await chromium.launch({channel:'msedge',headless:true});
 try{
  const probe=await browser.newContext();
  for(let attempt=0;attempt<40;attempt++){
   try { if((await probe.request.get(base+'/ingresar')).status()===200) break; } catch(_) {}
   await new Promise(resolve=>setTimeout(resolve,500));
  }
  await probe.close();
  async function login(correo,mobile){
   const context=await browser.newContext({viewport:mobile?{width:390,height:844}:{width:1440,height:1080},reducedMotion:'reduce'});
   await context.addInitScript(()=>localStorage.setItem('pomora-sound','off'));
   const html=await(await context.request.get(base+'/ingresar')).text(),csrf=/name="csrf" value="([^"]+)"/.exec(html)[1];
   await context.request.post(base+'/ingresar',{form:{csrf,correo,contrasena:'PruebaAvisos123!'}});
   const page=await context.newPage();await page.goto(base+'/compartido');
   return{context,page};
  }
  const users=await Promise.all([login('ana-avisos@example.test',false),login('luis-avisos@example.test',true)]);
  if(process.env.MODE==='before'){
   for(const user of users){
    const csrf=await user.page.locator('#sharedCard').getAttribute('data-csrf');
    const state=await(await user.context.request.post(base+'/compartido',{form:{accion:'actualizar',csrf,formato:'json'}})).json();
    assert.ok(state.notificacion);
    await user.page.waitForTimeout(3500);
    assert.equal(await user.page.locator('.completion-toast.visible').count(),0);
   }
   console.log('REPRODUCIDO: servidor envía el aviso del bloque inicial pero ninguno de los dos participantes muestra la tarjeta.');
   return;
  }
  await Promise.all(users.map(async(user,i)=>{
   await user.page.waitForSelector('.completion-toast.visible[data-type=CONCENTRACION]',{timeout:10000});
   await user.page.waitForFunction(()=>document.querySelector('.completion-art')?.complete&&document.querySelector('.completion-art').naturalWidth>0);
   assert.equal(await user.page.locator('.completion-backdrop.visible').count(),1);
   assert.ok((await user.page.locator('.completion-art').getAttribute('src')).endsWith('concentracion.png'));
   assert.equal(await user.page.locator('.completion-toast button').count(),0);
   assert.ok((await user.page.locator('.completion-duration').textContent()).includes('1 minuto y 30 segundos'));
   await user.page.screenshot({path:'docs/tdd-notificaciones/compartido-inicio-'+i+'.png'});
   console.log('OK participante '+i+': tarjeta inicial con imagen, oscurecimiento y duración real.');
   await user.page.waitForFunction(()=>!document.querySelector('.completion-toast').classList.contains('visible'),null,{timeout:16000});
   await user.page.waitForTimeout(3500);
   assert.equal(await user.page.locator('.completion-toast.visible').count(),0);
  }));
  await users[0].page.reload();
  await users[0].page.waitForTimeout(3500);
  assert.equal(await users[0].page.locator('.completion-toast.visible').count(),0);
  console.log('OK: mismo bloque no se repite por sondeo ni al recargar.');
  await Promise.all(users.map(async(user,i)=>{
   await user.page.waitForSelector('.completion-toast.visible[data-type=DESCANSO_LARGO]',{timeout:110000});
   assert.ok((await user.page.locator('.completion-art').getAttribute('src')).endsWith('descanso-largo.png'));
   assert.equal(await user.page.locator('.completion-backdrop.visible').count(),1);
   assert.ok((await user.page.locator('.completion-duration').textContent()).includes('25 segundos'));
   console.log('OK participante '+i+': nueva tarjeta al cambiar realmente a descanso.');
  }));
 }finally{await browser.close();}
})().catch(e=>{console.error(e);process.exitCode=1;});

