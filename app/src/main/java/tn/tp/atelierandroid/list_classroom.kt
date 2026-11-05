package tn.tp.atelierandroid

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.AdapterView
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.navigation.NavigationView
import com.google.android.material.tabs.TabLayout

/**
 * Activité affichant la liste des étudiants avec onglets (Classe actuelle vs Liste d'attente),
 * recherche dynamique par nom/prénom, filtre par groupe et suppression.
 */
class list_classroom : AppCompatActivity() {

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var tabLayout: TabLayout
    private lateinit var etSearch: EditText
    private lateinit var btnClearSearch: ImageButton
    private lateinit var spFiltreGroupe: Spinner
    private lateinit var rvStudents: RecyclerView
    private lateinit var tvEmpty: TextView
    private lateinit var studentAdapter: StudentAdapter

    private var isWaitingSelected: Boolean = false
    private var currentSearchQuery: String = ""
    private var currentGroupFilter: String = "Tous"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_list_classroom)

        dbHelper = DatabaseHelper(this)

        initViews()
        setupNavigationDrawer()
        setupRecyclerView()
        setupTabs()
        setupSearchAndFilter()
    }

    override fun onResume() {
        super.onResume()
        navigationView.setCheckedItem(R.id.nav_list)
        loadData()
    }

    private fun initViews() {
        drawerLayout = findViewById(R.id.drawerLayout)
        navigationView = findViewById(R.id.navigationView)
        tabLayout = findViewById(R.id.tabLayout)
        etSearch = findViewById(R.id.etSearch)
        btnClearSearch = findViewById(R.id.btnClearSearch)
        spFiltreGroupe = findViewById(R.id.spFiltreGroupe)
        rvStudents = findViewById(R.id.rvStudents)
        tvEmpty = findViewById(R.id.tvEmpty)
    }

    private fun setupRecyclerView() {
        rvStudents.layoutManager = LinearLayoutManager(this)
        studentAdapter = StudentAdapter(
            students = emptyList(),
            isWaitingList = false,
            onDeleteClick = { student ->
                showDeleteConfirmationDialog(student)
            }
        )
        rvStudents.adapter = studentAdapter
    }

    private fun setupTabs() {
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                isWaitingSelected = (tab?.position == 1)
                loadData()
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun setupSearchAndFilter() {
        // Écouteur pour la saisie dans le champ de recherche
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                currentSearchQuery = s?.toString()?.trim() ?: ""
                btnClearSearch.visibility = if (currentSearchQuery.isNotEmpty()) View.VISIBLE else View.GONE
                loadData()
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // Bouton pour effacer la recherche
        btnClearSearch.setOnClickListener {
            etSearch.text.clear()
        }

        // Filtre par groupe
        spFiltreGroupe.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selected = parent?.getItemAtPosition(position).toString()
                currentGroupFilter = if (position == 0) "Tous" else selected
                loadData()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    /**
     * Charge les données depuis SQLite en appliquant l'onglet actif,
     * le filtre par groupe et le filtre par recherche textuelle.
     */
    private fun loadData() {
        val students = dbHelper.getStudents(
            isWaiting = isWaitingSelected,
            groupe = currentGroupFilter,
            searchQuery = currentSearchQuery
        )

        studentAdapter.updateData(students, isWaitingSelected)

        // Gestion de l'affichage vide
        if (students.isEmpty()) {
            rvStudents.visibility = View.GONE
            tvEmpty.visibility = View.VISIBLE
            tvEmpty.text = if (isWaitingSelected) {
                getString(R.string.empty_list_attente)
            } else {
                getString(R.string.empty_list_actuelle)
            }
        } else {
            rvStudents.visibility = View.VISIBLE
            tvEmpty.visibility = View.GONE
        }

        updateTabBadges()
    }

    /**
     * Met à jour les libellés des onglets avec le nombre d'étudiants dans chaque catégorie.
     */
    private fun updateTabBadges() {
        val enrolledCount = dbHelper.getStudents(isWaiting = false, groupe = currentGroupFilter).size
        val waitingCount = dbHelper.getStudents(isWaiting = true, groupe = currentGroupFilter).size

        tabLayout.getTabAt(0)?.text = "${getString(R.string.tab_actuelle)} ($enrolledCount)"
        tabLayout.getTabAt(1)?.text = "${getString(R.string.tab_attente)} ($waitingCount)"
    }

    /**
     * Dialogue de confirmation avant suppression d'un étudiant.
     */
    private fun showDeleteConfirmationDialog(student: Student) {
        val message = "Voulez-vous vraiment supprimer ${student.nomComplet} (${student.groupe}) ?"

        AlertDialog.Builder(this)
            .setTitle(R.string.dialog_delete_title)
            .setMessage(message)
            .setPositiveButton(R.string.btn_supprimer) { _, _ ->
                val wasAdmitted = !student.isWaiting
                val deleted = dbHelper.deleteStudent(student.id)

                if (deleted) {
                    if (wasAdmitted) {
                        // Si l'étudiant était admis, on vérifie s'il y a un étudiant en attente à promouvoir
                        val promotedStudent = dbHelper.promoteNextWaitingStudent(student.groupe)
                        if (promotedStudent != null) {
                            Toast.makeText(
                                this,
                                "Étudiant supprimé. ${promotedStudent.nomComplet} a été promu(e) dans ${student.groupe} !",
                                Toast.LENGTH_LONG
                            ).show()
                        } else {
                            Toast.makeText(this, R.string.dialog_delete_title, Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(this, "Étudiant retiré de la liste d'attente.", Toast.LENGTH_SHORT).show()
                    }
                    loadData()
                } else {
                    Toast.makeText(this, "Erreur lors de la suppression.", Toast.LENGTH_SHORT).show()
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
