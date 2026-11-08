<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>

<section id="cicloSala"
         data-url="${pageContext.request.contextPath}/salas/ciclo/${sala.id}"
         data-sala-id="${sala.id}"
         data-csrf="${csrf}"
         data-responsable="${esResponsable}">

    <h3 id="faseCicloSala">Sincronizando el ciclo…</h3>

    <div class="clock-wrap">
        <svg class="clock-ring"
             viewBox="0 0 300 300"
             aria-hidden="true">
            <circle class="ring-track"
                    cx="150"
                    cy="150"
                    r="137"/>
        </svg>

        <div class="clock-content">
            <span class="clock-eyebrow">
                TIEMPO COMPARTIDO
            </span>

            <strong id="tiempoCicloSala"
                    role="timer"
                    aria-live="off">--:--</strong>

            <span class="clock-caption">
                Concentración y descansos del grupo
            </span>
        </div>
    </div>

    <p id="conexionCicloSala"
       class="connection-message"
       role="status"
       aria-live="polite">
        Conectando con la sala…
    </p>

    <p id="indicacionCicloSala" class="timer-hint"></p>

    <c:if test="${esResponsable and not cicloIniciado}">
        <form id="formIniciarCicloSala"
              action="${pageContext.request.contextPath}/salas"
              method="post">

            <input type="hidden"
                   name="csrf"
                   value="${csrf}">

            <button id="iniciarCicloSala"
                    class="button primary timer-button"
                    type="submit"
                    name="accion"
                    value="iniciarEstudio">
                Iniciar estudio
            </button>
        </form>
    </c:if>
</section>