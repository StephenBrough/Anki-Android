package com.ichi2.anki.flashcardviewer

/****************************************************************************************
 * Copyright (c) 2014 Timothy Rae <perceptualchaos2></perceptualchaos2>@gmail.com>      *
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
 * this program.  If not, see <http:></http:>//www.gnu.org/licenses/>.                  *
 */

import android.content.res.Configuration
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.SurfaceHolder
import android.view.WindowManager
import com.ichi2.anki.R
import com.ichi2.libanki.Sound
import kotlinx.android.synthetic.main.video_player.*


class VideoPlayer : AppCompatActivity(), android.view.SurfaceHolder.Callback {
    private var mPath: String = ""
    private var mSoundPlayer: Sound? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.video_player)
        mPath = intent.getStringExtra("path")
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        video_surface.holder.addCallback(this)
        mSoundPlayer = Sound()
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        mSoundPlayer!!.playSound(mPath, { mp ->
            finish();
            val originalListener = mSoundPlayer!!.mediaCompletionListener
            originalListener?.onCompletion(mp)
        }, video_surface)
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int,
                                height: Int) {
        // Do nothing...
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        mSoundPlayer!!.stopSounds()
        finish();
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        mSoundPlayer!!.notifyConfigurationChanged(video_surface)
    }
}