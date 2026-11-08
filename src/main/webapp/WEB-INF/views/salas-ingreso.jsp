<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<h2>Ingresar a una sala</h2>
<form action="${pageContext.request.contextPath}/salas" method="post">
    <input type="hidden" name="csrf" value="${csrf}">
    <label for="codigoIngreso">Código de acceso</label>
    <input id="codigoIngreso" name="codigo" type="text" maxlength="6"
           required autocomplete="off" placeholder="K7M2QX">
    <button class="button primary" type="submit" name="accion" value="solicitar">
        Solicitar ingreso
    </button>
</form>
