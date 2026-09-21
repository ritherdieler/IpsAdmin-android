package com.dscorp.ispadmin.presentation.ui.features.subscriptionfinder.compose

import com.dscorp.ispadmin.presentation.ui.features.payment.history.PaymentHistoryTestTags
import com.dscorp.ispadmin.presentation.ui.features.payment.register.RegisterPaymentContentDescriptions
import com.dscorp.ispadmin.presentation.ui.features.payment.register.RegisterPaymentTestTags
import com.dscorp.ispadmin.presentation.ui.features.subscriptiondetail.SubscriptionDetailTestTags
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.io.File

class SubscriptionFinderTestTagsTest {

    @Test
    fun `tags de busqueda estan definidos y no vacios`() {
        SubscriptionFinderTestTags.searchInteractive.forEach { tag ->
            assertThat(tag).isNotEmpty()
        }
        assertThat(SubscriptionFinderTestTags.searchInteractive).containsNoDuplicates()
    }

    @Test
    fun `tags de dialogos estan definidos y no vacios`() {
        SubscriptionFinderTestTags.dialogInteractive.forEach { tag ->
            assertThat(tag).isNotEmpty()
        }
        assertThat(SubscriptionFinderTestTags.dialogInteractive).containsNoDuplicates()
    }

    @Test
    fun `helpers dinamicos generan tags estables por id`() {
        assertThat(SubscriptionFinderTestTags.resultItem(42)).isEqualTo("subscription_result_item_42")
        assertThat(SubscriptionFinderTestTags.resultMenuItem(7, "payment_history"))
            .isEqualTo("subscription_result_menu_payment_history_7")
        assertThat(SubscriptionFinderTestTags.customerSave(99)).isEqualTo("subscription_result_customer_save_99")
    }

    @Test
    fun `menuActionTag mapea todas las opciones del menu`() {
        SubscriptionMenu.values().forEach { menu ->
            assertThat(SubscriptionFinderTestTags.menuActionTag(menu)).isNotEmpty()
        }
    }

    @Test
    fun `descripciones de contenido no interactivo estan definidas`() {
        assertThat(SubscriptionFinderContentDescriptions.EMPTY_STATE_ICON).isNotEmpty()
        assertThat(SubscriptionFinderContentDescriptions.SEARCH_LOADING).isNotEmpty()
        assertThat(SubscriptionFinderContentDescriptions.DATE_SEARCH_ICON).isNotEmpty()
    }

    @Test
    fun `SubscriptionFinderScreen aplica testTags y ObservabilityComposeText`() {
        val source = File(
            "src/main/java/com/dscorp/ispadmin/presentation/ui/features/subscriptionfinder/compose/SubscriptionFinderScreen.kt"
        ).readText()

        listOf(
            "SubscriptionFinderTestTags.QUERY_NAME",
            "SubscriptionFinderTestTags.QUERY_DOCUMENT",
            "SubscriptionFinderTestTags.QUERY_IP",
            "SubscriptionFinderTestTags.QUERY_CODE",
            "ObservabilityComposeText.report"
        ).forEach { ref ->
            assertThat(source).contains(ref)
        }
        assertThat(source).doesNotContain("ObservabilityComposeClick.report")
    }

    @Test
    fun `DateRangeSelector aplica testTags de fechas y Hoy`() {
        val source = File(
            "src/main/java/com/dscorp/ispadmin/presentation/ui/features/subscriptionfinder/compose/DateRangeSelector.kt"
        ).readText()

        listOf(
            "SubscriptionFinderTestTags.DATE_START",
            "SubscriptionFinderTestTags.DATE_END",
            "SubscriptionFinderTestTags.DATE_SUBMIT",
            "SubscriptionFinderTestTags.DATE_TODAY",
            "SubscriptionFinderTestTags.DATE_PICKER_TODAY",
            "formatDatePickerUtcMillis",
            "todayDdMmYyyy"
        ).forEach { ref ->
            assertThat(source).contains(ref)
        }
    }

    @Test
    fun `SubscriptionCard aplica testTags dinamicos`() {
        val source = File(
            "src/main/java/com/dscorp/ispadmin/presentation/ui/features/subscriptionfinder/compose/SubscriptionCard.kt"
        ).readText()

        assertThat(source).contains("SubscriptionFinderTestTags.resultItem")
        assertThat(source).contains("SubscriptionFinderTestTags.resultExpand")
        assertThat(source).contains("SubscriptionFinderTestTags.customerSave")
    }

    @Test
    fun `CardHeader aplica testTags de menu`() {
        val source = File(
            "src/main/java/com/dscorp/ispadmin/presentation/ui/features/subscriptionfinder/compose/CardHeader.kt"
        ).readText()

        assertThat(source).contains("SubscriptionFinderTestTags.resultMenu")
        assertThat(source).contains("SubscriptionFinderTestTags.resultMenuItem")
    }
}

class PaymentHistoryTestTagsTest {

    @Test
    fun `tags interactivos del historial de pagos estan definidos`() {
        assertThat(PaymentHistoryTestTags.interactive).containsExactly(
            PaymentHistoryTestTags.FILTER_PENDING,
            PaymentHistoryTestTags.RESTORE_CONNECTION,
            PaymentHistoryTestTags.REACTIVATION_NOTES,
            PaymentHistoryTestTags.REACTIVATE_SUBMIT
        ).inOrder()
        PaymentHistoryTestTags.interactive.forEach { tag ->
            assertThat(tag).isNotEmpty()
        }
    }

    @Test
    fun `paymentItem genera tag por id`() {
        assertThat(PaymentHistoryTestTags.paymentItem(15)).isEqualTo("payment_history_item_15")
    }

    @Test
    fun `PaymentHistoryContent aplica testTags requeridos`() {
        val source = File(
            "src/main/java/com/dscorp/ispadmin/presentation/ui/features/payment/history/PaymentHistoryContent.kt"
        ).readText()

        listOf(
            "PaymentHistoryTestTags.FILTER_PENDING",
            "PaymentHistoryTestTags.RESTORE_CONNECTION",
            "PaymentHistoryTestTags.paymentItem"
        ).forEach { ref ->
            assertThat(source).contains(ref)
        }
    }
}

class RegisterPaymentTestTagsTest {

    @Test
    fun `tags interactivos del registro de pago estan definidos`() {
        assertThat(RegisterPaymentTestTags.interactive).containsExactly(
            RegisterPaymentTestTags.BACK,
            RegisterPaymentTestTags.PAYMENT_METHOD,
            RegisterPaymentTestTags.ELECTRONIC_PAYER_NAME,
            RegisterPaymentTestTags.DISCOUNT_TOGGLE,
            RegisterPaymentTestTags.DISCOUNT_AMOUNT,
            RegisterPaymentTestTags.DISCOUNT_REASON,
            RegisterPaymentTestTags.SUBMIT,
            RegisterPaymentTestTags.SUCCESS_DISMISS
        ).inOrder()
        RegisterPaymentTestTags.interactive.forEach { tag ->
            assertThat(tag).isNotEmpty()
        }
    }

    @Test
    fun `RegisterPaymentScreen aplica testTags y ObservabilityComposeText`() {
        val source = File(
            "src/main/java/com/dscorp/ispadmin/presentation/ui/features/payment/register/RegisterPaymentScreen.kt"
        ).readText()

        listOf(
            "RegisterPaymentTestTags.BACK",
            "RegisterPaymentTestTags.PAYMENT_METHOD",
            "RegisterPaymentTestTags.ELECTRONIC_PAYER_NAME",
            "RegisterPaymentTestTags.DISCOUNT_TOGGLE",
            "RegisterPaymentTestTags.DISCOUNT_AMOUNT",
            "RegisterPaymentTestTags.DISCOUNT_REASON",
            "RegisterPaymentTestTags.SUBMIT",
            "RegisterPaymentTestTags.SUCCESS_DISMISS",
            "ObservabilityComposeText.report"
        ).forEach { ref ->
            assertThat(source).contains(ref)
        }
        assertThat(source).doesNotContain("ObservabilityComposeClick.report")
    }

    @Test
    fun `descripciones de contenido del registro de pago estan definidas`() {
        assertThat(RegisterPaymentContentDescriptions.DEBT_ICON).isNotEmpty()
        assertThat(RegisterPaymentContentDescriptions.SUCCESS_ICON).isNotEmpty()
    }
}

class SubscriptionDetailTestTagsTest {

    @Test
    fun `tags interactivos del detalle estan definidos`() {
        SubscriptionDetailTestTags.interactive.forEach { tag ->
            assertThat(tag).isNotEmpty()
        }
        assertThat(SubscriptionDetailTestTags.interactive).containsNoDuplicates()
    }

    @Test
    fun `SubscriptionDetailScreen aplica testTags requeridos`() {
        val source = File(
            "src/main/java/com/dscorp/ispadmin/presentation/ui/features/subscriptiondetail/SubscriptionDetailScreen.kt"
        ).readText()

        listOf(
            "SubscriptionDetailTestTags.CALL",
            "SubscriptionDetailTestTags.WHATSAPP",
            "SubscriptionDetailTestTags.FACADE_PHOTO"
        ).forEach { ref ->
            assertThat(source).contains(ref)
        }
    }
}
