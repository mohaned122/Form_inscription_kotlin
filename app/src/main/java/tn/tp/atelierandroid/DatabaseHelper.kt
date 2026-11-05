package tn.tp.atelierandroid

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/**
 * Gestionnaire de base de données SQLite pour l'application d'administration scolaire.
 * Gère la persistance des étudiants, la répartition en classe actuelle et liste d'attente,
 * ainsi que le calcul dynamique des places restantes par groupe.
 */
class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        const val DATABASE_NAME = "school_admin.db"
        const val DATABASE_VERSION = 1

        const val TABLE_STUDENTS = "students"
        const val COLUMN_ID = "id"
        const val COLUMN_NOM = "nom"
        const val COLUMN_PRENOM = "prenom"
        const val COLUMN_AGE = "age"
        const val COLUMN_GROUPE = "groupe"
        const val COLUMN_SUPPORT = "support"
        const val COLUMN_IS_WAITING = "is_waiting"
        const val COLUMN_CREATED_AT = "created_at"

        // Capacités maximales de base pour chaque classe
        val MAX_CAPACITIES = mapOf(
            "G1" to 1,
            "G2" to 8,
            "G3" to 8
        )
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createTableQuery = """
            CREATE TABLE $TABLE_STUDENTS (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_NOM TEXT NOT NULL,
                $COLUMN_PRENOM TEXT NOT NULL,
                $COLUMN_AGE INTEGER NOT NULL,
                $COLUMN_GROUPE TEXT NOT NULL,
                $COLUMN_SUPPORT TEXT NOT NULL,
                $COLUMN_IS_WAITING INTEGER NOT NULL DEFAULT 0,
                $COLUMN_CREATED_AT INTEGER NOT NULL
            )
        """.trimIndent()
        db.execSQL(createTableQuery)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_STUDENTS")
        onCreate(db)
    }

    /**
     * Insère un nouvel étudiant dans la base de données.
     * @return L'ID généré de l'étudiant, ou -1 en cas d'échec
     */
    fun insertStudent(student: Student): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_NOM, student.nom)
            put(COLUMN_PRENOM, student.prenom)
            put(COLUMN_AGE, student.age)
            put(COLUMN_GROUPE, student.groupe)
            put(COLUMN_SUPPORT, student.support)
            put(COLUMN_IS_WAITING, if (student.isWaiting) 1 else 0)
            put(COLUMN_CREATED_AT, student.createdAt)
        }
        return db.insert(TABLE_STUDENTS, null, values)
    }

    /**
     * Supprime un étudiant par son identifiant unique.
     * @return true si au moins une ligne a été supprimée, false sinon
     */
    fun deleteStudent(id: Long): Boolean {
        val db = writableDatabase
        val rows = db.delete(TABLE_STUDENTS, "$COLUMN_ID = ?", arrayOf(id.toString()))
        return rows > 0
    }

    /**
     * Récupère un étudiant par son ID.
     */
    fun getStudentById(id: Long): Student? {
        val db = readableDatabase
        val cursor = db.query(
            TABLE_STUDENTS,
            null,
            "$COLUMN_ID = ?",
            arrayOf(id.toString()),
            null,
            null,
            null
        )
        return cursor.use {
            if (it.moveToFirst()) cursorToStudent(it) else null
        }
    }

    /**
     * Récupère la liste des étudiants en fonction de leur statut (classe actuelle ou attente),
     * avec filtres optionnels par groupe et recherche textuelle par nom/prénom.
     */
    fun getStudents(
        isWaiting: Boolean,
        groupe: String? = null,
        searchQuery: String? = null
    ): List<Student> {
        val students = mutableListOf<Student>()
        val db = readableDatabase

        val selectionClauses = mutableListOf<String>()
        val selectionArgs = mutableListOf<String>()

        // Filtre selon le statut (0: classe actuelle, 1: liste d'attente)
        selectionClauses.add("$COLUMN_IS_WAITING = ?")
        selectionArgs.add(if (isWaiting) "1" else "0")

        // Filtre par groupe
        if (!groupe.isNullOrEmpty() && groupe != "Tous") {
            selectionClauses.add("$COLUMN_GROUPE = ?")
            selectionArgs.add(groupe)
        }

        // Recherche par nom ou prénom
        if (!searchQuery.isNullOrBlank()) {
            selectionClauses.add("($COLUMN_NOM LIKE ? OR $COLUMN_PRENOM LIKE ?)")
            val pattern = "%${searchQuery.trim()}%"
            selectionArgs.add(pattern)
            selectionArgs.add(pattern)
        }

        val whereClause = selectionClauses.joinToString(" AND ")
        // Ordre d'affichage : liste d'attente triée par date d'arrivée ASC, classe triée par nom ASC
        val orderBy = if (isWaiting) {
            "$COLUMN_CREATED_AT ASC, $COLUMN_ID ASC"
        } else {
            "$COLUMN_GROUPE ASC, $COLUMN_NOM ASC, $COLUMN_PRENOM ASC"
        }

        val cursor = db.query(
            TABLE_STUDENTS,
            null,
            whereClause,
            selectionArgs.toTypedArray(),
            null,
            null,
            orderBy
        )

        cursor.use {
            while (it.moveToNext()) {
                students.add(cursorToStudent(it))
            }
        }
        return students
    }

    /**
     * Compte le nombre d'étudiants admis (classe actuelle) dans un groupe donné.
     */
    fun getEnrolledCount(groupe: String): Int {
        val db = readableDatabase
        val query = "SELECT COUNT(*) FROM $TABLE_STUDENTS WHERE $COLUMN_GROUPE = ? AND $COLUMN_IS_WAITING = 0"
        val cursor = db.rawQuery(query, arrayOf(groupe))
        return cursor.use {
            if (it.moveToFirst()) it.getInt(0) else 0
        }
    }

    /**
     * Compte le nombre d'étudiants sur liste d'attente dans un groupe donné.
     */
    fun getWaitingCount(groupe: String): Int {
        val db = readableDatabase
        val query = "SELECT COUNT(*) FROM $TABLE_STUDENTS WHERE $COLUMN_GROUPE = ? AND $COLUMN_IS_WAITING = 1"
        val cursor = db.rawQuery(query, arrayOf(groupe))
        return cursor.use {
            if (it.moveToFirst()) it.getInt(0) else 0
        }
    }

    /**
     * Calcule le nombre de places restantes dans un groupe.
     */
    fun getAvailablePlaces(groupe: String): Int {
        val max = MAX_CAPACITIES[groupe] ?: 0
        val enrolled = getEnrolledCount(groupe)
        val remaining = max - enrolled
        return if (remaining > 0) remaining else 0
    }

    /**
     * Si une place se libère dans un groupe (par ex. suite à une suppression),
     * promeut automatiquement le premier étudiant de la liste d'attente vers la classe actuelle.
     * @return L'étudiant promu, ou null s'il n'y avait aucun étudiant en attente
     */
    fun promoteNextWaitingStudent(groupe: String): Student? {
        val db = writableDatabase
        // Trouver le premier étudiant en attente pour ce groupe
        val query = """
            SELECT * FROM $TABLE_STUDENTS 
            WHERE $COLUMN_GROUPE = ? AND $COLUMN_IS_WAITING = 1 
            ORDER BY $COLUMN_CREATED_AT ASC, $COLUMN_ID ASC 
            LIMIT 1
        """.trimIndent()

        var waitingStudent: Student? = null
        db.rawQuery(query, arrayOf(groupe)).use { cursor ->
            if (cursor.moveToFirst()) {
                waitingStudent = cursorToStudent(cursor)
            }
        }

        waitingStudent?.let { student ->
            val values = ContentValues().apply {
                put(COLUMN_IS_WAITING, 0)
            }
            db.update(TABLE_STUDENTS, values, "$COLUMN_ID = ?", arrayOf(student.id.toString()))
            return student.copy(isWaiting = false)
        }

        return null
    }

    private fun cursorToStudent(cursor: Cursor): Student {
        val id = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ID))
        val nom = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NOM))
        val prenom = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PRENOM))
        val age = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_AGE))
        val groupe = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_GROUPE))
        val support = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_SUPPORT))
        val isWaiting = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_IS_WAITING)) == 1
        val createdAt = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_CREATED_AT))

        return Student(
            id = id,
            nom = nom,
            prenom = prenom,
            age = age,
            groupe = groupe,
            support = support,
            isWaiting = isWaiting,
            createdAt = createdAt
        )
    }
}
