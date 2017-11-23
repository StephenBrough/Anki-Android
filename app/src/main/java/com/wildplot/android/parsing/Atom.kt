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

import com.wildplot.android.parsing.AtomTypes.*

class Atom internal constructor(atomString: String, private val parser: TopLevelParser) : TreeElement {
    var atomType = AtomType.INVALID
        private set
    private var atomObject: TreeElement? = null
    private var expression: Expression? = null

    enum class AtomType {
        VARIABLE, NUMBER, EXP_IN_BRACKETS, FUNCTION_MATH, FUNCTION_X, FUNCTION_X_Y, INVALID
    }

    init {

        if (!TopLevelParser.stringHasValidBrackets(atomString)) {
            atomType = AtomType.INVALID
        } else {
            var isValid = initAsExpInBrackets(atomString)
            if (!isValid)
                isValid = initAsFunctionMath(atomString)
            if (!isValid)
                isValid = initAsFunctionX(atomString)
            if (!isValid)
                isValid = initAsFunctionXY(atomString)
            if (!isValid)
                isValid = initAsNumber(atomString)
            if (!isValid)
                isValid = initAsXVariable(atomString)
            if (!isValid)
                isValid = initAsYVariable(atomString)
            if (!isValid)
                isValid = initAsVariable(atomString)
            if (!isValid)
                atomType = AtomType.INVALID
        }

    }


    private fun initAsExpInBrackets(atomString: String): Boolean {
        if (atomString.length > 0 && atomString[0] == '(' && atomString[atomString.length - 1] == ')') {
            val expressionString = atomString.substring(1, atomString.length - 1)
            val expressionInBrackets = Expression(expressionString, parser)
            val isValidExpressionInBrackets = expressionInBrackets.expressionType != Expression.ExpressionType.INVALID
            if (isValidExpressionInBrackets) {
                this.expression = expressionInBrackets
                this.atomType = AtomType.EXP_IN_BRACKETS
                return true
            }
        }

        return false
    }

    private fun initAsFunctionMath(atomString: String): Boolean {
        val mathFunctionAtom = MathFunctionAtom(atomString, parser)
        val isValidMathFunction = mathFunctionAtom.mathType !== MathFunctionAtom.MathType.INVALID
        if (isValidMathFunction) {
            this.atomType = AtomType.FUNCTION_MATH
            this.atomObject = mathFunctionAtom
            return true
        }

        return false
    }

    private fun initAsFunctionX(atomString: String): Boolean {
        val functionXAtom = FunctionXAtom(atomString, parser)
        val isValidFunctionXAtom = functionXAtom.atomType != AtomType.INVALID
        if (isValidFunctionXAtom) {
            this.atomType = AtomType.FUNCTION_X
            this.atomObject = functionXAtom
            return true
        }

        return false
    }

    private fun initAsFunctionXY(atomString: String): Boolean {
        val functionXYAtom = FunctionXYAtom(atomString, parser)
        val isValidFunctionXYAtom = functionXYAtom.atomType != AtomType.INVALID
        if (isValidFunctionXYAtom) {
            this.atomType = AtomType.FUNCTION_X_Y
            this.atomObject = functionXYAtom
            return true
        }

        return false
    }

    private fun initAsNumber(atomString: String): Boolean {
        val numberAtom = NumberAtom(atomString)
        val isValidNumberAtom = numberAtom.atomType != AtomType.INVALID
        if (isValidNumberAtom) {
            this.atomType = numberAtom.atomType
            this.atomObject = numberAtom
            return true
        }
        return false
    }

    private fun initAsXVariable(atomString: String): Boolean {
        if (atomString == parser.getxName()) {
            this.atomType = AtomType.VARIABLE
            this.atomObject = XVariableAtom(parser)
            return true
        }

        return false
    }

    private fun initAsYVariable(atomString: String): Boolean {
        if (atomString == parser.getyName()) {
            this.atomType = AtomType.VARIABLE
            this.atomObject = YVariableAtom(parser)
            return true
        }

        return false
    }

    private fun initAsVariable(atomString: String): Boolean {
        val variableAtom = VariableAtom(atomString, parser)
        val isValidVariableAtom = variableAtom.atomType != AtomType.INVALID
        if (isValidVariableAtom) {
            this.atomType = variableAtom.atomType
            this.atomObject = variableAtom
            return true
        }
        return false
    }

    override val value: Double
    get() = when (atomType) {
        Atom.AtomType.EXP_IN_BRACKETS -> expression!!.value
        Atom.AtomType.VARIABLE, Atom.AtomType.NUMBER, Atom.AtomType.FUNCTION_MATH, Atom.AtomType.FUNCTION_X, Atom.AtomType.FUNCTION_X_Y -> atomObject!!.value
        Atom.AtomType.INVALID -> throw ExpressionFormatException("cannot parse Atom object")
    }

    override val isVariable: Boolean get() = when (atomType) {
        Atom.AtomType.EXP_IN_BRACKETS -> expression!!.isVariable
        Atom.AtomType.VARIABLE, Atom.AtomType.NUMBER, Atom.AtomType.FUNCTION_MATH, Atom.AtomType.FUNCTION_X, Atom.AtomType.FUNCTION_X_Y -> atomObject!!.isVariable
        Atom.AtomType.INVALID -> throw ExpressionFormatException("cannot parse Atom object")
    }
}
