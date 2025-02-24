package it.unibo.tesi.chorol.visitor.expression;

import jolie.lang.Constants;
import jolie.lang.parse.ast.CompareConditionNode;
import jolie.lang.parse.ast.OLSyntaxNode;
import jolie.lang.parse.ast.expression.*;
import jolie.util.Pair;

import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

public class ExprVisitor extends ExprVisitorBase {

	/**
	 * Restituisce un valore numerico che rappresenta la precedenza dell'operatore
	 * associato al nodo. Numeri maggiori indicano una maggiore forza di legame.
	 */
	private int getPrecedence(Object node) {
		if (node instanceof OrConditionNode) return 1;
		else if (node instanceof AndConditionNode) return 2;
		else if (node instanceof CompareConditionNode) return 3;
		else if (node instanceof SumExpressionNode) return 4;
		else if (node instanceof ProductExpressionNode) return 5;
		// Costanti, variabili, ecc. hanno la precedenza massima.
		return 6;
	}

	/**
	 * Visita il nodo figlio e, se la sua precedenza è inferiore a quella del nodo corrente,
	 * lo racchiude tra parentesi per disambiguare.
	 */
	private String visitWithParenthesisIfNeeded(OLSyntaxNode node, int parentPrecedence, Void unused) {
		String result = node.accept(this, unused);
		if (this.getPrecedence(node) < parentPrecedence) return "(" + result + ")";
		return result;
	}

	@Override
	public String visit(OrConditionNode orConditionNode, Void unused) {
		int currentPrecedence = this.getPrecedence(orConditionNode);
		return orConditionNode.children().stream()
				       .map(child -> this.visitWithParenthesisIfNeeded(child, currentPrecedence, unused))
				       .collect(Collectors.joining(" || "));
	}

	@Override
	public String visit(AndConditionNode andConditionNode, Void unused) {
		int currentPrecedence = this.getPrecedence(andConditionNode);
		return andConditionNode.children().stream()
				       .map(child -> this.visitWithParenthesisIfNeeded(child, currentPrecedence, unused))
				       .collect(Collectors.joining(" && "));
	}

	@Override
	public String visit(CompareConditionNode compareConditionNode, Void unused) {
		int currentPrecedence = this.getPrecedence(compareConditionNode);
		String left = this.visitWithParenthesisIfNeeded(compareConditionNode.leftExpression(), currentPrecedence, unused);
		String right = this.visitWithParenthesisIfNeeded(compareConditionNode.rightExpression(), currentPrecedence, unused);
		return String.format("%s %s %s",
				left,
				ExprVisitorBase.compare2String(compareConditionNode.opType()),
				right);
	}

	@Override
	public String visit(SumExpressionNode sumExpressionNode, Void unused) {
		int currentPrecedence = this.getPrecedence(sumExpressionNode);
		StringBuilder sb = new StringBuilder();
		boolean first = true;
		sumExpressionNode.operands()
				.forEach(entry ->
						         this.visitWithParenthesisIfNeeded(entry.value(), currentPrecedence, unused));
		for (var entry : sumExpressionNode.operands()) {
			String operandString = this.visitWithParenthesisIfNeeded(entry.value(), currentPrecedence, unused);
			if (operandString == null || operandString.isBlank()) continue;
			String opStr = ExprVisitorBase.operand2String(entry.key());
			if (!first) sb.append(" ").append(opStr).append(" ").append(operandString);
			else {
				if (opStr.equals("+")) opStr = "";
				sb.append(opStr).append(operandString);
				first = false;
			}
		}
		return sb.toString();
	}

	@Override
	public String visit(ProductExpressionNode productExpressionNode, Void unused) {
		int currentPrecedence = this.getPrecedence(productExpressionNode);
		List<Pair<Constants.OperandType, OLSyntaxNode>> operands = productExpressionNode.operands();
		if (operands.isEmpty()) return "";
		Iterator<Pair<Constants.OperandType, OLSyntaxNode>> iter = operands.iterator();
		Pair<Constants.OperandType, OLSyntaxNode> firstEntry = iter.next();
		StringBuilder sb = new StringBuilder();
		sb.append(this.visitWithParenthesisIfNeeded(firstEntry.value(), currentPrecedence, unused));
		while (iter.hasNext()) {
			Pair<Constants.OperandType, OLSyntaxNode> entry = iter.next();
			String opStr = ExprVisitorBase.operand2String(entry.key());
			String operandString = this.visitWithParenthesisIfNeeded(entry.value(), currentPrecedence, unused);
			sb.append(" ").append(opStr).append(" ").append(operandString);
		}

		return sb.toString();
	}

	@Override
	public String visit(ConstantIntegerExpression constantIntegerExpression, Void unused) {
		return String.valueOf(constantIntegerExpression.value());
	}

	@Override
	public String visit(VariableExpressionNode variableExpressionNode, Void unused) {
		return variableExpressionNode.variablePath().toPrettyString();
	}
}
