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

package com.ichi2.anki.multimediacard.fields

import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.LinearLayout
import com.ichi2.anki.junkdrawer.CollectionHelper
import com.ichi2.anki.R
import com.ichi2.anki.multimediacard.AudioView
import timber.log.Timber
import java.io.File
import java.io.IOException

class BasicAudioFieldController : FieldControllerBase(), IFieldController {

    /**
     * This controller always return a temporary path where it writes the audio
     */
    private var tempAudioPath: String? = null
    private var origAudioPath: String? = null
    private var mAudioView: AudioView? = null


    override fun createUI(context: Context, layout: LinearLayout) {
        origAudioPath = mField!!.audioPath

        var bExist = false

        if (origAudioPath != null) {
            val f = File(origAudioPath!!)

            if (f.exists()) {
                tempAudioPath = f.absolutePath
                bExist = true
            }
        }

        if (!bExist) {
            tempAudioPath = try {
                val col = CollectionHelper.getInstance().getCol(context)
                val storingDirectory = File(col!!.media.dir())
                val file = File.createTempFile("ankidroid_audiorec", ".3gp", storingDirectory)
                file!!.absolutePath
            } catch (e: IOException) {
                Timber.e("Could not create temporary audio file. " + e.message)
                null
            }
        }

        mAudioView = AudioView.createRecorderInstance(mActivity!!, R.drawable.av_play, R.drawable.av_pause,
                R.drawable.av_stop, R.drawable.av_rec, R.drawable.av_rec_stop, tempAudioPath!!)
        mAudioView!!.setOnRecordingFinishEventListener(object: AudioView.OnRecordingFinishEventListener {
            override fun onRecordingFinish(v: View) {
                mField!!.audioPath = tempAudioPath
                mField!!.setHasTemporaryMedia(true)
            }
        })

        layout.addView(mAudioView, LinearLayout.LayoutParams.MATCH_PARENT)
    }

    override fun onDone() {
        mAudioView!!.notifyStopRecord()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {}

    override fun onFocusLost() = mAudioView!!.notifyReleaseRecorder()

    override fun onDestroy() = mAudioView!!.notifyReleaseRecorder()
}
