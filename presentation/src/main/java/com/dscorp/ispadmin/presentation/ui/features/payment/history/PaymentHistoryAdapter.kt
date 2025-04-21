package com.dscorp.ispadmin.presentation.ui.features.payment.history

import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.dscorp.ispadmin.R
import com.dscorp.ispadmin.databinding.PaymentHistoryItemBinding
import com.dscorp.ispadmin.domain.model.Payment
import java.util.Locale


class PaymentHistoryAdapter(
    val listener: PaymentHistoryAdapterListener,
    diffCallback: PaymentHistoryDiffCallback = PaymentHistoryDiffCallback()
) :
    ListAdapter<Payment, PaymentHistoryAdapter.PaymentHistoryViewHolder>(diffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PaymentHistoryViewHolder {
        val binding =
            PaymentHistoryItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PaymentHistoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PaymentHistoryViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class PaymentHistoryViewHolder(private val binding: PaymentHistoryItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(payment: Payment) {
            binding.payment = payment
            binding.executePendingBindings()

            setPaymentColor(payment)
            binding.tvPaymentAmount.text = generatePaymentSpanText(payment)
             getPaymentMethodIcon(payment.method)?.let {
                 binding.ivPaymentMethod.setImageResource(it)
             }
            binding.root.setOnClickListener {
                listener.onPaymentHistoryItemClicked(payment)
            }
        }

        private fun setPaymentColor(payment: Payment) {
            binding.tvPaymentStatus.setTextColor(
                if (!payment.paid) binding.root.context.getColor(R.color.red)
                else binding.root.context.getColor(R.color.green)
            )
        }

        private fun getPaymentMethodIcon(method: String?): Int? {
           return  if (method != null) {
                return when (method.lowercase(Locale.ROOT)) {
                    "yape" -> R.drawable.yape
                    "plin" -> R.drawable.plin
                    "transferencia" -> R.drawable.bcp
                    "efectivo" -> R.drawable.efectivo
                    else -> null
                }
            } else null
        }

        private fun generatePaymentSpanText(payment: Payment): SpannableString {
            val discountAmountStr = payment.discountAmountStr()

            val hasDiscount = discountAmountStr.isNotEmpty()

            val formattedText = if (hasDiscount) {
                val formattedDiscount = " - $discountAmountStr"
                "${payment.amountToPayStr()}$formattedDiscount"
            } else {
                payment.amountToPayStr()
            }

            val spannableText = SpannableString(formattedText)

            if (hasDiscount) {
                val discountStart = formattedText.indexOf(discountAmountStr)
                val discountEnd = discountStart + discountAmountStr.length

                val smallTextSize = 0.6f
                val redColor = binding.root.context.getColor(R.color.red)

                val sizeSpan = RelativeSizeSpan(smallTextSize)
                val colorSpan = ForegroundColorSpan(redColor)

                spannableText.setSpan(sizeSpan, discountStart, discountEnd, 0)
                spannableText.setSpan(colorSpan, discountStart, discountEnd, 0)
            }

            return spannableText
        }


    }
}

class PaymentHistoryDiffCallback : DiffUtil.ItemCallback<Payment>() {
    override fun areItemsTheSame(oldItem: Payment, newItem: Payment): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: Payment, newItem: Payment): Boolean {
        return oldItem.id == newItem.id
    }
}


