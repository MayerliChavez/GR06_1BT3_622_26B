package edu.proyecto.web;
import edu.proyecto.service.*;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.time.Instant;
import java.util.UUID;

@WebServlet(urlPatterns={"/ingresar","/registro","/salir"})
public class AccesoServlet extends HttpServlet {
    protected void doGet(HttpServletRequest req,HttpServletResponse resp) throws ServletException,IOException {
        resp.setHeader("Cache-Control","no-store");
        if(req.getSession().getAttribute("cuentaId")!=null) { resp.sendRedirect(req.getContextPath()+"/compartido"); return; }
        req.setAttribute("registro",req.getServletPath().equals("/registro"));
        req.setAttribute("csrf",IdentidadWeb.csrf(req));
        req.getRequestDispatcher("/WEB-INF/views/acceso.jsp").forward(req,resp);
    }
    protected void doPost(HttpServletRequest req,HttpServletResponse resp) throws ServletException,IOException {
        req.setCharacterEncoding("UTF-8"); resp.setHeader("Cache-Control","no-store");
        if(!IdentidadWeb.csrf(req).equals(req.getParameter("csrf"))) { resp.sendError(403); return; }
        var cuentas=(ServicioCuentas)getServletContext().getAttribute("servicioCuentas");
        try {
            if(req.getServletPath().equals("/salir")) {
                var id=(UUID)req.getSession().getAttribute("estudianteId");
                if(id!=null) {
                    var servicio=(ServicioEstudioCompartido)getServletContext().getAttribute("servicioCompartido");
                    synchronized(servicio) {
                        var repo=(edu.proyecto.repository.RepositorioSesionesCompartidas)getServletContext().getAttribute("repositorioCompartidas");
                        var compartida=repo.buscarAbiertaPorEstudiante(id);
                        if(compartida.isPresent())servicio.salir(compartida.get().getId(),id,Instant.now());
                        servicio.cancelarEspera(id);
                        ((PresenciaCompartida)getServletContext().getAttribute("presenciaCompartida")).olvidar(id);
                    }
                }
                req.getSession().invalidate(); resp.sendRedirect(req.getContextPath()+"/ingresar"); return;
            }
            if(req.getServletPath().equals("/registro")) {
                cuentas.registrar(req.getParameter("nombre"),req.getParameter("correo"),req.getParameter("contrasena"));
                req.setAttribute("mensaje","Cuenta creada. Ya puedes iniciar sesión."); req.setAttribute("registro",false);
                req.setAttribute("csrf",IdentidadWeb.csrf(req));
                req.getRequestDispatcher("/WEB-INF/views/acceso.jsp").forward(req,resp); return;
            }
            var cuenta=cuentas.autenticar(req.getParameter("correo"),req.getParameter("contrasena"))
                    .orElseThrow(()->new IllegalArgumentException("Correo o contraseña incorrectos."));
            req.getSession().invalidate(); var session=req.getSession(true);
            session.setAttribute("cuentaId",cuenta.getId()); session.setAttribute("estudianteId",cuenta.getEstudiante().getId());
            session.setAttribute("nombreVisible",cuenta.getEstudiante().getNombreVisible());
            IdentidadWeb.csrf(req); resp.sendRedirect(req.getContextPath()+"/compartido");
        } catch(IllegalArgumentException e) {
            resp.setStatus(400); req.setAttribute("error",e.getMessage()); req.setAttribute("registro",req.getServletPath().equals("/registro"));
            req.setAttribute("csrf",IdentidadWeb.csrf(req)); req.getRequestDispatcher("/WEB-INF/views/acceso.jsp").forward(req,resp);
        }
    }
}
