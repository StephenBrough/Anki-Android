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


class Expression(expressionString: String, private val parser: TopLevelParser) : TreeElement {
    var expressionType = ExpressionType.INVALID
        private set
    private var expression: Expression? = null
    private var term: Term? = null

    enum class ExpressionType {
        EXP_PLUS_TERM, EXP_MINUS_TERM, TERM, INVALID
    }

    init {
        if (!TopLevelParser.stringHasValidBrackets(expressionString)) {
            expressionType = ExpressionType.INVALID
        } else {
            var isReady = initAsExpPlusOrMinusTerm(expressionString)
            if (!isReady)
                isReady = initAsTerm(expressionString)
            if (!isReady)
                expressionType = ExpressionType.INVALID
        }
    }

    private fun initAsExpPlusOrMinusTerm(expressionString: String): Boolean {
        var bracketChecker = 0
        for (i in 0 until expressionString.length) {
            if (expressionString[i] == '(') {
                bracketChecker++
            }
            if (expressionString[i] == ')') {
                bracketChecker--
            }

            if ((expressionString[i] == '+' || expressionString[i] == '-') && bracketChecker == 0) {
                val leftSubString = expressionString.substring(0, i)
                if (!TopLevelParser.stringHasValidBrackets(leftSubString))
                    continue
                val leftExpression = Expression(leftSubString, parser)
                val isValidFirstPartExpression = leftExpression.expressionType != ExpressionType.INVALID

                if (!isValidFirstPartExpression)
                    continue

                val rightSubString = expressionString.substring(i + 1, expressionString.length)
                if (!TopLevelParser.stringHasValidBrackets(rightSubString))
                    continue

                val rightTerm = Term(rightSubString, parser)
                val isValidSecondPartTerm = rightTerm.termType != Term.TermType.INVALID

                if (isValidSecondPartTerm) {
                    if (expressionString[i] == '+') {
                        this.expressionType = ExpressionType.EXP_PLUS_TERM
                    } else {
                        this.expressionType = ExpressionType.EXP_MINUS_TERM
                    }

                    this.expression = leftExpression
                    this.term = rightTerm
                    return true
                }

            }
        }
        return false
    }

    private fun initAsTerm(expressionString: String): Boolean {
        if (!TopLevelParser.stringHasValidBrackets(expressionString))
            return false
        val term = Term(expressionString, parser)
        val isValidTerm = term.termType != Term.TermType.INVALID
        if (isValidTerm) {
            this.expressionType = ExpressionType.TERM
            this.term = term
            return true
        }
        return false
    }

    override val value: Double get() = when (expressionType) {
        Expression.ExpressionType.EXP_PLUS_TERM -> expression!!.value + term!!.value
        Expression.ExpressionType.EXP_MINUS_TERM -> expression!!.value - term!!.value
        Expression.ExpressionType.TERM -> term!!.value
        Expression.ExpressionType.INVALID -> throw ExpressionFormatException("could not parse Expression")
    }

    override val isVariable: Boolean get() = when (expressionType) {
        Expression.ExpressionType.EXP_PLUS_TERM, Expression.ExpressionType.EXP_MINUS_TERM -> expression!!.isVariable || term!!.isVariable
        Expression.ExpressionType.TERM -> term!!.isVariable
        Expression.ExpressionType.INVALID -> throw ExpressionFormatException("could not parse Expression")
    }

}
