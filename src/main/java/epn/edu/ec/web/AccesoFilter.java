package epn.edu.ec.web;

import epn.edu.ec.service.ServicioCierreSalaPrivada;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.UUID;

@WebFilter(urlPatterns = {
        "/pomodoro",
        "/historial",
        "/estado",
        "/compartido",
        "/racha",
        "/salas"
})
public class AccesoFilter implements Filter {

    @Override
    public void doFilter(
            ServletRequest request,
            ServletResponse response,
            FilterChain chain
    ) throws IOException, ServletException {

        var req = (HttpServletRequest) request;
        var resp = (HttpServletResponse) response;
        var session = req.getSession(false);

        resp.setHeader("Cache-Control", "no-store");

        if (session == null
                || session.getAttribute("cuentaId") == null) {

            if ("json".equals(req.getParameter("formato"))) {
                resp.setStatus(401);
                resp.setContentType(
                        "application/json;charset=UTF-8"
                );
                resp.getWriter().write(
                        "{\"error\":\"Inicia sesión para continuar.\"}"
                );
            } else {
                resp.sendRedirect(
                        req.getContextPath() + "/ingresar"
                );
            }

            return;
        }

        // Los sondeos en segundo plano no consumen el aviso.
        if ("GET".equals(req.getMethod())
                && req.getParameter("formato") == null) {

            var cierre = (ServicioCierreSalaPrivada)
                    req.getServletContext().getAttribute(
                            "servicioCierreSalaPrivada"
                    );

            if (cierre != null) {
                UUID estudianteId =
                        (UUID) session.getAttribute("estudianteId");

                var aviso = cierre.consumirAviso(estudianteId);

                if (aviso.isPresent()) {
                    session.setAttribute("mensaje", aviso.get());

                    if (!"/pomodoro".equals(req.getServletPath())) {
                        resp.sendRedirect(
                                req.getContextPath() + "/pomodoro"
                        );
                        return;
                    }
                }
            }
        }

        chain.doFilter(request, response);
    }
}