package tn.tp.atelierandroid

/**
 * Modèle représentant un étudiant inscrit.
 *
 * @param id Identifiant unique auto-incrémenté en base de données SQLite
 * @param nom Nom de l'étudiant
 * @param prenom Prénom de l'étudiant
 * @param age Âge de l'étudiant
 * @param groupe Groupe attribué (G1, G2, G3)
 * @param support Type de support ("Émulateur" ou "Téléphone réel")
 * @param isWaiting true si l'étudiant est sur la liste d'attente, false s'il est admis dans la classe actuelle
 * @param createdAt Horodatage d'inscription (permet de trier la liste d'attente par ordre d'arrivée)
 */
data class Student(
    val id: Long = 0L,
    val nom: String,
    val prenom: String,
    val age: Int,
    val groupe: String,
    val support: String,
    val isWaiting: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
) {
    val nomComplet: String
        get() = "$nom $prenom"

    val infos: String
        get() = "$age ans · $support"
}
