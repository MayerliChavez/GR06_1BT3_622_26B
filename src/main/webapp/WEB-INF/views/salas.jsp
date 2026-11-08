<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>

<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width,initial-scale=1">

    <title>Salas privadas · Pomora</title>

    <link rel="stylesheet"
          href="${pageContext.request.contextPath}/assets/pomora.css?v=salas1">

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
            Temporizador
        </a>

        <a class="nav-link"
           href="${pageContext.request.contextPath}/historial">
            Historial de estudio
        </a>

        <a class="nav-link"
           href="${pageContext.request.contextPath}/compartido">
            Estudio compartido
        </a>

        <a class="nav-link selected"
           href="${pageContext.request.contextPath}/salas"
           aria-current="page">
            Salas privadas
        </a>
    </nav>
</aside>

<main class="workspace">
    <header class="topbar">
        <span>SALAS PRIVADAS</span>
        <span>
            <c:out value="${sessionScope.nombreVisible}"/>
        </span>
    </header>

    <section class="page-heading">
        <p class="eyebrow">ESTUDIA CON TU GRUPO</p>
        <h1>Tu sala privada.</h1>
        <p>Reúnete con los compañeros que tú elijas.</p>
    </section>

    <c:if test="${not empty mensaje}">
        <p class="notice" role="alert">
            <c:out value="${mensaje}"/>
        </p>
    </c:if>

    <section class="timer-card">
        <c:choose>
            <c:when test="${not empty sala}">
                <div class="timer-card-head">
                    <span class="session-indicator">Sala abierta</span>
                    <strong>
                        ${esResponsable ? 'Responsable' : 'Participante'}
                    </strong>
                </div>

                <h2>Tu sala de estudio</h2>

                <label for="codigoSala">Código de acceso</label>

                <input id="codigoSala"
                       type="text"
                       readonly
                       value="${sala.codigo}"
                       aria-label="Código de acceso de seis caracteres">

                <button class="button primary"
                        id="copiarCodigoSala"
                        type="button">
                    Copiar código
                </button>

                <p id="resultadoCopiar"
                   role="status"
                   aria-live="polite"></p>

                <p class="timer-hint">
                    Comparte este código con tus compañeros.
                </p>

                <jsp:include page="salas-ciclo.jsp"/>

                <jsp:include page="salas-chat.jsp"/>

                <c:if test="${esResponsable}">
                    <button id="abrirCierreSala"
                            class="button"
                            type="button">
                        Cerrar sala
                    </button>

                    <dialog id="confirmarCierreSala">
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

                            <p>
                                ¿Deseas cerrar la sala para todos los participantes?
                            </p>

                            <button id="cancelarCierreSala"
                                    class="button"
                                    type="button">
                                Cancelar
                            </button>

                            <button class="button primary"
                                    type="submit"
                                    name="accion"
                                    value="cerrarSala">
                                Aceptar
                            </button>
                        </form>
                    </dialog>
                </c:if>

                <section id="esperaSala"
                         data-url="${pageContext.request.contextPath}/salas?formato=espera">
                    <jsp:include page="salas-espera.jsp"/>
                </section>

                <p id="conexionEspera" role="status"></p>
            </c:when>

            <c:when test="${not empty pendiente}">
                <h2 id="estadoSolicitudSala"
                    data-url="${pageContext.request.contextPath}/salas?formato=estadoSolicitud"
                    data-destino="${pageContext.request.contextPath}/salas">Sala de espera</h2>
                <p id="conexionSolicitud" role="status"></p>

                <p>
                    Esperando la aprobación del responsable de la sala.
                </p>

                <p>
                    Código:
                    <c:out value="${pendiente.sala.codigo}"/>
                </p>

                <form action="${pageContext.request.contextPath}/salas"
                      method="post">
                    <input type="hidden"
                           name="csrf"
                           value="${csrf}">

                    <button class="button primary"
                            type="submit"
                            name="accion"
                            value="cancelarSolicitud">
                        Cancelar solicitud
                    </button>
                </form>
                <jsp:include page="salas-ingreso.jsp"/>
            </c:when>

            <c:otherwise>
                <h2>Crea un espacio para estudiar juntos</h2>

                <p>
                    Serás el responsable de la sala y recibirás un código
                    de acceso.
                </p>

                <form action="${pageContext.request.contextPath}/salas"
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

                <jsp:include page="salas-ingreso.jsp"/>
            </c:otherwise>
        </c:choose>
    </section>
</main>
</body>
</html>
