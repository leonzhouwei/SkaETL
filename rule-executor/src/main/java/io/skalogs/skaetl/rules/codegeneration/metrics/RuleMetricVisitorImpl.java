package io.skalogs.skaetl.rules.codegeneration.metrics;

/*-
 * #%L
 * rule-executor
 * %%
 * Copyright (C) 2017 - 2018 SkaLogs
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import io.skalogs.skaetl.domain.JoinType;
import io.skalogs.skaetl.rules.RuleMetricBaseVisitor;
import io.skalogs.skaetl.rules.RuleMetricParser;
import io.skalogs.skaetl.rules.codegeneration.exceptions.RuleVisitorException;
import io.skalogs.skaetl.rules.functions.FunctionRegistry;
import lombok.Getter;
import org.antlr.v4.runtime.tree.RuleNode;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static io.skalogs.skaetl.rules.codegeneration.RuleToJava.*;

@Getter
public class RuleMetricVisitorImpl extends RuleMetricBaseVisitor<String> {

    private String from;
    private String window;
    private String where;
    private List<String> groupBy =  new ArrayList<>();
    private String having;
    private String aggFunction;
    private String aggFunctionField;

    private JoinType joinType;
    private String joinFrom;
    private String joinKeyFromA;
    private String joinKeyFromB;
    private String joinWhere;
    private String joinWindow;

    private final FunctionRegistry functionRegistry;

    public RuleMetricVisitorImpl(FunctionRegistry functionRegistry) {
        this.functionRegistry = functionRegistry;
    }

    @Override
    public String visitParse(RuleMetricParser.ParseContext ctx) {
        try {
            visit(ctx.select_clause());
            from = visitFrom(ctx.from());
            window = visitWindow(ctx.window());
            if (ctx.where() != null) {
                where = visit(ctx.where());
            }
            if (ctx.group_by() != null) {
                groupBy = visitGroupBy(ctx.group_by());
            }
            if (ctx.having() != null) {
                having = visitHaving(ctx.having());
            }
            if (ctx.join() != null) {
                visitJoin(ctx.join());
            }
            return "";
        } catch (Exception e) {
            throw new RuleVisitorException(e);
        }
    }

    public List<String> visitGroupBy(RuleMetricParser.Group_byContext ctx) {
        return ctx.group_by_expr().fieldname()
                .stream()
                .map(e -> StringUtils.trimToNull(visit(e)))
                .filter(e -> StringUtils.isNoneBlank(e))
                .collect(Collectors.toList());
    }

    @Override
    public String visitJoin(RuleMetricParser.JoinContext ctx) {
        visit(ctx.joinType());
        joinFrom = visit(ctx.target());
        joinKeyFromA = visit(ctx.fieldname(0));
        joinKeyFromB = visit(ctx.fieldname(1));
        if (ctx.where() != null) {
            joinWhere = visit(ctx.where());
        }
        joinWindow = visitJoinWindow(ctx.joinWindow());
        return "";
    }

    @Override
    public String visitInnerJoin(RuleMetricParser.InnerJoinContext ctx) {
        joinType = JoinType.INNER;
        return super.visitInnerJoin(ctx);
    }

    @Override
    public String visitOuterJoin(RuleMetricParser.OuterJoinContext ctx) {
        joinType = JoinType.OUTER;
        return super.visitOuterJoin(ctx);
    }

    @Override
    public String visitLeftJoin(RuleMetricParser.LeftJoinContext ctx) {
        joinType = JoinType.LEFT;
        return super.visitLeftJoin(ctx);
    }

    @Override
    public String visitJoinWindow(RuleMetricParser.JoinWindowContext ctx) {
        return "JoinWindows.of(" + visit(ctx.timeunit()) + ".toMillis(" + visit(ctx.INT()) + "))";
    }

    @Override
    public String visitTumblingWindowExpression(RuleMetricParser.TumblingWindowExpressionContext ctx) {
        return "aggregateTumblingWindow(kGroupedStream," +
                visit(ctx.INT()) +
                "," +
                visit(ctx.timeunit()) + ")";
    }

    @Override
    public String visitHoppingWindowExpression(RuleMetricParser.HoppingWindowExpressionContext ctx) {
        return "aggregateHoppingWindow(kGroupedStream," +
                visit(ctx.INT(0)) +
                "," + visit(ctx.timeunit(0)) + "," +
                visit(ctx.INT(1)) +
                "," + visit(ctx.timeunit(1)) + ")";
    }

    @Override
    public String visitSessionWindowExpression(RuleMetricParser.SessionWindowExpressionContext ctx) {
        return "aggregateSessionWindow(kGroupedStream," +
                visit(ctx.INT()) +
                "," +
                visit(ctx.timeunit()) + ")";
    }

    @Override
    public String visitHaving(RuleMetricParser.HavingContext ctx) {
        return visit(ctx.RESULT()) + " " + visit(ctx.COMPARISON_OPERATION()) + " " + visit(ctx.INT());
    }

    @Override
    public String visitAggfunction(RuleMetricParser.AggfunctionContext ctx) {
        aggFunction = visit(ctx.function_name());
        if (ctx.target() != null) {
            aggFunctionField = visit(ctx.target());
        }
        return aggFunction;
    }

    @Override
    public String visitTimeunit(RuleMetricParser.TimeunitContext ctx) {
        switch (ctx.getText()) {
            case "SECONDS":
            case "S":
            case "s":
                return "SECONDS";
            case "MINUTES":
            case "M":
            case "m":
                return "MINUTES";
            case "HOURS":
            case "H":
            case "h":
                return "HOURS";
            case "DAYS":
            case "D":
            case "d":
                return "DAYS";
            default:
                throw new RuntimeException(ctx.getText() + " is not a timeunit");
        }
    }

    @Override
    public String visitTerminal(TerminalNode node) {
        return node.getText();
    }

    @Override
    public String visitFloatAtom(RuleMetricParser.FloatAtomContext ctx) {
        return ctx.getText() + "f";
    }

    @Override
    public String visitFieldvalue(RuleMetricParser.FieldvalueContext ctx) {
        return "get(jsonValue,\"" + ctx.getText() + "\")";
    }

    protected String text(RuleNode node) {
        return node == null ? "" : node.getText();
    }

    @Override
    public String visitSubExpr(RuleMetricParser.SubExprContext ctx) {
        return "(" + visit(ctx.expr()) + ")";
    }

    @Override
    public String visitExponentExpr(RuleMetricParser.ExponentExprContext ctx) {
        return exp(visit(ctx.expr().get(0)), visit(ctx.expr().get(1)));
    }

    @Override
    public String visitHighPriorityOperationExpr(RuleMetricParser.HighPriorityOperationExprContext ctx) {
        String operation = ctx.HIGH_PRIORITY_OPERATION().getText();
        String expr1 = visit(ctx.expr(0));
        String expr2 = visit(ctx.expr(1));
        return highPriorityOperation(operation, expr1, expr2);
    }

    @Override
    public String visitLowPriorityOperationExpr(RuleMetricParser.LowPriorityOperationExprContext ctx) {
        String operation = ctx.LOW_PRIORITY_OPERATION().getText();
        String expr1 = visit(ctx.expr(0));
        String expr2 = visit(ctx.expr(1));
        return lowPriorityOperation(operation, expr1, expr2);
    }

    @Override
    public String visitComparisonExpr(RuleMetricParser.ComparisonExprContext ctx) {
        return comparisonMethod(ctx.COMPARISON_OPERATION().getText(), visit(ctx.expr(0)), visit(ctx.expr(1)));
    }

    @Override
    public String visitTimeCondition(RuleMetricParser.TimeConditionContext ctx) {
        return timeComparisonMethod(ctx.COMPARISON_OPERATION().getText(), visit(ctx.fieldvalue()), visit(ctx.INT()), visit(ctx.timeunit()));
    }

    @Override
    public String visitAndCondition(RuleMetricParser.AndConditionContext ctx) {
        return and(visit(ctx.expr(0)), visit(ctx.expr(1)));
    }

    @Override
    public String visitOrCondition(RuleMetricParser.OrConditionContext ctx) {
        return or(visit(ctx.expr(0)), visit(ctx.expr(1)));
    }

    @Override
    public String visitNotCondition(RuleMetricParser.NotConditionContext ctx) {
        return not(visit(ctx.expr()));
    }

    @Override
    public String visitIfCondition(RuleMetricParser.IfConditionContext ctx) {
        return ifCondition(visit(ctx.expr(0)), visit(ctx.expr(1)), visit(ctx.expr(2)));
    }

    @Override
    public String visitOneArgCondition(RuleMetricParser.OneArgConditionContext ctx) {
        String functionName = visit(ctx.functionname());
        return oneArgCondition(functionRegistry, functionName, visit(ctx.fieldvalue()));
    }

    @Override
    public String visitVarArgCondition(RuleMetricParser.VarArgConditionContext ctx) {
        String functionName = visit(ctx.functionname());
        String notOperation = ctx.NOT_OPERATION() != null ? "!" : "";
        String fieldValue = visit(ctx.fieldvalue());
        String args = visit(ctx.expr(), ",", "", "");
        return notOperation + varArgCondition(functionRegistry, functionName, fieldValue, args);
    }


    protected String visit(List<RuleMetricParser.ExprContext> exprs, String visitSeparators, String appendToVisitResultBegin, String appendToVisitResultEnd) {
        String args = "";
        for (RuleMetricParser.ExprContext expr : exprs) {
            if (!args.isEmpty()) {
                args += visitSeparators;
            }
            args += appendToVisitResultBegin + visit(expr) + appendToVisitResultEnd;
        }
        return args;
    }
}
