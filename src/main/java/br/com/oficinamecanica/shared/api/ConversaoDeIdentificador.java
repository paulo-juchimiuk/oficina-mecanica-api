package br.com.oficinamecanica.shared.api;

import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.InitBinder;
import java.beans.PropertyEditorSupport;
import java.util.UUID;

@ControllerAdvice
class ConversaoDeIdentificador {

    @InitBinder
    void registrarEditorEstrito(WebDataBinder binder) {
        binder.registerCustomEditor(UUID.class, new EditorEstrito());
    }

    private static class EditorEstrito extends PropertyEditorSupport {

        @Override
        public void setAsText(String texto) {
            if (texto == null || texto.isEmpty()) {
                setValue(null);
                return;
            }
            UUID identificador = UUID.fromString(texto);
            if (!identificador.toString().equalsIgnoreCase(texto)) {
                throw new IllegalArgumentException("Identificador fora do formato UUID");
            }
            setValue(identificador);
        }
    }
}
