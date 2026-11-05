package tn.tp.atelierandroid

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView

/**
 * Adaptateur pour afficher la liste des étudiants (Classe actuelle ou Liste d'attente).
 */
class StudentAdapter(
    private var students: List<Student>,
    private var isWaitingList: Boolean,
    private val onDeleteClick: (Student) -> Unit
) : RecyclerView.Adapter<StudentAdapter.StudentViewHolder>() {

    class StudentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvPosition: TextView = itemView.findViewById(R.id.tvPosition)
        val tvNomComplet: TextView = itemView.findViewById(R.id.tvNomComplet)
        val tvInfos: TextView = itemView.findViewById(R.id.tvInfos)
        val tvGroupe: TextView = itemView.findViewById(R.id.tvGroupe)
        val btnSupprimer: ImageButton = itemView.findViewById(R.id.btnSupprimer)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StudentViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_student, parent, false)
        return StudentViewHolder(view)
    }

    override fun onBindViewHolder(holder: StudentViewHolder, position: Int) {
        val student = students[position]
        val context = holder.itemView.context

        holder.tvPosition.text = "#${position + 1}"
        if (isWaitingList) {
            holder.tvPosition.setTextColor(ContextCompat.getColor(context, R.color.badge_waiting))
        } else {
            holder.tvPosition.setTextColor(ContextCompat.getColor(context, R.color.teal))
        }

        holder.tvNomComplet.text = student.nomComplet
        holder.tvInfos.text = student.infos
        holder.tvGroupe.text = student.groupe

        holder.btnSupprimer.setOnClickListener {
            onDeleteClick(student)
        }
    }

    override fun getItemCount(): Int = students.size

    fun updateData(newStudents: List<Student>, isWaiting: Boolean) {
        this.students = newStudents
        this.isWaitingList = isWaiting
        notifyDataSetChanged()
    }
}
