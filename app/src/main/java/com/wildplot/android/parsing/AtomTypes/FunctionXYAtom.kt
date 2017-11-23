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

import com.wildplot.android.parsing.*

import java.util.regex.Pattern

/**
 * @author Michael Goldbach
 */
class FunctionXYAtom(funcString: String, private val parser: TopLevelParser) : TreeElement {

    var atomType: Atom.AtomType = Atom.AtomType.FUNCTION_X
        private set
    private var expressionLeft: Expression? = null
    private var expressionRight: Expression? = null
    private var funcName: String? = null

    init {

        val isValid = init(funcString)
        if (!isValid) {
            this.atomType = Atom.AtomType.INVALID
        }
    }

    private fun init(funcString: String): Boolean {
        val leftBracket = funcString.indexOf("(")
        val rightBracket = funcString.lastIndexOf(")")
        var comma = funcString.indexOf(",")    //see if there even is a comma
        if (leftBracket > 1 && rightBracket > leftBracket + 1 && comma > leftBracket && comma < rightBracket) {
            //test all commas
            for (i in leftBracket + 1 until rightBracket) {
                if (funcString[i] == ',') {
                    comma = i
                }
                val funcName = funcString.substring(0, leftBracket)
                val p = Pattern.compile("[^a-zA-Z0-9]")
                val hasSpecialChar = p.matcher(funcName).find()

                if (hasSpecialChar || funcName.isEmpty()) {
                    this.atomType = Atom.AtomType.INVALID
                    return false
                }

                val leftExpressionString = funcString.substring(leftBracket + 1, comma)
                val rightExpressionString = funcString.substring(comma + 1, rightBracket)
                val leftExpressionInBrackets = Expression(leftExpressionString, parser)
                val rightExpressionInBrackets = Expression(rightExpressionString, parser)
                val isValidLeftExpression = leftExpressionInBrackets.expressionType != Expression.ExpressionType.INVALID
                val isValidRightExpression = rightExpressionInBrackets.expressionType != Expression.ExpressionType.INVALID
                if (isValidLeftExpression && isValidRightExpression) {
                    this.atomType = Atom.AtomType.FUNCTION_X
                    this.funcName = funcName
                    this.expressionLeft = leftExpressionInBrackets
                    this.expressionRight = rightExpressionInBrackets
                    return true
                }
            }
        }

        return false
    }

    override val value: Double get() {
        return if (atomType != Atom.AtomType.INVALID) {
            parser.getFuncVal(funcName!!, expressionLeft!!.value, expressionRight!!.value)
        } else
            throw ExpressionFormatException("Number is Invalid, cannot parse")
    }

    override val isVariable: Boolean get() {
        return if (atomType != Atom.AtomType.INVALID) {
            expressionLeft!!.isVariable || expressionRight!!.isVariable
        } else
            throw ExpressionFormatException("Number is Invalid, cannot parse")
    }
}
