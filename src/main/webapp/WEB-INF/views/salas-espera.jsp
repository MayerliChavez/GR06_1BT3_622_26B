<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>

<h2>Solicitudes de ingreso</h2>

<p>Ocupación: <c:out value="${ocupacion}"/>/4</p>

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
                </li>
            </c:forEach>
        </ul>
    </c:otherwise>
</c:choose>