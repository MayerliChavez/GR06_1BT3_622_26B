<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>

<header class="waiting-head">
    <div>
        <p class="eyebrow">QUIÉN QUIERE ENTRAR</p>
        <h2>Sala de espera</h2>
    </div>

    <div class="seat-meter">
        <span class="seats" aria-hidden="true">
            <c:forEach begin="1" end="${maxParticipantes}" var="asiento">
                <i class="${asiento le ocupacion ? 'taken' : ''}"></i>
            </c:forEach>
        </span>
        <span>Ocupación: <c:out value="${ocupacion}"/>/<c:out value="${maxParticipantes}"/></span>
    </div>
</header>

<c:if test="${esResponsable}">
    <p class="waiting-hint">Puedes aceptar solicitudes antes de iniciar el estudio o durante los descansos.</p>
</c:if>

<c:choose>
    <c:when test="${empty solicitudes}">
        <p class="waiting-empty">No hay solicitudes pendientes.</p>
    </c:when>

    <c:otherwise>
        <ul class="waiting-list">
            <c:forEach var="solicitud" items="${solicitudes}">
                <c:set var="nombre" value="${nombresSolicitantes[solicitud.estudianteId]}"/>
                <li>
                    <span class="waiting-avatar" aria-hidden="true"><c:out value="${fn:toUpperCase(fn:substring(nombre, 0, 1))}"/></span>
                    <span class="waiting-name">
                        <strong><c:out value="${nombre}"/></strong>
                        <small>— Pendiente</small>
                    </span>
                    <c:if test="${esResponsable}">
                    <form class="waiting-actions" action="${pageContext.request.contextPath}/salas" method="post">
                        <input type="hidden" name="csrf" value="${csrf}">
                        <input type="hidden" name="solicitudId" value="${solicitud.id}">
                        <button class="button primary" type="submit" name="accion" value="aceptarSolicitud">Aceptar</button>
                        <button class="button secondary" type="submit" name="accion" value="rechazarSolicitud">Rechazar</button>
                    </form>
                    </c:if>
                </li>
            </c:forEach>
        </ul>
    </c:otherwise>
</c:choose>
