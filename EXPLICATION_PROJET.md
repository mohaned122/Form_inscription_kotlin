# Atelier Android - Application d'Administration Scolaire

## 1. Présentation du Projet
Ce projet est une application mobile Android destinée à l'administration d'une école pour la gestion des inscriptions des étudiants dans différents groupes de cours (**G1**, **G2**, **G3**).
L'application fonctionne entièrement en mode **hors-ligne (off-line)** grâce à une base de données locale **SQLite**.

---

## 2. Architecture et Fonctionnalités Ajoutées

### 2.1. Persistance des Données avec SQLite (`DatabaseHelper.kt` & `Student.kt`)
- **Modèle de données (`Student`) :**
  - `id` : Clé primaire auto-incrémentée (INTEGER PRIMARY KEY AUTOINCREMENT)
  - `nom` : Nom de l'étudiant (TEXT)
  - `prenom` : Prénom de l'étudiant (TEXT)
  - `age` : Âge (INTEGER)
  - `groupe` : Groupe choisi (G1, G2, G3) (TEXT)
  - `support` : Support de test (« Émulateur » ou « Téléphone réel ») (TEXT)
  - `is_waiting` : Statut (0 = Admis dans la classe actuelle, 1 = En liste d'attente)
  - `created_at` : Horodatage d'inscription en millisecondes pour conserver l'ordre d'arrivée (FIFO)

- **Gestionnaire de base de données (`DatabaseHelper`) :**
  - Nom de la base : `school_admin.db`
  - Table : `students`
  - Méthodes implémentées :
    - `insertStudent(student: Student)` : Ajout d'un étudiant.
    - `deleteStudent(id: Long)` : Suppression d'un étudiant par son ID.
    - `getStudents(isWaiting: Boolean, groupe: String?, searchQuery: String?)` : Récupération avec filtrage multicritère (classe actuelle vs attente, groupe sélectionné, recherche textuelle).
    - `getEnrolledCount(groupe: String)` : Nombre d'étudiants admis par groupe.
    - `getWaitingCount(groupe: String)` : Nombre d'étudiants en attente par groupe.
    - `getAvailablePlaces(groupe: String)` : Calcul dynamique des places restantes selon les capacités définies (G1: 1 place, G2: 8 places, G3: 8 places).
    - `promoteNextWaitingStudent(groupe: String)` : Promotion automatique du premier étudiant de la liste d'attente vers la classe actuelle lorsqu'une place se libère.

---

### 2.2. Menu de Navigation Latéral - Navigation Drawer (`res/menu/drawer_menu.xml`)
Un tiroir de navigation latéral (accessible via le bouton hamburger dans l'en-tête de chaque écran ou par glissement depuis la gauche) a été intégré dans `MainActivity` et `list_classroom` :
- **Formulaire d'inscription (`nav_inscription`)** : Ouvre l'écran du formulaire.
- **Liste des classes (`nav_list`)** : Ouvre l'écran de consultation et gestion des classes.
- **Quitter (`nav_exit`)** : Ouvre une boîte de dialogue de confirmation avant de fermer l'application (`finishAffinity()`).
- En-tête personnalisé (`drawer_header.xml`) aux couleurs de la charte graphique (Navy, Teal).

---

### 2.3. Gestion de la Liste des Classes (`list_classroom.kt` & `activity_list_classroom.xml`)
L'écran de consultation des classes offre une suite complète d'outils d'administration :

1. **Onglets (Tabs) : Classe actuelle vs Liste d'attente :**
   - Un `TabLayout` permet de basculer instantanément entre la liste des étudiants admis dans la **Classe actuelle** et la **Liste d'attente**.
   - Des compteurs dynamiques sont intégrés aux onglets (ex. `Classe actuelle (3)`, `Liste d'attente (1)`).
2. **Recherche dynamique par nom ou prénom :**
   - Champ de recherche en temps réel (`TextWatcher`) permettant de filtrer les étudiants dès la frappe.
   - Bouton de réinitialisation rapide de la recherche.
3. **Filtre par groupe :**
   - Sélecteur déroulant (`Spinner`) pour filtrer par groupe spécifique (`Tous les groupes`, `G1`, `G2`, `G3`).
4. **Suppression avec boîte de dialogue de confirmation :**
   - Bouton de suppression (`btnSupprimer`) sur chaque ligne de la liste (`item_student.xml`).
   - Dialogue d'avertissement demandant confirmation avant la suppression définitive.
   - **Promotion intelligente de la liste d'attente :** Si un étudiant admis dans un groupe est supprimé, le système promeut automatiquement le premier étudiant en attente pour ce même groupe et en informe l'administrateur par un Toast.
5. **Indicateurs de position :**
   - Affichage de la position `#1, #2...` (rang d'attente en orange pour la liste d'attente, numéro d'étudiant en vert pour la classe actuelle).

---

### 2.4. Amélioration du Formulaire d'Inscription (`MainActivity.kt`)
- Les compteurs de places disponibles (`tvPlacesG1`, `tvPlacesG2`, `tvPlacesG3`) sont synchronisés en temps réel avec la base SQLite et affichent aussi le nombre d'inscrits en liste d'attente.
- Si un groupe a des places libres, l'étudiant est admis directement.
- Si le groupe est complet, l'étudiant est automatiquement inscrit sur la **liste d'attente** et sa position lui est notifiée.
- Les données saisies persistent même après la fermeture complète de l'application.

---

## 3. Inventaire des Fichiers Modifiés et Ajoutés

| Fichier | Type | Description |
|---|---|---|
| `Student.kt` | Code Kotlin | Modèle de données pour les étudiants. |
| `DatabaseHelper.kt` | Code Kotlin | Gestionnaire SQLite (CRUD, capacités, promotion d'attente). |
| `StudentAdapter.kt` | Code Kotlin | Adaptateur RecyclerView pour l'affichage de la liste. |
| `MainActivity.kt` | Code Kotlin | Formulaire d'inscription relié à SQLite + Navigation Drawer. |
| `list_classroom.kt` | Code Kotlin | Activité de gestion des classes (Tabs, recherche, suppression). |
| `activity_main.xml` | Layout XML | Ajout du DrawerLayout, bouton hamburger et navigation. |
| `activity_list_classroom.xml` | Layout XML | Écran complet des classes (Header, Recherche, Filtre, Tabs, RecyclerView). |
| `item_student.xml` | Layout XML | Ligne d'affichage pour chaque étudiant (Position, Nom, Infos, Groupe, Poubelle). |
| `drawer_header.xml` | Layout XML | En-tête du menu latéral. |
| `drawer_menu.xml` | Menu XML | Définition des entrées du menu (Inscription, Liste, Quitter). |
| `strings.xml`, `colors.xml`, `arrays.xml` | Ressources XML | Textes, thèmes de couleurs et tableaux de sélection. |
| `ic_*.xml` | Drawables Vectoriels | Icônes (menu hamburger, inscription, liste, quitter, recherche, effacer, supprimer). |
| `EXPLICATION_PROJET.md` | Documentation | Ce document explicatif. |

---

## 4. Validation et Tests
- Compilation Gradle (`assembleDebug`) : **Réussie sans aucune erreur**.
- Tests unitaires (`testDebugUnitTest`) : **Réussis (100% passés)**.
