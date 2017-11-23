/****************************************************************************************
 * Copyright (c) 2014 Timothy Rae   <perceptualchaos2></perceptualchaos2>@gmail.com>                        *
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

package com.ichi2.libanki

import android.content.Context
import com.ichi2.utils.compat.CompatHelper
import org.json.JSONException
import org.json.JSONObject
import timber.log.Timber
import java.io.*
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

open class Exporter {
    var mCol: Collection
    var mDid: Long? = null

    constructor(col: Collection) {
        mCol = col
        mDid = null
    }

    constructor(col: Collection, did: Long?) {
        mCol = col
        mDid = did
    }
}


open class AnkiExporter(col: Collection) : Exporter(col) {
    var mIncludeSched: Boolean = false
    var mIncludeMedia: Boolean = false
    var mSrc: Collection? = null
    var mMediaDir: String? = null
    var mCount: Int = 0
    var mMediaFiles = ArrayList<String>()


    init {
        mIncludeSched = false
        mIncludeMedia = true
    }


    /**
     * Export source database into new destination database Note: The following python syntax isn't supported in
     * Android: for row in mSrc.db.execute("select * from cards where id in "+ids2str(cids)): therefore we use a
     * different method for copying tables
     *
     * @param path String path to destination database
     * @throws JSONException
     * @throws IOException
     */

    @Throws(JSONException::class, IOException::class)
    open fun exportInto(path: String, context: Context) {
        // create a new collection at the target
        File(path).delete()
        val dst = Storage.Collection(context, path)
        mSrc = mCol
        val src = mSrc!!
        // find cards
        val cids: Array<Long>
        cids = if (mDid == null) {
            Utils.list2ObjectArray(src.db.queryColumn(Long::class.java, "SELECT id FROM cards", 0))
        } else {
            src.decks.cids(mDid!!, true)
        }
        // attach dst to src so we can copy data between them. This isn't done in original libanki as Python more
        // flexible
        dst.close()
        Timber.d("Attach DB")
        src.db.database.execSQL("ATTACH '$path' AS DST_DB")
        // copy cards, noting used nids (as unique set)
        Timber.d("Copy cards")
        src.db.database
                .execSQL("INSERT INTO DST_DB.cards select * from cards where id in " + Utils.ids2str(cids))
        val nids = HashSet(src.db.queryColumn(Long::class.java,
                "select nid from cards where id in " + Utils.ids2str(cids), 0))
        // notes
        Timber.d("Copy notes")
        val uniqueNids = ArrayList(nids)
        val strnids = Utils.ids2str(uniqueNids)
        src.db.database.execSQL("INSERT INTO DST_DB.notes select * from notes where id in " + strnids)
        // remove system tags if not exporting scheduling info
        if (!mIncludeSched) {
            Timber.d("Stripping system tags from list")
            val srcTags = src.db.queryColumn(String::class.java,
                    "select tags from notes where id in " + strnids, 0)
            val args = ArrayList<Array<Any>>(srcTags.size)
            val arg = ArrayList<Any>()
            for (row in srcTags.indices) {
                arg[0] = removeSystemTags(srcTags[row])
                arg[1] = uniqueNids[row]
                args.add(row, arg.toArray())
            }
            src.db.executeMany("UPDATE DST_DB.notes set tags=? where id=?", args)
        }
        // models used by the notes
        Timber.d("Finding models used by notes")
        val mids = src.db.queryColumn(Long::class.java,
                "select distinct mid from DST_DB.notes where id in " + strnids, 0)
        // card history and revlog
        if (mIncludeSched) {
            Timber.d("Copy history and revlog")
            src.db.database
                    .execSQL("insert into DST_DB.revlog select * from revlog where cid in " + Utils.ids2str(cids))
            // reopen collection to destination database (different from original python code)
            src.db.database.execSQL("DETACH DST_DB")
            dst.reopen()
        } else {
            Timber.d("Detaching destination db and reopening")
            // first reopen collection to destination database (different from original python code)
            src.db.database.execSQL("DETACH DST_DB")
            dst.reopen()
            // then need to reset card state
            Timber.d("Resetting cards")
            dst.sched.resetCards(cids)
        }
        // models - start with zero
        Timber.d("Copy models")
        src.models.all()
                .filter { mids.contains(it.getLong("id")) }
                .forEach { dst.models.update(it) }
        // decks
        Timber.d("Copy decks")
        val dids = ArrayList<Long>()
        if (mDid != null) {
            dids.add(mDid!!)
            dids += src.decks.children(mDid!!).values
        }
        val dconfs = JSONObject()
        for (d in src.decks.all()) {
            if (d.getString("id") == "1") {
                continue
            }
            if (mDid != null && !dids.contains(d.getLong("id"))) {
                continue
            }
            if (d.getInt("dyn") != 1 && d.getLong("conf") != 1L) {
                if (mIncludeSched) {
                    dconfs.put(java.lang.Long.toString(d.getLong("conf")), true)
                }
            }
            if (!mIncludeSched) {
                // scheduling not included, so reset deck settings to default
                d.put("conf", 1)
            }
            dst.decks.update(d)
        }
        // copy used deck confs
        Timber.d("Copy deck options")
        src.decks.allConf()
                .filter { dconfs.has(it.getString("id")) }
                .forEach { dst.decks.updateConf(it) }
        // find used media
        Timber.d("Find used media")
        val media = JSONObject()
        mMediaDir = src.media.dir()
        if (mIncludeMedia) {
            val mid = src.db.queryColumn(Long::class.java, "select mid from notes where id in " + strnids,
                    0)
            val flds = src.db.queryColumn(String::class.java,
                    "select flds from notes where id in " + strnids, 0)
            mid.indices
                    .flatMap { src.media.filesInStr(mid[it], flds[it]) }
                    .forEach { media.put(it, true) }
            if (mMediaDir != null) {
                for (f in File(mMediaDir!!).listFiles()) {
                    val fname = f.name
                    if (fname.startsWith("_")) {
                        // Loop through every model that will be exported, and check if it contains a reference to f
                        for (idx in mid.indices) {
                            if (_modelHasMedia(src.models.get(idx.toLong()), fname)) {
                                media.put(fname, true)
                                break
                            }
                        }
                    }
                }
            }
        }
        val keys = media.names()
        if (keys != null) {
            for (i in 0 until keys.length()) {
                mMediaFiles.add(keys.getString(i))
            }
        }
        Timber.d("Cleanup")
        dst.crt = src.crt
        // todo: tags?
        mCount = dst.cardCount()
        dst.setMod()
        postExport()
        dst.close()
    }

    /**
     * Returns whether or not the specified model contains a reference to the given media file.
     * In order to ensure relatively fast operation we only check if the styling, front, back templates *contain* fname,
     * and thus must allow for occasional false positives.
     * @param model the model to scan
     * @param fname the name of the media file to check for
     * @return
     * @throws JSONException
     */
    @Throws(JSONException::class)
    private fun _modelHasMedia(model: JSONObject?, fname: String): Boolean {
        // Don't crash if the model is null
        if (model == null) {
            Timber.w("_modelHasMedia given null model")
            return true
        }
        // First check the styling
        if (model.getString("css").contains(fname)) {
            return true
        }
        // If not there then check the templates
        val tmpls = model.getJSONArray("tmpls")
        return (0 until tmpls.length())
                .map { tmpls.getJSONObject(it) }
                .any { it.getString("qfmt").contains(fname) || it.getString("afmt").contains(fname) }
    }


    /**
     * overwrite to apply customizations to the deck before it's closed, such as update the deck description
     */
    protected fun postExport() {}


    private fun removeSystemTags(tags: String): String = mSrc!!.tags.remFromStr("marked leech", tags)


    fun setIncludeSched(includeSched: Boolean) {
        mIncludeSched = includeSched
    }


    fun setIncludeMedia(includeMedia: Boolean) {
        mIncludeMedia = includeMedia
    }


    fun setDid(did: Long?) {
        mDid = did
    }
}


class AnkiPackageExporter(col: Collection) : AnkiExporter(col) {


    @Throws(IOException::class, JSONException::class)
    override fun exportInto(path: String, context: Context) {
        // open a zip file
        val z = ZipFile(path)
        // if all decks and scheduling included, full export
        val media: JSONObject
        if (mIncludeSched && mDid == null) {
            media = exportVerbatim(z)
        } else {
            // otherwise, filter
            media = exportFiltered(z, path, context)
        }
        // media map
        z.writeStr("media", Utils.jsonToString(media))
        z.close()
    }


    @Throws(IOException::class)
    private fun exportVerbatim(z: ZipFile): JSONObject {
        // close our deck & write it into the zip file, and reopen
        mCount = mCol.cardCount()
        mCol.close()
        z.write(mCol.path, "collection.anki2")
        mCol.reopen()
        // copy all media
        val media = JSONObject()
        if (!mIncludeMedia) {
            return media
        }
        val mdir = File(mCol.media.dir())
        if (mdir.exists() && mdir.isDirectory) {
            val mediaFiles = mdir.listFiles()
            var c = 0
            for (f in mediaFiles) {
                z.write(f.path, Integer.toString(c))
                try {
                    media.put(Integer.toString(c), f.name)
                    c++
                } catch (e: JSONException) {
                    e.printStackTrace()
                }

            }
        }
        return media
    }


    @Throws(IOException::class, JSONException::class)
    private fun exportFiltered(z: ZipFile, path: String, context: Context): JSONObject {
        // export into the anki2 file
        val colfile = path.replace(".apkg", ".anki2")
        super.exportInto(colfile, context)
        z.write(colfile, "collection.anki2")
        // and media
        prepareMedia()
        val media = JSONObject()
        val mdir = File(mCol.media.dir())
        if (mdir.exists() && mdir.isDirectory) {
            var c = 0
            for (file in mMediaFiles) {
                val mpath = File(mdir, file)
                if (mpath.exists()) {
                    z.write(mpath.path, Integer.toString(c))
                }
                try {
                    media.put(Integer.toString(c), file)
                    c++
                } catch (e: JSONException) {
                    e.printStackTrace()
                }

            }
        }
        // tidy up intermediate files
        CompatHelper.compat.deleteDatabase(File(colfile))
        CompatHelper.compat.deleteDatabase(File(path.replace(".apkg", ".media.ad.db2")))
        val tempPath = path.replace(".apkg", ".media")
        val file = File(tempPath)
        if (file.exists()) {
            val deleteCmd = "rm -r " + tempPath
            val runtime = Runtime.getRuntime()
            try {
                runtime.exec(deleteCmd)
            } catch (e: IOException) {
            }

        }
        return media
    }


    protected fun prepareMedia() {
        // chance to move each file in self.mediaFiles into place before media
        // is zipped up
    }
}


/**
 * Wrapper around standard Python zip class used in this module for exporting to APKG
 *
 * @author Tim
 */
internal class ZipFile @Throws(FileNotFoundException::class)
constructor(path: String) {
    val BUFFER_SIZE = 1024
    private val mZos: ZipOutputStream


    init {
        mZos = ZipOutputStream(BufferedOutputStream(FileOutputStream(path)))
    }


    @Throws(IOException::class)
    fun write(path: String, entry: String) {
        val bis = BufferedInputStream(FileInputStream(path), BUFFER_SIZE)
        val ze = ZipEntry(entry)
        writeEntry(bis, ze)
    }


    @Throws(IOException::class)
    fun writeStr(entry: String, value: String) {
        // TODO: Does this work with abnormal characters?
        val `is` = ByteArrayInputStream(value.toByteArray())
        val bis = BufferedInputStream(`is`, BUFFER_SIZE)
        val ze = ZipEntry(entry)
        writeEntry(bis, ze)
    }


    @Throws(IOException::class)
    private fun writeEntry(bis: BufferedInputStream, ze: ZipEntry) {
        val buf = ByteArray(BUFFER_SIZE)
        mZos.putNextEntry(ze)
        var len = bis.read(buf, 0, BUFFER_SIZE)
        while (len != -1) {
            mZos.write(buf, 0, len)
            len = bis.read(buf, 0, BUFFER_SIZE)
        }
        mZos.closeEntry()
        bis.close()
    }


    fun close() {
        try {
            mZos.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }

    }
}
