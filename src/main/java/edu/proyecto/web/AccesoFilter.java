package edu.proyecto.web;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.*;
import java.io.IOException;

@WebFilter(urlPatterns={"/pomodoro","/historial","/estado","/compartido"})
public class AccesoFilter implements Filter {
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        var req=(HttpServletRequest)request; var resp=(HttpServletResponse)response;
        var session=req.getSession(false);
        resp.setHeader("Cache-Control","no-store");
        if(session==null || session.getAttribute("cuentaId")==null) {
            if("json".equals(req.getParameter("formato"))) { resp.setStatus(401); resp.setContentType("application/json"); resp.getWriter().write("{\"error\":\"Inicia sesión para continuar.\"}"); }
            else resp.sendRedirect(req.getContextPath()+"/ingresar");
            return;
        }
        chain.doFilter(request,response);
    }
}
