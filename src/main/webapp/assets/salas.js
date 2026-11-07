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
                if (!document.execCommand("copy")) throw new Error("Copia no disponible");
            }
            resultado.textContent = "Código copiado. Compártelo con tus compañeros.";
        } catch (error) {
            campo.focus();
            campo.select();
            resultado.textContent = "No se pudo copiar automáticamente. Selecciona y copia el código.";
        }
    });
}
