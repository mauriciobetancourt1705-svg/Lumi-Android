package com.lumi.android.voice

/**
 * Executes typed UI actions through the user-authorized AccessibilityService.
 */
class LumiAccessibilityActionExecutor {
    fun execute(intent: LumiIntent, onResult: (String, Boolean) -> Unit) {
        val service = LumiAccessibilityService.current()
        if (service == null) {
            onResult("Para controlar otras aplicaciones necesito que actives el acceso de accesibilidad de Lumi en Ajustes de Android.", false)
            return
        }

        val beforeRevision = service.revision()
        val result = when (intent) {
            LumiIntent.GoBack -> service.pressBack() to "Volví a la pantalla anterior."
            LumiIntent.GoHome -> service.pressHome() to "Volví a la pantalla de inicio."
            LumiIntent.OpenRecents -> service.openRecents() to "Abrí las aplicaciones recientes."
            LumiIntent.ScrollForward -> service.scrollForward() to "Desplacé la pantalla hacia abajo."
            LumiIntent.ScrollBackward -> service.scrollBackward() to "Desplacé la pantalla hacia arriba."
            LumiIntent.DescribeScreen -> {
                val description = service.describeScreen()
                if (description.isBlank()) false to "No pude leer el contenido visible de la pantalla." else true to "En pantalla veo:\n" + description
            }
            is LumiIntent.ClickText -> service.clickText(intent.text) to "Toqué «" + intent.text + "»."
            is LumiIntent.TypeText -> service.setText(intent.text) to "Escribí el texto indicado."
            else -> false to ""
        }

        if (!result.first) {
            onResult("No pude ejecutar esa acción en la pantalla actual.", false)
            return
        }

        // Verify that Android published at least one accessibility event after
        // a state-changing action. This prevents Lumi from blindly claiming success.
        if (intent is LumiIntent.ClickText || intent is LumiIntent.TypeText ||
            intent is LumiIntent.GoBack || intent is LumiIntent.GoHome ||
            intent is LumiIntent.OpenRecents || intent is LumiIntent.ScrollForward ||
            intent is LumiIntent.ScrollBackward
        ) {
            service.waitForScreenChange(beforeRevision) { changed ->
                if (changed) {
                    onResult(result.second, true)
                } else {
                    onResult("La acción fue enviada, pero no pude verificar un cambio en pantalla.", false)
                }
            }
        } else {
            onResult(result.second, true)
        }
    }
}
