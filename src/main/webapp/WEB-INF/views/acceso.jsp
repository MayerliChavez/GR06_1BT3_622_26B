<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!doctype html><html lang="es"><head><meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>${registro ? 'Crear cuenta' : 'Ingresar'} · Pomora</title><link rel="stylesheet" href="${pageContext.request.contextPath}/assets/pomora.css?v=vida1"><script src="${pageContext.request.contextPath}/assets/pomora-ui.js?v=vida1" defer></script></head>
<body class="access-page"><main class="access-card"><a class="access-brand" href="${pageContext.request.contextPath}/ingresar"><img class="access-logo" width="190" src="${pageContext.request.contextPath}/assets/pomora-logo.png" alt="Pomora"></a><p class="eyebrow">TU ESPACIO PARA CONCENTRARTE</p><h1>${registro ? 'Crea tu cuenta.' : 'Bienvenido a Pomora.'}</h1><p class="access-description">${registro ? 'Tu nombre identificará tus sesiones y será visible para tu compañero de estudio.' : 'Ingresa para estudiar, consultar tu historial y encontrar un compañero.'}</p>
<c:if test="${not empty mensaje}"><p class="notice" role="status"><c:out value="${mensaje}"/></p></c:if>
<c:if test="${not empty error}"><p class="notice" role="alert"><c:out value="${error}"/></p></c:if>
<form class="access-form" method="post" action="${pageContext.request.contextPath}/${registro ? 'registro' : 'ingresar'}"><input type="hidden" name="csrf" value="${csrf}">
<c:if test="${registro}"><label for="nombre">Nombre visible</label><input id="nombre" name="nombre" autocomplete="name" maxlength="80" required></c:if>
<label for="correo">Correo electrónico</label><input id="correo" name="correo" type="email" autocomplete="username" maxlength="254" required>
<label for="contrasena">Contraseña</label><input id="contrasena" name="contrasena" type="password" autocomplete="${registro ? 'new-password' : 'current-password'}" minlength="${registro ? 8 : 1}" maxlength="128" required>
<c:if test="${registro}"><small>Entre 8 y 128 caracteres.</small></c:if><button class="button primary" type="submit">${registro ? 'Crear cuenta' : 'Ingresar'} <span>→</span></button></form>
<p class="access-switch">${registro ? '¿Ya tienes cuenta?' : '¿Es tu primera vez?'} <a href="${pageContext.request.contextPath}/${registro ? 'ingresar' : 'registro'}">${registro ? 'Iniciar sesión' : 'Registrarse'}</a></p></main></body></html>
