package com.example.playlistmaker

import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ImageView
import android.widget.LinearLayout

class SettingsActivity: AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val backButton = findViewById<ImageView>(R.id.back)
        backButton.setOnClickListener {
            finish()
        }

        val shareLayout = findViewById<LinearLayout>(R.id.share_layout)
        shareLayout.setOnClickListener {
            shareApp()
        }

        val supportLayout = findViewById<LinearLayout>(R.id.support_layout)
        supportLayout.setOnClickListener {
            writeToSupport()
        }

        val agreementLayout = findViewById<LinearLayout>(R.id.agreement_layout)
        agreementLayout.setOnClickListener {
            openUserAgreement()
        }
    }

    private fun shareApp() {
        val link = getString(R.string.share_app_link)
        val message = getString(R.string.share_message, link)

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, message)
        }
        startActivity(Intent.createChooser(shareIntent, getString(R.string.share_chooser_title)))
    }

    private fun writeToSupport() {
        val email = getString(R.string.support_email)
        val subject = getString(R.string.support_subject)
        val body = getString(R.string.support_body)

        val mailIntent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:")
            putExtra(Intent.EXTRA_EMAIL, arrayOf(email))
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, body)
        }
        startActivity(Intent.createChooser(mailIntent, getString(R.string.mail_chooser_title)))
    }

    private fun openUserAgreement() {
        val url = getString(R.string.user_agreement_url)
        val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        startActivity(webIntent)
    }
}