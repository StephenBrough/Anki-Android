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

import com.wildplot.android.parsing.Expression
import com.wildplot.android.parsing.ExpressionFormatException
import com.wildplot.android.parsing.TopLevelParser
import com.wildplot.android.parsing.TreeElement


class MathFunctionAtom(funcString: String, private val parser: TopLevelParser) : TreeElement {
    var mathType = MathType.INVALID
        private set
    private var expression: Expression? = null
    private var hasSavedValue = false
    private var savedValue = 0.0

    enum class MathType {
        SIN, COS, TAN, SQRT, ACOS, ASIN, ATAN, SINH, COSH, LOG, LN, INVALID
    }

    init {
        val isValid = init(funcString)
        if (!isValid) {
            this.mathType = MathType.INVALID
        }
        if (isValid && !isVariable) {
            savedValue = value
            hasSavedValue = true
        }
    }

    private fun init(funcString: String): Boolean {
        val leftBracket = funcString.indexOf("(")
        val rightBracket = funcString.lastIndexOf(")")
        if (leftBracket > 1 && rightBracket > leftBracket + 1) {
            val funcName = funcString.substring(0, leftBracket)
            val expressionString = funcString.substring(leftBracket + 1, rightBracket)
            val expressionInBrackets = Expression(expressionString, parser)
            val isValidExpression = expressionInBrackets.expressionType != Expression.ExpressionType.INVALID
            if (isValidExpression) {
                when (funcName) {
                    "sin" -> this.mathType = MathType.SIN
                    "cos" -> this.mathType = MathType.COS
                    "tan" -> this.mathType = MathType.TAN
                    "sqrt" -> this.mathType = MathType.SQRT
                    "acos" -> this.mathType = MathType.ACOS
                    "asin" -> this.mathType = MathType.ASIN
                    "atan" -> this.mathType = MathType.ATAN
                    "sinh" -> this.mathType = MathType.SINH
                    "cosh" -> this.mathType = MathType.COSH
                    "log", "lg" -> this.mathType = MathType.LOG
                    "ln" -> this.mathType = MathType.LN
                    else -> {
                        this.mathType = MathType.INVALID
                        return false
                    }
                }
                this.expression = expressionInBrackets
                return true
            }
        }
        return false
    }

    override val value: Double get() {
        if (hasSavedValue)
            return savedValue

        return when (mathType) {
            MathFunctionAtom.MathType.SIN -> Math.sin(expression!!.value)
            MathFunctionAtom.MathType.COS -> Math.cos(expression!!.value)
            MathFunctionAtom.MathType.TAN -> Math.tan(expression!!.value)
            MathFunctionAtom.MathType.SQRT -> Math.sqrt(expression!!.value)
            MathFunctionAtom.MathType.ACOS -> Math.acos(expression!!.value)
            MathFunctionAtom.MathType.ASIN -> Math.asin(expression!!.value)
            MathFunctionAtom.MathType.ATAN -> Math.atan(expression!!.value)
            MathFunctionAtom.MathType.SINH -> Math.sinh(expression!!.value)
            MathFunctionAtom.MathType.COSH -> Math.cosh(expression!!.value)
            MathFunctionAtom.MathType.LOG -> Math.log10(expression!!.value)
            MathFunctionAtom.MathType.LN -> Math.log(expression!!.value)
            MathFunctionAtom.MathType.INVALID -> throw ExpressionFormatException("Number is Invalid, cannot parse")
        }
    }

    override val isVariable: Boolean get() {
        return if (mathType != MathType.INVALID) {
            expression!!.isVariable
        } else
            throw ExpressionFormatException("Number is Invalid, cannot parse")
    }
}
