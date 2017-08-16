package com.ichi2.anki

import android.app.Activity
import android.content.res.Configuration
import android.os.Bundle
import android.view.SurfaceHolder
import android.view.View
import android.view.WindowManager
import android.widget.VideoView
import com.ichi2.libanki.Sound


/****************************************************************************************
 * Copyright (c) 2014 Timothy Rae <perceptualchaos2></perceptualchaos2>@gmail.com>                          *
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

class VideoPlayer : Activity(), android.view.SurfaceHolder.Callback {
    internal var mVideoView: VideoView? = null
    internal var mPath: String = ""
    internal var mSoundPlayer: Sound? = null

    /** Called when the activity is first created.  */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.video_player)
        mPath = intent.getStringExtra("path")
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        mVideoView = findViewById<View>(R.id.video_surface) as VideoView?
        mVideoView!!.holder.addCallback(this)
        mSoundPlayer = Sound()
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        mSoundPlayer!!.playSound(mPath, { mp ->
            finish()
            val originalListener = mSoundPlayer!!.mediaCompletionListener
            originalListener?.onCompletion(mp)
        }, mVideoView)
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int,
                                height: Int) {
        // TODO Auto-generated method stub

    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        mSoundPlayer!!.stopSounds()
        finish()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        mSoundPlayer!!.notifyConfigurationChanged(mVideoView)
    }

    public override fun onStop() {
        super.onStop()
    }
}