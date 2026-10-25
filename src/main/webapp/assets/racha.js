"use strict";
(() => {
 const root = document.getElementById("studyCalendar");
 if (!root) return;
 const monthTitle = document.getElementById("calendarMonth"), grid = document.getElementById("calendarDays");
 const status = document.getElementById("calendarStatus"), detail = document.getElementById("calendarDetail");
 const formatMonth = new Intl.DateTimeFormat("es", {month:"long",year:"numeric",timeZone:"UTC"});
 const formatDay = new Intl.DateTimeFormat("es", {day:"numeric",month:"long",year:"numeric",timeZone:"UTC"});
 let today, month, dates = new Set(), busy = false, selected = null, signature = "";
 const iso = date => date.toISOString().slice(0,10);
 const fromIso = value => new Date(value + "T12:00:00Z");
 function render() {
  monthTitle.textContent = formatMonth.format(month);
  grid.replaceChildren();
  const year = month.getUTCFullYear(), index = month.getUTCMonth();
  const first = new Date(Date.UTC(year,index,1,12)), offset = (first.getUTCDay()+6)%7;
  const last = new Date(Date.UTC(year,index+1,0,12)).getUTCDate();
  for(let i=0;i<offset;i++) {
   const blank=document.createElement("span"); blank.className="calendar-empty"; grid.append(blank);
  }
  for(let day=1;day<=last;day++) {
   const date=new Date(Date.UTC(year,index,day,12)), key=iso(date), studied=dates.has(key);
   const button=document.createElement("button"); button.type="button"; button.textContent=day;
   button.dataset.date=key; button.className="calendar-day";
   button.classList.toggle("studied",studied); button.classList.toggle("is-today",key===today);
   button.classList.toggle("is-selected",key===selected);
   button.setAttribute("aria-label",formatDay.format(date)+(studied?", concentración completada":", sin concentración completada")+(key===today?", hoy":""));
   button.setAttribute("aria-pressed",String(key===selected));
   if(key===today) button.setAttribute("aria-current","date");
   if(key>today) { button.disabled=true; button.classList.add("future"); }
   button.addEventListener("click",()=>{
    selected=key;
    detail.textContent=formatDay.format(date)+": "+(studied?"completaste concentración. Este día cuenta una sola vez.":"no hay concentración completada registrada.");
    render();
   });
   grid.append(button);
  }
  const next = new Date(Date.UTC(year,index+1,1,12));
  document.getElementById("calendarNext").disabled=iso(next)>today;
 }
 async function sync() {
  if(busy || document.hidden) return; busy=true;
  try {
   const response=await fetch(root.dataset.url,{cache:"no-store",signal:AbortSignal.timeout(8000)});
   if(response.status===401) { status.textContent="Inicia sesión para consultar tu calendario."; return; }
   if(!response.ok) throw new Error();
   const data=await response.json();
   document.getElementById("rachaDias").textContent=data.rachaDias;
   document.getElementById("rachaUnidad").textContent=data.rachaDias===1?"día":"días";
   const changed=signature!==JSON.stringify([data.hoy,data.fechas]);
   signature=JSON.stringify([data.hoy,data.fechas]); today=data.hoy; dates=new Set(data.fechas);
   if(!month) { const current=fromIso(today); month=new Date(Date.UTC(current.getUTCFullYear(),current.getUTCMonth(),1,12)); }
   status.textContent=dates.has(today)?"✓ Hoy ya dejaste tu huella":"Tu próximo Pomodoro puede marcar hoy";
   if(changed) render();
  } catch(_) { status.textContent="No se pudo actualizar el calendario. Reintentando…"; }
  finally { busy=false; }
 }
 // Clase  que se encarga de las rachas del sistema
 document.getElementById("calendarPrevious").addEventListener("click",()=>{if(!month)return;month.setUTCMonth(month.getUTCMonth()-1);render();});
 document.getElementById("calendarNext").addEventListener("click",()=>{if(!month)return;month.setUTCMonth(month.getUTCMonth()+1);render();});
 document.getElementById("calendarToday").addEventListener("click",()=>{if(!today)return;const current=fromIso(today);month=new Date(Date.UTC(current.getUTCFullYear(),current.getUTCMonth(),1,12));render();});
 document.addEventListener("visibilitychange",()=>{if(!document.hidden)sync();});
 document.addEventListener("pomora:racha",sync);
 window.addEventListener("online",sync);
 sync();setInterval(sync,10000);
})();
