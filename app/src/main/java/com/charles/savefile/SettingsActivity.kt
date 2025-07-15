package com.charles.savefile

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.documentfile.provider.DocumentFile

class SettingsActivity : AppCompatActivity() {

    private lateinit var tvCurrentFolder: TextView
    private val PREF_NAME = "SaveFilePrefs"
    private val FOLDER_URI_KEY = "folder_uri"
    private val TAG = "SettingsActivity"

    private val selectFolderLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            try {
                // Prendre les permissions persistantes
                val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                contentResolver.takePersistableUriPermission(uri, flags)

                // Sauvegarder dans les SharedPreferences
                getSharedPreferences(PREF_NAME, MODE_PRIVATE).edit()
                    .putString(FOLDER_URI_KEY, uri.toString())
                    .apply()

                Log.d(TAG, "Dossier de sauvegarde mis à jour: $uri")
                Toast.makeText(this, "Dossier de sauvegarde mis à jour.", Toast.LENGTH_SHORT).show()
                updateCurrentFolderDisplay()
            } catch (e: Exception) {
                Log.e(TAG, "Erreur lors de la prise de permission", e)
                Toast.makeText(this, "Impossible d'obtenir les permissions pour ce dossier.", Toast.LENGTH_LONG).show()
            }
        } else {
            Log.w(TAG, "L'utilisateur n'a sélectionné aucun dossier.")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        setTitle("Configuration du Dossier")

        tvCurrentFolder = findViewById(R.id.tv_current_folder)
        val btnSelectFolder = findViewById<Button>(R.id.btn_select_folder)

        btnSelectFolder.setOnClickListener {
            selectFolderLauncher.launch(null)
        }
    }

    override fun onResume() {
        super.onResume()
        updateCurrentFolderDisplay()
    }

    private fun updateCurrentFolderDisplay() {
        val savedUriString = getSharedPreferences(PREF_NAME, MODE_PRIVATE).getString(FOLDER_URI_KEY, null)
        if (savedUriString != null) {
            val uri = Uri.parse(savedUriString)
            // Essayer de récupérer un nom de dossier lisible
            val docFile = DocumentFile.fromTreeUri(this, uri)
            val folderName = docFile?.name ?: "Dossier non accessible"
            tvCurrentFolder.text = "Dossier actuel : $folderName"
        } else {
            tvCurrentFolder.text = "Aucun dossier de sauvegarde configuré."
        }
    }
}
