package edu.proyecto.web;
import jakarta.servlet.http.*;
import java.util.UUID;

/** La identidad procede exclusivamente de la sesión autenticada del servidor. */
public final class IdentidadWeb {
    private IdentidadWeb() {}
    public static UUID estudiante(HttpServletRequest req, HttpServletResponse resp) {
        var session=req.getSession(false);
        if(session==null || session.getAttribute("cuentaId")==null) throw new IllegalStateException("Inicia sesión para continuar.");
        return (UUID)session.getAttribute("estudianteId");
    }
    public static String csrf(HttpServletRequest req) {
        var session=req.getSession(); String token=(String)session.getAttribute("csrf");
        if(token==null) { token=UUID.randomUUID().toString(); session.setAttribute("csrf",token); }
        return token;
    }
}
