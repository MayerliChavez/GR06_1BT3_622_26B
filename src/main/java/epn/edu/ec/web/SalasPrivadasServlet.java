package epn.edu.ec.web;

import epn.edu.ec.service.ServicioSalasPrivadas;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;

@WebServlet("/salas")
public class SalasPrivadasServlet extends HttpServlet {
    private ServicioSalasPrivadas servicio() {
        return (ServicioSalasPrivadas) getServletContext().getAttribute("servicioSalasPrivadas");
    }

    @Override protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        resp.setHeader("Cache-Control", "no-store");
        resp.setContentType("text/html;charset=UTF-8");
        var id = IdentidadWeb.estudiante(req, resp);
        req.setAttribute("csrf", IdentidadWeb.csrf(req));
        req.setAttribute("sala", servicio().consultarActual(id).orElse(null));
        req.setAttribute("mensaje", req.getSession().getAttribute("mensajeSala"));
        req.getSession().removeAttribute("mensajeSala");
        req.getRequestDispatcher("/WEB-INF/views/salas.jsp").forward(req, resp);
    }

    @Override protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        req.setCharacterEncoding("UTF-8");
        resp.setHeader("Cache-Control", "no-store");
        var id = IdentidadWeb.estudiante(req, resp);
        if (!IdentidadWeb.csrf(req).equals(req.getParameter("csrf"))) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        if (!"crear".equals(req.getParameter("accion"))) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }
        try {
            servicio().crear(id);
        } catch (IllegalStateException error) {
            req.getSession().setAttribute("mensajeSala", error.getMessage());
        }
        resp.sendRedirect(req.getContextPath() + "/salas");
    }
}
