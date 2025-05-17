package com.dscorp.ispadmin.presentation.ui.features.supportTicket.closedTickets

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.dscorp.ispadmin.R
import com.dscorp.ispadmin.databinding.ActivityPhotoViewerBinding
import com.dscorp.ispadmin.presentation.ui.features.composecomponents.ZoomableImage
import com.dscorp.ispadmin.presentation.ui.features.supportTicket.SupportTicketHelper
import com.example.data2.data.response.AssistanceTicketResponse

class PhotoViewer : AppCompatActivity() {


    private lateinit var binding: ActivityPhotoViewerBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPhotoViewerBinding.inflate(layoutInflater)
        enableEdgeToEdge()

        val ticket = intent.getSerializableExtra("ticket") as AssistanceTicketResponse?
        if (ticket == null) finish()
        binding.root.setContent {
            ZoomableImage(
                imageUrl = ticket!!.sheetImageUrl,
            )
        }

        setContentView(binding.root)
    }
}