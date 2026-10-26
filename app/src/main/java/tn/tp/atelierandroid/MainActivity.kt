package tn.tp.atelierandroid

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    private val fixedmap = mutableMapOf(
        "G1" to 1,
        "G2" to 8,
        "G3" to 8
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val tvPlacesG1 = findViewById<TextView>(R.id.tvPlacesG1)
        val tvPlacesG2 = findViewById<TextView>(R.id.tvPlacesG2)
        val tvPlacesG3 = findViewById<TextView>(R.id.tvPlacesG3)

        fun updateCounters() {
            tvPlacesG1.text = "G1 : ${fixedmap.getValue("G1")} places"
            tvPlacesG2.text = "G2 : ${fixedmap.getValue("G2")} places"
            tvPlacesG3.text = "G3 : ${fixedmap.getValue("G3")} places"
        }


        // ----- Récupération des composants -----
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

        // ----- Remplissage du Spinner -----
        // (inutile si vous utilisez android:entries="@array/groupes" dans le XML)
        spGroupe.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            listOf("Choisir un groupe", "G1", "G2", "G3")
        )

        // ----- Clic sur « Afficher la fiche » -----
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

                // Check whether the selected group is full
                (fixedmap[groupe] ?: 0) <= 0 ->
                    Toast.makeText(
                        this,
                        "Désolé, il n'y a plus de places dans $groupe",
                        Toast.LENGTH_LONG
                    ).show()

                else -> {
                    updateCounters()
                    fixedmap[groupe] = fixedmap.getValue(groupe) - 1
                    tvResultat.text = "FICHE D'INSCRIPTION\n" +
                            "Nom : $nom\n" +
                            "Prénom : $prenom\n" +
                            "Âge : $age ans\n" +
                            "Groupe : $groupe\n" +
                            "Support : $support"
                }
            }
        }

        // ----- Clic sur « Effacer » -----
        btnEffacer.setOnClickListener {
            etNom.text.clear()
            etPrenom.text.clear()
            etAge.text.clear()
            spGroupe.setSelection(0)
            rgSupport.check(R.id.rbEmulateur)
            cbConfirmation.isChecked = false
            tvResultat.text = "Votre fiche apparaîtra ici"
        }
    }
}