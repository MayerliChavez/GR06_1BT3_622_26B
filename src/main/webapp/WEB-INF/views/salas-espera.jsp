<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>

<h2>Solicitudes de ingreso</h2>

<p>Ocupación: <c:out value="${ocupacion}"/>/4</p>
<c:if test="${not aceptacionPermitida}">
    <p>Podrás aceptar nuevos participantes en el siguiente descanso.</p>
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
                    <form action="${pageContext.request.contextPath}/salas" method="post">
                        <input type="hidden" name="csrf" value="${csrf}">
                        <input type="hidden" name="solicitudId" value="${solicitud.id}">
                        <button class="button primary" type="submit" name="accion" value="aceptarSolicitud"
                                ${ocupacion >= 4 || not aceptacionPermitida ? 'disabled' : ''}>Aceptar</button>
                        <button class="button" type="submit" name="accion" value="rechazarSolicitud">Rechazar</button>
                    </form>
                </li>
            </c:forEach>
        </ul>
    </c:otherwise>
</c:choose>
