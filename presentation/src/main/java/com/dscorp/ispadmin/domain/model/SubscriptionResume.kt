package com.dscorp.ispadmin.domain.model

data class SubscriptionResume(
    val id: Int,
    val planName: String,
    val customerName: String,
    val antiquity: String,
    val qualification: String,
    val placeName: String,
    val ics: String,
    val lastPaymentDate: String?,
    val pendingInvoicesQuantity: Int,
    val totalDebt: Double,
    val ipAddress: String,
    val customer: CustomerData,
    val serviceStatus: ServiceStatus,
    val installationType: InstallationType,
    val napBox: NapBox?,
    val placeId: String,
    val location: GeoLocation,
    val hasFiberOnu: Boolean = false,
)
fun SubscriptionResume.createReminderMessage(): String {
        val message = """
        ¡Hola ${customerName}! 🌟

        Esperamos que estés bien. Queremos recordarte que tienes $pendingInvoicesQuantity facturas pendientes por un total de $totalDebt soles.

        Para evitar interrupciones en tu servicio, te sugerimos realizar el pago a la brevedad. Puedes hacerlo fácilmente utilizando alguno de los siguientes métodos de pago:

        **Medios de Pago:**
        1. **Transferencia Bancaria:**
           - Banco: BCP 🏦
           - Número de Cuenta: 335-98410-54-0-22 💳

        2. **Yape:**
           - Número Yape: 958 073 976 📲

        3. **Plin:**
           - Número Plin: 958 073 976 📱

        También puedes Acercarte a nuestra oficina ubicada en los jardines - La Villa, referencia al costado del nuevo terminal Maranatha.

        🕛 Horario de atención: 
        👉🏼 Lunes a viernes de 8:30 am hasta 06:00 pm
        👉🏼 Sábados de 8:30 hasta 01:00 pm 

        Agradecemos tu pronta atención a este asunto. Si ya realizaste el pago, por favor, ignora este mensaje. Si necesitas ayuda o tienes alguna pregunta, estamos aquí para ayudarte.

        ¡Gracias por confiar en Gigafiber Peru S.A.C.! 🚀
    """.trimIndent()

        return message
    }

