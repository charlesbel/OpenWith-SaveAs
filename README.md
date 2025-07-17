# Save As pour Android 💾

**Save As** est une application utilitaire simple pour Android qui comble une lacune fonctionnelle : elle permet d'enregistrer rapidement une copie de n'importe quel fichier partagé depuis une autre application dans un dossier de votre choix.

Fini les galères pour trouver où un fichier a été téléchargé ou partagé ! Vous configurez un dossier une seule fois, et tous vos fichiers y seront sauvegardés.

## ✨ Fonctionnalités

* **Configuration Simple** : Une unique interface pour choisir le dossier de sauvegarde par défaut.
* **Intégration Universelle** : S'intègre parfaitement avec le menu "Partager" (`ACTION_SEND`) et "Ouvrir avec" (`ACTION_VIEW`) de n'importe quelle application.
* **Invisible et Rapide** : L'opération de sauvegarde est effectuée en arrière-plan sans interface superflue, vous notifiant simplement du succès ou de l'échec.
* **Gestion des Conflits** : Si un fichier du même nom existe déjà, l'application ajoute automatiquement un numéro (ex: `document (1).pdf`) pour éviter d'écraser vos données.
* **Persistance** : Le choix du dossier est sauvegardé et persiste même après redémarrage ou réinstallation de l'application grâce aux règles de sauvegarde Android.
* **Respect de la Vie Privée** : L'application ne demande que les permissions strictement nécessaires.

## 🚀 Comment ça marche ?

L'application est composée de deux écrans principaux :

1.  **Écran de Configuration (`SettingsActivity`)**
    * C'est l'écran que vous voyez en ouvrant l'application depuis son icône.
    * Il vous permet de sélectionner un dossier sur votre appareil qui servira de destination pour toutes les sauvegardes futures.

2.  **Écran de Sauvegarde (`SaveActivity`)**
    * Cet écran est "invisible" et n'a pas d'interface utilisateur.
    * Il se déclenche uniquement lorsque vous utilisez la fonction "Partager" ou "Ouvrir avec" depuis une autre application (votre galerie, un client mail, un gestionnaire de fichiers, etc.) et que vous choisissez "Save As".
    * Il récupère le fichier, le copie dans votre dossier configuré et se ferme automatiquement.

## 🛠️ Détails Techniques

* **Langage** : Entièrement écrit en **Kotlin**, en suivant les conventions de style officielles.
* **Asynchrone** : Utilise les **Coroutines Kotlin** pour effectuer les opérations de lecture/écriture de fichiers en dehors du thread principal, garantissant une interface fluide.
* **Accès aux Fichiers** : Repose sur le **Storage Access Framework (SAF)** d'Android pour un accès sécurisé et pérenne aux dossiers, sans nécessiter de permissions de stockage globales.
* **Configuration** : Le chemin (URI) du dossier de sauvegarde est stocké dans les `SharedPreferences`.
* **Permissions** : Demande la permission `POST_NOTIFICATIONS` sur Android 13+ pour pouvoir afficher les notifications de confirmation.

## 🔧 Compilation

Le projet est une application Android standard utilisant Gradle.

1.  Clonez le dépôt.
2.  Ouvrez-le avec Android Studio.
3.  Lancez la compilation. La version de Gradle est définie dans le fichier `gradle/wrapper/gradle-wrapper.properties`.
