<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>

<h2>Sala de espera</h2>

<p>Ocupación: <c:out value="${ocupacion}"/>/<c:out value="${maxParticipantes}"/></p>
<c:if test="${esResponsable and not aceptacionPermitida}">
    <p><c:out value="${motivoAdmision}"/></p>
</c:if>

<c:choose>
    <c:when test="${empty solicitudes}">
        <p>No hay solicitudes pendientes.</p>
    </c:when>

    <c:otherwise>
        <ul>
            <c:forEach var="solicitud" items="${solicitudes}">
                <li>
                    <c:out value="${nombresSolicitantes[solicitud.estudianteId]}"/>
                    — Pendiente
                    <c:if test="${esResponsable}">
                    <form action="${pageContext.request.contextPath}/salas" method="post">
                        <input type="hidden" name="csrf" value="${csrf}">
                        <input type="hidden" name="solicitudId" value="${solicitud.id}">
                        <button class="button primary" type="submit" name="accion" value="aceptarSolicitud"
                                ${not aceptacionPermitida ? 'disabled' : ''}>Aceptar</button>
                        <button class="button" type="submit" name="accion" value="rechazarSolicitud">Rechazar</button>
                    </form>
                    </c:if>
                </li>
            </c:forEach>
        </ul>
    </c:otherwise>
</c:choose>
