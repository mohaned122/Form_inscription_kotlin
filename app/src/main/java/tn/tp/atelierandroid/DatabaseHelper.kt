package tn.tp.atelierandroid

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/**
 * Gestionnaire de base de données SQLite pour l'application d'administration scolaire.
 * Gère la persistance des étudiants, des groupes dynamiques, des capacités et des listes d'attente.
 */
class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        const val DATABASE_NAME = "school_admin.db"
        const val DATABASE_VERSION = 2

        // Table des étudiants
        const val TABLE_STUDENTS = "students"
        const val COLUMN_ID = "id"
        const val COLUMN_NOM = "nom"
        const val COLUMN_PRENOM = "prenom"
        const val COLUMN_AGE = "age"
        const val COLUMN_GROUPE = "groupe"
        const val COLUMN_SUPPORT = "support"
        const val COLUMN_IS_WAITING = "is_waiting"
        const val COLUMN_CREATED_AT = "created_at"

        // Table des groupes
        const val TABLE_GROUPS = "class_groups"
        const val COLUMN_GROUP_ID = "id"
        const val COLUMN_GROUP_NAME = "name"
        const val COLUMN_GROUP_CAPACITY = "capacity"

        // Capacités de secours par défaut si la table est vide
        val DEFAULT_GROUPS = listOf(
            "G1" to 1,
            "G2" to 8,
            "G3" to 8
        )
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createStudentsTable = """
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
        db.execSQL(createStudentsTable)

        val createGroupsTable = """
            CREATE TABLE $TABLE_GROUPS (
                $COLUMN_GROUP_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_GROUP_NAME TEXT UNIQUE NOT NULL,
                $COLUMN_GROUP_CAPACITY INTEGER NOT NULL
            )
        """.trimIndent()
        db.execSQL(createGroupsTable)

        seedDefaultGroups(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            val createGroupsTable = """
                CREATE TABLE IF NOT EXISTS $TABLE_GROUPS (
                    $COLUMN_GROUP_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                    $COLUMN_GROUP_NAME TEXT UNIQUE NOT NULL,
                    $COLUMN_GROUP_CAPACITY INTEGER NOT NULL
                )
            """.trimIndent()
            db.execSQL(createGroupsTable)
            seedDefaultGroups(db)
        }
    }

    /**
     * Initialise les groupes par défaut si la table est vide.
     */
    private fun seedDefaultGroups(db: SQLiteDatabase) {
        for ((name, capacity) in DEFAULT_GROUPS) {
            val values = ContentValues().apply {
                put(COLUMN_GROUP_NAME, name)
                put(COLUMN_GROUP_CAPACITY, capacity)
            }
            db.insertWithOnConflict(TABLE_GROUPS, null, values, SQLiteDatabase.CONFLICT_IGNORE)
        }
    }

    /**
     * Vérifie et s'assure que la table des groupes contient au moins les groupes par défaut.
     */
    fun ensureDefaultGroupsExist() {
        val db = writableDatabase
        val countCursor = db.rawQuery("SELECT COUNT(*) FROM $TABLE_GROUPS", null)
        val count = countCursor.use {
            if (it.moveToFirst()) it.getInt(0) else 0
        }
        if (count == 0) {
            seedDefaultGroups(db)
        }
    }

    // =========================================================================
    // GESTION DES GROUPES ET DES CAPACITÉS
    // =========================================================================

    /**
     * Récupère la liste de tous les groupes configurés, triés par nom.
     */
    fun getAllGroups(): List<Group> {
        ensureDefaultGroupsExist()
        val groups = mutableListOf<Group>()
        val db = readableDatabase
        val cursor = db.query(
            TABLE_GROUPS,
            null,
            null,
            null,
            null,
            null,
            "$COLUMN_GROUP_NAME ASC"
        )
        cursor.use {
            while (it.moveToNext()) {
                val id = it.getLong(it.getColumnIndexOrThrow(COLUMN_GROUP_ID))
                val name = it.getString(it.getColumnIndexOrThrow(COLUMN_GROUP_NAME))
                val capacity = it.getInt(it.getColumnIndexOrThrow(COLUMN_GROUP_CAPACITY))
                groups.add(Group(id, name, capacity))
            }
        }
        return groups
    }

    /**
     * Récupère la liste des noms de groupes sous forme de liste de chaînes.
     */
    fun getAllGroupNames(): List<String> {
        return getAllGroups().map { it.name }
    }

    /**
     * Récupère un groupe par son identifiant.
     */
    fun getGroupById(id: Long): Group? {
        val db = readableDatabase
        val cursor = db.query(
            TABLE_GROUPS,
            null,
            "$COLUMN_GROUP_ID = ?",
            arrayOf(id.toString()),
            null,
            null,
            null
        )
        return cursor.use {
            if (it.moveToFirst()) {
                val groupId = it.getLong(it.getColumnIndexOrThrow(COLUMN_GROUP_ID))
                val name = it.getString(it.getColumnIndexOrThrow(COLUMN_GROUP_NAME))
                val capacity = it.getInt(it.getColumnIndexOrThrow(COLUMN_GROUP_CAPACITY))
                Group(groupId, name, capacity)
            } else null
        }
    }

    /**
     * Récupère un groupe par son nom.
     */
    fun getGroupByName(name: String): Group? {
        val db = readableDatabase
        val cursor = db.query(
            TABLE_GROUPS,
            null,
            "$COLUMN_GROUP_NAME = ?",
            arrayOf(name.trim()),
            null,
            null,
            null
        )
        return cursor.use {
            if (it.moveToFirst()) {
                val groupId = it.getLong(it.getColumnIndexOrThrow(COLUMN_GROUP_ID))
                val groupName = it.getString(it.getColumnIndexOrThrow(COLUMN_GROUP_NAME))
                val capacity = it.getInt(it.getColumnIndexOrThrow(COLUMN_GROUP_CAPACITY))
                Group(groupId, groupName, capacity)
            } else null
        }
    }

    /**
     * Insère un nouveau groupe avec sa capacité maximale.
     * @return ID généré ou -1 si échec (ex: nom déjà existant)
     */
    fun insertGroup(name: String, capacity: Int): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_GROUP_NAME, name.trim())
            put(COLUMN_GROUP_CAPACITY, capacity)
        }
        return db.insert(TABLE_GROUPS, null, values)
    }

    /**
     * Met à jour le nom et/ou la capacité d'un groupe existant.
     * Si la capacité augmente, promeut automatiquement les étudiants en attente !
     */
    fun updateGroup(id: Long, newName: String, newCapacity: Int): Boolean {
        val db = writableDatabase
        val oldGroup = getGroupById(id) ?: return false
        val oldName = oldGroup.name
        val trimmedNewName = newName.trim()

        val values = ContentValues().apply {
            put(COLUMN_GROUP_NAME, trimmedNewName)
            put(COLUMN_GROUP_CAPACITY, newCapacity)
        }

        val rows = db.update(TABLE_GROUPS, values, "$COLUMN_GROUP_ID = ?", arrayOf(id.toString()))
        if (rows > 0) {
            // Si le nom du groupe a changé, mettre à jour les étudiants associés
            if (oldName != trimmedNewName) {
                val studentValues = ContentValues().apply {
                    put(COLUMN_GROUPE, trimmedNewName)
                }
                db.update(TABLE_STUDENTS, studentValues, "$COLUMN_GROUPE = ?", arrayOf(oldName))
            }

            // Si la capacité a augmenté, promouvoir les étudiants en attente
            checkAndPromoteWaitingStudents(trimmedNewName)
            return true
        }
        return false
    }

    /**
     * Supprime un groupe et tous les étudiants qui lui sont associés.
     */
    fun deleteGroup(id: Long): Boolean {
        val db = writableDatabase
        val group = getGroupById(id) ?: return false
        // Supprimer les étudiants inscrits dans ce groupe
        db.delete(TABLE_STUDENTS, "$COLUMN_GROUPE = ?", arrayOf(group.name))
        // Supprimer le groupe
        val rows = db.delete(TABLE_GROUPS, "$COLUMN_GROUP_ID = ?", arrayOf(id.toString()))
        return rows > 0
    }

    /**
     * Récupère la capacité maximale définie pour un groupe.
     */
    fun getGroupCapacity(groupe: String): Int {
        val db = readableDatabase
        val cursor = db.query(
            TABLE_GROUPS,
            arrayOf(COLUMN_GROUP_CAPACITY),
            "$COLUMN_GROUP_NAME = ?",
            arrayOf(groupe.trim()),
            null,
            null,
            null
        )
        return cursor.use {
            if (it.moveToFirst()) {
                it.getInt(0)
            } else {
                // Fallback de secours
                DEFAULT_GROUPS.find { def -> def.first == groupe }?.second ?: 8
            }
        }
    }

    // =========================================================================
    // GESTION DES ÉTUDIANTS
    // =========================================================================

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

        selectionClauses.add("$COLUMN_IS_WAITING = ?")
        selectionArgs.add(if (isWaiting) "1" else "0")

        if (!groupe.isNullOrEmpty() && groupe != "Tous" && groupe != "All") {
            selectionClauses.add("$COLUMN_GROUPE = ?")
            selectionArgs.add(groupe)
        }

        if (!searchQuery.isNullOrBlank()) {
            selectionClauses.add("($COLUMN_NOM LIKE ? OR $COLUMN_PRENOM LIKE ?)")
            val pattern = "%${searchQuery.trim()}%"
            selectionArgs.add(pattern)
            selectionArgs.add(pattern)
        }

        val whereClause = selectionClauses.joinToString(" AND ")
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
     * Calcule le nombre de places restantes dans un groupe par rapport à sa capacité dynamique.
     */
    fun getAvailablePlaces(groupe: String): Int {
        val capacity = getGroupCapacity(groupe)
        val enrolled = getEnrolledCount(groupe)
        val remaining = capacity - enrolled
        return if (remaining > 0) remaining else 0
    }

    /**
     * Si une place se libère dans un groupe (par ex. suite à une suppression ou hausse de quota),
     * promeut automatiquement le premier étudiant de la liste d'attente vers la classe actuelle.
     * @return L'étudiant promu, ou null s'il n'y avait aucun étudiant en attente
     */
    fun promoteNextWaitingStudent(groupe: String): Student? {
        val db = writableDatabase
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

    /**
     * Vérifie s'il y a des places disponibles et promeut autant d'étudiants en attente que possible.
     * @return Liste des étudiants promus
     */
    fun checkAndPromoteWaitingStudents(groupe: String): List<Student> {
        val promoted = mutableListOf<Student>()
        while (getAvailablePlaces(groupe) > 0) {
            val student = promoteNextWaitingStudent(groupe) ?: break
            promoted.add(student)
        }
        return promoted
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
