package tn.tp.atelierandroid

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView

/**
 * Adaptateur pour la liste des groupes et capacités dans la page des Paramètres.
 */
class GroupAdapter(
    private var groups: List<Group>,
    private val dbHelper: DatabaseHelper,
    private val onEditClick: (Group) -> Unit,
    private val onDeleteClick: (Group) -> Unit
) : RecyclerView.Adapter<GroupAdapter.GroupViewHolder>() {

    class GroupViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvGroupName: TextView = itemView.findViewById(R.id.tvGroupName)
        val tvGroupStats: TextView = itemView.findViewById(R.id.tvGroupStats)
        val tvStatusBadge: TextView = itemView.findViewById(R.id.tvStatusBadge)
        val tvWaitingBadge: TextView = itemView.findViewById(R.id.tvWaitingBadge)
        val btnEditGroup: ImageButton = itemView.findViewById(R.id.btnEditGroup)
        val btnDeleteGroup: ImageButton = itemView.findViewById(R.id.btnDeleteGroup)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GroupViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_group, parent, false)
        return GroupViewHolder(view)
    }

    override fun onBindViewHolder(holder: GroupViewHolder, position: Int) {
        val group = groups[position]
        val context = holder.itemView.context

        holder.tvGroupName.text = group.name

        val enrolledCount = dbHelper.getEnrolledCount(group.name)
        val waitingCount = dbHelper.getWaitingCount(group.name)
        val available = group.capacity - enrolledCount

        // Affichage des statistiques (ex: "1 / 1 inscrits")
        holder.tvGroupStats.text = context.getString(R.string.badge_places_stats, enrolledCount, group.capacity)

        // Statut Disponible ou Complet
        if (available <= 0) {
            holder.tvStatusBadge.text = context.getString(R.string.badge_complet)
            holder.tvStatusBadge.setTextColor(ContextCompat.getColor(context, R.color.danger_red))
        } else {
            holder.tvStatusBadge.text = context.getString(R.string.badge_places_disponibles, available)
            holder.tvStatusBadge.setTextColor(ContextCompat.getColor(context, R.color.teal))
        }

        // Badge d'attente
        if (waitingCount > 0) {
            holder.tvWaitingBadge.visibility = View.VISIBLE
            holder.tvWaitingBadge.text = context.getString(R.string.badge_waiting_stats, waitingCount)
        } else {
            holder.tvWaitingBadge.visibility = View.GONE
        }

        // Actions
        holder.btnEditGroup.setOnClickListener {
            onEditClick(group)
        }

        holder.btnDeleteGroup.setOnClickListener {
            onDeleteClick(group)
        }
    }

    override fun getItemCount(): Int = groups.size

    fun updateData(newGroups: List<Group>) {
        this.groups = newGroups
        notifyDataSetChanged()
    }
}
