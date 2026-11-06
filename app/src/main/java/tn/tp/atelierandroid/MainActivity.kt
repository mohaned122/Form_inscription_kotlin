package tn.tp.atelierandroid

import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView

class MainActivity : AppCompatActivity() {

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var llGroupCounters: LinearLayout
    private lateinit var spGroupe: Spinner

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        LocaleHelper.applySavedLocale(this)
        setContentView(R.layout.activity_main)

        dbHelper = DatabaseHelper(this)

        setupNavigationDrawer()

        llGroupCounters = findViewById(R.id.llGroupCounters)
        val etNom = findViewById<EditText>(R.id.etNom)
        val etPrenom = findViewById<EditText>(R.id.etPrenom)
        val etAge = findViewById<EditText>(R.id.etAge)
        spGroupe = findViewById(R.id.spGroupe)
        val rgSupport = findViewById<RadioGroup>(R.id.rgSupport)
        val rbEmulateur = findViewById<RadioButton>(R.id.rbEmulateur)
        val cbConfirmation = findViewById<CheckBox>(R.id.cbConfirmation)
        val btnAfficher = findViewById<Button>(R.id.btnAfficher)
        val btnEffacer = findViewById<Button>(R.id.btnEffacer)
        val tvResultat = findViewById<TextView>(R.id.tvResultat)

        // Chargement initial des groupes et des compteurs
        loadGroupsAndCounters()

        // Clic sur « Enregistrer l'inscription »
        btnAfficher.setOnClickListener {
            val nom = etNom.text.toString().trim()
            val prenom = etPrenom.text.toString().trim()
            val age: Int? = etAge.text.toString().toIntOrNull()
            val groupe = if (spGroupe.selectedItemPosition > 0) spGroupe.selectedItem.toString() else ""
            val support = if (rbEmulateur.isChecked) getString(R.string.emulateur) else getString(R.string.telephone)
            val confirme = cbConfirmation.isChecked

            when {
                nom.isEmpty() ->
                    Toast.makeText(this, R.string.error_nom_required, Toast.LENGTH_SHORT).show()

                prenom.isEmpty() ->
                    Toast.makeText(this, R.string.error_prenom_required, Toast.LENGTH_SHORT).show()

                (age == null || age !in 1..120) ->
                    Toast.makeText(this, R.string.error_age_invalid, Toast.LENGTH_SHORT).show()

                spGroupe.selectedItemPosition <= 0 || groupe.isEmpty() ->
                    Toast.makeText(this, R.string.error_group_required, Toast.LENGTH_SHORT).show()

                !confirme ->
                    Toast.makeText(this, R.string.error_confirm_required, Toast.LENGTH_SHORT).show()

                else -> {
                    val available = dbHelper.getAvailablePlaces(groupe)
                    val isWaiting = available <= 0

                    val student = Student(
                        nom = nom,
                        prenom = prenom,
                        age = age,
                        groupe = groupe,
                        support = support,
                        isWaiting = isWaiting
                    )

                    val insertId = dbHelper.insertStudent(student)
                    if (insertId != -1L) {
                        loadGroupsAndCounters()

                        if (isWaiting) {
                            val waitingPos = dbHelper.getWaitingCount(groupe)
                            Toast.makeText(
                                this,
                                getString(R.string.toast_waiting_list, groupe, waitingPos),
                                Toast.LENGTH_LONG
                            ).show()

                            tvResultat.text = "${getString(R.string.recap_title_waiting)}\n" +
                                    "${getString(R.string.recap_nom, nom)}\n" +
                                    "${getString(R.string.recap_prenom, prenom)}\n" +
                                    "${getString(R.string.recap_age, age)}\n" +
                                    "${getString(R.string.recap_groupe, groupe)}\n" +
                                    "${getString(R.string.recap_support, support)}\n" +
                                    getString(R.string.recap_status_waiting, waitingPos)
                        } else {
                            Toast.makeText(
                                this,
                                getString(R.string.toast_admitted, groupe),
                                Toast.LENGTH_SHORT
                            ).show()

                            tvResultat.text = "${getString(R.string.recap_title_admis)}\n" +
                                    "${getString(R.string.recap_nom, nom)}\n" +
                                    "${getString(R.string.recap_prenom, prenom)}\n" +
                                    "${getString(R.string.recap_age, age)}\n" +
                                    "${getString(R.string.recap_groupe, groupe)}\n" +
                                    "${getString(R.string.recap_support, support)}\n" +
                                    getString(R.string.recap_status_admitted)
                        }
                    }
                }
            }
        }

        // Clic sur « Effacer »
        btnEffacer.setOnClickListener {
            etNom.text.clear()
            etPrenom.text.clear()
            etAge.text.clear()
            if (spGroupe.adapter != null && spGroupe.adapter.count > 0) {
                spGroupe.setSelection(0)
            }
            rgSupport.check(R.id.rbEmulateur)
            cbConfirmation.isChecked = false
            tvResultat.text = getString(R.string.resultat_initial)
        }
    }

    override fun onResume() {
        super.onResume()
        loadGroupsAndCounters()
        navigationView.setCheckedItem(R.id.nav_inscription)
    }

    /**
     * Charge dynamiquement les groupes dans le Spinner et met à jour les badges de places.
     */
    private fun loadGroupsAndCounters() {
        val groups = dbHelper.getAllGroups()

        // Mise à jour du Spinner de groupes
        val currentSelected = spGroupe.selectedItem?.toString()
        val spinnerItems = listOf(getString(R.string.choose_group)) + groups.map { it.name }
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, spinnerItems)
        spGroupe.adapter = adapter

        if (!currentSelected.isNullOrEmpty()) {
            val prevIndex = spinnerItems.indexOf(currentSelected)
            if (prevIndex >= 0) {
                spGroupe.setSelection(prevIndex)
            }
        }

        // Mise à jour des badges dynamiques de comptage
        llGroupCounters.removeAllViews()
        val density = resources.displayMetrics.density

        for (group in groups) {
            val available = dbHelper.getAvailablePlaces(group.name)
            val waiting = dbHelper.getWaitingCount(group.name)

            val tvCard = TextView(this).apply {
                val params = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    marginEnd = (8 * density).toInt()
                }
                layoutParams = params
                background = ContextCompat.getDrawable(this@MainActivity, R.drawable.bg_result)
                gravity = Gravity.CENTER
                setPadding((12 * density).toInt(), (8 * density).toInt(), (12 * density).toInt(), (8 * density).toInt())
                setTextColor(ContextCompat.getColor(this@MainActivity, R.color.label))
                textSize = 12f

                val statusText = if (available > 0) {
                    "${group.name} : $available places"
                } else {
                    "${group.name} : ${getString(R.string.badge_complet)}"
                }

                val waitingText = if (waiting > 0) {
                    "\n(${getString(R.string.badge_waiting_stats, waiting)})"
                } else ""

                text = "$statusText$waitingText"
            }
            llGroupCounters.addView(tvCard)
        }
    }

    /**
     * Configure le menu latéral (Navigation Drawer) et ses actions.
     */
    private fun setupNavigationDrawer() {
        drawerLayout = findViewById(R.id.drawerLayout)
        navigationView = findViewById(R.id.navigationView)
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
                    true
                }

                R.id.nav_list -> {
                    drawerLayout.closeDrawer(GravityCompat.START)
                    val intent = Intent(this, list_classroom::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    }
                    startActivity(intent)
                    true
                }

                R.id.nav_settings -> {
                    drawerLayout.closeDrawer(GravityCompat.START)
                    val intent = Intent(this, SettingsActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    }
                    startActivity(intent)
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
