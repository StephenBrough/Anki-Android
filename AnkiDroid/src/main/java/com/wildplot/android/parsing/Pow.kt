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
package com.wildplot.android.parsing


class Pow(powString: String, private val parser: TopLevelParser) : TreeElement {
    var powType = PowType.INVALID
        private set
    private var atom: Atom? = null
    private var factor: Factor? = null

    enum class PowType {
        ATOM, ATOM_POW_FACTOR, ATOM_SQRT_FACTOR, INVALID
    }

    init {
        if (!TopLevelParser.stringHasValidBrackets(powString)) {
            powType = PowType.INVALID
        } else {
            var isReady: Boolean

            isReady = initAsAtom(powString)
            if (!isReady)
                isReady = initAsAtomPowFactor(powString)
            if (!isReady)
                isReady = initAsAtomSqrtFactor(powString)
            if (!isReady)
                powType = PowType.INVALID
        }
    }

    private fun initAsAtom(powString: String): Boolean {
        val atom = Atom(powString, parser)
        val isValidAtom = atom.atomType !== Atom.AtomType.INVALID
        if (isValidAtom) {
            this.powType = PowType.ATOM
            this.atom = atom
            return true
        }
        return false
    }

    private fun initAsAtomPowFactor(powString: String): Boolean {
        val opPos = powString.indexOf("^")
        if (opPos > 0) {
            val leftAtomString = powString.substring(0, opPos)
            val rightFactorString = powString.substring(opPos + 1, powString.length)
            if (!TopLevelParser.stringHasValidBrackets(leftAtomString) || !TopLevelParser.stringHasValidBrackets(rightFactorString))
                return false
            val leftAtom = Atom(leftAtomString, parser)
            val isValidAtom = leftAtom.atomType !== Atom.AtomType.INVALID
            if (isValidAtom) {
                val rightFactor = Factor(rightFactorString, parser)
                val isValidFactor = rightFactor.factorType !== Factor.FactorType.INVALID
                if (isValidFactor) {
                    this.powType = PowType.ATOM_POW_FACTOR
                    this.atom = leftAtom
                    this.factor = rightFactor
                    return true
                }
            }
        }

        return false
    }

    private fun initAsAtomSqrtFactor(powString: String): Boolean {
        val opPos = powString.indexOf("**")
        if (opPos > 0) {
            val leftAtomString = powString.substring(0, opPos)
            val rightFactorString = powString.substring(opPos + 2, powString.length)
            if (!TopLevelParser.stringHasValidBrackets(leftAtomString) || !TopLevelParser.stringHasValidBrackets(rightFactorString))
                return false
            val leftAtom = Atom(leftAtomString, parser)
            val isValidAtom = leftAtom.atomType !== Atom.AtomType.INVALID
            if (isValidAtom) {
                val rightFactor = Factor(rightFactorString, parser)
                val isValidFactor = rightFactor.factorType !== Factor.FactorType.INVALID
                if (isValidFactor) {
                    this.powType = PowType.ATOM_SQRT_FACTOR
                    this.atom = leftAtom
                    this.factor = rightFactor
                    return true
                }
            }
        }

        return false
    }

    override val value: Double get() = when (powType) {
        Pow.PowType.ATOM -> atom!!.value
        Pow.PowType.ATOM_POW_FACTOR -> Math.pow(atom!!.value, factor!!.value)
        Pow.PowType.ATOM_SQRT_FACTOR -> Math.pow(atom!!.value, 1.0 / factor!!.value)
        Pow.PowType.INVALID -> throw ExpressionFormatException("cannot parse Atom expression")
    }

    override val isVariable: Boolean get() = when (powType) {
        Pow.PowType.ATOM -> atom!!.isVariable
        Pow.PowType.ATOM_POW_FACTOR, Pow.PowType.ATOM_SQRT_FACTOR -> atom!!.isVariable || factor!!.isVariable
        Pow.PowType.INVALID -> throw ExpressionFormatException("cannot parse Atom expression")
    }
}
