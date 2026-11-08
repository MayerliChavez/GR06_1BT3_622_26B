"use strict";

(() => {
    const raiz = document.getElementById("cicloSala");
    if (!raiz) return;

    const tiempo = document.getElementById("tiempoCicloSala");
    const fase = document.getElementById("faseCicloSala");
    const conexion = document.getElementById("conexionCicloSala");
    const indicacion = document.getElementById("indicacionCicloSala");
    const formulario = document.getElementById("formIniciarCicloSala");
    const boton = document.getElementById("iniciarCicloSala");

    const esResponsable = raiz.dataset.responsable === "true";

    const nombres = {
        CONCENTRACION: "Concentración",
        DESCANSO_CORTO: "Descanso corto",
        DESCANSO_LARGO: "Descanso largo"
    };

    let socket = null;
    let estado = null;
    let recibidoEn = 0;
    let ultimaHoraServidor = 0;
    let demoraReconexion = 1000;
    let reconexion = null;
    let saliendo = false;

    function escribir(elemento, texto) {
        if (elemento.textContent !== texto) {
            elemento.textContent = texto;
        }
    }

    function pintarTiempo() {
        if (!estado) return;

        if (!estado.iniciado) {
            escribir(tiempo, "25:00");
            return;
        }

        const transcurrido = performance.now() - recibidoEn;

        const restantes = Math.max(
            0,
            estado.restanteMillis - transcurrido
        );

        const segundos = Math.ceil(restantes / 1000);

        const minutosTexto = String(
            Math.floor(segundos / 60)
        ).padStart(2, "0");

        const segundosTexto = String(
            segundos % 60
        ).padStart(2, "0");

        escribir(
            tiempo,
            `${minutosTexto}:${segundosTexto}`
        );
    }

    function aplicarEstado(nuevo) {
        if (nuevo.tipo !== "estadoCiclo"
            || nuevo.salaId !== raiz.dataset.salaId
            || typeof nuevo.iniciado !== "boolean"
            || !Number.isFinite(nuevo.horaServidor)
            || !Number.isFinite(nuevo.restanteMillis)) {
            throw new Error("Estado de ciclo incorrecto");
        }

        if (nuevo.horaServidor < ultimaHoraServidor) {
            return;
        }

        ultimaHoraServidor = nuevo.horaServidor;
        estado = nuevo;
        recibidoEn = performance.now();

        if (formulario) {
            formulario.hidden = nuevo.iniciado;
        }

        if (boton) {
            boton.disabled = nuevo.iniciado;
        }

        if (!nuevo.iniciado) {
            escribir(fase, "Preparados para estudiar");

            escribir(
                indicacion,
                esResponsable
                    ? "Inicia cuando haya al menos dos participantes."
                    : "Esperando que el responsable inicie el estudio."
            );
        } else {
            escribir(
                fase,
                nombres[nuevo.bloque] || "Ciclo compartido"
            );

            escribir(
                indicacion,
                nuevo.bloque === "CONCENTRACION"
                    ? "El grupo está en concentración."
                    : "El grupo está en descanso."
            );
        }

        escribir(conexion, "Conectado con la sala.");
        pintarTiempo();

        raiz.dispatchEvent(
            new CustomEvent("sala:estado", {detail: nuevo})
        );
    }

    function programarReconexion() {
        if (saliendo || reconexion !== null) return;

        escribir(
            conexion,
            "Conexión interrumpida. Reconectando…"
        );

        reconexion = window.setTimeout(() => {
            reconexion = null;
            conectar();
        }, demoraReconexion);

        demoraReconexion = Math.min(
            demoraReconexion * 2,
            10000
        );
    }

    function conectar() {
        if (saliendo) return;

        if (boton) {
            boton.disabled = true;
        }

        const direccion = new URL(
            raiz.dataset.url,
            window.location.href
        );

        direccion.protocol =
            window.location.protocol === "https:"
                ? "wss:"
                : "ws:";

        direccion.searchParams.set(
            "csrf",
            raiz.dataset.csrf
        );

        try {
            const canal = new WebSocket(direccion.href);
            socket = canal;

            canal.onopen = () => {
                if (socket !== canal) return;

                demoraReconexion = 1000;

                escribir(
                    conexion,
                    "Sincronizando con el servidor…"
                );

                canal.send("P");
            };

            canal.onmessage = evento => {
                if (socket !== canal) return;

                try {
                    aplicarEstado(JSON.parse(evento.data));
                } catch (error) {
                    escribir(
                        conexion,
                        "No se pudo leer el estado de la sala."
                    );
                }
            };

            canal.onerror = () => {
                if (socket === canal) {
                    canal.close();
                }
            };

            canal.onclose = evento => {
                if (socket !== canal || saliendo) return;
                raiz.dispatchEvent(
                    new CustomEvent("sala:desconexion", {
                        detail: {permanente: evento.code === 1008}
                    })
                );

                if (boton) {
                    boton.disabled = true;
                }

                if (evento.code === 1008) {
                    estado = null;
                    escribir(tiempo, "--:--");

                    escribir(
                        conexion,
                        "La sala ya no está disponible. Recarga la página."
                    );

                    return;
                }

                programarReconexion();
            };

        } catch (error) {
            programarReconexion();
        }
    }

    const reloj = window.setInterval(pintarTiempo, 250);

    // Mantiene viva la conexión y vuelve a comprobar el acceso.
    const latido = window.setInterval(() => {
        if (socket && socket.readyState === WebSocket.OPEN) {
            socket.send("P");
        }
    }, 25000);

    document.addEventListener("visibilitychange", () => {
        if (!document.hidden
            && socket
            && socket.readyState === WebSocket.OPEN) {
            socket.send("P");
        }
    });

    window.addEventListener("pagehide", () => {
        saliendo = true;

        window.clearInterval(reloj);
        window.clearInterval(latido);

        if (reconexion !== null) {
            window.clearTimeout(reconexion);
        }

        if (socket) {
            socket.close(1000, "Página cerrada");
        }
    });

    window.addEventListener("pageshow", evento => {
        if (evento.persisted) {
            window.location.reload();
        }
    });

    raiz.addEventListener("sala:enviarChat", evento => {
        try {
            if (!socket || socket.readyState !== WebSocket.OPEN) {
                throw new Error(
                    "No hay conexión. "
                    + "Espera a que se recupere la sala."
                );
            }

            socket.send("M" + evento.detail);

        } catch (error) {
            raiz.dispatchEvent(
                new CustomEvent("sala:errorChat", {
                    detail: error.message
                })
            );
        }
    });

    raiz.addEventListener("sala:reconectar", () => {
        if (socket) {
            socket.close(1000, "Recuperar conexión");
        }
    });
    conectar();
})();