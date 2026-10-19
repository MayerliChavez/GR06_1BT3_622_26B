const fs = require('node:fs');
const vm = require('node:vm');
const assert = require('node:assert/strict');
const listeners = new Map(), storage = new Map();
let notes = 0, audio;
const soundButton = {textContent:'', setAttribute(){}, addEventListener(){}};
const context = {
 console, Date, Set, Map, JSON,
 matchMedia:()=>({matches:true}),
 localStorage:{getItem:k=>storage.get(k)??null,setItem:(k,v)=>storage.set(k,v)},
 setTimeout:()=>0,clearTimeout(){},
 document:{body:{append(){}},createElement:()=>({className:'',setAttribute(){},classList:{add(){},remove(){}}}),
 querySelectorAll:selector=>selector==='.sound-toggle'?[soundButton]:[],
 addEventListener:(type,fn)=>listeners.set(type,fn)},
 window:{AudioContext:class {constructor(){this.state='suspended';this.currentTime=0;audio=this;}
 async resume(){this.state='running';}
 createOscillator(){notes++;return {frequency:{},connect(){},start(){},stop(){},disconnect(){}};}
 createGain(){return {gain:{setValueAtTime(){},linearRampToValueAtTime(){},exponentialRampToValueAtTime(){}},connect(){},disconnect(){}};}}}
};
vm.runInNewContext(fs.readFileSync('src/main/webapp/assets/pomora-ui.js','utf8'),context);
const settle=async()=>{for(let n=0;n<5;n++)await new Promise(setImmediate);};
(async()=>{
 context.window.PomoraUI.blockEnded({key:'prueba:1',type:'CONCENTRACION'});
 await settle();
 assert.equal(notes,0);
 assert.equal(storage.has('pomora-last-bells'),false,'No marcar como reproducido cuando falta permiso de audio');
 listeners.get('pointerdown')({target:{closest:()=>false}});
 await settle();
 assert.equal(notes,3,'Concentracion debe programar la alarma al activar audio');
 audio.state='suspended';
 context.window.PomoraUI.blockEnded({key:'prueba:2',type:'DESCANSO_CORTO'});
 await settle();
 assert.equal(notes,6,'Descanso debe recuperar audio suspendido y programar la misma alarma');
 context.window.PomoraUI.blockEnded({key:'prueba:2',type:'DESCANSO_CORTO'});
 await settle();
 assert.equal(notes,6,'El mismo bloque no debe sonar dos veces');
 console.log('OK: aviso de concentracion, aviso de descanso, recuperacion de audio y ausencia de duplicados');
})().catch(error=>{console.error(error);process.exitCode=1;});
