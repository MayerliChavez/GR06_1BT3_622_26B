"use strict";

const copiarCodigo = document.getElementById("copiarCodigoSala");

if (copiarCodigo) {
    copiarCodigo.addEventListener("click", async () => {
        const campo = document.getElementById("codigoSala");
        const resultado = document.getElementById("resultadoCopiar");

        try {
            if (navigator.clipboard && window.isSecureContext) {
                await navigator.clipboard.writeText(campo.value);
            } else {
                campo.focus();
                campo.select();
                campo.setSelectionRange(0, campo.value.length);

                if (!document.execCommand("copy")) {
                    throw new Error("Copia no disponible");
                }
            }

            resultado.textContent =
                "Código copiado. Compártelo con tus compañeros.";
        } catch (error) {
            campo.focus();
            campo.select();

            resultado.textContent =
                "No se pudo copiar automáticamente. "
                + "Selecciona y copia el código.";
        }
    });
}

const esperaSala = document.getElementById("esperaSala");

if (esperaSala) {
    let consultandoEspera = false;

    const actualizarEspera = async () => {
        if (consultandoEspera || document.hidden) {
            return;
        }

        consultandoEspera = true;

        try {
            const respuesta = await fetch(esperaSala.dataset.url, {
                cache: "no-store",
                credentials: "same-origin",
                redirect: "error"
            });

            if (!respuesta.ok) {
                throw new Error("No se pudo actualizar la espera");
            }

            const html = await respuesta.text();

            esperaSala.innerHTML = html;

            document.getElementById("conexionEspera").textContent = "";
        } catch (error) {
            document.getElementById("conexionEspera").textContent =
                "No se pudo actualizar la sala de espera. Reintentando…";
        } finally {
            consultandoEspera = false;
        }
    };

    setInterval(actualizarEspera, 3000);
}

const estadoSolicitudSala = document.getElementById("estadoSolicitudSala");
if (estadoSolicitudSala) {
    let consultandoSolicitud = false;
    const actualizarSolicitud = async () => {
        if (consultandoSolicitud || document.hidden) return;
        consultandoSolicitud = true;
        try {
            const respuesta = await fetch(estadoSolicitudSala.dataset.url, {
                cache: "no-store", credentials: "same-origin", redirect: "error"
            });
            if (!respuesta.ok) throw new Error("No se pudo consultar la solicitud");
            const resultado = await respuesta.json();
            if (resultado.estado !== "PENDIENTE") {
                window.location.assign(estadoSolicitudSala.dataset.destino);
                return;
            }
            document.getElementById("conexionSolicitud").textContent = "";
        } catch (error) {
            document.getElementById("conexionSolicitud").textContent =
                    "No se pudo actualizar tu solicitud. Reintentando…";
        } finally {
            consultandoSolicitud = false;
        }
    };
    setInterval(actualizarSolicitud, 3000);
}

(() => {
    const abrir = document.getElementById("abrirCierreSala");
    const dialogo = document.getElementById("confirmarCierreSala");
    const cancelar = document.getElementById("cancelarCierreSala");

    if (!abrir || !dialogo || !cancelar) return;

    abrir.addEventListener("click", () => {
        dialogo.showModal();
    });

    cancelar.addEventListener("click", () => {
        dialogo.close();
    });
})();
