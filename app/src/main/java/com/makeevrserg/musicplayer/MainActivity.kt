package com.makeevrserg.musicplayer

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.provider.MediaStore
import android.webkit.MimeTypeMap
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_debug.*

class MainActivity : AppCompatActivity() {
    private val TAG: String = "MainActivity"
    private val MUSIC_RESULT1: Int = 10
    private val MUSIC_RESULT2: Int = 11

    private var mContext: Context? = null
    private var crossfade = 2

    private var mediaPlayer1: MediaPlayer? = null
    private var mediaPlayer2: MediaPlayer? = null
    private var currentMediaPlayer: MediaPlayer? = null

    private var toast: Toast? = null

    //Должен быть глобальной переменной чтобы сбросить всё, если сменится основная музыка
    var handler: Handler? = null

    fun CheckToast() {
        if (toast == null)
            return
        toast!!.cancel()
        toast = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_debug)

        mContext = applicationContext

        buttonMusic1.setOnClickListener { SetMusicIntent(MUSIC_RESULT1) }
        buttonMusic2.setOnClickListener { SetMusicIntent(MUSIC_RESULT2) }

        seekBarCrossfade.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                //Нельзя менять во время проигрывания
                if (currentMediaPlayer != null && currentMediaPlayer!!.isPlaying) {
                    CheckToast()
                    toast = Toast.makeText(
                        mContext,
                        "Нельзя менять во время проигрывания",
                        Toast.LENGTH_SHORT
                    )
                    toast!!.show()
                    //Ставим старое значение
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
                CheckToast()
                toast =
                    Toast.makeText(this, "Не выбран один из музыкальных файлов", Toast.LENGTH_SHORT)
                toast!!.show()
                return@setOnClickListener
            }

            if (currentMediaPlayer == null) {
                currentMediaPlayer = mediaPlayer1
                initDelay()
            }
            //Нельзя начать играть если длина файлов меньше кроссфейда
            if (mediaPlayer1!!.duration / 1000 < crossfade || mediaPlayer2!!.duration / 1000 < crossfade) {
                CheckToast()
                toast = Toast.makeText(
                    this,
                    "Один из аудиофайлов слишком короткий для данного кроссфейда",
                    Toast.LENGTH_SHORT
                )
                toast!!.show()
                if (mediaPlayer2!!.isPlaying)
                    mediaPlayer2!!.pause()
                if (mediaPlayer1!!.isPlaying)
                    mediaPlayer1!!.pause()
                imageButtonPause.setImageDrawable(getDrawable(R.drawable.ic_play))
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
    }

    //Задаем интент который покажет список наших mp3 файлов
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
        //По requestCod'у видим наши активити
        when (requestCode) {
            MUSIC_RESULT1 ->
                if (resultCode == Activity.RESULT_OK) {//Проверям что активити возвращена с результатом
                    if (mediaPlayer1 != null) {
                        //Удаляем у хендлера все его postDelayed'ы
                        if (handler != null)
                            handler!!.removeCallbacksAndMessages(null)
                        //Ресетим активный плеер
                        if (currentMediaPlayer != null && currentMediaPlayer == mediaPlayer1) {
                            currentMediaPlayer!!.reset()
                            currentMediaPlayer = null
                            imageButtonPause.setImageDrawable(getDrawable(R.drawable.ic_play))
                        }
                        mediaPlayer1!!.reset()
                        mediaPlayer2!!.pause()
                    }
                    mediaPlayer1 = MediaPlayer.create(this, data?.data!!)
                }

            MUSIC_RESULT2 ->
                if (resultCode == Activity.RESULT_OK) {
                    if (mediaPlayer2 != null)
                        mediaPlayer2!!.reset()
                    mediaPlayer2 = MediaPlayer.create(this, data?.data!!)
                }

        }
    }

    private fun initDelay() {
        handler = Handler()
        handler!!.postDelayed(object : Runnable {
            override fun run() {

                val toEnd =
                    (currentMediaPlayer!!.duration - currentMediaPlayer!!.currentPosition) / 1000//Переводим время до конца в секунды
                //Если мы поставили на паузу, а потом сменили длительность кроссфейда - надо проверить, активировать ли второе аудио или поставить на паузу
                if (mediaPlayer2!!.isPlaying && toEnd > crossfade)
                    mediaPlayer2!!.pause()
                //Если попали в зону кроссфейда и основная запись не на паузе
                if (toEnd <= crossfade && currentMediaPlayer!!.isPlaying) {
                    //Включаем вторую запись
                    if (!mediaPlayer2!!.isPlaying) {
                        mediaPlayer2!!.start()
                        //Если мы сменили кроссфейд после паузу - должны перемотать вторую запись на новый момент
                        mediaPlayer2!!.seekTo((crossfade - toEnd) * 1000)
                    }
                    //Ставим громкость звука с delay'м в 100мс
                    val mSound: Float = (1.0f - (toEnd.toFloat() / (crossfade)))
                    mediaPlayer2!!.setVolume(mSound, mSound)
                }
                //Когда заканчивается первая запись - меняем указатели друг на друга
                //Теперь основной плеер у нас - это второй, а первый становится вторым
                if (toEnd == 0) {
                    currentMediaPlayer = mediaPlayer2
                    mediaPlayer2 = mediaPlayer1
                    mediaPlayer1 = currentMediaPlayer
                    initDelay()
                }
                //Если основнеая запись не окончена - запускаем эту же функцию снова
                if (toEnd >= 1)
                    handler!!.postDelayed(this, 200)

            }

        }, 0)
    }
}