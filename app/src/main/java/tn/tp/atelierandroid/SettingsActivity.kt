package tn.tp.atelierandroid

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.navigation.NavigationView

/**
 * Activité des Paramètres :
 * - Gestion de la langue de l'application (Français / Anglais)
 * - Gestion dynamique des groupes (Création, Modification, Suppression)
 * - Gestion des capacités maximales des classes et suivi en temps réel des quotas
 */
class SettingsActivity : AppCompatActivity() {

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var rgLanguage: RadioGroup
    private lateinit var rbLangFr: RadioButton
    private lateinit var rbLangEn: RadioButton
    private lateinit var btnAjouterGroupe: Button
    private lateinit var rvGroups: RecyclerView
    private lateinit var tvEmptyGroups: TextView
    private lateinit var groupAdapter: GroupAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        dbHelper = DatabaseHelper(this)

        initViews()
        setupNavigationDrawer()
        setupLanguageSection()
        setupGroupsRecyclerView()
        setupAddGroupButton()
    }

    override fun onResume() {
        super.onResume()
        navigationView.setCheckedItem(R.id.nav_settings)
        loadGroups()
    }

    private fun initViews() {
        drawerLayout = findViewById(R.id.drawerLayout)
        navigationView = findViewById(R.id.navigationView)
        rgLanguage = findViewById(R.id.rgLanguage)
        rbLangFr = findViewById(R.id.rbLangFr)
        rbLangEn = findViewById(R.id.rbLangEn)
        btnAjouterGroupe = findViewById(R.id.btnAjouterGroupe)
        rvGroups = findViewById(R.id.rvGroups)
        tvEmptyGroups = findViewById(R.id.tvEmptyGroups)
    }

    /**
     * Configure la sélection de langue et sa persistance.
     */
    private fun setupLanguageSection() {
        val currentLang = LocaleHelper.getLanguage(this)
        if (currentLang == LocaleHelper.LANG_EN) {
            rbLangEn.isChecked = true
        } else {
            rbLangFr.isChecked = true
        }

        rgLanguage.setOnCheckedChangeListener { _, checkedId ->
            val targetLang = if (checkedId == R.id.rbLangEn) LocaleHelper.LANG_EN else LocaleHelper.LANG_FR
            if (targetLang != LocaleHelper.getLanguage(this)) {
                LocaleHelper.setLocale(this, targetLang)
                Toast.makeText(this, R.string.toast_language_changed, Toast.LENGTH_SHORT).show()
                recreate()
            }
        }
    }

    /**
     * Configure le RecyclerView des groupes avec actions Modifier et Supprimer.
     */
    private fun setupGroupsRecyclerView() {
        rvGroups.layoutManager = LinearLayoutManager(this)
        groupAdapter = GroupAdapter(
            groups = emptyList(),
            dbHelper = dbHelper,
            onEditClick = { group ->
                showEditGroupDialog(group)
            },
            onDeleteClick = { group ->
                showDeleteGroupDialog(group)
            }
        )
        rvGroups.adapter = groupAdapter
    }

    private fun setupAddGroupButton() {
        btnAjouterGroupe.setOnClickListener {
            showAddGroupDialog()
        }
    }

    /**
     * Charge les groupes depuis SQLite et actualise l'affichage.
     */
    private fun loadGroups() {
        val groups = dbHelper.getAllGroups()
        groupAdapter.updateData(groups)

        if (groups.isEmpty()) {
            rvGroups.visibility = View.GONE
            tvEmptyGroups.visibility = View.VISIBLE
        } else {
            rvGroups.visibility = View.VISIBLE
            tvEmptyGroups.visibility = View.GONE
        }
    }

    /**
     * Dialogue pour créer un nouveau groupe.
     */
    private fun showAddGroupDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_group, null)
        val etName = dialogView.findViewById<EditText>(R.id.etDialogGroupName)
        val etCapacity = dialogView.findViewById<EditText>(R.id.etDialogGroupCapacity)

        AlertDialog.Builder(this)
            .setTitle(R.string.dialog_add_group_title)
            .setView(dialogView)
            .setPositiveButton(R.string.btn_enregistrer) { _, _ ->
                val name = etName.text.toString().trim()
                val capacity = etCapacity.text.toString().toIntOrNull()

                when {
                    name.isEmpty() -> {
                        Toast.makeText(this, R.string.error_group_name_empty, Toast.LENGTH_SHORT).show()
                    }
                    dbHelper.getGroupByName(name) != null -> {
                        Toast.makeText(this, R.string.error_group_name_exists, Toast.LENGTH_SHORT).show()
                    }
                    capacity == null || capacity <= 0 -> {
                        Toast.makeText(this, R.string.error_group_capacity_invalid, Toast.LENGTH_SHORT).show()
                    }
                    else -> {
                        val result = dbHelper.insertGroup(name, capacity)
                        if (result != -1L) {
                            Toast.makeText(
                                this,
                                getString(R.string.group_added_success, name, capacity),
                                Toast.LENGTH_SHORT
                            ).show()
                            loadGroups()
                        }
                    }
                }
            }
            .setNegativeButton(R.string.btn_annuler, null)
            .show()
    }

    /**
     * Dialogue pour modifier un groupe existant (nom et/ou capacité).
     */
    private fun showEditGroupDialog(group: Group) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_group, null)
        val etName = dialogView.findViewById<EditText>(R.id.etDialogGroupName)
        val etCapacity = dialogView.findViewById<EditText>(R.id.etDialogGroupCapacity)

        etName.setText(group.name)
        etCapacity.setText(group.capacity.toString())

        AlertDialog.Builder(this)
            .setTitle(R.string.dialog_edit_group_title)
            .setView(dialogView)
            .setPositiveButton(R.string.btn_enregistrer) { _, _ ->
                val newName = etName.text.toString().trim()
                val newCapacity = etCapacity.text.toString().toIntOrNull()

                when {
                    newName.isEmpty() -> {
                        Toast.makeText(this, R.string.error_group_name_empty, Toast.LENGTH_SHORT).show()
                    }
                    newName != group.name && dbHelper.getGroupByName(newName) != null -> {
                        Toast.makeText(this, R.string.error_group_name_exists, Toast.LENGTH_SHORT).show()
                    }
                    newCapacity == null || newCapacity <= 0 -> {
                        Toast.makeText(this, R.string.error_group_capacity_invalid, Toast.LENGTH_SHORT).show()
                    }
                    else -> {
                        val updated = dbHelper.updateGroup(group.id, newName, newCapacity)
                        if (updated) {
                            Toast.makeText(
                                this,
                                getString(R.string.group_updated_success, newName),
                                Toast.LENGTH_SHORT
                            ).show()

                            // Si la capacité a augmenté, vérifier si des étudiants ont été promus
                            val waitingCount = dbHelper.getWaitingCount(newName)
                            val availablePlaces = dbHelper.getAvailablePlaces(newName)
                            if (newCapacity > group.capacity && waitingCount > 0 && availablePlaces > 0) {
                                val promoted = dbHelper.checkAndPromoteWaitingStudents(newName)
                                if (promoted.isNotEmpty()) {
                                    Toast.makeText(
                                        this,
                                        getString(R.string.toast_waiting_promoted, promoted.size),
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            }

                            loadGroups()
                        }
                    }
                }
            }
            .setNegativeButton(R.string.btn_annuler, null)
            .show()
    }

    /**
     * Dialogue pour supprimer un groupe.
     */
    private fun showDeleteGroupDialog(group: Group) {
        val totalStudents = dbHelper.getEnrolledCount(group.name) + dbHelper.getWaitingCount(group.name)
        val message = getString(R.string.dialog_delete_group_message, group.name, totalStudents)

        AlertDialog.Builder(this)
            .setTitle(R.string.dialog_delete_group_title)
            .setMessage(message)
            .setPositiveButton(R.string.btn_supprimer) { _, _ ->
                val deleted = dbHelper.deleteGroup(group.id)
                if (deleted) {
                    Toast.makeText(this, R.string.group_deleted_success, Toast.LENGTH_SHORT).show()
                    loadGroups()
                }
            }
            .setNegativeButton(R.string.btn_annuler, null)
            .show()
    }

    /**
     * Configuration du Navigation Drawer.
     */
    private fun setupNavigationDrawer() {
        val btnMenu = findViewById<ImageButton>(R.id.btnMenu)
        btnMenu.setOnClickListener {
            if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                drawerLayout.closeDrawer(GravityCompat.START)
            } else {
                drawerLayout.openDrawer(GravityCompat.START)
            }
        }

        navigationView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_inscription -> {
                    drawerLayout.closeDrawer(GravityCompat.START)
                    val intent = Intent(this, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    }
                    startActivity(intent)
                    finish()
                    true
                }

                R.id.nav_list -> {
                    drawerLayout.closeDrawer(GravityCompat.START)
                    val intent = Intent(this, list_classroom::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    }
                    startActivity(intent)
                    finish()
                    true
                }

                R.id.nav_settings -> {
                    drawerLayout.closeDrawer(GravityCompat.START)
                    true
                }

                R.id.nav_exit -> {
                    drawerLayout.closeDrawer(GravityCompat.START)
                    showExitDialog()
                    true
                }

                else -> false
            }
        }
    }

    private fun showExitDialog() {
        AlertDialog.Builder(this)
            .setTitle(R.string.dialog_exit_title)
            .setMessage(R.string.dialog_exit_message)
            .setPositiveButton(R.string.btn_quitter) { _, _ ->
                finishAffinity()
            }
            .setNegativeButton(R.string.btn_annuler, null)
            .show()
    }
}
