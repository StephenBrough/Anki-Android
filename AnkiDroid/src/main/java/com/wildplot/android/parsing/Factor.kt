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


class Factor(factorString: String, private val parser: TopLevelParser) : TreeElement {
    var factorType = FactorType.INVALID
        private set
    private var factor: Factor? = null
    private var pow: Pow? = null

    enum class FactorType {
        PLUS_FACTOR, MINUS_FACTOR, POW, INVALID
    }

    init {
        if (!TopLevelParser.stringHasValidBrackets(factorString)) {
            factorType = FactorType.INVALID
        } else {
            var isReady: Boolean

            isReady = initAsPlusFactor(factorString)
            if (!isReady)
                isReady = initAsMinusFactor(factorString)
            if (!isReady)
                isReady = initAsPow(factorString)
            if (!isReady)
                factorType = FactorType.INVALID
        }
    }

    private fun initAsPlusFactor(factorString: String): Boolean {
        if (factorString.isNotEmpty() && factorString[0] == '+') {
            val isValidFactor: Boolean
            val leftSubString = factorString.substring(1, factorString.length)
            val leftFactor = Factor(leftSubString, parser)
            isValidFactor = leftFactor.factorType != FactorType.INVALID
            if (isValidFactor) {
                this.factorType = FactorType.PLUS_FACTOR
                this.factor = leftFactor
                return true
            }
        }

        return false
    }

    private fun initAsMinusFactor(factorString: String): Boolean {
        if (factorString.isNotEmpty() && factorString[0] == '-') {
            val isValidFactor: Boolean
            val leftSubString = factorString.substring(1, factorString.length)
            val leftFactor = Factor(leftSubString, parser)
            isValidFactor = leftFactor.factorType != FactorType.INVALID
            if (isValidFactor) {
                this.factorType = FactorType.MINUS_FACTOR
                this.factor = leftFactor
                return true
            }
        }

        return false
    }

    private fun initAsPow(factorString: String): Boolean {
        val pow = Pow(factorString, parser)
        val isValidPow = pow.powType != Pow.PowType.INVALID
        if (isValidPow) {
            this.factorType = FactorType.POW
            this.pow = pow
            return true
        }
        return false
    }

    override val value: Double get() = when (factorType) {
        Factor.FactorType.PLUS_FACTOR -> factor!!.value
        Factor.FactorType.MINUS_FACTOR -> -factor!!.value
        Factor.FactorType.POW -> pow!!.value
        Factor.FactorType.INVALID -> throw ExpressionFormatException("cannot parse expression at factor level")
    }

    override val isVariable: Boolean get() = when (factorType) {
        Factor.FactorType.PLUS_FACTOR, Factor.FactorType.MINUS_FACTOR -> factor!!.isVariable
        Factor.FactorType.POW -> pow!!.isVariable
        Factor.FactorType.INVALID -> throw ExpressionFormatException("cannot parse expression at factor level")
    }
}
