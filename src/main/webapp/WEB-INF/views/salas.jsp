<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>

<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width,initial-scale=1">
    <meta name="theme-color" content="#29243b">

    <title>Salas privadas · Pomora</title>

    <link rel="stylesheet"
          href="${pageContext.request.contextPath}/assets/pomora.css?v=salas3">

    <script src="${pageContext.request.contextPath}/assets/pomora-ui.js?v=salas2"
            defer></script>

    <script src="${pageContext.request.contextPath}/assets/salas.js?v=5"
            defer></script>

    <script src="${pageContext.request.contextPath}/assets/sala-ciclo.js?v=3"
            defer></script>

    <link rel="stylesheet"
          href="${pageContext.request.contextPath}/assets/chat.css?v=salas14">

    <script src="${pageContext.request.contextPath}/assets/chat-sala.js?v=2"
            defer></script>
</head>

<body>
<aside class="sidebar">
    <a class="brand"
       href="${pageContext.request.contextPath}/pomodoro">
        <img class="brand-logo"
             width="160"
             src="${pageContext.request.contextPath}/assets/pomora-logo-menu.png"
             alt="Pomora">
    </a>

    <p class="nav-caption">TU ESPACIO</p>

    <nav aria-label="Navegación principal">
        <a class="nav-link"
           href="${pageContext.request.contextPath}/pomodoro">
            <span aria-hidden="true">◷</span> Temporizador
        </a>

        <a class="nav-link"
           href="${pageContext.request.contextPath}/historial">
            <span aria-hidden="true">▤</span> Historial de estudio
        </a>

        <a class="nav-link"
           href="${pageContext.request.contextPath}/compartido">
            <span aria-hidden="true">♧</span> Estudio compartido
        </a>

        <a class="nav-link selected"
           href="${pageContext.request.contextPath}/salas"
           aria-current="page">
            <span aria-hidden="true"><%@ include file="icono-sala-privada.jspf" %></span> Salas privadas
            <span class="nav-marker"></span>
        </a>
    </nav>

    <div class="sidebar-note">
        <img class="custom-note-icon"
             src="${pageContext.request.contextPath}/assets/mi_icono_pomodora.png"
             alt="Pomora">
        <p>El progreso empieza<br>con un pequeño paso.</p>
        <span>Haz espacio para lo que importa.</span>
    </div>

    <details class="profile-menu">
        <summary class="profile" aria-label="Abrir opciones de mi perfil">
            <span class="avatar">E</span>
            <span class="profile-copy">
                <strong class="profile-name"><c:out value="${sessionScope.nombreVisible}"/></strong>
                <small>Mi espacio de estudio</small>
            </span>
            <span class="profile-chevron" aria-hidden="true">⌃</span>
        </summary>
        <div class="profile-dropdown">
            <span class="profile-dropdown-label">TU CUENTA</span>
            <strong><c:out value="${sessionScope.nombreVisible}"/></strong>
            <a href="${pageContext.request.contextPath}/historial">Ver mi historial <span>↗</span></a>
            <form class="logout-form"
                  action="${pageContext.request.contextPath}/salir"
                  method="post">
                <input type="hidden" name="csrf" value="${csrf}">
                <button type="submit">Cerrar sesión <span>→</span></button>
            </form>
        </div>
    </details>
</aside>

<main class="workspace">
    <header class="topbar">
        <span>SALAS PRIVADAS</span>
        <a href="${pageContext.request.contextPath}/historial">Ver tu historial →</a>
    </header>

    <section class="page-heading">
        <p class="eyebrow">ESTUDIA CON TU GRUPO</p>
        <h1>Tu sala privada<span>.</span></h1>
        <p>Reúnete con los compañeros que tú elijas.</p>
    </section>

    <c:if test="${not empty mensaje}">
        <p class="notice" role="alert">
            <c:out value="${mensaje}"/>
        </p>
    </c:if>

    <c:choose>
        <c:when test="${not empty sala}">
            <section class="room-bar">
                <div class="room-bar-title">
                    <p class="eyebrow">
                        <i class="room-live-dot" aria-hidden="true"></i> SALA ABIERTA
                    </p>
                    <h2>Tu sala de estudio</h2>
                    <span class="room-role">${esResponsable ? 'Responsable' : 'Participante'}</span>
                </div>

                <div class="room-bar-code">
                    <label for="codigoSala">Código de acceso</label>
                    <div class="room-code-row">
                        <input id="codigoSala"
                               class="room-code-display"
                               type="text"
                               readonly
                               value="${sala.codigo}"
                               aria-label="Código de acceso de seis caracteres">

                        <button class="button primary"
                                id="copiarCodigoSala"
                                type="button">
                            Copiar código
                        </button>
                    </div>
                    <p id="resultadoCopiar"
                       class="room-copy-status"
                       role="status"
                       aria-live="polite">Comparte este código con tus compañeros.</p>
                </div>

                <c:if test="${esResponsable}">
                    <button id="abrirCierreSala"
                            class="button secondary room-close"
                            type="button">
                        Cerrar sala
                    </button>

                    <dialog id="confirmarCierreSala" class="room-dialog">
                        <form action="${pageContext.request.contextPath}/salas"
                              method="post">
                            <input type="hidden"
                                   name="csrf"
                                   value="${csrf}">
                            <input type="hidden"
                                   name="salaId"
                                   value="${sala.id}">
                            <input type="hidden"
                                   name="confirmado"
                                   value="true">

                            <span class="card-symbol" aria-hidden="true"><%@ include file="icono-sala-privada.jspf" %></span>
                            <p>
                                ¿Deseas cerrar la sala para todos los participantes?
                            </p>
                            <small>El tiempo de concentración de cada uno quedará guardado en su historial.</small>

                            <div class="room-dialog-actions">
                                <button id="cancelarCierreSala"
                                        class="button secondary"
                                        type="button">
                                    Cancelar
                                </button>

                                <button class="button primary"
                                        type="submit"
                                        name="accion"
                                        value="cerrarSala">
                                    Aceptar
                                </button>
                            </div>
                        </form>
                    </dialog>
                </c:if>
            </section>

            <div class="room-layout">
                <jsp:include page="salas-chat.jsp"/>

                <section class="timer-card room-timer">
                    <div class="timer-card-head">
                        <span class="session-indicator"><i></i> Ciclo del grupo</span>
                        <span class="quiet-label">25 · 5 · 15 MIN</span>
                    </div>

                    <jsp:include page="salas-ciclo.jsp"/>
                </section>

                <section class="waiting-card">
                    <div id="esperaSala"
                         data-url="${pageContext.request.contextPath}/salas?formato=espera">
                        <jsp:include page="salas-espera.jsp"/>
                    </div>

                    <p id="conexionEspera" class="connection-message" role="status"></p>
                </section>
            </div>
        </c:when>

        <c:when test="${not empty pendiente}">
            <div class="room-options">
                <article class="room-card">
                    <span class="card-symbol" aria-hidden="true">◷</span>
                    <p class="eyebrow">SOLICITUD ENVIADA</p>
                    <h2 id="estadoSolicitudSala"
                        data-url="${pageContext.request.contextPath}/salas?formato=estadoSolicitud"
                        data-destino="${pageContext.request.contextPath}/salas">Sala de espera</h2>

                    <p class="room-card-text">
                        Esperando la aprobación del responsable de la sala.
                    </p>

                    <div class="room-code-chip">
                        <span>Código</span>
                        <strong><c:out value="${pendiente.sala.codigo}"/></strong>
                    </div>

                    <p id="conexionSolicitud" class="connection-message" role="status"></p>

                    <form class="room-form"
                          action="${pageContext.request.contextPath}/salas"
                          method="post">
                        <input type="hidden"
                               name="csrf"
                               value="${csrf}">

                        <button class="button secondary"
                                type="submit"
                                name="accion"
                                value="cancelarSolicitud">
                            Cancelar solicitud
                        </button>
                    </form>
                </article>

                <jsp:include page="salas-ingreso.jsp"/>
            </div>
        </c:when>

        <c:otherwise>
            <div class="room-options">
                <article class="room-card">
                    <span class="card-symbol" aria-hidden="true">✦</span>
                    <p class="eyebrow">CREA TU ESPACIO</p>
                    <h2>Crear una sala</h2>

                    <p class="room-card-text">
                        Serás el responsable de la sala y recibirás un código
                        de acceso para compartir con tus compañeros.
                    </p>

                    <ul class="room-facts">
                        <li><span class="step-dot focus-dot"></span>Participantes <strong>2 a 4</strong></li>
                        <li><span class="step-dot short-dot"></span>Código de acceso <strong>6 caracteres</strong></li>
                        <li><span class="step-dot long-dot"></span>Quién entra <strong>Tú decides</strong></li>
                    </ul>

                    <form class="room-form"
                          action="${pageContext.request.contextPath}/salas"
                          method="post">
                        <input type="hidden"
                               name="csrf"
                               value="${csrf}">

                        <button class="button primary"
                                type="submit"
                                name="accion"
                                value="crear">
                            Crear sala
                        </button>
                    </form>
                </article>

                <jsp:include page="salas-ingreso.jsp"/>
            </div>
        </c:otherwise>
    </c:choose>

    <p class="bottom-note">
        <span>✦</span> Concéntrense juntos y aprovechen los descansos para conversar.
    </p>

    <footer class="page-footer">
        <span>Pomora · un espacio para concentrarte</span>
        <span>Un bloque a la vez.</span>
    </footer>
</main>
</body>
</html>
