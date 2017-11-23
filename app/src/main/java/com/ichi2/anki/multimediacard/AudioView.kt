/****************************************************************************************
 * Copyright (c) 2013 Bibek Shrestha <bibekshrestha></bibekshrestha>@gmail.com>                          *
 * Copyright (c) 2013 Zaur Molotnikov <qutorial></qutorial>@gmail.com>                              *
 * Copyright (c) 2013 Nicolas Raoul <nicolas.raoul></nicolas.raoul>@gmail.com>                           *
 * Copyright (c) 2013 Flavio Lerda <flerda></flerda>@gmail.com>                                   *
 * *
 * This program is free software; you can redistribute it and/or modify it under        *
 * the terms of the GNU General Public License as published by the Free Software        *
 * Foundation; either version 3 of the License, or (at your option) any later           *
 * version.                                                                             *
 * *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY      *
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A      *
 * PARTICULAR PURPOSE. See the GNU General Public License for more details.             *
 * *
 * You should have received a copy of the GNU General Public License along with         *
 * this program.  If not, see <http:></http:>//www.gnu.org/licenses/>.                           *
 */

package com.ichi2.anki.multimediacard

import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.content.Context
import android.media.MediaPlayer
import android.media.MediaPlayer.OnCompletionListener
import android.media.MediaRecorder
import android.os.Build

import android.view.View
import android.view.View.OnClickListener
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.Toast
import com.ichi2.anki.R
import com.ichi2.utils.toast

import timber.log.Timber

// Not designed for visual editing
@SuppressLint("ViewConstructor")
class AudioView private constructor(private val mContext: Context, internal var mResPlayImage: Int, internal var mResPauseImage: Int, internal var mResStopImage: Int, audioPath: String) : LinearLayout(mContext) {
    var audioPath: String? = null
        protected set

    private var mPlayPause: PlayPauseButton? = null
    private var mStop: StopButton? = null
    private var mRecord: RecordButton? = null

    private var mRecorder: MediaRecorder? = null
    private var mPlayer: MediaPlayer? = null

    private var mOnRecordingFinishEventListener: OnRecordingFinishEventListener? = null

    private var mStatus = Status.IDLE
    internal var mResRecordImage: Int = 0
    internal var mResRecordStopImage: Int = 0

    internal enum class Status {
        IDLE, // Default initial state
        INITIALIZED, // When datasource has been set
        PLAYING, PAUSED, STOPPED, // The different possible states once playing
        // has started
        RECORDING // The recorder being played status
    }

    init {
        this.audioPath = audioPath

        this.orientation = LinearLayout.HORIZONTAL

        mPlayPause = PlayPauseButton(mContext)
        addView(mPlayPause, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT))

        mStop = StopButton(mContext)
        addView(mStop, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT))
    }

    private constructor(context: Context, resPlay: Int, resPause: Int, resStop: Int, resRecord: Int, resRecordStop: Int,
                        audioPath: String) : this(context, resPlay, resPause, resStop, audioPath) {
        mResRecordImage = resRecord
        mResRecordStopImage = resRecordStop

        this.orientation = LinearLayout.HORIZONTAL

        mRecord = RecordButton(context)
        addView(mRecord, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT))
    }

    fun setRecordButtonVisible(isVisible: Boolean) {
        if (isVisible) {
            mRecord!!.visibility = View.VISIBLE
        } else {
            mRecord!!.visibility = View.INVISIBLE
        }
    }

    fun setOnRecordingFinishEventListener(listener: OnRecordingFinishEventListener) {
        mOnRecordingFinishEventListener = listener
    }

    fun notifyPlay() {
        mPlayPause!!.update()
        mStop!!.update()
        if (mRecord != null) {
            mRecord!!.update()
        }
    }


    fun notifyStop() {
        // Send state change signal to all buttons
        mPlayPause!!.update()
        mStop!!.update()
        if (mRecord != null) {
            mRecord!!.update()
        }
    }


    fun notifyPause() {
        mPlayPause!!.update()
        mStop!!.update()
        if (mRecord != null) {
            mRecord!!.update()
        }
    }


    fun notifyRecord() {
        mPlayPause!!.update()
        mStop!!.update()
        if (mRecord != null) {
            mRecord!!.update()
        }
    }


    fun notifyStopRecord() {
        if (mRecorder != null && mStatus == Status.RECORDING) {
            mRecorder!!.stop()
            mStatus = Status.IDLE
            if (mOnRecordingFinishEventListener != null) {
                mOnRecordingFinishEventListener!!.onRecordingFinish(this@AudioView)
            }
        }
        mPlayPause!!.update()
        mStop!!.update()
        if (mRecord != null) {
            mRecord!!.update()
        }
    }


    fun notifyReleaseRecorder() {
        if (mRecorder != null) {
            mRecorder!!.release()
        }
    }


    protected inner class PlayPauseButton(context: Context) : ImageButton(context) {
        internal var onClickListener: OnClickListener = OnClickListener {
            if (audioPath == null) {
                return@OnClickListener
            }

            when (mStatus) {
                AudioView.Status.IDLE -> try {
                    mPlayer = MediaPlayer()
                    mPlayer!!.setDataSource(audioPath)
                    mPlayer!!.setOnCompletionListener {
                        mStatus = Status.STOPPED
                        mPlayer!!.stop()
                        notifyStop()
                    }
                    mPlayer!!.prepare()
                    mPlayer!!.start()

                    setImageResource(mResPauseImage)
                    mStatus = Status.PLAYING
                    notifyPlay()
                } catch (e: Exception) {
                    Timber.e(e.message)
                    context.toast(context.getString(R.string.multimedia_editor_audio_view_playing_failed))
                    mStatus = Status.IDLE
                }

                AudioView.Status.PAUSED -> {
                    // -> Play, continue playing
                    mStatus = Status.PLAYING
                    setImageResource(mResPauseImage)
                    mPlayer!!.start()
                    notifyPlay()
                }

                AudioView.Status.STOPPED -> {
                    // -> Play, start from beginning
                    mStatus = Status.PLAYING
                    setImageResource(mResPauseImage)
                    try {
                        mPlayer!!.prepare()
                        mPlayer!!.seekTo(0)
                    } catch (e: Exception) {
                        Timber.e(e.message)
                    }

                    mPlayer!!.start()
                    notifyPlay()
                }

                AudioView.Status.PLAYING -> {
                    setImageResource(mResPlayImage)
                    mPlayer!!.pause()
                    mStatus = Status.PAUSED
                    notifyPause()
                }

                AudioView.Status.RECORDING -> {
                }
                else -> {
                }
            }// this button should be disabled
        }


        init {
            setImageResource(mResPlayImage)
            setOnClickListener(onClickListener)
        }


        fun update() {
            when (mStatus) {
                AudioView.Status.IDLE, AudioView.Status.STOPPED -> {
                    setImageResource(mResPlayImage)
                    isEnabled = true
                }

                AudioView.Status.RECORDING -> isEnabled = false

                else -> isEnabled = true
            }
        }
    }


    protected inner class StopButton(context: Context) : ImageButton(context) {
        init {
            setImageResource(mResStopImage)

            setOnClickListener {
                when (mStatus) {
                    AudioView.Status.PAUSED, AudioView.Status.PLAYING -> {
                        mPlayer!!.stop()
                        mStatus = Status.STOPPED
                        notifyStop()
                    }
                    else -> {
                    }
                }
            }
        }

        fun update() = when (mStatus) {
            AudioView.Status.RECORDING -> isEnabled = false

            else -> isEnabled = true
        }
        // It doesn't need to update itself on any other state changes

    }

    protected inner class RecordButton(context: Context) : ImageButton(context) {
        internal var onClickListener: OnClickListener = object : OnClickListener {
            @TargetApi(Build.VERSION_CODES.GINGERBREAD_MR1)
            override fun onClick(v: View) {
                // Since mAudioPath is not compulsory, we check if it exists
                if (audioPath == null) {
                    return
                }

                when (mStatus) {
                    AudioView.Status.IDLE // If not already recorded or not already played
                        , AudioView.Status.STOPPED // if already recorded or played
                    -> {
                        var highSampling = false
                        val currentapiVersion = android.os.Build.VERSION.SDK_INT
                        if (currentapiVersion >= android.os.Build.VERSION_CODES.GINGERBREAD_MR1) {
                            try {
                                // try high quality AAC @ 44.1kHz / 192kbps first
                                mRecorder = initMediaRecorder()
                                mRecorder!!.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                                mRecorder!!.setAudioChannels(2)
                                mRecorder!!.setAudioSamplingRate(44100)
                                mRecorder!!.setAudioEncodingBitRate(192000)
                                mRecorder!!.prepare()
                                mRecorder!!.start()
                                highSampling = true
                            } catch (e: Exception) {
                            }

                        }

                        if (!highSampling) {
                            // fall back on default
                            try {
                                mRecorder = initMediaRecorder()
                                mRecorder!!.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)

                                mRecorder!!.prepare()
                                mRecorder!!.start()

                            } catch (e: Exception) {
                                Timber.e("RecordButton.onClick() :: errorSnackbar recording to " + audioPath + "\n" + e.message)
                                context.toast(context.getText(R.string.multimedia_editor_audio_view_recording_failed))
                                mStatus = Status.STOPPED
                            }

                        }

                        mStatus = Status.RECORDING
                        setImageResource(mResRecordImage)
                        notifyRecord()
                    }

                    AudioView.Status.RECORDING -> {
                        setImageResource(mResRecordStopImage)
                        notifyStopRecord()
                    }
                }// do nothing
            }


            private fun initMediaRecorder(): MediaRecorder {
                val mr = MediaRecorder()
                mr.setAudioSource(MediaRecorder.AudioSource.MIC)
                mr.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
                mStatus = Status.INITIALIZED
                mr.setOutputFile(audioPath) // audioPath
                // could
                // change
                return mr
            }
        }


        init {
            setImageResource(mResRecordStopImage)
            setOnClickListener(onClickListener)
        }


        fun update() = when (mStatus) {
            AudioView.Status.PLAYING, AudioView.Status.PAUSED -> isEnabled = false

            else -> isEnabled = true
        }
    }


    interface OnRecordingFinishEventListener {
        fun onRecordingFinish(v: View)
    }


    companion object {
        fun createRecorderInstance(context: Context, resPlay: Int, resPause: Int, resStop: Int,
                                   resRecord: Int, resRecordStop: Int, audioPath: String): AudioView =
                AudioView(context, resPlay, resPause, resStop, resRecord, resRecordStop, audioPath)
    }
}
