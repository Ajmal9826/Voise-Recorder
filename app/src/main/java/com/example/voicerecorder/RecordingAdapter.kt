package com.example.voicerecorder
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import android.view.*
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import java.io.File

class RecordingAdapter(
    private val files: ArrayList<File>,
    private val listener: OnItemClickListener
) : RecyclerView.Adapter<RecordingAdapter.ViewHolder>() {

    private var playingFile: File? = null
    fun setPlayingState(file: File, isPlaying: Boolean) {
        playingFile = if(isPlaying) file else null
        notifyDataSetChanged()
    }

    inner class ViewHolder(v: View): RecyclerView.ViewHolder(v) {
        private var handler = Handler(Looper.getMainLooper())
        private var updateSeekbar: Runnable? = null

        fun bind(file: File) {
            val tvName = itemView.findViewById<TextView>(R.id.tvFileName)
            val seekBar = itemView.findViewById<SeekBar>(R.id.seekBar)
            val tvCurrent = itemView.findViewById<TextView>(R.id.tvCurrentTime)
            val tvTotal = itemView.findViewById<TextView>(R.id.tvTotalTime)
            val playBtn = itemView.findViewById<ImageButton>(R.id.btnPlayPause)
            val menuBtn = itemView.findViewById<ImageButton>(R.id.btnMenu)

            tvName.text = file.name
            playBtn.setImageResource(if(playingFile == file) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play)

            playBtn.setOnClickListener { listener.onPlayPause(file, seekBar, tvCurrent, tvTotal) }
            menuBtn.setOnClickListener { listener.onMenuClick(file, it) }

            seekBar.setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener{
                override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                    if(fromUser) listener.onSeekTo(file, progress)
                }
                override fun onStartTrackingTouch(p0: SeekBar?) {}
                override fun onStopTrackingTouch(p0: SeekBar?) {}
            })

            updateSeekbar = object : Runnable {
                override fun run() {
                    if(playingFile == file && listener.getMediaPlayer()?.isPlaying == true) {
                        val mp = listener.getMediaPlayer()
                        seekBar.max = mp?.duration?: 0
                        seekBar.progress = mp?.currentPosition?: 0
                        tvCurrent.text = formatTime(mp?.currentPosition?: 0)
                        tvTotal.text = formatTime(mp?.duration?: 0)
                        handler.postDelayed(this, 500)
                    }
                }
            }
            if(playingFile == file) handler.post(updateSeekbar!!) else {
                tvCurrent.text = "0:00"; tvTotal.text = formatTime(MediaPlayer.create(itemView.context, android.net.Uri.fromFile(file))?.duration?: 0)
            }
        }
        private fun formatTime(ms: Int): String {
            val seconds = ms / 1000
            return String.format("%d:%02d", seconds / 60, seconds % 60)
        }
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_recording, parent, false))
    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(files[position])
    override fun getItemCount() = files.size

    interface OnItemClickListener {
        fun onPlayPause(file: File, seekBar: SeekBar, tvCurrent: TextView, tvTotal: TextView)
        fun onSeekTo(file: File, position: Int)
        fun getMediaPlayer(): MediaPlayer?
        fun onMenuClick(file: File, view: View)
        fun onRename(file: File)
        fun onDelete(file: File)
        fun onShare(file: File)
    }
}
