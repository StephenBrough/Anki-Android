/****************************************************************************************
 * Copyright (c) 2016 Jeffrey van Prehn <jvanprehn></jvanprehn>@gmail.com>                           *
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

package com.ichi2.anki.stats

import com.ichi2.libanki.Stats

/**
 * Interface between Stats.java and AdvancedStatistics.java
 */
data class StatsMetaInfo (
    var dynamicAxis: Boolean = false,
    var hasColoredCumulative: Boolean = false,
    var type: Stats.AxisType = Stats.AxisType.TYPE_LIFE,
    var title: Int = 0,
    var backwards: Boolean = false,
    var valueLabels: IntArray = IntArray(0),
    var colors: IntArray = IntArray(0),
    var axisTitles: IntArray = IntArray(0),
    var maxCards: Int = 0,
    var maxElements: Int = 0,
    var firstElement: Double = 0.0,
    var lastElement: Double = 0.0,
    var zeroIndex: Int = 0,
    var cumulative: Array<DoubleArray>? = emptyArray(),
    var count: Double = 0.toDouble(),
    var seriesList: Array<DoubleArray> = emptyArray(),
    var isStatsCalculated: Boolean = false,
    var isDataAvailable: Boolean = false
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as StatsMetaInfo

        if (dynamicAxis != other.dynamicAxis) return false
        if (hasColoredCumulative != other.hasColoredCumulative) return false
        if (type != other.type) return false
        if (title != other.title) return false
        if (backwards != other.backwards) return false
        if (maxCards != other.maxCards) return false
        if (maxElements != other.maxElements) return false
        if (firstElement != other.firstElement) return false
        if (lastElement != other.lastElement) return false
        if (count != other.count) return false

        return true
    }

    override fun hashCode(): Int {
        var result = dynamicAxis.hashCode()
        result = 31 * result + hasColoredCumulative.hashCode()
        result = 31 * result + type.hashCode()
        result = 31 * result + title
        result = 31 * result + backwards.hashCode()
        result = 31 * result + maxCards
        result = 31 * result + maxElements
        result = 31 * result + firstElement.hashCode()
        result = 31 * result + lastElement.hashCode()
        result = 31 * result + count.hashCode()
        return result
    }
}