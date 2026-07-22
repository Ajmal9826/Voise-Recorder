    package com.example.voicerecorder

    import android.view.LayoutInflater
    import android.view.View
    import android.view.ViewGroup
    import android.widget.ImageButton
    import android.widget.TextView
    import androidx.recyclerview.widget.RecyclerView
    import java.io.File

    class RecordingAdapter(
        private var files: List<File>,
        private val listener: OnItemClickListener
    ) : RecyclerView.Adapter<RecordingAdapter.ViewHolder>() {

        interface OnItemClickListener {
            fun onPlay(file: File)
            fun onShare(file: File)
            fun onDelete(file: File)
            fun onRename(file: File)
        }

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val tvName: TextView = itemView.findViewById(R.id.tvFileName)
            val tvDetails: TextView = itemView.findViewById(R.id.tvFileDetails)
            val btnPlay: ImageButton = itemView.findViewById(R.id.btnPlay)
            val btnShare: ImageButton = itemView.findViewById(R.id.btnShare)
            val btnRename: ImageButton = itemView.findViewById(R.id.btnRename)
            val btnDelete: ImageButton = itemView.findViewById(R.id.btnDelete)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_recording, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val file = files[position]
            holder.tvName.text = file.nameWithoutExtension
            holder.tvDetails.text = "${file.extension} • ${file.length() / 1024} KB"

            holder.btnPlay.setOnClickListener { listener.onPlay(file) }
            holder.btnShare.setOnClickListener { listener.onShare(file) }
            holder.btnRename.setOnClickListener { listener.onRename(file) }
            holder.btnDelete.setOnClickListener { listener.onDelete(file) }
        }

        override fun getItemCount() = files.size
    }
