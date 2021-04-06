package com.makeevrserg.musicplayer

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.provider.MediaStore
import android.util.Log
import android.webkit.MimeTypeMap
import android.widget.SeekBar
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_debug.*
import java.lang.Exception
import kotlin.math.log

class MainActivity : AppCompatActivity() {
    private var mediaPlayer1: MediaPlayer? = null
    private var mediaPlayer2: MediaPlayer? = null
    private var currentMediaPlayer: MediaPlayer? = null

    private final val TAG: String = "MainActivity"
    private final val MUSIC_RESULT1: Int = 10
    private final val MUSIC_RESULT2: Int = 11
    private final var mContext: Context? = null
    private var crossfade = 2


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mContext = applicationContext
        setContentView(R.layout.activity_debug)
        buttonMusic1.setOnClickListener {
            SetMusicIntent(MUSIC_RESULT1)
        }
        buttonMusic2.setOnClickListener {
            SetMusicIntent(MUSIC_RESULT2)
        }
        seekBarCrossfade.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                //Нельзя менять во время проигрывания
                if (currentMediaPlayer != null && currentMediaPlayer!!.isPlaying) {
                    Toast.makeText(
                        mContext,
                        "Нельзя менять во время проигрывания",
                        Toast.LENGTH_SHORT
                    )
                        .show()
                    seekBar!!.progress = crossfade
                } else
                    crossfade = progress

            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                return
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                return
            }

        })

        imageButtonPause.setOnClickListener {
            if (mediaPlayer1 == null || mediaPlayer2 == null) {
                Toast.makeText(this, "Не выбран один из музыкальных файлов", Toast.LENGTH_SHORT)
                    .show()
                return@setOnClickListener
            }
            if (currentMediaPlayer == null) {
                currentMediaPlayer = mediaPlayer1
                initializeSeekBar()
            }

            if (currentMediaPlayer!!.isPlaying) {
                imageButtonPause.setImageDrawable(getDrawable(R.drawable.ic_play))
                currentMediaPlayer!!.pause()
                if (mediaPlayer2!!.isPlaying)
                    mediaPlayer2!!.pause()
            } else {
                imageButtonPause.setImageDrawable(getDrawable(R.drawable.ic_pause))
                currentMediaPlayer!!.start()
            }
        }


        //Debug only
        seekBarProgress.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) currentMediaPlayer?.seekTo(progress)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                return
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                return
            }

        })


    }

    //Запускаем интент который покажет список наших mp3 файлов
    //Запускаем активити которая возвратит результат
    fun SetMusicIntent(RES: Int) {
        val fileBrowserIntent: Intent =
            Intent(Intent.ACTION_GET_CONTENT, MediaStore.Audio.Media.EXTERNAL_CONTENT_URI)
        val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension("mp3")
        fileBrowserIntent.type = mimeType
        startActivityForResult(fileBrowserIntent, RES)
    }

    @Override
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            MUSIC_RESULT1 ->
                if (resultCode == Activity.RESULT_OK) {
                    if (mediaPlayer1 != null) {
                        if (currentMediaPlayer != null && currentMediaPlayer == mediaPlayer1) {
                            currentMediaPlayer!!.reset()
                            currentMediaPlayer = null
                            imageButtonPause.setImageDrawable(getDrawable(R.drawable.ic_play))
                        }
                        mediaPlayer1!!.reset()
                    }
                    mediaPlayer1 = MediaPlayer.create(this, data?.data!!)
                }
            MUSIC_RESULT2 ->
                if (resultCode == Activity.RESULT_OK) {
                    if (mediaPlayer2 != null)
                        mediaPlayer2!!.reset()
                    mediaPlayer2 = MediaPlayer.create(this, data?.data!!)
                    Log.d(TAG, "onActivityResult: Created Media2")
                }

        }
    }

    //Debug only
    private fun toMim(seconds: Int): String {
        return (seconds / 60).toString() + ":" + (seconds % 60).toString()
    }

    private fun initializeSeekBar() {
        //Debug only
        seekBarProgress.max = currentMediaPlayer!!.duration
        //Debug only
        textViewMaxProgress.setText(toMim(currentMediaPlayer!!.duration / 1000))

        val handler = Handler()
        handler.postDelayed(object : Runnable {
            override fun run() {
                try {
                    //Debug only
                    seekBarProgress.progress = currentMediaPlayer!!.currentPosition
                    val toEnd =
                        (currentMediaPlayer!!.duration - currentMediaPlayer!!.currentPosition) / 1000
                    //Делаем кроссфейд
                    if (mediaPlayer2!!.isPlaying && toEnd>crossfade)
                        mediaPlayer2!!.pause()
                    if (toEnd <= crossfade && currentMediaPlayer!!.isPlaying) {
                        if (!mediaPlayer2!!.isPlaying) {
                            mediaPlayer2!!.start()
                            Log.d(TAG, "seekTo: " + (crossfade - toEnd) * 1000)
                            Log.d(TAG, "currPos: " + mediaPlayer2!!.currentPosition)
                            if (mediaPlayer2 == null)
                                Log.d(TAG, "null: ")
                            mediaPlayer2!!.seekTo((crossfade - toEnd) * 1000)
                        }
                        Log.d(TAG, "media2Voilume: " + 1.0f / toEnd)
                        mediaPlayer2!!.setVolume(1.0f / toEnd, 1.0f / toEnd)
                    }
                    if (toEnd == 0) {
                        currentMediaPlayer = mediaPlayer2
                        mediaPlayer2 = mediaPlayer1
                        mediaPlayer1 = currentMediaPlayer
                        initializeSeekBar()
                    }
                    textViewCurrentProgress.setText(toMim(currentMediaPlayer!!.currentPosition / 1000))
                    if (toEnd >= 1)
                        handler.postDelayed(this, 1000)
                } catch (e: Exception) {
                    seekBarProgress.progress = 0
                }
            }

        }, 0)
    }
}