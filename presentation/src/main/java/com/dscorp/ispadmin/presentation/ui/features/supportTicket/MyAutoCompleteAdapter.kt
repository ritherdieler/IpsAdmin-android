package com.dscorp.ispadmin.presentation.ui.features.supportTicket

import android.content.Context
import android.widget.ArrayAdapter
import android.widget.Filter
import com.dscorp.ispadmin.domain.model.SubscriptionFastSearchResponse

class MyAutoCompleteAdapter(context: Context, resource: Int, objects: List<SubscriptionFastSearchResponse>) : ArrayAdapter<SubscriptionFastSearchResponse>(context, resource, objects) {

    private val originalList: List<SubscriptionFastSearchResponse> = objects

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(constraint: CharSequence?): FilterResults {
                val results = FilterResults()
                val filteredList = ArrayList<SubscriptionFastSearchResponse>()

                if (constraint != null) {
                    for (item in originalList) {
                        if (item.fullName.contains(constraint, ignoreCase = true)) {
                            filteredList.add(item)
                        }
                    }

                    results.values = filteredList
                    results.count = filteredList.size
                }

                return results
            }

            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                clear()
                if (results != null && results.count > 0) {
                    addAll(results.values as List<SubscriptionFastSearchResponse>)
                    notifyDataSetChanged()
                } else {
                    notifyDataSetInvalidated()
                }
            }
        }
    }
}