// Verifica audio y transiciones con respuestas simuladas, sin cambiar tiempos reales.
const fs = require('node:fs'), vm = require('node:vm'), assert = require('node:assert/strict'), path = require('node:path');
const asset = file => fs.readFileSync(path.join(__dirname, '../src/main/webapp/assets', file), 'utf8');
const node = () => ({dataset:{},style:{setProperty(){}},classList:{add(){},remove(){},toggle(){}},setAttribute(){},append(){},addEventListener(){},textContent:'',value:''});
async function audioTest() {
 const handlers={}, buttons={}, storage=new Map(), notes=[], levels=[];
 const toggle=node(); toggle.addEventListener=(name,fn)=>buttons.toggle=fn;
 class Audio {
  constructor(){this.state='suspended';this.currentTime=0;this.destination={};}
  async resume(){this.state='running';}
  createOscillator(){return {frequency:{},connect(){},disconnect(){},start(time){notes.push(time);},stop(){}};}
  createGain(){return {gain:{setValueAtTime(){},linearRampToValueAtTime(value){levels.push(value);},exponentialRampToValueAtTime(){}},connect(){},disconnect(){}};}
 }
 const context={window:{AudioContext:Audio},matchMedia:()=>({matches:true}),localStorage:{getItem:key=>storage.get(key)??null,setItem:(k,v)=>storage.set(k,v)},setTimeout(){},clearTimeout(){},
  document:{body:{append(){}},createElement:node,addEventListener:(n,f)=>handlers[n]=f,querySelectorAll:selector=>selector==='.sound-toggle'?[toggle]:[]}};
 vm.runInNewContext(asset('pomora-ui.js'),context);
 await buttons.toggle(); assert.equal(notes.length,0); assert.equal(toggle.textContent,'♪ Sonido activo');
 context.window.PomoraUI.blockEnded({key:'individual:1',type:'CONCENTRACION'}); assert.equal(notes.length,3);
 context.window.PomoraUI.blockEnded({key:'individual:1',type:'CONCENTRACION'}); assert.equal(notes.length,3);
 context.window.PomoraUI.blockEnded({key:'compartida:2',type:'DESCANSO_CORTO'}); assert.equal(notes.length,6);
 await buttons.toggle(); context.window.PomoraUI.blockEnded({key:'compartida:3',type:'DESCANSO_LARGO'}); assert.equal(notes.length,6);
 assert.ok(levels.every(level=>level<=.06));
 console.log('OK: alarma de tres notas, concentración y descanso, volumen suave, silencio y ausencia de duplicados');
}
async function transitionTest(shared) {
 const nodes={}, intervals=[], ended=[]; const get=id=>nodes[id] ||= node();
 const card=get(shared?'sharedCard':'timerCard');
 card.dataset={session:'sesion-prueba',state:'EN_EJECUCION',type:'CONCENTRACION',order:'1',remaining:'10000',target:'10000',url:'/prueba',csrf:'token',waiting:'false'};
 const next=shared?{compartidaId:'sesion-prueba',esperando:false,orden:2,tipo:'DESCANSO_CORTO',restanteMillis:300000,objetivoMillis:300000,presente:true}
  :{sesionId:'sesion-prueba',estado:'COMPLETADO',orden:1,tipo:'CONCENTRACION',restanteMillis:0,objetivoMillis:10000};
 const context={window:{PomoraUI:{blockEnded:event=>ended.push(event)},addEventListener(){},location:{reload(){throw Error('Recarga inesperada');}}},document:{getElementById:get,addEventListener(){}},
  performance:{now:()=>0},Intl,URLSearchParams,AbortSignal:{timeout(){}},fetch:async()=>({ok:true,json:async()=>next}),setInterval:(fn,time)=>{intervals.push({fn,time});},location:{reload(){throw Error('Recarga inesperada');}}};
 vm.runInNewContext(asset(shared?'compartido.js':'pomora.js'),context);
 await new Promise(resolve=>setImmediate(resolve));
 await intervals.find(i=>i.time===3000).fn();
 await intervals.find(i=>i.time===3000).fn();
 assert.equal(ended.length,1); assert.equal(ended[0].key,'sesion-prueba:1'); assert.equal(ended[0].type,'CONCENTRACION');
 console.log(`OK: ${shared?'compartido':'individual'} avisa una vez tras la finalización confirmada por el servidor`);
}
(async()=>{await audioTest();await transitionTest(false);await transitionTest(true);})().catch(error=>{console.error(error);process.exitCode=1;});
