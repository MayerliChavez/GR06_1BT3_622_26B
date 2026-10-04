package edu.proyecto;
import edu.proyecto.model.*;
import edu.proyecto.repository.*;
import edu.proyecto.service.*;
import jakarta.persistence.*;
import org.junit.jupiter.api.*;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class CuentasTest {
    private EntityManagerFactory factory;
    private ServicioCuentas cuentas;
    @BeforeEach void abrir() {
        factory=Persistence.createEntityManagerFactory("tarea3",Map.of("jakarta.persistence.jdbc.url","jdbc:h2:mem:cuentas"+UUID.randomUUID(),"hibernate.hbm2ddl.auto","create-drop","hibernate.show_sql","false"));
        cuentas=new ServicioCuentas(factory);
    }
    @AfterEach void cerrar() { factory.close(); }
    @Test void registraValidaNormalizaYAutenticaSinGuardarTextoPlano() {
        var cuenta=cuentas.registrar("  Ana  "," ANA@EJEMPLO.COM ","Clave-Prueba-123");
        assertEquals("Ana",cuenta.getEstudiante().getNombreVisible());
        assertEquals("ana@ejemplo.com",cuenta.getCorreo());
        assertFalse(cuenta.getHashContrasena().contains("Clave-Prueba-123"));
        assertEquals(cuenta.getId(),cuentas.autenticar("ANA@ejemplo.com","Clave-Prueba-123").orElseThrow().getId());
        assertTrue(cuentas.autenticar("ana@ejemplo.com","incorrecta").isEmpty());
        assertTrue(cuentas.autenticar("desconocido@ejemplo.com","Clave-Prueba-123").isEmpty());
        assertThrows(IllegalArgumentException.class,()->cuentas.registrar("Otra","ana@ejemplo.com","Clave-Prueba-456"));
        assertThrows(IllegalArgumentException.class,()->cuentas.registrar("Ana","incorrecto","Clave-Prueba-123"));
        assertThrows(IllegalArgumentException.class,()->cuentas.registrar("Ana","otra@ejemplo.com","corta"));
        var segunda=cuentas.registrar("Luis","luis@ejemplo.com","Clave-Prueba-123");
        assertNotEquals(cuenta.getHashContrasena(),segunda.getHashContrasena());
    }
    @Test void primerEmparejamientoYPersistenciaConservanNombresDeAmbasCuentas() {
        var ana=cuentas.registrar("Ana Pérez","ana@ejemplo.com","Clave-Prueba-123");
        var luis=cuentas.registrar("Luis Gómez","luis@ejemplo.com","Clave-Prueba-456");
        var individuales=new RepositorioSesionesJpa(factory);
        var compartidas=new RepositorioSesionesCompartidasJpa(factory);
        var servicio=new ServicioEstudioCompartido(new EmparejadorEnMemoria(),individuales,compartidas,
                new ConfiguracionPomodoro(Duration.ofMinutes(25),Duration.ofMinutes(5),Duration.ofMinutes(15),4));
        var ahora=Instant.now(); servicio.solicitarEmparejamiento(ana.getId(),ahora);
        var pareja=servicio.solicitarEmparejamiento(luis.getId(),ahora).orElseThrow();
        var guardada=compartidas.buscarPorId(pareja.getId()).orElseThrow();
        assertEquals(Set.of("Ana Pérez","Luis Gómez"),new HashSet<>(guardada.getParticipaciones().stream().map(p->p.getEstudiante().getNombreVisible()).toList()));
        servicio.actualizarSesion(pareja.getId(),ahora.plusSeconds(1));
        assertEquals("Ana Pérez",cuentas.autenticar("ana@ejemplo.com","Clave-Prueba-123").orElseThrow().getEstudiante().getNombreVisible());
        assertEquals("Luis Gómez",individuales.buscarPorEstudiante(luis.getId()).get(0).getEstudiante().getNombreVisible());
    }
}
