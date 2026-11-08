<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<article class="room-card room-card-soft">
    <span class="card-symbol" aria-hidden="true">♧</span>
    <p class="eyebrow">ÚNETE A TU GRUPO</p>
    <h2>Ingresar a una sala</h2>

    <p class="room-card-text">
        Escribe el código de 6 caracteres que te compartió
        el responsable de la sala.
    </p>

    <form class="room-form" action="${pageContext.request.contextPath}/salas" method="post">
        <input type="hidden" name="csrf" value="${csrf}">
        <label for="codigoIngreso">Código de acceso</label>
        <input id="codigoIngreso" class="room-code-input" name="codigo" type="text" maxlength="6"
               required autocomplete="off" autocapitalize="characters" spellcheck="false"
               placeholder="K7M2QX">
        <button class="button primary" type="submit" name="accion" value="solicitar">
            Solicitar ingreso
        </button>
    </form>
</article>
