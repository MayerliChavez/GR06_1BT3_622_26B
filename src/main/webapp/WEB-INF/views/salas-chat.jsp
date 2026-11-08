<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>

<section id="chatSala"
         class="chat-card private-chat"
         data-user="${sessionScope.estudianteId}"
         aria-labelledby="tituloChatSala">

    <header class="chat-head">
        <div>
            <h2 id="tituloChatSala">Chat de la sala</h2>

            <p id="estadoChatSala"
               class="chat-status"
               role="status">Conectando…</p>
        </div>
        <span aria-hidden="true">♧</span>
    </header>

    <div id="mensajesChatSala"
         class="chat-log"
         role="log"
         aria-live="polite"
         aria-label="Mensajes de la sala">

        <p class="chat-empty">Todavía no hay mensajes.</p>
    </div>

    <form id="formChatSala" class="chat-form">
        <label class="sr-only" for="textoChatSala">
            Mensaje para tu sala
        </label>

        <textarea id="textoChatSala"
                  rows="2"
                  disabled
                  placeholder="Escribe un mensaje…"
                  aria-describedby="errorChatSala"></textarea>

        <button id="enviarChatSala"
                type="submit"
                disabled>
            Enviar
        </button>
    </form>

    <p id="errorChatSala"
       class="chat-error"
       role="alert"></p>

    <p class="chat-hint">
        Puedes conversar antes de iniciar y durante los descansos.
        Máximo 2000 caracteres.
    </p>
</section>
