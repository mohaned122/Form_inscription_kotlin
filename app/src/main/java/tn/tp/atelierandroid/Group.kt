package tn.tp.atelierandroid

/**
 * Modèle de données représentant une classe / un groupe d'étudiants.
 *
 * @param id Identifiant unique en base SQLite
 * @param name Nom unique du groupe (ex: G1, G2, G3, G4...)
 * @param capacity Capacité maximale d'accueil d'étudiants dans ce groupe
 */
data class Group(
    val id: Long = 0L,
    val name: String,
    val capacity: Int
)
