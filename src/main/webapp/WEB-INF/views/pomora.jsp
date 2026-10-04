<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="es">
<head>
 <meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1">
 <meta name="theme-color" content="#29243b">
 <title>${esHistorial ? 'Historial' : 'Tu enfoque'} · Pomora</title>
 <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/pomora.css?v=compartida3">
 <script src="${pageContext.request.contextPath}/assets/pomora.js" defer></script>
</head>
<body>
<aside class="sidebar">
 <a class="brand" href="${pageContext.request.contextPath}/pomodoro"><img class="brand-logo" width="160" src="${pageContext.request.contextPath}/assets/pomora-logo.png" alt="Pomora"></a>
 <p class="nav-caption">TU ESPACIO</p>
 <nav aria-label="Navegación principal">
  <a class="nav-link ${!esHistorial ? 'selected' : ''}" href="${pageContext.request.contextPath}/pomodoro"><span aria-hidden="true">◷</span> Temporizador <span class="nav-marker"></span></a>
  <a class="nav-link ${esHistorial ? 'selected' : ''}" href="${pageContext.request.contextPath}/historial"><span aria-hidden="true">▤</span> Historial de estudio</a>
  <a class="nav-link" href="${pageContext.request.contextPath}/compartido"><span aria-hidden="true">♧</span> Estudio compartido</a>
 </nav><form class="logout-form" action="${pageContext.request.contextPath}/salir" method="post"><input type="hidden" name="csrf" value="${csrf}"><button type="submit">Cerrar sesión</button></form>
 <div class="sidebar-note"><span class="note-symbol">✦</span><p>El progreso empieza<br>con un pequeño paso.</p><span>Haz espacio para lo que importa.</span></div>
 <div class="profile"><span class="avatar">E</span><div><strong><c:out value="${sessionScope.nombreVisible}"/></strong><small>Espacio individual</small></div><span class="online-dot" title="Aplicación local"></span></div>
</aside>
<main class="workspace">
 <header class="topbar"><span>${enSesionCompartida && !esHistorial ? 'SESIÓN COMPARTIDA ACTIVA' : 'POMODORO INDIVIDUAL'}</span><span id="fechaHoy"></span></header>
 <c:if test="${not empty mensaje}"><div class="notice" role="alert"><c:out value="${mensaje}"/></div></c:if>
 <c:choose>
 <c:when test="${esHistorial}">
  <section class="page-heading"><p class="eyebrow">CADA BLOQUE CUENTA</p><h1>Tu camino de estudio<span>.</span></h1><p>Un registro de los momentos que dedicaste a concentrarte.</p></section>
  <section class="stats-row" aria-label="Resumen de estudio">
   <div class="stat"><span>Tiempo de concentración</span><strong><c:out value="${totalEstudiado}"/></strong><small>Minutos y segundos acumulados</small></div>
   <div class="stat"><span>Concentraciones completadas</span><strong><c:out value="${concentracionesCompletadas}"/></strong><small>Bloques con el tiempo cumplido</small></div>
   <div class="stat"><span>Sesiones registradas</span><strong><c:out value="${totalSesiones}"/></strong><small>De la más reciente a la primera</small></div>
  </section>
  <section class="history-card"><div class="section-title"><h2>Historial de sesiones</h2><span class="quiet-label">Hora de Colombia</span></div>
   <c:choose><c:when test="${empty historial}"><div class="empty-state"><span class="empty-icon">◷</span><h3>Tu primer bloque te espera</h3><p>Al iniciar el temporizador, tu sesión aparecerá aquí.</p><a class="button primary" href="${pageContext.request.contextPath}/pomodoro">Ir al temporizador <span>→</span></a></div></c:when>
   <c:otherwise><div class="table-scroll"><table class="session-table"><thead><tr><th>Inicio de sesión</th><th>Fin de sesión</th><th>Estado</th><th>Concentración</th></tr></thead><tbody>
    <c:forEach var="fila" items="${historial}"><tr><td><strong><c:out value="${fila.inicio}"/></strong></td><td><c:out value="${fila.fin}"/></td><td><span class="session-badge ${fila.estado}"><c:out value="${fila.estado}"/></span></td><td class="duration-cell"><c:out value="${fila.estudiado}"/></td></tr>
    <tr class="details-row"><td colspan="4"><details><summary>Ver bloques de esta sesión <span>↗</span></summary><table class="block-table"><thead><tr><th>Bloque</th><th>Tipo</th><th>Estado</th><th>Tiempo activo</th><th>Inicio</th><th>Fin</th></tr></thead><tbody><c:forEach var="bloque" items="${fila.bloques}"><tr><td><c:out value="${bloque.orden}"/></td><td><c:out value="${bloque.tipo}"/></td><td><c:out value="${bloque.estado}"/></td><td><c:out value="${bloque.tiempo}"/></td><td><c:out value="${bloque.inicio}"/></td><td><c:out value="${bloque.fin}"/></td></tr></c:forEach></tbody></table></details></td></tr>
    </c:forEach>
   </tbody></table></div></c:otherwise></c:choose>
  </section>
 </c:when>
 <c:when test="${enSesionCompartida}">
  <div class="shared-notice-layout">
  <section class="page-heading"><p class="eyebrow">UN MISMO RITMO</p><h1>Estás estudiando en compañía<span>.</span></h1><p>Tu temporizador pertenece a una sesión compartida activa.</p></section>
  <section class="timer-card shared-session-notice" aria-labelledby="sharedSessionTitle">
   <span class="shared-session-symbol" aria-hidden="true">♧</span>
   <h2 id="sharedSessionTitle">Tienes una sesión compartida activa</h2>
   <p>Consulta el tiempo y a tu compañero en la pantalla de estudio compartido. Aquí volverá a aparecer el temporizador individual cuando salgas de esa sesión.</p>
   <a class="button primary" href="${pageContext.request.contextPath}/compartido">Volver a mi sesión compartida <span>→</span></a>
   <p class="timer-hint">Tu sesión sigue en curso.</p>
  </section>
  <div class="shared-notice-details" aria-label="Estudiar en compañía">
   <div><span aria-hidden="true">◷</span><strong>Un ritmo común</strong><p>Concentración y descanso en el mismo momento.</p></div>
   <div><span aria-hidden="true">♧</span><strong>En buena compañía</strong><p>Comparte el enfoque, cada uno con su propia tarea.</p></div>
   <div><span aria-hidden="true">▤</span><strong>Tu progreso cuenta</strong><p>Tu tiempo de estudio permanece en tu historial.</p></div>
  </div>
  <p class="shared-notice-note">Un poco de compañía. Un bloque a la vez.</p>
  </div>
 </c:when>
 <c:otherwise>
  <section class="page-heading"><p class="eyebrow">MENOS RUIDO, MÁS ENFOQUE</p><h1>Tu momento de enfoque<span>.</span></h1><p>Una tarea. Un bloque. A tu ritmo.</p></section>
  <div class="focus-layout">
   <section class="timer-card" id="timerCard" data-url="${pageContext.request.contextPath}/pomodoro" data-csrf="${csrf}" data-session="${sesionId}" data-state="${estadoBloque}" data-type="${tipoBloque}" data-order="${ordenBloque}" data-remaining="${restanteMillis}" data-target="${objetivoMillis}">
    <div class="timer-card-head"><span class="session-indicator"><i></i> <span id="statusLabel">${estadoBloque == 'PAUSADO' ? 'En pausa' : (estadoBloque == 'EN_EJECUCION' ? 'En ejecución' : (estadoBloque == 'COMPLETADO' ? 'Bloque completado' : 'Listo para empezar'))}</span></span><span class="quiet-label" id="blockLabel">BLOQUE ${ordenBloque}</span></div>
    <div class="phase-strip" aria-label="Tipo de bloque"><span id="phaseFocus" class="${tipoBloque == 'CONCENTRACION' ? 'active' : ''}">Concentración</span><span id="phaseShort" class="${tipoBloque == 'DESCANSO_CORTO' ? 'active' : ''}">Descanso corto</span><span id="phaseLong" class="${tipoBloque == 'DESCANSO_LARGO' ? 'active' : ''}">Descanso largo</span></div>
    <div class="clock-wrap"><svg class="clock-ring" viewBox="0 0 300 300" aria-hidden="true"><circle class="ring-track" cx="150" cy="150" r="137"/><circle class="ring-progress" id="ringProgress" cx="150" cy="150" r="137"/></svg><div class="clock-content"><span class="clock-eyebrow" id="clockType">${tipoBloque == 'CONCENTRACION' ? 'TIEMPO DE ENFOQUE' : 'TIEMPO DE DESCANSO'}</span><strong id="clock" role="timer" aria-label="Tiempo restante">${tiempoPantalla}</strong><span class="clock-caption" id="clockCaption">${estadoBloque == 'PAUSADO' ? 'Tu tiempo está a salvo.' : 'Dedica este momento a una sola tarea.'}</span></div></div>
    <p class="sr-only" id="statusAnnouncement" aria-live="polite"></p>
    <form id="timerForm" action="${pageContext.request.contextPath}/pomodoro" method="post"><input type="hidden" name="csrf" value="${csrf}"><input type="hidden" name="sesionId" value="${sesionId}" id="sessionInput"><input type="hidden" name="accion" id="actionInput" value="${estadoBloque == 'PAUSADO' ? 'reanudar' : (estadoBloque == 'EN_EJECUCION' ? 'pausar' : (estadoBloque == 'COMPLETADO' ? 'siguiente' : 'iniciar'))}"><button class="button primary timer-button" id="mainAction" type="submit"><span id="mainActionIcon" aria-hidden="true">${estadoBloque == 'EN_EJECUCION' ? 'Ⅱ' : '▶'}</span> <span id="mainActionLabel">${estadoBloque == 'PAUSADO' ? 'Reanudar temporizador' : (estadoBloque == 'EN_EJECUCION' ? 'Pausar temporizador' : (estadoBloque == 'COMPLETADO' ? 'Iniciar siguiente bloque' : 'Iniciar temporizador'))}</span></button></form>
    <p class="timer-hint" id="timerHint">${estadoBloque == 'PAUSADO' ? 'Continúa cuando estés listo.' : 'Puedes hacer una pausa cuando lo necesites.'}</p>
    <c:if test="${hayActiva}"><div class="session-actions"><form method="post" action="${pageContext.request.contextPath}/pomodoro"><input type="hidden" name="csrf" value="${csrf}"><input type="hidden" name="sesionId" value="${sesionId}"><button name="accion" value="finalizar" class="text-button">Finalizar sesión</button></form><span>·</span><form method="post" action="${pageContext.request.contextPath}/pomodoro"><input type="hidden" name="csrf" value="${csrf}"><input type="hidden" name="sesionId" value="${sesionId}"><button name="accion" value="abandonar" class="text-button muted">Abandonar</button></form></div></c:if>
    <noscript><p class="connection-message">Activa JavaScript para ver la cuenta regresiva y completar los bloques automáticamente.</p></noscript>
    <p class="connection-message" id="connectionMessage" role="status"></p>
   </section>
   <aside class="focus-aside"><section class="rhythm-card"><span class="card-symbol">✳</span><p class="eyebrow">ENCUENTRA TU RITMO</p><h2>Pequeños bloques.<br>Grandes avances.</h2><p>Concentrarte también significa darte tiempo para descansar.</p><div class="rhythm-step"><span class="step-dot focus-dot"></span><span>Concentración</span><strong>25 min</strong></div><div class="rhythm-step"><span class="step-dot short-dot"></span><span>Descanso corto</span><strong>5 min</strong></div><div class="rhythm-step"><span class="step-dot long-dot"></span><span>Descanso largo</span><strong>15 min</strong></div><small>Un descanso largo cada 4 concentraciones completadas.</small></section>
   <section class="progress-card"><div class="section-title"><h2>Tu recorrido</h2><span>↗</span></div><div class="mini-stat"><span>Tiempo de estudio</span><strong>${totalEstudiado}</strong></div><div class="mini-stat"><span>Bloques de enfoque completos</span><strong>${concentracionesCompletadas}</strong></div><a href="${pageContext.request.contextPath}/historial" class="history-link">Consultar historial <span>→</span></a></section></aside>
  </div>
  <p class="bottom-note"><span>✦</span> No necesitas hacerlo todo ahora. Solo empieza con este bloque.</p>
 </c:otherwise>
 </c:choose>
 <footer class="page-footer"><span>Pomora · un espacio para concentrarte</span><span>Un bloque a la vez.</span></footer>
</main>
</body></html>
