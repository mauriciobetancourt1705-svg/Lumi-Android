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

        if (result.first) onResult(result.second, true)
        else onResult("No pude ejecutar esa acción en la pantalla actual.", false)
    }
}
