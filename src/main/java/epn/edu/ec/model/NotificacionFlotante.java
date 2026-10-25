package epn.edu.ec.model;

import java.util.UUID;

/** Dato transitorio de presentacion; no se persiste ni es una entidad JPA. */
public record NotificacionFlotante(String id, UUID estudianteId, String mensaje,
                                  String posicion, long duracionMillis,
                                  TipoBloque tipoBloque, long duracionBloqueMillis) {}
