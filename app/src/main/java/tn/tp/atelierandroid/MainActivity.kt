package tn.tp.atelierandroid

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageButton
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView

class MainActivity : AppCompatActivity() {

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var tvPlacesG1: TextView
    private lateinit var tvPlacesG2: TextView
    private lateinit var tvPlacesG3: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        dbHelper = DatabaseHelper(this)

        // Configuration du Navigation Drawer
        setupNavigationDrawer()

        // Récupération des composants de comptage
        tvPlacesG1 = findViewById(R.id.tvPlacesG1)
        tvPlacesG2 = findViewById(R.id.tvPlacesG2)
        tvPlacesG3 = findViewById(R.id.tvPlacesG3)

        // Récupération des composants du formulaire
        val etNom = findViewById<EditText>(R.id.etNom)
        val etPrenom = findViewById<EditText>(R.id.etPrenom)
        val etAge = findViewById<EditText>(R.id.etAge)
        val spGroupe = findViewById<Spinner>(R.id.spGroupe)
        val rgSupport = findViewById<RadioGroup>(R.id.rgSupport)
        val rbEmulateur = findViewById<RadioButton>(R.id.rbEmulateur)
        val cbConfirmation = findViewById<CheckBox>(R.id.cbConfirmation)
        val btnAfficher = findViewById<Button>(R.id.btnAfficher)
        val btnEffacer = findViewById<Button>(R.id.btnEffacer)
        val tvResultat = findViewById<TextView>(R.id.tvResultat)

        // Remplissage du Spinner
        spGroupe.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            listOf("Choisir un groupe", "G1", "G2", "G3")
        )

        // Mise à jour initiale des compteurs de places
        updateCounters()

        // Clic sur « Enregistrer l'inscription »
        btnAfficher.setOnClickListener {
            val nom = etNom.text.toString().trim()
            val prenom = etPrenom.text.toString().trim()
            val age: Int? = etAge.text.toString().toIntOrNull()
            val groupe = spGroupe.selectedItem.toString()
            val support = if (rbEmulateur.isChecked) "Émulateur" else "Téléphone réel"
            val confirme = cbConfirmation.isChecked

            when {
                nom.isEmpty() ->
                    Toast.makeText(this, "Le nom est obligatoire", Toast.LENGTH_SHORT).show()

                prenom.isEmpty() ->
                    Toast.makeText(this, "Le prénom est obligatoire", Toast.LENGTH_SHORT).show()

                age == null || age !in 1..120 ->
                    Toast.makeText(this, "Âge invalide (entre 1 et 120)", Toast.LENGTH_SHORT).show()

                spGroupe.selectedItemPosition == 0 ->
                    Toast.makeText(this, "Veuillez choisir un groupe", Toast.LENGTH_SHORT).show()

                !confirme ->
                    Toast.makeText(this, "Veuillez confirmer les informations", Toast.LENGTH_SHORT).show()

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
                        updateCounters()

                        if (isWaiting) {
                            val waitingPos = dbHelper.getWaitingCount(groupe)
                            Toast.makeText(
                                this,
                                "Groupe $groupe complet ! Inscrit sur liste d'attente (#$waitingPos)",
                                Toast.LENGTH_LONG
                            ).show()

                            tvResultat.text = "FICHE D'INSCRIPTION (LISTE D'ATTENTE)\n" +
                                    "Nom : $nom\n" +
                                    "Prénom : $prenom\n" +
                                    "Âge : $age ans\n" +
                                    "Groupe : $groupe\n" +
                                    "Support : $support\n" +
                                    "Statut : Liste d'attente (Position #$waitingPos)"
                        } else {
                            Toast.makeText(
                                this,
                                "Étudiant inscrit avec succès dans $groupe !",
                                Toast.LENGTH_SHORT
                            ).show()

                            tvResultat.text = "FICHE D'INSCRIPTION (ADMIS)\n" +
                                    "Nom : $nom\n" +
                                    "Prénom : $prenom\n" +
                                    "Âge : $age ans\n" +
                                    "Groupe : $groupe\n" +
                                    "Support : $support\n" +
                                    "Statut : Admis dans la classe actuelle"
                        }
                    } else {
                        Toast.makeText(
                            this,
                            "Erreur lors de l'enregistrement dans la base de données",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }

        // Clic sur « Effacer »
        btnEffacer.setOnClickListener {
            etNom.text.clear()
            etPrenom.text.clear()
            etAge.text.clear()
            spGroupe.setSelection(0)
            rgSupport.check(R.id.rbEmulateur)
            cbConfirmation.isChecked = false
            tvResultat.text = getString(R.string.resultat_initial)
        }
    }

    override fun onResume() {
        super.onResume()
        updateCounters()
        navigationView.setCheckedItem(R.id.nav_inscription)
    }

    /**
     * Met à jour dynamiquement les compteurs de places disponibles pour chaque classe.
     */
    private fun updateCounters() {
        val g1Available = dbHelper.getAvailablePlaces("G1")
        val g2Available = dbHelper.getAvailablePlaces("G2")
        val g3Available = dbHelper.getAvailablePlaces("G3")

        val g1Waiting = dbHelper.getWaitingCount("G1")
        val g2Waiting = dbHelper.getWaitingCount("G2")
        val g3Waiting = dbHelper.getWaitingCount("G3")

        tvPlacesG1.text = "G1 : $g1Available places" + if (g1Waiting > 0) "\n($g1Waiting en attente)" else ""
        tvPlacesG2.text = "G2 : $g2Available places" + if (g2Waiting > 0) "\n($g2Waiting en attente)" else ""
        tvPlacesG3.text = "G3 : $g3Available places" + if (g3Waiting > 0) "\n($g3Waiting en attente)" else ""
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
                    val intent = Intent(this, list_classroom::class.java)
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
