/****************************************************************************************
 * Copyright (c) 2014 Michael Goldbach <michael></michael>@wildplot.com>                           *
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
package com.wildplot.android.parsing.AtomTypes

import com.wildplot.android.parsing.Atom
import com.wildplot.android.parsing.ExpressionFormatException
import com.wildplot.android.parsing.TreeElement
import kotlin.properties.Delegates

class NumberAtom(factorString: String) : TreeElement {

    var atomType: Atom.AtomType = Atom.AtomType.NUMBER
        private set
    private var mValue: Double by Delegates.notNull()

    init {
        try {
            this.mValue = java.lang.Double.parseDouble(factorString)
        } catch (e: NumberFormatException) {
            atomType = Atom.AtomType.INVALID
        }

    }

    override val value: Double get() {
        return if (atomType != Atom.AtomType.INVALID)
            mValue
        else
            throw ExpressionFormatException("Number is Invalid, cannot parse")
    }

    override val isVariable: Boolean get() {
        return if (atomType != Atom.AtomType.INVALID)
            false
        else
            throw ExpressionFormatException("Number is Invalid, cannot parse")
    }
}
