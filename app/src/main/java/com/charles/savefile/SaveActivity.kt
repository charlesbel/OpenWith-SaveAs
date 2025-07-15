package com.charles.savefile

import android.content.ContentResolver
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Cette activité n'a pas d'interface utilisateur. Son seul but est de recevoir un fichier
 * depuis une intention VIEW ou SEND, et de le sauvegarder dans le dossier configuré
 * dans SettingsActivity.
 */
class SaveActivity : AppCompatActivity() {

    private val PREF_NAME = "SaveFilePrefs"
    private val FOLDER_URI_KEY = "folder_uri"
    private val TAG = "SaveActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "=== SaveActivity Démarrée ===")

        // Cette activité est invisible et n'a pas de fichier de layout.

        val incomingUri = handleIncomingIntent()
        val savedFolderUriString = getSharedPreferences(PREF_NAME, MODE_PRIVATE).getString(FOLDER_URI_KEY, null)

        // --- Logique Principale ---
        when {
            incomingUri == null -> {
                Log.e(TAG, "Aucun fichier reçu dans l'intent.")
                Toast.makeText(this, "Erreur : Fichier non valide.", Toast.LENGTH_SHORT).show()
                finish() // Fermer l'activité
            }
            savedFolderUriString == null -> {
                Log.e(TAG, "Aucun dossier de sauvegarde n'est configuré.")
                Toast.makeText(this, "Veuillez configurer un dossier en ouvrant l'application 'Save As' d'abord.", Toast.LENGTH_LONG).show()
                finish() // Fermer l'activité
            }
            else -> {
                val folderUri = Uri.parse(savedFolderUriString)
                val originalFilename = getFileNameFromUri(incomingUri) ?: "copie_de_fichier"
                Log.d(TAG, "Prêt à sauvegarder '$originalFilename' dans '$folderUri'")
                saveFile(incomingUri, folderUri, originalFilename)
            }
        }
    }

    private fun handleIncomingIntent(): Uri? {
        val intentAction = intent?.action
        // Gérer à la fois SEND (URI de contenu) et VIEW (souvent un URI de fichier direct)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when (intentAction) {
                Intent.ACTION_SEND -> intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
                Intent.ACTION_VIEW -> intent.data
                else -> null
            }
        } else {
            @Suppress("DEPRECATION")
            when (intentAction) {
                Intent.ACTION_SEND -> intent.getParcelableExtra(Intent.EXTRA_STREAM)
                Intent.ACTION_VIEW -> intent.data
                else -> null
            }
        }
    }

    private fun saveFile(sourceUri: Uri, folderUri: Uri, filename: String) {
        // Utiliser les Coroutines pour effectuer les opérations de fichier en dehors du thread principal
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Vérifier la permission avant de continuer
                if (!hasValidFolderPermission(folderUri)) {
                    throw SecurityException("La permission pour le dossier de destination a été perdue. Veuillez le re-sélectionner dans l'application.")
                }

                val folderDocument = DocumentFile.fromTreeUri(this@SaveActivity, folderUri)
                    ?: throw Exception("Impossible d'accéder au dossier de destination.")

                val uniqueFileName = generateUniqueFileName(folderDocument, filename)
                val mimeType = contentResolver.getType(sourceUri) ?: "application/octet-stream"

                Log.d(TAG, "Création du fichier '$uniqueFileName' (MIME: $mimeType)")
                val newFile = folderDocument.createFile(mimeType, uniqueFileName)
                    ?: throw Exception("Impossible de créer le fichier dans le dossier de destination.")

                // Copier le contenu de l'URI source vers l'URI du fichier nouvellement créé
                contentResolver.openInputStream(sourceUri)?.use { inputStream ->
                    contentResolver.openOutputStream(newFile.uri)?.use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                } ?: throw Exception("Impossible d'ouvrir le flux d'entrée du fichier source.")

                Log.d(TAG, "Copie du fichier réussie.")
                // Revenir sur le thread principal pour afficher le Toast
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@SaveActivity, "✅ Fichier '$uniqueFileName' sauvegardé", Toast.LENGTH_LONG).show()
                }

            } catch (e: Exception) {
                Log.e(TAG, "Erreur durant l'opération de sauvegarde du fichier", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@SaveActivity, "❌ Erreur: ${e.message}", Toast.LENGTH_LONG).show()
                }
            } finally {
                // Toujours terminer l'activité après la tentative
                withContext(Dispatchers.Main) {
                    finish()
                }
            }
        }
    }

    private fun hasValidFolderPermission(folderUri: Uri): Boolean {
        // Vérifier si nous avons toujours la permission d'écriture persistante pour le dossier
        val persistedPermissions = contentResolver.persistedUriPermissions
        return persistedPermissions.any { it.uri == folderUri && it.isWritePermission }
    }

    private fun generateUniqueFileName(folder: DocumentFile, originalName: String): String {
        var finalName = originalName
        var counter = 1
        val nameWithoutExt = originalName.substringBeforeLast('.')
        val ext = originalName.substringAfterLast('.', "")

        // Si un fichier avec le même nom existe, ajoute un numéro (ex: "fichier (1).txt")
        while (folder.findFile(finalName) != null) {
            finalName = if (ext.isNotEmpty()) "$nameWithoutExt ($counter).$ext" else "$nameWithoutExt ($counter)"
            counter++
        }
        return finalName
    }

    private fun getFileNameFromUri(uri: Uri): String? {
        // Essayer d'obtenir le nom de fichier depuis le content resolver
        if (uri.scheme == ContentResolver.SCHEME_CONTENT) {
            try {
                contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (nameIndex != -1) return cursor.getString(nameIndex)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Erreur lors de la requête du nom de fichier", e)
            }
        }
        // Solution de secours : obtenir le dernier segment du chemin
        return uri.lastPathSegment?.substringAfterLast('/')
    }
}
