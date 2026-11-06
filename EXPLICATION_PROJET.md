# Atelier Android - Application d'Administration Scolaire

## 1. Présentation du Projet
Ce projet est une application mobile Android destinée à l'administration d'une école pour la gestion des inscriptions des étudiants dans différents groupes de cours.
L'application fonctionne entièrement en mode **hors-ligne (off-line)** grâce à une base de données locale **SQLite** et supporte le multilinguisme (**Français** et **Anglais**).

---

## 2. Architecture et Fonctionnalités

### 2.1. Persistance des Données avec SQLite (`DatabaseHelper.kt`, `Student.kt`, `Group.kt`)
- **Modèle `Student` (Étudiant) :**
  - `id` : Clé primaire auto-incrémentée (INTEGER PRIMARY KEY AUTOINCREMENT)
  - `nom`, `prenom` : Nom et prénom de l'étudiant (TEXT)
  - `age` : Âge (INTEGER)
  - `groupe` : Groupe choisi (ex: G1, G2, G3, G4...) (TEXT)
  - `support` : Support de test (« Émulateur » ou « Téléphone réel ») (TEXT)
  - `is_waiting` : Statut (0 = Admis dans la classe actuelle, 1 = En liste d'attente)
  - `created_at` : Horodatage d'inscription en millisecondes pour conserver l'ordre d'arrivée (FIFO)

- **Modèle `Group` (Groupe / Classe) :**
  - `id` : Clé primaire auto-incrémentée
  - `name` : Nom unique du groupe (ex: G1, G2, G3, G4, Classe A...)
  - `capacity` : Capacité maximale configurable

- **Gestionnaire `DatabaseHelper` :**
  - Nom de la base : `school_admin.db` (Version 2)
  - Tables : `students` et `class_groups`
  - Groupes pré-initialisés par défaut : G1 (1 place), G2 (8 places), G3 (8 places)
  - Opérations sur les groupes : `getAllGroups()`, `insertGroup()`, `updateGroup()`, `deleteGroup()`, `getGroupCapacity()`
  - Opérations sur les étudiants : `insertStudent()`, `deleteStudent()`, `getStudents()` avec filtres et recherche
  - **Promotion automatique intelligente :** 
    - Lorsqu'un étudiant admis est supprimé, le premier étudiant de la liste d'attente est automatiquement promu dans la classe actuelle.
    - Si la capacité d'un groupe est augmentée dans les Paramètres, la méthode `checkAndPromoteWaitingStudents()` promeut automatiquement les étudiants en attente pour combler les nouvelles places disponibles.

---

### 2.2. Page des Paramètres (`SettingsActivity.kt` & `activity_settings.xml`)
L'écran des Paramètres regroupe deux fonctionnalités majeures :

1. **Paramètres de Langue (Language Settings) :**
   - Basculement instantané entre **Français** et **English**.
   - Implémenté via `LocaleHelper.kt` avec l'API standard `AppCompatDelegate.setApplicationLocales` et sauvegarde dans les `SharedPreferences`.
   - Tous les textes, libellés, boutons, dialogues et messages toast sont entièrement internationalisés dans `res/values/strings.xml` (Français) et `res/values-en/strings.xml` (Anglais).
   - La sélection est persistée et restaurée automatiquement au redémarrage de l'application.

2. **Gestion Dynamique des Groupes et des Capacités :**
   - **Création de nouveaux groupes :** Bouton « Nouveau groupe » avec dialogue de saisie du nom (ex: G4) et de la capacité maximale.
   - **Modification :** Modification du nom et de la capacité maximale de chaque groupe.
   - **Suppression sécurisée :** Suppression d'un groupe avec dialogue d'avertissement et suppression en cascade des étudiants associés.
   - **Indicateurs visuels en temps réel :**
     - Nombre d'inscrits par rapport à la capacité maximale (ex: `1 / 1 inscrits`).
     - Badge d'état : `Complet` (en rouge) ou `X place(s) disponible(s)` (en vert).
     - Badge de liste d'attente : `X en attente` (en orange) si des étudiants patientent.

---

### 2.3. Menu de Navigation Latéral - Navigation Drawer (`res/menu/drawer_menu.xml`)
Accessible depuis tous les écrans via le bouton hamburger ou un balayage latéral :
- **Formulaire d'inscription (`nav_inscription`)** : Saisie et enregistrement des étudiants.
- **Liste des classes (`nav_list`)** : Consultation des classes, onglets admis/attente, recherche et suppression.
- **Paramètres (`nav_settings`)** : Changement de langue et gestion des groupes / quotas.
- **Quitter (`nav_exit`)** : Confirmation avant fermeture de l'application.

---

### 2.4. Formulaire d'Inscription Dynamique (`MainActivity.kt`)
- **Compteurs dynamiques :** Une barre horizontale défilante affiche en temps réel les places disponibles et les étudiants en attente pour **tous les groupes configurés** dans SQLite.
- **Spinner dynamique :** La liste déroulante des groupes s'adapte automatiquement aux groupes créés ou modifiés dans les Paramètres.
- **Vérification de quota :**
  - Si des places sont libres, l'étudiant est admis directement dans la classe actuelle.
  - Si le groupe est complet, l'étudiant est basculé sur la **liste d'attente** avec indication de son rang (ex: `#1`).

---

### 2.5. Liste des Classes (`list_classroom.kt`)
- **Onglets (Tabs) :** Séparation claire entre **Classe actuelle** et **Liste d'attente**, avec compteurs en direct.
- **Recherche en temps réel :** Filtrage instantané par nom ou prénom avec bouton d'effacement rapide.
- **Filtre de groupe dynamique :** Sélecteur déroulant alimenté par les groupes de la base SQLite.
- **Suppression d'étudiant :** Dialogue de confirmation et promotion automatique du premier étudiant en attente.

---

## 3. Inventaire des Fichiers du Projet

| Fichier | Type | Description |
|---|---|---|
| `Group.kt` | Kotlin | Modèle de données pour les groupes et leurs capacités. |
| `Student.kt` | Kotlin | Modèle de données pour les étudiants. |
| `DatabaseHelper.kt` | Kotlin | Gestionnaire SQLite v2 (étudiants, groupes dynamiques, quotas, promotions). |
| `LocaleHelper.kt` | Kotlin | Gestionnaire et persistance de la langue (Français / Anglais). |
| `GroupAdapter.kt` | Kotlin | Adaptateur RecyclerView pour l'affichage et la gestion des groupes. |
| `StudentAdapter.kt` | Kotlin | Adaptateur RecyclerView pour la liste des étudiants. |
| `MainActivity.kt` | Kotlin | Écran principal (formulaire, compteurs dynamiques, drawer). |
| `list_classroom.kt` | Kotlin | Écran de gestion des classes (recherche, onglets, filtre dynamique). |
| `SettingsActivity.kt` | Kotlin | Écran des Paramètres (langue, création/édition/suppression de groupes). |
| `activity_settings.xml` | Layout XML | Interface utilisateur de la page des paramètres. |
| `dialog_group.xml` | Layout XML | Boîte de dialogue pour ajouter / éditer un groupe. |
| `item_group.xml` | Layout XML | Carte graphique d'un groupe avec badges de places et boutons d'action. |
| `activity_main.xml` | Layout XML | Interface du formulaire avec compteurs défilants dynamiques. |
| `activity_list_classroom.xml` | Layout XML | Interface de consultation des classes avec onglets et recherche. |
| `item_student.xml` | Layout XML | Ligne d'affichage pour chaque étudiant. |
| `values/strings.xml` | Ressources XML | Textes en Français (langue par défaut). |
| `values-en/strings.xml` | Ressources XML | Textes en Anglais (English translation). |
| `drawer_menu.xml` | Menu XML | Menu latéral avec Inscription, Liste, Paramètres, Quitter. |
| `ic_settings.xml`, `ic_add.xml`, `ic_edit.xml` | Drawables | Icônes vectorielles pour les paramètres et actions. |
| `GroupModelTest.kt` | Test Unitaire | Tests unitaires pour les modèles Group et Student. |
| `EXPLICATION_PROJET.md` | Documentation | Documentation complète du projet. |

---

## 4. Validation et Tests
- Compilation Gradle (`assembleDebug`) : **Réussie (BUILD SUCCESSFUL)**.
- Tests unitaires (`testDebugUnitTest`) : **4 passed, 0 failed, 100% succès**.
